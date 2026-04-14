<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[
    ORM\Entity(
        repositoryClass: App\Entity\Repository\BloodTransferRequestRepository::class,
    ),
]
#[ORM\Table(name: "blood_transfer_request")]
class BloodTransferRequest
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $transferId = null;

    #[ORM\Column(type: "string", length: 50)]
    private string $requestingStaffId;

    #[ORM\Column(type: "string", length: 50, nullable: true)]
    private string|null $approvingStaffId = null;

    #[ORM\Column(type: "integer")]
    private int $quantityUnitsRequested;

    #[ORM\Column(type: "integer", nullable: true)]
    private int|null $quantityUnitsApproved = null;

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    private string|null $status = null;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $reason = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $requestedAt = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $approvedAt = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $deliveryExpectedAt = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $actualDeliveryAt = null;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $notes = null;

    #[ORM\ManyToOne(targetEntity: Hospital::class)]
    #[
        ORM\JoinColumn(
            name: "requesting_hospital_id",
            referencedColumnName: "hospital_id",
        ),
    ]
    private ?Hospital $requestingHospital = null;

    #[ORM\ManyToOne(targetEntity: Hospital::class)]
    #[
        ORM\JoinColumn(
            name: "approving_hospital_id",
            referencedColumnName: "hospital_id",
        ),
    ]
    private ?Hospital $approvingHospital = null;

    #[ORM\ManyToOne(targetEntity: BloodType::class)]
    #[
        ORM\JoinColumn(
            name: "blood_type_id",
            referencedColumnName: "blood_type_id",
        ),
    ]
    private ?BloodType $bloodType = null;

    #[ORM\ManyToOne(targetEntity: HospitalStaff::class)]
    #[
        ORM\JoinColumn(
            name: "requesting_staff_id",
            referencedColumnName: "user_id",
        ),
    ]
    private ?HospitalStaff $requestingStaff = null;

    #[ORM\ManyToOne(targetEntity: HospitalStaff::class)]
    #[
        ORM\JoinColumn(
            name: "approving_staff_id",
            referencedColumnName: "user_id",
        ),
    ]
    private ?HospitalStaff $approvingStaff = null;

    public function getTransferId(): int
    {
        return $this->transferId;
    }

    public function getRequestingStaffId(): string
    {
        return $this->requestingStaffId;
    }

    public function setRequestingStaffId(string $requestingStaffId): static
    {
        $this->requestingStaffId = $requestingStaffId;

        return $this;
    }

    public function getApprovingStaffId(): string|null
    {
        return $this->approvingStaffId;
    }

    public function setApprovingStaffId(?string $approvingStaffId): static
    {
        $this->approvingStaffId = $approvingStaffId;

        return $this;
    }

    public function getQuantityUnitsRequested(): int
    {
        return $this->quantityUnitsRequested;
    }

    public function setQuantityUnitsRequested(
        int $quantityUnitsRequested,
    ): static {
        $this->quantityUnitsRequested = $quantityUnitsRequested;

        return $this;
    }

    public function getQuantityUnitsApproved(): int|null
    {
        return $this->quantityUnitsApproved;
    }

    public function setQuantityUnitsApproved(
        ?int $quantityUnitsApproved,
    ): static {
        $this->quantityUnitsApproved = $quantityUnitsApproved;

        return $this;
    }

    public function getStatus(): string|null
    {
        return $this->status;
    }

    public function setStatus(?string $status): static
    {
        $this->status = $status;

        return $this;
    }

    public function getReason(): string|null
    {
        return $this->reason;
    }

    public function setReason(?string $reason): static
    {
        $this->reason = $reason;

        return $this;
    }

    public function getRequestedAt(): \DateTimeInterface|null
    {
        return $this->requestedAt;
    }

    public function setRequestedAt(?\DateTimeInterface $requestedAt): static
    {
        $this->requestedAt = $requestedAt;

        return $this;
    }

    public function getApprovedAt(): \DateTimeInterface|null
    {
        return $this->approvedAt;
    }

    public function setApprovedAt(?\DateTimeInterface $approvedAt): static
    {
        $this->approvedAt = $approvedAt;

        return $this;
    }

    public function getDeliveryExpectedAt(): \DateTimeInterface|null
    {
        return $this->deliveryExpectedAt;
    }

    public function setDeliveryExpectedAt(
        ?\DateTimeInterface $deliveryExpectedAt,
    ): static {
        $this->deliveryExpectedAt = $deliveryExpectedAt;

        return $this;
    }

    public function getActualDeliveryAt(): \DateTimeInterface|null
    {
        return $this->actualDeliveryAt;
    }

    public function setActualDeliveryAt(
        ?\DateTimeInterface $actualDeliveryAt,
    ): static {
        $this->actualDeliveryAt = $actualDeliveryAt;

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

    public function getRequestingHospital(): ?Hospital
    {
        return $this->requestingHospital;
    }

    public function setRequestingHospital(?Hospital $requestingHospital): static
    {
        $this->requestingHospital = $requestingHospital;

        return $this;
    }

    public function getApprovingHospital(): ?Hospital
    {
        return $this->approvingHospital;
    }

    public function setApprovingHospital(?Hospital $approvingHospital): static
    {
        $this->approvingHospital = $approvingHospital;

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

    public function getRequestingStaff(): ?HospitalStaff
    {
        return $this->requestingStaff;
    }

    public function setRequestingStaff(?HospitalStaff $requestingStaff): static
    {
        $this->requestingStaff = $requestingStaff;

        return $this;
    }

    public function getApprovingStaff(): ?HospitalStaff
    {
        return $this->approvingStaff;
    }

    public function setApprovingStaff(?HospitalStaff $approvingStaff): static
    {
        $this->approvingStaff = $approvingStaff;

        return $this;
    }
}
