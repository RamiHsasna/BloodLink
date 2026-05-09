<?php

namespace App\Command;

use Doctrine\DBAL\Connection;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

class CreateHospitalsCommand extends Command
{
    protected static $defaultName = 'app:create-hospitals';

    public function __construct(private Connection $connection)
    {
        parent::__construct();
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $hospitals = [
            ['Monastir Central Hospital', 'Monastir', 'Tunisia'],
            ['Tunis Teaching Hospital', 'Tunis', 'Tunisia'],
            ['Sousse Medical Center', 'Sousse', 'Tunisia'],
            ['Sfax Regional Hospital', 'Sfax', 'Tunisia'],
            ['Gafsa Health Clinic', 'Gafsa', 'Tunisia'],
            ['Djerba General Hospital', 'Djerba', 'Tunisia'],
            ['Bizerte Medical Care', 'Bizerte', 'Tunisia'],
            ['Kairouan Central Clinic', 'Kairouan', 'Tunisia'],
        ];

        foreach ($hospitals as $hospital) {
            $hospitalId = $this->generateUuidV4();
            $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');

            $this->connection->executeStatement(
                "INSERT INTO hospitals (hospital_id, name, city, country, created_at) VALUES (:id, :name, :city, :country, :now)",
                [
                    'id' => $hospitalId,
                    'name' => $hospital[0],
                    'city' => $hospital[1],
                    'country' => $hospital[2],
                    'now' => $now,
                ]
            );

            $output->writeln("<info>✓ Created hospital: {$hospital[0]}</info>");
        }

        $output->writeln("\n<fg=green>All hospitals created successfully!</>");
        return Command::SUCCESS;
    }

    private function generateUuidV4(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);

        $hex = bin2hex($bytes);

        return sprintf(
            '%s-%s-%s-%s-%s',
            substr($hex, 0, 8),
            substr($hex, 8, 4),
            substr($hex, 12, 4),
            substr($hex, 16, 4),
            substr($hex, 20, 12),
        );
    }
}
