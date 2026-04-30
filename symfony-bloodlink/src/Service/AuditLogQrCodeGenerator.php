<?php

namespace App\Service;

use App\Entity\BloodTransferRequestLog;
use App\Entity\DonationLog;
use Endroid\QrCode\Builder\Builder;
use Endroid\QrCode\Encoding\Encoding;
use Endroid\QrCode\ErrorCorrectionLevel;
use Endroid\QrCode\Writer\SvgWriter;

/**
 * Generates an offline-readable QR code for an audit log.
 *
 * The QR payload is the textual detail of the log itself, so a phone scan
 * displays the data immediately without opening any web page.
 */
final class AuditLogQrCodeGenerator
{
    public function donationLogDataUri(DonationLog $log): string
    {
        return $this->buildDataUri($this->donationLogPayload($log));
    }

    public function transferLogDataUri(BloodTransferRequestLog $log): string
    {
        return $this->buildDataUri($this->transferLogPayload($log));
    }

    public function donationLogPayload(DonationLog $log): string
    {
        $donation = $log->getDonation();
        $user = $log->getUser();

        $lines = [
            'BloodLink :: Donation Audit Log',
            '------------------------------',
            'Log ID      : ' . $log->getLogId(),
            'Action      : ' . $log->getAction(),
            'Status      : ' . ($log->getPreviousStatus() ?? 'N/A') . ' -> ' . ($log->getNewStatus() ?? 'N/A'),
            'Created at  : ' . ($log->getCreatedAt()?->format('Y-m-d H:i') ?? 'N/A'),
            'Donation ID : ' . ($donation?->getDonationId() ?? 'N/A'),
            'Donation    : ' . ($donation?->getStatus() ?? 'N/A'),
            'Donor       : ' . ($donation && $donation->getDonor()
                ? trim($donation->getDonor()->getFirstName() . ' ' . $donation->getDonor()->getLastName())
                : 'N/A'),
            'Hospital    : ' . ($donation?->getHospital()?->getName() ?? 'N/A'),
            'Logged by   : ' . ($user
                ? trim($user->getFirstName() . ' ' . $user->getLastName()) . ' (' . $user->getUserType() . ')'
                : 'N/A'),
            'Anomaly     : ' . ($log->isAnomalyDetected()
                ? ($log->getAnomalySeverity() ?? 'medium') . ' / ' . ($log->getAnomalyScore() ?? 0) . ' /100'
                : 'none'),
        ];

        $notes = trim((string) $log->getNotes());
        if ($notes !== '') {
            $lines[] = 'Notes       : ' . $this->truncate($notes, 200);
        }

        return implode("\n", $lines);
    }

    public function transferLogPayload(BloodTransferRequestLog $log): string
    {
        $request = $log->getBloodTransferRequest();
        $user = $log->getUser();

        $lines = [
            'BloodLink :: Transfer Audit Log',
            '-------------------------------',
            'Log ID       : ' . $log->getLogId(),
            'Action       : ' . $log->getAction(),
            'Status       : ' . ($log->getPreviousStatus() ?? 'N/A') . ' -> ' . ($log->getNewStatus() ?? 'N/A'),
            'Created at   : ' . ($log->getCreatedAt()?->format('Y-m-d H:i') ?? 'N/A'),
            'Transfer ID  : ' . ($request?->getTransferId() ?? 'N/A'),
            'Requesting   : ' . ($request?->getRequestingHospital()?->getName() ?? 'N/A'),
            'Approving    : ' . ($request?->getApprovingHospital()?->getName() ?? 'N/A'),
            'Logged by    : ' . ($user
                ? trim($user->getFirstName() . ' ' . $user->getLastName()) . ' (' . $user->getUserType() . ')'
                : 'N/A'),
            'Anomaly      : ' . ($log->isAnomalyDetected()
                ? ($log->getAnomalySeverity() ?? 'medium') . ' / ' . ($log->getAnomalyScore() ?? 0) . '/100'
                : 'none'),
        ];

        $notes = trim((string) $log->getNotes());
        if ($notes !== '') {
            $lines[] = 'Notes        : ' . $this->truncate($notes, 200);
        }

        return implode("\n", $lines);
    }

    private function buildDataUri(string $payload): string
    {
        $builder = new Builder(
            writer: new SvgWriter(),
            writerOptions: [],
            validateResult: false,
            data: $payload,
            encoding: new Encoding('UTF-8'),
            errorCorrectionLevel: ErrorCorrectionLevel::Medium,
            size: 280,
            margin: 10,
        );

        return $builder->build()->getDataUri();
    }

    private function truncate(string $text, int $max): string
    {
        if (mb_strlen($text) <= $max) {
            return $text;
        }

        return mb_substr($text, 0, $max - 1) . '…';
    }
}
