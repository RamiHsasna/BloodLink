<?php

namespace App\Service;

use App\Entity\User;
use App\Entity\Donor;
use App\Entity\DonorEligibility;
use App\Repository\DonorRepository;
use App\Repository\DonorEligibilityRepository;
use Dompdf\Dompdf;
use Dompdf\Options;

class EligibilityReportService
{
    public function __construct(
        private DonorRepository $donorRepository,
        private DonorEligibilityRepository $eligibilityRepository,
    ) {}

    /**
     * Generate a PDF report for donor eligibility and return as binary string
     * 
     * @param User $user The donor user
     * @return string Binary PDF content
     */
    public function generateEligibilityReportPdf(User $user): string
    {
        $donor = $this->donorRepository->findOneBy(['userId' => $user->getUserId()]);
        $eligibility = $this->eligibilityRepository->findOneBy(['user' => $user]);

        if (!$donor) {
            throw new \Exception('Donor profile not found for this user.');
        }

        $html = $this->generateHtml($user, $donor, $eligibility);
        
        return $this->renderPdf($html);
    }

    /**
     * Generate HTML content for the eligibility report
     */
    private function generateHtml(User $user, Donor $donor, ?DonorEligibility $eligibility): string
    {
        $isEligible = $eligibility && $eligibility->getIsCurrentlyEligible();
        $statusText = $isEligible ? 'CURRENTLY ELIGIBLE' : 'NOT ELIGIBLE';
        $statusColor = $isEligible ? '#10b981' : '#ef4444';
        $statusBgColor = $isEligible ? '#ecfdf5' : '#fef2f2';
        
        $reportDate = new \DateTime();
        $lastCalculatedAt = $eligibility?->getLastCalculatedAt() 
            ? $eligibility->getLastCalculatedAt()->format('Y-m-d')
            : 'N/A';
        
        // Calculate values outside heredoc to avoid parse errors
        $donorCity = $donor->getCity() ?: 'Not specified';
        $lastDonationDisplay = $this->getLastDonationDisplay($donor);
        $bloodTypeDisplay = $this->getBloodTypeDisplay($eligibility);
        $donorIdTruncated = $this->truncateId($user->getUserId());
        $totalDonations = $donor->getTotalDonations() ?? 0;

        $html = <<<HTML
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BloodLink - Donor Eligibility Report</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            color: #0f172a;
            background-color: #f1f3f7;
            line-height: 1.6;
        }
        
        .container {
            max-width: 800px;
            margin: 0;
            background-color: #ffffff;
            padding: 40px;
        }
        
        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 3px solid #8f1d1f;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }
        
        .logo {
            font-size: 28px;
            font-weight: bold;
            color: #8f1d1f;
            font-family: Georgia, serif;
        }
        
        .report-type {
            text-align: right;
            font-size: 12px;
            color: #64748b;
        }
        
        .report-type .title {
            font-size: 14px;
            font-weight: bold;
            color: #0f172a;
        }
        
        .status-badge {
            display: inline-block;
            background-color: {$statusBgColor};
            color: {$statusColor};
            padding: 8px 16px;
            border-radius: 8px;
            font-weight: bold;
            font-size: 16px;
            margin: 20px 0;
        }
        
        .section {
            margin-bottom: 30px;
        }
        
        .section-title {
            font-size: 14px;
            font-weight: bold;
            text-transform: uppercase;
            color: #64748b;
            border-bottom: 2px solid #e2e8f0;
            padding-bottom: 8px;
            margin-bottom: 16px;
        }
        
