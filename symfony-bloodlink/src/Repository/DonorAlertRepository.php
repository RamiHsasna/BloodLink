<?php

namespace App\Repository;

use App\Entity\DonorAlert;
use DateTimeImmutable;
use Doctrine\DBAL\ParameterType;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<DonorAlert>
 */
class DonorAlertRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, DonorAlert::class);
    }

    /**
     * @return array{items: array<int, DonorAlert>, total: int, page: int, pages: int, per_page: int}
     */
    public function searchPaginated(
        ?string $query,
        ?DateTimeImmutable $startDate,
        ?DateTimeImmutable $endDate,
        ?string $donorResponse = null,
        ?string $deliveryState = null,
        ?string $hospitalId = null,
        int $page = 1,
        int $perPage = 10,
    ): array {
        $page = max(1, $page);
        $perPage = max(1, $perPage);

        $ids = $this->fetchFilteredIds(
            $query,
            $startDate,
            $endDate,
            $donorResponse,
            $deliveryState,
            $hospitalId,
            null,
            $page,
            $perPage,
        );
        $total = $this->countFilteredAlerts(
            $query,
            $startDate,
            $endDate,
            $donorResponse,
            $deliveryState,
            $hospitalId,
            null,
        );

        return [
            'items' => $this->loadDetailedEntriesByIds($ids),
            'total' => $total,
            'page' => $page,
            'pages' => max(1, (int) ceil($total / $perPage)),
            'per_page' => $perPage,
        ];
    }

    /**
     * @return array{items: array<int, DonorAlert>, total: int, page: int, pages: int, per_page: int}
     */
    public function searchForDonor(
        string $donorId,
        ?string $query,
        int $page = 1,
        int $perPage = 10,
    ): array {
        $page = max(1, $page);
        $perPage = max(1, $perPage);

        $ids = $this->fetchFilteredIds(
            $query,
            null,
            null,
            null,
            null,
            null,
            $donorId,
            $page,
            $perPage,
        );
        $total = $this->countFilteredAlerts(
            $query,
            null,
            null,
            null,
            null,
            null,
            $donorId,
        );

        return [
            'items' => $this->loadDetailedEntriesByIds($ids),
            'total' => $total,
            'page' => $page,
            'pages' => max(1, (int) ceil($total / $perPage)),
            'per_page' => $perPage,
        ];
    }

    public function findOneForDonor(string $donorAlertId, string $donorId): ?DonorAlert
    {
        $sql = 'SELECT da.donor_alert_id::text
                FROM donor_alerts da
                JOIN alerts a ON da.alert_id::text = a.alert_id
                WHERE da.donor_alert_id::text = :donorAlertId
                  AND da.donor_id::text = :donorId
                LIMIT 1';
        $matchedId = $this->getEntityManager()->getConnection()->fetchOne($sql, [
            'donorAlertId' => $donorAlertId,
            'donorId' => $donorId,
        ]);

        if ($matchedId === false || $matchedId === null) {
            return null;
        }

        return $this->loadDetailedEntriesByIds([(string) $matchedId])[0] ?? null;
    }

    public function findVisibleById(string $donorAlertId): ?DonorAlert
    {
        $sql = 'SELECT da.donor_alert_id::text
                FROM donor_alerts da
                JOIN alerts a ON da.alert_id::text = a.alert_id
                WHERE da.donor_alert_id::text = :donorAlertId
                LIMIT 1';
        $matchedId = $this->getEntityManager()->getConnection()->fetchOne($sql, [
            'donorAlertId' => $donorAlertId,
        ]);

        if ($matchedId === false || $matchedId === null) {
            return null;
        }

        return $this->loadDetailedEntriesByIds([(string) $matchedId])[0] ?? null;
    }

    public function countAllAlerts(?string $hospitalId = null): int
    {
        return $this->countFilteredAlerts(null, null, null, null, null, $hospitalId, null);
    }

    public function countNotified(?string $hospitalId = null): int
    {
        return $this->countFilteredAlerts(
            null,
            null,
            null,
            null,
            'NOTIFIED_OR_READ',
            $hospitalId,
            null,
        );
    }

    public function countUnread(?string $hospitalId = null): int
    {
        return $this->countFilteredAlerts(null, null, null, null, 'UNREAD', $hospitalId, null);
    }

    public function countUnreadForDonor(string $donorId): int
    {
        return $this->countFilteredAlerts(null, null, null, null, 'UNREAD', null, $donorId);
    }

    /**
     * @return array<string, int>
     */
    public function getResponseBreakdown(?string $hospitalId = null): array
    {
        [$whereSql, $params, $types] = $this->buildSqlFilters(
            null,
            null,
            null,
            null,
            null,
            $hospitalId,
            null,
        );
        $sql = 'SELECT da.donor_response AS response, COUNT(da.donor_alert_id) AS total'
            . $this->baseSql()
            . $whereSql
            . ' GROUP BY da.donor_response';
        $rows = $this->getEntityManager()->getConnection()->fetchAllAssociative($sql, $params, $types);

        $breakdown = [];
        foreach ($rows as $row) {
            $response = $row['response'] ?? DonorAlert::RESPONSE_NO_RESPONSE;
            $breakdown[(string) $response] = (int) $row['total'];
        }

        return $breakdown;
    }

    /**
     * @return array<int, DonorAlert>
     */
    public function findRecentDetailed(int $limit = 6, ?string $hospitalId = null): array
    {
        $ids = $this->fetchFilteredIds(
            null,
            null,
            null,
            null,
            null,
            $hospitalId,
            null,
            1,
            max(1, $limit),
        );

        return $this->loadDetailedEntriesByIds($ids);
    }

    private function createDetailQueryBuilder(): QueryBuilder
    {
        return $this->createQueryBuilder('donorAlert')
            ->leftJoin('donorAlert.donor', 'donor')->addSelect('donor')
            ->leftJoin('donor.user', 'donorUser')->addSelect('donorUser');
    }

    private function countFilteredAlerts(
        ?string $query,
        ?DateTimeImmutable $startDate,
        ?DateTimeImmutable $endDate,
        ?string $donorResponse,
        ?string $deliveryState,
        ?string $hospitalId,
        ?string $donorId,
    ): int {
        [$whereSql, $params, $types] = $this->buildSqlFilters(
            $query,
            $startDate,
            $endDate,
            $donorResponse,
            $deliveryState,
            $hospitalId,
            $donorId,
        );
        $sql = 'SELECT COUNT(*)' . $this->baseSql() . $whereSql;

        return (int) $this->getEntityManager()->getConnection()->fetchOne($sql, $params, $types);
    }

    /**
     * @return array<int, string>
     */
    private function fetchFilteredIds(
        ?string $query,
        ?DateTimeImmutable $startDate,
        ?DateTimeImmutable $endDate,
        ?string $donorResponse,
        ?string $deliveryState,
        ?string $hospitalId,
        ?string $donorId,
        int $page,
        int $perPage,
    ): array {
        [$whereSql, $params, $types] = $this->buildSqlFilters(
            $query,
            $startDate,
            $endDate,
            $donorResponse,
            $deliveryState,
            $hospitalId,
            $donorId,
        );
        $sql = 'SELECT da.donor_alert_id::text'
            . $this->baseSql()
            . $whereSql
            . ' ORDER BY da.notification_sent_at DESC NULLS LAST, da.donor_alert_id DESC'
            . ' LIMIT :limit OFFSET :offset';

        $params['limit'] = max(1, $perPage);
        $params['offset'] = max(0, ($page - 1) * $perPage);
        $types['limit'] = ParameterType::INTEGER;
        $types['offset'] = ParameterType::INTEGER;

        return array_values(array_map('strval', $this->getEntityManager()->getConnection()->fetchFirstColumn($sql, $params, $types)));
    }

    /**
     * @return array{0: string, 1: array<string, mixed>, 2: array<string, int>}
     */
    private function buildSqlFilters(
        ?string $query,
        ?DateTimeImmutable $startDate,
        ?DateTimeImmutable $endDate,
        ?string $donorResponse,
        ?string $deliveryState,
        ?string $hospitalId,
        ?string $donorId,
    ): array {
        $clauses = [];
        $params = [];
        $types = [];

        if ($hospitalId !== null && $hospitalId !== '') {
            $clauses[] = 'a.hospital_id = :hospitalId';
            $params['hospitalId'] = $hospitalId;
        }

        if ($donorId !== null && $donorId !== '') {
            $clauses[] = 'da.donor_id::text = :donorId';
            $params['donorId'] = $donorId;
        }

        if ($query !== null && trim($query) !== '') {
            $clauses[] = '(LOWER(COALESCE(d.first_name, \'\')) LIKE :pattern
                OR LOWER(COALESCE(d.last_name, \'\')) LIKE :pattern
                OR LOWER(COALESCE(u.email, \'\')) LIKE :pattern
                OR LOWER(COALESCE(a.title, \'\')) LIKE :pattern
                OR LOWER(COALESCE(a.severity, \'\')) LIKE :pattern
                OR LOWER(COALESCE(da.donor_response, \'\')) LIKE :pattern
                OR LOWER(da.donor_alert_id::text) LIKE :pattern)';
            $params['pattern'] = '%' . mb_strtolower(trim($query)) . '%';
        }

        if ($startDate !== null) {
            $clauses[] = 'da.notification_sent_at >= :startDate';
            $params['startDate'] = $startDate->setTime(0, 0, 0)->format('Y-m-d H:i:s');
        }

        if ($endDate !== null) {
            $clauses[] = 'da.notification_sent_at <= :endDate';
            $params['endDate'] = $endDate->setTime(23, 59, 59)->format('Y-m-d H:i:s');
        }

        if ($donorResponse !== null && $donorResponse !== '') {
            $clauses[] = 'da.donor_response = :donorResponse';
            $params['donorResponse'] = $donorResponse;
        }

        if ($deliveryState === 'PENDING') {
            $clauses[] = '(da.is_notified = false OR da.is_notified IS NULL)';
        } elseif ($deliveryState === 'NOTIFIED') {
            $clauses[] = 'da.is_notified = true';
            $clauses[] = '(da.is_read = false OR da.is_read IS NULL)';
        } elseif ($deliveryState === 'READ') {
            $clauses[] = 'da.is_read = true';
        } elseif ($deliveryState === 'UNREAD') {
            $clauses[] = '(da.is_read = false OR da.is_read IS NULL)';
        } elseif ($deliveryState === 'NOTIFIED_OR_READ') {
            $clauses[] = 'da.is_notified = true';
        }

        return [
            $clauses === [] ? '' : ' WHERE ' . implode(' AND ', $clauses),
            $params,
            $types,
        ];
    }

    private function baseSql(): string
    {
        return ' FROM donor_alerts da
                 JOIN alerts a ON da.alert_id::text = a.alert_id
                 LEFT JOIN donors d ON da.donor_id = d.user_id
                 LEFT JOIN users u ON d.user_id = u.user_id';
    }

    /**
     * @param array<int, string> $ids
     *
     * @return array<int, DonorAlert>
     */
    private function loadDetailedEntriesByIds(array $ids): array
    {
        if ($ids === []) {
            return [];
        }

        /** @var array<int, DonorAlert> $items */
        $items = $this->createDetailQueryBuilder()
            ->andWhere('donorAlert.donorAlertId IN (:ids)')
            ->setParameter('ids', $ids)
            ->getQuery()
            ->getResult();

        $indexed = [];
        foreach ($items as $item) {
            $indexed[$item->getDonorAlertId()] = $item;
        }

        $ordered = [];
        foreach ($ids as $id) {
            if (isset($indexed[$id])) {
                $ordered[] = $indexed[$id];
            }
        }

        return $ordered;
    }
}
