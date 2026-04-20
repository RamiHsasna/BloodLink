<?php

namespace App\Service;

use App\Entity\Hospital;
use App\Repository\HospitalRepository;
use App\Util\LocationUtil;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Uid\Uuid;

/**
 * Service class for hospital-related operations.
 */
class HospitalService
{
    public function __construct(
        private readonly HospitalRepository $hospitalRepository,
        private readonly EntityManagerInterface $entityManager,
    ) {}

    /**
     * Get all hospitals.
     *
     * @return Hospital[]
     */
    public function getAllHospitals(): array
    {
        return $this->hospitalRepository->findAll();
    }

    /**
     * Get hospital by ID.
     *
     * @param string $hospitalId Hospital UUID
     *
     * @return Hospital|null
     */
    public function getHospitalById(string $hospitalId): ?Hospital
    {
        try {
            return $this->hospitalRepository->find($hospitalId);
        } catch (\Exception $e) {
            return null;
        }
    }

    /**
     * Create a new hospital.
     *
     * @param array $hospitalData Hospital data (name, address, city, latitude, longitude, phone, email)
     *
     * @return Hospital
     *
     * @throws \InvalidArgumentException If validation fails
     */
    public function createHospital(array $hospitalData): Hospital
    {
        // Validate required fields
        $this->validateHospitalData($hospitalData, true);

        // Create Hospital entity
        $hospital = new Hospital();

        // Generate UUID
        $hospital->setHospitalId((string) Uuid::v4());

        // Set basic fields
        $hospital->setName($hospitalData["name"]);
        $hospital->setAddress($hospitalData["address"] ?? null);
        $hospital->setCity($hospitalData["city"] ?? null);

        // Set location
        if (isset($hospitalData["latitude"])) {
            $hospital->setLatitude((string) $hospitalData["latitude"]);
        }
        if (isset($hospitalData["longitude"])) {
            $hospital->setLongitude((string) $hospitalData["longitude"]);
        }

        // Set contact info
        if (isset($hospitalData["phone"])) {
            $hospital->setPhone($hospitalData["phone"]);
        }
        if (isset($hospitalData["email"])) {
            $hospital->setEmail($hospitalData["email"]);
        }

        // Set active status (default true)
        $hospital->setIsActive($hospitalData["isActive"] ?? true);

        // Set timestamps
        $now = new \DateTime();
        $hospital->setCreatedAt($now);
        $hospital->setUpdatedAt($now);

        // Persist to database
        $this->entityManager->persist($hospital);
        $this->entityManager->flush();

        return $hospital;
    }

    /**
     * Update hospital information.
     *
     * @param string $hospitalId Hospital UUID
     * @param array $updateData Fields to update
     *
     * @return Hospital
     *
     * @throws \InvalidArgumentException If hospital not found or validation fails
     */
    public function updateHospital(
        string $hospitalId,
        array $updateData,
    ): Hospital {
        // Find hospital
        $hospital = $this->hospitalRepository->find($hospitalId);
        if (!$hospital) {
            throw new \InvalidArgumentException(
                "Hospital not found with ID: {$hospitalId}",
            );
        }

        // Validate update data
        $this->validateHospitalData($updateData, false);

        // Update fields if provided
        if (isset($updateData["name"])) {
            $hospital->setName($updateData["name"]);
        }
        if (isset($updateData["address"])) {
            $hospital->setAddress($updateData["address"]);
        }
        if (isset($updateData["city"])) {
            $hospital->setCity($updateData["city"]);
        }
        if (isset($updateData["latitude"])) {
            $hospital->setLatitude((string) $updateData["latitude"]);
        }
        if (isset($updateData["longitude"])) {
            $hospital->setLongitude((string) $updateData["longitude"]);
        }
        if (isset($updateData["phone"])) {
            $hospital->setPhone($updateData["phone"]);
        }
        if (isset($updateData["email"])) {
            $hospital->setEmail($updateData["email"]);
        }

        if (isset($updateData["isActive"])) {
            $hospital->setIsActive($updateData["isActive"]);
        }

        // Update timestamp
        $hospital->setUpdatedAt(new \DateTime());

        // Persist changes
        $this->entityManager->flush();

        return $hospital;
    }

    /**
     * Delete a hospital.
     *
     * @param string $hospitalId Hospital UUID
     *
     * @return bool Success status
     *
     * @throws \InvalidArgumentException If hospital not found
     */
    public function deleteHospital(string $hospitalId): bool
    {
        $hospital = $this->hospitalRepository->find($hospitalId);
        if (!$hospital) {
            throw new \InvalidArgumentException(
                "Hospital not found with ID: {$hospitalId}",
            );
        }

        try {
            $this->entityManager->remove($hospital);
            $this->entityManager->flush();
            return true;
        } catch (\Exception $e) {
            throw new \InvalidArgumentException(
                "Cannot delete hospital: {$e->getMessage()}",
            );
        }
    }

