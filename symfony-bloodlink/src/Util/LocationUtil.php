<?php

namespace App\Util;

/**
 * Utility class for location-based calculations and distance computations.
 */
class LocationUtil
{
    /**
     * Earth's radius in kilometers.
     */
    private const EARTH_RADIUS_KM = 6371;

    /**
     * Calculate the great-circle distance between two points on Earth using the Haversine formula.
     *
     * @param float $latitude1 Latitude of first point in degrees
     * @param float $longitude1 Longitude of first point in degrees
     * @param float $latitude2 Latitude of second point in degrees
     * @param float $longitude2 Longitude of second point in degrees
     *
     * @return float Distance in kilometers
     */
    public static function calculateDistance(
        float $latitude1,
        float $longitude1,
        float $latitude2,
        float $longitude2,
    ): float {
        // Convert degrees to radians
        $lat1Rad = deg2rad($latitude1);
        $lon1Rad = deg2rad($longitude1);
        $lat2Rad = deg2rad($latitude2);
        $lon2Rad = deg2rad($longitude2);

        // Haversine formula
        // a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
        // c = 2 ⋅ atan2( √a, √(1−a) )
        // d = R ⋅ c

        $latDistance = $lat2Rad - $lat1Rad;
        $lonDistance = $lon2Rad - $lon1Rad;

        $a =
            sin($latDistance / 2) * sin($latDistance / 2) +
            cos($lat1Rad) *
                cos($lat2Rad) *
                sin($lonDistance / 2) *
                sin($lonDistance / 2);

        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

        return self::EARTH_RADIUS_KM * $c;
    }

    /**
     * Find donors within a specified radius from a hospital.
     *
     * @param float $hospitalLatitude Hospital latitude
     * @param float $hospitalLongitude Hospital longitude
     * @param array $donors Array of donors with latitude/longitude
     * @param float $radiusKm Search radius in kilometers
     *
     * @return array Filtered donors within radius, sorted by distance
     */
    public static function findDonorsWithinRadius(
        float $hospitalLatitude,
        float $hospitalLongitude,
        array $donors,
        float $radiusKm = 50,
    ): array {
        $donorsWithDistance = [];

        foreach ($donors as $donor) {
            // Skip donors without coordinates
            if (
                !isset($donor["latitude"]) ||
                !isset($donor["longitude"]) ||
                $donor["latitude"] === null ||
                $donor["longitude"] === null
            ) {
                continue;
            }

            $donorLat = (float) $donor["latitude"];
            $donorLon = (float) $donor["longitude"];

            // Calculate distance
            $distance = self::calculateDistance(
                $hospitalLatitude,
                $hospitalLongitude,
                $donorLat,
                $donorLon,
            );

            // Filter by radius
            if ($distance <= $radiusKm) {
                $donorsWithDistance[] = [
                    "donor" => $donor,
                    "distance" => $distance,
                ];
            }
        }

        // Sort by distance (closest first)
        usort($donorsWithDistance, function ($a, $b) {
            return $a["distance"] <=> $b["distance"];
        });

        // Return just the donor objects
        return array_map(function ($item) {
            return $item["donor"];
        }, $donorsWithDistance);
    }

    /**
     * Validate if coordinates are within valid geographic bounds.
     *
     * @param float $latitude Latitude value (-90 to 90)
     * @param float $longitude Longitude value (-180 to 180)
     *
     * @return bool
     */
    public static function isValidCoordinate(
        float $latitude,
        float $longitude,
    ): bool {
        return $latitude >= -90 &&
            $latitude <= 90 &&
            $longitude >= -180 &&
            $longitude <= 180;
    }

    /**
     * Convert string coordinate to float, handling various formats.
     *
     * @param string $coordinate Coordinate as string
     *
     * @return float|null Parsed coordinate or null if invalid
     */
    public static function parseCoordinate(string $coordinate): ?float
    {
        // Trim whitespace
        $coordinate = trim($coordinate);

        // Handle decimal format (e.g., "40.7128")
        if (is_numeric($coordinate)) {
            $value = (float) $coordinate;
            // Validate range based on context would need additional parameter
            // For now, return the value and let caller validate
            return $value;
        }

        // Handle DMS format (e.g., "40°42'46.6"N" or "40° 42' 46.6" N")
        // Pattern: degrees°minutes'seconds"direction or similar variations
        $pattern =
            '/^(\d+)[\s°]+(\d+)[\s\']+(\d+(?:\.\d+)?)["\s]*(N|S|E|W)?\s*(N|S|E|W)?$/i';

        if (preg_match($pattern, $coordinate, $matches)) {
            $degrees = (int) $matches[1];
            $minutes = (int) $matches[2];
            $seconds = (float) $matches[3];

            // Get direction (N, S, E, W)
            $direction = $matches[4] ?? ($matches[5] ?? null);

            // Convert to decimal
            $decimal = $degrees + $minutes / 60 + $seconds / 3600;

            // Apply direction
            if (
                $direction &&
                in_array(strtoupper($direction), ["S", "W"], true)
            ) {
                $decimal = -$decimal;
            }

            return $decimal;
        }

        // If no format matches, return null
        return null;
    }
}
