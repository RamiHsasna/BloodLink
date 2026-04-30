<?php

namespace App\Controller;

use App\Entity\BloodTransferRequestLog;
use App\Entity\DonationLog;
use App\Entity\User;
use App\Repository\BloodTransferRequestLogRepository;
use App\Repository\DonationLogRepository;
use App\Service\AuditAiClient;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

final class AuditChatbotController extends AbstractController
{
    public function __construct(
        private readonly AuditAiClient $auditAiClient,
        private readonly DonationLogRepository $donationLogRepository,
        private readonly BloodTransferRequestLogRepository $transferLogRepository,
    ) {
    }

    #[Route('/dashboard/logs/chatbot', name: 'dashboard_logs_chatbot', methods: ['POST'])]
    public function chat(Request $request): JsonResponse
    {
        $sessionUser = $this->getSessionUser($request);
        if ($sessionUser === null) {
            return new JsonResponse(['reply' => 'Please sign in before using the audit assistant.'], 403);
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType === User::TYPE_DONOR) {
            return new JsonResponse(['reply' => 'The audit assistant is available only in the Back Office.'], 403);
        }

        $hospitalId = $userType === User::TYPE_HOSPITAL_STAFF
            ? trim((string) ($sessionUser['hospital_id'] ?? ''))
            : null;

        if ($userType === User::TYPE_HOSPITAL_STAFF && ($hospitalId === null || $hospitalId === '')) {
            return new JsonResponse(['reply' => 'Your staff account is not linked to a hospital.'], 403);
        }

        $data = json_decode($request->getContent(), true);
        $question = is_array($data) ? trim((string) ($data['message'] ?? '')) : '';
        if ($question === '') {
            return new JsonResponse(['reply' => 'Ask a question about audit logs, anomaly review, or SMS alert status.']);
        }

        $context = [
            'scope' => $hospitalId === null ? 'all hospitals' : 'current staff hospital only',
            'donation_logs' => array_map(
                fn (DonationLog $log): array => $this->donationLogContext($log),
                $this->donationLogRepository->findRecentDetailed(8, $hospitalId),
            ),
            'transfer_logs' => array_map(
                fn (BloodTransferRequestLog $log): array => $this->transferLogContext($log),
                $this->transferLogRepository->findRecentDetailed(8, $hospitalId),
            ),
        ];

        return new JsonResponse([
            'reply' => $this->auditAiClient->answerAuditQuestion($context, $question),
        ]);
    }

    /**
     * @return array<string, mixed>|null
     */
    private function getSessionUser(Request $request): ?array
    {
        $sessionUser = $request->getSession()->get('auth_user');

        return is_array($sessionUser) ? $sessionUser : null;
    }

    /**
     * @return array<string, mixed>
     */
    private function donationLogContext(DonationLog $log): array
    {
        return [
            'type' => 'donation',
            'log_id' => substr($log->getLogId(), 0, 8),
            'action' => $log->getAction(),
            'previous_status' => $log->getPreviousStatus(),
            'new_status' => $log->getNewStatus(),
            'created_at' => $log->getCreatedAt()?->format('Y-m-d H:i'),
            'actor_type' => $log->getUser()?->getUserType(),
            'hospital' => $log->getDonation()?->getHospital()?->getName(),
            'anomaly_detected' => $log->isAnomalyDetected(),
            'anomaly_severity' => $log->getAnomalySeverity(),
            'anomaly_score' => $log->getAnomalyScore(),
            'anomaly_reviewed' => $log->isAnomalyReviewed(),
            'sms_status' => $log->getSmsAlertStatus(),
            'reasons' => $log->getAnomalyReasons(),
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function transferLogContext(BloodTransferRequestLog $log): array
    {
        $transfer = $log->getBloodTransferRequest();

        return [
            'type' => 'transfer',
            'log_id' => substr($log->getLogId(), 0, 8),
            'action' => $log->getAction(),
            'previous_status' => $log->getPreviousStatus(),
            'new_status' => $log->getNewStatus(),
            'created_at' => $log->getCreatedAt()?->format('Y-m-d H:i'),
            'actor_type' => $log->getUser()?->getUserType(),
            'requesting_hospital' => $transfer?->getRequestingHospital()?->getName(),
            'approving_hospital' => $transfer?->getApprovingHospital()?->getName(),
            'anomaly_detected' => $log->isAnomalyDetected(),
            'anomaly_severity' => $log->getAnomalySeverity(),
            'anomaly_score' => $log->getAnomalyScore(),
            'anomaly_reviewed' => $log->isAnomalyReviewed(),
            'sms_status' => $log->getSmsAlertStatus(),
            'reasons' => $log->getAnomalyReasons(),
        ];
    }
}
