<?php

namespace App\Repository;

use App\Entity\HospitalStaff;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<HospitalStaff>
 */
class HospitalStaffRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, HospitalStaff::class);
    }

    // Add custom query methods here as needed
}
