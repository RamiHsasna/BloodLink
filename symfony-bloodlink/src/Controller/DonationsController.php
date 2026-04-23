<?php

namespace App\Controller;

use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class DonationsController extends AbstractController
{
    // ─── LIST ────────────────────────────────────────────────────────────────
    #[Route('/dashboard/donations', name: 'donations_index', methods: ['GET'])]
    public function index(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        $userId = (string) ($sessionUser['id'] ?? '');
        $search = trim((string) $request->query->get('q', ''));
        $status = strtoupper(trim((string) $request->query->get('status', '')));

        // Base query with joins
        $sql = "
            SELECT d.*,
                   de.name AS donation_event_name,
                   h.name AS hospital_name,
                   donor.first_name AS donor_first_name,
                   donor.last_name AS donor_last_name
            FROM donations d
            LEFT JOIN donation_events de ON de.event_id = d.donation_event_id
            LEFT JOIN hospital h ON h.hospital_id = d.hospital_id
            LEFT JOIN donors donor ON donor.user_id = d.user_id
            WHERE 1=1
        ";
        $params = [];

        // If donor, show only their donations
        if ($userType === 'DONOR') {
            $sql .= " AND d.user_id = :donor_id";
            $params['donor_id'] = $userId;
        }
        // Admin sees all

        // Search by donor name or event name
        if ($search !== '') {
            $sql .= " AND (CONCAT(donor.first_name, ' ', donor.last_name) ILIKE :q 
                      OR de.name ILIKE :q 
                      OR h.name ILIKE :q)";
            $params['q'] = '%' . $search . '%';
        }

        // Filter by status
        if ($status !== '') {
            $sql .= " AND d.status = :status";
            $params['status'] = $status;
        }

        $sql .= " ORDER BY d.donation_date DESC";

        $donations = $connection->fetchAllAssociative($sql, $params);

        // Stats query
        $statsSql = "
            SELECT
                COUNT(*) AS total,
                COUNT(*) FILTER (WHERE status = 'scheduled') AS scheduled,
                COUNT(*) FILTER (WHERE status = 'completed') AS completed,
                COUNT(*) FILTER (WHERE status = 'cancelled') AS cancelled,
                COUNT(*) FILTER (WHERE screening_passed = true) AS screening_passed,
                COALESCE(SUM(units_collected), 0) AS total_units_collected
            FROM donations
        ";

        if ($userType === 'DONOR') {
            $statsSql .= " WHERE user_id = :donor_id";
        }

        $stats = $connection->fetchAssociative($statsSql, 
            $userType === 'DONOR' ? ['donor_id' => $userId] : []
        );

        // Get hospitals and donation events for filters
        $hospitals = $connection->fetchAllAssociative(
            "SELECT hospital_id, name FROM hospital ORDER BY name"
        );

        $donationEvents = $connection->fetchAllAssociative(
            "SELECT event_id, name FROM donation_events WHERE status IN ('PLANNED', 'ACTIVE') ORDER BY start_date DESC"
        );

        return $this->render('dashboard/donations.html.twig', [
            'session_user' => $sessionUser,
            'donations' => $donations,
            'stats' => $stats,
            'hospitals' => $hospitals,
            'donation_events' => $donationEvents,
            'search' => $search,
            'status_filter' => $status,
            'user_type' => $userType,
        ]);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────
    #[Route('/dashboard/donations/create', name: 'donations_create', methods: ['POST'])]
    public function create(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot create donation records.');
            return $this->redirectToRoute('donations_index');
        }

        // Get donor - accept either UUID or name
        $donorInput = $request->request->get('donor_name');
        $userId = null;

        if ($donorInput) {
            // Try to find by UUID first
            $donor = $connection->fetchAssociative(
                "SELECT user_id FROM donors WHERE user_id::text = :id LIMIT 1",
                ['id' => $donorInput]
            );
            
            // If not found, try by name (first_name or last_name)
            if (!$donor) {
                $donor = $connection->fetchAssociative(
                    "SELECT user_id FROM donors WHERE CONCAT(first_name, ' ', last_name) ILIKE :name OR first_name ILIKE :name OR last_name ILIKE :name LIMIT 1",
                    ['name' => '%' . $donorInput . '%']
                );
            }

            // If still not found, create new donor
            if (!$donor) {
                $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
                
                // Parse name into first and last
                $nameParts = explode(' ', trim($donorInput), 2);
                $firstName = $nameParts[0];
                $lastName = $nameParts[1] ?? '';
                $bloodType = $request->request->get('blood_type_id') ?: 'O+';
                $phone = $request->request->get('donor_phone') ?: null;
                $city = $request->request->get('donor_city') ?: null;
                
                // Generate email
                $email = strtolower(str_replace(' ', '.', $firstName . '.' . $lastName)) . '@bloodlink.local';
                
                // Check if user with this email already exists
                $existingUser = $connection->fetchAssociative(
                    "SELECT user_id FROM users WHERE email = :email LIMIT 1",
                    ['email' => $email]
                );
                
                if ($existingUser) {
                    // User already exists, use it
                    $userId = $existingUser['user_id'];
                    
                    // Check if donor record exists
                    $existingDonor = $connection->fetchAssociative(
                        "SELECT user_id FROM donors WHERE user_id = :user_id LIMIT 1",
                        ['user_id' => $userId]
                    );
                    
                    if (!$existingDonor) {
                        // Create donor record
                        $connection->executeStatement("
                            INSERT INTO donors (user_id, first_name, last_name, blood_type_id, city, last_donation_date, is_currently_eligible, total_donations, created_at)
                            VALUES (:user_id, :first_name, :last_name, :blood_type_id, :city, null, true, 0, :created_at)
                        ", [
                            'user_id' => $userId,
                            'first_name' => $firstName,
                            'last_name' => $lastName,
                            'blood_type_id' => $bloodType,
                            'city' => $city,
                            'created_at' => $now,
                        ]);
                    }
                } else {
                    // Create new user
                    $userId = $this->generateUuidV4();
                    $connection->executeStatement("
                        INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone, user_type, created_at)
                        VALUES (:user_id, :email, :password_hash, :first_name, :last_name, :phone, :user_type, :created_at)
                    ", [
                        'user_id' => $userId,
                        'email' => $email,
                        'password_hash' => password_hash(bin2hex(random_bytes(8)), PASSWORD_BCRYPT),
                        'first_name' => $firstName,
                        'last_name' => $lastName,
                        'phone' => $phone,
                        'user_type' => 'DONOR',
                        'created_at' => $now,
                    ]);

                    // Create donor record
                    $connection->executeStatement("
                        INSERT INTO donors (user_id, first_name, last_name, blood_type_id, city, last_donation_date, is_currently_eligible, total_donations, created_at)
                        VALUES (:user_id, :first_name, :last_name, :blood_type_id, :city, null, true, 0, :created_at)
                    ", [
                        'user_id' => $userId,
                        'first_name' => $firstName,
                        'last_name' => $lastName,
                        'blood_type_id' => $bloodType,
                        'city' => $city,
                        'created_at' => $now,
                    ]);

                    $this->addFlash('success', "New donor '$firstName $lastName' created!");
                }
            } else {
                $userId = $donor['user_id'];
            }
        }

        $id = $this->generateUuidV4();
        $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');

        $statusValue = strtoupper((string) $request->request->get('status', 'COMPLETED'));
        if (!in_array($statusValue, ['COMPLETED', 'FAILED', 'DEFERRED', 'CANCELLED'], true)) {
            $statusValue = 'COMPLETED';
        }

        $volumeCollectedRaw = trim((string) $request->request->get('volume_collected', ''));
        $volumeCollected = null;
        if ($volumeCollectedRaw !== '') {
            $normalized = str_replace(',', '.', preg_replace('/[^0-9\.\-]/', '', $volumeCollectedRaw));
            if (is_numeric($normalized)) {
                $volumeCollected = round((float) $normalized, 2);
                if ($volumeCollected < 0 || $volumeCollected >= 1000) {
                    $volumeCollected = null;
                }
            }
        }

        $connection->executeStatement("
            INSERT INTO donations
                (donation_id, user_id, blood_type_id, donation_event_id, hospital_id, 
                 donation_date, units_collected, volume_collected, status, 
                 screening_passed, medical_notes, created_at)
            VALUES
                (:donation_id, :user_id, :blood_type_id, :donation_event_id, :hospital_id,
                 :donation_date, :units_collected, :volume_collected, :status,
                 :screening_passed, :medical_notes, :created_at)
        ", [
            'donation_id' => $id,
            'user_id' => $userId,
            'blood_type_id' => trim((string) $request->request->get('blood_type_id')) ?: null,
            'donation_event_id' => $request->request->get('donation_event_id') ?: null,
            'hospital_id' => $request->request->get('hospital_id') ?: null,
            'donation_date' => $request->request->get('donation_date'),
            'units_collected' => (int) ($request->request->get('units_collected') ?? 0),
            'volume_collected' => $volumeCollected,
            'status' => $statusValue,
            'screening_passed' => $request->request->get('screening_passed') ? 1 : 0,
            'medical_notes' => $request->request->get('medical_notes') ?: null,
            'created_at' => $now,
        ]);

        return $this->redirectToRoute('donations_index');
    }

    // ─── EDIT ─────────────────────────────────────────────────────────────────
    #[Route('/dashboard/donations/{id}/edit', name: 'donations_edit', methods: ['POST'])]
    public function edit(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot edit donation records.');
            return $this->redirectToRoute('donations_index');
        }

        // Get donor - accept either UUID or name
        $donorInput = $request->request->get('donor_name');
        $userId = null;

        if ($donorInput) {
            // Try to find by UUID first
            $donor = $connection->fetchAssociative(
                "SELECT user_id FROM donors WHERE user_id::text = :id LIMIT 1",
                ['id' => $donorInput]
            );
            
            // If not found, try by name (first_name or last_name)
            if (!$donor) {
                $donor = $connection->fetchAssociative(
                    "SELECT user_id FROM donors WHERE CONCAT(first_name, ' ', last_name) ILIKE :name OR first_name ILIKE :name OR last_name ILIKE :name LIMIT 1",
                    ['name' => '%' . $donorInput . '%']
                );
            }

            // If still not found, create new donor
            if (!$donor) {
                $userId = $this->generateUuidV4();
                $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
                
                // Parse name into first and last
                $nameParts = explode(' ', trim($donorInput), 2);
                $firstName = $nameParts[0];
                $lastName = $nameParts[1] ?? '';
                $bloodType = trim((string) $request->request->get('blood_type_id')) ?: 'O+';

                // Create user record first
                $connection->executeStatement("
                    INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone, user_type, created_at)
                    VALUES (:user_id, :email, :password_hash, :first_name, :last_name, :phone, :user_type, :created_at)
                ", [
                    'user_id' => $userId,
                    'email' => strtolower(str_replace(' ', '.', $firstName . '.' . $lastName)) . '@bloodlink.local',
                    'password_hash' => password_hash(bin2hex(random_bytes(8)), PASSWORD_BCRYPT),
                    'first_name' => $firstName,
                    'last_name' => $lastName,
                    'phone' => null,
                    'user_type' => 'DONOR',
                    'created_at' => $now,
                ]);

                // Create donor record
                $connection->executeStatement("
                    INSERT INTO donors (user_id, first_name, last_name, blood_type_id, city, last_donation_date, is_currently_eligible, total_donations, created_at)
                    VALUES (:user_id, :first_name, :last_name, :blood_type_id, :city, null, true, 0, :created_at)
                ", [
                    'user_id' => $userId,
                    'first_name' => $firstName,
                    'last_name' => $lastName,
                    'blood_type_id' => $bloodType,
                    'city' => null,
                    'created_at' => $now,
                ]);

                $this->addFlash('success', "New donor '$firstName $lastName' created!");
            } else {
                $userId = $donor['user_id'];
            }
        }

        $statusValue = strtoupper((string) $request->request->get('status', 'COMPLETED'));
        if (!in_array($statusValue, ['COMPLETED', 'FAILED', 'DEFERRED', 'CANCELLED'], true)) {
            $statusValue = 'COMPLETED';
        }

        $volumeCollectedRaw = trim((string) $request->request->get('volume_collected', ''));
        $volumeCollected = null;
        if ($volumeCollectedRaw !== '') {
            $normalized = str_replace(',', '.', preg_replace('/[^0-9\.\-]/', '', $volumeCollectedRaw));
            if (is_numeric($normalized)) {
                $volumeCollected = round((float) $normalized, 2);
                if ($volumeCollected < 0 || $volumeCollected >= 1000) {
                    $volumeCollected = null;
                }
            }
        }

        $connection->executeStatement("
            UPDATE donations SET
                user_id                = :user_id,
                blood_type_id          = :blood_type_id,
                donation_event_id      = :donation_event_id,
                hospital_id            = :hospital_id,
                donation_date          = :donation_date,
                units_collected        = :units_collected,
                volume_collected       = :volume_collected,
                status                 = :status,
                screening_passed       = :screening_passed,
                medical_notes          = :medical_notes
            WHERE donation_id = :donation_id
        ", [
            'donation_id' => $id,
            'user_id' => $userId,
            'blood_type_id' => trim((string) $request->request->get('blood_type_id')) ?: null,
            'donation_event_id' => $request->request->get('donation_event_id') ?: null,
            'hospital_id' => $request->request->get('hospital_id') ?: null,
            'donation_date' => $request->request->get('donation_date'),
            'units_collected' => (int) ($request->request->get('units_collected') ?? 0),
            'volume_collected' => $volumeCollected,
            'status' => $statusValue,
            'screening_passed' => $request->request->get('screening_passed') ? 1 : 0,
            'medical_notes' => $request->request->get('medical_notes') ?: null,
        ]);

        return $this->redirectToRoute('donations_index');
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────
    #[Route('/dashboard/donations/{id}/delete', name: 'donations_delete', methods: ['POST'])]
    public function delete(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot delete donation records.');
            return $this->redirectToRoute('donations_index');
        }

        $connection->executeStatement(
            "DELETE FROM donations WHERE donation_id = :id",
            ['id' => $id]
        );

        return $this->redirectToRoute('donations_index');
    }

    private function generateUuidV4(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);

        $hex = bin2hex($bytes);

        return sprintf(
            '%s-%s-%s-%s-%s',
            substr($hex, 0, 8),
            substr($hex, 8, 4),
            substr($hex, 12, 4),
            substr($hex, 16, 4),
            substr($hex, 20, 12),
        );
    }
}
