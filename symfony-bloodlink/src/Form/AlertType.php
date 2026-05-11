<?php

namespace App\Form;

use App\Entity\Alert;
use App\Entity\BloodType;
use Doctrine\ORM\EntityRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class AlertType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('hospitalId', EntityType::class, [
                'class' => \App\Entity\Hospital::class,
                'label' => 'Hospital',
                'choice_label' => 'name',
                'choice_value' => 'hospitalId',
                'placeholder' => 'Select hospital',
                'mapped' => false,
                'query_builder' => function (EntityRepository $er) use ($options) {
                    $qb = $er->createQueryBuilder('h')
                        ->orderBy('h.name', 'ASC');
                    
                    if ($options['allowed_hospital_id']) {
                        $qb->where('h.hospitalId = :hospitalId')
                           ->setParameter('hospitalId', $options['allowed_hospital_id']);
                    }
                    
                    return $qb;
                },
                'constraints' => [
                    new Assert\NotBlank(message: 'Please select a hospital.'),
                ],
                'attr' => [
                    'disabled' => $options['allowed_hospital_id'] !== null,
                ],
            ])
            ->add('bloodTypeId', ChoiceType::class, [
                'label' => 'Blood Type Needed',
                'choices' => [
                    'O+' => 'O+',
                    'O-' => 'O-',
                    'A+' => 'A+',
                    'A-' => 'A-',
                    'B+' => 'B+',
                    'B-' => 'B-',
                    'AB+' => 'AB+',
                    'AB-' => 'AB-',
                ],
                'placeholder' => 'Select blood type',
                'constraints' => [
                    new Assert\NotBlank(message: 'Please select a blood type.'),
                ],
            ])
            ->add('severity', ChoiceType::class, [
                'label' => 'Severity Level',
                'choices' => [
                    'Info' => 'INFO',
                    'Warning' => 'WARNING',
                    'Urgent' => 'URGENT',
                    'Critical' => 'CRITICAL',
                ],
                'placeholder' => 'Select severity',
                'constraints' => [
                    new Assert\NotBlank(message: 'Please select a severity level.'),
                ],
            ])
            ->add('quantityNeeded', IntegerType::class, [
                'label' => 'Quantity Needed (units)',
                'attr' => ['min' => 1, 'max' => 100],
                'constraints' => [
                    new Assert\NotBlank(message: 'Please enter the quantity needed.'),
                    new Assert\Positive(message: 'Quantity must be a positive number.'),
                    new Assert\LessThanOrEqual(value: 100, message: 'Quantity cannot exceed 100 units.'),
                ],
            ])
            ->add('title', TextType::class, [
                'label' => 'Alert Title',
                'attr' => ['maxlength' => 255, 'placeholder' => 'e.g. Urgent need for O+ blood'],
                'constraints' => [
                    new Assert\NotBlank(message: 'Please enter a title.'),
                    new Assert\Length(max: 255, maxMessage: 'Title cannot be longer than 255 characters.'),
                ],
            ])
            ->add('message', TextareaType::class, [
                'label' => 'Message',
                'attr' => ['rows' => 4, 'placeholder' => 'Describe the blood need and any important details...'],
                'constraints' => [
                    new Assert\NotBlank(message: 'Please enter a message.'),
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Alert::class,
            'allowed_hospital_id' => null,
        ]);
        $resolver->setAllowedTypes('allowed_hospital_id', ['null', 'string']);
    }
}
