<?php

namespace App\Service;

use App\Entity\BloodTransferRequestLog;
use App\Entity\DonationLog;
use App\Repository\BloodTransferRequestLogRepository;
use App\Repository\DonationLogRepository;
use DateTimeImmutable;

final class AuditAnomalyDetector
{
    private const SEVERITY_ORDER = [
        'none' => 0,
        'low' => 1,
        'medium' => 2,
        'high' => 3,
        'critical' => 4,
    ];

    public function __construct(
        private readonly AuditAiClient $aiClient,
        private readonly DonationLogRepository $donationLogRepository,
        private readonly BloodTransferRequestLogRepository $transferLogRepository,
    ) {
    }

    public function analyzeDonationLog(DonationLog $log): AuditAnomalyResult
    {
        $rules = $this->evaluateCommonRules(
            'donation',
            $log->getAction(),
            $log->getPreviousStatus(),
            $log->getNewStatus(),
            $log->getNotes(),
            $log->getCreatedAt(),
            $log->getLogId(),
            $log->getUser()?->getUserId(),
            DonationLog::allowedStatuses(),
        );

        if ($log->getAction() === DonationLog::ACTION_SCREENING_FAILED) {
            $this->addRule($rules, 70, 'high', 'Screening failure requires audit review.');
        }

        if ($log->getNewStatus() === DonationLog::STATUS_REJECTED) {
            $this->addRule($rules, 65, 'high', 'Donation was moved into rejected status.');
        }

        if (
            $log->getAction() === DonationLog::ACTION_SCREENING_PASSED
            && !in_array($log->getNewStatus(), [DonationLog::STATUS_APPROVED, DonationLog::STATUS_COMPLETED], true)
        ) {
            $this->addRule($rules, 45, 'medium', 'Screening passed action does not align with the recorded new status.');
        }

        if (
            $log->getUser() !== null
            && $this->donationLogRepository->countByActorSince($log->getUser()->getUserId(), $this->recentWindowStart($log->getCreatedAt()), $log->getLogId()) >= 5
        ) {
            $this->addRule($rules, 55, 'medium', 'The same actor created several donation audit entries in a short window.');
        }

        return $this->mergeAiResult($rules, $this->buildDonationPayload($log));
    }

    public function analyzeTransferLog(BloodTransferRequestLog $log): AuditAnomalyResult
    {
        $rules = $this->evaluateCommonRules(
            'transfer',
            $log->getAction(),
            $log->getPreviousStatus(),
            $log->getNewStatus(),
            $log->getNotes(),
            $log->getCreatedAt(),
            $log->getLogId(),
            $log->getUser()?->getUserId(),
            BloodTransferRequestLog::allowedStatuses(),
        );

        if (
            $log->getAction() === BloodTransferRequestLog::ACTION_DISPATCHED
            && !in_array($log->getPreviousStatus(), [BloodTransferRequestLog::STATUS_APPROVED, BloodTransferRequestLog::STATUS_IN_TRANSIT], true)
        ) {
            $this->addRule($rules, 75, 'high', 'Transfer dispatch was recorded from an unexpected previous status.');
        }

        if (
            $log->getNewStatus() === BloodTransferRequestLog::STATUS_DELIVERED
            && $log->getPreviousStatus() !== BloodTransferRequestLog::STATUS_IN_TRANSIT
        ) {
            $this->addRule($rules, 70, 'high', 'Transfer was marked delivered without an in-transit previous status.');
        }

        if (
            $log->getUser() !== null
            && $this->transferLogRepository->countByActorSince($log->getUser()->getUserId(), $this->recentWindowStart($log->getCreatedAt()), $log->getLogId()) >= 5
        ) {
            $this->addRule($rules, 55, 'medium', 'The same actor created several transfer audit entries in a short window.');
        }

        return $this->mergeAiResult($rules, $this->buildTransferPayload($log));
    }

    public function applyDonationResult(DonationLog $log, AuditAnomalyResult $result): void
    {
        $log
            ->setAnomalyDetected($result->isAnomalous())
            ->setAnomalySeverity($result->getSeverity())
            ->setAnomalyScore($result->getScore())
            ->setAnomalyReasons($result->getReasons())
            ->setAnomalyExplanation($result->getExplanation())
            ->setAnomalyRecommendedAction($result->getRecommendedAction())
            ->setAnomalyAnalysisProvider($result->getProvider())
            ->setAnomalyRawResponse($result->getRawAiResponse());
    }

