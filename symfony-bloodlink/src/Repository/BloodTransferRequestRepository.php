<?php

namespace App\Repository;

use App\Entity\BloodTransferRequest;
use Doctrine\DBAL\ParameterType;
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

    /**
     * @return array<int, array<string, mixed>>
     */
    public function findTransferContextForDonor(string $donorId, int $limit = 8): array
    {
        $sql = <<<'SQL'
            SELECT *
            FROM (
                SELECT DISTINCT ON (btr.transfer_id)
                    btr.transfer_id,
                    COALESCE(btr.status, 'PENDING') AS transfer_status,
                    btr.quantity_units_requested,
                    btr.quantity_units_approved,
                    btr.requested_at,
                    btr.approved_at,
                    btr.delivery_expected_at,
                    btr.actual_delivery_at,
                    bt.blood_type_id,
                    rh.name AS requesting_hospital_name,
                    ah.name AS approving_hospital_name,
                    d.donation_id::text AS donation_id,
                    d.donation_date,
                    d.status AS donation_status,
                    COALESCE(
                        btr.actual_delivery_at,
                        btr.delivery_expected_at,
                        btr.approved_at,
                        btr.requested_at,
                        d.donation_date
                    ) AS context_at
                FROM donations d
                JOIN blood_transfer_request btr
                    ON btr.blood_type_id = d.blood_type_id
                    AND (
                        btr.requesting_hospital_id = d.hospital_id
                        OR btr.approving_hospital_id = d.hospital_id
                    )
                LEFT JOIN hospital rh ON rh.hospital_id = btr.requesting_hospital_id
                LEFT JOIN hospital ah ON ah.hospital_id = btr.approving_hospital_id
                LEFT JOIN blood_type bt ON bt.blood_type_id = btr.blood_type_id
                WHERE d.user_id::text = :donorId
                    AND (
                        btr.requested_at IS NULL
                        OR d.donation_date IS NULL
                        OR btr.requested_at >= d.donation_date
                    )
                ORDER BY btr.transfer_id, context_at DESC NULLS LAST
            ) transfer_context
            ORDER BY context_at DESC NULLS LAST, transfer_id DESC
            LIMIT :limit
            SQL;

        return $this->getEntityManager()
            ->getConnection()
            ->fetchAllAssociative($sql, [
                'donorId' => $donorId,
                'limit' => max(1, $limit),
            ], [
                'limit' => ParameterType::INTEGER,
            ]);
    }
}
