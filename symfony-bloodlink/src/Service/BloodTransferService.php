<?php

namespace App\Service;

use App\Entity\BloodTransferRequest;
use App\Repository\BloodTransferRequestRepository;
use App\Repository\HospitalRepository;
use App\Repository\BloodTypeRepository;
use App\Repository\HospitalStaffRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Uid\Uuid;

/**
 * Service class for blood transfer request workflow.
 * Handles creation, approval, rejection, and cancellation of blood transfers.
 */
class BloodTransferService
{
    public function __construct(
        private readonly BloodTransferRequestRepository $transferRepository,
        private readonly HospitalRepository $hospitalRepository,
        private readonly BloodTypeRepository $bloodTypeRepository,
        private readonly HospitalStaffRepository $hospitalStaffRepository,
        private readonly BloodInventoryService $inventoryService,
        private readonly EntityManagerInterface $entityManager,
    ) {}

    /**
     * Create a blood transfer request.
     *
     * @param array $requestData Transfer request data (requesting_hospital_id, approving_hospital_id, blood_type, units_needed, requesting_staff_id, reason, notes, delivery_expected_at)
     *
     * @return BloodTransferRequest
     *
     * @throws \InvalidArgumentException If validation fails
     */
    public function createTransferRequest(
        array $requestData,
    ): BloodTransferRequest {
        // Validate required fields
        $this->validateTransferRequestData($requestData);

        // Fetch entities
        $requestingHospital = $this->hospitalRepository->find(
            $requestData["requesting_hospital_id"],
        );
        $approvingHospital = $this->hospitalRepository->find(
            $requestData["approving_hospital_id"],
        );
        $bloodType = $this->bloodTypeRepository->find(
            $requestData["blood_type"],
        );
        $requestingStaff = $this->hospitalStaffRepository->find(
            $requestData["requesting_staff_id"],
        );

        if (
            !$requestingHospital ||
            !$approvingHospital ||
            !$bloodType ||
            !$requestingStaff
        ) {
            throw new \InvalidArgumentException(
                "Hospital, blood type, or staff not found",
            );
        }

        // Create transfer request
        $transfer = new BloodTransferRequest();
        $transfer->setRequestingHospital($requestingHospital);
        $transfer->setApprovingHospital($approvingHospital);
        $transfer->setBloodType($bloodType);
        $transfer->setQuantityUnitsRequested($requestData["units_needed"]);
        $transfer->setRequestingStaff($requestingStaff);
        $transfer->setRequestingStaffId($requestData["requesting_staff_id"]);
        $transfer->setStatus("PENDING");
        $transfer->setRequestedAt(new \DateTime());

        if (!empty($requestData["reason"])) {
            $transfer->setReason($requestData["reason"]);
        }

        if (isset($requestData["notes"])) {
            $transfer->setNotes($requestData["notes"]);
        }

        if (!empty($requestData["delivery_expected_at"])) {
            $transfer->setDeliveryExpectedAt(
                new \DateTime($requestData["delivery_expected_at"]),
            );
        }

        $this->entityManager->persist($transfer);
        $this->entityManager->flush();

        return $transfer;
    }

    /**
     * Get all pending transfer requests.
     *
     * @return BloodTransferRequest[]
     */
    public function getPendingTransfers(): array
    {
        return $this->transferRepository
            ->createQueryBuilder("tr")
            ->where("tr.status = :status")
            ->setParameter("status", "PENDING")
            ->orderBy("tr.requestedAt", "DESC")
            ->getQuery()
            ->getResult();
    }

    /**
     * Get transfer requests for a hospital.
     *
     * @param string $hospitalId Hospital UUID
     * @param string|null $status Filter by status (PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED)
     *
     * @return BloodTransferRequest[]
     */
    public function getHospitalTransfers(
        string $hospitalId,
        ?string $status = null,
    ): array {
        $qb = $this->transferRepository
            ->createQueryBuilder("tr")
            ->where("tr.requestingHospital = :hospitalId")
            ->setParameter("hospitalId", $hospitalId);

        if ($status) {
            $qb->andWhere("tr.status = :status")->setParameter(
                "status",
                strtoupper($status),
            );
        }

        return $qb->orderBy("tr.requestedAt", "DESC")->getQuery()->getResult();
    }

