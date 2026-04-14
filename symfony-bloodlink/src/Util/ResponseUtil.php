<?php

namespace App\Util;

/**
 * Utility class for standardized API response formatting.
 */
class ResponseUtil
{
    /**
     * Format a successful response.
     *
     * @param mixed $data
     * @param string $message
     *
     * @return array
     */
    public static function success($data = null, string $message = 'Success'): array
    {
        return [
            'success' => true,
            'message' => $message,
            'data' => $data,
        ];
    }

    /**
     * Format an error response.
     *
     * @param string $message
     * @param mixed $errors
     * @param int $statusCode
     *
     * @return array
     */
    public static function error(string $message = 'Error', $errors = null, int $statusCode = 400): array
    {
        return [
            'success' => false,
            'message' => $message,
            'errors' => $errors,
            'statusCode' => $statusCode,
        ];
    }

    /**
     * Format a paginated response.
     *
     * @param array $items
     * @param int $page
     * @param int $perPage
     * @param int $total
     *
     * @return array
     */
    public static function paginated(array $items, int $page, int $perPage, int $total): array
    {
        return [
            'success' => true,
            'data' => $items,
            'pagination' => [
                'currentPage' => $page,
                'perPage' => $perPage,
                'totalItems' => $total,
                'totalPages' => ceil($total / $perPage),
            ],
        ];
    }
}
