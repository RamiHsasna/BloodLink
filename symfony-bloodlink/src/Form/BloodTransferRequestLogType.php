<?php

namespace App\Form;

use App\Entity\BloodTransferRequest;
use App\Entity\BloodTransferRequestLog;
use App\Entity\User;
use Doctrine\ORM\EntityRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class BloodTransferRequestLogType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('action', ChoiceType::class, [
                'label' => 'Action',
                'choices' => BloodTransferRequestLog::actionChoices(),
                'placeholder' => 'Choose the transfer-log action',
            ])
            ->add('previousStatus', TextType::class, [
                'label' => 'Previous Status',
                'required' => false,
                'disabled' => true,
                'attr' => [
                    'placeholder' => 'Auto-filled from the selected transfer request',
                ],
            ])
            ->add('newStatus', ChoiceType::class, [
                'label' => 'New Status',
                'choices' => BloodTransferRequestLog::statusChoices(),
                'required' => true,
                'placeholder' => 'Choose the next transfer status',
                'disabled' => $options['lock_snapshot'],
            ])
            ->add('notes', TextareaType::class, [
                'label' => 'Notes',
                'required' => false,
                'attr' => [
                    'rows' => 4,
                    'placeholder' => 'Track the important operational details for this transfer update.',
                ],
            ])
            ->add('bloodTransferRequest', EntityType::class, [
                'class' => BloodTransferRequest::class,
                'label' => 'Transfer Request',
                'query_builder' => static function (EntityRepository $repository) use ($options) {
                    $qb = $repository->createQueryBuilder('transfer')
                        ->leftJoin('transfer.requestingHospital', 'requestingHospital')->addSelect('requestingHospital')
                        ->leftJoin('transfer.approvingHospital', 'approvingHospital')->addSelect('approvingHospital')
                        ->orderBy('transfer.requestedAt', 'DESC')
                        ->addOrderBy('transfer.transferId', 'DESC');

                    if ($options['allowed_hospital_id'] !== null) {
                        $qb->andWhere('(requestingHospital.hospitalId = :hospitalId OR approvingHospital.hospitalId = :hospitalId)')
                            ->setParameter('hospitalId', $options['allowed_hospital_id']);
                    }

                    return $qb;
                },
                'choice_label' => static function (BloodTransferRequest $transfer): string {
                    $requestingHospital = $transfer->getRequestingHospital();
                    $approvingHospital = $transfer->getApprovingHospital();

                    return sprintf(
                        '#%d • %s → %s • %s',
                        $transfer->getTransferId(),
                        $requestingHospital?->getName() ?? 'Unknown',
                        $approvingHospital?->getName() ?? 'Pending',
                        $transfer->getStatus() ?? 'N/A',
                    );
                },
                'placeholder' => 'Choose the transfer request',
                'disabled' => $options['lock_snapshot'],
            ])
            ->add('user', EntityType::class, [
                'class' => User::class,
                'label' => 'Changed By',
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
            'data_class' => BloodTransferRequestLog::class,
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
