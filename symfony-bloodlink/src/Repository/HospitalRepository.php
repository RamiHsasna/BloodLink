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
}
