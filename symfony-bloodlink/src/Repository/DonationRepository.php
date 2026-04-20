<?php

namespace App\Repository;

use App\Entity\Donation;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Donation>
 */
class DonationRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Donation::class);
    }

    /**
     * @return array<int, Donation>
     */
    public function findDetailedForDonor(string $donorId, int $limit = 8): array
    {
        return $this->createQueryBuilder('donation')
            ->leftJoin('donation.donor', 'donor')->addSelect('donor')
            ->leftJoin('donation.hospital', 'hospital')->addSelect('hospital')
            ->leftJoin('donation.bloodType', 'bloodType')->addSelect('bloodType')
            ->andWhere('donor.userId = :donorId')
            ->setParameter('donorId', $donorId)
            ->orderBy('donation.donationDate', 'DESC')
            ->addOrderBy('donation.donationId', 'DESC')
            ->setMaxResults(max(1, $limit))
            ->getQuery()
            ->getResult();
    }

    public function countForDonor(string $donorId): int
    {
        return (int) $this->createQueryBuilder('donation')
            ->leftJoin('donation.donor', 'donor')
            ->select('COUNT(donation.donationId)')
            ->andWhere('donor.userId = :donorId')
            ->setParameter('donorId', $donorId)
            ->getQuery()
            ->getSingleScalarResult();
    }
}
