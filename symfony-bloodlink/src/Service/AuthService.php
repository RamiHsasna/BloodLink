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
        $email = trim((string) ($data['email'] ?? ''));
        $phone = trim((string) ($data['phone'] ?? ''));
        $city = trim((string) ($data['city'] ?? ''));
        $bloodTypeId = trim((string) ($data['blood_type_id'] ?? ''));
        $password = (string) ($data['password'] ?? '');
        $confirmPassword = (string) ($data['confirm_password'] ?? '');
        $acceptedTerms = (bool) ($data['terms'] ?? false);

        $errors = [];

        if ($firstName == '') {
            $errors['first_name'] = 'First name is required.';
        }
        if ($lastName == '') {
            $errors['last_name'] = 'Last name is required.';
        }
        if ($email == '') {
            $errors['email'] = 'Email is required.';
        } elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $errors['email'] = 'Please enter a valid email address.';
        }
        if ($city == '') {
            $errors['city'] = 'City is required.';
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

        if ($email !== '' && $this->findUserByEmailInsensitive($email) !== null) {
            $errors['email'] = 'An account with this email already exists.';
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
            return [
                'success' => false,
                'errors' => $errors,
                'user' => null,
            ];
        }

        $userId = $this->generateUuidV4();
        $createdAt = (new DateTimeImmutable())->format('Y-m-d H:i:s');

        try {
            $this->connection->beginTransaction();

            $this->connection->executeStatement(
                'INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone, user_type, created_at) VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?)',
                [
                    $userId,
                    $email,
                    $password,
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
                    $city,
                    $bloodTypeId,
                    $createdAt,
                ],
            );

            $this->connection->commit();
        } catch (Throwable) {
            if ($this->connection->isTransactionActive()) {
                $this->connection->rollBack();
            }

            return [
                'success' => false,
                'errors' => ['global' => 'Unable to create account right now. Please try again.'],
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

    /**
     * @return array<string, mixed>|null
     */
    private function findUserByEmailInsensitive(string $email): ?array
    {
        if ($email == '') {
            return null;
        }

        $user = $this->connection->fetchAssociative(
            'SELECT user_id, email, password_hash, first_name, last_name, user_type FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1',
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
