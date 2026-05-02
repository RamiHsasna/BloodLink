<?php

namespace App\Repository;

use App\Entity\DonationEventDonor;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<DonationEventDonor>
 */
class DonationEventDonorRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, DonationEventDonor::class);
    }

    // Add custom query methods here as needed
}
