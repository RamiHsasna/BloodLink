<?php

namespace App\Controller;

use Doctrine\DBAL\Connection;
use Doctrine\DBAL\Exception\UniqueConstraintViolationException;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
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
        $isAjax = $request->isXmlHttpRequest() || str_contains((string) $request->headers->get('Accept', ''), 'application/json');
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
            (
                SELECT COUNT(*)
                FROM donation_event_donor ded
                WHERE ded.event_id = de.event_id
            ) AS participants_count,
            COUNT(d.donation_id) AS total_donations,
            COALESCE(SUM(d.volume_collected), 0) AS total_litres_collected
         FROM donation_events de
            LEFT JOIN hospital h ON h.hospital_id = de.hospital_id
            LEFT JOIN donations d ON d.donation_event_id = de.event_id
            WHERE 1=1
        ";
        $params = [];

        // Hospital staff can only see donation events for their hospital
        if ($userType === 'HOSPITAL_STAFF') {
            $hospitalId = $sessionUser['hospital_id'] ?? null;
            if (!$hospitalId) {
                $this->addFlash('error', 'Your hospital assignment is missing.');
                return $this->redirectToRoute('dashboard_donor_home');
            }
            $sql .= " AND de.hospital_id::text = :hospital_id";
            $params['hospital_id'] = $hospitalId;
        }

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

        $statsSql = " 
            SELECT
                COUNT(DISTINCT de.event_id)                                         AS total_events,
                COUNT(DISTINCT CASE WHEN de.status = 'PLANNED'   THEN de.event_id END) AS planned,
                COUNT(DISTINCT CASE WHEN de.status = 'ACTIVE'    THEN de.event_id END) AS active,
                COUNT(DISTINCT CASE WHEN de.status = 'COMPLETED' THEN de.event_id END) AS completed,
                COUNT(DISTINCT CASE WHEN de.status = 'CANCELLED' THEN de.event_id END) AS cancelled,
                COALESCE(SUM(d.volume_collected), 0)                                AS total_litres
            FROM donation_events de
            LEFT JOIN donations d ON d.donation_event_id = de.event_id
            WHERE 1=1
        ";
        
        $statsParams = [];
        if ($userType === 'HOSPITAL_STAFF') {
            $statsSql .= " AND de.hospital_id::text = :hospital_id";
            $statsParams['hospital_id'] = $hospitalId;
        }
        
        $stats = $connection->fetchAssociative($statsSql, $statsParams);

        return $this->render('dashboard/donation_events.html.twig', [
            'session_user'  => $sessionUser,
            'events'        => $events,
            'stats'         => $stats,
            'search'        => $search,
            'status_filter' => $status,
            'user_type'     => $userType,
        ]);
    }

    #[Route('/dashboard/donation-events/{id}/participants', name: 'donation_events_participants', methods: ['GET'])]
    public function participants(string $id, Request $request, Connection $connection): JsonResponse
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            return $this->json(['error' => 'Forbidden'], 403);
        }

        $event = $connection->fetchAssociative(
            'SELECT de.event_id, de.name, de.hospital_id FROM donation_events de WHERE de.event_id = :id LIMIT 1',
            ['id' => $id]
        );

        if (!$event) {
            return $this->json(['error' => 'Event not found'], 404);
        }

        $participants = $connection->fetchAllAssociative(
            "
            SELECT
                ded.id,
                ded.user_id,
                ded.created_at AS joined_at,
                u.first_name,
                u.last_name,
                u.email,
                u.phone,
                COALESCE(dr.blood_type_id, 'N/A') AS blood_type,
                er.status AS eligibility_status,
                er.qr_token,
                er.valid_until,
                er.doctor_signed_at,
                de.blood_type_cache,
                de.is_currently_eligible,
                de.days_until_eligible,
                de.last_calculated_at,
                de.eligibility_details,
                COALESCE(dr.city, '') AS donor_city
            FROM donation_event_donor ded
            INNER JOIN users u ON u.user_id = ded.user_id
            LEFT JOIN donors dr ON dr.user_id = ded.user_id
            LEFT JOIN eligibility_report er ON er.user_id = ded.user_id AND er.event_id = ded.event_id
            LEFT JOIN donor_eligibility de ON de.user_id = ded.user_id
            WHERE ded.event_id = :event_id
            ORDER BY ded.created_at DESC
            ",
            ['event_id' => $id]
        );

        return $this->json([
            'event' => [
                'id' => $event['event_id'],
                'name' => $event['name'],
            ],
            'count' => count($participants),
            'participants' => $participants,
        ]);
    }

    #[Route('/dashboard/eligibility-report/{eventId}/{userId}', name: 'eligibility_report_detail', methods: ['GET'])]
    public function eligibilityReportDetail(string $eventId, string $userId, Request $request, Connection $connection): JsonResponse
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            return $this->json(['error' => 'Forbidden'], 403);
        }

        $reportRow = $connection->fetchAssociative(
            'SELECT er.status, er.qr_token, er.valid_until, er.questionnaire, er.doctor_notes, er.disqualifying_reasons, er.doctor_signed_at, er.created_at, er.updated_at, de.is_currently_eligible, de.days_until_eligible, de.last_calculated_at, de.blood_type_cache, de.eligibility_details, u.first_name, u.last_name, u.email FROM eligibility_report er INNER JOIN donor_eligibility de ON de.user_id = er.user_id INNER JOIN users u ON u.user_id = er.user_id WHERE er.event_id = :event_id AND er.user_id = :user_id LIMIT 1',
            ['event_id' => $eventId, 'user_id' => $userId]
        );

        if (!$reportRow) {
            return $this->json([
                'error' => 'No eligibility data found for this donor',
                'debug' => [
                    'event_id' => $eventId,
                    'user_id' => $userId,
                ],
            ], 404);
        }

        $details = (string) ($reportRow['eligibility_details'] ?? '');
        $parsed = $this->parseEligibilityDetails($details);
        $reasons = $this->extractAssessmentReasons($details);

        $questionnaire = $reportRow['questionnaire'];
        if (is_string($questionnaire) && trim($questionnaire) !== '') {
            $decodedQuestionnaire = json_decode($questionnaire, true);
            $questionnaire = json_last_error() === JSON_ERROR_NONE ? $decodedQuestionnaire : $questionnaire;
        }

        $disqualifyingReasons = $reportRow['disqualifying_reasons'];
        if (is_string($disqualifyingReasons) && trim($disqualifyingReasons) !== '') {
            $decodedReasons = json_decode($disqualifyingReasons, true);
            $disqualifyingReasons = json_last_error() === JSON_ERROR_NONE ? $decodedReasons : $disqualifyingReasons;
        }

        $status = $reportRow['status'] ?? (($reportRow['is_currently_eligible'] ?? false) ? 'ELIGIBLE' : 'PENDING');

        return $this->json([
            'id' => $userId,
            'event_id' => $eventId,
            'first_name' => $reportRow['first_name'] ?? '',
            'last_name' => $reportRow['last_name'] ?? '',
            'email' => $reportRow['email'] ?? '',
            'blood_type' => $reportRow['blood_type_cache'] ?? 'N/A',
            'status' => $status,
            'is_currently_eligible' => (bool) ($reportRow['is_currently_eligible'] ?? false),
            'days_until_eligible' => $reportRow['days_until_eligible'] ?? null,
            'last_calculated_at' => $reportRow['last_calculated_at'] ?? null,
            'eligibility_details' => $details,
            'questionnaire' => !empty($questionnaire) ? $questionnaire : (!empty($parsed) ? $parsed : null),
            'doctor_notes' => !empty($reportRow['doctor_notes']) ? $reportRow['doctor_notes'] : ($reasons !== '' ? $reasons : null),
            'disqualifying_reasons' => $disqualifyingReasons,
            'doctor_signed_at' => $reportRow['doctor_signed_at'] ?? null,
            'valid_until' => $reportRow['valid_until'] ?? null,
            'qr_token' => $reportRow['qr_token'] ?? null,
            'created_at' => $reportRow['created_at'] ?? null,
            'updated_at' => $reportRow['updated_at'] ?? null,
        ]);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────
    #[Route('/dashboard/donation-events/create', name: 'donation_events_create', methods: ['POST'])]
    public function create(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $isAjax = $request->isXmlHttpRequest() || str_contains((string) $request->headers->get('Accept', ''), 'application/json');

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

    #[Route('/dashboard/donation-events/{id}/participate', name: 'donation_events_participate', methods: ['POST'])]
    public function participate(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $isAjax = $request->isXmlHttpRequest() || str_contains((string) $request->headers->get('Accept', ''), 'application/json');

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType !== 'DONOR') {
            $this->addFlash('error', 'Only donors can participate in donation events.');
            return $this->redirectToRoute('donation_events_index');
        }

        $userId = (string) ($sessionUser['id'] ?? '');
        if ($userId === '') {
            $this->addFlash('error', 'Missing user session data. Please sign in again.');
            return $this->redirectToRoute('auth_index');
        }

        // Load event, donor blood type and eligibility to validate participation rules
        $eventRow = $connection->fetchAssociative(
            'SELECT event_id, name, target_blood_types, status FROM donation_events WHERE event_id = :id LIMIT 1',
            ['id' => $id]
        );
        if (!$eventRow) {
            if ($isAjax) return $this->json(['ok' => false, 'message' => 'Event not found.'], 404);
            $this->addFlash('error', 'Event not found.');
            return $this->redirectToRoute('dashboard_donor_events');
        }

        // Check event status
        if (!in_array($eventRow['status'], ['PLANNED', 'ACTIVE'])) {
            $msg = sprintf('Registration is closed. This event is currently %s.', strtolower($eventRow['status']));
            if ($isAjax) return $this->json(['ok' => false, 'message' => $msg], 400);
            $this->addFlash('error', $msg);
            return $this->redirectToRoute('dashboard_donor_events');
        }

        $donorRow = $connection->fetchAssociative(
            'SELECT blood_type_id FROM donors WHERE user_id = :user_id LIMIT 1',
            ['user_id' => $userId]
        );
        $donorBloodType = trim((string) ($donorRow['blood_type_id'] ?? ''));

        // Validate blood type match
        $targetTypesRaw = trim((string) ($eventRow['target_blood_types'] ?? ''));
        if ($targetTypesRaw !== '' && strtolower($targetTypesRaw) !== 'all' && strtolower($targetTypesRaw) !== 'any') {
            $targets = array_map('trim', explode(',', $targetTypesRaw));
            $normalizedTargets = array_map(function ($t) { return strtoupper($t); }, $targets);
            $normalizedDonor = strtoupper($donorBloodType);

            if ($normalizedDonor === '' || $normalizedDonor === 'N/A') {
                $msg = 'Your blood type is not on file. Please update your profile to participate.';
                if ($isAjax) return $this->json(['ok' => false, 'message' => $msg], 400);
                $this->addFlash('error', $msg);
                return $this->redirectToRoute('dashboard_donor_events');
            }

            if (!in_array($normalizedDonor, $normalizedTargets, true)) {
                $msg = sprintf('Event requires %s (your type: %s).', $targetTypesRaw, $donorBloodType);
                if ($isAjax) return $this->json(['ok' => false, 'message' => $msg], 400);
                $this->addFlash('error', $msg);
                return $this->redirectToRoute('dashboard_donor_events');
            }
        }

        $eligibilityData = $connection->fetchAssociative(
            'SELECT is_currently_eligible, days_until_eligible, eligibility_details FROM donor_eligibility WHERE user_id = :user_id LIMIT 1',
            ['user_id' => $userId]
        );

        if (!$eligibilityData) {
            $msg = 'Please complete the eligibility check on your dashboard before joining events.';
            if ($isAjax) return $this->json(['ok' => false, 'message' => $msg], 400);
            $this->addFlash('error', $msg);
            return $this->redirectToRoute('dashboard_donor_eligibility');
        }

        // Validate donor eligibility status
        $isCurrentlyEligible = (bool) ($eligibilityData['is_currently_eligible'] ?? false);
        if (!$isCurrentlyEligible) {
            $reasons = $this->extractAssessmentReasons($eligibilityData['eligibility_details'] ?? '');
            $daysUntil = (int) ($eligibilityData['days_until_eligible'] ?? 0);
            
            if ($daysUntil > 0) {
                $msg = sprintf('Temporarily not eligible for %d day(s).', $daysUntil);
            } else {
                $msg = 'You are currently not eligible to donate.';
            }

            if ($reasons !== '') {
                $reasonList = explode("\n", $reasons);
                $briefReason = trim($reasonList[0]);
                $msg .= ' Reason: ' . $briefReason;
            }

            if ($isAjax) return $this->json(['ok' => false, 'message' => $msg], 400);
            $this->addFlash('error', $msg);
            return $this->redirectToRoute('dashboard_donor_events');
        }

        $isEligible = 'ELIGIBLE';

        $alreadyJoined = $connection->fetchOne(
            'SELECT 1 FROM donation_event_donor WHERE event_id = :event_id AND user_id = :user_id LIMIT 1',
            ['event_id' => $id, 'user_id' => $userId]
        );

        if ($alreadyJoined) {
            if ($isAjax) return $this->json(['ok' => false, 'message' => 'You have already joined this event.'], 409);
            $this->addFlash('error', 'You have already joined this event.');
            return $this->redirectToRoute('dashboard_donor_events');
        }

        $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
        $connection->beginTransaction();
        try {
            $participationId = $this->generateUuidV4();
            $reportId = $this->generateUuidV4();
            $qrToken = bin2hex(random_bytes(16));

            $questionnaire = null;
            $doctorNotes = null;
            if ($eligibilityData && !empty($eligibilityData['eligibility_details'])) {
                $parsed = $this->parseEligibilityDetails($eligibilityData['eligibility_details']);
                $questionnaire = !empty($parsed) ? json_encode($parsed) : null;
                $reasons = $this->extractAssessmentReasons($eligibilityData['eligibility_details']);
                $doctorNotes = !empty($reasons) ? $reasons : null;
            }

            $connection->executeStatement(
                'INSERT INTO donation_event_donor (id, event_id, user_id, created_at) VALUES (:id, :event_id, :user_id, :created_at)',
                [
                    'id' => $participationId,
                    'event_id' => $id,
                    'user_id' => $userId,
                    'created_at' => $now,
                ]
            );

            $connection->executeStatement(
                'INSERT INTO eligibility_report (id, user_id, event_id, status, qr_token, questionnaire, doctor_notes, created_at, updated_at) VALUES (:id, :user_id, :event_id, :status, :qr_token, :questionnaire, :doctor_notes, :created_at, :updated_at)',
                [
                    'id' => $reportId,
                    'user_id' => $userId,
                    'event_id' => $id,
                    'status' => $isEligible,
                    'qr_token' => $qrToken,
                    'questionnaire' => $questionnaire,
                    'doctor_notes' => $doctorNotes,
                    'created_at' => $now,
                    'updated_at' => $now,
                ]
            );

            $connection->commit();
            if ($isAjax) return $this->json(['ok' => true, 'message' => 'Successfully joined the event!']);
            $this->addFlash('success', 'You have successfully joined the event!');
        } catch (UniqueConstraintViolationException) {
            $connection->rollBack();
            if ($isAjax) return $this->json(['ok' => false, 'message' => 'You have already joined this event.'], 409);
            $this->addFlash('error', 'You have already joined this event.');
        } catch (\Throwable $e) {
            $connection->rollBack();
            
            // Log the detailed error for debugging
            try {
                $logDir = __DIR__ . '/../../var/log';
                $errorLine = sprintf(
                    "%s | participate error | eventId=%s | userId=%s | message=%s | trace=%s\n",
                    (new \DateTimeImmutable())->format('Y-m-d H:i:s'),
                    $id,
                    $userId,
                    $e->getMessage(),
                    $e->getTraceAsString()
                );
                @file_put_contents($logDir . '/participate_debug.log', $errorLine, FILE_APPEND | LOCK_EX);
            } catch (\Throwable) {}

            if ($isAjax) return $this->json(['ok' => false, 'message' => 'Could not complete your registration. ' . $e->getMessage()], 500);
            $this->addFlash('error', 'Could not complete your registration: ' . $e->getMessage());
        }

        return $this->redirectToRoute('dashboard_donor_events');
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

    private function parseEligibilityDetails(?string $details): array
    {
        if (!$details || trim($details) === '') {
            return [];
        }

        $parsed = [];
        $lines = preg_split('/\R/u', $details) ?: [];
        foreach ($lines as $line) {
            $line = trim($line);
            if ($line === '' || str_starts_with($line, '===') || str_starts_with($line, '- ')) {
                continue;
            }

            if (!str_contains($line, ':')) {
                continue;
            }

            [$key, $value] = explode(':', $line, 2);
            $key = trim($key);
            $value = trim($value);
            if ($key !== '') {
                $parsed[$key] = $value;
            }
        }

        return $parsed;
    }

    private function extractAssessmentReasons(?string $details): string
    {
        if (!$details || trim($details) === '') {
            return '';
        }

        $lines = preg_split('/\R/u', $details) ?: [];
        $reasons = [];
        $inReasons = false;

        foreach ($lines as $line) {
            $line = trim($line);
            if ($line === '=== ASSESSMENT REASONS ===' || str_contains($line, 'ASSESSMENT REASONS')) {
                $inReasons = true;
                continue;
            }

            if (!$inReasons) {
                continue;
            }

            if ($line === '' || str_starts_with($line, '===')) {
                break;
            }

            if (str_starts_with($line, '- ')) {
                $reasons[] = substr($line, 2);
            } elseif ($line !== '') {
                $reasons[] = $line;
            }
        }

        return implode("\n", $reasons);
    }
}