    /**
     * Find hospitals near a location.
     *
     * @param float $latitude Latitude
     * @param float $longitude Longitude
     * @param float $radiusKm Search radius in kilometers
     *
     * @return Hospital[]
     *
     * @throws \InvalidArgumentException If coordinates are invalid
     */
    public function findNearbyHospitals(
        float $latitude,
        float $longitude,
        float $radiusKm = 50,
    ): array {
        // Validate coordinates
        if (!LocationUtil::isValidCoordinate($latitude, $longitude)) {
            throw new \InvalidArgumentException("Invalid coordinates provided");
        }

        // Get all hospitals with coordinates
        $allHospitals = $this->hospitalRepository->findAll();

        $nearbyHospitals = [];

        foreach ($allHospitals as $hospital) {
            // Skip hospitals without coordinates
            if (
                $hospital->getLatitude() === null ||
                $hospital->getLongitude() === null
            ) {
                continue;
            }

            // Calculate distance
            $hospitalLat = (float) $hospital->getLatitude();
            $hospitalLon = (float) $hospital->getLongitude();

            $distance = LocationUtil::calculateDistance(
                $latitude,
                $longitude,
                $hospitalLat,
                $hospitalLon,
            );

            // Filter by radius
            if ($distance <= $radiusKm) {
                $nearbyHospitals[] = [
                    "hospital" => $hospital,
                    "distance" => $distance,
                ];
            }
        }

        // Sort by distance (closest first)
        usort($nearbyHospitals, function ($a, $b) {
            return $a["distance"] <=> $b["distance"];
        });

        // Extract just the hospitals
        return array_map(function ($item) {
            return $item["hospital"];
        }, $nearbyHospitals);
    }

    /**
     * Activate or deactivate a hospital.
     *
     * @param string $hospitalId Hospital UUID
     * @param bool $isActive Active status
     *
     * @return Hospital
     *
     * @throws \InvalidArgumentException If hospital not found
     */
    public function setHospitalActiveStatus(
        string $hospitalId,
        bool $isActive,
    ): Hospital {
        $hospital = $this->hospitalRepository->find($hospitalId);
        if (!$hospital) {
            throw new \InvalidArgumentException(
                "Hospital not found with ID: {$hospitalId}",
            );
        }

        $hospital->setIsActive($isActive);
        $hospital->setUpdatedAt(new \DateTime());

        $this->entityManager->flush();

        return $hospital;
    }

    /**
     * Validate hospital data.
     *
     * @param array $data Hospital data to validate
     * @param bool $isCreation Whether this is for creation (stricter validation)
     *
     * @throws \InvalidArgumentException If validation fails
     */
    private function validateHospitalData(
        array $data,
        bool $isCreation = false,
    ): void {
        // For creation, name is required
        if ($isCreation && empty($data["name"])) {
            throw new \InvalidArgumentException("Hospital name is required");
        }

        // Validate name if provided
        if (isset($data["name"]) && !is_string($data["name"])) {
            throw new \InvalidArgumentException(
                "Hospital name must be a string",
            );
        }

        if (isset($data["name"]) && strlen(trim($data["name"])) === 0) {
            throw new \InvalidArgumentException(
                "Hospital name cannot be empty",
            );
        }

        // Validate email format if provided
        if (isset($data["email"]) && !empty($data["email"])) {
            if (!filter_var($data["email"], FILTER_VALIDATE_EMAIL)) {
                throw new \InvalidArgumentException("Invalid email format");
            }
        }

        // Validate latitude and longitude if provided
        if (isset($data["latitude"])) {
            $lat = (float) $data["latitude"];
            if ($lat < -90 || $lat > 90) {
                throw new \InvalidArgumentException(
                    "Latitude must be between -90 and 90",
                );
            }
        }

        if (isset($data["longitude"])) {
            $lon = (float) $data["longitude"];
            if ($lon < -180 || $lon > 180) {
                throw new \InvalidArgumentException(
                    "Longitude must be between -180 and 180",
                );
            }
        }

        // Validate phone format if provided (basic validation)
        if (isset($data["phone"]) && !empty($data["phone"])) {
            if (!preg_match('/^[\d\s\-\+\(\)]+$/', $data["phone"])) {
                throw new \InvalidArgumentException("Invalid phone format");
            }
        }
    }
}
