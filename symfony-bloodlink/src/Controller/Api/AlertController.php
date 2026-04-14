<?php

namespace App\Controller\Api;

use App\Service\AlertService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for blood donation alerts.
 */
#[Route('/api/alerts', name: 'api_alerts_')]
class AlertController extends AbstractController
{
    public function __construct(
        private readonly AlertService $alertService,
    ) {
    }

    /**
     * Get all active alerts.
     */
    #[Route('', methods: ['GET'])]
    public function listActiveAlerts(): JsonResponse
    {
        // TODO: Implement active alerts retrieval
        // Optional query parameter: status

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get alerts for a hospital.
     */
    #[Route('/hospital/{hospitalId}', methods: ['GET'])]
    public function getHospitalAlerts(string $hospitalId): JsonResponse
    {
        // TODO: Implement hospital-specific alerts retrieval

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Create a new blood donation alert.
     */
    #[Route('', methods: ['POST'])]
    public function createAlert(Request $request): JsonResponse
    {
        // TODO: Implement alert creation
        // Request body:
        // {
        //   "hospital_id": "uuid",
        //   "blood_type": "O+",
        //   "units_needed": 20,
        //   "urgency": "critical|urgent|routine",
        //   "description": "Optional description"
        // }

        return new JsonResponse(ResponseUtil::success([]), 201);
    }

    /**
     * Send alert to eligible donors.
     */
    #[Route('/{alertId}/send', methods: ['POST'])]
    public function sendAlert(string $alertId, Request $request): JsonResponse
    {
        // TODO: Implement alert sending
        // Request body:
        // {
        //   "target_donor_count": 50
        // }
        // Response includes created donor alerts

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get alert details with response statistics.
     */
    #[Route('/{alertId}', methods: ['GET'])]
    public function getAlert(string $alertId): JsonResponse
    {
        // TODO: Implement alert retrieval with statistics
        // Include response counts, response rate, etc.

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get donor alerts for a specific donor.
     */
    #[Route('/donor/{donorId}', methods: ['GET'])]
    public function getDonorAlerts(string $donorId): JsonResponse
    {
        // TODO: Implement donor-specific alerts retrieval

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Record donor response to alert.
     */
    #[Route('/donor-alert/{donorAlertId}/respond', methods: ['POST'])]
    public function respondToAlert(string $donorAlertId, Request $request): JsonResponse
    {
        // TODO: Implement response recording
        // Request body:
        // {
        //   "response": "accepted|rejected|no_response"
        // }

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Close an alert.
     */
    #[Route('/{alertId}/close', methods: ['POST'])]
    public function closeAlert(string $alertId, Request $request): JsonResponse
    {
        // TODO: Implement alert closure
        // Request body:
        // {
        //   "reason": "Closure reason"
        // }

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Cancel an alert.
     */
    #[Route('/{alertId}/cancel', methods: ['POST'])]
    public function cancelAlert(string $alertId): JsonResponse
    {
        // TODO: Implement alert cancellation

        return new JsonResponse(ResponseUtil::success([]));
    }
}
