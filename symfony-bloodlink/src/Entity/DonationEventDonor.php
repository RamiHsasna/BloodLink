<?php

namespace App\Entity;

use App\Repository\DonationEventDonorRepository;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity(repositoryClass: DonationEventDonorRepository::class)]
#[ORM\Table(name: 'donation_event_donor')]
#[ORM\UniqueConstraint(name: 'unique_participation', columns: ['event_id', 'user_id'])]
class DonationEventDonor
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid')]
    #[ORM\GeneratedValue(strategy: 'CUSTOM')]
    #[ORM\CustomIdGenerator(class: 'doctrine.uuid_generator')]
    private ?Uuid $id = null;

    #[ORM\ManyToOne(targetEntity: DonationEvent::class)]
    #[ORM\JoinColumn(name: 'event_id', referencedColumnName: 'event_id', nullable: false, onDelete: 'CASCADE')]
    private DonationEvent $event;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false, onDelete: 'CASCADE')]
    private User $user;

    #[ORM\Column(type: 'datetime', nullable: true, options: ['default' => 'now()'])]
    private ?\DateTimeInterface $createdAt = null;

    public function __construct()
    {
        $this->createdAt = new \DateTime();
    }

    public function getId(): ?Uuid { return $this->id; }

    public function getEvent(): DonationEvent { return $this->event; }
    public function setEvent(DonationEvent $event): static { $this->event = $event; return $this; }

    public function getUser(): User { return $this->user; }
    public function setUser(User $user): static { $this->user = $user; return $this; }

    public function getCreatedAt(): ?\DateTimeInterface { return $this->createdAt; }
}