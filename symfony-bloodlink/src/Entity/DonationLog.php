<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[
    ORM\Entity(
        repositoryClass: App\Entity\Repository\DonationLogRepository::class,
    ),
]
#[ORM\Table(name: "donation_log")]
class DonationLog
{
    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $logId;

    #[ORM\Column(type: "string")]
    private string $action;

    #[ORM\Column(type: "string", nullable: true)]
    private string|null $previousStatus = null;

    #[ORM\Column(type: "string", nullable: true)]
    private string|null $newStatus = null;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $notes = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    #[ORM\ManyToOne(targetEntity: Donation::class)]
    #[ORM\JoinColumn(name: "donation_id", referencedColumnName: "donation_id")]
    private ?Donation $donation = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: "logged_by", referencedColumnName: "user_id")]
    private ?User $user = null;

    public function getLogId(): string
    {
        return $this->logId;
    }

    public function setLogId(string $logId): static
    {
        $this->logId = $logId;

        return $this;
    }

    public function getAction(): string
    {
        return $this->action;
    }

    public function setAction(string $action): static
    {
        $this->action = $action;

        return $this;
    }

    public function getPreviousStatus(): string|null
    {
        return $this->previousStatus;
    }

    public function setPreviousStatus(?string $previousStatus): static
    {
        $this->previousStatus = $previousStatus;

        return $this;
    }

    public function getNewStatus(): string|null
    {
        return $this->newStatus;
    }

    public function setNewStatus(?string $newStatus): static
    {
        $this->newStatus = $newStatus;

        return $this;
    }

    public function getNotes(): string|null
    {
        return $this->notes;
    }

    public function setNotes(?string $notes): static
    {
        $this->notes = $notes;

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

    public function getDonation(): ?Donation
    {
        return $this->donation;
    }

    public function setDonation(?Donation $donation): static
    {
        $this->donation = $donation;

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
