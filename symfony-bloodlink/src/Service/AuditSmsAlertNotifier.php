<?php

namespace App\Service;

use App\Entity\BloodTransferRequestLog;
use App\Entity\DonationLog;
use Symfony\Contracts\HttpClient\HttpClientInterface;

final class AuditSmsAlertNotifier
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
    ) {
    }

    /**
     * @return array{status: string, recipient: ?string, error: ?string}
     */
    public function notifyDonationLog(DonationLog $log, AuditAnomalyResult $result): array
    {
        $recipients = $this->resolveRecipients([
            $log->getDonation()?->getHospital()?->getPhone(),
        ]);

        return $this->send(
            $recipients,
            sprintf(
                'BloodLink audit anomaly: donation log %s scored %d/%d (%s). Review: %s',
                substr($log->getLogId(), 0, 8),
                $result->getScore(),
                100,
                strtoupper($result->getSeverity()),
                $result->getRecommendedAction(),
            ),
        );
    }

    /**
     * @return array{status: string, recipient: ?string, error: ?string}
     */
    public function notifyTransferLog(BloodTransferRequestLog $log, AuditAnomalyResult $result): array
    {
        $transfer = $log->getBloodTransferRequest();
        $recipients = $this->resolveRecipients([
            $transfer?->getRequestingHospital()?->getPhone(),
            $transfer?->getApprovingHospital()?->getPhone(),
        ]);

        return $this->send(
            $recipients,
            sprintf(
                'BloodLink audit anomaly: transfer log %s scored %d/%d (%s). Review: %s',
                substr($log->getLogId(), 0, 8),
                $result->getScore(),
                100,
                strtoupper($result->getSeverity()),
                $result->getRecommendedAction(),
            ),
        );
    }

    /**
     * @param array<int, ?string> $fallbackRecipients
     *
     * @return array<int, string>
     */
    private function resolveRecipients(array $fallbackRecipients): array
    {
        $configured = array_filter(array_map(
            static fn (string $value): string => trim($value),
            explode(',', $this->env('AUDIT_ALERT_SMS_RECIPIENTS')),
        ));

        $recipients = $configured;
        foreach ($fallbackRecipients as $recipient) {
            $recipient = is_string($recipient) ? trim($recipient) : '';
            if ($recipient !== '') {
                $recipients[] = $recipient;
            }
        }

        return array_values(array_unique($recipients));
    }

    /**
     * @param array<int, string> $recipients
     *
     * @return array{status: string, recipient: ?string, error: ?string}
     */
    private function send(array $recipients, string $message): array
    {
        if ($recipients === []) {
            return [
                'status' => 'skipped',
                'recipient' => null,
                'error' => 'No SMS recipients configured for audit alerts.',
            ];
        }

        $sid = $this->env('TWILIO_ACCOUNT_SID');
        $token = $this->env('TWILIO_AUTH_TOKEN');
        $from = $this->env('TWILIO_FROM_NUMBER');

        if ($sid === '' || $token === '' || $from === '') {
            return [
                'status' => 'skipped',
                'recipient' => implode(', ', $recipients),
                'error' => 'Twilio credentials are not configured.',
            ];
        }

        $sent = [];
        $errors = [];

        foreach ($recipients as $recipient) {
            try {
                $response = $this->httpClient->request(
                    'POST',
                    sprintf('https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json', rawurlencode($sid)),
                    [
                        'auth_basic' => [$sid, $token],
                        'body' => [
                            'From' => $from,
                            'To' => $recipient,
                            'Body' => $message,
                        ],
                        'timeout' => 10,
                    ],
                );

                if ($response->getStatusCode() >= 400) {
                    $errors[] = sprintf('%s: HTTP %d', $recipient, $response->getStatusCode());
                    continue;
                }

                $sent[] = $recipient;
            } catch (\Throwable $exception) {
                $errors[] = sprintf('%s: %s', $recipient, $exception->getMessage());
            }
        }

        if ($sent !== []) {
            return [
                'status' => 'sent',
                'recipient' => implode(', ', $sent),
                'error' => $errors === [] ? null : implode('; ', $errors),
            ];
        }

        return [
            'status' => 'failed',
            'recipient' => implode(', ', $recipients),
            'error' => $errors === [] ? 'SMS provider did not accept the alert.' : implode('; ', $errors),
        ];
    }

    private function env(string $key): string
    {
        $value = $_ENV[$key] ?? $_SERVER[$key] ?? getenv($key);

        return is_string($value) ? trim($value) : '';
    }
}
