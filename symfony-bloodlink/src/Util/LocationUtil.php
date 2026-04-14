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
        // TODO: Implement Haversine formula for distance calculation
        // Formula: a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
        // c = 2 ⋅ atan2( √a, √(1−a) )
        // d = R ⋅ c
        // where φ is latitude, λ is longitude, R is earth's radius

        return 0.0; // Placeholder
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
        // TODO: Implement filtering logic
        // 1. Calculate distance from hospital to each donor
        // 2. Filter donors within specified radius
        // 3. Sort by distance (closest first)
        // 4. Return filtered and sorted array

        return [];
    }

    /**
     * Validate if coordinates are within valid geographic bounds.
     *
     * @param float $latitude Latitude value (-90 to 90)
     * @param float $longitude Longitude value (-180 to 180)
     *
     * @return bool
     */
    public static function isValidCoordinate(float $latitude, float $longitude): bool {
        // TODO: Implement coordinate validation
        // Latitude should be between -90 and 90
        // Longitude should be between -180 and 180

        return true; // Placeholder
    }

    /**
     * Convert string coordinate to float, handling various formats.
     *
     * @param string $coordinate Coordinate as string
     *
     * @return float|null Parsed coordinate or null if invalid
     */
    public static function parseCoordinate(string $coordinate): ?float {
        // TODO: Implement coordinate parsing
        // Handle formats like: "40.7128", "40°42'46.6"N", etc.

        return null; // Placeholder
    }
}
