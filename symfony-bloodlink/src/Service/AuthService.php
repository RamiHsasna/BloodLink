<?php

namespace App\Service;

use DateTimeImmutable;
use Doctrine\DBAL\Connection;
use Throwable;

class AuthService
{
    public function __construct(
        private readonly Connection $connection,
    ) {
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    public function getBloodTypes(): array
    {
        return $this->connection
            ->executeQuery('SELECT blood_type_id FROM blood_type ORDER BY blood_type_id ASC')
            ->fetchAllAssociative();
    }

    /**
     * @return array<string, mixed>|null
     */
    public function signIn(string $email, string $password): ?array
    {
        $normalizedEmail = trim($email);
        if ($normalizedEmail == '' || $password == '') {
            return null;
        }

        $user = $this->findUserByEmailInsensitive($normalizedEmail);
        if ($user === null) {
            return null;
        }

        $stored = (string) ($user['password_hash'] ?? '');
        if (hash_equals($stored, $password) || password_verify($password, $stored)) {
            return $user;
        }

        return null;
    }

    /**
     * @param array<string, mixed> $data
     *
        * @return array{success: bool, errors: array<string, string>, user: ?array<string, mixed>}
     */
    public function signUpDonor(array $data): array
    {
        $firstName = trim((string) ($data['first_name'] ?? ''));
        $lastName = trim((string) ($data['last_name'] ?? ''));
        $email = $this->normalizeEmail((string) ($data['email'] ?? ''));
        $phone = trim((string) ($data['phone'] ?? ''));
        $city = trim((string) ($data['city'] ?? ''));
        $bloodTypeId = trim((string) ($data['blood_type_id'] ?? ''));
        $password = (string) ($data['password'] ?? '');
        $confirmPassword = (string) ($data['confirm_password'] ?? '');
        $acceptedTerms = (bool) ($data['terms'] ?? false);

        // Lightweight debug log for signup attempts. Does not include raw passwords.
        try {
            $logDir = __DIR__ . '/../../var/log';
            if (!is_dir($logDir)) {
                @mkdir($logDir, 0755, true);
            }
            $debugLine = sprintf(
                "%s | signUp attempt | email=%s | first=%s | last=%s | phone=%s | city=%s | blood_type=%s | terms=%s\n",
                (new DateTimeImmutable())->format('Y-m-d H:i:s'),
                $email ?: '(none)',
                $firstName ?: '(none)',
                $lastName ?: '(none)',
                $phone ?: '(none)',
                $city ?: '(none)',
                $bloodTypeId ?: '(none)',
                $acceptedTerms ? '1' : '0'
            );
            @file_put_contents($logDir . '/signup_debug.log', $debugLine, FILE_APPEND | LOCK_EX);
        } catch (\Throwable) {
            // Swallow logging failures
        }

        $errors = [];

        if ($firstName == '') {
            $errors['first_name'] = 'First name is required.';
        } elseif (mb_strlen($firstName) > 100) {
            $errors['first_name'] = 'First name must be less than 100 characters.';
        } elseif (!$this->isValidPersonName($firstName)) {
            $errors['first_name'] = 'First name may only contain letters, spaces, hyphens, and apostrophes.';
        }

        if ($lastName == '') {
            $errors['last_name'] = 'Last name is required.';
        } elseif (mb_strlen($lastName) > 100) {
            $errors['last_name'] = 'Last name must be less than 100 characters.';
        } elseif (!$this->isValidPersonName($lastName)) {
            $errors['last_name'] = 'Last name may only contain letters, spaces, hyphens, and apostrophes.';
        }

        if ($email == '') {
            $errors['email'] = 'Email is required.';
        } elseif (mb_strlen($email) > 255) {
            $errors['email'] = 'Email must be less than 255 characters.';
        } elseif (!$this->isValidEmailAddress($email)) {
            $errors['email'] = 'Please enter a valid email address.';
        }

        if ($phone !== '') {
            if (mb_strlen($phone) > 20) {
                $errors['phone'] = 'Phone number must be less than 20 characters.';
            } elseif (!$this->isValidPhoneNumber($phone)) {
                $errors['phone'] = 'Phone number must contain 8 to 15 digits.';
            }
        }

        if ($city !== '' && mb_strlen($city) > 100) {
            $errors['city'] = 'City name is too long.';
        }
        if ($bloodTypeId == '') {
            $errors['blood_type_id'] = 'Please select a blood type.';
        }
        if ($password == '') {
            $errors['password'] = 'Password is required.';
        } elseif (mb_strlen($password) < 8) {
            $errors['password'] = 'Password must be at least 8 characters.';
        }
        if ($confirmPassword == '') {
            $errors['confirm_password'] = 'Please confirm your password.';
        } elseif ($confirmPassword !== $password) {
            $errors['confirm_password'] = 'Passwords do not match.';
        }
        if (!$acceptedTerms) {
            $errors['terms'] = 'Please accept the Terms of Service and Privacy Policy.';
        }

        $bloodTypeExists = false;
        if ($bloodTypeId !== '') {
            $bloodTypeExists = (bool) $this->connection->fetchOne(
                'SELECT 1 FROM blood_type WHERE blood_type_id = ? LIMIT 1',
                [$bloodTypeId],
            );
        }

        if (!$bloodTypeExists) {
            $errors['blood_type_id'] = 'Selected blood type is not valid.';
        }

        if ($errors !== []) {
            try {
                $logDir = __DIR__ . '/../../var/log';
                $errorLine = sprintf(
                    "%s | signUp validation failed | email=%s | errors=%s\n",
                    (new DateTimeImmutable())->format('Y-m-d H:i:s'),
                    $email,
                    json_encode($errors)
                );
                @file_put_contents($logDir . '/signup_debug.log', $errorLine, FILE_APPEND | LOCK_EX);
            } catch (\Throwable) {}

            return [
                'success' => false,
                'errors' => $errors,
                'user' => null,
            ];
        }

        $userId = $this->generateUuidV4();
        $createdAt = (new DateTimeImmutable())->format('Y-m-d H:i:s');
        $passwordHash = password_hash($password, PASSWORD_DEFAULT);

        if ($passwordHash === false) {
            return [
                'success' => false,
                'errors' => ['global' => 'Unable to secure your password right now. Please try again.'],
                'user' => null,
            ];
        }

        try {
            $this->connection->beginTransaction();

            $this->connection->executeStatement(
                'INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone, user_type, created_at) VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?)',
                [
                    $userId,
                    $email,
                    $passwordHash,
                    $firstName,
                    $lastName,
                    $phone === '' ? null : $phone,
                    'DONOR',
                    $createdAt,
                ],
            );

            $this->connection->executeStatement(
                'INSERT INTO donors (user_id, first_name, last_name, city, blood_type_id, is_currently_eligible, total_donations, created_at) VALUES (?::uuid, ?, ?, ?, ?, true, 0, ?)',
                [
                    $userId,
                    $firstName,
                    $lastName,
                    $city === '' ? null : $city,
                    $bloodTypeId,
                    $createdAt,
                ],
            );

            $this->connection->commit();

            try {
                $logDir = __DIR__ . '/../../var/log';
                $successLine = sprintf(
                    "%s | signUp success | email=%s | userId=%s\n",
                    (new DateTimeImmutable())->format('Y-m-d H:i:s'),
                    $email,
                    $userId
                );
                @file_put_contents($logDir . '/signup_debug.log', $successLine, FILE_APPEND | LOCK_EX);
            } catch (\Throwable) {}
        } catch (Throwable $e) {
            if ($this->connection->isTransactionActive()) {
                $this->connection->rollBack();
            }

            try {
                $logDir = __DIR__ . '/../../var/log';
                $dbErrorLine = sprintf(
                    "%s | signUp database error | email=%s | message=%s | code=%s\n",
                    (new DateTimeImmutable())->format('Y-m-d H:i:s'),
                    $email,
                    $e->getMessage(),
                    $e->getCode()
                );
                @file_put_contents($logDir . '/signup_debug.log', $dbErrorLine, FILE_APPEND | LOCK_EX);
            } catch (\Throwable) {}

            $message = 'Unable to create account right now. Please try again.';
            if (($e->getCode() !== 0 || $e->getMessage() !== '')) {
                $message .= ' (' . $e->getMessage() . ')';
            }

            return [
                'success' => false,
                'errors' => ['global' => $message],
                'user' => null,
            ];
        }

        return [
            'success' => true,
            'errors' => [],
            'user' => [
                'user_id' => $userId,
                'email' => $email,
                'first_name' => $firstName,
                'last_name' => $lastName,
                'user_type' => 'DONOR',
            ],
        ];
    }

    private function normalizeEmail(string $email): string
    {
        $normalized = trim($email);
        if ($normalized === '') {
            return '';
        }

        $normalized = preg_replace('/\s+/u', '', $normalized) ?? $normalized;
        if (str_starts_with(strtolower($normalized), 'mailto:')) {
            $normalized = substr($normalized, 7);
        }

        return $normalized;
    }

    private function isValidEmailAddress(string $email): bool
    {
        return filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
    }

    private function isValidPersonName(string $value): bool
    {
        // Allow Unicode letters, spaces, hyphens, and apostrophes
        return preg_match('/^[\p{L} \-\']+$/u', $value) === 1;
    }

    private function isValidPhoneNumber(string $value): bool
    {
        return preg_match('/^[0-9]{8,15}$/', $value) === 1;
    }

    /**
     * @return array<string, mixed>|null
     */
    private function findUserByEmailInsensitive(string $email): ?array
    {
        if ($email == '') {
            return null;
        }

        $user = $this->connection->fetchAssociative(
            'SELECT u.user_id,
                    u.email,
                    u.password_hash,
                    u.first_name,
                    u.last_name,
                    u.user_type,
                    hs.hospital_id::text AS hospital_id,
                    h.name AS hospital_name
             FROM users u
             LEFT JOIN hospital_staff hs ON hs.user_id = u.user_id
             LEFT JOIN hospital h ON h.hospital_id = hs.hospital_id
             WHERE LOWER(u.email) = LOWER(?)
             LIMIT 1',
            [$email],
        );

        return $user !== false ? $user : null;
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
