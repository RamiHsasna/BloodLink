<?php

namespace App\Service;

use App\Entity\User;
use App\Entity\Donor;
use App\Entity\DonorEligibility;
use App\Repository\DonorRepository;
use App\Repository\DonorEligibilityRepository;
use Dompdf\Dompdf;
use Dompdf\Options;
use Twig\Environment;

class EligibilityReportService
{
    public function __construct(
        private DonorRepository $donorRepository,
        private DonorEligibilityRepository $eligibilityRepository,
        private Environment $twig,
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
        $reportDate = new \DateTimeImmutable();
        $parsed = $this->parseEligibilityDetails($eligibility?->getEligibilityDetails());

        $fullName = trim($donor->getFirstName() . ' ' . $donor->getLastName());
        $donorId = $this->truncateId($user->getUserId());
        $dateOfBirth = $parsed['Date of birth'] ?? '';
        $gender = $parsed['Gender'] ?? '';
        $bloodType = $this->getBloodTypeDisplay($eligibility);

        $weight = $parsed['Weight'] ?? '';
        $temperature = '';
        $bloodPressure = '';
        $pulseRate = '';
        $hemoglobin = '';

        $isEligible = (bool) ($eligibility?->getIsCurrentlyEligible() ?? false);
        $daysUntilEligible = $eligibility?->getDaysUntilEligible();
        $infectiousDisease = $this->isYes($parsed['Infectious disease'] ?? null);

        $accepted = $isEligible;
        $deferredTemporary = !$isEligible && !$infectiousDisease;
        $deferredPermanent = !$isEligible && $infectiousDisease;

        $reasonsText = $this->extractAssessmentReasons($eligibility?->getEligibilityDetails());
        if ($reasonsText === '') {
            if ($isEligible) {
                $reasonsText = 'No deferral reason. Donor is currently eligible.';
            } elseif ($daysUntilEligible !== null && $daysUntilEligible > 0) {
                $reasonsText = 'Deferred for ' . (string) $daysUntilEligible . ' day(s) pending reevaluation.';
            } else {
                $reasonsText = 'Further medical review required.';
            }
        }

        $questionnaireItems = $this->buildQuestionnaireItems($parsed);

        return $this->twig->render('dashboard/eligibility_report_pdf.html.twig', [
            'reportDate' => $reportDate->format('Y-m-d H:i'),
            'fullName' => $fullName,
            'donorId' => $donorId,
            'dateOfBirth' => $dateOfBirth,
            'gender' => $gender,
            'bloodType' => $bloodType,
            'weight' => $weight,
            'temperature' => $temperature,
            'bloodPressure' => $bloodPressure,
            'pulseRate' => $pulseRate,
            'hemoglobin' => $hemoglobin,
            'questionnaireItems' => $questionnaireItems,
            'accepted' => $accepted,
            'deferredTemporary' => $deferredTemporary,
            'deferredPermanent' => $deferredPermanent,
            'reasonsText' => $reasonsText,
        ]);
    }

    /**
     * @return array<string, string>
     */
    private function parseEligibilityDetails(?string $details): array
    {
        if ($details === null || trim($details) === '') {
            return [];
        }

        $parsed = [];
        $lines = preg_split('/\R/u', $details) ?: [];
        foreach ($lines as $line) {
            $line = trim($line);
            if ($line === '' || str_starts_with($line, '===') || str_starts_with($line, '- ')) {
                continue;
            }

            if (!str_contains($line, ':')) {
                continue;
            }

            [$key, $value] = explode(':', $line, 2);
            $key = trim($key);
            $value = trim($value);
            if ($key !== '') {
                $parsed[$key] = $value;
            }
        }

        return $parsed;
    }

    private function extractAssessmentReasons(?string $details): string
    {
        if ($details === null || trim($details) === '') {
            return '';
        }

        $lines = preg_split('/\R/u', $details) ?: [];
        $reasons = [];
        $inReasons = false;

        foreach ($lines as $line) {
            $line = trim($line);
            if ($line === '=== ASSESSMENT REASONS ===') {
                $inReasons = true;
                continue;
            }

            if (!$inReasons) {
                continue;
            }

            if ($line === '' || str_starts_with($line, '===')) {
                continue;
            }

            if (str_starts_with($line, '- ')) {
                $reasons[] = substr($line, 2);
            } else {
                $reasons[] = $line;
            }
        }

        return implode("\n", $reasons);
    }

    /**
     * @param array<string, string> $parsed
     */
    private function buildQuestionnaireItems(array $parsed): array
    {
        $map = [
            'Are you feeling well and healthy today?' => $this->invertYesNo($parsed['Not feeling well'] ?? null),
            'Do you have fever or elevated temperature?' => $parsed['Have fever/elevated temperature'] ?? null,
            'Do you currently have any acute illness?' => $parsed['Have acute illness'] ?? null,
            'Female: are you currently pregnant or recent birth?' => $parsed['Pregnant or recent birth'] ?? null,
            'Do you have a heart disease or cardiac condition?' => $parsed['Heart disease/cardiac condition'] ?? null,
            'Do you have anemia or iron deficiency?' => $parsed['Anemia/iron deficiency'] ?? null,
            'Do you have chronic illness or disease?' => $parsed['Chronic illness or disease'] ?? null,
            'Have you taken any medication in the last week?' => $parsed['On medication'] ?? null,
            'Do you have a bleeding disorder or clotting issue?' => $parsed['Bleeding disorder/clotting issue'] ?? null,
            'Have you tested positive for HIV, hepatitis, or malaria?' => $parsed['Infectious disease'] ?? null,
            'Have you had surgery or procedures in the last 6 months?' => $parsed['Recent surgery/procedures'] ?? null,
            'Have you received any vaccine recently?' => $parsed['Recent vaccine'] ?? null,
            'Have you traveled abroad recently?' => $parsed['Recent travel abroad'] ?? null,
            'Have you had a tattoo or piercing in the last 6 months?' => $parsed['Recent tattoo/piercing'] ?? null,
            'Have you had a blood transfusion in the last 6 months?' => $parsed['Recent blood transfusion'] ?? null,
        ];

        $items = [];
        foreach ($map as $question => $answer) {
            $yes = $this->isYes($answer);
            $no = $this->isNo($answer);

            $items[] = [
                'question' => $question,
                'yes' => $yes,
                'no' => $no,
            ];
        }

        return $items;
    }

    private function invertYesNo(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return null;
        }

        if ($this->isYes($value)) {
            return 'No';
        }

        if ($this->isNo($value)) {
            return 'Yes';
        }

        return null;
    }

    private function isYes(?string $value): bool
    {
        if ($value === null) {
            return false;
        }

        return in_array(strtolower(trim($value)), ['yes', 'true', '1'], true);
    }

    private function isNo(?string $value): bool
    {
        if ($value === null) {
            return false;
        }

        return in_array(strtolower(trim($value)), ['no', 'false', '0'], true);
    }

    /**
     * Get blood type display string
     */
    private function getBloodTypeDisplay(?DonorEligibility $eligibility): string
    {
        if (!$eligibility || !$eligibility->getBloodTypeCache()) {
            return '';
        }
        return $eligibility->getBloodTypeCache();
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
