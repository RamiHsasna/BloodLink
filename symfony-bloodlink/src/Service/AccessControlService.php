<?php

namespace App\Service;

use App\Entity\Hospital;
use App\Repository\HospitalRepository;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;

/**
 * Service for managing access control based on hospital status.
 */
class AccessControlService
{
    public function __construct(
        private readonly HospitalRepository $hospitalRepository,
        private readonly HospitalService $hospitalService,
    ) {}

    /**
     * Validate that a hospital can perform operations (is currently active).
     *
     * @param string $hospitalId Hospital UUID
     *
     * @throws AccessDeniedHttpException If hospital is deactivated
     */
    public function validateHospitalActive(string $hospitalId): void
    {
        $hospital = $this->hospitalRepository->find($hospitalId);
        if (!$hospital) {
            throw new AccessDeniedHttpException("Hospital not found");
        }

        // Auto-check and reactivate if expired
        $this->hospitalService->checkAndAutoReactivateIfExpired($hospitalId);

        // Refresh from DB
        $hospital = $this->hospitalRepository->find($hospitalId);

        // Check if currently active
        if (!$hospital->isCurrentlyActive()) {
            $reason = $hospital->getDeactivationReason() ?? "Unknown";
            $endDate = $hospital->getDeactivationEndDate();
            $endDateStr = $endDate ? $endDate->format('Y-m-d H:i') : "indefinitely";

            throw new AccessDeniedHttpException(
                "Hospital is currently inactive until {$endDateStr}. Reason: {$reason}"
            );
        }
    }

    /**
     * Validate that a hospital can perform a specific operation.
     *
     * @param Hospital $hospital Hospital entity
     *
     * @throws AccessDeniedHttpException If hospital is deactivated
     */
    public function validateHospitalForOperation(Hospital $hospital): void
    {
        // Auto-check and reactivate if expired
        $this->hospitalService->checkAndAutoReactivateIfExpired(
            $hospital->getHospitalId(),
        );

        // Refresh
        $hospital = $this->hospitalRepository->find($hospital->getHospitalId());

        if (!$hospital->isCurrentlyActive()) {
            $reason = $hospital->getDeactivationReason() ?? "Unknown";
            $endDate = $hospital->getDeactivationEndDate();
            $endDateStr = $endDate ? $endDate->format('Y-m-d H:i') : "indefinitely";

            throw new AccessDeniedHttpException(
                "Your hospital is currently inactive until {$endDateStr}. Reason: {$reason}. Contact admin to reactivate."
            );
        }
    }

    /**
     * Get deactivation info for a hospital (for display purposes).
     *
     * @param Hospital $hospital Hospital entity
     *
     * @return array|null Array with 'reason', 'endDate', and 'daysRemaining' or null if active
     */
    public function getDeactivationInfo(Hospital $hospital): ?array
    {
        if ($hospital->getIsActive() && $hospital->getDeactivationEndDate() === null) {
            return null; // Hospital is fully active
        }

        $reason = $hospital->getDeactivationReason();
        $endDate = $hospital->getDeactivationEndDate();

        if ($endDate === null) {
            // Indefinite deactivation
            return [
                'reason' => $reason ?? 'Hospital is deactivated indefinitely',
                'endDate' => null,
                'daysRemaining' => null,
                'isIndefinite' => true,
            ];
        }

        $now = new \DateTime();
        $interval = $now->diff($endDate);
        $daysRemaining = (int) $interval->format('%r%a'); // Negative if expired

        return [
            'reason' => $reason,
            'endDate' => $endDate,
            'daysRemaining' => max(0, $daysRemaining),
            'isIndefinite' => false,
            'isExpired' => $daysRemaining < 0,
        ];
    }

    /**
     * Check if staff can login (always allow - they'll see read-only mode if hospital inactive)
     *
     * @param Hospital $hospital Hospital entity
     *
     * @return bool Always returns true
     */
    public function canStaffLogin(Hospital $hospital): bool
    {
        // Per requirements: staff can always login (operations blocked server-side)
        return true;
    }
}
