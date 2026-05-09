<?php

namespace App\Form;

use App\Entity\Hospital;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TelType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Regex;

class HospitalType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('name', TextType::class, [
                'label' => 'Hospital Name',
                'attr' => [
                    'placeholder' => 'Enter hospital name',
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Hospital name is required']),
                    new Length(['min' => 2, 'minMessage' => 'Hospital name must be at least 2 characters']),
                ],
            ])
            ->add('address', TextareaType::class, [
                'label' => 'Address',
                'attr' => [
                    'placeholder' => 'Enter street address',
                    'rows' => 3,
                ],
                'required' => false,
            ])
            ->add('city', TextType::class, [
                'label' => 'City',
                'attr' => [
                    'placeholder' => 'Enter city name',
                ],
                'required' => false,
            ])
            ->add('phone', TelType::class, [
                'label' => 'Phone',
                'attr' => [
                    'placeholder' => 'e.g. +216 98 123 4567',
                ],
                'required' => false,
                'constraints' => [
                    new Regex([
                        'pattern' => '/^[+]?[0-9\s\-().]{6,20}$/',
                        'message' => 'Phone must be 6-20 characters with valid format',
                    ]),
                ],
            ])
            ->add('email', EmailType::class, [
                'label' => 'Email',
                'attr' => [
                    'placeholder' => 'Enter email address',
                ],
                'required' => false,
                'constraints' => [
                    new Email(['message' => 'Invalid email format']),
                ],
            ])
            ->add('latitude', NumberType::class, [
                'label' => 'Latitude',
                'attr' => [
                    'placeholder' => '-90 to 90',
                    'step' => '0.00000001',
                ],
                'required' => false,
                'scale' => 8,
            ])
            ->add('longitude', NumberType::class, [
                'label' => 'Longitude',
                'attr' => [
                    'placeholder' => '-180 to 180',
                    'step' => '0.00000001',
                ],
                'required' => false,
                'scale' => 8,
            ])
            ->add('isActive', CheckboxType::class, [
                'label' => 'Active',
                'required' => false,
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Hospital::class,
        ]);
    }
}
