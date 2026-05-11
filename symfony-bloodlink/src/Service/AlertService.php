<?php

namespace App\Service;

use App\Entity\Alert;
use App\Entity\Donor;
use App\Entity\DonorAlert;
use App\Repository\AlertRepository;
use App\Repository\DonorAlertRepository;
use App\Repository\DonorRepository;
use App\Repository\HospitalRepository;
use App\Util\BloodCompatibilityUtil;
use App\Util\LocationUtil;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;

/**
 * Service class for managing blood donation alerts and donor notifications.
 * Handles alert creation, targeting, and donor alerting workflows.
 */
class AlertService
{
    public function __construct(
        private readonly AlertRepository $alertRepository,
        private readonly DonorAlertRepository $donorAlertRepository,
        private readonly DonorRepository $donorRepository,
        private readonly HospitalRepository $hospitalRepository,
        private readonly EntityManagerInterface $entityManager,
        private readonly MailerInterface $mailer,
        private readonly EmailTemplateService $emailTemplateService,
    ) {
    }

    /**
     * Create and persist a new blood donation alert, find compatible nearby donors,
     * create DonorAlert records, and send email notifications.
     *
     * @param Alert  $alert       The alert entity (already populated from form)
     * @param string $hospitalId  The hospital ID
     * @param string $staffId     The staff user ID
     *
     * @return array{alert: Alert, notified_donors: int, total_compatible: int, emails_sent: int}
     */
    public function createAlertAndNotify(Alert $alert, string $hospitalId, string $staffId): array
    {
        // 1. Set system fields
        $alert->setAlertId($this->generateUuidV4());
        $alert->setHospitalId($hospitalId);
        $alert->setStaffId($staffId);
        $alert->setIsResolved(false);
        $alert->setCreatedAt(new \DateTime());

        if ($alert->getTargetRadiusKm() === null) {
            $alert->setTargetRadiusKm(50);
        }

        // 2. Persist the alert
        $this->entityManager->persist($alert);
        $this->entityManager->flush();

        // 3. Find hospital location
        $hospital = $this->hospitalRepository->find($hospitalId);
        $hospitalLat = $hospital ? (float) $hospital->getLatitude() : null;
        $hospitalLon = $hospital ? (float) $hospital->getLongitude() : null;
        $hospitalName = $hospital ? $hospital->getName() : 'Unknown Hospital';

        // 4. Find compatible donor blood types
        $compatibleTypes = BloodCompatibilityUtil::getCompatibleDonorTypes($alert->getBloodTypeId());

        // 5. Find donors with compatible blood types
        $compatibleDonors = $this->donorRepository->findByCompatibleBloodTypes($compatibleTypes);
        $totalCompatible = count($compatibleDonors);

        // 6. Filter by geographic proximity using Haversine formula
        $nearbyDonors = [];
        if ($hospitalLat !== null && $hospitalLon !== null && $hospitalLat != 0 && $hospitalLon != 0) {
            $radiusKm = (float) $alert->getTargetRadiusKm();

            foreach ($compatibleDonors as $donor) {
                $donorLat = $donor->getLatitude();
                $donorLon = $donor->getLongitude();

                if ($donorLat === null || $donorLon === null) {
                    continue;
                }

                $distance = LocationUtil::calculateDistance(
                    $hospitalLat,
                    $hospitalLon,
                    (float) $donorLat,
                    (float) $donorLon,
                );

                if ($distance <= $radiusKm) {
                    $nearbyDonors[] = [
                        'donor' => $donor,
                        'distance' => $distance,
                    ];
                }
            }

            // Sort by distance (closest first)
            usort($nearbyDonors, fn($a, $b) => $a['distance'] <=> $b['distance']);
        } else {
            // No hospital coords: use all compatible donors
            foreach ($compatibleDonors as $donor) {
                $nearbyDonors[] = [
                    'donor' => $donor,
                    'distance' => null,
                ];
            }
        }

        // 7. Create DonorAlert records and send emails
        $emailsSent = 0;
        $notifiedCount = 0;

        foreach ($nearbyDonors as $item) {
            /** @var Donor $donor */
            $donor = $item['donor'];

            // Create DonorAlert record
            $donorAlert = new DonorAlert();
            $donorAlert->setDonorAlertId($this->generateUuidV4());
            $donorAlert->setAlert($alert);
            $donorAlert->setDonor($donor);
            $donorAlert->setIsNotified(true);
            $donorAlert->setNotificationSentAt(new \DateTimeImmutable());
            $donorAlert->setIsRead(false);
            $donorAlert->setDonorResponse(DonorAlert::RESPONSE_NO_RESPONSE);

            $this->entityManager->persist($donorAlert);
            $notifiedCount++;

            // Send email notification
            $user = $donor->getUser();
            if ($user !== null) {
                try {
                    $donorEmail = $user->getEmail();
                    $donorName = $user->getFirstName() ?: $donor->getFirstName();
                    $bloodTypeNeeded = trim($alert->getBloodTypeId());
                    $donorBloodType = $donor->getBloodType() ? trim($donor->getBloodType()->getBloodTypeId()) : 'N/A';
                    $distanceText = $item['distance'] !== null
                        ? sprintf('%.1f km away', $item['distance'])
                        : 'Distance unknown';

                    $severityColor = match ($alert->getSeverity()) {
                        'CRITICAL' => '#dc2626',
                        'URGENT' => '#ea580c',
                        'WARNING' => '#d97706',
                        default => '#2563eb',
                    };

                    $htmlBody = $this->buildAlertEmailHtml(
                        $donorName,
                        $hospitalName,
                        $bloodTypeNeeded,
                    );

                    $email = (new Email())
                        ->from('bloodlink.supportteam@gmail.com')
                        ->to($donorEmail)
                        ->subject(sprintf('[%s] %s — Blood Needed: %s', $alert->getSeverity(), $alert->getTitle(), $bloodTypeNeeded))
                        ->html($htmlBody);

                    $this->mailer->send($email);
                    $emailsSent++;
                } catch (\Throwable $e) {
                    // Log error but continue with other donors
                    error_log(sprintf(
                        '[AlertService] Failed to send email to donor %s: %s',
                        $donor->getUserId(),
                        $e->getMessage(),
                    ));
                }
            }
        }

        $this->entityManager->flush();

        return [
            'alert' => $alert,
            'notified_donors' => $notifiedCount,
            'total_compatible' => $totalCompatible,
            'emails_sent' => $emailsSent,
        ];
    }

