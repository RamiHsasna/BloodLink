<?php

namespace App\Repository;

use App\Entity\EligibilityReport;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<EligibilityReport>
 */
class EligibilityReportRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, EligibilityReport::class);
    }

    // Add custom query methods here as needed
}
