<?php

namespace App\Util;

/**
 * Utility class for blood type compatibility checking.
 * Implements ABO and Rh factor compatibility rules.
 */
class BloodCompatibilityUtil
{
    /**
     * Check if a donor's blood type can donate to a recipient's blood type.
     *
     * @param string $donorBloodType Donor blood type (e.g., "O+", "AB-")
     * @param string $recipientBloodType Recipient blood type
     *
     * @return bool True if donation is compatible
     */
    public static function isCompatible(string $donorBloodType, string $recipientBloodType): bool
    {
        // TODO: Implement blood type compatibility logic
        // ABO Compatibility Rules:
        // - O can donate to all (universal donor)
        // - A can donate to A and AB
        // - B can donate to B and AB
        // - AB can only donate to AB
        // Rh Compatibility:
        // - Rh- can donate to both Rh+ and Rh-
        // - Rh+ can donate only to Rh+

        return true; // Placeholder
    }

    /**
     * Get list of compatible blood types for a recipient.
     *
     * @param string $recipientBloodType Recipient blood type
     *
     * @return array List of compatible blood types
     */
    public static function getCompatibleDonorTypes(string $recipientBloodType): array
    {
        // TODO: Implement logic to return all compatible donor types
        // Example: For "AB+", return ["O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"]

        return [];
    }

    /**
     * Get list of blood types that a donor can give to.
     *
     * @param string $donorBloodType Donor blood type
     *
     * @return array List of recipient blood types that can receive this blood type
     */
    public static function getCompatibleRecipientTypes(string $donorBloodType): array
    {
        // TODO: Implement logic to return all compatible recipient types
        // Example: For "O+", return ["O+", "A+", "B+", "AB+"]

        return [];
    }

    /**
     * Check if blood type string is valid.
     *
     * @param string $bloodType Blood type to validate
     *
     * @return bool
     */
    public static function isValidBloodType(string $bloodType): bool
    {
        // TODO: Implement validation
        // Valid types: O+, O-, A+, A-, B+, B-, AB+, AB-

        return true; // Placeholder
    }

    /**
     * Parse blood type into ABO and Rh components.
     *
     * @param string $bloodType Blood type string
     *
     * @return array Array with 'abo' and 'rh' keys, or null if invalid
     */
    public static function parseBloodType(string $bloodType): ?array
    {
        // TODO: Implement parsing
        // Example: "AB+" -> ['abo' => 'AB', 'rh' => '+']

        return null; // Placeholder
    }
}
