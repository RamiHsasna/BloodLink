<?php

namespace App\Repository;

use App\Entity\BloodTransferRequestLog;
use DateTimeImmutable;
use DateTimeInterface;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\ORM\Tools\Pagination\Paginator;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<BloodTransferRequestLog>
 */
class BloodTransferRequestLogRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, BloodTransferRequestLog::class);
    }

    /**
     * @return array{items: array<int, BloodTransferRequestLog>, total: int, page: int, pages: int, per_page: int}
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
                OR LOWER(COALESCE(log.previousStatus, \'\')) LIKE :pattern
                OR LOWER(COALESCE(log.newStatus, \'\')) LIKE :pattern
                OR LOWER(COALESCE(log.notes, \'\')) LIKE :pattern
                OR LOWER(COALESCE(actor.firstName, \'\')) LIKE :pattern
                OR LOWER(COALESCE(actor.lastName, \'\')) LIKE :pattern
                OR LOWER(COALESCE(actor.email, \'\')) LIKE :pattern
                OR LOWER(COALESCE(CONCAT(\'\', transfer.transferId), \'\')) LIKE :pattern
                OR LOWER(COALESCE(requestingHospital.name, \'\')) LIKE :pattern
                OR LOWER(COALESCE(approvingHospital.name, \'\')) LIKE :pattern)'
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
            ->leftJoin('log.bloodTransferRequest', 'transfer')
            ->leftJoin('transfer.requestingHospital', 'requestingHospital')
            ->leftJoin('transfer.approvingHospital', 'approvingHospital')
            ->select('COUNT(log.logId)');

        $this->applyHospitalScope($qb, $hospitalId);

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    public function countRecentSince(DateTimeInterface $since, ?string $hospitalId = null): int
    {
        $qb = $this->createQueryBuilder('log')
            ->leftJoin('log.bloodTransferRequest', 'transfer')
            ->leftJoin('transfer.requestingHospital', 'requestingHospital')
            ->leftJoin('transfer.approvingHospital', 'approvingHospital')
            ->select('COUNT(log.logId)')
            ->andWhere('log.createdAt >= :since')
            ->setParameter('since', $since);

        $this->applyHospitalScope($qb, $hospitalId);

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    /**
     * @return array<string, int>
     */
    public function getDailyCountsSince(DateTimeInterface $since, ?string $hospitalId = null): array
    {
        $sql = <<<'SQL'
            SELECT TO_CHAR(log.created_at, 'YYYY-MM-DD') AS day_key, COUNT(log.log_id) AS total
            FROM blood_transfer_request_log log
            JOIN blood_transfer_request transfer ON transfer.transfer_id = log.transfer_id
            WHERE log.created_at >= :since
        SQL;
        $params = [
            'since' => $since->format('Y-m-d H:i:s'),
        ];

        if ($hospitalId !== null && $hospitalId !== '') {
            $sql .= ' AND (transfer.requesting_hospital_id::text = :hospitalId OR transfer.approving_hospital_id::text = :hospitalId)';
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
     * @return array<int, BloodTransferRequestLog>
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

    private function createBaseQueryBuilder(): QueryBuilder
    {
        return $this->createQueryBuilder('log')
            ->leftJoin('log.user', 'actor')->addSelect('actor')
            ->leftJoin('log.bloodTransferRequest', 'transfer')->addSelect('transfer')
            ->leftJoin('transfer.requestingHospital', 'requestingHospital')->addSelect('requestingHospital')
            ->leftJoin('transfer.approvingHospital', 'approvingHospital')->addSelect('approvingHospital');
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

    private function applyHospitalScope(QueryBuilder $qb, ?string $hospitalId): void
    {
        if ($hospitalId === null || $hospitalId === '') {
            return;
        }

        $qb->andWhere('(requestingHospital.hospitalId = :hospitalId OR approvingHospital.hospitalId = :hospitalId)')
            ->setParameter('hospitalId', $hospitalId);
    }

    /**
     * @return array{items: array<int, BloodTransferRequestLog>, total: int, page: int, pages: int, per_page: int}
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
