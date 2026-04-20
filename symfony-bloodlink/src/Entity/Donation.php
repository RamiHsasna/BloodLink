<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[ORM\Entity(repositoryClass: \App\Repository\DonationRepository::class)]
#[ORM\Table(name: "donations")]
class Donation
{
    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $donationId;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $donationDate;

    #[ORM\Column(type: "integer", nullable: true)]
    private int|null $unitsCollected = null;

    #[ORM\Column(type: "decimal", precision: 5, scale: 2, nullable: true)]
    private string|null $volumeCollected = null;

    #[ORM\Column(type: "string", length: 20)]
    private string $status;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $screeningPassed = null;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $medicalNotes = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    #[ORM\ManyToOne(targetEntity: BloodType::class)]
    #[
        ORM\JoinColumn(
            name: "blood_type_id",
            referencedColumnName: "blood_type_id",
        ),
    ]
    private ?BloodType $bloodType = null;

    #[ORM\ManyToOne(targetEntity: DonationEvent::class)]
    #[
        ORM\JoinColumn(
            name: "donation_event_id",
            referencedColumnName: "event_id",
        ),
    ]
    private ?DonationEvent $donationEvent = null;

    #[ORM\ManyToOne(targetEntity: Hospital::class)]
    #[ORM\JoinColumn(name: "hospital_id", referencedColumnName: "hospital_id")]
    private ?Hospital $hospital = null;

    #[ORM\ManyToOne(targetEntity: Donor::class)]
    #[ORM\JoinColumn(name: "user_id", referencedColumnName: "user_id")]
    private ?Donor $donor = null;

    public function getDonationId(): string
    {
        return $this->donationId;
    }

    public function setDonationId(string $donationId): static
    {
        $this->donationId = $donationId;

        return $this;
    }

    public function getDonationDate(): \DateTimeInterface
    {
        return $this->donationDate;
    }

    public function setDonationDate(\DateTimeInterface $donationDate): static
    {
        $this->donationDate = $donationDate;

        return $this;
    }

    public function getUnitsCollected(): int|null
    {
        return $this->unitsCollected;
    }

    public function setUnitsCollected(?int $unitsCollected): static
    {
        $this->unitsCollected = $unitsCollected;

        return $this;
    }

    public function getVolumeCollected(): string|null
    {
        return $this->volumeCollected;
    }

    public function setVolumeCollected(?string $volumeCollected): static
    {
        $this->volumeCollected = $volumeCollected;

        return $this;
    }

    public function getStatus(): string
    {
        return $this->status;
    }

    public function setStatus(string $status): static
    {
        $this->status = $status;

        return $this;
    }

    public function getScreeningPassed(): bool|null
    {
        return $this->screeningPassed;
    }

    public function setScreeningPassed(?bool $screeningPassed): static
    {
        $this->screeningPassed = $screeningPassed;

        return $this;
    }

    public function getMedicalNotes(): string|null
    {
        return $this->medicalNotes;
    }

    public function setMedicalNotes(?string $medicalNotes): static
    {
        $this->medicalNotes = $medicalNotes;

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

    public function getBloodType(): ?BloodType
    {
        return $this->bloodType;
    }

    public function setBloodType(?BloodType $bloodType): static
    {
        $this->bloodType = $bloodType;

        return $this;
    }

    public function getDonationEvent(): ?DonationEvent
    {
        return $this->donationEvent;
    }

    public function setDonationEvent(?DonationEvent $donationEvent): static
    {
        $this->donationEvent = $donationEvent;

        return $this;
    }

    public function getHospital(): ?Hospital
    {
        return $this->hospital;
    }

    public function setHospital(?Hospital $hospital): static
    {
        $this->hospital = $hospital;

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
}
