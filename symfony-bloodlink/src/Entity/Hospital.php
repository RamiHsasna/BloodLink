<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[ORM\Entity(repositoryClass: \App\Repository\HospitalRepository::class)]
#[ORM\Table(name: "hospital")]
class Hospital
{
    #[ORM\Id]
    #[ORM\Column(type: "uuid")]
    private string $hospitalId;

    #[ORM\Column(type: "string", length: 255)]
    private string $name;

    #[ORM\Column(type: "text", nullable: true)]
    private string|null $address = null;

    #[ORM\Column(type: "string", length: 100, nullable: true)]
    private string|null $city = null;

    #[ORM\Column(type: "decimal", precision: 10, scale: 8, nullable: true)]
    private string|null $latitude = null;

    #[ORM\Column(type: "decimal", precision: 11, scale: 8, nullable: true)]
    private string|null $longitude = null;

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    private string|null $phone = null;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private string|null $email = null;

    #[ORM\Column(type: "boolean", nullable: true)]
    private bool|null $isActive = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $createdAt = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    private \DateTimeInterface|null $updatedAt = null;

    public function getHospitalId(): string
    {
        return $this->hospitalId;
    }

    public function setHospitalId(string $hospitalId): static
    {
        $this->hospitalId = $hospitalId;

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

    public function getAddress(): string|null
    {
        return $this->address;
    }

    public function setAddress(?string $address): static
    {
        $this->address = $address;

        return $this;
    }

    public function getCity(): string|null
    {
        return $this->city;
    }

    public function setCity(?string $city): static
    {
        $this->city = $city;

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

    public function getPhone(): string|null
    {
        return $this->phone;
    }

    public function setPhone(?string $phone): static
    {
        $this->phone = $phone;

        return $this;
    }

    public function getEmail(): string|null
    {
        return $this->email;
    }

    public function setEmail(?string $email): static
    {
        $this->email = $email;

        return $this;
    }

    public function getIsActive(): bool|null
    {
        return $this->isActive;
    }

    public function setIsActive(?bool $isActive): static
    {
        $this->isActive = $isActive;

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
}
