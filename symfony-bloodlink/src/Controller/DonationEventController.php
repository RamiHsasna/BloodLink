<?php

namespace App\Controller;

use App\Entity\DonationEvent;
use App\Entity\Donor;
use App\Entity\Hospital;
use App\Entity\User;
use App\Form\DonationEventType;
use App\Repository\DonationEventRepository;
use DateTimeImmutable;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Throwable;

class DonationEventController extends AbstractController
{
    public function __construct(
        private readonly DonationEventRepository $donationEventRepository,
        private readonly EntityManagerInterface $entityManager,
    ) {
    }

    #[Route('/dashboard/donation-events', name: 'dashboard_events_index', methods: ['GET'])]
    public function index(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $query = trim((string) $request->query->get('q', ''));
        $status = strtoupper(trim((string) $request->query->get('status', '')));
        if (!in_array($status, DonationEvent::allowedStatuses(), true)) {
            $status = '';
        }

        $result = $this->donationEventRepository->searchPaginated(
            $query,
            $status,
            $hospitalId,
            max(1, $request->query->getInt('page', 1)),
            9,
        );
        $donationCounts = $this->donationEventRepository->getDonationCountsForEvents(
            array_map(
                static fn (DonationEvent $event): string => $event->getEventId(),
                $result['items'],
            ),
        );

        return $this->render('donation_events/back_office/index.html.twig', [
            'session_user' => $sessionUser,
            'filters' => [
                'q' => $query,
                'status' => $status,
            ],
            'result' => $result,
            'entries' => $result['items'],
            'donation_counts' => $donationCounts,
            'summary_cards' => [
                [
                    'label' => 'All Events',
                    'value' => $result['total'],
                    'caption' => 'Donation-event records currently visible in this workspace.',
                ],
                [
                    'label' => 'Upcoming',
                    'value' => $this->donationEventRepository->countUpcoming($hospitalId),
                    'caption' => 'Events whose schedule is still open or upcoming.',
                ],
                [
                    'label' => 'Active',
                    'value' => $this->donationEventRepository->countByStatus(DonationEvent::STATUS_ACTIVE, $hospitalId),
                    'caption' => 'Live collection windows happening right now.',
                ],
                [
                    'label' => 'Planned',
                    'value' => $this->donationEventRepository->countByStatus(DonationEvent::STATUS_PLANNED, $hospitalId),
                    'caption' => 'Prepared campaigns waiting to go live.',
                ],
            ],
            'status_choices' => DonationEvent::statusChoices(),
        ]);
    }

    #[Route('/dashboard/donation-events/new', name: 'dashboard_events_new', methods: ['GET', 'POST'])]
    public function new(Request $request): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $hospitalId = $this->getScopedHospitalId($sessionUser);
        $now = new DateTimeImmutable();

        $event = (new DonationEvent())
            ->setEventId($this->generateUuidV4())
            ->setStatus(DonationEvent::STATUS_PLANNED)
            ->setCreatedAt($now)
            ->setUpdatedAt($now)
            ->setStartDate($now->modify('+1 day')->setTime(9, 0))
            ->setEndDate($now->modify('+1 day')->setTime(17, 0));

        if ($hospitalId !== null) {
            $hospital = $this->entityManager->find(Hospital::class, $hospitalId);
            if ($hospital instanceof Hospital) {
                $event->setHospital($hospital);
            }
        }

