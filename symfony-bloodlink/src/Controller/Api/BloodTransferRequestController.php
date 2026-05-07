<?php

namespace App\Controller\Api;

use App\Service\BloodTransferService;
use App\Service\HospitalService;
use App\Service\AccessControlService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for blood transfer request management.
 */
#[Route("/api/transfers", name: "api_transfers_")]
class BloodTransferRequestController extends AbstractController
{
    public function __construct(
        private readonly BloodTransferService $transferService,
        private readonly HospitalService $hospitalService,
        private readonly AccessControlService $accessControlService,
    ) {}

    /**
     * Get all pending transfer requests.
     */
    #[Route("", methods: ["GET"])]
    public function listPendingTransfers(Request $request): JsonResponse
    {
        try {
            $status = $request->query->get("status", "PENDING");

            if ($status === "PENDING") {
                $transfers = $this->transferService->getPendingTransfers();
            } else {
                // For other statuses, we need to get all and filter
                // This is a simplified approach
                $transfers = $this->transferService->getPendingTransfers();
            }

            $transfersData = array_map(function ($transfer) {
                return $this->transferToArray($transfer);
            }, $transfers);

            return new JsonResponse(
                ResponseUtil::success(
                    $transfersData,
                    "Pending transfers retrieved successfully",
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve transfers: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get transfer requests for a hospital.
     */
    #[Route("/hospital/{hospitalId}", methods: ["GET"])]
    public function getHospitalTransfers(
        string $hospitalId,
        Request $request,
    ): JsonResponse {
        try {
            // Verify hospital exists
            $hospital = $this->hospitalService->getHospitalById($hospitalId);
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Get optional status filter
            $status = $request->query->get("status");

            // Get transfers for hospital
            $transfers = $this->transferService->getHospitalTransfers(
                $hospitalId,
                $status,
            );

            $transfersData = array_map(function ($transfer) {
                return $this->transferToArray($transfer);
            }, $transfers);

            return new JsonResponse(
                ResponseUtil::success(
                    $transfersData,
                    "Hospital transfers retrieved successfully",
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve hospital transfers: " .
                        $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Create a blood transfer request.
     */
    #[Route("", methods: ["POST"])]
    public function createTransfer(Request $request): JsonResponse
    {
        try {
            // Extract JSON data
            $data = json_decode($request->getContent(), true);

            if (!$data) {
                return new JsonResponse(
                    ResponseUtil::error("Invalid JSON data"),
                    400,
                );
            }

            // Validate required fields
            $requiredFields = [
                "requesting_hospital_id",
                "approving_hospital_id",
                "blood_type_id",
                "units_requested",
                "requesting_staff_id",
                "reason",
            ];
            $errors = [];

            foreach ($requiredFields as $field) {
                if (!isset($data[$field]) || $data[$field] === "") {
                    $errors[$field] = "{$field} is required";
                }
            }

            if (!empty($errors)) {
                return new JsonResponse(
                    ResponseUtil::error("Validation failed", $errors, 400),
                    400,
                );
            }

            // Validate hospitals exist
            $hospital = $this->hospitalService->getHospitalById(
                $data["requesting_hospital_id"],
            );
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Requesting hospital not found",
                        null,
                        404,
                    ),
                    404,
                );
            }

            // Check if requesting hospital is active
            try {
                $this->accessControlService->validateHospitalActive(
                    $data["requesting_hospital_id"],
                );
            } catch (\Exception $e) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Cannot create transfer: " . $e->getMessage(),
                        null,
                        403,
                    ),
                    403,
                );
            }

            $approvingHospital = $this->hospitalService->getHospitalById(
                $data["approving_hospital_id"],
            );
            if (!$approvingHospital) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Supplying hospital not found",
                        null,
                        404,
                    ),
                    404,
                );
            }

            // Check if supplying hospital is active
            try {
                $this->accessControlService->validateHospitalActive(
                    $data["approving_hospital_id"],
                );
            } catch (\Exception $e) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Supplying hospital is not available: " . $e->getMessage(),
                        null,
                        403,
                    ),
                    403,
                );
            }

            if (
                $data["approving_hospital_id"] ===
                $data["requesting_hospital_id"]
            ) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Supplying hospital must be different from requesting hospital",
                        null,
                        400,
                    ),
                    400,
                );
            }

            // Validate units requested
            if (
                !is_int($data["units_requested"]) ||
                $data["units_requested"] <= 0
            ) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Units requested must be a positive integer",
                    ),
                    400,
                );
            }

            // Create transfer request
            $transfer = $this->transferService->createTransferRequest([
                "requesting_hospital_id" => $data["requesting_hospital_id"],
                "approving_hospital_id" => $data["approving_hospital_id"],
                "blood_type" => $data["blood_type_id"],
                "units_needed" => $data["units_requested"],
                "requesting_staff_id" => $data["requesting_staff_id"],
                "reason" => $data["reason"],
                "notes" => $data["notes"] ?? null,
                "delivery_expected_at" => $data["delivery_expected_at"] ?? null,
            ]);

            return new JsonResponse(
                ResponseUtil::success(
                    $this->transferToArray($transfer),
                    "Transfer request created successfully",
                ),
                201,
            );
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error("Validation error: " . $e->getMessage()),
                400,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to create transfer request: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get transfer request details.
     */
    #[Route("/{transferId}", methods: ["GET"])]
    public function getTransfer(string $transferId): JsonResponse
    {
        try {
            // Get transfer
            $transfer = $this->transferService->getTransferById(
                (int) $transferId,
            );

            if (!$transfer) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Transfer request not found",
                        null,
                        404,
                    ),
                    404,
                );
            }

            return new JsonResponse(
                ResponseUtil::success(
                    $this->transferToArray($transfer),
                    "Transfer request retrieved successfully",
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve transfer request: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Approve a transfer request.
     */
    #[Route("/{transferId}/approve", methods: ["POST"])]
    public function approveTransfer(
        string $transferId,
        Request $request,
    ): JsonResponse {
        try {
            // Extract JSON data
            $data = json_decode($request->getContent(), true);

            if (!$data) {
                return new JsonResponse(
                    ResponseUtil::error("Invalid JSON data"),
                    400,
                );
            }

            // Validate required fields
            if (
                !isset($data["approver_staff_id"]) ||
                empty($data["approver_staff_id"])
            ) {
                return new JsonResponse(
                    ResponseUtil::error("approver_staff_id is required"),
                    400,
                );
            }

            // Approve transfer
            $transfer = $this->transferService->approveTransfer(
                (int) $transferId,
                $data["approver_staff_id"],
                $data["quantity_approved"] ?? null,
            );

            return new JsonResponse(
                ResponseUtil::success(
                    $this->transferToArray($transfer),
                    "Transfer request approved successfully",
                ),
            );
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error("Validation error: " . $e->getMessage()),
                400,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to approve transfer request: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Reject a transfer request.
     */
    #[Route("/{transferId}/reject", methods: ["POST"])]
    public function rejectTransfer(
        string $transferId,
        Request $request,
    ): JsonResponse {
        try {
            // Extract JSON data
            $data = json_decode($request->getContent(), true);

            if (!$data) {
                return new JsonResponse(
                    ResponseUtil::error("Invalid JSON data"),
                    400,
                );
            }

            // Validate required fields
            if (
                !isset($data["rejector_staff_id"]) ||
                empty($data["rejector_staff_id"])
            ) {
                return new JsonResponse(
                    ResponseUtil::error("rejector_staff_id is required"),
                    400,
                );
            }

            if (!isset($data["reason"]) || empty($data["reason"])) {
                return new JsonResponse(
                    ResponseUtil::error("reason is required"),
                    400,
                );
            }

            // Reject transfer
            $transfer = $this->transferService->rejectTransfer(
                (int) $transferId,
                $data["rejector_staff_id"],
                $data["reason"],
            );

            return new JsonResponse(
                ResponseUtil::success(
                    $this->transferToArray($transfer),
                    "Transfer request rejected successfully",
                ),
            );
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error("Validation error: " . $e->getMessage()),
                400,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to reject transfer request: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Cancel a transfer request.
     */
    #[Route("/{transferId}/cancel", methods: ["POST"])]
    public function cancelTransfer(
        string $transferId,
        Request $request,
    ): JsonResponse {
        try {
            // Extract JSON data
            $data = json_decode($request->getContent(), true);

            if (!$data) {
                return new JsonResponse(
                    ResponseUtil::error("Invalid JSON data"),
                    400,
                );
            }

            // Validate required fields
            if (!isset($data["reason"]) || empty($data["reason"])) {
                return new JsonResponse(
                    ResponseUtil::error("reason is required"),
                    400,
                );
            }

            // Cancel transfer
            $transfer = $this->transferService->cancelTransfer(
                (int) $transferId,
                $data["reason"],
            );

            return new JsonResponse(
                ResponseUtil::success(
                    $this->transferToArray($transfer),
                    "Transfer request cancelled successfully",
                ),
            );
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error("Validation error: " . $e->getMessage()),
                400,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to cancel transfer request: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Mark transfer as completed.
     */
    #[Route("/{transferId}/complete", methods: ["POST"])]
    public function completeTransfer(string $transferId): JsonResponse
    {
        try {
            // Complete transfer
            $transfer = $this->transferService->completeTransfer(
                (int) $transferId,
            );

            return new JsonResponse(
                ResponseUtil::success(
                    $this->transferToArray($transfer),
                    "Transfer request completed successfully",
                ),
            );
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error("Validation error: " . $e->getMessage()),
                400,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to complete transfer request: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get transfer history for hospital.
     */
    #[Route("/hospital/{hospitalId}/history", methods: ["GET"])]
    public function getTransferHistory(
        string $hospitalId,
        Request $request,
    ): JsonResponse {
        try {
            // Verify hospital exists
            $hospital = $this->hospitalService->getHospitalById($hospitalId);
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Get limit from query parameter
            $limit = min(100, max(1, (int) $request->query->get("limit", 50)));

            // Get transfer history
            $transfers = $this->transferService->getTransferHistory(
                $hospitalId,
                $limit,
            );

            $transfersData = array_map(function ($transfer) {
                return $this->transferToArray($transfer);
            }, $transfers);

            return new JsonResponse(
                ResponseUtil::success(
                    $transfersData,
                    "Transfer history retrieved successfully",
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve transfer history: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Convert transfer entity to array for response.
     */
    private function transferToArray($transfer): array
    {
        return [
            "transferId" => $transfer->getTransferId(),
            "requestingHospital" => [
                "hospitalId" => $transfer
                    ->getRequestingHospital()
                    ?->getHospitalId(),
                "name" => $transfer->getRequestingHospital()?->getName(),
            ],
            "approvingHospital" => [
                "hospitalId" => $transfer
                    ->getApprovingHospital()
                    ?->getHospitalId(),
                "name" => $transfer->getApprovingHospital()?->getName(),
            ],
            "bloodType" => [
                "bloodTypeId" => $transfer->getBloodType()?->getBloodTypeId(),
                "name" => $transfer->getBloodType()?->getName(),
            ],
            "quantityUnitsRequested" => $transfer->getQuantityUnitsRequested(),
            "quantityUnitsApproved" => $transfer->getQuantityUnitsApproved(),
            "status" => $transfer->getStatus(),
            "reason" => $transfer->getReason(),
            "requestingStaff" => [
                "staffId" => $transfer->getRequestingStaff()?->getUserId(),
                "name" => $transfer->getRequestingStaff()
                    ? $transfer->getRequestingStaff()->getFirstName() .
                    " " .
                    $transfer->getRequestingStaff()->getLastName()
                    : null,
            ],
            "approvingStaff" => [
                "staffId" => $transfer->getApprovingStaff()?->getUserId(),
                "name" => $transfer->getApprovingStaff()
                    ? $transfer->getApprovingStaff()->getFirstName() .
                    " " .
                    $transfer->getApprovingStaff()->getLastName()
                    : null,
            ],
            "requestedAt" => $transfer
                ->getRequestedAt()
                ?->format("Y-m-d H:i:s"),
            "approvedAt" => $transfer->getApprovedAt()?->format("Y-m-d H:i:s"),
            "deliveryExpectedAt" => $transfer
                ->getDeliveryExpectedAt()
                ?->format("Y-m-d H:i:s"),
            "actualDeliveryAt" => $transfer
                ->getActualDeliveryAt()
                ?->format("Y-m-d H:i:s"),
            "notes" => $transfer->getNotes(),
        ];
    }
}
