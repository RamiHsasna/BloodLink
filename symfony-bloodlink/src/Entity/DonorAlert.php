<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[
    ORM\Entity(
        repositoryClass: App\Entity\Repository\DonorAlertRepository::class,
    ),
]
#[ORM\Table(name: "donor_alerts")]
class DonorAlert
{
    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $donorAlertId;

    #[ORM\Column(type: "string")]
    private string $alertId;

    #[ORM\Column(type: "string")]
    private string $donorId;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isNotified = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $notificationSentAt = null;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isRead = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $readAt = null;

    #[ORM\Column(type: "string", length: 50, nullable: true)]
    private string|null $donorResponse = null;

    public function getDonorAlertId(): string
    {
        return $this->donorAlertId;
    }

    public function setDonorAlertId(string $donorAlertId): static
    {
        $this->donorAlertId = $donorAlertId;

        return $this;
    }

    public function getAlertId(): string
    {
        return $this->alertId;
    }

    public function setAlertId(string $alertId): static
    {
        $this->alertId = $alertId;

        return $this;
    }

    public function getDonorId(): string
    {
        return $this->donorId;
    }

    public function setDonorId(string $donorId): static
    {
        $this->donorId = $donorId;

        return $this;
    }

    public function getIsNotified(): bool|null
    {
        return $this->isNotified;
    }

    public function setIsNotified(?bool $isNotified): static
    {
        $this->isNotified = $isNotified;

        return $this;
    }

    public function getNotificationSentAt(): \DateTimeInterface|null
    {
        return $this->notificationSentAt;
    }

    public function setNotificationSentAt(
        ?\DateTimeInterface $notificationSentAt,
    ): static {
        $this->notificationSentAt = $notificationSentAt;

        return $this;
    }

    public function getIsRead(): bool|null
    {
        return $this->isRead;
    }

    public function setIsRead(?bool $isRead): static
    {
        $this->isRead = $isRead;

        return $this;
    }

    public function getReadAt(): \DateTimeInterface|null
    {
        return $this->readAt;
    }

    public function setReadAt(?\DateTimeInterface $readAt): static
    {
        $this->readAt = $readAt;

        return $this;
    }

    public function getDonorResponse(): string|null
    {
        return $this->donorResponse;
    }

    public function setDonorResponse(?string $donorResponse): static
    {
        $this->donorResponse = $donorResponse;

        return $this;
    }
}
