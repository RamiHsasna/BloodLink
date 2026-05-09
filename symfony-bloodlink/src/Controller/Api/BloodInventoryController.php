<?php

namespace App\Controller\Api;

use App\Service\BloodInventoryService;
use App\Service\HospitalService;
use App\Service\AccessControlService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for blood inventory management.
 */
#[Route("/api/inventory", name: "api_inventory_")]
class BloodInventoryController extends AbstractController
{
    public function __construct(
        private readonly BloodInventoryService $inventoryService,
        private readonly HospitalService $hospitalService,
        private readonly AccessControlService $accessControlService,
    ) {}

    /**
     * Get inventory for a hospital.
     */
    #[Route("/hospital/{hospitalId}", methods: ["GET"])]
    public function getHospitalInventory(string $hospitalId): JsonResponse
    {
        try {
            // Verify hospital exists
            $hospital = $this->hospitalService->getHospitalById($hospitalId);
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Get inventory for hospital
            $inventory = $this->inventoryService->getHospitalInventory(
                $hospitalId,
            );

            // Convert to arrays for response
            $inventoryData = array_map(function ($inv) {
                return [
                    "inventoryId" => $inv->getInventoryId() ?? null,
                    "hospitalId" => $inv->getHospital()->getHospitalId(),
                    "bloodType" => [
                        "bloodTypeId" => $inv->getBloodType()->getBloodTypeId(),
                        "name" => $inv->getBloodType()->getName(),
                    ],
                    "quantityUnits" => $inv->getQuantityUnits(),
                    "status" => $inv->getStatus(),
                    "updatedAt" => $inv->getUpdatedAt()?->format("Y-m-d H:i:s"),
                ];
            }, $inventory);

            return new JsonResponse(
                ResponseUtil::success(
                    $inventoryData,
                    "Inventory retrieved successfully",
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve inventory: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get stock level for a specific blood type at a hospital.
     */
    #[Route("/hospital/{hospitalId}/{bloodType}", methods: ["GET"])]
    public function getStockLevel(
        string $hospitalId,
        string $bloodType,
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

            // Get stock level
            $stockLevel = $this->inventoryService->getStockLevel(
                $hospitalId,
                $bloodType,
            );

            return new JsonResponse(
                ResponseUtil::success([
                    "hospitalId" => $hospitalId,
                    "bloodType" => $bloodType,
                    "quantityUnits" => $stockLevel,
                ]),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve stock level: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Add blood to inventory.
     */
    #[Route("/add", methods: ["POST"])]
    public function addToInventory(Request $request): JsonResponse
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
            $requiredFields = ["hospital_id", "blood_type", "units"];
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

            // Validate hospital exists
            $hospital = $this->hospitalService->getHospitalById(
                $data["hospital_id"],
            );
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Check if hospital is active
            try {
                $this->accessControlService->validateHospitalActive(
                    $data["hospital_id"],
                );
            } catch (\Exception $e) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Cannot add inventory: " . $e->getMessage(),
                        null,
                        403,
                    ),
                    403,
                );
            }

            // Validate units is positive integer
            if (!is_int($data["units"]) || $data["units"] <= 0) {
                return new JsonResponse(
                    ResponseUtil::error("Units must be a positive integer"),
                    400,
                );
            }

            // Add to inventory
            $inventory = $this->inventoryService->addToInventory(
                $data["hospital_id"],
                $data["blood_type"],
                $data["units"],
            );

            return new JsonResponse(
                ResponseUtil::success(
                    [
                        "inventoryId" => $inventory->getInventoryId() ?? null,
                        "hospitalId" => $inventory
                            ->getHospital()
                            ->getHospitalId(),
                        "bloodType" => $inventory
                            ->getBloodType()
                            ->getBloodTypeId(),
                        "quantityUnits" => $inventory->getQuantityUnits(),
                        "status" => $inventory->getStatus(),
                        "updatedAt" => $inventory
                            ->getUpdatedAt()
                            ?->format("Y-m-d H:i:s"),
                    ],
                    "Blood added to inventory successfully",
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
                    "Failed to add blood to inventory: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Remove blood from inventory.
     */
    #[Route("/remove", methods: ["POST"])]
    public function removeFromInventory(Request $request): JsonResponse
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
            $requiredFields = ["hospital_id", "blood_type", "units"];
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

            // Validate hospital exists
            $hospital = $this->hospitalService->getHospitalById(
                $data["hospital_id"],
            );
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Check if hospital is active
            try {
                $this->accessControlService->validateHospitalActive(
                    $data["hospital_id"],
                );
            } catch (\Exception $e) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Cannot remove inventory: " . $e->getMessage(),
                        null,
                        403,
                    ),
                    403,
                );
            }

            // Validate units is positive integer
            if (!is_int($data["units"]) || $data["units"] <= 0) {
                return new JsonResponse(
                    ResponseUtil::error("Units must be a positive integer"),
                    400,
                );
            }

            // Remove from inventory
            $inventory = $this->inventoryService->removeFromInventory(
                $data["hospital_id"],
                $data["blood_type"],
                $data["units"],
            );

            return new JsonResponse(
                ResponseUtil::success(
                    [
                        "inventoryId" => $inventory->getInventoryId() ?? null,
                        "hospitalId" => $inventory
                            ->getHospital()
                            ->getHospitalId(),
                        "bloodType" => $inventory
                            ->getBloodType()
                            ->getBloodTypeId(),
                        "quantityUnits" => $inventory->getQuantityUnits(),
                        "status" => $inventory->getStatus(),
                        "updatedAt" => $inventory
                            ->getUpdatedAt()
                            ?->format("Y-m-d H:i:s"),
                    ],
                    "Blood removed from inventory successfully",
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to remove blood from inventory: " .
                        $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get critical stock alerts.
     */
    #[Route("/alerts/hospital/{hospitalId}", methods: ["GET"])]
    public function getCriticalAlerts(
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

            // Get threshold from query parameter (default 5)
            $threshold = max(1, (int) $request->query->get("threshold", 5));

            // Get critical alerts
            $alerts = $this->inventoryService->getCriticalStockAlerts(
                $hospitalId,
                $threshold,
            );

            // Convert to arrays for response
            $alertsData = array_map(function ($alert) {
                return [
                    "inventoryId" => $alert->getInventoryId() ?? null,
                    "hospitalId" => $alert->getHospital()->getHospitalId(),
                    "bloodType" => [
                        "bloodTypeId" => $alert
                            ->getBloodType()
                            ->getBloodTypeId(),
                        "name" => $alert->getBloodType()->getName(),
                    ],
                    "quantityUnits" => $alert->getQuantityUnits(),
                    "status" => $alert->getStatus(),
                    "updatedAt" => $alert
                        ->getUpdatedAt()
                        ?->format("Y-m-d H:i:s"),
                ];
            }, $alerts);

            return new JsonResponse(
                ResponseUtil::success(
                    $alertsData,
                    "Critical stock alerts retrieved successfully",
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve critical alerts: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Update inventory status (expired, quarantined, etc.).
     */
    #[Route("/{inventoryId}/status", methods: ["PATCH"])]
    public function updateStatus(
        string $inventoryId,
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

            // Validate status field
            if (!isset($data["status"]) || empty($data["status"])) {
                return new JsonResponse(
                    ResponseUtil::error("Status field is required"),
                    400,
                );
            }

            // Update status
            $inventory = $this->inventoryService->updateInventoryStatus(
                (int) $inventoryId,
                $data["status"],
            );

            return new JsonResponse(
                ResponseUtil::success(
                    [
                        "inventoryId" => $inventory->getInventoryId() ?? null,
                        "hospitalId" => $inventory
                            ->getHospital()
                            ->getHospitalId(),
                        "bloodType" => $inventory
                            ->getBloodType()
                            ->getBloodTypeId(),
                        "quantityUnits" => $inventory->getQuantityUnits(),
                        "status" => $inventory->getStatus(),
                        "updatedAt" => $inventory
                            ->getUpdatedAt()
                            ?->format("Y-m-d H:i:s"),
                    ],
                    "Inventory status updated successfully",
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
                    "Failed to update inventory status: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get inventory statistics for hospital.
     */
    #[Route("/stats/hospital/{hospitalId}", methods: ["GET"])]
    public function getStatistics(string $hospitalId): JsonResponse
    {
        try {
            // Verify hospital exists
            $hospital = $this->hospitalService->getHospitalById($hospitalId);
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Get all inventory for hospital
            $inventory = $this->inventoryService->getHospitalInventory(
                $hospitalId,
            );

            // Calculate statistics
            $totalUnits = 0;
            $bloodTypeDistribution = [];
            $criticalCount = 0;

            foreach ($inventory as $item) {
                $totalUnits += $item->getQuantityUnits();
                $bloodTypeId = $item->getBloodType()->getBloodTypeId();
                $bloodTypeDistribution[$bloodTypeId] = $item->getQuantityUnits();

                if ($item->getQuantityUnits() < 5) {
                    $criticalCount++;
                }
            }

            // Get critical alerts count
            $criticalAlerts = $this->inventoryService->getCriticalStockAlerts(
                $hospitalId,
                5,
            );

            return new JsonResponse(
                ResponseUtil::success([
                    "hospitalId" => $hospitalId,
                    "totalUnits" => $totalUnits,
                    "bloodTypeCount" => count($inventory),
                    "bloodTypeDistribution" => $bloodTypeDistribution,
                    "criticalAlertsCount" => count($criticalAlerts),
                    "averageUnitsPerType" => $inventory
                        ? round($totalUnits / count($inventory), 2)
                        : 0,
                ]),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve statistics: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Check if blood is available at hospital.
     */
    #[Route("/check", methods: ["POST"])]
    public function checkAvailability(Request $request): JsonResponse
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
            $requiredFields = ["hospital_id", "blood_type", "units"];
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

            // Verify hospital exists
            $hospital = $this->hospitalService->getHospitalById(
                $data["hospital_id"],
            );
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Check availability
            $isAvailable = $this->inventoryService->hasAvailableBlood(
                $data["hospital_id"],
                $data["blood_type"],
                (int) $data["units"],
            );

            $currentStock = $this->inventoryService->getStockLevel(
                $data["hospital_id"],
                $data["blood_type"],
            );

            return new JsonResponse(
                ResponseUtil::success([
                    "hospitalId" => $data["hospital_id"],
                    "bloodType" => $data["blood_type"],
                    "unitsRequested" => (int) $data["units"],
                    "currentStock" => $currentStock,
                    "isAvailable" => $isAvailable,
                    "deficit" => max(0, (int) $data["units"] - $currentStock),
                ]),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to check availability: " . $e->getMessage(),
                ),
                400,
            );
        }
    }
}
