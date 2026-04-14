<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[
    ORM\Entity(
        repositoryClass: App\Entity\Repository\DonationEventRepository::class,
    ),
]
#[ORM\Table(name: "donation_events")]
class DonationEvent
{
    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $eventId;

    #[ORM\Column(type: "string", length: 255)]
    private string $name;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $description = null;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $startDate;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $endDate;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private string|null $location = null;

    #[ORM\Column(type: "decimal", precision: 10, scale: 8, nullable: true)]
    private string|null $latitude = null;

    #[ORM\Column(type: "decimal", precision: 11, scale: 8, nullable: true)]
    private string|null $longitude = null;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private string|null $targetBloodTypes = null;

    #[ORM\Column(type: "integer", nullable: true)]
    private int|null $targetCollectionUnits = null;

    #[ORM\Column(type: "integer", nullable: true)]
    private int|null $actualCollectionUnits = null;

    #[ORM\Column(type: "string", length: 20)]
    private string $status;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $updatedAt = null;

    #[ORM\ManyToOne(targetEntity: Hospital::class)]
    #[ORM\JoinColumn(name: "hospital_id", referencedColumnName: "hospital_id")]
    private ?Hospital $hospital = null;

    public function getEventId(): string
    {
        return $this->eventId;
    }

    public function setEventId(string $eventId): static
    {
        $this->eventId = $eventId;

        return $this;
    }

    public function getName(): string
    {
        return $this->name;
    }

    public function setName(string $name): static
    {
        $this->name = $name;

        return $this;
    }

    public function getDescription(): string|null
    {
        return $this->description;
    }

    public function setDescription(?string $description): static
    {
        $this->description = $description;

        return $this;
    }

    public function getStartDate(): \DateTimeInterface
    {
        return $this->startDate;
    }

    public function setStartDate(\DateTimeInterface $startDate): static
    {
        $this->startDate = $startDate;

        return $this;
    }

    public function getEndDate(): \DateTimeInterface
    {
        return $this->endDate;
    }

    public function setEndDate(\DateTimeInterface $endDate): static
    {
        $this->endDate = $endDate;

        return $this;
    }

    public function getLocation(): string|null
    {
        return $this->location;
    }

    public function setLocation(?string $location): static
    {
        $this->location = $location;

        return $this;
    }

    public function getLatitude(): string|null
    {
        return $this->latitude;
    }

    public function setLatitude(?string $latitude): static
    {
        $this->latitude = $latitude;

        return $this;
    }

    public function getLongitude(): string|null
    {
        return $this->longitude;
    }

    public function setLongitude(?string $longitude): static
    {
        $this->longitude = $longitude;

        return $this;
    }

    public function getTargetBloodTypes(): string|null
    {
        return $this->targetBloodTypes;
    }

    public function setTargetBloodTypes(?string $targetBloodTypes): static
    {
        $this->targetBloodTypes = $targetBloodTypes;

        return $this;
    }

    public function getTargetCollectionUnits(): int|null
    {
        return $this->targetCollectionUnits;
    }

    public function setTargetCollectionUnits(
        ?int $targetCollectionUnits,
    ): static {
        $this->targetCollectionUnits = $targetCollectionUnits;

        return $this;
    }

    public function getActualCollectionUnits(): int|null
    {
        return $this->actualCollectionUnits;
    }

    public function setActualCollectionUnits(
        ?int $actualCollectionUnits,
    ): static {
        $this->actualCollectionUnits = $actualCollectionUnits;

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

    public function getHospital(): ?Hospital
    {
        return $this->hospital;
    }

    public function setHospital(?Hospital $hospital): static
    {
        $this->hospital = $hospital;

        return $this;
    }
}
