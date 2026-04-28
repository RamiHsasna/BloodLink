<?php

namespace App\Service;

final class AuditAnomalyResult
{
    /**
     * @param array<int, string> $reasons
     * @param array<string, mixed> $rawAiResponse
     */
    public function __construct(
        private readonly bool $anomalous,
        private readonly string $severity,
        private readonly int $score,
        private readonly array $reasons,
        private readonly string $explanation,
        private readonly string $recommendedAction,
        private readonly string $provider,
        private readonly array $rawAiResponse = [],
    ) {
    }

    public function isAnomalous(): bool
    {
        return $this->anomalous;
    }

    public function getSeverity(): string
    {
        return $this->severity;
    }

    public function getScore(): int
    {
        return $this->score;
    }

    /**
     * @return array<int, string>
     */
    public function getReasons(): array
    {
        return $this->reasons;
    }

    public function getExplanation(): string
    {
        return $this->explanation;
    }

    public function getRecommendedAction(): string
    {
        return $this->recommendedAction;
    }

    public function getProvider(): string
    {
        return $this->provider;
    }

    /**
     * @return array<string, mixed>
     */
    public function getRawAiResponse(): array
    {
        return $this->rawAiResponse;
    }
}