    public function applyTransferResult(BloodTransferRequestLog $log, AuditAnomalyResult $result): void
    {
        $log
            ->setAnomalyDetected($result->isAnomalous())
            ->setAnomalySeverity($result->getSeverity())
            ->setAnomalyScore($result->getScore())
            ->setAnomalyReasons($result->getReasons())
            ->setAnomalyExplanation($result->getExplanation())
            ->setAnomalyRecommendedAction($result->getRecommendedAction())
            ->setAnomalyAnalysisProvider($result->getProvider())
            ->setAnomalyRawResponse($result->getRawAiResponse());
    }

    /**
     * @param array<int, string> $statusOrder
     *
     * @return array{score: int, severity: string, reasons: array<int, string>, explanation: string, recommended_action: string}
     */
    private function evaluateCommonRules(
        string $logType,
        string $action,
        ?string $previousStatus,
        ?string $newStatus,
        ?string $notes,
        ?\DateTimeInterface $createdAt,
        string $logId,
        ?string $actorId,
        array $statusOrder,
    ): array {
        $rules = [
            'score' => 0,
            'severity' => 'none',
            'reasons' => [],
            'explanation' => 'No anomaly signals were detected by rule checks.',
            'recommended_action' => 'No extra review required.',
        ];

        if ($previousStatus !== null && $newStatus !== null) {
            $previousIndex = array_search($previousStatus, $statusOrder, true);
            $newIndex = array_search($newStatus, $statusOrder, true);
            if (is_int($previousIndex) && is_int($newIndex) && $newIndex < $previousIndex) {
                $this->addRule($rules, 72, 'high', 'Status moved backward in the workflow.');
            }
        }

        if ($createdAt !== null) {
            $hour = (int) $createdAt->format('G');
            if ($hour < 6 || $hour >= 22) {
                $this->addRule($rules, 35, 'low', 'Audit entry was recorded outside normal operating hours.');
            }
        }

        $notesText = mb_strtolower(trim((string) $notes));
        if (
            $notesText === ''
            && in_array($newStatus, ['REJECTED', 'DELIVERED'], true)
        ) {
            $this->addRule($rules, 45, 'medium', 'Sensitive status change was recorded without explanatory notes.');
        }

        if ($notesText !== '' && preg_match('/\b(override|manual|urgent|emergency|correction|backdate)\b/i', $notesText) === 1) {
            $this->addRule($rules, 40, 'medium', 'Notes contain operational keywords that usually require review.');
        }

        if ($action === '' || $logId === '' || $actorId === null || $actorId === '') {
            $this->addRule($rules, 60, 'medium', sprintf('The %s audit payload is missing important attribution data.', $logType));
        }

        return $rules;
    }

    /**
     * @param array{score: int, severity: string, reasons: array<int, string>, explanation: string, recommended_action: string} $rules
     */
    private function addRule(array &$rules, int $score, string $severity, string $reason): void
    {
        $rules['score'] = max($rules['score'], $score);
        if (self::SEVERITY_ORDER[$severity] > self::SEVERITY_ORDER[$rules['severity']]) {
            $rules['severity'] = $severity;
        }

        if (!in_array($reason, $rules['reasons'], true)) {
            $rules['reasons'][] = $reason;
        }

        $rules['explanation'] = 'One or more anomaly signals were detected on this audit entry.';
        $rules['recommended_action'] = in_array($severity, ['high', 'critical'], true)
            ? 'Review the linked record, confirm the actor and status transition, and contact the responsible hospital if needed.'
            : 'Review during the next audit pass.';
    }

