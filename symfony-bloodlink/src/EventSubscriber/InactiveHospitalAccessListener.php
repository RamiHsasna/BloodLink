<?php

namespace App\EventSubscriber;

use App\Service\HospitalService;
use App\Service\AccessControlService;
use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpKernel\Event\ControllerEvent;
use Symfony\Component\HttpKernel\KernelEvents;
use Symfony\Component\HttpFoundation\Request;

/**
 * Event listener to handle access control for inactive hospitals.
 * Automatically reactivates hospitals with expired deactivation periods
 * and checks hospital status before allowing staff operations.
 */
class InactiveHospitalAccessListener implements EventSubscriberInterface
{
    public function __construct(
        private readonly HospitalService $hospitalService,
        private readonly AccessControlService $accessControlService,
    ) {}

    public static function getSubscribedEvents(): array
    {
        return [
            KernelEvents::CONTROLLER => 'onKernelController',
        ];
    }

    /**
     * Hook into controller execution to auto-check and reactivate expired hospitals.
     * This happens silently - if a hospital has expired deactivation, it gets reactivated
     * and the user can proceed.
     */
    public function onKernelController(ControllerEvent $event): void
    {
        $request = $event->getRequest();
        $session = $request->getSession();

        // Get session user
        $sessionUser = $session->get('auth_user');
        if (!$sessionUser) {
            return; // Not logged in
        }

        // Only check for hospital staff (not admins or donors)
        $userType = strtoupper($sessionUser['user_type'] ?? '');
        if ($userType !== 'HOSPITAL_STAFF') {
            return; // Not hospital staff
        }

        // Get hospital ID from session
        $hospitalId = $sessionUser['hospital_id'] ?? null;
        if (!$hospitalId) {
            return; // No hospital assigned
        }

        try {
            // Auto-check and reactivate if expired (silent)
            $hospital = $this->hospitalService->checkAndAutoReactivateIfExpired($hospitalId);

            // Store updated hospital info in session
            $this->updateSessionHospitalInfo($session, $hospitalId);
        } catch (\Exception $e) {
            // Log but don't fail - hospital access control is handled per-operation
            error_log("Error checking hospital status: " . $e->getMessage());
        }
    }

    /**
     * Update hospital info in session after auto-reactivation.
     */
    private function updateSessionHospitalInfo($session, string $hospitalId): void
    {
        try {
            $hospital = $this->hospitalService->getHospitalById($hospitalId);
            if (!$hospital) {
                return;
            }

            $sessionUser = $session->get('auth_user');
            if ($sessionUser) {
                $sessionUser['hospital_active'] = $hospital->isCurrentlyActive();
                $session->set('auth_user', $sessionUser);
            }
        } catch (\Exception $e) {
            // Ignore errors
        }
    }
}
