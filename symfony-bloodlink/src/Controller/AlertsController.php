<?php

namespace App\Controller;

use App\Entity\Alert;
use App\Entity\DonorAlert;
use App\Entity\User;
use App\Form\AlertType;
use App\Form\DonorAlertType;
use App\Repository\DonorAlertRepository;
use App\Service\AlertService;
use DateTimeImmutable;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Routing\Attribute\Route;

class AlertsController extends AbstractController
{
    public function __construct(
        private readonly DonorAlertRepository $donorAlertRepository,
        private readonly EntityManagerInterface $entityManager,
        private readonly MailerInterface $mailer,
    ) {
    }

    #[Route('/dashboard/alerts', name: 'dashboard_alerts_index', methods: ['GET'])]
    public function index(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $filters = $this->extractFilters($request, [
            'response' => DonorAlert::DONOR_RESPONSES,
            'delivery' => array_values($this->donorAlertDeliveryChoices()),
        ]);
        $result = $this->donorAlertRepository->searchPaginated(
            $filters['query'],
            $filters['start_date'],
            $filters['end_date'],
            $filters['response'],
            $filters['delivery'],
            $hospitalId,
            $filters['page'],
            10,
        );

        return $this->render('alerts/back_office/index.html.twig', [
            'session_user' => $sessionUser,
            'filters' => $filters,
            'result' => $result,
            'entries' => $result['items'],
            'response_choices' => DonorAlert::donorResponseChoices(),
            'delivery_choices' => $this->donorAlertDeliveryChoices(),
        ]);
    }


    // ─── BLOOD ALERT CREATION (new feature) ──────────────────────────────────