    /**
     * @param array{score: int, severity: string, reasons: array<int, string>, explanation: string, recommended_action: string} $rules
     * @param array<string, mixed> $payload
     */
    private function mergeAiResult(array $rules, array $payload): AuditAnomalyResult
    {
        $ai = $this->aiClient->analyzeAuditPayload($payload);
        $provider = $ai === null ? 'rules' : 'rules+ai';

        if ($ai !== null) {
            $aiScore = max(0, min(100, (int) ($ai['score'] ?? 0)));
            $aiSeverity = $this->normalizeSeverity((string) ($ai['severity'] ?? 'none'));
            $aiReasons = $this->normalizeReasons($ai['reasons'] ?? []);

            $rules['score'] = max($rules['score'], $aiScore);
            if (self::SEVERITY_ORDER[$aiSeverity] > self::SEVERITY_ORDER[$rules['severity']]) {
                $rules['severity'] = $aiSeverity;
            }

            foreach ($aiReasons as $reason) {
                if (!in_array($reason, $rules['reasons'], true)) {
                    $rules['reasons'][] = $reason;
                }
            }

            $rules['explanation'] = trim((string) ($ai['explanation'] ?? '')) !== ''
                ? trim((string) $ai['explanation'])
                : $rules['explanation'];
            $rules['recommended_action'] = trim((string) ($ai['recommended_action'] ?? '')) !== ''
                ? trim((string) $ai['recommended_action'])
                : $rules['recommended_action'];
        }

        $anomalous = $rules['score'] >= 35 || $rules['severity'] !== 'none' || $rules['reasons'] !== [];

        if (!$anomalous) {
            $rules['score'] = 0;
            $rules['severity'] = 'none';
            $rules['reasons'] = [];
            $rules['explanation'] = 'No anomaly signals were detected by the configured checks.';
            $rules['recommended_action'] = 'No extra review required.';
        }

        return new AuditAnomalyResult(
            $anomalous,
            $rules['severity'],
            max(0, min(100, $rules['score'])),
            $rules['reasons'],
            $rules['explanation'],
            $rules['recommended_action'],
            $provider,
            $ai ?? [],
        );
    }

    private function normalizeSeverity(string $severity): string
    {
        $severity = mb_strtolower(trim($severity));

        return array_key_exists($severity, self::SEVERITY_ORDER) ? $severity : 'none';
    }

    private function recentWindowStart(?\DateTimeInterface $createdAt): DateTimeImmutable
    {
        if ($createdAt === null) {
            return new DateTimeImmutable('-30 minutes');
        }

        return DateTimeImmutable::createFromInterface($createdAt)->modify('-30 minutes');
    }

    /**
     * @param mixed $value
     *
     * @return array<int, string>
     */
    private function normalizeReasons(mixed $value): array
    {
        if (!is_array($value)) {
            return [];
        }

        $reasons = [];
        foreach ($value as $reason) {
            $reason = trim((string) $reason);
            if ($reason !== '') {
                $reasons[] = $reason;
            }
        }

        return array_slice(array_values(array_unique($reasons)), 0, 6);
    }

    /**
     * @return array<string, mixed>
     */
    private function buildDonationPayload(DonationLog $log): array
    {
        $donation = $log->getDonation();

        return [
            'log_type' => 'donation',
            'log_id' => $log->getLogId(),
            'action' => $log->getAction(),
            'previous_status' => $log->getPreviousStatus(),
            'new_status' => $log->getNewStatus(),
            'notes' => $log->getNotes(),
            'created_at' => $log->getCreatedAt()?->format(DATE_ATOM),
            'actor' => $this->actorPayload($log->getUser()),
            'donation' => [
                'id' => $donation?->getDonationId(),
                'current_status' => $donation?->getStatus(),
                'hospital' => $donation?->getHospital()?->getName(),
            ],
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function buildTransferPayload(BloodTransferRequestLog $log): array
    {
        $transfer = $log->getBloodTransferRequest();

        return [
            'log_type' => 'transfer',
            'log_id' => $log->getLogId(),
            'action' => $log->getAction(),
            'previous_status' => $log->getPreviousStatus(),
            'new_status' => $log->getNewStatus(),
            'notes' => $log->getNotes(),
            'created_at' => $log->getCreatedAt()?->format(DATE_ATOM),
            'actor' => $this->actorPayload($log->getUser()),
            'transfer' => [
                'id' => $transfer?->getTransferId(),
                'current_status' => $transfer?->getStatus(),
                'requesting_hospital' => $transfer?->getRequestingHospital()?->getName(),
                'approving_hospital' => $transfer?->getApprovingHospital()?->getName(),
                'requested_units' => $transfer?->getQuantityUnitsRequested(),
                'approved_units' => $transfer?->getQuantityUnitsApproved(),
            ],
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function actorPayload(?\App\Entity\User $user): array
    {
        return [
            'id_suffix' => $user !== null ? substr($user->getUserId(), 0, 8) : null,
            'type' => $user?->getUserType(),
        ];
    }
}
