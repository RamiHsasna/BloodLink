<?php

namespace App\Repository;

use App\Entity\Donor;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Donor>
 */
class DonorRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Donor::class);
    }

    /**
     * Find donors whose blood type is in the given list of compatible blood type IDs.
     *
     * @param string[] $compatibleBloodTypeIds List of blood_type_id values (e.g., ["O+ ", "O- "])
     *
     * @return Donor[]
     */
    public function findByCompatibleBloodTypes(array $compatibleBloodTypeIds): array
    {
        if (empty($compatibleBloodTypeIds)) {
            return [];
        }

        return $this->createQueryBuilder('d')
            ->join('d.bloodType', 'bt')
            ->join('d.user', 'u')
            ->where('bt.bloodTypeId IN (:bloodTypes)')
            ->setParameter('bloodTypes', $compatibleBloodTypeIds)
            ->getQuery()
            ->getResult();
    }

    /**
     * Find all donors that have valid latitude/longitude coordinates.
     *
     * @return Donor[]
     */
    public function findWithCoordinates(): array
    {
        return $this->createQueryBuilder('d')
            ->where('d.latitude IS NOT NULL')
            ->andWhere('d.longitude IS NOT NULL')
            ->getQuery()
            ->getResult();
    }
}
