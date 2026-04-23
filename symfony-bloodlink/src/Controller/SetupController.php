<?php

namespace App\Controller;

use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class SetupController extends AbstractController
{
    #[Route('/setup/create-hospitals', name: 'setup_create_hospitals', methods: ['GET'])]
    public function createHospitals(Connection $connection): Response
    {
        try {
            // Check if hospitals already exist
            $count = $connection->fetchOne("SELECT COUNT(*) FROM hospitals");
            
            if ($count > 0) {
                return new Response("Hospitals already exist in database (Count: {$count})", 200);
            }

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

            $created = 0;
            foreach ($hospitals as $hospital) {
                $hospitalId = $this->generateUuidV4();
                $now = (new \DateTimeImmutable())->format('Y-m-d H:i:s');

                $connection->executeStatement(
                    "INSERT INTO hospitals (hospital_id, name, city, country, created_at) VALUES (:id, :name, :city, :country, :now)",
                    [
                        'id' => $hospitalId,
                        'name' => $hospital[0],
                        'city' => $hospital[1],
                        'country' => $hospital[2],
                        'now' => $now,
                    ]
                );
                $created++;
            }

            return new Response("<h1>✓ Success!</h1><p>Created {$created} hospitals successfully.</p>", 200);

        } catch (\Exception $e) {
            return new Response("<h1>✗ Error</h1><p>" . htmlspecialchars($e->getMessage()) . "</p>", 500);
        }
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
