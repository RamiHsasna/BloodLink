<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Context\ExecutionContextInterface;

#[ORM\Entity(repositoryClass: \App\Repository\BloodTransferRequestLogRepository::class)]
#[ORM\Table(name: "blood_transfer_request_log")]
class BloodTransferRequestLog
{
    public const ACTION_REQUESTED = 'REQUESTED';
    public const ACTION_APPROVED = 'APPROVED';
    public const ACTION_CONFIRMED = 'CONFIRMED';
    public const ACTION_DISPATCHED = 'DISPATCHED';

    public const STATUS_PENDING = 'PENDING';
    public const STATUS_APPROVED = 'APPROVED';
    public const STATUS_IN_TRANSIT = 'IN_TRANSIT';
    public const STATUS_DELIVERED = 'DELIVERED';

    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $logId;

    #[ORM\Column(type: "string")]
    #[Assert\NotBlank(message: 'Action is required.')]
    #[Assert\Length(max: 120)]
    #[Assert\Choice(callback: [self::class, 'allowedActions'], message: 'Choose a valid transfer-log action.')]
    private string $action;

    #[ORM\Column(type: "string", nullable: true)]
    #[Assert\Length(max: 120)]
    #[Assert\Choice(callback: [self::class, 'allowedStatuses'], message: 'Choose a valid transfer status.')]
    private string|null $previousStatus = null;

    #[ORM\Column(type: "string", nullable: true)]
    #[Assert\Length(max: 120)]
    #[Assert\Choice(callback: [self::class, 'allowedStatuses'], message: 'Choose a valid transfer status.')]
    private string|null $newStatus = null;

    #[ORM\Column(type: "text", nullable: true)]
    #[Assert\Length(max: 1200)]
    private string|null $notes = null;

    #[ORM\Column(type: "datetime_immutable", nullable: true)]
    private ?\DateTimeImmutable $createdAt = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: "changed_by", referencedColumnName: "user_id")]
    #[Assert\NotNull(message: 'Select the staff member who changed the transfer request.')]
    private ?User $user = null;

    #[ORM\ManyToOne(targetEntity: BloodTransferRequest::class)]
    #[ORM\JoinColumn(name: "transfer_id", referencedColumnName: "transfer_id")]
    #[Assert\NotNull(message: 'Select a transfer request.')]
    private ?BloodTransferRequest $bloodTransferRequest = null;

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

    public function getCreatedAt(): ?\DateTimeImmutable
    {
        return $this->createdAt;
    }

    public function setCreatedAt(?\DateTimeImmutable $createdAt): static
    {
        $this->createdAt = $createdAt;

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

    public function getBloodTransferRequest(): ?BloodTransferRequest
    {
        return $this->bloodTransferRequest;
    }

    public function setBloodTransferRequest(?BloodTransferRequest $bloodTransferRequest): static
    {
        $this->bloodTransferRequest = $bloodTransferRequest;

        return $this;
    }

    public static function actionChoices(): array
    {
        return [
            'Requested' => self::ACTION_REQUESTED,
            'Approved' => self::ACTION_APPROVED,
            'Confirmed' => self::ACTION_CONFIRMED,
            'Dispatched' => self::ACTION_DISPATCHED,
        ];
    }

    public static function statusChoices(): array
    {
        return [
            'Pending' => self::STATUS_PENDING,
            'Approved' => self::STATUS_APPROVED,
            'In Transit' => self::STATUS_IN_TRANSIT,
            'Delivered' => self::STATUS_DELIVERED,
        ];
    }

    public static function allowedActions(): array
    {
        return array_values(self::actionChoices());
    }

    public static function allowedStatuses(): array
    {
        return array_values(self::statusChoices());
    }

    #[Assert\Callback]
    public function validateStatusTransition(ExecutionContextInterface $context): void
    {
        if (
            $this->previousStatus !== null
            && $this->newStatus !== null
            && strcasecmp($this->previousStatus, $this->newStatus) === 0
        ) {
            $context->buildViolation('Previous status and new status should differ when both are filled.')
                ->atPath('newStatus')
                ->addViolation();
        }
    }
}
