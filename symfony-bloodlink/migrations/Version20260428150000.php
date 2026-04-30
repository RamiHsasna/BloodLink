<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260428150000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add Audit Logs anomaly detection and SMS alert metadata.';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('ALTER TABLE donation_log ADD anomaly_detected BOOLEAN DEFAULT false NOT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_severity VARCHAR(20) DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_score INT DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_reasons JSON DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_explanation TEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_recommended_action TEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_analysis_provider VARCHAR(40) DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_raw_response JSON DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_reviewed BOOLEAN DEFAULT false NOT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_reviewed_at TIMESTAMP(0) WITHOUT TIME ZONE DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD anomaly_reviewed_by UUID DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD sms_alert_status VARCHAR(20) DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD sms_alert_recipient VARCHAR(255) DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD sms_alert_error TEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE donation_log ADD sms_alert_sent_at TIMESTAMP(0) WITHOUT TIME ZONE DEFAULT NULL');
        $this->addSql('CREATE INDEX IDX_DONATION_LOG_ANOMALY_REVIEWED_BY ON donation_log (anomaly_reviewed_by)');
        $this->addSql('CREATE INDEX IDX_DONATION_LOG_ANOMALY_STATUS ON donation_log (anomaly_detected, anomaly_severity, anomaly_reviewed)');
        $this->addSql('ALTER TABLE donation_log ADD CONSTRAINT FK_DONATION_LOG_ANOMALY_REVIEWER FOREIGN KEY (anomaly_reviewed_by) REFERENCES users (user_id) NOT DEFERRABLE INITIALLY IMMEDIATE');

        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_detected BOOLEAN DEFAULT false NOT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_severity VARCHAR(20) DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_score INT DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_reasons JSON DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_explanation TEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_recommended_action TEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_analysis_provider VARCHAR(40) DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_raw_response JSON DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_reviewed BOOLEAN DEFAULT false NOT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_reviewed_at TIMESTAMP(0) WITHOUT TIME ZONE DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD anomaly_reviewed_by UUID DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD sms_alert_status VARCHAR(20) DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD sms_alert_recipient VARCHAR(255) DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD sms_alert_error TEXT DEFAULT NULL');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD sms_alert_sent_at TIMESTAMP(0) WITHOUT TIME ZONE DEFAULT NULL');
        $this->addSql('CREATE INDEX IDX_TRANSFER_LOG_ANOMALY_REVIEWED_BY ON blood_transfer_request_log (anomaly_reviewed_by)');
        $this->addSql('CREATE INDEX IDX_TRANSFER_LOG_ANOMALY_STATUS ON blood_transfer_request_log (anomaly_detected, anomaly_severity, anomaly_reviewed)');
        $this->addSql('ALTER TABLE blood_transfer_request_log ADD CONSTRAINT FK_TRANSFER_LOG_ANOMALY_REVIEWER FOREIGN KEY (anomaly_reviewed_by) REFERENCES users (user_id) NOT DEFERRABLE INITIALLY IMMEDIATE');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE donation_log DROP CONSTRAINT FK_DONATION_LOG_ANOMALY_REVIEWER');
        $this->addSql('DROP INDEX IDX_DONATION_LOG_ANOMALY_REVIEWED_BY');
        $this->addSql('DROP INDEX IDX_DONATION_LOG_ANOMALY_STATUS');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_detected');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_severity');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_score');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_reasons');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_explanation');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_recommended_action');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_analysis_provider');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_raw_response');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_reviewed');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_reviewed_at');
        $this->addSql('ALTER TABLE donation_log DROP anomaly_reviewed_by');
        $this->addSql('ALTER TABLE donation_log DROP sms_alert_status');
        $this->addSql('ALTER TABLE donation_log DROP sms_alert_recipient');
        $this->addSql('ALTER TABLE donation_log DROP sms_alert_error');
        $this->addSql('ALTER TABLE donation_log DROP sms_alert_sent_at');

        $this->addSql('ALTER TABLE blood_transfer_request_log DROP CONSTRAINT FK_TRANSFER_LOG_ANOMALY_REVIEWER');
        $this->addSql('DROP INDEX IDX_TRANSFER_LOG_ANOMALY_REVIEWED_BY');
        $this->addSql('DROP INDEX IDX_TRANSFER_LOG_ANOMALY_STATUS');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_detected');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_severity');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_score');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_reasons');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_explanation');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_recommended_action');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_analysis_provider');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_raw_response');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_reviewed');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_reviewed_at');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP anomaly_reviewed_by');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP sms_alert_status');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP sms_alert_recipient');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP sms_alert_error');
        $this->addSql('ALTER TABLE blood_transfer_request_log DROP sms_alert_sent_at');
    }
}
