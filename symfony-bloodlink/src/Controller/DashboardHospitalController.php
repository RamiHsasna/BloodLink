<?php

namespace App\Controller;

use App\Form\HospitalType;
use App\Service\HospitalService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route("/admin/hospitals", name: "dashboard_hospitals_")]
class DashboardHospitalController extends AbstractController
{
    public function __construct(
        private readonly HospitalService $hospitalService,
    ) {}

    #[Route("", name: "index", methods: ["GET"])]
    public function index(Request $request): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType === "DONOR") {
            $this->addFlash("error", "You are not allowed to manage hospitals.");
            return $this->redirectToRoute("dashboard_users");
        }

        // Get all hospitals
        $hospitals = $this->hospitalService->getAllHospitals();

        // Get search and filter parameters
        $searchQuery = trim((string) $request->query->get("q", ""));
        $statusFilter = trim((string) $request->query->get("status", "all"));

        // Filter hospitals
        $filteredHospitals = $this->filterHospitals($hospitals, $searchQuery, $statusFilter);

        // Calculate statistics
        $stats = [
            "total" => count($hospitals),
            "active" => count(array_filter($hospitals, fn($h) => $h->getIsActive())),
            "inactive" => count(array_filter($hospitals, fn($h) => !$h->getIsActive())),
        ];

        return $this->render("dashboard/hospitals.html.twig", [
            "hospitals" => $filteredHospitals,
            "stats" => $stats,
            "search_query" => $searchQuery,
            "status_filter" => $statusFilter,
            "session_user" => $sessionUser,
        ]);
    }

    #[Route("/create", name: "create", methods: ["POST"])]
    public function create(Request $request): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType === "DONOR") {
            $this->addFlash("error", "You are not allowed to manage hospitals.");
            return $this->redirectToRoute("dashboard_hospitals_index");
        }

        // Extract form data
        $name = trim((string) $request->request->get("name", ""));
        $address = trim((string) $request->request->get("address", ""));
        $city = trim((string) $request->request->get("city", ""));
        $phone = trim((string) $request->request->get("phone", ""));
        $email = trim((string) $request->request->get("email", ""));
        $latitude = trim((string) $request->request->get("latitude", ""));
        $longitude = trim((string) $request->request->get("longitude", ""));
        $isActive = (bool) $request->request->get("isActive");

        if ($name === "") {
            $this->addFlash("error", "Hospital name is required.");
            return $this->redirectToRoute("dashboard_hospitals_index");
        }

        try {
            $hospitalData = [
                "name" => $name,
                "address" => $address ?: null,
                "city" => $city ?: null,
                "phone" => $phone ?: null,
                "email" => $email ?: null,
                "latitude" => $latitude ? (float) $latitude : null,
                "longitude" => $longitude ? (float) $longitude : null,
                "isActive" => $isActive,
            ];

            $this->hospitalService->createHospital($hospitalData);
            $this->addFlash("success", "Hospital created successfully.");
        } catch (\Exception $e) {
            $this->addFlash("error", "Error creating hospital: " . $e->getMessage());
        }

        return $this->redirectToRoute("dashboard_hospitals_index");
    }

    #[Route("/{hospitalId}/edit", name: "edit", methods: ["POST"])]
    public function edit(Request $request, string $hospitalId): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType === "DONOR") {
            $this->addFlash("error", "You are not allowed to manage hospitals.");
            return $this->redirectToRoute("dashboard_hospitals_index");
        }

        // Extract form data
        $name = trim((string) $request->request->get("name", ""));
        $address = trim((string) $request->request->get("address", ""));
        $city = trim((string) $request->request->get("city", ""));
        $phone = trim((string) $request->request->get("phone", ""));
        $email = trim((string) $request->request->get("email", ""));
        $latitude = trim((string) $request->request->get("latitude", ""));
        $longitude = trim((string) $request->request->get("longitude", ""));
        $isActive = (bool) $request->request->get("isActive");

        if ($name === "") {
            $this->addFlash("error", "Hospital name is required.");
            return $this->redirectToRoute("dashboard_hospitals_index");
        }

        try {
            $updateData = [
                "name" => $name,
                "address" => $address ?: null,
                "city" => $city ?: null,
                "phone" => $phone ?: null,
                "email" => $email ?: null,
                "latitude" => $latitude ? (float) $latitude : null,
                "longitude" => $longitude ? (float) $longitude : null,
                "isActive" => $isActive,
            ];

            $this->hospitalService->updateHospital($hospitalId, $updateData);
            $this->addFlash("success", "Hospital updated successfully.");
        } catch (\Exception $e) {
            $this->addFlash("error", "Error updating hospital: " . $e->getMessage());
        }

        return $this->redirectToRoute("dashboard_hospitals_index");
    }

    #[Route("/{hospitalId}/delete", name: "delete", methods: ["POST"])]
    public function delete(Request $request, string $hospitalId): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType === "DONOR") {
            $this->addFlash("error", "You are not allowed to manage hospitals.");
            return $this->redirectToRoute("dashboard_hospitals_index");
        }

        try {
            $this->hospitalService->deleteHospital($hospitalId);
            $this->addFlash("success", "Hospital deleted successfully.");
        } catch (\Exception $e) {
            $this->addFlash("error", "Error deleting hospital: " . $e->getMessage());
        }

        return $this->redirectToRoute("dashboard_hospitals_index");
    }

    #[Route("/bulk-delete", name: "bulk_delete", methods: ["POST"])]
    public function bulkDelete(Request $request): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType === "DONOR") {
            $this->addFlash("error", "You are not allowed to manage hospitals.");
            return $this->redirectToRoute("dashboard_hospitals_index");
        }

        $hospitalIds = $request->request->all("hospital_ids");
        $successCount = 0;
        $errorCount = 0;

        if (is_array($hospitalIds)) {
            foreach ($hospitalIds as $hospitalId) {
                try {
                    $this->hospitalService->deleteHospital($hospitalId);
                    $successCount++;
                } catch (\Exception $e) {
                    $errorCount++;
                }
            }
        }

        if ($successCount > 0) {
            $this->addFlash("success", "$successCount hospital(s) deleted successfully.");
        }
        if ($errorCount > 0) {
            $this->addFlash("error", "$errorCount hospital(s) could not be deleted.");
        }

        return $this->redirectToRoute("dashboard_hospitals_index");
    }

    #[Route("/{hospitalId}/toggle-status", name: "toggle_status", methods: ["POST"])]
    public function toggleStatus(Request $request, string $hospitalId): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType === "DONOR") {
            return $this->json(["error" => "Not authorized"], 403);
        }

        try {
            $hospital = $this->hospitalService->getHospitalById($hospitalId);
            if (!$hospital) {
                return $this->json(["error" => "Hospital not found"], 404);
            }

            $newStatus = !$hospital->getIsActive();
            $this->hospitalService->updateHospital($hospitalId, ["isActive" => $newStatus]);

            return $this->json([
                "success" => true,
                "new_status" => $newStatus,
                "message" => "Hospital status updated successfully.",
            ]);
        } catch (\Exception $e) {
            return $this->json(["error" => $e->getMessage()], 400);
        }
    }

    /**
     * Filter hospitals based on search query and status filter.
     *
     * @param array $hospitals
     * @param string $searchQuery
     * @param string $statusFilter
     *
     * @return array
     */
    private function filterHospitals(array $hospitals, string $searchQuery, string $statusFilter): array
    {
        return array_filter($hospitals, function ($hospital) use ($searchQuery, $statusFilter) {
            // Filter by status
            if ($statusFilter === "active" && !$hospital->getIsActive()) {
                return false;
            }
            if ($statusFilter === "inactive" && $hospital->getIsActive()) {
                return false;
            }

            // Filter by search query
            if ($searchQuery !== "") {
                $searchLower = strtolower($searchQuery);
                $name = strtolower($hospital->getName() ?? "");
                $city = strtolower($hospital->getCity() ?? "");
                $email = strtolower($hospital->getEmail() ?? "");
                $address = strtolower($hospital->getAddress() ?? "");

                if (
                    strpos($name, $searchLower) === false &&
                    strpos($city, $searchLower) === false &&
                    strpos($email, $searchLower) === false &&
                    strpos($address, $searchLower) === false
                ) {
                    return false;
                }
            }

            return true;
        });
    }
}
