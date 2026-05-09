<?php

namespace App\Service;

use App\Entity\Donor;
use App\Entity\DonorEligibility;
use App\Repository\DonorEligibilityRepository;
use Doctrine\ORM\EntityManagerInterface;

/**
 * Service class for donor eligibility checks and management.
 * Implements business logic for determining if a donor can donate.
 */
class EligibilityService
{
    private const MINIMUM_DONATION_INTERVAL_DAYS = 56; // 8 weeks for blood donation

    public function __construct(
        private readonly DonorEligibilityRepository $eligibilityRepository,
        private readonly EntityManagerInterface $entityManager,
    ) {
    }

    /**
     * Check if a donor is currently eligible to donate.
     *
     * @param Donor $donor Donor entity
     *
     * @return bool True if eligible
     */
    public function isDonorEligible(Donor $donor): bool
    {
        // TODO: Implement eligibility check
        // Criteria to check:
        // 1. No medical conditions preventing donation
        // 2. Sufficient time passed since last donation (minimum 56 days)
        // 3. Within age range (typically 17-65 years)
        // 4. Adequate hemoglobin levels
        // 5. No blood pressure issues
        // 6. Not pregnant/nursing
        // 7. No recent vaccinations
        // 8. No recent travels to high-risk areas

        return false;
    }

    /**
     * Calculate days until donor becomes eligible again.
     *
     * @param Donor $donor Donor entity
     *
     * @return int Days until eligible (0 if already eligible)
     */
    public function getDaysUntilEligible(Donor $donor): int
    {
        // TODO: Implement calculation
        // If last donation exists:
        //   - Calculate date = lastDonationDate + MINIMUM_DONATION_INTERVAL_DAYS
        //   - Calculate days remaining = date - today
        //   - Return max(0, days_remaining)
        // Otherwise return 0

        return 0;
    }

    /**
     * Update donor eligibility status.
     *
     * @param string $donorId Donor ID
     * @param bool $isEligible New eligibility status
     * @param string|null $details Additional eligibility details/notes
     *
     * @return DonorEligibility
     */
    public function updateEligibility(
        string $donorId,
        bool $isEligible,
        ?string $details = null,
    ): DonorEligibility {
        // TODO: Implement eligibility update
        // 1. Find or create DonorEligibility record
        // 2. Update isCurrentlyEligible flag
        // 3. Set lastCalculatedAt timestamp
        // 4. Store eligibility details if provided
        // 5. Calculate daysUntilEligible
        // 6. Cache donor location and blood type
        // 7. Persist and return

        return new DonorEligibility();
    }

    /**
     * Recalculate eligibility for all donors.
     *
     * @return int Number of donors updated
     */
    public function recalculateAllEligibilities(): int
    {
        // TODO: Implement bulk recalculation
        // 1. Fetch all donors
        // 2. For each donor, check eligibility
        // 3. Update DonorEligibility records
        // 4. Persist changes
        // 5. Return count of updated donors

        return 0;
    }

    /**
     * Get eligibility reason (why donor is or isn't eligible).
     *
     * @param Donor $donor Donor entity
     *
     * @return string Human-readable reason
     */
    public function getEligibilityReason(Donor $donor): string
    {
        // TODO: Implement reason generation
        // Return descriptive message based on eligibility status and criteria

        return '';
    }

    /**
     * Get donors by eligibility status.
     *
     * @param bool $isEligible Filter by eligibility
     *
     * @return Donor[]
     */
    public function getDonorsByEligibility(bool $isEligible): array
    {
        // TODO: Implement query

        return [];
    }

    /**
     * Cache location and blood type in eligibility record.
     *
     * @param DonorEligibility $eligibility Eligibility record
     * @param Donor $donor Donor entity
     *
     * @return DonorEligibility
     */
    public function cacheLocationAndBloodType(DonorEligibility $eligibility, Donor $donor): DonorEligibility
    {
        // TODO: Implement caching
        // Store latitude, longitude, and blood type in eligibility record
        // Useful for fast searches without loading full Donor entity

        return $eligibility;
    }
}
