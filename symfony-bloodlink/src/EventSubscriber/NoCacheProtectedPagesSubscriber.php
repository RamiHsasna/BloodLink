<?php

namespace App\EventSubscriber;

use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpKernel\Event\ResponseEvent;
use Symfony\Component\HttpKernel\KernelEvents;

class NoCacheProtectedPagesSubscriber implements EventSubscriberInterface
{
    public static function getSubscribedEvents(): array
    {
        return [
            KernelEvents::RESPONSE => 'onKernelResponse',
        ];
    }

    public function onKernelResponse(ResponseEvent $event): void
    {
        if (!$event->isMainRequest()) {
            return;
        }

        $request = $event->getRequest();
        $response = $event->getResponse();

        $routeName = (string) $request->attributes->get('_route', '');
        $isProtectedRoute = str_starts_with($routeName, 'dashboard_');

        $hasAuthSession = false;
        if ($request->hasSession()) {
            $hasAuthSession = $request->getSession()->has('auth_user');
        }

        if (!$isProtectedRoute && !$hasAuthSession) {
            return;
        }

        // Prevent browsers from serving cached protected pages after logout.
        $response->headers->set('Cache-Control', 'no-store, no-cache, must-revalidate, private, max-age=0');
        $response->headers->set('Pragma', 'no-cache');
        $response->headers->set('Expires', '0');
    }
}
