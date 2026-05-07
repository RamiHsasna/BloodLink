<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Add hospital deactivation metadata columns
 */
final class Version20260507100000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add hospital deactivation reason, duration, and end date columns';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE hospital ADD deactivation_reason TEXT');
        $this->addSql('ALTER TABLE hospital ADD deactivation_start_date TIMESTAMP DEFAULT NULL');
        $this->addSql('ALTER TABLE hospital ADD deactivation_duration_days INT DEFAULT NULL');
        $this->addSql('ALTER TABLE hospital ADD deactivation_end_date TIMESTAMP DEFAULT NULL');

        // Create index for faster queries on deactivation end date
        $this->addSql('CREATE INDEX idx_hospital_deactivation_end_date ON hospital(deactivation_end_date)');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('DROP INDEX IF EXISTS idx_hospital_deactivation_end_date');
        $this->addSql('ALTER TABLE hospital DROP COLUMN deactivation_end_date');
        $this->addSql('ALTER TABLE hospital DROP COLUMN deactivation_duration_days');
        $this->addSql('ALTER TABLE hospital DROP COLUMN deactivation_start_date');
        $this->addSql('ALTER TABLE hospital DROP COLUMN deactivation_reason');
    }
}
