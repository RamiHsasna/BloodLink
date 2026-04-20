<?php

namespace App\Controller\Api;

use App\Service\HospitalService;
use App\Service\BloodInventoryService;
use App\Util\ResponseUtil;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * API Controller for hospital endpoints.
 */
#[Route("/api/hospitals", name: "api_hospitals_")]
class HospitalController extends AbstractController
{
    public function __construct(
        private readonly HospitalService $hospitalService,
        private readonly BloodInventoryService $bloodInventoryService,
    ) {}

    /**
     * Get all hospitals with pagination.
     */
    #[Route("", methods: ["GET"])]
    public function listHospitals(Request $request): JsonResponse
    {
        try {
            // Extract pagination parameters from request
            $page = max(1, (int) $request->query->get("page", 1));
            $perPage = min(
                100,
                max(1, (int) $request->query->get("per_page", 10)),
            );

            // Get all hospitals
            $hospitals = $this->hospitalService->getAllHospitals();

            // Manually implement pagination
            $total = count($hospitals);
            $offset = ($page - 1) * $perPage;
            $paginatedHospitals = array_slice($hospitals, $offset, $perPage);

            // Convert to arrays for response
            $hospitalsData = array_map(function ($hospital) {
                return $this->hospitalToArray($hospital);
            }, $paginatedHospitals);

            return new JsonResponse(
                ResponseUtil::paginated(
                    $hospitalsData,
                    $page,
                    $perPage,
                    $total,
                ),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve hospitals: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get hospital by ID.
     */
    #[Route("/{hospitalId}", methods: ["GET"])]
    public function getHospital(string $hospitalId): JsonResponse
    {
        try {
            // Fetch hospital by ID
            $hospital = $this->hospitalService->getHospitalById($hospitalId);

            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            return new JsonResponse(
                ResponseUtil::success($this->hospitalToArray($hospital)),
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to retrieve hospital: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Create a new hospital.
     */
    #[Route("", methods: ["POST"])]
    public function createHospital(Request $request): JsonResponse
    {
        try {
            // Extract JSON data from request
            $data = json_decode($request->getContent(), true);

            if (!$data) {
                return new JsonResponse(
                    ResponseUtil::error("Invalid JSON data"),
                    400,
                );
            }

            // Validate required fields
            $requiredFields = ["name"];
            $errors = [];

            foreach ($requiredFields as $field) {
                if (!isset($data[$field]) || empty($data[$field])) {
                    $errors[$field] = "{$field} is required";
                }
            }

            if (!empty($errors)) {
                return new JsonResponse(
                    ResponseUtil::error("Validation failed", $errors, 400),
                    400,
                );
            }

            // Create hospital
            $hospital = $this->hospitalService->createHospital($data);

            return new JsonResponse(
                ResponseUtil::success(
                    $this->hospitalToArray($hospital),
                    "Hospital created successfully",
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
                    "Failed to create hospital: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Update hospital information.
     */
    #[Route("/{hospitalId}", methods: ["PUT", "PATCH"])]
    public function updateHospital(
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

            // Extract JSON data
            $data = json_decode($request->getContent(), true);

            if ($data === null && $request->getContent()) {
                return new JsonResponse(
                    ResponseUtil::error("Invalid JSON data"),
                    400,
                );
            }

            if (!$data) {
                $data = [];
            }

            // Update hospital
            $updatedHospital = $this->hospitalService->updateHospital(
                $hospitalId,
                $data,
            );

            return new JsonResponse(
                ResponseUtil::success(
                    $this->hospitalToArray($updatedHospital),
                    "Hospital updated successfully",
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
                    "Failed to update hospital: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Delete a hospital.
     */
    #[Route("/{hospitalId}", methods: ["DELETE"])]
    public function deleteHospital(string $hospitalId): JsonResponse
    {
        try {
            // Check if hospital exists
            $hospital = $this->hospitalService->getHospitalById($hospitalId);
            if (!$hospital) {
                return new JsonResponse(
                    ResponseUtil::error("Hospital not found", null, 404),
                    404,
                );
            }

            // Delete hospital
            $this->hospitalService->deleteHospital($hospitalId);

            return new JsonResponse(null, 204);
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Cannot delete hospital: " . $e->getMessage(),
                ),
                400,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to delete hospital: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Find hospitals near a location.
     */
    #[Route("/nearby", methods: ["GET"])]
    public function findNearbyHospitals(Request $request): JsonResponse
    {
        try {
            // Extract latitude, longitude, radius from query parameters
            $latitude = $request->query->get("latitude");
            $longitude = $request->query->get("longitude");
            $radiusKm = (float) $request->query->get("radius_km", 50);

            // Validate parameters
            if (!$latitude || !$longitude) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Latitude and longitude are required",
                        null,
                        400,
                    ),
                    400,
                );
            }

            $latitude = (float) $latitude;
            $longitude = (float) $longitude;

            // Validate radius
            if ($radiusKm <= 0) {
                return new JsonResponse(
                    ResponseUtil::error(
                        "Radius must be greater than 0",
                        null,
                        400,
                    ),
                    400,
                );
            }

            // Find nearby hospitals
            $hospitals = $this->hospitalService->findNearbyHospitals(
                $latitude,
                $longitude,
                $radiusKm,
            );

            // Convert to arrays for response
            $hospitalsData = array_map(function ($hospital) {
                return $this->hospitalToArray($hospital);
            }, $hospitals);

            return new JsonResponse(
                ResponseUtil::success($hospitalsData, "Found nearby hospitals"),
            );
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error("Invalid coordinates: " . $e->getMessage()),
                400,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to find nearby hospitals: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Get hospital blood inventory.
     */
    #[Route("/{hospitalId}/inventory", methods: ["GET"])]
    public function getInventory(string $hospitalId): JsonResponse
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
            $inventory = $this->bloodInventoryService->getHospitalInventory(
                $hospitalId,
            );

            // Convert to arrays for response
            $inventoryData = array_map(function ($inv) {
                return [
                    "hospitalId" => $inv->getHospitalId(),
                    "bloodType" => $inv->getBloodType(),
                    "units" => $inv->getUnits(),
                    "expirationDate" => $inv
                        ->getExpirationDate()
                        ?->format("Y-m-d"),
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
     * Activate/deactivate a hospital.
     */
    #[Route("/{hospitalId}/status", methods: ["PATCH"])]
    public function updateStatus(
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

            // Extract isActive flag from request
            $data = json_decode($request->getContent(), true);

            if (!isset($data["isActive"])) {
                return new JsonResponse(
                    ResponseUtil::error("isActive field is required"),
                    400,
                );
            }

            if (!is_bool($data["isActive"])) {
                return new JsonResponse(
                    ResponseUtil::error("isActive must be a boolean"),
                    400,
                );
            }

            // Update status
            $updatedHospital = $this->hospitalService->setHospitalActiveStatus(
                $hospitalId,
                $data["isActive"],
            );

            return new JsonResponse(
                ResponseUtil::success(
                    $this->hospitalToArray($updatedHospital),
                    "Hospital status updated successfully",
                ),
            );
        } catch (\InvalidArgumentException $e) {
            return new JsonResponse(
                ResponseUtil::error("Hospital not found: " . $e->getMessage()),
                404,
            );
        } catch (\Exception $e) {
            return new JsonResponse(
                ResponseUtil::error(
                    "Failed to update status: " . $e->getMessage(),
                ),
                400,
            );
        }
    }

    /**
     * Convert hospital entity to array for response.
     */
    private function hospitalToArray($hospital): array
    {
        return [
            "hospitalId" => $hospital->getHospitalId(),
            "name" => $hospital->getName(),
            "address" => $hospital->getAddress(),
            "city" => $hospital->getCity(),
            "latitude" => $hospital->getLatitude(),
            "longitude" => $hospital->getLongitude(),
            "phone" => $hospital->getPhone(),
            "email" => $hospital->getEmail(),
            "isActive" => $hospital->getIsActive(),
            "createdAt" => $hospital->getCreatedAt()?->format("Y-m-d H:i:s"),
            "updatedAt" => $hospital->getUpdatedAt()?->format("Y-m-d H:i:s"),
        ];
    }
}
