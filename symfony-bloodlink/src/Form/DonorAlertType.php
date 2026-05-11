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
                    return sprintf('%s • %s • %s', $alert->getTitle(), $alert->getSeverity(), substr($alert->getAlertId(), 0, 8));
                },
                'placeholder' => 'Choose the alert',
            ])
            ->add('donor', EntityType::class, [
                'class' => Donor::class,
                'label' => 'Recipient Donor',
                'choice_label' => function (Donor $donor) {
                    return sprintf('%s %s (%s)', $donor->getFirstName(), $donor->getLastName(), $donor->getBloodType() ? $donor->getBloodType()->getBloodTypeId() : 'N/A');
                },
                'placeholder' => 'Select a donor',
            ])
            ->add('donorResponse', ChoiceType::class, [
                'label' => 'Donor Response',
                'choices' => DonorAlert::donorResponseChoices(),
                'required' => false,
            ])
            ->add('isNotified', CheckboxType::class, [
                'label' => 'Notification Sent',
                'required' => false,
                'false_values' => [null, false],
            ])
            ->add('isRead', CheckboxType::class, [
                'label' => 'Read by Donor',
                'required' => false,
                'false_values' => [null, false],
            ])
            ->add('notificationSentAt', DateTimeType::class, [
                'label' => 'Notification Sent At',
                'widget' => 'single_text',
                'required' => false,
            ])
            ->add('readAt', DateTimeType::class, [
                'label' => 'Read At',
                'widget' => 'single_text',
                'required' => false,
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
