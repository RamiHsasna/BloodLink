<?php

namespace App\Service;

use App\Entity\BloodInventory;
use App\Repository\BloodInventoryRepository;
use App\Repository\BloodTypeRepository;
use App\Repository\HospitalRepository;
use Doctrine\ORM\EntityManagerInterface;

/**
 * Service class for managing blood inventory levels.
 */
class BloodInventoryService
{
    public function __construct(
        private readonly BloodInventoryRepository $inventoryRepository,
        private readonly HospitalRepository $hospitalRepository,
        private readonly BloodTypeRepository $bloodTypeRepository,
        private readonly EntityManagerInterface $entityManager,
    ) {}

    /**
     * Get blood inventory for a hospital.
     *
     * @param string $hospitalId Hospital UUID
     *
     * @return BloodInventory[]
     */
    public function getHospitalInventory(string $hospitalId): array
    {
        return $this->inventoryRepository
            ->createQueryBuilder("bi")
            ->where("bi.hospital = :hospitalId")
            ->setParameter("hospitalId", $hospitalId)
            ->orderBy("bi.bloodType", "ASC")
            ->getQuery()
            ->getResult();
    }

    /**
     * Get current stock level for a specific blood type at a hospital.
     *
     * @param string $hospitalId Hospital UUID
     * @param string $bloodType Blood type code
     *
     * @return int Current stock units
     */
    public function getStockLevel(string $hospitalId, string $bloodType): int
    {
        $inventory = $this->inventoryRepository
            ->createQueryBuilder("bi")
            ->where("bi.hospital = :hospitalId")
            ->andWhere("bi.bloodType = :bloodTypeId")
            ->setParameter("hospitalId", $hospitalId)
            ->setParameter("bloodTypeId", $bloodType)
            ->getQuery()
            ->getOneOrNullResult();

        return $inventory ? $inventory->getQuantityUnits() : 0;
    }

    /**
     * Check if sufficient blood is available.
     *
     * @param string $hospitalId Hospital UUID
     * @param string $bloodType Blood type required
     * @param int $unitsRequired Units needed
     *
     * @return bool
     */
    public function hasAvailableBlood(
        string $hospitalId,
        string $bloodType,
        int $unitsRequired,
    ): bool {
        return $this->getStockLevel($hospitalId, $bloodType) >= $unitsRequired;
    }

    /**
     * Add blood to inventory.
     *
     * @param string $hospitalId Hospital UUID
     * @param string $bloodType Blood type
     * @param int $units Units to add
     *
     * @return BloodInventory Updated inventory record
     */
    public function addToInventory(
        string $hospitalId,
        string $bloodType,
        int $units,
    ): BloodInventory {
        $inventory = $this->getInventoryRecord($hospitalId, $bloodType);

        if ($inventory) {
            $inventory->setQuantityUnits(
                $inventory->getQuantityUnits() + $units
            );
        } else {
            $hospital = $this->hospitalRepository->find($hospitalId);
            $bloodTypeEntity = $this->bloodTypeRepository->find($bloodType);

            if (!$hospital || !$bloodTypeEntity) {
                throw new \InvalidArgumentException(
                    "Hospital or blood type not found"
                );
            }

            $inventory = new BloodInventory();
            $inventory->setHospital($hospital);
            $inventory->setBloodType($bloodTypeEntity);
            $inventory->setQuantityUnits($units);

            $this->entityManager->persist($inventory);
        }

        $inventory->setUpdatedAt(new \DateTime());
        $this->entityManager->flush();

        return $inventory;
    }

    /**
     * Remove blood from inventory.
     *
     * @param string $hospitalId Hospital UUID
     * @param string $bloodType Blood type
     * @param int $units Units to remove
     *
     * @return BloodInventory Updated inventory record
     *
     * @throws \Exception If insufficient blood available
     */
    public function removeFromInventory(
        string $hospitalId,
        string $bloodType,
        int $units,
    ): BloodInventory {
        if (!$this->hasAvailableBlood($hospitalId, $bloodType, $units)) {
            $available = $this->getStockLevel($hospitalId, $bloodType);
            throw new \Exception(
                "Insufficient blood inventory. Required: {$units}, Available: {$available}"
            );
        }

        $inventory = $this->getInventoryRecord($hospitalId, $bloodType);
        if (!$inventory) {
            throw new \Exception("Inventory record not found");
        }

        $inventory->setQuantityUnits(
            $inventory->getQuantityUnits() - $units
        );
        $inventory->setUpdatedAt(new \DateTime());

        $this->entityManager->flush();

        return $inventory;
    }

    /**
     * Get critical stock alerts (low inventory).
     *
     * @param string $hospitalId Hospital UUID
     * @param int $minimumThreshold Minimum threshold for alert
     *
     * @return BloodInventory[]
     */
    public function getCriticalStockAlerts(
        string $hospitalId,
        int $minimumThreshold = 5,
    ): array {
        return $this->inventoryRepository
            ->createQueryBuilder("bi")
            ->where("bi.hospital = :hospitalId")
            ->andWhere("bi.quantityUnits < :threshold")
            ->setParameter("hospitalId", $hospitalId)
            ->setParameter("threshold", $minimumThreshold)
            ->orderBy("bi.quantityUnits", "ASC")
            ->getQuery()
            ->getResult();
    }

    /**
     * Update blood expiration date or status.
     *
     * @param int $inventoryId Inventory record ID
     * @param string $status New status (active, expired, quarantined)
     *
     * @return BloodInventory
     *
     * @throws \InvalidArgumentException
     */
    public function updateInventoryStatus(
        int $inventoryId,
        string $status,
    ): BloodInventory {
        $inventory = $this->inventoryRepository->find($inventoryId);

        if (!$inventory) {
            throw new \InvalidArgumentException(
                "Inventory not found: {$inventoryId}"
            );
        }

        $validStatuses = ["OPTIMAL", "LOW", "CRITICAL", "EXPIRED", "QUARANTINED"];
        if (!in_array(strtoupper($status), $validStatuses)) {
            throw new \InvalidArgumentException("Invalid status: {$status}");
        }

        $inventory->setStatus(strtoupper($status));
        $inventory->setUpdatedAt(new \DateTime());

        $this->entityManager->flush();

        return $inventory;
    }

    /**
     * Get inventory record for a hospital and blood type.
     *
     * @param string $hospitalId Hospital UUID
     * @param string $bloodType Blood type
     *
     * @return BloodInventory|null
     */
    private function getInventoryRecord(
        string $hospitalId,
        string $bloodType,
    ): ?BloodInventory {
        return $this->inventoryRepository
            ->createQueryBuilder("bi")
            ->where("bi.hospital = :hospitalId")
            ->andWhere("bi.bloodType = :bloodTypeId")
            ->setParameter("hospitalId", $hospitalId)
            ->setParameter("bloodTypeId", $bloodType)
            ->getQuery()
            ->getOneOrNullResult();
    }
}
