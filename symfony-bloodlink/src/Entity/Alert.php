<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[ORM\Entity(repositoryClass: App\Entity\Repository\AlertRepository::class)]
#[ORM\Table(name: "alerts")]
class Alert
{
    #[ORM\Id]
    #[ORM\Column(type: "string", length: 36)]
    private string $alertId;

    #[ORM\Column(type: "string", length: 36)]
    private string $hospitalId;

    #[ORM\Column(type: "string", length: 36)]
    private string $staffId;

    #[ORM\Column(type: "string", length: 3)]
    private string $bloodTypeId;

    #[ORM\Column(type: "string", length: 20)]
    private string $severity;

    #[ORM\Column(type: "integer")]
    private int $quantityNeeded;

    #[ORM\Column(type: "string", length: 255)]
    private string $title;

    #[ORM\Column(type: "text")]
    private string $message;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isResolved = null;

    #[ORM\Column(type: "integer", nullable: true)]
    private int|null $targetRadiusKm = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $resolvedAt = null;

    public function getAlertId(): string
    {
        return $this->alertId;
    }

    public function setAlertId(string $alertId): static
    {
        $this->alertId = $alertId;

        return $this;
    }

    public function getHospitalId(): string
    {
        return $this->hospitalId;
    }

    public function setHospitalId(string $hospitalId): static
    {
        $this->hospitalId = $hospitalId;

        return $this;
    }

    public function getStaffId(): string
    {
        return $this->staffId;
    }

    public function setStaffId(string $staffId): static
    {
        $this->staffId = $staffId;

        return $this;
    }

    public function getBloodTypeId(): string
    {
        return $this->bloodTypeId;
    }

    public function setBloodTypeId(string $bloodTypeId): static
    {
        $this->bloodTypeId = $bloodTypeId;

        return $this;
    }

    public function getSeverity(): string
    {
        return $this->severity;
    }

    public function setSeverity(string $severity): static
    {
        $this->severity = $severity;

        return $this;
    }

    public function getQuantityNeeded(): int
    {
        return $this->quantityNeeded;
    }

    public function setQuantityNeeded(int $quantityNeeded): static
    {
        $this->quantityNeeded = $quantityNeeded;

        return $this;
    }

    public function getTitle(): string
    {
        return $this->title;
    }

    public function setTitle(string $title): static
    {
        $this->title = $title;

        return $this;
    }

    public function getMessage(): string
    {
        return $this->message;
    }

    public function setMessage(string $message): static
    {
        $this->message = $message;

        return $this;
    }

    public function getIsResolved(): bool|null
    {
        return $this->isResolved;
    }

    public function setIsResolved(?bool $isResolved): static
    {
        $this->isResolved = $isResolved;

        return $this;
    }

    public function getTargetRadiusKm(): int|null
    {
        return $this->targetRadiusKm;
    }

    public function setTargetRadiusKm(?int $targetRadiusKm): static
    {
        $this->targetRadiusKm = $targetRadiusKm;

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

    public function getResolvedAt(): \DateTimeInterface|null
    {
        return $this->resolvedAt;
    }

    public function setResolvedAt(?\DateTimeInterface $resolvedAt): static
    {
        $this->resolvedAt = $resolvedAt;

        return $this;
    }
}
