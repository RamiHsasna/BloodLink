<?php
require_once __DIR__ . '/vendor/autoload.php';

use Doctrine\DBAL\DriverManager;

$dbUrl = 'postgresql://postgres.fgqrfdjoambaecwducpf:yLtVQD9QIlC7ee13@aws-1-eu-west-1.pooler.supabase.com:5432/postgres?serverVersion=15&charset=utf8&sslmode=require';

try {
    $connection = DriverManager::getConnection(['url' => $dbUrl]);
    
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
        $hospitalId = bin2hex(random_bytes(8)) . '-' . bin2hex(random_bytes(4)) . '-' . bin2hex(random_bytes(2)) . '-' . bin2hex(random_bytes(2)) . '-' . bin2hex(random_bytes(6));
        
        // Format UUID properly
        $hospitalId = sprintf(
            '%s-%s-%s-%s-%s',
            substr(bin2hex(random_bytes(16)), 0, 8),
            substr(bin2hex(random_bytes(16)), 0, 4),
            substr(bin2hex(random_bytes(16)), 0, 4),
            substr(bin2hex(random_bytes(16)), 0, 4),
            substr(bin2hex(random_bytes(16)), 0, 12)
        );
        
        $connection->executeStatement(
            "INSERT INTO hospitals (hospital_id, name, city, country, created_at) VALUES (:id, :name, :city, :country, :now)",
            [
                'id' => $hospitalId,
                'name' => $hospital[0],
                'city' => $hospital[1],
                'country' => $hospital[2],
                'now' => (new DateTime())->format('Y-m-d H:i:s'),
            ]
        );
        
        echo "✓ Created hospital: {$hospital[0]}\n";
    }
    
    echo "\nAll hospitals created successfully!\n";
    $connection->close();
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
    exit(1);
}
