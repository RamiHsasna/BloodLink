<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[ORM\Entity(repositoryClass: \App\Repository\DonorRepository::class)]
#[ORM\Table(name: "donors")]
class Donor
{
    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $userId;

    #[ORM\Column(type: "date", nullable: true)]
    private \DateTimeInterface|null $lastDonationDate = null;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isCurrentlyEligible = null;

    #[ORM\Column(type: "decimal", precision: 10, scale: 8, nullable: true)]
    private string|null $latitude = null;

    #[ORM\Column(type: "decimal", precision: 11, scale: 8, nullable: true)]
    private string|null $longitude = null;

    #[ORM\Column(type: "integer", nullable: true)]
    private int|null $totalDonations = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    #[ORM\Column(type: "text")]
    private string $firstName;

    #[ORM\Column(type: "text")]
    private string $lastName;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $city = null;

    #[ORM\ManyToOne(targetEntity: BloodType::class)]
    #[
        ORM\JoinColumn(
            name: "blood_type_id",
            referencedColumnName: "blood_type_id",
        ),
    ]
    private ?BloodType $bloodType = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: "user_id", referencedColumnName: "user_id")]
    private ?User $user = null;

    public function getUserId(): string
    {
        return $this->userId;
    }

    public function setUserId(string $userId): static
    {
        $this->userId = $userId;

        return $this;
    }

    public function getLastDonationDate(): \DateTimeInterface|null
    {
        return $this->lastDonationDate;
    }

    public function setLastDonationDate(
        ?\DateTimeInterface $lastDonationDate,
    ): static {
        $this->lastDonationDate = $lastDonationDate;

        return $this;
    }

    public function getIsCurrentlyEligible(): bool|null
    {
        return $this->isCurrentlyEligible;
    }

    public function setIsCurrentlyEligible(?bool $isCurrentlyEligible): static
    {
        $this->isCurrentlyEligible = $isCurrentlyEligible;

        return $this;
    }

    public function getLatitude(): string|null
    {
        return $this->latitude;
    }

    public function setLatitude(?string $latitude): static
    {
        $this->latitude = $latitude;

        return $this;
    }

    public function getLongitude(): string|null
    {
        return $this->longitude;
    }

    public function setLongitude(?string $longitude): static
    {
        $this->longitude = $longitude;

        return $this;
    }

    public function getTotalDonations(): int|null
    {
        return $this->totalDonations;
    }

    public function setTotalDonations(?int $totalDonations): static
    {
        $this->totalDonations = $totalDonations;

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

    public function getCity(): string|null
    {
        return $this->city;
    }

    public function setCity(?string $city): static
    {
        $this->city = $city;

        return $this;
    }

    public function getBloodType(): ?BloodType
    {
        return $this->bloodType;
    }

    public function setBloodType(?BloodType $bloodType): static
    {
        $this->bloodType = $bloodType;

        return $this;
    }

    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(?User $user): static
    {
        $this->user = $user;

        return $this;
    }
}
