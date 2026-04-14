<?php

namespace App\Repository;

use App\Entity\DonorAlert;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<DonorAlert>
 */
class DonorAlertRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, DonorAlert::class);
    }

    // Add custom query methods here as needed
}
