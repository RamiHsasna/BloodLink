<?php

namespace App\Form;

use App\Entity\Donation;
use App\Entity\DonationLog;
use App\Entity\User;
use Doctrine\ORM\EntityRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class DonationLogType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('action', ChoiceType::class, [
                'label' => 'Action',
                'choices' => DonationLog::actionChoices(),
                'placeholder' => 'Choose the donation-log action',
            ])
            ->add('previousStatus', TextType::class, [
                'label' => 'Previous Status',
                'required' => false,
                'disabled' => true,
                'attr' => [
                    'placeholder' => 'Auto-filled from the selected donation',
                ],
            ])
            ->add('newStatus', ChoiceType::class, [
                'label' => 'New Status',
                'choices' => DonationLog::statusChoices(),
                'required' => true,
                'placeholder' => 'Choose the next donation status',
                'disabled' => $options['lock_snapshot'],
            ])
            ->add('notes', TextareaType::class, [
                'label' => 'Notes',
                'required' => false,
                'attr' => [
                    'rows' => 4,
                    'placeholder' => 'Add the operational context for this log entry.',
                ],
            ])
            ->add('donation', EntityType::class, [
                'class' => Donation::class,
                'query_builder' => static function (EntityRepository $repository) use ($options) {
                    $qb = $repository->createQueryBuilder('donation')
                        ->leftJoin('donation.donor', 'donor')->addSelect('donor')
                        ->leftJoin('donation.hospital', 'hospital')->addSelect('hospital')
                        ->orderBy('donation.donationDate', 'DESC')
                        ->addOrderBy('donation.donationId', 'DESC');

                    if ($options['allowed_hospital_id'] !== null) {
                        $qb->andWhere('hospital.hospitalId = :hospitalId')
                            ->setParameter('hospitalId', $options['allowed_hospital_id']);
                    }

                    return $qb;
                },
                'choice_label' => static function (Donation $donation): string {
                    $donor = $donation->getDonor();
                    $donorLabel = $donor !== null
                        ? trim($donor->getFirstName() . ' ' . $donor->getLastName())
                        : 'Unknown donor';

                    return sprintf(
                        '%s • %s • %s',
                        substr($donation->getDonationId(), 0, 8),
                        $donation->getStatus(),
                        $donorLabel,
                    );
                },
                'placeholder' => 'Choose the donation record',
                'disabled' => $options['lock_snapshot'],
            ])
            ->add('user', EntityType::class, [
                'class' => User::class,
                'label' => 'Logged By',
                'query_builder' => static function (EntityRepository $repository) use ($options) {
                    $qb = $repository->createQueryBuilder('user')
                        ->andWhere('user.userType IN (:userTypes)')
                        ->setParameter('userTypes', User::backOfficeTypes())
                        ->orderBy('user.firstName', 'ASC')
                        ->addOrderBy('user.lastName', 'ASC');

                    if ($options['actor_user_id'] !== null) {
                        $qb->andWhere('user.userId = :actorUserId')
                            ->setParameter('actorUserId', $options['actor_user_id']);
                    }

                    return $qb;
                },
                'choice_label' => static function (User $user): string {
                    return sprintf(
                        '%s %s • %s',
                        $user->getFirstName(),
                        $user->getLastName(),
                        $user->getUserType(),
                    );
                },
                'placeholder' => 'Choose the acting user',
                'disabled' => $options['lock_snapshot'] || $options['lock_actor'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => DonationLog::class,
            'lock_snapshot' => false,
            'lock_actor' => false,
            'allowed_hospital_id' => null,
            'actor_user_id' => null,
        ]);

        $resolver->setAllowedTypes('lock_snapshot', 'bool');
        $resolver->setAllowedTypes('lock_actor', 'bool');
        $resolver->setAllowedTypes('allowed_hospital_id', ['null', 'string']);
        $resolver->setAllowedTypes('actor_user_id', ['null', 'string']);
    }
}
