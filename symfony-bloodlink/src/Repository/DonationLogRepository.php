<?php

namespace App\Repository;

use App\Entity\DonationLog;
use DateTimeImmutable;
use DateTimeInterface;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\ORM\Tools\Pagination\Paginator;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<DonationLog>
 */
class DonationLogRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, DonationLog::class);
    }

    /**
     * @return array{items: array<int, DonationLog>, total: int, page: int, pages: int, per_page: int}
     */
    public function searchPaginated(
        ?string $query,
        ?DateTimeImmutable $startDate,
        ?DateTimeImmutable $endDate,
        ?string $action = null,
        ?string $newStatus = null,
        ?string $hospitalId = null,
        int $page = 1,
        int $perPage = 10,
    ): array {
        $qb = $this->createBaseQueryBuilder()
            ->orderBy('log.createdAt', 'DESC')
            ->addOrderBy('log.logId', 'DESC');

        $this->applyHospitalScope($qb, $hospitalId);

        if ($query !== null && $query !== '') {
            $pattern = '%' . mb_strtolower(trim($query)) . '%';
            $qb->andWhere(
                '(LOWER(log.action) LIKE :pattern
                OR LOWER(CONCAT(\'\', log.logId)) LIKE :pattern
                OR LOWER(COALESCE(log.previousStatus, \'\')) LIKE :pattern
                OR LOWER(COALESCE(log.newStatus, \'\')) LIKE :pattern
                OR LOWER(COALESCE(log.notes, \'\')) LIKE :pattern
                OR LOWER(COALESCE(actor.firstName, \'\')) LIKE :pattern
                OR LOWER(COALESCE(actor.lastName, \'\')) LIKE :pattern
                OR LOWER(COALESCE(actor.email, \'\')) LIKE :pattern
                OR LOWER(COALESCE(donor.firstName, \'\')) LIKE :pattern
                OR LOWER(COALESCE(donor.lastName, \'\')) LIKE :pattern
                OR LOWER(CONCAT(\'\', donation.donationId)) LIKE :pattern)'
            )->setParameter('pattern', $pattern);
        }

        if ($action !== null && $action !== '') {
            $qb->andWhere('log.action = :action')
                ->setParameter('action', $action);
        }

        if ($newStatus !== null && $newStatus !== '') {
            $qb->andWhere('log.newStatus = :newStatus')
                ->setParameter('newStatus', $newStatus);
        }

        $this->applyDateWindow($qb, 'log.createdAt', $startDate, $endDate);

        return $this->paginate($qb, $page, $perPage);
    }

    public function countAllLogs(?string $hospitalId = null): int
    {
        $qb = $this->createQueryBuilder('log')
            ->leftJoin('log.donation', 'donation')
            ->select('COUNT(log.logId)')
        ;

        $this->applyHospitalScope($qb, $hospitalId, false);

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    public function countRecentSince(DateTimeInterface $since, ?string $hospitalId = null): int
    {
        $qb = $this->createQueryBuilder('log')
            ->leftJoin('log.donation', 'donation')
            ->select('COUNT(log.logId)')
            ->andWhere('log.createdAt >= :since')
            ->setParameter('since', $since)
        ;

        $this->applyHospitalScope($qb, $hospitalId, false);

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    /**
     * @return array<string, int>
     */
    public function getDailyCountsSince(DateTimeInterface $since, ?string $hospitalId = null): array
    {
        $sql = <<<'SQL'
            SELECT TO_CHAR(log.created_at, 'YYYY-MM-DD') AS day_key, COUNT(log.log_id) AS total
            FROM donation_log log
            JOIN donations donation ON donation.donation_id = log.donation_id
            WHERE log.created_at >= :since
        SQL;
        $params = [
            'since' => $since->format('Y-m-d H:i:s'),
        ];

        if ($hospitalId !== null && $hospitalId !== '') {
            $sql .= ' AND donation.hospital_id::text = :hospitalId';
            $params['hospitalId'] = $hospitalId;
        }

        $sql .= ' GROUP BY day_key ORDER BY day_key ASC';

        $rows = $this->getEntityManager()->getConnection()->fetchAllAssociative($sql, $params);
        $dailyCounts = [];
        foreach ($rows as $row) {
            $dayKey = (string) ($row['day_key'] ?? '');
            if ($dayKey === '') {
                continue;
            }

            $dailyCounts[$dayKey] = (int) ($row['total'] ?? 0);
        }

        return $dailyCounts;
    }

    /**
     * @return array<int, DonationLog>
     */
    public function findRecentDetailed(int $limit = 5, ?string $hospitalId = null): array
    {
        $qb = $this->createBaseQueryBuilder()
            ->orderBy('log.createdAt', 'DESC')
            ->addOrderBy('log.logId', 'DESC')
            ->setMaxResults($limit);

        $this->applyHospitalScope($qb, $hospitalId);

        return $qb->getQuery()->getResult();
    }

    /**
     * @return array<int, DonationLog>
     */
    public function findDetailedForDonor(string $donorId, int $limit = 12): array
    {
        return $this->createBaseQueryBuilder()
            ->leftJoin('donation.hospital', 'hospital')->addSelect('hospital')
            ->leftJoin('donation.bloodType', 'bloodType')->addSelect('bloodType')
            ->andWhere('donor.userId = :donorId')
            ->setParameter('donorId', $donorId)
            ->orderBy('log.createdAt', 'DESC')
            ->addOrderBy('log.logId', 'DESC')
            ->setMaxResults(max(1, $limit))
            ->getQuery()
            ->getResult();
    }

    public function countForDonor(string $donorId): int
    {
        return (int) $this->createQueryBuilder('log')
            ->leftJoin('log.donation', 'donation')
            ->leftJoin('donation.donor', 'donor')
            ->select('COUNT(log.logId)')
            ->andWhere('donor.userId = :donorId')
            ->setParameter('donorId', $donorId)
            ->getQuery()
            ->getSingleScalarResult();
    }

    private function createBaseQueryBuilder(): QueryBuilder
    {
        return $this->createQueryBuilder('log')
            ->leftJoin('log.user', 'actor')->addSelect('actor')
            ->leftJoin('log.donation', 'donation')->addSelect('donation')
            ->leftJoin('donation.donor', 'donor')->addSelect('donor');
    }

    private function applyDateWindow(
        QueryBuilder $qb,
        string $field,
        ?DateTimeImmutable $startDate,
        ?DateTimeImmutable $endDate,
    ): void {
        if ($startDate !== null) {
            $qb->andWhere(sprintf('%s >= :startDate', $field))
                ->setParameter('startDate', $startDate->setTime(0, 0, 0));
        }

        if ($endDate !== null) {
            $qb->andWhere(sprintf('%s <= :endDate', $field))
                ->setParameter('endDate', $endDate->setTime(23, 59, 59));
        }
    }

    private function applyHospitalScope(QueryBuilder $qb, ?string $hospitalId, bool $withSelect = true): void
    {
        if ($hospitalId === null || $hospitalId === '') {
            return;
        }

        $qb->leftJoin('donation.hospital', 'hospital')
            ->andWhere('hospital.hospitalId = :hospitalId')
            ->setParameter('hospitalId', $hospitalId);

        if ($withSelect) {
            $qb->addSelect('hospital');
        }
    }

    /**
     * @return array{items: array<int, DonationLog>, total: int, page: int, pages: int, per_page: int}
     */
    private function paginate(QueryBuilder $qb, int $page, int $perPage): array
    {
        $page = max(1, $page);
        $perPage = max(1, $perPage);

        $query = $qb->getQuery()
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage);

        $paginator = new Paginator($query, true);
        $total = count($paginator);

        return [
            'items' => iterator_to_array($paginator),
            'total' => $total,
            'page' => $page,
            'pages' => max(1, (int) ceil($total / $perPage)),
            'per_page' => $perPage,
        ];
    }
}