    /**
     * Get transfer request by ID.
     *
     * @param int $transferId Transfer request ID
     *
     * @return BloodTransferRequest|null
     */
    public function getTransferById(int $transferId): ?BloodTransferRequest
    {
        return $this->transferRepository->find($transferId);
    }

    /**
     * Approve a blood transfer request.
     *
     * @param int $transferId Transfer request ID
     * @param string $approverStaffId Hospital staff ID approving the request
     * @param int|null $quantityApproved Quantity approved (optional, defaults to requested)
     *
     * @return BloodTransferRequest
     *
     * @throws \InvalidArgumentException If validation fails
     * @throws \Exception If insufficient blood available
     */
    public function approveTransfer(
        int $transferId,
        string $approverStaffId,
        ?int $quantityApproved = null,
    ): BloodTransferRequest {
        // Find transfer request
        $transfer = $this->transferRepository->find($transferId);
        if (!$transfer) {
            throw new \InvalidArgumentException(
                "Transfer request not found with ID: {$transferId}",
            );
        }

        // Check current status is 'PENDING'
        if ($transfer->getStatus() !== "PENDING") {
            throw new \InvalidArgumentException(
                "Cannot approve transfer with status: {$transfer->getStatus()}",
            );
        }

        // Get approver staff
        $approvingStaff = $this->hospitalStaffRepository->find(
            $approverStaffId,
        );
        if (!$approvingStaff) {
            throw new \InvalidArgumentException(
                "Approver staff not found with ID: {$approverStaffId}",
            );
        }

        if ($transfer->getRequestingStaffId() === $approverStaffId) {
            throw new \InvalidArgumentException(
                "Request creators cannot approve their own transfer",
            );
        }

        // Determine quantity to approve
        $qtyToApprove =
            $quantityApproved ?? $transfer->getQuantityUnitsRequested();

        // Validate quantity
        if ($qtyToApprove <= 0) {
            throw new \InvalidArgumentException(
                "Approved quantity must be greater than 0",
            );
        }

        if ($qtyToApprove > $transfer->getQuantityUnitsRequested()) {
            throw new \InvalidArgumentException(
                "Approved quantity cannot exceed requested quantity",
            );
        }

        $approvingHospital = $approvingStaff->getHospital();
        if (
            $transfer->getApprovingHospital() &&
            $approvingHospital &&
            $transfer->getApprovingHospital()->getHospitalId() !==
                $approvingHospital->getHospitalId()
        ) {
            throw new \InvalidArgumentException(
                "Approver does not belong to the supplying hospital",
            );
        }

        if (!$transfer->getApprovingHospital() && $approvingHospital) {
            $transfer->setApprovingHospital($approvingHospital);
        }

        // Update transfer
        $transfer->setStatus("APPROVED");
        $transfer->setQuantityUnitsApproved($qtyToApprove);
        $transfer->setApprovedAt(new \DateTime());
        $transfer->setApprovingStaff($approvingStaff);
        $transfer->setApprovingStaffId($approverStaffId);

        if ($transfer->getDeliveryExpectedAt() === null) {
            $transfer->setDeliveryExpectedAt(new \DateTime("+3 days"));
        }

        $this->entityManager->flush();

        return $transfer;
    }

