<?php

namespace App\Controller;

use App\Service\AuthService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class AuthController extends AbstractController
{
    #[Route('/', name: 'home', methods: ['GET'])]
    #[Route('/auth', name: 'auth_index', methods: ['GET'])]
    public function index(Request $request, AuthService $authService): Response
    {
        $activeTab = (string) $request->query->get('tab', 'signin');
        if (!in_array($activeTab, ['signin', 'signup'], true)) {
            $activeTab = 'signin';
        }

        $signInEmail = (string) $request->query->get('email', '');

        return $this->render('auth/index.html.twig', [
            'active_tab' => $activeTab,
            'blood_types' => $authService->getBloodTypes(),
            'sign_in_errors' => [],
            'sign_up_errors' => [],
            'sign_up_old' => [],
            'sign_in_email' => $signInEmail,
            'session_user' => $request->getSession()->get('auth_user'),
        ]);
    }

    #[Route('/auth/sign-in', name: 'auth_sign_in', methods: ['POST'])]
    public function signIn(Request $request, AuthService $authService): Response
    {
        $email = trim((string) $request->request->get('sign_in_email', ''));
        $password = (string) $request->request->get('sign_in_password', '');

        $errors = [];
        if ($email === '') {
            $errors['email'] = 'Email is required.';
        }
        if ($password === '') {
            $errors['password'] = 'Password is required.';
        }

        if ($errors === []) {
            $user = $authService->signIn($email, $password);
            if ($user === null) {
                $errors['password'] = 'Invalid email or password.';
            } else {
                $request->getSession()->set('auth_user', [
                    'id' => (string) ($user['user_id'] ?? ''),
                    'email' => (string) ($user['email'] ?? ''),
                    'first_name' => (string) ($user['first_name'] ?? ''),
                    'last_name' => (string) ($user['last_name'] ?? ''),
                    'user_type' => (string) ($user['user_type'] ?? ''),
                    'hospital_id' => isset($user['hospital_id']) && $user['hospital_id'] !== null
                        ? (string) $user['hospital_id']
                        : null,
                    'hospital_name' => isset($user['hospital_name']) && $user['hospital_name'] !== null
                        ? trim((string) $user['hospital_name'])
                        : null,
                ]);

                $this->addFlash('success', 'Signed in successfully.');

                $isDonor = strtoupper((string) ($user['user_type'] ?? '')) === 'DONOR';

                return $this->redirectToRoute($isDonor ? 'dashboard_donor_home' : 'dashboard_users');
            }
        }

        return $this->render('auth/index.html.twig', [
            'active_tab' => 'signin',
            'blood_types' => $authService->getBloodTypes(),
            'sign_in_errors' => $errors,
            'sign_up_errors' => [],
            'sign_up_old' => [],
            'sign_in_email' => $email,
            'session_user' => $request->getSession()->get('auth_user'),
        ]);
    }

    #[Route('/auth/sign-up', name: 'auth_sign_up', methods: ['POST'])]
    public function signUp(Request $request, AuthService $authService): Response
    {
        $data = [
            'first_name' => (string) $request->request->get('first_name', ''),
            'last_name' => (string) $request->request->get('last_name', ''),
            'email' => (string) $request->request->get('email', ''),
            'phone' => (string) $request->request->get('phone', ''),
            'city' => (string) $request->request->get('city', ''),
            'blood_type_id' => (string) $request->request->get('blood_type_id', ''),
            'password' => (string) $request->request->get('password', ''),
            'confirm_password' => (string) $request->request->get('confirm_password', ''),
            'terms' => $request->request->get('terms') === '1',
        ];

        $result = $authService->signUpDonor($data);

        if ($result['success']) {
            $this->addFlash('success', 'Account created successfully. Please sign in.');

            return $this->redirectToRoute('auth_index', [
                'tab' => 'signin',
                'email' => (string) $data['email'],
            ]);
        }

        return $this->render('auth/index.html.twig', [
            'active_tab' => 'signup',
            'blood_types' => $authService->getBloodTypes(),
            'sign_in_errors' => [],
            'sign_up_errors' => $result['errors'],
            'sign_up_old' => $data,
            'sign_in_email' => '',
            'session_user' => $request->getSession()->get('auth_user'),
        ]);
    }

    #[Route('/auth/logout', name: 'auth_logout', methods: ['POST'])]
    public function logout(Request $request): Response
    {
        $request->getSession()->remove('auth_user');
        $this->addFlash('success', 'You are now signed out.');

        return $this->redirectToRoute('auth_index');
    }
}
