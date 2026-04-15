<?php

namespace App\Controller\Dev;

use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Dev-only diagnostics: compare CLI vs web SAPI (Apache often uses different settings than `php` in terminal).
 */
final class DevPhpController
{
    #[Route('/_dev/php-extensions', name: 'dev_php_extensions', methods: ['GET'])]
    public function __invoke(): Response
    {
        $drivers = class_exists(\PDO::class) ? \PDO::getAvailableDrivers() : [];

        $data = [
            'sapi' => \PHP_SAPI,
            'php_binary' => \PHP_BINARY ?: '(empty — use phpinfo if needed)',
            'php_version' => \PHP_VERSION,
            'loaded_ini' => \php_ini_loaded_file() ?: '(none)',
            'pdo_drivers' => $drivers,
            'pdo_pgsql_loaded' => \extension_loaded('pdo_pgsql'),
            'hint' => 'If `pgsql` is missing here but works in terminal, enable extension=pdo_pgsql in the php.ini Apache uses, then restart Apache. Or run: composer run dev:serve',
        ];

        return new JsonResponse($data, 200, [], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
    }
}
