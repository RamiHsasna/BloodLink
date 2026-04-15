<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Throwable;

class GeocodingService
{
    public function __construct(private readonly HttpClientInterface $httpClient)
    {
    }

    /**
     * @return array{latitude:string,longitude:string}|null
     */
    public function geocodeCity(string $city): ?array
    {
        $city = trim($city);
        if ($city === '') {
            return null;
        }

        try {
            $response = $this->httpClient->request('GET', 'https://nominatim.openstreetmap.org/search', [
                'query' => [
                    'q' => $city,
                    'format' => 'jsonv2',
                    'limit' => 1,
                ],
                'headers' => [
                    'User-Agent' => 'BloodLink Symfony App/1.0',
                    'Accept' => 'application/json',
                ],
                'timeout' => 8,
            ]);

            $data = $response->toArray(false);
            if (!is_array($data) || $data === [] || !is_array($data[0] ?? null)) {
                return null;
            }

            $lat = trim((string) ($data[0]['lat'] ?? ''));
            $lon = trim((string) ($data[0]['lon'] ?? ''));
            if ($lat === '' || $lon === '') {
                return null;
            }

            return [
                'latitude' => $lat,
                'longitude' => $lon,
            ];
        } catch (Throwable) {
            return null;
        }
    }
}
