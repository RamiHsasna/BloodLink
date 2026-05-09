<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: \App\Repository\UserRepository::class)]
#[ORM\Table(name: "users")]
class User
{
    public const TYPE_ADMIN = 'ADMIN';
    public const TYPE_DONOR = 'DONOR';
    public const TYPE_HOSPITAL_STAFF = 'HOSPITAL_STAFF';

    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $userId;

    #[ORM\Column(type: "string", length: 255)]
    private string $email;

    #[ORM\Column(type: "string", length: 255)]
    private string $passwordHash;

    #[ORM\Column(type: "string", length: 100)]
    private string $firstName;

    #[ORM\Column(type: "string", length: 100)]
    private string $lastName;

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    private string|null $phone = null;

    #[ORM\Column(type: "string", length: 20)]
    private string $userType;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    public function getUserId(): string
    {
        return $this->userId;
    }

    public function setUserId(string $userId): static
    {
        $this->userId = $userId;

        return $this;
    }

    public function getEmail(): string
    {
        return $this->email;
    }

    public function setEmail(string $email): static
    {
        $this->email = $email;

        return $this;
    }

    public function getPasswordHash(): string
    {
        return $this->passwordHash;
    }

    public function setPasswordHash(string $passwordHash): static
    {
        $this->passwordHash = $passwordHash;

        return $this;
    }

    public function getFirstName(): string
    {
        return $this->firstName;
    }

    public function setFirstName(string $firstName): static
    {
        $this->firstName = $firstName;

        return $this;
    }

    public function getLastName(): string
    {
        return $this->lastName;
    }

    public function setLastName(string $lastName): static
    {
        $this->lastName = $lastName;

        return $this;
    }

    public function getPhone(): string|null
    {
        return $this->phone;
    }

    public function setPhone(?string $phone): static
    {
        $this->phone = $phone;

        return $this;
    }

    public function getUserType(): string
    {
        return $this->userType;
    }

    public function setUserType(string $userType): static
    {
        $this->userType = $userType;

        return $this;
    }

    public function getCreatedAt(): \DateTimeInterface|null
    {
        return $this->createdAt;
    }

    public function setCreatedAt(?\DateTimeInterface $createdAt): static
    {
        $this->createdAt = $createdAt;

        return $this;
    }

    /**
     * @return array<int, string>
     */
    public static function backOfficeTypes(): array
    {
        return [
            self::TYPE_ADMIN,
            self::TYPE_HOSPITAL_STAFF,
        ];
    }
}
