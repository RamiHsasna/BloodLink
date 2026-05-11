<?php

namespace App\Util;

/**
 * Utility class for blood type compatibility checking.
 * Implements ABO and Rh factor compatibility rules.
 */
class BloodCompatibilityUtil
{
    /**
     * Map of blood type IDs to compatible donor blood type IDs.
     * Key = recipient blood_type_id, Value = array of compatible donor blood_type_ids.
     *
     * Blood type IDs in the database (e.g., "O+", "AB-", etc.).
     */
    private static array $compatibilityMap = [
        'O-'  => ['O-'],
        'O+'  => ['O-', 'O+'],
        'A-'  => ['O-', 'A-'],
        'A+'  => ['O-', 'O+', 'A-', 'A+'],
        'B-'  => ['O-', 'B-'],
        'B+'  => ['O-', 'O+', 'B-', 'B+'],
        'AB-' => ['O-', 'A-', 'B-', 'AB-'],
        'AB+' => ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'],
    ];

    /**
     * Check if a donor's blood type can donate to a recipient's blood type.
     *
     * @param string $donorBloodType Donor blood type (e.g., "O+ ", "AB-")
     * @param string $recipientBloodType Recipient blood type
     *
     * @return bool True if donation is compatible
     */
    public static function isCompatible(string $donorBloodType, string $recipientBloodType): bool
    {
        $donorNormalized = self::normalizeBloodTypeId($donorBloodType);
        $recipientNormalized = self::normalizeBloodTypeId($recipientBloodType);

        $compatibleDonors = self::$compatibilityMap[$recipientNormalized] ?? [];

        return in_array($donorNormalized, $compatibleDonors, true);
    }

    /**
     * Get list of compatible donor blood type IDs for a recipient.
     *
     * @param string $recipientBloodType Recipient blood type (blood_type_id)
     *
     * @return array List of compatible donor blood_type_ids
     */
    public static function getCompatibleDonorTypes(string $recipientBloodType): array
    {
        $normalized = self::normalizeBloodTypeId($recipientBloodType);

        return self::$compatibilityMap[$normalized] ?? [];
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
        $donorNormalized = self::normalizeBloodTypeId($donorBloodType);
        $recipients = [];

        foreach (self::$compatibilityMap as $recipientType => $compatibleDonors) {
            if (in_array($donorNormalized, $compatibleDonors, true)) {
                $recipients[] = $recipientType;
            }
        }

        return $recipients;
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
        $normalized = self::normalizeBloodTypeId($bloodType);

        return isset(self::$compatibilityMap[$normalized]);
    }

    /**
     * Parse blood type into ABO and Rh components.
     *
     * @param string $bloodType Blood type string
     *
     * @return array|null Array with 'abo' and 'rh' keys, or null if invalid
     */
    public static function parseBloodType(string $bloodType): ?array
    {
        $trimmed = trim($bloodType);

        if (preg_match('/^(O|A|B|AB)([+-])$/', $trimmed, $matches)) {
            return [
                'abo' => $matches[1],
                'rh' => $matches[2],
            ];
        }

        return null;
    }

    /**
     * Normalize a blood type ID to match the 3-character database format.
     * The DB uses CHAR(3), so "O+" becomes "O+ " (padded with space).
     */
    public static function normalizeBloodTypeId(string $bloodTypeId): string
    {
        return trim($bloodTypeId);
    }
}
