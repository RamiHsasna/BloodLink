<?php

namespace App\Controller;

use App\Service\BloodTransferService;
use DateTimeImmutable;
use Doctrine\DBAL\Connection;
use Throwable;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class TransferDashboardController extends AbstractController
{
    #[
        Route(
            "/dashboard/transfers",
            name: "dashboard_transfers",
            methods: ["GET"],
        ),
    ]
    public function index(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType === "DONOR") {
            return $this->redirectToRoute("dashboard_donor_home");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        $isAdmin = $userType === "ADMIN";
        $currentHospital = null;

        if (!$isAdmin) {
            $currentHospital = $this->fetchStaffHospital(
                $connection,
                $currentUserId,
            );

            if (!$currentHospital) {
                $this->addFlash(
                    "error",
                    "Your hospital assignment is missing. Please contact an administrator.",
                );

                return $this->redirectToRoute("dashboard_users");
            }
        }

        $q = trim((string) $request->query->get("q", ""));
        $status = strtoupper(
            trim((string) $request->query->get("status", "ALL")),
        );
        $bloodType = strtoupper(
            trim((string) $request->query->get("blood_type", "")),
        );

        $transfers = $this->fetchTransfers(
            $connection,
            $currentHospital["id"] ?? null,
            $isAdmin,
            $q,
            $status,
            $bloodType,
        );

        $stats = $this->buildTransferStats($transfers);

        $hospitalOptions = $this->fetchHospitalOptions($connection);
        if (!$isAdmin && !empty($currentHospital["id"])) {
            $hospitalOptions = array_values(
                array_filter(
                    $hospitalOptions,
                    static fn(array $h): bool => $h["id"] !==
                        $currentHospital["id"],
                ),
            );
        }

        return $this->render("dashboard/transfers.html.twig", [
            "session_user" => $sessionUser,
            "user_type" => $userType,
            "current_user_id" => $currentUserId,
            "current_hospital" => $currentHospital,
            "transfers" => $transfers,
            "stats" => $stats,
            "q" => $q,
            "status" => $status,
            "blood_type" => $bloodType,
            "blood_types" => $this->fetchBloodTypes($connection),
            "hospital_options" => $hospitalOptions,
            "can_create" => $userType === "HOSPITAL_STAFF",
        ]);
    }

    #[
        Route(
            "/dashboard/transfers/create",
            name: "dashboard_transfers_create",
            methods: ["POST"],
        ),
    ]
    public function createTransfer(
        Request $request,
        Connection $connection,
        BloodTransferService $transferService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType !== "HOSPITAL_STAFF") {
            $this->addFlash(
                "error",
                "You are not allowed to create transfers.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        $currentHospital = $this->fetchStaffHospital(
            $connection,
            $currentUserId,
        );
        if (!$currentHospital) {
            $this->addFlash(
                "error",
                "Your hospital assignment is missing. Please contact an administrator.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $requestingHospitalId = (string) $currentHospital["id"];
        $approvingHospitalId = trim(
            (string) $request->request->get("approving_hospital_id", ""),
        );
        $bloodTypeId = trim(
            (string) $request->request->get("blood_type_id", ""),
        );
        $unitsRequested = (int) $request->request->get("units_requested", 0);
        $reason = trim((string) $request->request->get("reason", ""));
        $notes = trim((string) $request->request->get("notes", ""));
        $deliveryExpectedAt = trim(
            (string) $request->request->get("delivery_expected_at", ""),
        );

        if (
            $approvingHospitalId === "" ||
            $bloodTypeId === "" ||
            $unitsRequested <= 0 ||
            $reason === ""
        ) {
            $this->addFlash(
                "error",
                "Supplying hospital, blood type, quantity, and reason are required.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        if ($approvingHospitalId === $requestingHospitalId) {
            $this->addFlash(
                "error",
                "Supplying hospital must be different from the requesting hospital.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        try {
            $transferService->createTransferRequest([
                "requesting_hospital_id" => $requestingHospitalId,
                "approving_hospital_id" => $approvingHospitalId,
                "blood_type" => $bloodTypeId,
                "units_needed" => $unitsRequested,
                "requesting_staff_id" => $currentUserId,
                "reason" => $reason,
                "notes" => $notes !== "" ? $notes : null,
                "delivery_expected_at" =>
                    $deliveryExpectedAt !== "" ? $deliveryExpectedAt : null,
            ]);

            $this->addFlash(
                "success",
                "Transfer request created successfully.",
            );
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to create transfer request: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_transfers");
    }

    #[
        Route(
            "/dashboard/transfers/{transferId}/update",
            name: "dashboard_transfers_update",
            methods: ["POST"],
        ),
    ]
    public function updateTransfer(
        int $transferId,
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if (!in_array($userType, ["HOSPITAL_STAFF", "ADMIN"], true)) {
            $this->addFlash(
                "error",
                "You are not allowed to update transfers.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        if ($userType === "ADMIN") {
            $adminHospital = $this->fetchStaffHospital(
                $connection,
                $currentUserId,
            );
            if (!$adminHospital) {
                $this->addFlash(
                    "error",
                    "Admin users must be linked to a hospital staff profile to perform this action.",
                );

                return $this->redirectToRoute("dashboard_transfers");
            }
        }
        $transfer = $connection->fetchAssociative(
            "SELECT requesting_staff_id, requesting_hospital_id, status FROM blood_transfer_request WHERE transfer_id = ?",
            [$transferId],
        );

        if (!$transfer) {
            $this->addFlash("error", "Transfer not found.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        $status = strtoupper((string) ($transfer["status"] ?? "PENDING"));
        if ($status !== "PENDING") {
            $this->addFlash("error", "Only pending transfers can be updated.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        if (
            $userType !== "ADMIN" &&
            $transfer["requesting_staff_id"] !== $currentUserId
        ) {
            $this->addFlash(
                "error",
                "You can only update your own transfer requests.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $approvingHospitalId = trim(
            (string) $request->request->get("approving_hospital_id", ""),
        );
        $bloodTypeId = trim(
            (string) $request->request->get("blood_type_id", ""),
        );
        $unitsRequested = (int) $request->request->get("units_requested", 0);
        $reason = trim((string) $request->request->get("reason", ""));
        $notes = trim((string) $request->request->get("notes", ""));
        $deliveryExpectedAt = trim(
            (string) $request->request->get("delivery_expected_at", ""),
        );

        if (
            $approvingHospitalId === "" ||
            $bloodTypeId === "" ||
            $unitsRequested <= 0 ||
            $reason === ""
        ) {
            $this->addFlash(
                "error",
                "Supplying hospital, blood type, quantity, and reason are required.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        if (
            $approvingHospitalId ===
            (string) ($transfer["requesting_hospital_id"] ?? "")
        ) {
            $this->addFlash(
                "error",
                "Supplying hospital must be different from the requesting hospital.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        try {
            $connection->executeStatement(
                "UPDATE blood_transfer_request SET approving_hospital_id = ?::uuid, blood_type_id = ?, quantity_units_requested = ?, reason = ?, notes = ?, delivery_expected_at = ? WHERE transfer_id = ?",
                [
                    $approvingHospitalId,
                    $bloodTypeId,
                    $unitsRequested,
                    $reason,
                    $notes !== "" ? $notes : null,
                    $deliveryExpectedAt !== "" ? $deliveryExpectedAt : null,
                    $transferId,
                ],
            );

            $this->addFlash(
                "success",
                "Transfer request updated successfully.",
            );
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to update transfer request: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_transfers");
    }

    #[
        Route(
            "/dashboard/transfers/{transferId}/approve",
            name: "dashboard_transfers_approve",
            methods: ["POST"],
        ),
    ]
    public function approveTransfer(
        int $transferId,
        Request $request,
        Connection $connection,
        BloodTransferService $transferService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if (!in_array($userType, ["HOSPITAL_STAFF", "ADMIN"], true)) {
            $this->addFlash(
                "error",
                "You are not allowed to approve transfers.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        $transfer = $transferService->getTransferById($transferId);
        if (!$transfer) {
            $this->addFlash("error", "Transfer not found.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentHospital = $this->fetchStaffHospital(
            $connection,
            $currentUserId,
        );
        if (!$currentHospital) {
            $this->addFlash(
                "error",
                "Admin users must be linked to a hospital staff profile to perform this action.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        if ($userType !== "ADMIN") {
            $approvingHospital = $transfer->getApprovingHospital();
            if (
                $approvingHospital &&
                $approvingHospital->getHospitalId() !== $currentHospital["id"]
            ) {
                $this->addFlash(
                    "error",
                    "You can only approve transfers assigned to your hospital.",
                );

                return $this->redirectToRoute("dashboard_transfers");
            }
        }

        $qtyApprovedRaw = trim(
            (string) $request->request->get("quantity_approved", ""),
        );
        $qtyApproved = $qtyApprovedRaw !== "" ? (int) $qtyApprovedRaw : null;

        try {
            $transferService->approveTransfer(
                $transferId,
                $currentUserId,
                $qtyApproved,
            );
            $this->addFlash("success", "Transfer approved successfully.");
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to approve transfer: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_transfers");
    }

    #[
        Route(
            "/dashboard/transfers/{transferId}/deny",
            name: "dashboard_transfers_deny",
            methods: ["POST"],
        ),
    ]
    public function denyTransfer(
        int $transferId,
        Request $request,
        Connection $connection,
        BloodTransferService $transferService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if (!in_array($userType, ["HOSPITAL_STAFF", "ADMIN"], true)) {
            $this->addFlash("error", "You are not allowed to deny transfers.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        $transfer = $transferService->getTransferById($transferId);
        if (!$transfer) {
            $this->addFlash("error", "Transfer not found.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentHospital = $this->fetchStaffHospital(
            $connection,
            $currentUserId,
        );
        if (!$currentHospital) {
            $this->addFlash(
                "error",
                "Admin users must be linked to a hospital staff profile to perform this action.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        if ($userType !== "ADMIN") {
            $approvingHospital = $transfer->getApprovingHospital();
            if (
                $approvingHospital &&
                $approvingHospital->getHospitalId() !== $currentHospital["id"]
            ) {
                $this->addFlash(
                    "error",
                    "You can only deny transfers assigned to your hospital.",
                );

                return $this->redirectToRoute("dashboard_transfers");
            }
        }

        $reason = trim((string) $request->request->get("reason", ""));
        if ($reason === "") {
            $reason = "Denied by hospital.";
        }

        try {
            $transferService->rejectTransfer(
                $transferId,
                $currentUserId,
                $reason,
            );
            $this->addFlash("success", "Transfer denied successfully.");
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to deny transfer: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_transfers");
    }

    #[
        Route(
            "/dashboard/transfers/{transferId}/cancel",
            name: "dashboard_transfers_cancel",
            methods: ["POST"],
        ),
    ]
    public function cancelTransfer(
        int $transferId,
        Request $request,
        Connection $connection,
        BloodTransferService $transferService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if (!in_array($userType, ["HOSPITAL_STAFF", "ADMIN"], true)) {
            $this->addFlash(
                "error",
                "You are not allowed to cancel transfers.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        $transfer = $connection->fetchAssociative(
            "SELECT requesting_staff_id, requesting_hospital_id::text AS requesting_hospital_id, approving_hospital_id::text AS approving_hospital_id FROM blood_transfer_request WHERE transfer_id = ?",
            [$transferId],
        );

        if (!$transfer) {
            $this->addFlash("error", "Transfer not found.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        $cancelSource = "requester";
        if ($userType === "ADMIN") {
            $cancelSource = "admin";
        }

        if ($userType !== "ADMIN") {
            $currentHospital = $this->fetchStaffHospital(
                $connection,
                $currentUserId,
            );

            if (!$currentHospital) {
                $this->addFlash(
                    "error",
                    "Your hospital assignment is missing.",
                );

                return $this->redirectToRoute("dashboard_transfers");
            }

            $isRequester = $transfer["requesting_staff_id"] === $currentUserId;
            $isRequesterHospital =
                (string) ($transfer["requesting_hospital_id"] ?? "") ===
                $currentHospital["id"];
            $isApproverHospital =
                (string) ($transfer["approving_hospital_id"] ?? "") ===
                $currentHospital["id"];

            if (
                !$isRequester &&
                !$isRequesterHospital &&
                !$isApproverHospital
            ) {
                $this->addFlash(
                    "error",
                    "You can only cancel transfers linked to your hospital.",
                );

                return $this->redirectToRoute("dashboard_transfers");
            }

            if ($isApproverHospital && !$isRequesterHospital) {
                $cancelSource = "supplier";
            } else {
                $cancelSource = "requester";
            }
        }

        $reason = trim((string) $request->request->get("reason", ""));
        if ($reason === "") {
            if ($cancelSource === "supplier") {
                $reason = "Cancelled by supplier.";
            } elseif ($cancelSource === "admin") {
                $reason = "Cancelled by admin.";
            } else {
                $reason = "Cancelled by requester.";
            }
        }

        try {
            $transferService->cancelTransfer($transferId, $reason);
            $this->addFlash("success", "Transfer cancelled successfully.");
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to cancel transfer: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_transfers");
    }

    #[
        Route(
            "/dashboard/transfers/{transferId}/in-transit",
            name: "dashboard_transfers_in_transit",
            methods: ["POST"],
        ),
    ]
    public function markInTransit(
        int $transferId,
        Request $request,
        Connection $connection,
        BloodTransferService $transferService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if (!in_array($userType, ["HOSPITAL_STAFF", "ADMIN"], true)) {
            $this->addFlash(
                "error",
                "You are not allowed to update transfer status.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        $transfer = $transferService->getTransferById($transferId);
        if (!$transfer || !$transfer->getApprovingHospital()) {
            $this->addFlash("error", "Transfer not found.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentHospital = $this->fetchStaffHospital(
            $connection,
            $currentUserId,
        );
        if (!$currentHospital) {
            $this->addFlash(
                "error",
                "Admin users must be linked to a hospital staff profile to perform this action.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        if ($userType !== "ADMIN") {
            if (
                $transfer->getApprovingHospital()->getHospitalId() !==
                $currentHospital["id"]
            ) {
                $this->addFlash(
                    "error",
                    "Only the supplying hospital can mark this transfer in transit.",
                );

                return $this->redirectToRoute("dashboard_transfers");
            }
        }

        try {
            $transferService->markInTransit($transferId, $currentUserId);
            $this->addFlash("success", "Transfer marked in transit.");
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to update transfer: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_transfers");
    }

    #[
        Route(
            "/dashboard/transfers/{transferId}/deliver",
            name: "dashboard_transfers_deliver",
            methods: ["POST"],
        ),
    ]
    public function deliverTransfer(
        int $transferId,
        Request $request,
        Connection $connection,
        BloodTransferService $transferService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if (!in_array($userType, ["HOSPITAL_STAFF", "ADMIN"], true)) {
            $this->addFlash(
                "error",
                "You are not allowed to update transfer status.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentUserId = (string) ($sessionUser["id"] ?? "");
        $transfer = $transferService->getTransferById($transferId);
        if (!$transfer || !$transfer->getRequestingHospital()) {
            $this->addFlash("error", "Transfer not found.");

            return $this->redirectToRoute("dashboard_transfers");
        }

        $currentHospital = $this->fetchStaffHospital(
            $connection,
            $currentUserId,
        );
        if (!$currentHospital) {
            $this->addFlash(
                "error",
                "Admin users must be linked to a hospital staff profile to perform this action.",
            );

            return $this->redirectToRoute("dashboard_transfers");
        }

        if ($userType !== "ADMIN") {
            if (
                $transfer->getRequestingHospital()->getHospitalId() !==
                $currentHospital["id"]
            ) {
                $this->addFlash(
                    "error",
                    "Only the requesting hospital can mark this transfer delivered.",
                );

                return $this->redirectToRoute("dashboard_transfers");
            }
        }

        try {
            $transferService->completeTransfer($transferId);
            $this->addFlash("success", "Transfer marked delivered.");
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to deliver transfer: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_transfers");
    }

    /**
     * @return array{id:string, name:string}|null
     */
    private function fetchStaffHospital(
        Connection $connection,
        string $userId,
    ): ?array {
        try {
            $row = $connection->fetchAssociative(
                "SELECT hs.hospital_id::text AS id, h.name AS name FROM hospital_staff hs LEFT JOIN hospital h ON h.hospital_id = hs.hospital_id WHERE hs.user_id::text = ? LIMIT 1",
                [$userId],
            );

            if (!$row) {
                return null;
            }

            return [
                "id" => (string) ($row["id"] ?? ""),
                "name" => (string) ($row["name"] ?? "Unknown Hospital"),
            ];
        } catch (Throwable) {
            return null;
        }
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    private function fetchTransfers(
        Connection $connection,
        ?string $hospitalId,
        bool $isAdmin,
        string $q,
        string $status,
        string $bloodType,
    ): array {
        $sql =
            "SELECT tr.transfer_id, tr.status, tr.quantity_units_requested, tr.quantity_units_approved, tr.requested_at, tr.approved_at, tr.delivery_expected_at, tr.actual_delivery_at, tr.reason, tr.notes, tr.requesting_staff_id, tr.approving_staff_id, " .
            "rh.hospital_id::text AS requesting_hospital_id, rh.name AS requesting_hospital_name, " .
            "ah.hospital_id::text AS approving_hospital_id, ah.name AS approving_hospital_name, " .
            "bt.blood_type_id AS blood_type_id " .
            "FROM blood_transfer_request tr " .
            "LEFT JOIN hospital rh ON rh.hospital_id = tr.requesting_hospital_id " .
            "LEFT JOIN hospital ah ON ah.hospital_id = tr.approving_hospital_id " .
            "LEFT JOIN blood_type bt ON bt.blood_type_id = tr.blood_type_id " .
            "WHERE 1=1";

        $params = [];

        if (!$isAdmin && $hospitalId) {
            $sql .=
                " AND (tr.requesting_hospital_id::text = :hospitalId OR tr.approving_hospital_id::text = :hospitalId)";
            $params["hospitalId"] = $hospitalId;
        }

        if ($status !== "" && $status !== "ALL") {
            if ($status === "DELIVERED") {
                $sql .= " AND UPPER(tr.status) IN ('DELIVERED', 'COMPLETED')";
            } elseif ($status === "DENIED") {
                $sql .= " AND UPPER(tr.status) IN ('DENIED', 'REJECTED')";
            } else {
                $sql .= " AND UPPER(tr.status) = :status";
                $params["status"] = $status;
            }
        }

        if ($bloodType !== "") {
            $sql .= " AND bt.blood_type_id = :bloodType";
            $params["bloodType"] = $bloodType;
        }

        if ($q !== "") {
            $sql .=
                " AND (" .
                "LOWER(COALESCE(rh.name, '')) LIKE :q " .
                "OR LOWER(COALESCE(ah.name, '')) LIKE :q " .
                "OR LOWER(COALESCE(bt.blood_type_id, '')) LIKE :q " .
                "OR CAST(tr.transfer_id AS TEXT) LIKE :q" .
                ")";
            $params["q"] = "%" . mb_strtolower($q) . "%";
        }

        $sql .= " ORDER BY tr.requested_at DESC NULLS LAST";

        try {
            $rows = $connection
                ->executeQuery($sql, $params)
                ->fetchAllAssociative();

            return array_map(function (array $row): array {
                $status = $this->normalizeTransferStatus(
                    (string) ($row["status"] ?? "PENDING"),
                );

                $cancelSource = null;
                $cancelLabel = null;
                if ($status === "CANCELLED") {
                    $reason = strtolower((string) ($row["reason"] ?? ""));
                    if (stripos($reason, "supplier") !== false) {
                        $cancelSource = "supplier";
                        $cancelLabel = "Cancelled by supplier";
                    } elseif (stripos($reason, "admin") !== false) {
                        $cancelSource = "admin";
                        $cancelLabel = "Cancelled by admin";
                    } else {
                        $cancelSource = "requester";
                        $cancelLabel = "Cancelled by requester";
                    }
                }

                return [
                    "transfer_id" => (int) ($row["transfer_id"] ?? 0),
                    "status" => $status,
                    "quantity_units_requested" =>
                        (int) ($row["quantity_units_requested"] ?? 0),
                    "quantity_units_approved" =>
                        $row["quantity_units_approved"] !== null
                            ? (int) $row["quantity_units_approved"]
                            : null,
                    "requested_at" => $this->formatDate(
                        $row["requested_at"] ?? null,
                    ),
                    "approved_at" => $this->formatDate(
                        $row["approved_at"] ?? null,
                    ),
                    "delivery_expected_at" => $this->formatDate(
                        $row["delivery_expected_at"] ?? null,
                    ),
                    "actual_delivery_at" => $this->formatDate(
                        $row["actual_delivery_at"] ?? null,
                    ),
                    "reason" => (string) ($row["reason"] ?? ""),
                    "notes" => (string) ($row["notes"] ?? ""),
                    "cancel_source" => $cancelSource,
                    "cancel_label" => $cancelLabel,
                    "requesting_staff_id" =>
                        (string) ($row["requesting_staff_id"] ?? ""),
                    "approving_staff_id" =>
                        (string) ($row["approving_staff_id"] ?? ""),
                    "requesting_hospital_id" =>
                        (string) ($row["requesting_hospital_id"] ?? ""),
                    "requesting_hospital_name" =>
                        (string) ($row["requesting_hospital_name"] ??
                            "Unknown Hospital"),
                    "approving_hospital_id" =>
                        (string) ($row["approving_hospital_id"] ?? ""),
                    "approving_hospital_name" =>
                        (string) ($row["approving_hospital_name"] ??
                            "Unknown Hospital"),
                    "blood_type_id" => (string) ($row["blood_type_id"] ?? ""),
                ];
            }, $rows);
        } catch (Throwable) {
            return [];
        }
    }

    /**
     * @return array{total:int,pending:int,approved:int,in_transit:int,delivered:int,denied:int,cancelled:int}
     */
    private function buildTransferStats(array $transfers): array
    {
        $stats = [
            "total" => count($transfers),
            "pending" => 0,
            "approved" => 0,
            "in_transit" => 0,
            "delivered" => 0,
            "denied" => 0,
            "cancelled" => 0,
        ];

        foreach ($transfers as $transfer) {
            $status = strtoupper((string) ($transfer["status"] ?? "PENDING"));

            switch ($status) {
                case "APPROVED":
                    $stats["approved"]++;
                    break;
                case "IN_TRANSIT":
                    $stats["in_transit"]++;
                    break;
                case "DELIVERED":
                    $stats["delivered"]++;
                    break;
                case "DENIED":
                    $stats["denied"]++;
                    break;
                case "CANCELLED":
                    $stats["cancelled"]++;
                    break;
                default:
                    $stats["pending"]++;
                    break;
            }
        }

        return $stats;
    }

    private function normalizeTransferStatus(string $status): string
    {
        $status = strtoupper($status);
        if ($status === "REJECTED") {
            return "DENIED";
        }
        if ($status === "COMPLETED") {
            return "DELIVERED";
        }

        return $status !== "" ? $status : "PENDING";
    }

    private function formatDate(?string $value): ?string
    {
        if (!$value) {
            return null;
        }

        try {
            return (new DateTimeImmutable($value))->format("Y-m-d H:i");
        } catch (Throwable) {
            return $value;
        }
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    private function fetchBloodTypes(Connection $connection): array
    {
        try {
            $rows = $connection
                ->executeQuery(
                    "SELECT blood_type_id FROM blood_type ORDER BY blood_type_id ASC",
                )
                ->fetchFirstColumn();

            return array_map(static fn($v): string => (string) $v, $rows);
        } catch (Throwable) {
            return [];
        }
    }

    /**
     * @return array<int, array{id:string,name:string}>
     */
    private function fetchHospitalOptions(Connection $connection): array
    {
        try {
            $rows = $connection
                ->executeQuery(
                    "SELECT hospital_id::text AS id, name FROM hospital ORDER BY name ASC",
                )
                ->fetchAllAssociative();

            return array_map(static function (array $row): array {
                return [
                    "id" => (string) ($row["id"] ?? ""),
                    "name" => (string) ($row["name"] ?? "Unnamed Hospital"),
                ];
            }, $rows);
        } catch (Throwable) {
            return [];
        }
    }
}