        .info-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            margin-bottom: 20px;
        }
        
        .info-item {
            padding: 12px;
            background-color: #f8fafc;
            border-radius: 8px;
            border-left: 4px solid #8f1d1f;
        }
        
        .info-label {
            font-size: 11px;
            text-transform: uppercase;
            color: #64748b;
            font-weight: 600;
            margin-bottom: 4px;
            letter-spacing: 0.5px;
        }
        
        .info-value {
            font-size: 16px;
            font-weight: bold;
            color: #0f172a;
        }
        
        .details-box {
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 16px;
            margin-top: 16px;
        }
        
        .details-box .label {
            font-size: 12px;
            text-transform: uppercase;
            color: #64748b;
            font-weight: 600;
            margin-bottom: 8px;
        }
        
        .details-box .content {
            font-size: 13px;
            color: #475569;
            white-space: pre-wrap;
            word-wrap: break-word;
        }
        
        .footer {
            border-top: 1px solid #e2e8f0;
            padding-top: 20px;
            margin-top: 30px;
            text-align: center;
            font-size: 11px;
            color: #64748b;
        }
        
        .footer-note {
            font-size: 10px;
            color: #94a3b8;
            margin-top: 10px;
        }
        
        .warning-box {
            background-color: #fffbeb;
            border-left: 4px solid #f59e0b;
            padding: 12px;
            border-radius: 4px;
            margin: 16px 0;
            font-size: 12px;
            color: #7c2d12;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo">BloodLink</div>
            <div class="report-type">
                <div class="title">ELIGIBILITY REPORT</div>
                <div>{$reportDate->format('F d, Y')}</div>
            </div>
        </div>
        
        <div style="text-align: center; margin-bottom: 30px;">
            <h1 style="font-size: 24px; margin-bottom: 12px;">Donor Eligibility Assessment</h1>
            <div class="status-badge">{$statusText}</div>
        </div>
        
        <div class="section">
            <div class="section-title">Donor Information</div>
            <div class="info-grid">
                <div class="info-item">
                    <div class="info-label">Full Name</div>
                    <div class="info-value">{$donor->getFirstName()} {$donor->getLastName()}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Blood Type</div>
                    <div class="info-value">{$bloodTypeDisplay}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Location</div>
                    <div class="info-value">{$donorCity}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Donor ID</div>
                    <div class="info-value">{$donorIdTruncated}</div>
                </div>
            </div>
        </div>
        
        <div class="section">
            <div class="section-title">Eligibility Status</div>
            <div class="info-grid">
                <div class="info-item">
                    <div class="info-label">Current Status</div>
                    <div class="info-value" style="color: {$statusColor};">{$statusText}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Last Calculated</div>
                    <div class="info-value">{$lastCalculatedAt}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Last Donation</div>
                    <div class="info-value">{$lastDonationDisplay}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Total Donations</div>
                    <div class="info-value">{$totalDonations}</div>
                </div>
            </div>
        </div>
        
        {$this->getDaysUntilEligibleSection($eligibility)}
        
        {$this->getDetailsSection($eligibility)}
        
        <div class="warning-box">
             This report is generated for informational purposes only. Final eligibility determination 
            will be made by a qualified healthcare professional during your screening appointment.
        </div>
        
        <div class="footer">
            <div>BloodLink - Donor Management Platform</div>
            <div class="footer-note">
                This is an official eligibility report. Please present this report to the medical staff 
                at your nearest blood donation center.
            </div>
        </div>
    </div>
</body>
</html>
HTML;

        return $html;
    }

    /**
     * Get blood type display string
     */
    private function getBloodTypeDisplay(?DonorEligibility $eligibility): string
    {
        if (!$eligibility || !$eligibility->getBloodTypeCache()) {
            return 'Not specified';
        }
        return $eligibility->getBloodTypeCache();
    }

    /**
     * Get last donation display
     */
    private function getLastDonationDisplay(Donor $donor): string
    {
        if (!$donor->getLastDonationDate()) {
            return 'Never';
        }
        
        $lastDonation = $donor->getLastDonationDate();
        $today = new \DateTime();
        $interval = $today->diff($lastDonation);
        
        return $lastDonation->format('Y-m-d') . ' (' . $interval->days . ' days ago)';
    }

    /**
     * Get days until eligible section HTML
     */
    private function getDaysUntilEligibleSection(?DonorEligibility $eligibility): string
    {
        if (!$eligibility || $eligibility->getIsCurrentlyEligible() || $eligibility->getDaysUntilEligible() === null) {
            return '';
        }

        $daysUntil = $eligibility->getDaysUntilEligible();
        return <<<HTML
<div class="section">
    <div class="section-title">Eligibility Timeline</div>
    <div class="info-grid">
        <div class="info-item">
            <div class="info-label">Days Until Eligible</div>
            <div class="info-value">{$daysUntil} days</div>
        </div>
    </div>
</div>
HTML;
    }

    /**
     * Get details section HTML
     */
    private function getDetailsSection(?DonorEligibility $eligibility): string
    {
        if (!$eligibility || !$eligibility->getEligibilityDetails()) {
            return '';
        }

        $details = htmlspecialchars($eligibility->getEligibilityDetails(), ENT_QUOTES, 'UTF-8');
        return <<<HTML
<div class="section">
    <div class="section-title">Assessment Details</div>
    <div class="details-box">
        <div class="label">Eligibility Assessment</div>
        <div class="content">{$details}</div>
    </div>
</div>
HTML;
    }

    /**
     * Render PDF from HTML
     */
    private function renderPdf(string $html): string
    {
        $options = new Options();
        $options->set([
            'isRemoteEnabled' => false,
            'chroot' => sys_get_temp_dir(),
        ]);

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return $dompdf->output();
    }

    /**
     * Truncate UUID for display
     */
    private function truncateId(string $id): string
    {
        return substr($id, 0, 13) . '...';
    }
}
