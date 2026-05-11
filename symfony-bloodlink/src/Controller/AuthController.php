<?php

namespace App\Controller;

use App\Service\AuthService;
use App\Service\HospitalService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

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

    #[Route('/auth/sign-in', name: 'auth_sign_in', methods: ['GET', 'POST'])]
    public function signIn(Request $request, AuthService $authService, HospitalService $hospitalService): Response
    {
        if ($request->isMethod('GET')) {
            return $this->redirectToRoute('auth_index', ['tab' => 'signin']);
        }

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
                // Check if hospital staff's hospital is deactivated
                $userType = strtoupper((string) ($user['user_type'] ?? ''));
                if ($userType === 'HOSPITAL_STAFF' && isset($user['hospital_id']) && $user['hospital_id'] !== null) {
                    $hospital = $hospitalService->getHospitalById($user['hospital_id']);
                    if ($hospital !== null) {
                        $isActive = $hospital->isCurrentlyActive();
                        if (!$isActive) {
                            $deactivationReason = $hospital->getDeactivationReason() ?? 'Unknown reason';
                            $deactivationEndDate = $hospital->getDeactivationEndDate();

                            if ($deactivationEndDate === null) {
                                // Indefinite deactivation
                                $errors['login'] = sprintf(
                                    'Your hospital is currently deactivated and is not accepting staff logins. Reason: %s. Please contact your administrator for more information.',
                                    $deactivationReason
                                );
                            } else {
                                $formattedDate = $deactivationEndDate->format('M d, Y \a\t g:i A');
                                $errors['login'] = sprintf(
                                    'Your hospital is currently deactivated and is not accepting staff logins until %s. Reason: %s. Please try again later.',
                                    $formattedDate,
                                    $deactivationReason
                                );
                            }
                        }
                    }
                }

                // If no deactivation errors, proceed with login
                if (!isset($errors['login'])) {
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

                    if ($userType === 'HOSPITAL_STAFF') {
                        return $this->redirectToRoute('dashboard_hospital_staff_home');
                    } elseif ($userType === 'ADMIN') {
                        return $this->redirectToRoute('dashboard_users');
                    } else {
                        return $this->redirectToRoute('dashboard_donor_home');
                    }
                }
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

    #[Route('/auth/sign-up', name: 'auth_sign_up', methods: ['GET', 'POST'])]
    public function signUp(Request $request, AuthService $authService): Response
    {
        if ($request->isMethod('GET')) {
            return $this->redirectToRoute('auth_index', ['tab' => 'signup']);
        }

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
        $session = $request->getSession();
        $session->remove('auth_user');
        $session->invalidate();

        $response = $this->redirectToRoute('auth_index');
        $response->headers->set('Cache-Control', 'no-store, no-cache, must-revalidate, private, max-age=0');
        $response->headers->set('Pragma', 'no-cache');
        $response->headers->set('Expires', '0');

        return $response;
    }

    #[Route('/auth/forgot-password', name: 'auth_forgot_password', methods: ['GET', 'POST'])]
    public function forgotPassword(Request $request, AuthService $authService, MailerInterface $mailer): Response
    {
        if ($request->isMethod('POST')) {
            $emailAddress = trim((string) $request->request->get('email', ''));
            $token = $authService->createPasswordResetToken($emailAddress);

            if ($token) {
                $resetUrl = $this->generateUrl('auth_reset_password', ['token' => $token], UrlGeneratorInterface::ABSOLUTE_URL);

                // For debug purposes, also log the link
                @file_put_contents($this->getParameter('kernel.logs_dir') . '/reset_links.log', sprintf("[%s] Reset link for %s: %s\n", date('Y-m-d H:i:s'), $emailAddress, $resetUrl), FILE_APPEND);

                try {
                    $email = (new Email())
                        ->from('bloodlink.supportteam@gmail.com')
                        ->to($emailAddress)
                        ->subject('Password Reset Request for BloodLink')
                        ->html("
                            <div style=\"font-family:'Segoe UI',Tahoma,sans-serif;color:#333;background-color:#f6f9fc;padding:40px 0;\">
                                <div style=\"max-width:600px;margin:0 auto;background-color:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.05);\">
                                    <div style=\"padding:30px 40px;text-align:left;\">
                                        <div style=\"display:inline-block;vertical-align:middle;margin-right:10px;width:32px;height:32px;background-color:#c52228;border-radius:50% 50% 50% 0;transform:rotate(-45deg);margin-top:-5px;\"></div>
                                        <span style=\"color:#c52228;font-size:28px;font-weight:700;display:inline-block;vertical-align:middle;\">BloodLink</span>
                                    </div>
                                    <div style=\"padding:0 40px 40px 40px;\">
                                        <h1 style=\"font-size:22px;font-weight:700;margin:0 0 24px 0;color:#1a1a1a;\">Password Reset Request</h1>
                                        <div style=\"font-size:16px;line-height:1.6;color:#444444;margin-bottom:30px;\">
                                            <p>You requested a password reset.</p>
                                            <p>Click the link below to set a new password.</p>
                                        </div>
                                        <div style=\"text-align:center;\">
                                            <a href=\"$resetUrl\" style=\"background-color:#c52228;color:#ffffff;padding:16px 32px;text-decoration:none;border-radius:8px;font-weight:700;font-size:16px;display:inline-block;\">Reset Your Password</a>
                                        </div>
                                    </div>
                                    <div style=\"padding:0 40px 40px 40px;font-size:14px;color:#666666;line-height:1.5;\">
                                        <p>Regards,</p>
                                        <p style=\"margin-top:20px;font-weight:600;color:#333333;\">The BloodLink Team</p>
                                    </div>
                                </div>
                            </div>
                        ");

                    $mailer->send($email);
                } catch (\Exception $e) {
                    // Log error but don't show to user to avoid leaking info
                    @file_put_contents($this->getParameter('kernel.logs_dir') . '/mailer_errors.log', sprintf("[%s] Error sending to %s: %s\n", date('Y-m-d H:i:s'), $emailAddress, $e->getMessage()), FILE_APPEND);
                }
            }

            $this->addFlash('success', 'If an account exists with that email, we have sent a password reset link.');
            return $this->redirectToRoute('auth_index');
        }

        return $this->render('auth/forgot_password.html.twig');
    }

    #[Route('/auth/reset-password/{token}', name: 'auth_reset_password', methods: ['GET', 'POST'])]
    public function resetPassword(string $token, Request $request, AuthService $authService): Response
    {
        $user = $authService->getUserByResetToken($token);
        if (!$user) {
            $this->addFlash('error', 'Invalid or expired reset token.');
            return $this->redirectToRoute('auth_index');
        }

        if ($request->isMethod('POST')) {
            $password = (string) $request->request->get('password', '');
            $confirmPassword = (string) $request->request->get('confirm_password', '');

            if ($password === '' || $password !== $confirmPassword) {
                $this->addFlash('error', 'Passwords do not match or are empty.');
            } else {
                if ($authService->resetPassword($token, $password)) {
                    $this->addFlash('success', 'Password reset successfully. You can now sign in.');
                    return $this->redirectToRoute('auth_index');
                } else {
                    $this->addFlash('error', 'An error occurred. Please try again.');
                }
            }
        }

        return $this->render('auth/reset_password.html.twig', [
            'token' => $token,
        ]);
    }
}
