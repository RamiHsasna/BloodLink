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

    #[ORM\Column(type: "boolean", options: ["default" => false])]
    private bool $anomalyDetected = false;

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    private ?string $anomalySeverity = null;

    #[ORM\Column(type: "integer", nullable: true)]
    private ?int $anomalyScore = null;

    /**
     * @var array<int, string>|null
     */
    #[ORM\Column(type: "json", nullable: true)]
    private ?array $anomalyReasons = null;

    #[ORM\Column(type: "text", nullable: true)]
    private ?string $anomalyExplanation = null;

    #[ORM\Column(type: "text", nullable: true)]
    private ?string $anomalyRecommendedAction = null;

    #[ORM\Column(type: "string", length: 40, nullable: true)]
    private ?string $anomalyAnalysisProvider = null;

    /**
     * @var array<string, mixed>|null
     */
    #[ORM\Column(type: "json", nullable: true)]
    private ?array $anomalyRawResponse = null;

    #[ORM\Column(type: "boolean", options: ["default" => false])]
    private bool $anomalyReviewed = false;

    #[ORM\Column(type: "datetime_immutable", nullable: true)]
    private ?\DateTimeImmutable $anomalyReviewedAt = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: "anomaly_reviewed_by", referencedColumnName: "user_id", nullable: true)]
    private ?User $anomalyReviewedBy = null;

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    private ?string $smsAlertStatus = null;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private ?string $smsAlertRecipient = null;

    #[ORM\Column(type: "text", nullable: true)]
    private ?string $smsAlertError = null;

    #[ORM\Column(type: "datetime_immutable", nullable: true)]
    private ?\DateTimeImmutable $smsAlertSentAt = null;

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

    public function isAnomalyDetected(): bool
    {
        return $this->anomalyDetected;
    }

    public function setAnomalyDetected(bool $anomalyDetected): static
    {
        $this->anomalyDetected = $anomalyDetected;

        return $this;
    }

    public function getAnomalySeverity(): ?string
    {
        return $this->anomalySeverity;
    }

    public function setAnomalySeverity(?string $anomalySeverity): static
    {
        $this->anomalySeverity = $anomalySeverity;

        return $this;
    }

    public function getAnomalyScore(): ?int
    {
        return $this->anomalyScore;
    }

    public function setAnomalyScore(?int $anomalyScore): static
    {
        $this->anomalyScore = $anomalyScore;

        return $this;
    }

    /**
     * @return array<int, string>
     */
    public function getAnomalyReasons(): array
    {
        return $this->anomalyReasons ?? [];
    }

    /**
     * @param array<int, string>|null $anomalyReasons
     */
    public function setAnomalyReasons(?array $anomalyReasons): static
    {
        $this->anomalyReasons = $anomalyReasons;

        return $this;
    }

    public function getAnomalyExplanation(): ?string
    {
        return $this->anomalyExplanation;
    }

    public function setAnomalyExplanation(?string $anomalyExplanation): static
    {
        $this->anomalyExplanation = $anomalyExplanation;

        return $this;
    }

    public function getAnomalyRecommendedAction(): ?string
    {
        return $this->anomalyRecommendedAction;
    }

    public function setAnomalyRecommendedAction(?string $anomalyRecommendedAction): static
    {
        $this->anomalyRecommendedAction = $anomalyRecommendedAction;

        return $this;
    }

    public function getAnomalyAnalysisProvider(): ?string
    {
        return $this->anomalyAnalysisProvider;
    }

    public function setAnomalyAnalysisProvider(?string $anomalyAnalysisProvider): static
    {
        $this->anomalyAnalysisProvider = $anomalyAnalysisProvider;

        return $this;
    }

    /**
     * @return array<string, mixed>
     */
    public function getAnomalyRawResponse(): array
    {
        return $this->anomalyRawResponse ?? [];
    }

    /**
     * @param array<string, mixed>|null $anomalyRawResponse
     */
    public function setAnomalyRawResponse(?array $anomalyRawResponse): static
    {
        $this->anomalyRawResponse = $anomalyRawResponse;

        return $this;
    }

    public function isAnomalyReviewed(): bool
    {
        return $this->anomalyReviewed;
    }

    public function setAnomalyReviewed(bool $anomalyReviewed): static
    {
        $this->anomalyReviewed = $anomalyReviewed;

        return $this;
    }

    public function getAnomalyReviewedAt(): ?\DateTimeImmutable
    {
        return $this->anomalyReviewedAt;
    }

    public function setAnomalyReviewedAt(?\DateTimeImmutable $anomalyReviewedAt): static
    {
        $this->anomalyReviewedAt = $anomalyReviewedAt;

        return $this;
    }

    public function getAnomalyReviewedBy(): ?User
    {
        return $this->anomalyReviewedBy;
    }

    public function setAnomalyReviewedBy(?User $anomalyReviewedBy): static
    {
        $this->anomalyReviewedBy = $anomalyReviewedBy;

        return $this;
    }

    public function getSmsAlertStatus(): ?string
    {
        return $this->smsAlertStatus;
    }

    public function setSmsAlertStatus(?string $smsAlertStatus): static
    {
        $this->smsAlertStatus = $smsAlertStatus;

        return $this;
    }

    public function getSmsAlertRecipient(): ?string
    {
        return $this->smsAlertRecipient;
    }

    public function setSmsAlertRecipient(?string $smsAlertRecipient): static
    {
        $this->smsAlertRecipient = $smsAlertRecipient;

        return $this;
    }

    public function getSmsAlertError(): ?string
    {
        return $this->smsAlertError;
    }

    public function setSmsAlertError(?string $smsAlertError): static
    {
        $this->smsAlertError = $smsAlertError;

        return $this;
    }

    public function getSmsAlertSentAt(): ?\DateTimeImmutable
    {
        return $this->smsAlertSentAt;
    }

    public function setSmsAlertSentAt(?\DateTimeImmutable $smsAlertSentAt): static
    {
        $this->smsAlertSentAt = $smsAlertSentAt;

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