    #[Route('/dashboard/alerts/create-blood-alert', name: 'dashboard_alerts_create_blood_alert', methods: ['GET', 'POST'])]
    public function createBloodAlert(Request $request, AlertService $alertService): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);

        $alert = new Alert();
        $form = $this->createForm(AlertType::class, $alert, [
            'allowed_hospital_id' => $this->getScopedHospitalId($sessionUser),
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            try {
                $staffId = (string) ($sessionUser['id'] ?? '');
                
                $hospital = $form->get('hospitalId')->getData();
                $hospitalId = $hospital ? $hospital->getHospitalId() : null;

                if ($hospitalId === null || $hospitalId === '') {
                    $this->addFlash('error', 'No hospital selected. Please ensure a hospital is chosen for the alert.');
                    return $this->redirectToRoute('dashboard_alerts_create_blood_alert');
                }

                $result = $alertService->createAlertAndNotify($alert, $hospitalId, $staffId);

                $this->addFlash('success', sprintf(
                    'Blood alert created! %d compatible donor(s) found, %d within radius notified, %d email(s) sent.',
                    $result['total_compatible'],
                    $result['notified_donors'],
                    $result['emails_sent'],
                ));

                return $this->redirectToRoute('dashboard_alerts_index');
            } catch (\Throwable $e) {
                $this->addFlash('error', 'Failed to create alert: ' . $e->getMessage());
            }
        }

        return $this->render('alerts/back_office/create_blood_alert.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'page_title' => 'Create Blood Alert',
        ]);
    }

    // ─── EXISTING ROUTES ─────────────────────────────────────────────────────

    #[Route('/dashboard/alerts/{donorAlertId}', name: 'dashboard_alerts_show', methods: ['GET'])]
    public function show(Request $request, string $donorAlertId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $entry = $this->findDonorAlertOrThrow($donorAlertId, $sessionUser);

        return $this->render('alerts/back_office/show.html.twig', [
            'session_user' => $sessionUser,
            'entry' => $entry,
        ]);
    }

    #[Route('/dashboard/alerts/blood-alert/{alertId}/edit', name: 'dashboard_alerts_edit_blood_alert', methods: ['GET', 'POST'])]
    public function editBloodAlert(Request $request, string $alertId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $alert = $this->alertRepository->find($alertId);

        if (!$alert) {
            throw $this->createNotFoundException('Alert not found.');
        }

        $form = $this->createForm(AlertType::class, $alert, [
            'allowed_hospital_id' => $this->getScopedHospitalId($sessionUser),
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $this->entityManager->flush();
            $this->addFlash('success', 'Blood alert updated successfully.');

            return $this->redirectToRoute('dashboard_alerts_index');
        }

        return $this->render('alerts/back_office/edit_blood_alert.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'alert' => $alert,
        ]);
    }

    #[Route('/dashboard/alerts/{donorAlertId}/delete', name: 'dashboard_alerts_delete', methods: ['POST'])]
    public function delete(Request $request, string $donorAlertId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $entry = $this->findDonorAlertOrThrow($donorAlertId, $this->getSessionUser($request));

        if ($this->isCsrfTokenValid('delete-donor-alert-' . $donorAlertId, (string) $request->request->get('_token'))) {
            $this->entityManager->remove($entry);
            $this->entityManager->flush();
            $this->addFlash('success', 'Donor alert deleted successfully.');
        } else {
            $this->addFlash('error', 'The delete request is invalid.');
        }

        return $this->redirectToRoute('dashboard_alerts_index');
    }

    #[Route('/dashboard/donor/alerts', name: 'dashboard_donor_alerts', methods: ['GET'])]
    public function donorHistory(Request $request): Response
    {
        if ($guard = $this->denyUnlessDonor($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $query = trim((string) $request->query->get('q', ''));
        $page = max(1, $request->query->getInt('page', 1));

        $result = $this->donorAlertRepository->searchForDonor((string) $sessionUser['id'], $query, $page, 8);

        return $this->render('alerts/front/index.html.twig', [
            'session_user' => $sessionUser,
            'result' => $result,
            'entries' => $result['items'],
            'query' => $query,
            'summary' => [
                'total' => $result['total'],
                'unread' => $this->donorAlertRepository->countUnreadForDonor((string) $sessionUser['id']),
            ],
        ]);
    }

    #[Route('/dashboard/donor/alerts/{donorAlertId}', name: 'dashboard_donor_alert_show', methods: ['GET'])]
    public function donorDetail(Request $request, string $donorAlertId): Response
    {
        if ($guard = $this->denyUnlessDonor($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        if (!$this->isUuid($donorAlertId)) {
            throw $this->createNotFoundException('Donor alert not found.');
        }

        $entry = $this->donorAlertRepository->findOneForDonor($donorAlertId, (string) $sessionUser['id']);
        if (!$entry instanceof DonorAlert) {
            throw $this->createNotFoundException('Donor alert not found.');
        }

        return $this->render('alerts/front/show.html.twig', [
            'session_user' => $sessionUser,
            'entry' => $entry,
        ]);
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────

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

            return $this->redirectToRoute('dashboard_alerts_index');
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
     * @return array<string, string>
     */
    private function donorAlertDeliveryChoices(): array
    {
        return [
            'Pending delivery' => 'PENDING',
            'Notified' => 'NOTIFIED',
            'Read by donor' => 'READ',
        ];
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function findDonorAlertOrThrow(string $donorAlertId, ?array $sessionUser = null): DonorAlert
    {
        if (!$this->isUuid($donorAlertId)) {
            throw $this->createNotFoundException('Donor alert not found.');
        }

        $entry = $this->donorAlertRepository->findVisibleById($donorAlertId);
        if (!$entry instanceof DonorAlert) {
            throw $this->createNotFoundException('Donor alert not found.');
        }

        $scopedHospitalId = $this->getScopedHospitalId($sessionUser);
        if ($scopedHospitalId !== null && $entry->getAlert()?->getHospitalId() !== $scopedHospitalId) {
            throw $this->createNotFoundException('Donor alert not found.');
        }

        return $entry;
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

    private function isUuid(string $value): bool
    {
        return preg_match(
            '/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i',
            $value,
        ) === 1;
    }
}
