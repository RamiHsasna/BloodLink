<?php

namespace App\Controller\Api;

use App\Service\HospitalService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for hospital endpoints.
 */
#[Route('/api/hospitals', name: 'api_hospitals_')]
class HospitalController extends AbstractController
{
    public function __construct(
        private readonly HospitalService $hospitalService,
    ) {
    }

    /**
     * Get all hospitals.
     */
    #[Route('', methods: ['GET'])]
    public function listHospitals(Request $request): JsonResponse
    {
        // TODO: Implement hospital listing
        // 1. Extract pagination parameters from request
        // 2. Call HospitalService::getAllHospitals()
        // 3. Format response using ResponseUtil::paginated()
        // 4. Return JsonResponse

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get hospital by ID.
     */
    #[Route('/{hospitalId}', methods: ['GET'])]
    public function getHospital(string $hospitalId): JsonResponse
    {
        // TODO: Implement hospital retrieval
        // 1. Call HospitalService::getHospitalById()
        // 2. Return 404 if not found
        // 3. Return hospital data

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Create a new hospital.
     */
    #[Route('', methods: ['POST'])]
    public function createHospital(Request $request): JsonResponse
    {
        // TODO: Implement hospital creation
        // 1. Extract JSON data from request
        // 2. Validate required fields (name, address, city, etc.)
        // 3. Call HospitalService::createHospital()
        // 4. Return 201 Created with hospital data
        // 5. Handle validation errors

        return new JsonResponse(ResponseUtil::success([]), 201);
    }

    /**
     * Update hospital information.
     */
    #[Route('/{hospitalId}', methods: ['PUT', 'PATCH'])]
    public function updateHospital(string $hospitalId, Request $request): JsonResponse
    {
        // TODO: Implement hospital update
        // 1. Verify hospital exists
        // 2. Extract JSON data
        // 3. Validate fields
        // 4. Call HospitalService::updateHospital()
        // 5. Return updated hospital data

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Delete a hospital.
     */
    #[Route('/{hospitalId}', methods: ['DELETE'])]
    public function deleteHospital(string $hospitalId): JsonResponse
    {
        // TODO: Implement hospital deletion
        // 1. Check if hospital can be deleted
        // 2. Call HospitalService::deleteHospital()
        // 3. Return 204 No Content on success

        return new JsonResponse(null, 204);
    }

    /**
     * Find hospitals near a location.
     */
    #[Route('/nearby', methods: ['GET'])]
    public function findNearbyHospitals(Request $request): JsonResponse
    {
        // TODO: Implement nearby search
        // 1. Extract latitude, longitude, radius from query parameters
        // 2. Validate coordinates
        // 3. Call HospitalService::findNearbyHospitals()
        // 4. Return sorted results

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get hospital blood inventory.
     */
    #[Route('/{hospitalId}/inventory', methods: ['GET'])]
    public function getInventory(string $hospitalId): JsonResponse
    {
        // TODO: Implement inventory retrieval
        // Related to BloodInventoryController but scoped to hospital
        // Call appropriate inventory service method

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Activate/deactivate a hospital.
     */
    #[Route('/{hospitalId}/status', methods: ['PATCH'])]
    public function updateStatus(string $hospitalId, Request $request): JsonResponse
    {
        // TODO: Implement status update
        // Extract isActive flag and update

        return new JsonResponse(ResponseUtil::success([]));
    }
}
