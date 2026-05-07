<?php

namespace App\Repository;

use App\Entity\Hospital;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Hospital>
 */
class HospitalRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Hospital::class);
    }

    /**
     * Find all active hospitals
     *
     * @return Hospital[]
     */
    public function findActive(): array
    {
        return $this->createQueryBuilder('h')
            ->where('h.isActive = true')
            ->orderBy('h.name', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find hospitals by city
     *
     * @param string $city
     * @return Hospital[]
     */
    public function findByCity(string $city): array
    {
        return $this->createQueryBuilder('h')
            ->where('h.city = :city')
            ->setParameter('city', $city)
            ->orderBy('h.name', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find hospital by email
     *
     * @param string $email
     * @return Hospital|null
     */
    public function findByEmail(string $email): ?Hospital
    {
        return $this->createQueryBuilder('h')
            ->where('h.email = :email')
            ->setParameter('email', $email)
            ->getQuery()
            ->getOneOrNullResult();
    }

    /**
     * Find all deactivated hospitals
     *
     * @return Hospital[]
     */
    public function findDeactivatedHospitals(): array
    {
        return $this->createQueryBuilder('h')
            ->where('h.isActive = false')
            ->orderBy('h.deactivationStartDate', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find hospitals with expired deactivation periods (should be auto-reactivated)
     *
     * @return Hospital[]
     */
    public function findExpiredDeactivations(): array
    {
        return $this->createQueryBuilder('h')
            ->where('h.isActive = false')
            ->andWhere('h.deactivationEndDate IS NOT NULL')
            ->andWhere('h.deactivationEndDate <= :now')
            ->setParameter('now', new \DateTime())
            ->orderBy('h.deactivationEndDate', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Find currently active hospitals (respecting deactivation duration)
     *
     * @return Hospital[]
     */
    public function findCurrentlyActiveHospitals(): array
    {
        // Get all hospitals marked as active
        $hospitals = $this->createQueryBuilder('h')
            ->where('h.isActive = true')
            ->orderBy('h.name', 'ASC')
            ->getQuery()
            ->getResult();

        // Filter out those with active deactivation periods
        $now = new \DateTime();
        return array_filter($hospitals, function (Hospital $hospital) use ($now) {
            if ($hospital->getDeactivationEndDate() === null) {
                return true;
            }
            return $hospital->getDeactivationEndDate() <= $now;
        });
    }
}