    /**
     * Build a professional HTML email body for the blood alert notification.
     */
    private function buildAlertEmailHtml(
        string $donorName,
        string $hospitalName,
        string $bloodTypeId,
    ): string {
        $title = 'Urgent Blood Request';
        $content = "
            <p>Hello <strong>{$donorName}</strong>,</p>
            <p>The hospital <strong>{$hospitalName}</strong> has an urgent need for blood (Type: <strong>{$bloodTypeId}</strong>).</p>
            <p>Your blood type may be compatible.</p>
            <p>Every donation can save a life.</p>
        ";

        return $this->emailTemplateService->render(
            $title,
            $content,
            'View request and volunteer',
            '#' // This would be the dashboard URL
        );
    }

    /**
     * Get active alerts (not resolved).
     *
     * @return Alert[]
     */
    public function getActiveAlerts(): array
    {
        return $this->alertRepository->findBy(
            ['isResolved' => false],
            ['createdAt' => 'DESC'],
        );
    }

    /**
     * Get alerts for a hospital.
     *
     * @param string $hospitalId Hospital UUID
     *
     * @return Alert[]
     */
    public function getHospitalAlerts(string $hospitalId): array
    {
        return $this->alertRepository->findBy(
            ['hospitalId' => $hospitalId],
            ['createdAt' => 'DESC'],
        );
    }

    /**
     * Record donor response to alert.
     *
     * @param string $donorAlertId Donor alert ID
     * @param string $response Response status (accepted, rejected, no_response)
     *
     * @return DonorAlert
     */
    public function recordDonorResponse(string $donorAlertId, string $response): DonorAlert
    {
        $donorAlert = $this->donorAlertRepository->find($donorAlertId);

        if ($donorAlert === null) {
            throw new \RuntimeException('DonorAlert not found: ' . $donorAlertId);
        }

        $donorAlert->setDonorResponse($response);
        $donorAlert->setIsRead(true);
        $donorAlert->setReadAt(new \DateTimeImmutable());
        $this->entityManager->flush();

        return $donorAlert;
    }

    /**
     * Get alert details including response statistics.
     *
     * @param string $alertId Alert ID
     *
     * @return array Alert data with response stats
     */
    public function getAlertStatistics(string $alertId): array
    {
        $alert = $this->alertRepository->find($alertId);
        if ($alert === null) {
            return [];
        }

        $donorAlerts = $this->donorAlertRepository->findBy(['alert' => $alert]);
        $total = count($donorAlerts);
        $interested = 0;
        $notInterested = 0;
        $noResponse = 0;

        foreach ($donorAlerts as $da) {
            match ($da->getDonorResponse()) {
                DonorAlert::RESPONSE_INTERESTED => $interested++,
                DonorAlert::RESPONSE_NOT_INTERESTED => $notInterested++,
                default => $noResponse++,
            };
        }

        return [
            'alert' => $alert,
            'total_sent' => $total,
            'interested' => $interested,
            'not_interested' => $notInterested,
            'no_response' => $noResponse,
            'response_rate' => $total > 0 ? round(($interested + $notInterested) / $total * 100, 1) : 0,
        ];
    }

    /**
     * Close/complete an alert.
     */
    public function closeAlert(string $alertId, string $closureReason): Alert
    {
        $alert = $this->alertRepository->find($alertId);
        if ($alert === null) {
            throw new \RuntimeException('Alert not found: ' . $alertId);
        }

        $alert->setIsResolved(true);
        $alert->setResolvedAt(new \DateTime());
        $this->entityManager->flush();

        return $alert;
    }

    /**
     * Cancel an active alert.
     */
    public function cancelAlert(string $alertId): Alert
    {
        return $this->closeAlert($alertId, 'Cancelled');
    }

    /**
     * Get donor alerts for a specific donor.
     *
     * @param string $donorId Donor ID
     *
     * @return DonorAlert[]
     */
    public function getDonorAlerts(string $donorId): array
    {
        return $this->donorAlertRepository->findBy(
            ['donor' => $donorId],
            ['notificationSentAt' => 'DESC'],
        );
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
