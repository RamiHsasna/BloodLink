<?php

namespace App\Repository;

use App\Entity\DonorEligibility;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<DonorEligibility>
 */
class DonorEligibilityRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, DonorEligibility::class);
    }

    // Add custom query methods here as needed
}
