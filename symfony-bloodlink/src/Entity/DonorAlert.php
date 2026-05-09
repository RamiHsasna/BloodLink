<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Context\ExecutionContextInterface;

#[ORM\Entity(repositoryClass: \App\Repository\DonorAlertRepository::class)]
#[ORM\Table(name: "donor_alerts")]
class DonorAlert
{
    public const RESPONSE_INTERESTED = 'INTERESTED';
    public const RESPONSE_NOT_INTERESTED = 'NOT_INTERESTED';
    public const RESPONSE_ALREADY_DONATED = 'ALREADY_DONATED';
    public const RESPONSE_NO_RESPONSE = 'NO_RESPONSE';

    public const DONOR_RESPONSES = [
        self::RESPONSE_INTERESTED,
        self::RESPONSE_NOT_INTERESTED,
        self::RESPONSE_ALREADY_DONATED,
        self::RESPONSE_NO_RESPONSE,
    ];

    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $donorAlertId;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isNotified = null;

    #[ORM\Column(type: "datetime_immutable", nullable: true)]
    private ?\DateTimeImmutable $notificationSentAt = null;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isRead = null;

    #[ORM\Column(type: "datetime_immutable", nullable: true)]
    private ?\DateTimeImmutable $readAt = null;

    #[ORM\Column(type: "string", length: 50, nullable: true)]
    #[Assert\Choice(choices: self::DONOR_RESPONSES, message: 'Choose a valid donor response.')]
    private string|null $donorResponse = null;

    #[ORM\ManyToOne(targetEntity: Alert::class)]
    #[ORM\JoinColumn(name: "alert_id", referencedColumnName: "alert_id")]
    #[Assert\NotNull(message: 'Select the related alert.')]
    private ?Alert $alert = null;

    #[ORM\ManyToOne(targetEntity: Donor::class)]
    #[ORM\JoinColumn(name: "donor_id", referencedColumnName: "user_id")]
    #[Assert\NotNull(message: 'Select the donor who received the alert.')]
    private ?Donor $donor = null;

    public function getDonorAlertId(): string
    {
        return $this->donorAlertId;
    }

    public function setDonorAlertId(string $donorAlertId): static
    {
        $this->donorAlertId = $donorAlertId;

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

    public function getNotificationSentAt(): ?\DateTimeImmutable
    {
        return $this->notificationSentAt;
    }

    public function setNotificationSentAt(?\DateTimeImmutable $notificationSentAt): static
    {
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

    public function getReadAt(): ?\DateTimeImmutable
    {
        return $this->readAt;
    }

    public function setReadAt(?\DateTimeImmutable $readAt): static
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

    public function getAlert(): ?Alert
    {
        return $this->alert;
    }

    public function setAlert(?Alert $alert): static
    {
        $this->alert = $alert;

        return $this;
    }

    public function getDonor(): ?Donor
    {
        return $this->donor;
    }

    public function setDonor(?Donor $donor): static
    {
        $this->donor = $donor;

        return $this;
    }

    /**
     * @return array<string, string>
     */
    public static function donorResponseChoices(): array
    {
        return [
            'Interested' => self::RESPONSE_INTERESTED,
            'Not Interested' => self::RESPONSE_NOT_INTERESTED,
            'Already Donated' => self::RESPONSE_ALREADY_DONATED,
            'No Response' => self::RESPONSE_NO_RESPONSE,
        ];
    }

    #[Assert\Callback]
    public function validateLifecycle(ExecutionContextInterface $context): void
    {
        if ($this->isNotified === true && $this->notificationSentAt === null) {
            $context->buildViolation('Notification date is required when the alert has been sent.')
                ->atPath('notificationSentAt')
                ->addViolation();
        }

        if ($this->isNotified !== true && $this->notificationSentAt !== null) {
            $context->buildViolation('Clear the notification date when the donor has not been notified yet.')
                ->atPath('notificationSentAt')
                ->addViolation();
        }

        if ($this->isRead === true && $this->readAt === null) {
            $context->buildViolation('Read date is required when the donor alert is marked as read.')
                ->atPath('readAt')
                ->addViolation();
        }

        if ($this->isRead !== true && $this->readAt !== null) {
            $context->buildViolation('Clear the read date when the donor alert is still unread.')
                ->atPath('readAt')
                ->addViolation();
        }

        if (
            $this->donorResponse !== null
            && $this->donorResponse !== self::RESPONSE_NO_RESPONSE
            && $this->isRead !== true
        ) {
            $context->buildViolation('A donor response should only be recorded after the alert has been marked as read.')
                ->atPath('donorResponse')
                ->addViolation();
        }
    }
}
