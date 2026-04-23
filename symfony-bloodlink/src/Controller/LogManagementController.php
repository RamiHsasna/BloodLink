<?php

namespace App\Controller;

use App\Entity\BloodTransferRequestLog;
use App\Entity\DonationLog;
use App\Entity\User;
use App\Form\BloodTransferRequestLogType;
use App\Form\DonationLogType;
use App\Repository\BloodTransferRequestLogRepository;
use App\Repository\BloodTransferRequestRepository;
use App\Repository\DonationLogRepository;
use App\Repository\DonationRepository;
use DateTimeImmutable;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Form\FormError;
use Symfony\Component\Form\FormInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class LogManagementController extends AbstractController
{
    public function __construct(
        private readonly DonationLogRepository $donationLogRepository,
        private readonly BloodTransferRequestLogRepository $transferLogRepository,
        private readonly EntityManagerInterface $entityManager,
    ) {
    }

    #[Route('/dashboard/logs', name: 'dashboard_logs_index', methods: ['GET'])]
    public function index(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $since = (new DateTimeImmutable('today'))->modify('-6 days')->setTime(0, 0, 0);
        $donationCount = $this->donationLogRepository->countAllLogs($hospitalId);
        $transferCount = $this->transferLogRepository->countAllLogs($hospitalId);
        $recentDonationCount = $this->donationLogRepository->countRecentSince($since, $hospitalId);
        $recentTransferCount = $this->transferLogRepository->countRecentSince($since, $hospitalId);
        $dailyDonationCounts = $this->donationLogRepository->getDailyCountsSince($since, $hospitalId);
        $dailyTransferCounts = $this->transferLogRepository->getDailyCountsSince($since, $hospitalId);

        return $this->render('log_management/dashboard/index.html.twig', [
            'session_user' => $sessionUser,
            'summary_cards' => [
                [
                    'label' => 'Donation Logs',
                    'value' => $donationCount,
                    'caption' => $recentDonationCount . ' in the last 7 days',
                ],
                [
                    'label' => 'Transfer Logs',
                    'value' => $transferCount,
                    'caption' => $recentTransferCount . ' in the last 7 days',
                ],
                [
                    'label' => 'Audit Entries',
                    'value' => $donationCount + $transferCount,
                    'caption' => 'Combined donation and transfer history',
                ],
                [
                    'label' => 'Recent Changes',
                    'value' => $recentDonationCount + $recentTransferCount,
                    'caption' => 'New audit records in the last 7 days',
                ],
            ],
            'workspace_cards' => [
                [
                    'label' => 'Donation audits',
                    'href' => 'dashboard_logs_donations',
                    'caption' => 'Track collection, screening, and completion transitions for donor records.',
                ],
                [
                    'label' => 'Transfer audits',
                    'href' => 'dashboard_logs_transfers',
                    'caption' => 'Review request, approval, dispatch, and delivery checkpoints between hospitals.',
                ],
            ],
            'mix_chart' => [
                'donations' => $donationCount,
                'transfers' => $transferCount,
                'total' => $donationCount + $transferCount,
                'donation_percent' => ($donationCount + $transferCount) > 0
                    ? (int) round(($donationCount / ($donationCount + $transferCount)) * 100)
                    : 0,
                'transfer_percent' => ($donationCount + $transferCount) > 0
                    ? (int) round(($transferCount / ($donationCount + $transferCount)) * 100)
                    : 0,
            ],
            'activity_chart' => $this->buildActivityChart($since, $dailyDonationCounts, $dailyTransferCounts),
            'recent_activity' => $this->buildRecentActivity($hospitalId),
        ]);
    }

    #[Route('/dashboard/logs/donations', name: 'dashboard_logs_donations', methods: ['GET'])]
    public function donationLogs(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $filters = $this->extractFilters($request, [
            'action' => DonationLog::allowedActions(),
            'status' => DonationLog::allowedStatuses(),
        ]);
        $result = $this->donationLogRepository->searchPaginated(
            $filters['query'],
            $filters['start_date'],
            $filters['end_date'],
            $filters['action'],
            $filters['status'],
            $hospitalId,
            $filters['page'],
            10,
        );

        return $this->render('log_management/donation_logs/index.html.twig', [
            'session_user' => $sessionUser,
            'filters' => $filters,
            'result' => $result,
            'entries' => $result['items'],
            'action_choices' => DonationLog::actionChoices(),
            'status_choices' => DonationLog::statusChoices(),
        ]);
    }

    #[Route('/dashboard/logs/donations/new', name: 'dashboard_logs_donations_new', methods: ['GET', 'POST'])]
    public function newDonationLog(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $log = (new DonationLog())
            ->setLogId($this->generateUuidV4())
            ->setCreatedAt(new DateTimeImmutable());

        if ($this->isHospitalStaff($sessionUser)) {
            $actor = $this->resolveSessionActor($sessionUser);
            if (!$actor instanceof User) {
                $this->addFlash('error', 'The current staff account could not be resolved for this audit entry.');

                return $this->redirectToRoute('dashboard_logs_donations');
            }

            $log->setUser($actor);
        }

        $form = $this->createForm(DonationLogType::class, $log, [
            'lock_snapshot' => false,
            'lock_actor' => $this->isHospitalStaff($sessionUser),
            'actor_user_id' => $this->isHospitalStaff($sessionUser) ? (string) ($sessionUser['id'] ?? '') : null,
            'allowed_hospital_id' => $hospitalId,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted()) {
            $this->prepareDonationLogTransitionForCreate($log, $form);
        }

        if ($form->isSubmitted() && $form->isValid()) {
            $this->entityManager->persist($log);
            $this->entityManager->flush();

            $this->addFlash('success', 'Donation log created successfully.');

            return $this->redirectToRoute('dashboard_logs_donations_show', ['logId' => $log->getLogId()]);
        }

        return $this->render('log_management/donation_logs/form.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'page_title' => 'Create Donation Log',
            'submit_label' => 'Create Donation Log',
            'entry' => $log,
        ]);
    }

    #[Route('/dashboard/logs/donations/{logId}', name: 'dashboard_logs_donations_show', methods: ['GET'])]
    public function showDonationLog(Request $request, string $logId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $log = $this->findDonationLogOrThrow($logId, $sessionUser);

        return $this->render('log_management/donation_logs/show.html.twig', [
            'session_user' => $sessionUser,
            'entry' => $log,
        ]);
    }

    #[Route('/dashboard/logs/donations/{logId}/edit', name: 'dashboard_logs_donations_edit', methods: ['GET', 'POST'])]
    public function editDonationLog(Request $request, string $logId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $log = $this->findDonationLogOrThrow($logId, $sessionUser);
        $hospitalId = $this->getScopedHospitalId($sessionUser);

        $form = $this->createForm(DonationLogType::class, $log, [
            'lock_snapshot' => true,
            'lock_actor' => true,
            'allowed_hospital_id' => $hospitalId,
            'actor_user_id' => $log->getUser()?->getUserId(),
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $this->entityManager->flush();
            $this->addFlash('success', 'Donation log updated successfully.');

            return $this->redirectToRoute('dashboard_logs_donations_show', ['logId' => $log->getLogId()]);
        }

        return $this->render('log_management/donation_logs/form.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'page_title' => 'Edit Donation Log',
            'submit_label' => 'Save Changes',
            'entry' => $log,
        ]);
    }

    #[Route('/dashboard/logs/donations/{logId}/delete', name: 'dashboard_logs_donations_delete', methods: ['POST'])]
    public function deleteDonationLog(Request $request, string $logId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $log = $this->findDonationLogOrThrow($logId, $this->getSessionUser($request));

        if ($this->isCsrfTokenValid('delete-donation-log-' . $logId, (string) $request->request->get('_token'))) {
            $this->entityManager->remove($log);
            $this->entityManager->flush();
            $this->addFlash('success', 'Donation log deleted successfully.');
        } else {
            $this->addFlash('error', 'The delete request is invalid.');
        }

        return $this->redirectToRoute('dashboard_logs_donations');
    }

    #[Route('/dashboard/logs/transfers', name: 'dashboard_logs_transfers', methods: ['GET'])]
    public function transferLogs(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $filters = $this->extractFilters($request, [
            'action' => BloodTransferRequestLog::allowedActions(),
            'status' => BloodTransferRequestLog::allowedStatuses(),
        ]);
        $result = $this->transferLogRepository->searchPaginated(
            $filters['query'],
            $filters['start_date'],
            $filters['end_date'],
            $filters['action'],
            $filters['status'],
            $hospitalId,
            $filters['page'],
            10,
        );

        return $this->render('log_management/transfer_logs/index.html.twig', [
            'session_user' => $sessionUser,
            'filters' => $filters,
            'result' => $result,
            'entries' => $result['items'],
            'action_choices' => BloodTransferRequestLog::actionChoices(),
            'status_choices' => BloodTransferRequestLog::statusChoices(),
        ]);
    }

    #[Route('/dashboard/logs/transfers/new', name: 'dashboard_logs_transfers_new', methods: ['GET', 'POST'])]
    public function newTransferLog(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $log = (new BloodTransferRequestLog())
            ->setLogId($this->generateUuidV4())
            ->setCreatedAt(new DateTimeImmutable());

        if ($this->isHospitalStaff($sessionUser)) {
            $actor = $this->resolveSessionActor($sessionUser);
            if (!$actor instanceof User) {
                $this->addFlash('error', 'The current staff account could not be resolved for this audit entry.');

                return $this->redirectToRoute('dashboard_logs_transfers');
            }

            $log->setUser($actor);
        }

        $form = $this->createForm(BloodTransferRequestLogType::class, $log, [
            'lock_snapshot' => false,
            'lock_actor' => $this->isHospitalStaff($sessionUser),
            'actor_user_id' => $this->isHospitalStaff($sessionUser) ? (string) ($sessionUser['id'] ?? '') : null,
            'allowed_hospital_id' => $hospitalId,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted()) {
            $this->prepareTransferLogTransitionForCreate($log, $form);
        }

        if ($form->isSubmitted() && $form->isValid()) {
            $this->entityManager->persist($log);
            $this->entityManager->flush();

            $this->addFlash('success', 'Transfer log created successfully.');

            return $this->redirectToRoute('dashboard_logs_transfers_show', ['logId' => $log->getLogId()]);
        }

        return $this->render('log_management/transfer_logs/form.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'page_title' => 'Create Transfer Log',
            'submit_label' => 'Create Transfer Log',
            'entry' => $log,
        ]);
    }

    #[Route('/dashboard/logs/transfers/{logId}', name: 'dashboard_logs_transfers_show', methods: ['GET'])]
    public function showTransferLog(Request $request, string $logId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $log = $this->findTransferLogOrThrow($logId, $sessionUser);

        return $this->render('log_management/transfer_logs/show.html.twig', [
            'session_user' => $sessionUser,
            'entry' => $log,
        ]);
    }

    #[Route('/dashboard/logs/transfers/{logId}/edit', name: 'dashboard_logs_transfers_edit', methods: ['GET', 'POST'])]
    public function editTransferLog(Request $request, string $logId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $log = $this->findTransferLogOrThrow($logId, $sessionUser);
        $hospitalId = $this->getScopedHospitalId($sessionUser);

        $form = $this->createForm(BloodTransferRequestLogType::class, $log, [
            'lock_snapshot' => true,
            'lock_actor' => true,
            'allowed_hospital_id' => $hospitalId,
            'actor_user_id' => $log->getUser()?->getUserId(),
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $this->entityManager->flush();
            $this->addFlash('success', 'Transfer log updated successfully.');

            return $this->redirectToRoute('dashboard_logs_transfers_show', ['logId' => $log->getLogId()]);
        }

        return $this->render('log_management/transfer_logs/form.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'page_title' => 'Edit Transfer Log',
            'submit_label' => 'Save Changes',
            'entry' => $log,
        ]);
    }

    #[Route('/dashboard/logs/transfers/{logId}/delete', name: 'dashboard_logs_transfers_delete', methods: ['POST'])]
    public function deleteTransferLog(Request $request, string $logId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $log = $this->findTransferLogOrThrow($logId, $this->getSessionUser($request));

        if ($this->isCsrfTokenValid('delete-transfer-log-' . $logId, (string) $request->request->get('_token'))) {
            $this->entityManager->remove($log);
            $this->entityManager->flush();
            $this->addFlash('success', 'Transfer log deleted successfully.');
        } else {
            $this->addFlash('error', 'The delete request is invalid.');
        }

        return $this->redirectToRoute('dashboard_logs_transfers');
    }

    #[Route('/dashboard/donor/audit-trail', name: 'dashboard_donor_audit_trail', methods: ['GET'])]
    public function donorAuditTrail(
        Request $request,
        DonationRepository $donationRepository,
        BloodTransferRequestRepository $transferRepository,
    ): Response {
        if ($guard = $this->denyUnlessDonor($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $donorId = trim((string) ($sessionUser['id'] ?? ''));
        if ($donorId === '') {
            $this->addFlash('error', 'The donor session could not be resolved.');

            return $this->redirectToRoute('auth_index');
        }

        $donations = $donationRepository->findDetailedForDonor($donorId, 8);
        $donationLogs = $this->donationLogRepository->findDetailedForDonor($donorId, 12);
        $transferContext = array_map(static function (array $item): array {
            $contextAt = $item['context_at'] ?? null;
            $requestedAt = $item['requested_at'] ?? null;

            return [
                'status' => $item['transfer_status'] ?? null,
                'requestingHospital' => [
                    'name' => $item['requesting_hospital_name'] ?? 'Requesting hospital',
                ],
                'approvingHospital' => [
                    'name' => $item['approving_hospital_name'] ?? 'Approving hospital',
                ],
                'occurredAt' => is_string($contextAt) && $contextAt !== '' ? new DateTimeImmutable($contextAt) : null,
                'requestedAt' => is_string($requestedAt) && $requestedAt !== '' ? new DateTimeImmutable($requestedAt) : null,
                'bloodTypeId' => $item['blood_type_id'] ?? null,
                'donationId' => $item['donation_id'] ?? null,
                'donationStatus' => $item['donation_status'] ?? null,
            ];
        }, $transferRepository->findTransferContextForDonor($donorId, 8));
        $latestDonationAt = $donations !== [] ? $donations[0]->getDonationDate() : null;

        return $this->render('log_management/front/index.html.twig', [
            'session_user' => $sessionUser,
            'summary' => [
                'donations' => $donationRepository->countForDonor($donorId),
                'logs' => $this->donationLogRepository->countForDonor($donorId),
                'transfers' => count($transferContext),
                'latest_donation_at' => $latestDonationAt,
            ],
            'donations' => $donations,
            'donation_logs' => $donationLogs,
            'transfer_context' => $transferContext,
        ]);
    }

    /**
     * @param array<string, array<int, string>> $allowedValues
     *
     * @return array{query: string, start: string, end: string, action: string, status: string, response: string, delivery: string, start_date: ?DateTimeImmutable, end_date: ?DateTimeImmutable, page: int}
     */
    private function extractFilters(Request $request, array $allowedValues = []): array
    {
        $query = trim((string) $request->query->get('q', ''));
        $start = trim((string) $request->query->get('start', ''));
        $end = trim((string) $request->query->get('end', ''));
        $action = $this->sanitizeFilterValue((string) $request->query->get('action', ''), $allowedValues['action'] ?? null);
        $status = $this->sanitizeFilterValue((string) $request->query->get('status', ''), $allowedValues['status'] ?? null);
        $response = $this->sanitizeFilterValue((string) $request->query->get('response', ''), $allowedValues['response'] ?? null);
        $delivery = $this->sanitizeFilterValue((string) $request->query->get('delivery', ''), $allowedValues['delivery'] ?? null);

        return [
            'query' => $query,
            'start' => $start,
            'end' => $end,
            'action' => $action,
            'status' => $status,
            'response' => $response,
            'delivery' => $delivery,
            'start_date' => $this->parseDate($start),
            'end_date' => $this->parseDate($end),
            'page' => max(1, $request->query->getInt('page', 1)),
        ];
    }

    /**
     * @param array<int, string>|null $allowedValues
     */
    private function sanitizeFilterValue(string $value, ?array $allowedValues): string
    {
        $value = trim($value);
        if ($value === '') {
            return '';
        }

        if ($allowedValues === null) {
            return $value;
        }

        return in_array($value, $allowedValues, true) ? $value : '';
    }

    private function parseDate(string $value): ?DateTimeImmutable
    {
        if ($value === '') {
            return null;
        }

        try {
            return new DateTimeImmutable($value);
        } catch (\Throwable) {
            return null;
        }
    }

    private function denyUnlessBackOffice(Request $request): ?RedirectResponse
    {
        $sessionUser = $this->getSessionUser($request);
        if ($sessionUser === null) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));

        if ($userType === User::TYPE_DONOR) {
            $this->addFlash('error', 'This area is available only to the Back Office team.');

            return $this->redirectToRoute('dashboard_donor_home');
        }

        if ($userType === User::TYPE_HOSPITAL_STAFF && $this->getScopedHospitalId($sessionUser) === null) {
            $this->addFlash('error', 'Your hospital staff account is not linked to a hospital yet.');

            return $this->redirectToRoute('dashboard_users');
        }

        return null;
    }

    private function denyUnlessDonor(Request $request): ?RedirectResponse
    {
        $sessionUser = $this->getSessionUser($request);
        if ($sessionUser === null) {
            return $this->redirectToRoute('auth_index');
        }

        if (strtoupper((string) ($sessionUser['user_type'] ?? '')) !== User::TYPE_DONOR) {
            $this->addFlash('error', 'This page is reserved for donor accounts.');

            return $this->redirectToRoute('dashboard_logs_index');
        }

        return null;
    }

    /**
     * @return array<string, mixed>|null
     */
    private function getSessionUser(Request $request): ?array
    {
        $sessionUser = $request->getSession()->get('auth_user');

        return is_array($sessionUser) ? $sessionUser : null;
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function getScopedHospitalId(?array $sessionUser): ?string
    {
        if (!$this->isHospitalStaff($sessionUser)) {
            return null;
        }

        $hospitalId = trim((string) ($sessionUser['hospital_id'] ?? ''));

        return $hospitalId !== '' ? $hospitalId : null;
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function isHospitalStaff(?array $sessionUser): bool
    {
        return strtoupper((string) ($sessionUser['user_type'] ?? '')) === User::TYPE_HOSPITAL_STAFF;
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function resolveSessionActor(?array $sessionUser): ?User
    {
        $userId = trim((string) ($sessionUser['id'] ?? ''));
        if ($userId === '') {
            return null;
        }

        $actor = $this->entityManager->find(User::class, $userId);

        return $actor instanceof User ? $actor : null;
    }

    /**
     * @return array<int, array{type: string, title: string, caption: string, occurred_at: ?\DateTimeInterface, route: string, route_params: array<string, scalar>}>
     */
    private function buildRecentActivity(?string $hospitalId = null): array
    {
        $entries = [];

        foreach ($this->donationLogRepository->findRecentDetailed(4, $hospitalId) as $log) {
            $entries[] = [
                'type' => 'Donation Log',
                'title' => $log->getAction(),
                'caption' => sprintf(
                    '%s • %s',
                    $log->getDonation()?->getStatus() ?? 'No donation status',
                    $this->formatUserLabel($log->getUser()?->getFirstName(), $log->getUser()?->getLastName()),
                ),
                'occurred_at' => $log->getCreatedAt(),
                'route' => 'dashboard_logs_donations_show',
                'route_params' => ['logId' => $log->getLogId()],
            ];
        }

        foreach ($this->transferLogRepository->findRecentDetailed(4, $hospitalId) as $log) {
            $entries[] = [
                'type' => 'Transfer Log',
                'title' => $log->getAction(),
                'caption' => sprintf(
                    'Transfer #%d • %s',
                    $log->getBloodTransferRequest()?->getTransferId() ?? 0,
                    $this->formatUserLabel($log->getUser()?->getFirstName(), $log->getUser()?->getLastName()),
                ),
                'occurred_at' => $log->getCreatedAt(),
                'route' => 'dashboard_logs_transfers_show',
                'route_params' => ['logId' => $log->getLogId()],
            ];
        }

        usort($entries, static function (array $left, array $right): int {
            $leftTimestamp = $left['occurred_at']?->getTimestamp() ?? 0;
            $rightTimestamp = $right['occurred_at']?->getTimestamp() ?? 0;

            return $rightTimestamp <=> $leftTimestamp;
        });

        return array_slice($entries, 0, 8);
    }

    /**
     * @param array<string, int> $dailyDonationCounts
     * @param array<string, int> $dailyTransferCounts
     *
     * @return array{days: array<int, array{label: string, donation_count: int, transfer_count: int, donation_height: int, transfer_height: int}>, max: int}
     */
    private function buildActivityChart(
        DateTimeImmutable $since,
        array $dailyDonationCounts,
        array $dailyTransferCounts,
    ): array {
        $days = [];
        $max = 0;

        for ($index = 0; $index < 7; ++$index) {
            $day = $since->modify(sprintf('+%d days', $index));
            $key = $day->format('Y-m-d');
            $donationCount = (int) ($dailyDonationCounts[$key] ?? 0);
            $transferCount = (int) ($dailyTransferCounts[$key] ?? 0);
            $max = max($max, $donationCount, $transferCount);

            $days[] = [
                'label' => $day->format('d M'),
                'donation_count' => $donationCount,
                'transfer_count' => $transferCount,
                'donation_height' => 0,
                'transfer_height' => 0,
            ];
        }

        $scale = max(1, $max);
        foreach ($days as &$day) {
            $day['donation_height'] = $day['donation_count'] > 0
                ? max(12, (int) round(($day['donation_count'] / $scale) * 100))
                : 0;
            $day['transfer_height'] = $day['transfer_count'] > 0
                ? max(12, (int) round(($day['transfer_count'] / $scale) * 100))
                : 0;
        }
        unset($day);

        return [
            'days' => $days,
            'max' => $scale,
        ];
    }

    private function formatUserLabel(?string $firstName, ?string $lastName): string
    {
        $label = trim(($firstName ?? '') . ' ' . ($lastName ?? ''));

        return $label !== '' ? $label : 'Unknown user';
    }

    private function generateUuidV4(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);

        $hex = bin2hex($bytes);

        return sprintf(
            '%s-%s-%s-%s-%s',
            substr($hex, 0, 8),
            substr($hex, 8, 4),
            substr($hex, 12, 4),
            substr($hex, 16, 4),
            substr($hex, 20, 12),
        );
    }

    private function prepareDonationLogTransitionForCreate(DonationLog $log, FormInterface $form): void
    {
        $donation = $log->getDonation();
        if ($donation === null) {
            return;
        }

        $previousStatus = trim($donation->getStatus());
        if ($previousStatus === '') {
            $form->get('donation')->addError(new FormError('The selected donation has no current status to audit.'));

            return;
        }

        $log->setPreviousStatus($previousStatus);
        $newStatus = trim((string) ($log->getNewStatus() ?? ''));

        if ($newStatus === '') {
            $form->get('newStatus')->addError(new FormError('Select the next donation status for this transition.'));

            return;
        }

        $log->setNewStatus($newStatus);

        if (strcasecmp($previousStatus, $newStatus) === 0) {
            $form->get('newStatus')->addError(new FormError('The new status must differ from the donation current status.'));
        }
    }

    private function prepareTransferLogTransitionForCreate(BloodTransferRequestLog $log, FormInterface $form): void
    {
        $transferRequest = $log->getBloodTransferRequest();
        if ($transferRequest === null) {
            return;
        }

        $previousStatus = trim((string) ($transferRequest->getStatus() ?? ''));
        if ($previousStatus === '') {
            $form->get('bloodTransferRequest')->addError(new FormError('The selected transfer request has no current status to audit.'));

            return;
        }

        $log->setPreviousStatus($previousStatus);
        $newStatus = trim((string) ($log->getNewStatus() ?? ''));

        if ($newStatus === '') {
            $form->get('newStatus')->addError(new FormError('Select the next transfer status for this transition.'));

            return;
        }

        $log->setNewStatus($newStatus);

        if (strcasecmp($previousStatus, $newStatus) === 0) {
            $form->get('newStatus')->addError(new FormError('The new status must differ from the transfer current status.'));
        }
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function findDonationLogOrThrow(string $logId, ?array $sessionUser = null): DonationLog
    {
        if (!$this->isUuid($logId)) {
            throw $this->createNotFoundException('Donation log not found.');
        }

        $log = $this->donationLogRepository->find($logId);
        if (!$log instanceof DonationLog) {
            throw $this->createNotFoundException('Donation log not found.');
        }

        $scopedHospitalId = $this->getScopedHospitalId($sessionUser);
        if ($scopedHospitalId !== null && $log->getDonation()?->getHospital()?->getHospitalId() !== $scopedHospitalId) {
            throw $this->createNotFoundException('Donation log not found.');
        }

        return $log;
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function findTransferLogOrThrow(string $logId, ?array $sessionUser = null): BloodTransferRequestLog
    {
        if (!$this->isUuid($logId)) {
            throw $this->createNotFoundException('Transfer log not found.');
        }

        $log = $this->transferLogRepository->find($logId);
        if (!$log instanceof BloodTransferRequestLog) {
            throw $this->createNotFoundException('Transfer log not found.');
        }

        $scopedHospitalId = $this->getScopedHospitalId($sessionUser);
        if ($scopedHospitalId !== null) {
            $requestingHospitalId = $log->getBloodTransferRequest()?->getRequestingHospital()?->getHospitalId();
            $approvingHospitalId = $log->getBloodTransferRequest()?->getApprovingHospital()?->getHospitalId();

            if ($requestingHospitalId !== $scopedHospitalId && $approvingHospitalId !== $scopedHospitalId) {
                throw $this->createNotFoundException('Transfer log not found.');
            }
        }

        return $log;
    }

    private function isUuid(string $value): bool
    {
        return preg_match(
            '/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i',
            $value,
        ) === 1;
    }
}
