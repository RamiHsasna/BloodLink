<?php

namespace App\Form;

use App\Entity\DonationEvent;
use App\Entity\Hospital;
use Doctrine\ORM\EntityRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class DonationEventType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('name', TextType::class, [
                'label' => 'Event Name',
                'attr' => [
                    'placeholder' => 'Example: Spring Donation Drive',
                ],
            ])
            ->add('status', ChoiceType::class, [
                'label' => 'Status',
                'choices' => DonationEvent::statusChoices(),
                'placeholder' => 'Choose the event status',
            ])
            ->add('hospital', EntityType::class, [
                'class' => Hospital::class,
                'label' => 'Hosting Hospital',
                'query_builder' => static function (EntityRepository $repository) use ($options) {
                    $qb = $repository->createQueryBuilder('hospital')
                        ->orderBy('hospital.name', 'ASC');

                    if ($options['allowed_hospital_id'] !== null) {
                        $qb->andWhere('hospital.hospitalId = :hospitalId')
                            ->setParameter('hospitalId', $options['allowed_hospital_id']);
                    }

                    return $qb;
                },
                'choice_label' => static fn (Hospital $hospital): string => $hospital->getName(),
                'placeholder' => 'Choose the hosting hospital',
                'disabled' => $options['lock_hospital'],
            ])
            ->add('startDate', DateTimeType::class, [
                'label' => 'Start Date',
                'widget' => 'single_text',
            ])
            ->add('endDate', DateTimeType::class, [
                'label' => 'End Date',
                'widget' => 'single_text',
            ])
            ->add('location', TextType::class, [
                'label' => 'Location',
                'required' => false,
                'attr' => [
                    'placeholder' => 'Ward, lobby, mobile unit, or city location',
                ],
            ])
            ->add('targetBloodTypes', TextType::class, [
                'label' => 'Target Blood Types',
                'required' => false,
                'attr' => [
                    'placeholder' => 'Example: O-, A+, B+',
                ],
            ])
            ->add('targetCollectionUnits', IntegerType::class, [
                'label' => 'Target Units',
                'required' => false,
                'attr' => [
                    'min' => 1,
                    'placeholder' => 'Example: 25',
                ],
            ])
            ->add('actualCollectionUnits', IntegerType::class, [
                'label' => 'Collected Units',
                'required' => false,
                'attr' => [
                    'min' => 0,
                    'placeholder' => 'Filled after the event progresses',
                ],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'required' => false,
                'attr' => [
                    'rows' => 5,
                    'placeholder' => 'Explain the event context, donor focus, and practical notes.',
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => DonationEvent::class,
            'lock_hospital' => false,
            'allowed_hospital_id' => null,
        ]);

        $resolver->setAllowedTypes('lock_hospital', 'bool');
        $resolver->setAllowedTypes('allowed_hospital_id', ['null', 'string']);
    }
}
