<?php

namespace App\Repository;

use App\Entity\DonationEvent;
use DateTimeImmutable;
use Doctrine\DBAL\ArrayParameterType;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\ORM\Tools\Pagination\Paginator;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<DonationEvent>
 */
class DonationEventRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, DonationEvent::class);
    }

    /**
     * @return array{items: array<int, DonationEvent>, total: int, page: int, pages: int, per_page: int}
     */
    public function searchPaginated(
        ?string $query,
        ?string $status,
        ?string $hospitalId = null,
        int $page = 1,
        int $perPage = 10,
    ): array {
        $qb = $this->createBaseQueryBuilder()
            ->orderBy('event.startDate', 'DESC')
            ->addOrderBy('event.eventId', 'DESC');

        if ($query !== null && $query !== '') {
            $pattern = '%' . mb_strtolower(trim($query)) . '%';
            $qb->andWhere(
                '(LOWER(event.name) LIKE :pattern
                OR LOWER(COALESCE(event.description, \'\')) LIKE :pattern
                OR LOWER(COALESCE(event.location, \'\')) LIKE :pattern
                OR LOWER(COALESCE(event.targetBloodTypes, \'\')) LIKE :pattern
                OR LOWER(COALESCE(hospital.name, \'\')) LIKE :pattern)'
            )->setParameter('pattern', $pattern);
        }

        if ($status !== null && $status !== '') {
            $qb->andWhere('event.status = :status')
                ->setParameter('status', $status);
        }

        $this->applyHospitalScope($qb, $hospitalId);

        return $this->paginate($qb, $page, $perPage);
    }

    /**
     * @return array<int, DonationEvent>
     */
    public function findUpcomingForDonor(?string $bloodTypeId = null, int $limit = 8): array
    {
        $qb = $this->createBaseQueryBuilder()
            ->andWhere('event.status IN (:statuses)')
            ->andWhere('event.endDate >= :now')
            ->setParameter('statuses', [
                DonationEvent::STATUS_ACTIVE,
                DonationEvent::STATUS_PLANNED,
            ])
            ->setParameter('now', new DateTimeImmutable())
            ->setMaxResults($limit);

        if ($bloodTypeId !== null && $bloodTypeId !== '') {
            $qb->addSelect(
                '(CASE
                    WHEN LOWER(event.targetBloodTypes) LIKE :bloodTypePattern THEN 0
                    WHEN event.targetBloodTypes IS NULL OR event.targetBloodTypes = \'\' THEN 1
                    ELSE 2
                END) AS HIDDEN donorMatchRank'
            )->setParameter('bloodTypePattern', '%' . mb_strtolower($bloodTypeId) . '%')
            ->orderBy('donorMatchRank', 'ASC');
        } else {
            $qb->orderBy('event.startDate', 'ASC');
        }

        if ($bloodTypeId !== null && $bloodTypeId !== '') {
            $qb->addOrderBy('event.startDate', 'ASC')
                ->addOrderBy('event.eventId', 'ASC');
        } else {
            $qb->addOrderBy('event.eventId', 'ASC');
        }

        return $qb->getQuery()->getResult();
    }

    public function countByStatus(string $status, ?string $hospitalId = null): int
    {
        $qb = $this->createQueryBuilder('event')
            ->leftJoin('event.hospital', 'hospital')
            ->select('COUNT(event.eventId)')
            ->andWhere('event.status = :status')
            ->setParameter('status', $status);

        $this->applyHospitalScope($qb, $hospitalId);

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    public function countUpcoming(?string $hospitalId = null): int
    {
        $qb = $this->createQueryBuilder('event')
            ->leftJoin('event.hospital', 'hospital')
            ->select('COUNT(event.eventId)')
            ->andWhere('event.endDate >= :now')
            ->setParameter('now', new DateTimeImmutable());

        $this->applyHospitalScope($qb, $hospitalId);

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    /**
     * @param array<int, string> $eventIds
     *
     * @return array<string, int>
     */
    public function getDonationCountsForEvents(array $eventIds): array
    {
        if ($eventIds === []) {
            return [];
        }

        $rows = $this->getEntityManager()->getConnection()->fetchAllAssociative(
            'SELECT donation_event_id::text AS event_id, COUNT(*) AS total
             FROM donations
             WHERE donation_event_id::text IN (:eventIds)
             GROUP BY donation_event_id',
            ['eventIds' => $eventIds],
            ['eventIds' => ArrayParameterType::STRING],
        );

        $counts = [];
        foreach ($rows as $row) {
            $eventId = (string) ($row['event_id'] ?? '');
            if ($eventId === '') {
                continue;
            }

            $counts[$eventId] = (int) ($row['total'] ?? 0);
        }

        return $counts;
    }

    private function createBaseQueryBuilder(): QueryBuilder
    {
        return $this->createQueryBuilder('event')
            ->leftJoin('event.hospital', 'hospital')
            ->addSelect('hospital');
    }

    private function applyHospitalScope(QueryBuilder $qb, ?string $hospitalId): void
    {
        if ($hospitalId === null || $hospitalId === '') {
            return;
        }

        $qb->andWhere('hospital.hospitalId = :hospitalId')
            ->setParameter('hospitalId', $hospitalId);
    }

    /**
     * @return array{items: array<int, DonationEvent>, total: int, page: int, pages: int, per_page: int}
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
