<?php

namespace App\Controller\Api;

use App\Service\BloodTransferService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for blood transfer request management.
 */
#[Route('/api/transfers', name: 'api_transfers_')]
class BloodTransferRequestController extends AbstractController
{
    public function __construct(
        private readonly BloodTransferService $transferService,
    ) {
    }

    /**
     * Get all pending transfer requests.
     */
    #[Route('', methods: ['GET'])]
    public function listPendingTransfers(): JsonResponse
    {
        // TODO: Implement pending transfer retrieval
        // Optional query parameter: status (pending|approved|rejected|completed)

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get transfer requests for a hospital.
     */
    #[Route('/hospital/{hospitalId}', methods: ['GET'])]
    public function getHospitalTransfers(string $hospitalId, Request $request): JsonResponse
    {
        // TODO: Implement hospital-specific transfer retrieval
        // Optional query parameter: status filter

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Create a blood transfer request.
     */
    #[Route('', methods: ['POST'])]
    public function createTransfer(Request $request): JsonResponse
    {
        // TODO: Implement transfer request creation
        // Request body:
        // {
        //   "requesting_hospital_id": "uuid",
        //   "blood_type": "O+",
        //   "units_needed": 10,
        //   "urgency": "critical|urgent|routine",
        //   "notes": "Optional notes"
        // }

        return new JsonResponse(ResponseUtil::success([]), 201);
    }

    /**
     * Get transfer request details.
     */
    #[Route('/{transferId}', methods: ['GET'])]
    public function getTransfer(string $transferId): JsonResponse
    {
        // TODO: Implement transfer retrieval

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Approve a transfer request.
     */
    #[Route('/{transferId}/approve', methods: ['POST'])]
    public function approveTransfer(string $transferId, Request $request): JsonResponse
    {
        // TODO: Implement transfer approval
        // Request body:
        // {
        //   "approver_staff_id": "uuid"
        // }
        // Verify approver is authorized at receiving hospital

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Reject a transfer request.
     */
    #[Route('/{transferId}/reject', methods: ['POST'])]
    public function rejectTransfer(string $transferId, Request $request): JsonResponse
    {
        // TODO: Implement transfer rejection
        // Request body:
        // {
        //   "rejector_staff_id": "uuid",
        //   "reason": "Rejection reason"
        // }

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Cancel a transfer request.
     */
    #[Route('/{transferId}/cancel', methods: ['POST'])]
    public function cancelTransfer(string $transferId, Request $request): JsonResponse
    {
        // TODO: Implement transfer cancellation
        // Request body:
        // {
        //   "reason": "Cancellation reason"
        // }

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Mark transfer as completed.
     */
    #[Route('/{transferId}/complete', methods: ['POST'])]
    public function completeTransfer(string $transferId): JsonResponse
    {
        // TODO: Implement transfer completion

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get transfer history for hospital.
     */
    #[Route('/hospital/{hospitalId}/history', methods: ['GET'])]
    public function getTransferHistory(string $hospitalId, Request $request): JsonResponse
    {
        // TODO: Implement transfer history retrieval
        // Query parameter: limit (default 50)

        return new JsonResponse(ResponseUtil::success([]));
    }
}
