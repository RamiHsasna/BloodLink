<?php

namespace App\Controller\Api;

use App\Service\DonorService;
use App\Service\EligibilityService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for donor endpoints.
 */
#[Route('/api/donors', name: 'api_donors_')]
class DonorController extends AbstractController
{
    public function __construct(
        private readonly DonorService $donorService,
        private readonly EligibilityService $eligibilityService,
    ) {
    }

    /**
     * Get all donors.
     */
    #[Route('', methods: ['GET'])]
    public function listDonors(Request $request): JsonResponse
    {
        // TODO: Implement donor listing with pagination
        // Optional filters: blood_type, eligible, etc.

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get donor by ID.
     */
    #[Route('/{donorId}', methods: ['GET'])]
    public function getDonor(string $donorId): JsonResponse
    {
        // TODO: Implement donor retrieval
        // Include eligibility status

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Create a new donor profile.
     */
    #[Route('', methods: ['POST'])]
    public function createDonor(Request $request): JsonResponse
    {
        // TODO: Implement donor creation
        // 1. Validate input (firstName, lastName, bloodType, location, etc.)
        // 2. Call DonorService::createDonor()
        // 3. Return 201 Created

        return new JsonResponse(ResponseUtil::success([]), 201);
    }

    /**
     * Update donor information.
     */
    #[Route('/{donorId}', methods: ['PUT', 'PATCH'])]
    public function updateDonor(string $donorId, Request $request): JsonResponse
    {
        // TODO: Implement donor update
        // Note: Updating location or blood type may invalidate eligibility cache

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get donor eligibility status.
     */
    #[Route('/{donorId}/eligibility', methods: ['GET'])]
    public function getDonorEligibility(string $donorId): JsonResponse
    {
        // TODO: Implement eligibility retrieval
        // Return:
        // - isCurrentlyEligible
        // - daysUntilEligible
        // - lastCalculatedAt
        // - reason

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Find eligible donors by blood type.
     */
    #[Route('/search/by-blood-type', methods: ['GET'])]
    public function findByBloodType(Request $request): JsonResponse
    {
        // TODO: Implement search
        // Query parameter: blood_type (required)
        // Return eligible donors with specified blood type

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Find eligible donors near a location.
     */
    #[Route('/search/nearby', methods: ['GET'])]
    public function findNearbyEligible(Request $request): JsonResponse
    {
        // TODO: Implement nearby donor search
        // Query parameters:
        // - blood_type (required)
        // - latitude (required)
        // - longitude (required)
        // - radius_km (optional, default 50)
        // Return eligible donors sorted by proximity

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get donor donations history.
     */
    #[Route('/{donorId}/donations', methods: ['GET'])]
    public function getDonationHistory(string $donorId): JsonResponse
    {
        // TODO: Implement donation history retrieval

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Get donor alerts received.
     */
    #[Route('/{donorId}/alerts', methods: ['GET'])]
    public function getAlerts(string $donorId): JsonResponse
    {
        // TODO: Implement alerts retrieval

        return new JsonResponse(ResponseUtil::success([]));
    }

    /**
     * Check donor eligibility status (recalculate).
     */
    #[Route('/{donorId}/check-eligibility', methods: ['POST'])]
    public function checkEligibility(string $donorId): JsonResponse
    {
        // TODO: Implement eligibility check/recalculation
        // Force recalculation rather than using cached status

        return new JsonResponse(ResponseUtil::success([]));
    }
}
