<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[ORM\Entity(repositoryClass: App\Entity\Repository\BloodTypeRepository::class)]
#[ORM\Table(name: "blood_type")]
class BloodType
{
    #[ORM\Id]
    #[ORM\Column(type: "string", length: 3)]
    private string $bloodTypeId;

    #[ORM\Column(type: "string", length: 2)]
    private string $aboType;

    #[ORM\Column(type: "string", length: 8)]
    private string $rhFactor;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isUniversalDonor = null;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isUniversalRecipient = null;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $compatibleDonors = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $updatedAt = null;

    public function getBloodTypeId(): string
    {
        return $this->bloodTypeId;
    }

    public function setBloodTypeId(string $bloodTypeId): static
    {
        $this->bloodTypeId = $bloodTypeId;

        return $this;
    }

    public function getAboType(): string
    {
        return $this->aboType;
    }

    public function setAboType(string $aboType): static
    {
        $this->aboType = $aboType;

        return $this;
    }

    public function getRhFactor(): string
    {
        return $this->rhFactor;
    }

    public function setRhFactor(string $rhFactor): static
    {
        $this->rhFactor = $rhFactor;

        return $this;
    }

    public function getIsUniversalDonor(): bool|null
    {
        return $this->isUniversalDonor;
    }

    public function setIsUniversalDonor(?bool $isUniversalDonor): static
    {
        $this->isUniversalDonor = $isUniversalDonor;

        return $this;
    }

    public function getIsUniversalRecipient(): bool|null
    {
        return $this->isUniversalRecipient;
    }

    public function setIsUniversalRecipient(?bool $isUniversalRecipient): static
    {
        $this->isUniversalRecipient = $isUniversalRecipient;

        return $this;
    }

    public function getCompatibleDonors(): string|null
    {
        return $this->compatibleDonors;
    }

    public function setCompatibleDonors(?string $compatibleDonors): static
    {
        $this->compatibleDonors = $compatibleDonors;

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

    public function getUpdatedAt(): \DateTimeInterface|null
    {
        return $this->updatedAt;
    }

    public function setUpdatedAt(?\DateTimeInterface $updatedAt): static
    {
        $this->updatedAt = $updatedAt;

        return $this;
    }
}
