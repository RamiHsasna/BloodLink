<?php

namespace App\Doctrine\Types;

use Doctrine\DBAL\Platforms\AbstractPlatform;
use Doctrine\DBAL\Types\Type;

class TextArrayType extends Type
{
    public const NAME = "_text";

    public function getSQLDeclaration(
        array $column,
        AbstractPlatform $platform,
    ): string {
        return "text[]";
    }

    public function convertToPHPValue(
        $value,
        AbstractPlatform $platform,
    ): ?array {
        if ($value === null) {
            return null;
        }

        if (is_array($value)) {
            return $value;
        }

        // Parse PostgreSQL array format: {item1,item2,item3}
        if (is_string($value)) {
            $value = trim($value, "{}");
            if ($value === "") {
                return [];
            }
            return explode(",", $value);
        }

        return null;
    }

    public function convertToDatabaseValue(
        $value,
        AbstractPlatform $platform,
    ): ?string {
        if ($value === null) {
            return null;
        }

        if (!is_array($value)) {
            $value = [$value];
        }

        if (empty($value)) {
            return "{}";
        }

        return "{" .
            implode(
                ",",
                array_map(static fn($item) => trim((string) $item), $value),
            ) .
            "}";
    }

    public function getName(): string
    {
        return self::NAME;
    }

    public function requiresSQLCommentHint(AbstractPlatform $platform): bool
    {
        return true;
    }
}
