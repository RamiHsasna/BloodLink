<?php

namespace App\Controller;

use Doctrine\DBAL\Connection;
use Dompdf\Dompdf;
use Endroid\QrCode\QrCode;
use Endroid\QrCode\Writer\PngWriter;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class DonationsController extends AbstractController
{
    #[Route('/dashboard/donations', name: 'donations_index', methods: ['GET'])]
    public function index(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === 'DONOR') {
            $this->addFlash('error', 'Access denied.');
            return $this->redirectToRoute('dashboard_donor_home');
        }

        $search      = trim((string) $request->query->get('q', ''));
        $status      = trim((string) $request->query->get('status', ''));
        $eventId     = trim((string) $request->query->get('event_id', ''));
        $bloodFilter = trim((string) $request->query->get('blood_type', ''));

        $sql = "
            SELECT d.*,
                   u.first_name                        AS donor_first_name,
                   u.last_name                         AS donor_last_name,
                   u.first_name || ' ' || u.last_name AS donor_name,
                   dr.total_donations                  AS donor_total_donations,
                   dr.blood_type_id                    AS donor_blood_type,
                   h.name                              AS hospital_name,
                   de.name                             AS event_name,
                   d.blood_type_id                     AS blood_code,
                   (SELECT COUNT(*) FROM donations d2
                    WHERE d2.user_id = d.user_id
                    AND d2.donation_date < d.donation_date) AS prior_donations
            FROM donations d
            LEFT JOIN donors dr ON dr.user_id = d.user_id
            LEFT JOIN users u   ON u.user_id  = dr.user_id
            LEFT JOIN hospital h ON h.hospital_id = d.hospital_id
            LEFT JOIN donation_events de ON de.event_id = d.donation_event_id
            WHERE 1=1
        ";
        $params = [];

        if ($search !== '') {
            $sql .= " AND (u.first_name ILIKE :q OR u.last_name ILIKE :q OR h.name ILIKE :q)";
            $params['q'] = '%' . $search . '%';
        }
        if ($status !== '') {
            $sql .= " AND d.status = :status";
            $params['status'] = strtoupper($status);
        }
        if ($eventId !== '') {
            $sql .= " AND d.donation_event_id = :event_id";
            $params['event_id'] = $eventId;
        }
        if ($bloodFilter !== '') {
            $sql .= " AND d.blood_type_id = :blood_type";
            $params['blood_type'] = $bloodFilter;
        }

        $sql .= " ORDER BY d.donation_date DESC";

        $donations = $connection->fetchAllAssociative($sql, $params);

        $stats = $connection->fetchAssociative("
            SELECT
                COUNT(*)                                            AS total,
                COUNT(*) FILTER (WHERE status = 'COMPLETED')       AS completed,
                COUNT(*) FILTER (WHERE status = 'CANCELLED')       AS cancelled,
                COUNT(*) FILTER (WHERE status = 'SCHEDULED')       AS scheduled,
                COUNT(*) FILTER (WHERE screening_passed = true)    AS screening_passed,
                COALESCE(SUM(volume_collected), 0)                 AS total_litres,
                COALESCE(SUM(units_collected), 0)                  AS total_units_collected
            FROM donations
        ");

        $events = $connection->fetchAllAssociative(
            "SELECT event_id, name, start_date, end_date FROM donation_events ORDER BY start_date DESC"
        );

        $hospitals = $connection->fetchAllAssociative(
            "SELECT hospital_id, name FROM hospital ORDER BY name"
        );

        $bloodTypes = $connection->fetchAllAssociative(
            "SELECT blood_type_id FROM blood_type ORDER BY blood_type_id"
        );

        $donors = $connection->fetchAllAssociative(
            "SELECT dr.user_id, dr.first_name, dr.last_name, dr.blood_type_id,
                    dr.total_donations, dr.city
             FROM donors dr
             ORDER BY dr.first_name, dr.last_name"
        );

        return $this->render('dashboard/donations.html.twig', [
            'session_user'   => $sessionUser,
            'donations'      => $donations,
            'stats'          => $stats,
            'events'         => $events,
            'donation_events'=> $events,
            'hospitals'      => $hospitals,
            'blood_types'    => $bloodTypes,
            'donors'         => $donors,
            'search'         => $search,
            'status_filter'  => $status,
            'event_filter'   => $eventId,
            'blood_filter'   => $bloodFilter,
            'user_type'      => $userType,
        ]);
    }

    #[Route('/dashboard/donations/ai-predict', name: 'donations_ai_predict', methods: ['POST'])]
    public function aiPredict(Request $request): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $prompt = $request->request->get('prompt', '');
        if (!$prompt) {
            return $this->json(['error' => 'No prompt provided'], 400);
        }

        $apiKey = $_ENV['ANTHROPIC_API_KEY'] ?? '';

        $ch = curl_init('https://api.anthropic.com/v1/messages');
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST           => true,
            CURLOPT_TIMEOUT        => 30,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_HTTPHEADER     => [
                'Content-Type: application/json',
                'x-api-key: ' . $apiKey,
                'anthropic-version: 2023-06-01',
            ],
            CURLOPT_POSTFIELDS => json_encode([
                'model'      => 'claude-sonnet-4-20250514',
                'max_tokens' => 1000,
                'messages'   => [['role' => 'user', 'content' => $prompt]],
            ]),
        ]);

        $response  = curl_exec($ch);
        $curlError = curl_error($ch);
        $httpCode  = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if (!$response) {
            return $this->json(['result' => 'cURL failed. Error: ' . $curlError . ' | Key starts with: ' . substr($apiKey, 0, 10)]);
        }

        $data = json_decode($response, true);

        if (isset($data['error'])) {
            return $this->json(['result' => 'API error (' . $httpCode . '): ' . ($data['error']['message'] ?? json_encode($data))]);
        }

        $text = $data['content'][0]['text'] ?? 'No content. Raw: ' . substr($response, 0, 300);
        return $this->json(['result' => $text]);
    }

    #[Route('/dashboard/donations/create', name: 'donations_create', methods: ['POST'])]
    public function create(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
        $id  = $this->generateUuidV4();

        $volume = $request->request->get('volume_collected') ?: null;
        if ($volume !== null && (float)$volume >= 1000) {
            $this->addFlash('error', 'Volume collected cannot exceed 999.99 mL.');
            return $this->redirectToRoute('donations_index');
        }

        $donorName = trim((string) $request->request->get('donor_name', ''));
        $userId    = null;

        if ($donorName === '') {
            $this->addFlash('error', 'Donor name is required.');
            return $this->redirectToRoute('donations_index');
        }

        if (preg_match('/\d/', $donorName)) {
            $this->addFlash('error', 'Donor name cannot contain numbers.');
            return $this->redirectToRoute('donations_index');
        }

        $parts     = explode(' ', $donorName, 2);
        $firstName = $parts[0] ?? '';
        $lastName  = $parts[1] ?? '';

        $donor = $connection->fetchAssociative(
            "SELECT u.user_id FROM users u
             JOIN donors dr ON dr.user_id = u.user_id
             WHERE u.first_name ILIKE :first AND u.last_name ILIKE :last
             LIMIT 1",
            ['first' => $firstName, 'last' => $lastName]
        );

        if ($donor) {
            $userId = $donor['user_id'];
        } else {
            $userId = $this->generateUuidV4();
            $connection->executeStatement("
                INSERT INTO users (user_id, first_name, last_name, email, password_hash, user_type, is_active, created_at, updated_at)
                VALUES (:id, :first, :last, :email, :pass, 'DONOR', true, :now, :now)
            ", [
                'id'    => $userId,
                'first' => $firstName,
                'last'  => $lastName,
                'email' => strtolower($firstName . '.' . $lastName . '@bloodlink.local'),
                'pass'  => password_hash('bloodlink123', PASSWORD_BCRYPT),
                'now'   => $now,
            ]);
            $connection->executeStatement("
                INSERT INTO donors (user_id, first_name, last_name, total_donations, created_at)
                VALUES (:id, :first, :last, 0, :now)
            ", [
                'id'    => $userId,
                'first' => $firstName,
                'last'  => $lastName,
                'now'   => $now,
            ]);
            $this->addFlash('success', "New donor '$donorName' created automatically.");
        }

        $donationDate = $request->request->get('donation_date');
        $eventId      = $request->request->get('donation_event_id') ?: null;

        if ($eventId && $donationDate) {
            $event = $connection->fetchAssociative(
                "SELECT start_date, end_date FROM donation_events WHERE event_id = :id",
                ['id' => $eventId]
            );
            if ($event) {
                $dDate  = new \DateTimeImmutable($donationDate);
                $eStart = new \DateTimeImmutable($event['start_date']);
                $eEnd   = new \DateTimeImmutable($event['end_date']);
                if ($dDate < $eStart || $dDate > $eEnd) {
                    $this->addFlash('error', 'Donation date must be within the event\'s date range (' .
                        $eStart->format('M d, Y') . ' – ' . $eEnd->format('M d, Y') . ').');
                    return $this->redirectToRoute('donations_index');
                }
            }
        }

        $connection->executeStatement("
            INSERT INTO donations
                (donation_id, user_id, hospital_id, donation_event_id, blood_type_id,
                 donation_date, volume_collected, units_collected,
                 status, screening_passed, medical_notes, created_at)
            VALUES
                (:id, :user_id, :hospital_id, :event_id, :blood_type_id,
                 :donation_date, :volume_collected, NULL,
                 'COMPLETED', :screening_passed, :medical_notes, :now)
        ", [
            'id'               => $id,
            'user_id'          => $userId,
            'hospital_id'      => $request->request->get('hospital_id') ?: null,
            'event_id'         => $eventId,
            'blood_type_id'    => $request->request->get('blood_type_id') ?: null,
            'donation_date'    => $donationDate,
            'volume_collected' => $volume,
            'screening_passed' => $request->request->get('screening_passed') === '1' ? 'true' : 'false',
            'medical_notes'    => $request->request->get('medical_notes') ?: null,
            'now'              => $now,
        ]);

        $connection->executeStatement("
            UPDATE donors SET
                total_donations    = total_donations + 1,
                last_donation_date = :date
            WHERE user_id = :uid
        ", ['date' => $donationDate, 'uid' => $userId]);

        $this->addFlash('success', 'Donation recorded successfully.');
        return $this->redirectToRoute('donations_index');
    }

    #[Route('/dashboard/donations/{id}/edit', name: 'donations_edit', methods: ['POST'])]
    public function edit(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $connection->executeStatement("
            UPDATE donations SET
                hospital_id       = :hospital_id,
                donation_event_id = :event_id,
                blood_type_id     = :blood_type_id,
                donation_date     = :donation_date,
                volume_collected  = :volume_collected,
                status            = :status,
                screening_passed  = :screening_passed,
                medical_notes     = :medical_notes
            WHERE donation_id = :id
        ", [
            'id'               => $id,
            'hospital_id'      => $request->request->get('hospital_id') ?: null,
            'event_id'         => $request->request->get('donation_event_id') ?: null,
            'blood_type_id'    => $request->request->get('blood_type_id') ?: null,
            'donation_date'    => $request->request->get('donation_date'),
            'volume_collected' => $request->request->get('volume_collected') ?: null,
            'status'           => $request->request->get('status', 'COMPLETED'),
            'screening_passed' => $request->request->get('screening_passed') === '1' ? 'true' : 'false',
            'medical_notes'    => $request->request->get('medical_notes') ?: null,
        ]);

        $this->addFlash('success', 'Donation updated successfully.');
        return $this->redirectToRoute('donations_index');
    }

    #[Route('/dashboard/donations/{id}/delete', name: 'donations_delete', methods: ['POST'])]
    public function delete(string $id, Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $connection->executeStatement(
            "DELETE FROM donations WHERE donation_id = :id",
            ['id' => $id]
        );

        $this->addFlash('success', 'Donation deleted successfully.');
        return $this->redirectToRoute('donations_index');
    }

    #[Route('/dashboard/donations/{id}/pdf', name: 'donations_pdf', methods: ['GET'])]
    public function exportPdf(string $id, Connection $connection, Request $request): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $donation = $connection->fetchAssociative("
            SELECT d.*,
                   u.first_name || ' ' || u.last_name AS donor_name,
                   u.email                             AS donor_email,
                   dr.blood_type_id                    AS donor_blood_type,
                   dr.total_donations                  AS donor_total_donations,
                   dr.city                             AS donor_city,
                   h.name                              AS hospital_name,
                   de.name                             AS event_name,
                   d.blood_type_id                     AS blood_code
            FROM donations d
            LEFT JOIN donors dr ON dr.user_id = d.user_id
            LEFT JOIN users u   ON u.user_id  = dr.user_id
            LEFT JOIN hospital h ON h.hospital_id = d.hospital_id
            LEFT JOIN donation_events de ON de.event_id = d.donation_event_id
            WHERE d.donation_id = :id
        ", ['id' => $id]);

        if (!$donation) {
            throw $this->createNotFoundException('Donation not found');
        }

        $qrCode   = new QrCode('BLOODLINK-DONATION:' . $id);
        $writer   = new PngWriter();
        $result   = $writer->write($qrCode);
        $qrBase64 = base64_encode($result->getString());

        $donationDate = $donation['donation_date']
            ? (new \DateTimeImmutable($donation['donation_date']))->format('M d, Y H:i')
            : 'N/A';

        $screeningBadge = $donation['screening_passed']
            ? '<span style="background:#dcfce7;color:#166534;padding:3px 10px;border-radius:20px;font-size:12px;font-weight:700;">✓ Passed</span>'
            : '<span style="background:#fee2e2;color:#991b1b;padding:3px 10px;border-radius:20px;font-size:12px;font-weight:700;">✗ Failed</span>';

        $html = <<<HTML
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
    body { font-family: Arial, sans-serif; color: #1f2937; padding: 40px; margin: 0; }
    .brand { font-size: 28px; font-weight: 800; color: #8f1d1f; }
    .subtitle { font-size: 16px; color: #6b7280; margin-top: 4px; }
    .section { margin-bottom: 24px; }
    .section-title { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .07em; color: #6b7280; border-bottom: 1px solid #e5e7eb; padding-bottom: 6px; margin-bottom: 14px; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .item .lbl { font-size: 10px; color: #9ca3af; text-transform: uppercase; letter-spacing: .05em; margin-bottom: 2px; }
    .item .val { font-size: 15px; font-weight: 600; color: #111827; }
    .footer { margin-top: 40px; text-align: center; font-size: 11px; color: #9ca3af; border-top: 1px solid #e5e7eb; padding-top: 16px; }
    .id-text { font-size: 9px; color: #9ca3af; word-break: break-all; margin-top: 4px; }
    table { width: 100%; border-collapse: collapse; }
</style>
</head>
<body>
<table>
<tr>
<td style="vertical-align:top;">
    <div class="brand">BloodLink</div>
    <div class="subtitle">Donation Certificate</div>
</td>
<td style="text-align:right;vertical-align:top;">
    <img src="data:image/png;base64,{$qrBase64}" style="width:90px;height:90px;">
</td>
</tr>
</table>
<hr style="border:none;border-top:3px solid #8f1d1f;margin:16px 0 28px;">
<div class="section">
    <div class="section-title">Donor Information</div>
    <div class="grid">
        <div class="item"><div class="lbl">Full Name</div><div class="val">{$donation['donor_name']}</div></div>
        <div class="item"><div class="lbl">Blood Type</div><div class="val">{$donation['blood_code']}</div></div>
        <div class="item"><div class="lbl">City</div><div class="val">{$donation['donor_city']}</div></div>
        <div class="item"><div class="lbl">Total Donations</div><div class="val">{$donation['donor_total_donations']}</div></div>
    </div>
</div>
<div class="section">
    <div class="section-title">Donation Details</div>
    <div class="grid">
        <div class="item"><div class="lbl">Date</div><div class="val">{$donationDate}</div></div>
        <div class="item"><div class="lbl">Volume Collected</div><div class="val">{$donation['volume_collected']} mL</div></div>
        <div class="item"><div class="lbl">Hospital</div><div class="val">{$donation['hospital_name']}</div></div>
        <div class="item"><div class="lbl">Event</div><div class="val">{$donation['event_name']}</div></div>
        <div class="item"><div class="lbl">Status</div><div class="val"><span style="background:#dcfce7;color:#166534;padding:3px 10px;border-radius:20px;font-size:12px;font-weight:700;">{$donation['status']}</span></div></div>
        <div class="item"><div class="lbl">Screening</div><div class="val">{$screeningBadge}</div></div>
    </div>
</div>
HTML;

        if ($donation['medical_notes']) {
            $notes = htmlspecialchars($donation['medical_notes']);
            $html .= <<<HTML
<div class="section">
    <div class="section-title">Medical Notes</div>
    <p style="font-size:14px;color:#374151;line-height:1.6;">{$notes}</p>
</div>
HTML;
        }

        $generated = date('M d, Y H:i');
        $html .= <<<HTML
<div class="footer">
    <div>Generated by BloodLink on {$generated}</div>
    <div class="id-text">Donation ID: {$id}</div>
</div>
</body>
</html>
HTML;

        $dompdf = new Dompdf();
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return new Response(
            $dompdf->output(),
            200,
            [
                'Content-Type'        => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="donation-' . substr($id, 0, 8) . '.pdf"',
            ]
        );
    }

    #[Route('/dashboard/donations/donor/{userId}/history', name: 'donations_donor_history', methods: ['GET'])]
    public function donorHistory(string $userId, Connection $connection, Request $request): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $history = $connection->fetchAllAssociative("
            SELECT d.*,
                   h.name AS hospital_name,
                   de.name AS event_name,
                   d.blood_type_id AS blood_code
            FROM donations d
            LEFT JOIN hospital h ON h.hospital_id = d.hospital_id
            LEFT JOIN donation_events de ON de.event_id = d.donation_event_id
            WHERE d.user_id = :uid
            ORDER BY d.donation_date DESC
        ", ['uid' => $userId]);

        $donor = $connection->fetchAssociative("
            SELECT dr.*, u.first_name, u.last_name, u.email
            FROM donors dr
            JOIN users u ON u.user_id = dr.user_id
            WHERE dr.user_id = :uid
        ", ['uid' => $userId]);

        return $this->json(['donor' => $donor, 'history' => $history]);
    }
    #[Route('/dashboard/donor/events', name: 'dashboard_donor_events', methods: ['GET'])]
    public function donorEvents(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) return $this->redirectToRoute('auth_index');

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType !== 'DONOR') return $this->redirectToRoute('donations_index');

        // Auto-sync statuses
        $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');
        $connection->executeStatement("
            UPDATE donation_events
            SET status = 'ACTIVE', updated_at = :now
            WHERE status = 'PLANNED' AND start_date <= :now AND end_date >= :now
        ", ['now' => $now]);
        $connection->executeStatement("
            UPDATE donation_events
            SET status = 'COMPLETED', updated_at = :now
            WHERE status IN ('PLANNED', 'ACTIVE') AND end_date < :now
        ", ['now' => $now]);

        $userId = (string) ($sessionUser['id'] ?? '');

        $events = $connection->fetchAllAssociative("
            SELECT de.*,
                   h.name AS hospital_name,
                   COUNT(d.donation_id) AS total_donations,
                   CASE
                       WHEN EXISTS (
                           SELECT 1
                           FROM donation_event_donor ded
                           WHERE ded.event_id = de.event_id
                             AND ded.user_id = :user_id
                       ) THEN true
                       ELSE false
                   END AS has_joined
            FROM donation_events de
            LEFT JOIN hospital h ON h.hospital_id = de.hospital_id
            LEFT JOIN donations d ON d.donation_event_id = de.event_id
            WHERE de.status IN ('PLANNED', 'ACTIVE')
            GROUP BY de.event_id, h.name
            ORDER BY de.start_date ASC
        ", ['user_id' => $userId]);

        return $this->render('dashboard/donor_events.html.twig', [
            'session_user' => $sessionUser,
            'events'       => $events,
        ]);
    }

   #[Route('/dashboard/donor/my-donations', name: 'donor_my_donations', methods: ['GET'])]
    public function myDonations(Request $request, Connection $connection): Response
    {
    $sessionUser = $request->getSession()->get('auth_user');
    if (!$sessionUser) return $this->redirectToRoute('auth_index');

    $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
    if ($userType !== 'DONOR') return $this->redirectToRoute('donations_index');

    $userId = (string) ($sessionUser['id'] ?? '');

    $donations = $connection->fetchAllAssociative("
        SELECT d.*,
               h.name          AS hospital_name,
               de.name         AS event_name,
               d.blood_type_id AS blood_code,
               (SELECT COUNT(*) FROM donations d2
                WHERE d2.user_id = d.user_id
                AND d2.donation_date < d.donation_date) AS prior_donations
        FROM donations d
        LEFT JOIN hospital h ON h.hospital_id = d.hospital_id
        LEFT JOIN donation_events de ON de.event_id = d.donation_event_id
        WHERE d.user_id = :uid
        ORDER BY d.donation_date DESC
    ", ['uid' => $userId]);

    $stats = $connection->fetchAssociative("
        SELECT
            COUNT(*)                                         AS total,
            COUNT(*) FILTER (WHERE screening_passed = true)  AS passed,
            COALESCE(SUM(volume_collected), 0)               AS total_volume
        FROM donations
        WHERE user_id = :uid
    ", ['uid' => $userId]);

    return $this->render('dashboard/donor_my_donations.html.twig', [
        'session_user' => $sessionUser,
        'donations'    => $donations,
        'stats'        => $stats,
    ]);
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