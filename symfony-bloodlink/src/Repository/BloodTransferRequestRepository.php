<?php

namespace App\Repository;

use App\Entity\BloodTransferRequest;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<BloodTransferRequest>
 */
class BloodTransferRequestRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, BloodTransferRequest::class);
    }

    // Add custom query methods here as needed
}
