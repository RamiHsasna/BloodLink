<?php

namespace App\Controller\Api;

use App\Service\BloodInventoryService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for blood inventory management.
 */
#[Route('/api/inventory', name: 'api_inventory_')]
class BloodInventoryController extends AbstractController
{
    public function __construct(
        private readonly BloodInventoryService $inventoryService,
    ) {
    }

    /**
     * Get inventory for a hospital.
     */
    #[Route('/hospital/{hospitalId}', methods: ['GET'])]
    public function getHospitalInventory(string $hospitalId): JsonResponse
    {
        // TODO: Implement hospital inventory retrieval
        // Return all blood types with current stock levels for the hospital

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get stock level for a specific blood type at a hospital.
     */
    #[Route('/hospital/{hospitalId}/{bloodType}', methods: ['GET'])]
    public function getStockLevel(string $hospitalId, string $bloodType): JsonResponse
    {
        // TODO: Implement single stock level retrieval
        // Return current units available for blood type

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Add blood to inventory.
     */
    #[Route('/add', methods: ['POST'])]
    public function addToInventory(Request $request): JsonResponse
    {
        // TODO: Implement inventory addition
        // Request body:
        // {
        //   "hospital_id": "uuid",
        //   "blood_type": "O+",
        //   "units": 5
        // }

        return new JsonResponse(ResponseUtil::success([]), 201);
    }

    /**
     * Remove blood from inventory.
     */
    #[Route('/remove', methods: ['POST'])]
    public function removeFromInventory(Request $request): JsonResponse
    {
        // TODO: Implement inventory removal
        // Check for sufficient stock before removing

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get critical stock alerts.
     */
    #[Route('/alerts/hospital/{hospitalId}', methods: ['GET'])]
    public function getCriticalAlerts(string $hospitalId, Request $request): JsonResponse
    {
        // TODO: Implement critical alerts retrieval
        // Optional query parameter: threshold (default 5 units)
        // Return blood types below threshold

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Update inventory status (expired, quarantined, etc.).
     */
    #[Route('/{inventoryId}/status', methods: ['PATCH'])]
    public function updateStatus(string $inventoryId, Request $request): JsonResponse
    {
        // TODO: Implement status update
        // Request body: { "status": "expired|quarantined|active" }

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get inventory statistics for hospital.
     */
    #[Route('/stats/hospital/{hospitalId}', methods: ['GET'])]
    public function getStatistics(string $hospitalId): JsonResponse
    {
        // TODO: Implement statistics calculation
        // Return total units, distribution by blood type, critical alerts count, etc.

        return new JsonResponse(ResponseUtil::success([]));
    }
}