    /**
     * Reject a blood transfer request.
     *
     * @param int $transferId Transfer request ID
     * @param string $rejectorStaffId Hospital staff ID rejecting the request
     * @param string $rejectionReason Reason for rejection
     *
     * @return BloodTransferRequest
     *
     * @throws \InvalidArgumentException
     */
    public function rejectTransfer(
        int $transferId,
        string $rejectorStaffId,
        string $rejectionReason,
    ): BloodTransferRequest {
        // Find transfer request
        $transfer = $this->transferRepository->find($transferId);
        if (!$transfer) {
            throw new \InvalidArgumentException(
                "Transfer request not found with ID: {$transferId}",
            );
        }

        // Check current status is 'PENDING'
        if ($transfer->getStatus() !== "PENDING") {
            throw new \InvalidArgumentException(
                "Cannot reject transfer with status: {$transfer->getStatus()}",
            );
        }

        // Get rejector staff
        $rejectorStaff = $this->hospitalStaffRepository->find($rejectorStaffId);
        if (!$rejectorStaff) {
            throw new \InvalidArgumentException(
                "Rejector staff not found with ID: {$rejectorStaffId}",
            );
        }

        // Update transfer
        $transfer->setStatus("DENIED");
        $transfer->setReason($rejectionReason);
        $transfer->setApprovedAt(new \DateTime());
        $transfer->setApprovingStaff($rejectorStaff);
        $transfer->setApprovingStaffId($rejectorStaffId);

        $this->entityManager->flush();

        return $transfer;
    }

    /**
     * Cancel a blood transfer request.
     *
     * @param int $transferId Transfer request ID
     * @param string $cancellationReason Reason for cancellation
     *
     * @return BloodTransferRequest
     *
     * @throws \InvalidArgumentException
     */
    public function cancelTransfer(
        int $transferId,
        string $cancellationReason,
    ): BloodTransferRequest {
        // Find transfer request
        $transfer = $this->transferRepository->find($transferId);
        if (!$transfer) {
            throw new \InvalidArgumentException(
                "Transfer request not found with ID: {$transferId}",
            );
        }

        // Check status allows cancellation (pending only)
        if ($transfer->getStatus() !== "PENDING") {
            throw new \InvalidArgumentException(
                "Cannot cancel transfer with status: {$transfer->getStatus()}",
            );
        }

        // Update transfer
        $transfer->setStatus("CANCELLED");
        $transfer->setReason($cancellationReason);

        $this->entityManager->flush();

        return $transfer;
    }

    /**
     * Complete a transfer request.
     *
     * @param int $transferId Transfer request ID
     *
     * @return BloodTransferRequest
     *
     * @throws \InvalidArgumentException
     */
    public function completeTransfer(int $transferId): BloodTransferRequest
    {
        // Find transfer request
        $transfer = $this->transferRepository->find($transferId);
        if (!$transfer) {
            throw new \InvalidArgumentException(
                "Transfer request not found with ID: {$transferId}",
            );
        }

        // Check status is 'IN_TRANSIT'
        if ($transfer->getStatus() !== "IN_TRANSIT") {
            throw new \InvalidArgumentException(
                "Cannot complete transfer with status: {$transfer->getStatus()}",
            );
        }

        $requestingHospital = $transfer->getRequestingHospital();
        $approvingHospital = $transfer->getApprovingHospital();
        $bloodType = $transfer->getBloodType();
        $qtyToDeliver =
            $transfer->getQuantityUnitsApproved() ??
            $transfer->getQuantityUnitsRequested();

        if (!$requestingHospital || !$approvingHospital || !$bloodType) {
            throw new \InvalidArgumentException(
                "Transfer is missing hospital or blood type information",
            );
        }

        $bloodTypeId = $bloodType->getBloodTypeId();

        if (
            !$this->inventoryService->hasAvailableBlood(
                $approvingHospital->getHospitalId(),
                $bloodTypeId,
                $qtyToDeliver,
            )
        ) {
            throw new \Exception(
                "Insufficient blood inventory at supplying hospital",
            );
        }

        $this->inventoryService->removeFromInventory(
            $approvingHospital->getHospitalId(),
            $bloodTypeId,
            $qtyToDeliver,
        );

        $this->inventoryService->addToInventory(
            $requestingHospital->getHospitalId(),
            $bloodTypeId,
            $qtyToDeliver,
        );

        // Update transfer
        $transfer->setStatus("DELIVERED");
        $transfer->setActualDeliveryAt(new \DateTime());

        $this->entityManager->flush();

        return $transfer;
    }

