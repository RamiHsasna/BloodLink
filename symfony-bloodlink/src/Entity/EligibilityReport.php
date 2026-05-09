<?php

namespace App\Entity;

use App\Repository\EligibilityReportRepository;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity(repositoryClass: EligibilityReportRepository::class)]
#[ORM\Table(name: 'eligibility_report')]
#[ORM\HasLifecycleCallbacks]
class EligibilityReport
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid')]
    #[ORM\GeneratedValue(strategy: 'CUSTOM')]
    #[ORM\CustomIdGenerator(class: 'doctrine.uuid_generator')]
    private ?Uuid $id = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false, onDelete: 'CASCADE')]
    private User $user;

    #[ORM\ManyToOne(targetEntity: DonationEvent::class)]
    #[ORM\JoinColumn(name: 'event_id', referencedColumnName: 'event_id', nullable: false, onDelete: 'CASCADE')]
    private DonationEvent $event;

    #[ORM\Column(type: 'string', length: 30)]
    private string $status = 'PENDING';

    #[ORM\Column(type: 'date', nullable: true)]
    private ?\DateTimeInterface $validUntil = null;

    #[ORM\Column(type: 'json', nullable: true)]
    private ?array $questionnaire = null;

    #[ORM\Column(type: 'json', nullable: true)]
    private ?array $donationHistory = null;

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $doctorNotes = null;

    #[ORM\Column(type: 'datetime', nullable: true)]
    private ?\DateTimeInterface $doctorSignedAt = null;

    #[ORM\Column(type: 'json', nullable: true)]
    private ?array $disqualifyingReasons = null;

    #[ORM\Column(type: 'string', length: 64, unique: true)]
    private string $qrToken;

    #[ORM\Column(type: 'datetime')]
    private \DateTimeInterface $createdAt;

    #[ORM\Column(type: 'datetime')]
    private \DateTimeInterface $updatedAt;

    public function __construct()
    {
        $this->qrToken   = bin2hex(random_bytes(32));
        $this->createdAt = new \DateTime();
        $this->updatedAt = new \DateTime();
    }

    #[ORM\PreUpdate]
    public function onPreUpdate(): void
    {
        $this->updatedAt = new \DateTime();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public function getId(): ?Uuid { return $this->id; }

    public function getUser(): User { return $this->user; }
    public function setUser(User $user): static { $this->user = $user; return $this; }

    public function getEvent(): DonationEvent { return $this->event; }
    public function setEvent(DonationEvent $event): static { $this->event = $event; return $this; }

    public function getStatus(): string { return $this->status; }
    public function setStatus(string $status): static { $this->status = $status; return $this; }

    public function getValidUntil(): ?\DateTimeInterface { return $this->validUntil; }
    public function setValidUntil(?\DateTimeInterface $d): static { $this->validUntil = $d; return $this; }

    public function getQuestionnaire(): ?array { return $this->questionnaire; }
    public function setQuestionnaire(?array $q): static { $this->questionnaire = $q; return $this; }

    public function getDonationHistory(): ?array { return $this->donationHistory; }
    public function setDonationHistory(?array $h): static { $this->donationHistory = $h; return $this; }

    public function getDoctorNotes(): ?string { return $this->doctorNotes; }
    public function setDoctorNotes(?string $n): static { $this->doctorNotes = $n; return $this; }

    public function getDoctorSignedAt(): ?\DateTimeInterface { return $this->doctorSignedAt; }
    public function setDoctorSignedAt(?\DateTimeInterface $d): static { $this->doctorSignedAt = $d; return $this; }

    public function getDisqualifyingReasons(): ?array { return $this->disqualifyingReasons; }
    public function setDisqualifyingReasons(?array $r): static { $this->disqualifyingReasons = $r; return $this; }

    public function getQrToken(): string { return $this->qrToken; }

    public function getCreatedAt(): \DateTimeInterface { return $this->createdAt; }
    public function getUpdatedAt(): \DateTimeInterface { return $this->updatedAt; }

    public function isSignedByDoctor(): bool { return $this->doctorSignedAt !== null; }
}