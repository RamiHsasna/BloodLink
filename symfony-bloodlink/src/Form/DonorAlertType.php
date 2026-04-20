<?php

namespace App\Form;

use App\Entity\Alert;
use App\Entity\Donor;
use App\Entity\DonorAlert;
use Doctrine\ORM\EntityRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class DonorAlertType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('alert', EntityType::class, [
                'class' => Alert::class,
                'label' => 'Alert',
                'query_builder' => static function (EntityRepository $repository) use ($options) {
                    $qb = $repository->createQueryBuilder('alert')
                        ->orderBy('alert.createdAt', 'DESC')
                        ->addOrderBy('alert.title', 'ASC');

                    if ($options['allowed_hospital_id'] !== null) {
                        $qb->andWhere('alert.hospitalId = :hospitalId')
                            ->setParameter('hospitalId', $options['allowed_hospital_id']);
                    }

                    return $qb;
                },
                'choice_label' => static function (Alert $alert): string {
                    return sprintf(
                        '%s • %s • %s',
                        $alert->getTitle(),
                        $alert->getSeverity(),
                        substr($alert->getAlertId(), 0, 8),
                    );
                },
                'placeholder' => 'Choose the alert',
            ])
            ->add('donor', EntityType::class, [
                'class' => Donor::class,
                'label' => 'Donor',
                'query_builder' => static function (EntityRepository $repository) {
                    return $repository->createQueryBuilder('donor')
                        ->orderBy('donor.firstName', 'ASC')
                        ->addOrderBy('donor.lastName', 'ASC');
                },
                'choice_label' => static function (Donor $donor): string {
                    $bloodType = $donor->getBloodType();

                    return sprintf(
                        '%s %s • %s',
                        $donor->getFirstName(),
                        $donor->getLastName(),
                        $bloodType?->getBloodTypeId() ?? 'Unknown blood type',
                    );
                },
                'placeholder' => 'Choose the donor',
            ])
            ->add('donorResponse', ChoiceType::class, [
                'label' => 'Donor Response',
                'choices' => DonorAlert::donorResponseChoices(),
                'required' => false,
                'placeholder' => false,
                'empty_data' => DonorAlert::RESPONSE_NO_RESPONSE,
            ])
            ->add('isNotified', CheckboxType::class, [
                'label' => 'Notification sent',
                'required' => false,
            ])
            ->add('notificationSentAt', DateTimeType::class, [
                'label' => 'Notification sent at',
                'required' => false,
                'widget' => 'single_text',
                'input' => 'datetime_immutable',
            ])
            ->add('isRead', CheckboxType::class, [
                'label' => 'Alert read',
                'required' => false,
            ])
            ->add('readAt', DateTimeType::class, [
                'label' => 'Read at',
                'required' => false,
                'widget' => 'single_text',
                'input' => 'datetime_immutable',
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => DonorAlert::class,
            'allowed_hospital_id' => null,
        ]);

        $resolver->setAllowedTypes('allowed_hospital_id', ['null', 'string']);
    }
}
