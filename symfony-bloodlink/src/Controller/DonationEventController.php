<?php

namespace App\Controller;

use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class DonationEventController extends AbstractController
{
    // ─── AUTO STATUS SYNC ────────────────────────────────────────────────────
    private function syncStatuses(Connection $connection): void
    {
        $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');

        // PLANNED → ACTIVE when start_date has arrived
        $connection->executeStatement("
            UPDATE donation_events
            SET status = 'ACTIVE', updated_at = :now
            WHERE status = 'PLANNED'
              AND start_date <= :now
              AND end_date >= :now
        ", ['now' => $now]);

        // ACTIVE → COMPLETED when end_date has passed
        $connection->executeStatement("
            UPDATE donation_events
            SET status = 'COMPLETED', updated_at = :now
            WHERE status = 'ACTIVE'
              AND end_date < :now
        ", ['now' => $now]);

        // PLANNED → COMPLETED if missed active window
        $connection->executeStatement("
            UPDATE donation_events
            SET status = 'COMPLETED', updated_at = :now
            WHERE status = 'PLANNED'
              AND end_date < :now
        ", ['now' => $now]);
    }

    // ─── LIST ────────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events', name: 'donation_events_index', methods: ['GET'])]
    #[Route('/dashboard/donation-events', name: 'dashboard_events_index', methods: ['GET'])]
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

        // Auto-sync statuses on every page load
        $this->syncStatuses($connection);

        $search = trim((string) $request->query->get('q', ''));
        $status = trim((string) $request->query->get('status', ''));

        $sql = "
         SELECT de.*,
            de.cancellation_reason,
            h.name AS hospital_name,
            COUNT(d.donation_id) AS total_donations,
            COALESCE(SUM(d.volume_collected), 0) AS total_litres_collected
         FROM donation_events de
            LEFT JOIN hospital h ON h.hospital_id = de.hospital_id
            LEFT JOIN donations d ON d.donation_event_id = de.event_id
            WHERE 1=1
        ";
        $params = [];

        if ($search !== '') {
            $sql .= " AND (de.name ILIKE :q OR de.description ILIKE :q OR de.location ILIKE :q)";
            $params['q'] = '%' . $search . '%';
        }

        if ($status !== '') {
            $sql .= " AND de.status = :status";
            $params['status'] = strtoupper($status);
        }

        $sql .= " GROUP BY de.event_id, h.name ORDER BY de.start_date DESC";

        $events = $connection->fetchAllAssociative($sql, $params);

        $stats = $connection->fetchAssociative("
            SELECT
                COUNT(DISTINCT de.event_id)                                         AS total_events,
                COUNT(DISTINCT CASE WHEN de.status = 'PLANNED'   THEN de.event_id END) AS planned,
                COUNT(DISTINCT CASE WHEN de.status = 'ACTIVE'    THEN de.event_id END) AS active,
                COUNT(DISTINCT CASE WHEN de.status = 'COMPLETED' THEN de.event_id END) AS completed,
                COUNT(DISTINCT CASE WHEN de.status = 'CANCELLED' THEN de.event_id END) AS cancelled,
                COALESCE(SUM(d.volume_collected), 0)                                AS total_litres
            FROM donation_events de
            LEFT JOIN donations d ON d.donation_event_id = de.event_id
        ");

        return $this->render('dashboard/donation_events.html.twig', [
            'session_user'  => $sessionUser,
            'events'        => $events,
            'stats'         => $stats,
            'search'        => $search,
            'status_filter' => $status,
            'user_type'     => $userType,
        ]);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events/create', name: 'donation_events_create', methods: ['POST'])]
    public function create(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot create donation events.');
            return $this->redirectToRoute('donation_events_index');
        }

        $startDate = $request->request->get('start_date');
        $endDate   = $request->request->get('end_date');
        $now       = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
        $today     = (new \DateTimeImmutable())->format('Y-m-d');

        // Start date cannot be in the past
        if ($startDate && substr($startDate, 0, 10) < $today) {
            $this->addFlash('error', 'Start date cannot be in the past.');
            return $this->redirectToRoute('donation_events_index');
        }

        // End date must be at least 12 hours after start date
            if ($startDate && $endDate) {
               $start = new \DateTimeImmutable($startDate);
               $end   = new \DateTimeImmutable($endDate);
               $diff  = $end->getTimestamp() - $start->getTimestamp();
               if ($diff < 43200) { // 43200 seconds = 12 hours
                $this->addFlash('error', 'The event must last at least 12 hours. Please adjust the end date and time.');
                return $this->redirectToRoute('donation_events_index');
            }
        }

        $id         = $this->generateUuidV4();
        $hospitalId = $this->resolveHospital($connection, $request->request->get('hospital_name'), $now);

        $connection->executeStatement("
            INSERT INTO donation_events
                (event_id, name, description, start_date, end_date, location,
                 target_blood_types, target_collection_units,
                 actual_collection_units, hospital_id, status, created_at, updated_at)
            VALUES
                (:event_id, :name, :description, :start_date, :end_date, :location,
                 :target_blood_types, :target_collection_units,
                 0, :hospital_id, 'PLANNED', :now, :now)
        ", [
            'event_id'                => $id,
            'name'                    => $request->request->get('name'),
            'description'             => $request->request->get('description') ?: null,
            'start_date'              => $startDate,
            'end_date'                => $endDate,
            'location'                => $request->request->get('location') ?: null,
            'target_blood_types'      => $request->request->get('target_blood_types') ?: null,
            'target_collection_units' => $request->request->get('target_collection_units') ? (int) $request->request->get('target_collection_units') : null,
            'hospital_id'             => $hospitalId,
            'now'                     => $now,
        ]);

        $this->addFlash('success', 'Donation event created successfully.');
        return $this->redirectToRoute('donation_events_index');
    }

    // ─── EDIT ─────────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events/{id}/edit', name: 'donation_events_edit', methods: ['POST'])]
    public function edit(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot edit donation events.');
            return $this->redirectToRoute('donation_events_index');
        }

        $startDate = $request->request->get('start_date');
        $endDate   = $request->request->get('end_date');

        // End date must be >= start date
        if ($startDate && $endDate && $endDate < $startDate) {
            $this->addFlash('error', 'End date must be on or after the start date.');
            return $this->redirectToRoute('donation_events_index');
        }

        $now        = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
        $hospitalId = $this->resolveHospital($connection, $request->request->get('hospital_name'), $now);

        $status = $request->request->get('status', 'PLANNED');
        if (!in_array($status, ['PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'])) {
            $status = 'PLANNED';
        }

        $connection->executeStatement("
            UPDATE donation_events SET
                name                     = :name,
                description              = :description,
                start_date               = :start_date,
                end_date                 = :end_date,
                location                 = :location,
                target_blood_types       = :target_blood_types,
                target_collection_units  = :target_collection_units,
                hospital_id              = :hospital_id,
                status                   = :status,
                updated_at               = :now
            WHERE event_id = :event_id
        ", [
            'event_id'                => $id,
            'name'                    => $request->request->get('name'),
            'description'             => $request->request->get('description') ?: null,
            'start_date'              => $startDate,
            'end_date'                => $endDate,
            'location'                => $request->request->get('location') ?: null,
            'target_blood_types'      => $request->request->get('target_blood_types') ?: null,
            'target_collection_units' => $request->request->get('target_collection_units') ?: null,
            'hospital_id'             => $hospitalId,
            'status'                  => $status,
            'now'                     => $now,
        ]);

        $this->addFlash('success', 'Donation event updated successfully.');
        return $this->redirectToRoute('donation_events_index');
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events/{id}/delete', name: 'donation_events_delete', methods: ['POST'])]
    public function delete(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Donors cannot delete donation events.');
            return $this->redirectToRoute('donation_events_index');
        }

        $connection->executeStatement(
            "DELETE FROM donation_events WHERE event_id = :id",
            ['id' => $id]
        );

        $this->addFlash('success', 'Event deleted successfully.');
        return $this->redirectToRoute('donation_events_index');
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    private function resolveHospital(Connection $connection, ?string $input, string $now): ?string
    {
        if (!$input) return null;

        $hospital = $connection->fetchAssociative(
            "SELECT hospital_id FROM hospital WHERE name ILIKE :name LIMIT 1",
            ['name' => $input]
        );

        if ($hospital) return $hospital['hospital_id'];

        $hospitalId = $this->generateUuidV4();
        $connection->executeStatement("
            INSERT INTO hospital (hospital_id, name, address, city, created_at, updated_at)
            VALUES (:id, :name, '', '', :now, :now)
        ", ['id' => $hospitalId, 'name' => $input, 'now' => $now]);

        return $hospitalId;
    }
     #[Route('/dashboard/donation-events/{id}/cancel', name: 'donation_events_cancel', methods: ['POST'])]
     public function cancel(string $id, Request $request, Connection $connection): Response
    {
       $sessionUser = $request->getSession()->get('auth_user');
       if (!$sessionUser) return $this->redirectToRoute('auth_index');

       $reason = trim((string) $request->request->get('cancellation_reason', ''));
       if ($reason === '') {
        $this->addFlash('error', 'Cancellation reason is required.');
        return $this->redirectToRoute('donation_events_index');
     }

     $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');

     $connection->executeStatement("
        UPDATE donation_events
        SET status = 'CANCELLED',
            cancellation_reason = :reason,
            updated_at = :now
        WHERE event_id = :id
     ", ['id' => $id, 'reason' => $reason, 'now' => $now]);

    $this->addFlash('success', 'Event cancelled successfully.');
    return $this->redirectToRoute('donation_events_index');
 }
    
    private function generateUuidV4(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        $hex = bin2hex($bytes);
        return sprintf('%s-%s-%s-%s-%s',
            substr($hex, 0, 8), substr($hex, 8, 4),
            substr($hex, 12, 4), substr($hex, 16, 4),
            substr($hex, 20, 12)
        );
    }
}
