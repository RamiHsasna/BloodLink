<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

final class AuditAiClient
{
    private const ENDPOINT = 'https://openrouter.ai/api/v1/chat/completions';
    private const MODEL = 'anthropic/claude-3-haiku';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
    ) {
    }

    /**
     * @param array<string, mixed> $payload
     *
     * @return array<string, mixed>|null
     */
    public function analyzeAuditPayload(array $payload): ?array
    {
        $apiKey = $this->env('ANTHROPIC_API_KEY');
        if ($apiKey === '') {
            return null;
        }

        $messages = [
            [
                'role' => 'system',
                'content' => implode("\n", [
                    'You are an audit anomaly reviewer for the BloodLink medical audit logs module.',
                    'Return only valid JSON with keys: anomalous, severity, score, reasons, explanation, recommended_action.',
                    'severity must be one of none, low, medium, high, critical.',
                    'score must be an integer from 0 to 100.',
                    'reasons must be a short array of concrete audit-risk reasons.',
                    'Never invent facts beyond the supplied payload.',
                ]),
            ],
            [
                'role' => 'user',
                'content' => json_encode($payload, JSON_PARTIAL_OUTPUT_ON_ERROR) ?: '{}',
            ],
        ];

        try {
            $response = $this->httpClient->request('POST', self::ENDPOINT, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $apiKey,
                    'Content-Type' => 'application/json',
                    'HTTP-Referer' => 'http://localhost:8000',
                    'X-Title' => 'BloodLink AI Assistant',
                ],
                'json' => [
                    'model' => self::MODEL,
                    'messages' => $messages,
                    'max_tokens' => 1000,
                    'temperature' => 0.1,
                ],
                'timeout' => 12,
            ]);

            $data = $response->toArray(false);
            $content = (string) ($data['choices'][0]['message']['content'] ?? '');
            if ($content === '') {
                return null;
            }

            return $this->decodeJsonObject($content);
        } catch (\Throwable) {
            return null;
        }
    }

    /**
     * @param array<string, mixed> $context
     */
    public function answerAuditQuestion(array $context, string $question): string
    {
        $apiKey = $this->env('ANTHROPIC_API_KEY');
        if ($apiKey === '') {
            return 'AI service API key is not configured.';
        }

        $question = trim($question);
        if ($question === '') {
            return 'Ask a question about the visible audit logs.';
        }

        $messages = [
            [
                'role' => 'system',
                'content' => implode("\n", [
                    'You are the BloodLink Audit Logs assistant, a helpful teammate for hospital staff reviewing audit activity.',
                    'Answer only questions about the audit log context supplied by the server.',
                    'Use the anomaly severity, anomaly score, SMS status, action, status transition, and timestamp when relevant.',
                    'Be conversational and practical, not rigid.',
                    'If the user greets you, greet them back and offer two or three audit-log things you can check.',
                    'If the user replies with a short answer like yes, no, ok, or thanks, respond naturally and continue the conversation instead of asking for a formal question.',
                    'For "no", acknowledge it and offer another useful audit-log direction.',
                    'When useful, suggest the next audit action in plain language.',
                    'Do not invent records or expose secrets.',
                    'If the question is outside audit logs, briefly steer back to what you can help with in audit logs.',
                    'Keep the answer concise, maximum three sentences.',
                ]),
            ],
            [
                'role' => 'user',
                'content' => json_encode([
                    'context' => $context,
                    'question' => $question,
                ], JSON_PARTIAL_OUTPUT_ON_ERROR) ?: '{}',
            ],
        ];

        try {
            $response = $this->httpClient->request('POST', self::ENDPOINT, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $apiKey,
                    'Content-Type' => 'application/json',
                    'HTTP-Referer' => 'http://localhost:8000',
                    'X-Title' => 'BloodLink AI Assistant',
                ],
                'json' => [
                    'model' => self::MODEL,
                    'messages' => $messages,
                    'max_tokens' => 1000,
                    'temperature' => 0.2,
                ],
                'timeout' => 12,
            ]);

            $data = $response->toArray(false);
            $content = trim((string) ($data['choices'][0]['message']['content'] ?? ''));

            return $content !== '' ? $content : 'No answer was returned by the AI service.';
        } catch (\Throwable $exception) {
            return 'AI service error: ' . $exception->getMessage();
        }
    }

    /**
     * @return array<string, mixed>|null
     */
    private function decodeJsonObject(string $content): ?array
    {
        $content = trim($content);
        if ($content === '') {
            return null;
        }

        $decoded = json_decode($content, true);
        if (is_array($decoded)) {
            return $decoded;
        }

        if (preg_match('/\{.*\}/s', $content, $matches) !== 1) {
            return null;
        }

        $decoded = json_decode($matches[0], true);

        return is_array($decoded) ? $decoded : null;
    }

    private function env(string $key): string
    {
        $value = $_ENV[$key] ?? $_SERVER[$key] ?? getenv($key);

        return is_string($value) ? trim($value) : '';
    }
}