    /**
     * Mark a transfer request as in transit.
     *
     * @param int $transferId Transfer request ID
     * @param string $staffId Hospital staff ID marking the transfer in transit
     *
     * @return BloodTransferRequest
     *
     * @throws \InvalidArgumentException
     */
    public function markInTransit(
        int $transferId,
        string $staffId,
    ): BloodTransferRequest {
        $transfer = $this->transferRepository->find($transferId);
        if (!$transfer) {
            throw new \InvalidArgumentException(
                "Transfer request not found with ID: {$transferId}",
            );
        }

        if ($transfer->getStatus() !== "APPROVED") {
            throw new \InvalidArgumentException(
                "Cannot mark in transit with status: {$transfer->getStatus()}",
            );
        }

        $staff = $this->hospitalStaffRepository->find($staffId);
        if (!$staff) {
            throw new \InvalidArgumentException(
                "Staff not found with ID: {$staffId}",
            );
        }

        $approvingHospital = $transfer->getApprovingHospital();
        if (
            $approvingHospital &&
            $staff->getHospital() &&
            $staff->getHospital()->getHospitalId() !==
                $approvingHospital->getHospitalId()
        ) {
            throw new \InvalidArgumentException(
                "Staff does not belong to the supplying hospital",
            );
        }

        if (!$approvingHospital && $staff->getHospital()) {
            $transfer->setApprovingHospital($staff->getHospital());
        }

        $transfer->setStatus("IN_TRANSIT");
        $this->entityManager->flush();

        return $transfer;
    }

    /**
     * Get transfer history for a hospital.
     *
     * @param string $hospitalId Hospital UUID
     * @param int $limit Limit results
     *
     * @return BloodTransferRequest[]
     */
    public function getTransferHistory(
        string $hospitalId,
        int $limit = 50,
    ): array {
        return $this->transferRepository
            ->createQueryBuilder("tr")
            ->where("tr.requestingHospital = :hospitalId")
            ->orWhere("tr.approvingHospital = :hospitalId")
            ->setParameter("hospitalId", $hospitalId)
            ->orderBy("tr.requestedAt", "DESC")
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * Get pending transfers for a hospital (as approver/receiver).
     *
     * @param string $hospitalId Hospital UUID
     *
     * @return BloodTransferRequest[]
     */
    public function getPendingTransfersForHospital(string $hospitalId): array
    {
        // Get staff members of this hospital
        $staffMembers = $this->hospitalStaffRepository
            ->createQueryBuilder("hs")
            ->where("hs.hospital = :hospitalId")
            ->setParameter("hospitalId", $hospitalId)
            ->getQuery()
            ->getResult();

        $staffIds = array_map(fn($staff) => $staff->getUserId(), $staffMembers);

        if (empty($staffIds)) {
            return [];
        }

        // Get pending transfers for these staff
        return $this->transferRepository
            ->createQueryBuilder("tr")
            ->where("tr.status = :status")
            ->andWhere("tr.requestingStaffId IN (:staffIds)")
            ->setParameter("status", "PENDING")
            ->setParameter("staffIds", $staffIds)
            ->orderBy("tr.requestedAt", "DESC")
            ->getQuery()
            ->getResult();
    }

    /**
     * Validate transfer request data.
     *
     * @param array $data Transfer request data
     *
     * @throws \InvalidArgumentException
     */
    private function validateTransferRequestData(array $data): void
    {
        // Check required fields
        $requiredFields = [
            "requesting_hospital_id",
            "approving_hospital_id",
            "blood_type",
            "units_needed",
            "requesting_staff_id",
            "reason",
        ];

        foreach ($requiredFields as $field) {
            if (!isset($data[$field]) || empty($data[$field])) {
                throw new \InvalidArgumentException("{$field} is required");
            }
        }

        // Validate units needed
        $units = $data["units_needed"];
        if (!is_int($units) || $units <= 0) {
            throw new \InvalidArgumentException(
                "Units needed must be a positive integer",
            );
        }

        // Validate units don't exceed reasonable limits
        if ($units > 100) {
            throw new \InvalidArgumentException(
                "Units needed cannot exceed 100",
            );
        }
    }
}
