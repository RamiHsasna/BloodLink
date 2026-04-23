<?php

namespace App\Controller;

use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class DonationEventController extends AbstractController
{
    // ─── LIST ────────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events', name: 'donation_events_index', methods: ['GET'])]
    public function index(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot manage donation events.');
            return $this->redirectToRoute('dashboard_users');
        }

        $search = trim((string) $request->query->get('q', ''));
        $status = trim((string) $request->query->get('status', ''));

        // Base query
        $sql = "
            SELECT de.*,
                   h.name AS hospital_name,
                   COUNT(d.donation_id) AS total_donations,
                   COALESCE(SUM(d.units_collected), 0) AS total_units_collected
            FROM donation_events de
            LEFT JOIN hospitals h ON h.hospital_id = de.hospital_id
            LEFT JOIN donations d ON d.donation_event_id = de.event_id
            WHERE 1=1
        ";
        $params = [];

        // Search by name, description, or location
        if ($search !== '') {
            $sql .= " AND (de.name ILIKE :q 
                      OR de.description ILIKE :q 
                      OR de.location ILIKE :q)";
            $params['q'] = '%' . $search . '%';
        }

        // Filter by status (database status: PLANNED, ACTIVE, COMPLETED, CANCELLED)
        $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
        if ($status !== '') {
            $sql .= " AND de.status = :status";
            $params['status'] = strtoupper($status);
        }

        $sql .= " GROUP BY de.event_id, h.name ORDER BY de.start_date DESC";

        $events = $connection->fetchAllAssociative($sql, $params);

        // Stats query
        $statsSql = "
            SELECT
                COUNT(DISTINCT de.event_id) AS total_events,
                COUNT(DISTINCT CASE WHEN de.status = 'PLANNED' THEN de.event_id END) AS planned,
                COUNT(DISTINCT CASE WHEN de.status = 'ACTIVE' THEN de.event_id END) AS active,
                COUNT(DISTINCT CASE WHEN de.status = 'COMPLETED' THEN de.event_id END) AS completed,
                COUNT(DISTINCT CASE WHEN de.status = 'CANCELLED' THEN de.event_id END) AS cancelled,
                COALESCE(SUM(d.units_collected), 0) AS total_units
            FROM donation_events de
            LEFT JOIN donations d ON d.donation_event_id = de.event_id
        ";

        $stats = $connection->fetchAssociative($statsSql);

        return $this->render('dashboard/donation_events.html.twig', [
            'session_user' => $sessionUser,
            'events' => $events,
            'stats' => $stats,
            'search' => $search,
            'status_filter' => $status,
            'user_type' => $userType,
        ]);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events/create', name: 'donation_events_create', methods: ['POST'])]
    public function create(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot create donation events.');
            return $this->redirectToRoute('donation_events_index');
        }

        $id = $this->generateUuidV4();
        $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');

        // Handle hospital - accept name and auto-create if needed
        $hospitalInput = $request->request->get('hospital_name');
        $hospitalId = null;

        if ($hospitalInput) {
            // Try to find by ID first
            $hospital = $connection->fetchAssociative(
                "SELECT hospital_id FROM hospital WHERE hospital_id::text = :id LIMIT 1",
                ['id' => $hospitalInput]
            );
            
            // If not found, try by name
            if (!$hospital) {
                $hospital = $connection->fetchAssociative(
                    "SELECT hospital_id FROM hospital WHERE name ILIKE :name LIMIT 1",
                    ['name' => '%' . $hospitalInput . '%']
                );
            }

            // If still not found, create new hospital
            if (!$hospital) {
                $hospitalId = $this->generateUuidV4();
                
                $connection->executeStatement("
                    INSERT INTO hospital (hospital_id, name, address, city, created_at)
                    VALUES (:hospital_id, :name, :address, :city, :created_at)
                ", [
                    'hospital_id' => $hospitalId,
                    'name' => $hospitalInput,
                    'address' => '',
                    'city' => '',
                    'created_at' => $now,
                ]);
                
                $this->addFlash('success', "New hospital '$hospitalInput' created!");
            } else {
                $hospitalId = $hospital['hospital_id'];
            }
        }

        $connection->executeStatement("
            INSERT INTO donation_events
                (event_id, name, description, start_date, end_date, location,
                 latitude, longitude, target_blood_types, target_collection_units, 
                 actual_collection_units, hospital_id, status, created_at)
            VALUES
                (:event_id, :name, :description, :start_date, :end_date, :location,
                 :latitude, :longitude, :target_blood_types, :target_collection_units,
                 :actual_collection_units, :hospital_id, :status, :created_at)
        ", [
            'event_id' => $id,
            'name' => $request->request->get('name'),
            'description' => $request->request->get('description') ?: null,
            'start_date' => $request->request->get('start_date'),
            'end_date' => $request->request->get('end_date'),
            'location' => $request->request->get('location') ?: null,
            'latitude' => $request->request->get('latitude') ?: null,
            'longitude' => $request->request->get('longitude') ?: null,
            'target_blood_types' => $request->request->get('target_blood_types') ?: null,
            'target_collection_units' => $request->request->get('target_collection_units') ?: null,
            'actual_collection_units' => 0,
            'hospital_id' => $hospitalId,
            'status' => 'PLANNED',
            'created_at' => $now,
        ]);

        return $this->redirectToRoute('donation_events_index');
    }

    // ─── EDIT ─────────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events/{id}/edit', name: 'donation_events_edit', methods: ['POST'])]
    public function edit(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot edit donation events.');
            return $this->redirectToRoute('donation_events_index');
        }

        // Handle hospital - accept name and auto-create if needed
        $hospitalInput = $request->request->get('hospital_name');
        $hospitalId = null;

        if ($hospitalInput) {
            // Try to find by ID first
            $hospital = $connection->fetchAssociative(
                "SELECT hospital_id FROM hospital WHERE hospital_id::text = :id LIMIT 1",
                ['id' => $hospitalInput]
            );
            
            // If not found, try by name
            if (!$hospital) {
                $hospital = $connection->fetchAssociative(
                    "SELECT hospital_id FROM hospital WHERE name ILIKE :name LIMIT 1",
                    ['name' => '%' . $hospitalInput . '%']
                );
            }

            // If still not found, create new hospital
            if (!$hospital) {
                $hospitalId = $this->generateUuidV4();
                $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
                
                $connection->executeStatement("
                    INSERT INTO hospital (hospital_id, name, address, city, created_at)
                    VALUES (:hospital_id, :name, :address, :city, :created_at)
                ", [
                    'hospital_id' => $hospitalId,
                    'name' => $hospitalInput,
                    'address' => '',
                    'city' => '',
                    'created_at' => $now,
                ]);
                
                $this->addFlash('success', "New hospital '$hospitalInput' created!");
            } else {
                $hospitalId = $hospital['hospital_id'];
            }
        }

        $statusValue = $request->request->get('status', 'PLANNED');
        if (!in_array($statusValue, ['PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'])) {
            $statusValue = 'PLANNED';
        }

        $connection->executeStatement("
            UPDATE donation_events SET
                name                       = :name,
                description                = :description,
                start_date                 = :start_date,
                end_date                   = :end_date,
                location                   = :location,
                latitude                   = :latitude,
                longitude                  = :longitude,
                target_blood_types         = :target_blood_types,
                target_collection_units    = :target_collection_units,
                hospital_id                = :hospital_id,
                status                     = :status
            WHERE event_id = :event_id
        ", [
            'event_id' => $id,
            'name' => $request->request->get('name'),
            'description' => $request->request->get('description') ?: null,
            'start_date' => $request->request->get('start_date'),
            'end_date' => $request->request->get('end_date'),
            'location' => $request->request->get('location') ?: null,
            'latitude' => $request->request->get('latitude') ?: null,
            'longitude' => $request->request->get('longitude') ?: null,
            'target_blood_types' => $request->request->get('target_blood_types') ?: null,
            'target_collection_units' => $request->request->get('target_collection_units') ?: null,
            'hospital_id' => $hospitalId,
            'status' => $statusValue,
        ]);

        return $this->redirectToRoute('donation_events_index');
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events/{id}/delete', name: 'donation_events_delete', methods: ['POST'])]
    public function delete(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot delete donation events.');
            return $this->redirectToRoute('donation_events_index');
        }

        $connection->executeStatement(
            "DELETE FROM donation_events WHERE event_id = :id",
            ['id' => $id]
        );

        return $this->redirectToRoute('donation_events_index');
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
