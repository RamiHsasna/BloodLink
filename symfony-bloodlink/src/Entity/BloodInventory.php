<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[ORM\Entity(repositoryClass: App\Entity\Repository\BloodInventoryRepository::class)]
#[ORM\Table(name: 'blood_inventory')]
class BloodInventory
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $inventoryId = null;

    #[ORM\Column(type: 'integer')]
    private int $quantityUnits;

    #[ORM\Column(type: 'string', length: 20, nullable: true)]
    private string|null $status = null;

    #[ORM\Column(type: 'datetime', nullable: true)]
    private \DateTimeInterface|null $updatedAt = null;

    #[ORM\ManyToOne(targetEntity: BloodType::class)]
    #[ORM\JoinColumn(name: 'blood_type_id', referencedColumnName: 'blood_type_id')]
    private ?BloodType $bloodType = null;

    #[ORM\ManyToOne(targetEntity: Hospital::class)]
    #[ORM\JoinColumn(name: 'hospital_id', referencedColumnName: 'hospital_id')]
    private ?Hospital $hospital = null;

    public function getInventoryId(): int
    {
        return $this->inventoryId;
    }


    public function getQuantityUnits(): int
    {
        return $this->quantityUnits;
    }


    public function setQuantityUnits(int $quantityUnits): static
    {
        $this->quantityUnits = $quantityUnits;

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


    public function getUpdatedAt(): \DateTimeInterface|null
    {
        return $this->updatedAt;
    }


    public function setUpdatedAt(?\DateTimeInterface $updatedAt): static
    {
        $this->updatedAt = $updatedAt;

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