        $form = $this->createForm(DonationEventType::class, $event, [
            'lock_hospital' => $hospitalId !== null,
            'allowed_hospital_id' => $hospitalId,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $event->setUpdatedAt(new DateTimeImmutable());
            if ($event->getCreatedAt() === null) {
                $event->setCreatedAt(new DateTimeImmutable());
            }

            $this->entityManager->persist($event);
            $this->entityManager->flush();

            $this->addFlash('success', 'Donation event created successfully.');

            return $this->redirectToRoute('dashboard_events_show', ['eventId' => $event->getEventId()]);
        }

        return $this->render('donation_events/back_office/form.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'page_title' => 'Create Donation Event',
            'page_description' => 'Schedule a donor-facing collection campaign and assign it to the hosting hospital.',
            'submit_label' => 'Create Event',
            'entry' => $event,
        ]);
    }

    #[Route('/dashboard/donation-events/{eventId}', name: 'dashboard_events_show', methods: ['GET'])]
    public function show(Request $request, string $eventId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $event = $this->findEventOrThrow($eventId, $sessionUser);
        $donationCount = $this->donationEventRepository->getDonationCountsForEvents([$event->getEventId()])[$event->getEventId()] ?? 0;

        return $this->render('donation_events/back_office/show.html.twig', [
            'session_user' => $sessionUser,
            'entry' => $event,
            'donation_count' => $donationCount,
        ]);
    }

    #[Route('/dashboard/donation-events/{eventId}/edit', name: 'dashboard_events_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, string $eventId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $event = $this->findEventOrThrow($eventId, $sessionUser);
        $hospitalId = $this->getScopedHospitalId($sessionUser);

        $form = $this->createForm(DonationEventType::class, $event, [
            'lock_hospital' => $hospitalId !== null,
            'allowed_hospital_id' => $hospitalId,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $event->setUpdatedAt(new DateTimeImmutable());
            $this->entityManager->flush();

            $this->addFlash('success', 'Donation event updated successfully.');

            return $this->redirectToRoute('dashboard_events_show', ['eventId' => $event->getEventId()]);
        }

        return $this->render('donation_events/back_office/form.html.twig', [
            'session_user' => $sessionUser,
            'form' => $form->createView(),
            'page_title' => 'Edit Donation Event',
            'page_description' => 'Adjust the campaign schedule, blood-type focus, and hosting location.',
            'submit_label' => 'Save Changes',
            'entry' => $event,
        ]);
    }

    #[Route('/dashboard/donation-events/{eventId}/delete', name: 'dashboard_events_delete', methods: ['POST'])]
    public function delete(Request $request, string $eventId): Response
    {
        if ($guard = $this->denyUnlessBackOffice($request)) {
            return $guard;
        }

        $event = $this->findEventOrThrow($eventId, $this->getSessionUser($request));

        if ($this->isCsrfTokenValid('delete-donation-event-' . $eventId, (string) $request->request->get('_token'))) {
            try {
                $this->entityManager->remove($event);
                $this->entityManager->flush();
                $this->addFlash('success', 'Donation event deleted successfully.');
            } catch (Throwable) {
                $this->addFlash('error', 'This donation event cannot be deleted while linked donation records still reference it.');
            }
        } else {
            $this->addFlash('error', 'The delete request is invalid.');
        }

        return $this->redirectToRoute('dashboard_events_index');
    }

    #[Route('/dashboard/donor/events', name: 'dashboard_donor_events', methods: ['GET'])]
    public function donorIndex(Request $request): Response
    {
        if ($guard = $this->denyUnlessDonor($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $donor = $this->resolveDonorFromSession($sessionUser);
        $bloodTypeId = $donor?->getBloodType()?->getBloodTypeId();
        $events = $this->donationEventRepository->findUpcomingForDonor($bloodTypeId, 8);
        $donationCounts = $this->donationEventRepository->getDonationCountsForEvents(
            array_map(static fn (DonationEvent $event): string => $event->getEventId(), $events),
        );

        $matchedCount = 0;
        foreach ($events as $event) {
            if ($this->eventMatchesBloodType($event, $bloodTypeId)) {
                ++$matchedCount;
            }
        }

        return $this->render('donation_events/front/index.html.twig', [
            'session_user' => $sessionUser,
            'entries' => $events,
            'donation_counts' => $donationCounts,
            'donor_blood_type' => $bloodTypeId,
            'summary_cards' => [
                [
                    'label' => 'Upcoming Events',
                    'value' => count($events),
                    'caption' => 'Campaigns currently visible to the donor front office.',
                ],
                [
                    'label' => 'Matching Blood Type',
                    'value' => $matchedCount,
                    'caption' => $bloodTypeId !== null ? 'Events that explicitly mention ' . $bloodTypeId . '.' : 'No donor blood type could be resolved.',
                ],
                [
                    'label' => 'Always Read-only',
                    'value' => 'FO',
                    'caption' => 'Donors can inspect campaigns, but not manage them.',
                ],
            ],
        ]);
    }

    #[Route('/dashboard/donor/events/{eventId}', name: 'dashboard_donor_events_show', methods: ['GET'])]
    public function donorShow(Request $request, string $eventId): Response
    {
        if ($guard = $this->denyUnlessDonor($request)) {
            return $guard;
        }

        $sessionUser = $this->getSessionUser($request);
        $event = $this->findEventOrThrow($eventId);
        $donor = $this->resolveDonorFromSession($sessionUser);
        $bloodTypeId = $donor?->getBloodType()?->getBloodTypeId();
        $donationCount = $this->donationEventRepository->getDonationCountsForEvents([$event->getEventId()])[$event->getEventId()] ?? 0;

        return $this->render('donation_events/front/show.html.twig', [
            'session_user' => $sessionUser,
            'entry' => $event,
            'donation_count' => $donationCount,
            'donor_blood_type' => $bloodTypeId,
            'blood_type_match' => $this->eventMatchesBloodType($event, $bloodTypeId),
        ]);
    }

    /**
     * @return array<string, mixed>|null
     */
    private function getSessionUser(Request $request): ?array
    {
        $sessionUser = $request->getSession()->get('auth_user');

        return is_array($sessionUser) ? $sessionUser : null;
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

            return $this->redirectToRoute('dashboard_events_index');
        }

        return null;
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function getScopedHospitalId(?array $sessionUser): ?string
    {
        if (strtoupper((string) ($sessionUser['user_type'] ?? '')) !== User::TYPE_HOSPITAL_STAFF) {
            return null;
        }

        $hospitalId = trim((string) ($sessionUser['hospital_id'] ?? ''));

        return $hospitalId !== '' ? $hospitalId : null;
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function resolveDonorFromSession(?array $sessionUser): ?Donor
    {
        $userId = trim((string) ($sessionUser['id'] ?? ''));
        if ($userId === '') {
            return null;
        }

        $donor = $this->entityManager->find(Donor::class, $userId);

        return $donor instanceof Donor ? $donor : null;
    }

    /**
     * @param array<string, mixed>|null $sessionUser
     */
    private function findEventOrThrow(string $eventId, ?array $sessionUser = null): DonationEvent
    {
        if (!$this->isUuid($eventId)) {
            throw $this->createNotFoundException('Donation event not found.');
        }

        $event = $this->donationEventRepository->find($eventId);
        if (!$event instanceof DonationEvent) {
            throw $this->createNotFoundException('Donation event not found.');
        }

        $scopedHospitalId = $this->getScopedHospitalId($sessionUser);
        if ($scopedHospitalId !== null && $event->getHospital()?->getHospitalId() !== $scopedHospitalId) {
            throw $this->createNotFoundException('Donation event not found.');
        }

        return $event;
    }

    private function eventMatchesBloodType(DonationEvent $event, ?string $bloodTypeId): bool
    {
        if ($bloodTypeId === null || $bloodTypeId === '') {
            return false;
        }

        $targetBloodTypes = trim((string) $event->getTargetBloodTypes());
        if ($targetBloodTypes === '') {
            return false;
        }

        return str_contains(mb_strtolower($targetBloodTypes), mb_strtolower($bloodTypeId));
    }

    private function isUuid(string $value): bool
    {
        return preg_match(
            '/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i',
            $value,
        ) === 1;
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
}
