<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[
    ORM\Entity(
        repositoryClass: App\Entity\Repository\DonorEligibilityRepository::class,
    ),
]
#[ORM\Table(name: "donor_eligibility")]
class DonorEligibility
{
    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $donorEligibilityId;

    #[ORM\Column(type: "boolean")]
    private bool $isCurrentlyEligible;

    #[ORM\Column(type: "integer", nullable: true)]
    private int|null $daysUntilEligible = null;

    #[ORM\Column(type: "date", nullable: true)]
    private \DateTimeInterface|null $lastCalculatedAt = null;

    #[ORM\Column(type: "decimal", precision: 10, scale: 8, nullable: true)]
    private string|null $latitudeCache = null;

    #[ORM\Column(type: "decimal", precision: 11, scale: 8, nullable: true)]
    private string|null $longitudeCache = null;

    #[ORM\Column(type: "string", length: 3, nullable: true)]
    private string|null $bloodTypeCache = null;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $eligibilityDetails = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: "user_id", referencedColumnName: "user_id")]
    private ?User $user = null;

    public function getDonorEligibilityId(): string
    {
        return $this->donorEligibilityId;
    }

    public function setDonorEligibilityId(string $donorEligibilityId): static
    {
        $this->donorEligibilityId = $donorEligibilityId;

        return $this;
    }

    public function getIsCurrentlyEligible(): bool
    {
        return $this->isCurrentlyEligible;
    }

    public function setIsCurrentlyEligible(bool $isCurrentlyEligible): static
    {
        $this->isCurrentlyEligible = $isCurrentlyEligible;

        return $this;
    }

    public function getDaysUntilEligible(): int|null
    {
        return $this->daysUntilEligible;
    }

    public function setDaysUntilEligible(?int $daysUntilEligible): static
    {
        $this->daysUntilEligible = $daysUntilEligible;

        return $this;
    }

    public function getLastCalculatedAt(): \DateTimeInterface|null
    {
        return $this->lastCalculatedAt;
    }

    public function setLastCalculatedAt(
        ?\DateTimeInterface $lastCalculatedAt,
    ): static {
        $this->lastCalculatedAt = $lastCalculatedAt;

        return $this;
    }

    public function getLatitudeCache(): string|null
    {
        return $this->latitudeCache;
    }

    public function setLatitudeCache(?string $latitudeCache): static
    {
        $this->latitudeCache = $latitudeCache;

        return $this;
    }

    public function getLongitudeCache(): string|null
    {
        return $this->longitudeCache;
    }

    public function setLongitudeCache(?string $longitudeCache): static
    {
        $this->longitudeCache = $longitudeCache;

        return $this;
    }

    public function getBloodTypeCache(): string|null
    {
        return $this->bloodTypeCache;
    }

    public function setBloodTypeCache(?string $bloodTypeCache): static
    {
        $this->bloodTypeCache = $bloodTypeCache;

        return $this;
    }

    public function getEligibilityDetails(): string|null
    {
        return $this->eligibilityDetails;
    }

    public function setEligibilityDetails(?string $eligibilityDetails): static
    {
        $this->eligibilityDetails = $eligibilityDetails;

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
