<?php

namespace App\Repository;

use App\Entity\BloodInventory;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<BloodInventory>
 */
class BloodInventoryRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, BloodInventory::class);
    }

    // Add custom query methods here as needed
}
