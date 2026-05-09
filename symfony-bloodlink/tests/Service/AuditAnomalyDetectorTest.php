<?php

namespace App\Tests\Service;

use App\Entity\DonationLog;
use App\Entity\User;
use App\Repository\BloodTransferRequestLogRepository;
use App\Repository\DonationLogRepository;
use App\Service\AuditAiClient;
use App\Service\AuditAnomalyDetector;
use PHPUnit\Framework\TestCase;
use Symfony\Component\HttpClient\MockHttpClient;

final class AuditAnomalyDetectorTest extends TestCase
{
    public function testDonationScreeningFailureIsHighRiskAnomaly(): void
    {
        $_ENV['NVIDIA_API_KEY'] = '';
        $_SERVER['NVIDIA_API_KEY'] = '';

        $detector = $this->detector();
        $log = (new DonationLog())
            ->setLogId('f6f34e25-58ef-4f47-a951-35297465324d')
            ->setAction(DonationLog::ACTION_SCREENING_FAILED)
            ->setPreviousStatus(DonationLog::STATUS_APPROVED)
            ->setNewStatus(DonationLog::STATUS_REJECTED)
            ->setCreatedAt(new \DateTimeImmutable('2026-04-28 11:00:00'))
            ->setUser($this->actor());

        $result = $detector->analyzeDonationLog($log);

        self::assertTrue($result->isAnomalous());
        self::assertSame('high', $result->getSeverity());
        self::assertGreaterThanOrEqual(70, $result->getScore());
        self::assertContains('Screening failure requires audit review.', $result->getReasons());
    }

    public function testRoutineDonationTransitionIsNormal(): void
    {
        $_ENV['NVIDIA_API_KEY'] = '';
        $_SERVER['NVIDIA_API_KEY'] = '';

        $detector = $this->detector();
        $log = (new DonationLog())
            ->setLogId('efef25f6-2f8b-4dbd-a029-21fb5496eae2')
            ->setAction(DonationLog::ACTION_COLLECTED)
            ->setPreviousStatus(DonationLog::STATUS_PENDING)
            ->setNewStatus(DonationLog::STATUS_APPROVED)
            ->setNotes('Collected during the regular morning shift.')
            ->setCreatedAt(new \DateTimeImmutable('2026-04-28 10:30:00'))
            ->setUser($this->actor());

        $result = $detector->analyzeDonationLog($log);

        self::assertFalse($result->isAnomalous());
        self::assertSame('none', $result->getSeverity());
        self::assertSame(0, $result->getScore());
    }

    private function detector(): AuditAnomalyDetector
    {
        $donationRepository = $this->createMock(DonationLogRepository::class);
        $donationRepository
            ->method('countByActorSince')
            ->willReturn(0);

        $transferRepository = $this->createMock(BloodTransferRequestLogRepository::class);
        $transferRepository
            ->method('countByActorSince')
            ->willReturn(0);

        return new AuditAnomalyDetector(
            new AuditAiClient(new MockHttpClient()),
            $donationRepository,
            $transferRepository,
        );
    }

    private function actor(): User
    {
        return (new User())
            ->setUserId('a45e6f52-8a20-43b8-b8b6-4638976de8ed')
            ->setEmail('audit@example.test')
            ->setFirstName('Audit')
            ->setLastName('Owner')
            ->setUserType(User::TYPE_ADMIN)
            ->setPasswordHash('unused');
    }
}
