<?php

namespace App\Controller;

use App\Service\GeocodingService;
use DateTimeImmutable;
use Doctrine\DBAL\Connection;
use Throwable;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class DashboardController extends AbstractController
{
    #[Route("/dashboard/donor", name: "dashboard_donor_home", methods: ["GET"])]
    public function donorHome(
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType !== "DONOR") {
            return $this->redirectToRoute("dashboard_users");
        }

        $userId = (string) ($sessionUser["id"] ?? "");
        $records = $this->fetchDonorOwnRecord($connection, $userId);
        $ownRecord = $records[0] ?? null;
        $donorSummary = $this->buildDonorSummary(
            $connection,
            $userId,
            $ownRecord,
        );

        return $this->render("dashboard/donor_home.html.twig", [
            "session_user" => $sessionUser,
            "own_record" => $ownRecord,
            "donor_summary" => $donorSummary,
        ]);
    }

    #[Route('/dashboard/donor/articles', name: 'dashboard_donor_articles', methods: ['GET'])]
    public function donorArticles(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType !== 'DONOR') {
            return $this->redirectToRoute('dashboard_users');
        }

        $userId = (string) ($sessionUser['id'] ?? '');
        $records = $this->fetchDonorOwnRecord($connection, $userId);
        $ownRecord = $records[0] ?? null;
        $donorSummary = $this->buildDonorSummary($connection, $userId, $ownRecord);

        $articles = [
            [
                'title' => 'How donated blood reaches critical patients in under 24 hours',
                'excerpt' => 'From blood draw to screening and secure transport, this explains the complete fast-track process used by partner hospitals.',
                'category' => 'Operations',
                'read_time' => '5 min read',
                'published_at' => 'April 15, 2026',
                'author' => 'BloodLink Editorial Team',
            ],
            [
                'title' => 'First donation checklist: what to do the night before',
                'excerpt' => 'A practical checklist covering hydration, sleep, food choices, and documents to bring on donation day.',
                'category' => 'Beginner Guide',
                'read_time' => '4 min read',
                'published_at' => 'April 11, 2026',
                'author' => 'Donor Success Unit',
            ],
            [
                'title' => 'Seasonal shortages explained and how recurring donors help',
                'excerpt' => 'Why blood demand can spike quickly and how regular donation cycles stabilize emergency care capacity.',
                'category' => 'Impact',
                'read_time' => '6 min read',
                'published_at' => 'April 8, 2026',
                'author' => 'Regional Coordination Desk',
            ],
            [
                'title' => 'Understanding eligibility pauses after travel, vaccines, or procedures',
                'excerpt' => 'A plain-language overview of common temporary deferrals and how to plan your next donation date.',
                'category' => 'Eligibility',
                'read_time' => '5 min read',
                'published_at' => 'April 4, 2026',
                'author' => 'Clinical Policy Team',
            ],
        ];

        return $this->render('dashboard/donor_articles.html.twig', [
            'session_user' => $sessionUser,
            'donor_summary' => $donorSummary,
            'articles' => $articles,
        ]);
    }

    #[Route('/dashboard/donor/tips', name: 'dashboard_donor_tips', methods: ['GET'])]
    public function donorTips(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get('auth_user');
        if (!$sessionUser) {
            return $this->redirectToRoute('auth_index');
        }

        $userType = strtoupper((string) ($sessionUser['user_type'] ?? ''));
        if ($userType !== 'DONOR') {
            return $this->redirectToRoute('dashboard_users');
        }

        $userId = (string) ($sessionUser['id'] ?? '');
        $records = $this->fetchDonorOwnRecord($connection, $userId);
        $ownRecord = $records[0] ?? null;
        $donorSummary = $this->buildDonorSummary($connection, $userId, $ownRecord);

        $tipGroups = [
            [
                'title' => 'Before Donation',
                'tips' => [
                    'Drink extra water in the 24 hours before your appointment.',
                    'Choose a balanced meal with iron and vitamin C 2 to 3 hours before donating.',
                    'Avoid donating on an empty stomach to reduce lightheadedness.',
                ],
            ],
            [
                'title' => 'After Donation',
                'tips' => [
                    'Keep the bandage on for at least 4 hours and avoid heavy lifting with that arm.',
                    'Take a short rest and continue hydrating during the day.',
                    'Postpone intense workouts for 24 hours to help recovery.',
                ],
            ],
            [
                'title' => 'Long-Term Donor Health',
                'tips' => [
                    'Track your last donation date to plan your next eligible session.',
                    'Include iron-rich foods weekly: lentils, spinach, beans, and lean protein.',
                    'Report any health changes before donating so staff can advise safely.',
                ],
            ],
        ];

        return $this->render('dashboard/donor_tips.html.twig', [
            'session_user' => $sessionUser,
            'donor_summary' => $donorSummary,
            'tip_groups' => $tipGroups,
        ]);
    }

    #[Route("/dashboard/users", name: "dashboard_users", methods: ["GET"])]
    public function users(Request $request, Connection $connection): Response
    {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $q = trim((string) $request->query->get("q", ""));
        $type = strtoupper(trim((string) $request->query->get("type", "ALL")));
        if (!in_array($type, ["ALL", "DONOR", "HOSPITAL_STAFF"], true)) {
            $type = "ALL";
        }

        $users = $this->fetchUsers($connection, $q, $type);
        $stats = $this->fetchUserStats($connection);

        return $this->render("dashboard/users.html.twig", [
            "session_user" => $sessionUser,
            "users" => $users,
            "q" => $q,
            "type" => $type,
            "stats" => $stats,
            "blood_types" => $this->fetchBloodTypes($connection),
            "hospital_options" => $this->fetchHospitalOptions($connection),
        ]);
    }

    #[
        Route(
            "/dashboard/users/create",
            name: "dashboard_users_create",
            methods: ["POST"],
        ),
    ]
    public function createUser(
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        if (
            strtoupper((string) ($sessionUser["user_type"] ?? "")) === "DONOR"
        ) {
            $this->addFlash("error", "You are not allowed to manage users.");

            return $this->redirectToRoute("dashboard_users");
        }

        $email = $this->normalizeEmail(
            (string) $request->request->get("email", ""),
        );
        $firstName = trim((string) $request->request->get("first_name", ""));
        $lastName = trim((string) $request->request->get("last_name", ""));
        $phone = trim((string) $request->request->get("phone", ""));
        $userType = strtoupper(
            trim((string) $request->request->get("user_type", "DONOR")),
        );
        $password = (string) $request->request->get("password", "");
        $bloodTypeId = trim(
            (string) $request->request->get("blood_type_id", ""),
        );
        $lastDonationDate = trim(
            (string) $request->request->get("last_donation_date", ""),
        );
        $donorCity = trim((string) $request->request->get("donor_city", ""));
        $totalDonations = trim(
            (string) $request->request->get("total_donations", ""),
        );
        $staffRole = trim((string) $request->request->get("staff_role", ""));
        $hospitalId = trim((string) $request->request->get("hospital_id", ""));
        $department = trim((string) $request->request->get("department", ""));

        if (!in_array($userType, ["DONOR", "HOSPITAL_STAFF"], true)) {
            $userType = "DONOR";
        }

        if (
            $email === "" ||
            $firstName === "" ||
            $lastName === "" ||
            $password === ""
        ) {
            $this->addFlash(
                "error",
                "Email, first name, last name and password are required.",
            );

            return $this->redirectToRoute("dashboard_users");
        }

        if (!$this->isValidEmailAddress($email)) {
            $this->addFlash(
                "error",
                "Please enter a valid email address (example: name@example.com).",
            );

            return $this->redirectToRoute("dashboard_users");
        }

        if ($userType === "DONOR" && $bloodTypeId === "") {
            $this->addFlash("error", "Blood type is required for donor users.");

            return $this->redirectToRoute("dashboard_users");
        }

        if (
            $userType === "HOSPITAL_STAFF" &&
            ($staffRole === "" || $hospitalId === "")
        ) {
            $this->addFlash(
                "error",
                "Role and hospital are required for hospital staff users.",
            );

            return $this->redirectToRoute("dashboard_users");
        }

        $userId = $this->generateUuidV4();

        try {
            $connection->beginTransaction();

            $connection->executeStatement(
                "INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone, user_type, created_at) VALUES (?::uuid, ?, ?, ?, ?, ?, ?, NOW())",
                [
                    $userId,
                    $email,
                    $password,
                    $firstName,
                    $lastName,
                    $phone !== "" ? $phone : null,
                    $userType,
                ],
            );

            if ($userType === "DONOR") {
                $this->upsertDonorProfile(
                    $connection,
                    $userId,
                    $firstName,
                    $lastName,
                    $bloodTypeId,
                    $donorCity !== "" ? $donorCity : null,
                    $lastDonationDate !== "" ? $lastDonationDate : null,
                    $totalDonations !== "" ? (int) $totalDonations : 0,
                );
            }

            if ($userType === "HOSPITAL_STAFF") {
                $this->upsertHospitalStaffProfile(
                    $connection,
                    $userId,
                    $firstName,
                    $lastName,
                    $staffRole,
                    $hospitalId,
                    $department !== "" ? $department : null,
                );
            }

            $connection->commit();
            $this->addFlash("success", "User created successfully.");
        } catch (Throwable $e) {
            if ($connection->isTransactionActive()) {
                $connection->rollBack();
            }
            $this->addFlash(
                "error",
                "Unable to create user: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_users");
    }

    #[
        Route(
            "/dashboard/users/{userId}/update",
            name: "dashboard_users_update",
            methods: ["POST"],
        ),
    ]
    public function updateUser(
        string $userId,
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        if (
            strtoupper((string) ($sessionUser["user_type"] ?? "")) === "DONOR"
        ) {
            $this->addFlash("error", "You are not allowed to manage users.");

            return $this->redirectToRoute("dashboard_users");
        }

        $email = $this->normalizeEmail(
            (string) $request->request->get("email", ""),
        );
        $firstName = trim((string) $request->request->get("first_name", ""));
        $lastName = trim((string) $request->request->get("last_name", ""));
        $phone = trim((string) $request->request->get("phone", ""));
        $userType = strtoupper(
            trim((string) $request->request->get("user_type", "DONOR")),
        );
        $bloodTypeId = trim(
            (string) $request->request->get("blood_type_id", ""),
        );
        $lastDonationDate = trim(
            (string) $request->request->get("last_donation_date", ""),
        );
        $donorCity = trim((string) $request->request->get("donor_city", ""));
        $totalDonations = trim(
            (string) $request->request->get("total_donations", ""),
        );
        $staffRole = trim((string) $request->request->get("staff_role", ""));
        $hospitalId = trim((string) $request->request->get("hospital_id", ""));
        $department = trim((string) $request->request->get("department", ""));

        if (!in_array($userType, ["DONOR", "HOSPITAL_STAFF"], true)) {
            $userType = "DONOR";
        }

        if ($email === "" || $firstName === "" || $lastName === "") {
            $this->addFlash(
                "error",
                "Email, first name, and last name are required.",
            );

            return $this->redirectToRoute("dashboard_users");
        }

        if (!$this->isValidEmailAddress($email)) {
            $this->addFlash(
                "error",
                "Please enter a valid email address (example: name@example.com).",
            );

            return $this->redirectToRoute("dashboard_users");
        }

        if ($userType === "DONOR" && $bloodTypeId === "") {
            $this->addFlash("error", "Blood type is required for donor users.");

            return $this->redirectToRoute("dashboard_users");
        }

        if (
            $userType === "HOSPITAL_STAFF" &&
            ($staffRole === "" || $hospitalId === "")
        ) {
            $this->addFlash(
                "error",
                "Role and hospital are required for hospital staff users.",
            );

            return $this->redirectToRoute("dashboard_users");
        }

        try {
            $connection->beginTransaction();

            $connection->executeStatement(
                "UPDATE users SET email = ?, first_name = ?, last_name = ?, phone = ?, user_type = ? WHERE user_id::text = ?",
                [
                    $email,
                    $firstName,
                    $lastName,
                    $phone !== "" ? $phone : null,
                    $userType,
                    $userId,
                ],
            );

            if ($userType === "DONOR") {
                $this->upsertDonorProfile(
                    $connection,
                    $userId,
                    $firstName,
                    $lastName,
                    $bloodTypeId,
                    $donorCity !== "" ? $donorCity : null,
                    $lastDonationDate !== "" ? $lastDonationDate : null,
                    $totalDonations !== "" ? (int) $totalDonations : 0,
                );
            }

            if ($userType === "HOSPITAL_STAFF") {
                $this->upsertHospitalStaffProfile(
                    $connection,
                    $userId,
                    $firstName,
                    $lastName,
                    $staffRole,
                    $hospitalId,
                    $department !== "" ? $department : null,
                );
            }

            $connection->commit();
            $this->addFlash("success", "User updated successfully.");
        } catch (Throwable $e) {
            if ($connection->isTransactionActive()) {
                $connection->rollBack();
            }
            $this->addFlash(
                "error",
                "Unable to update user: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_users");
    }

    #[
        Route(
            "/dashboard/users/{userId}/delete",
            name: "dashboard_users_delete",
            methods: ["POST"],
        ),
    ]
    public function deleteUser(
        string $userId,
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        if (
            strtoupper((string) ($sessionUser["user_type"] ?? "")) === "DONOR"
        ) {
            $this->addFlash("error", "You are not allowed to manage users.");

            return $this->redirectToRoute("dashboard_users");
        }

        try {
            $connection->executeStatement(
                "DELETE FROM users WHERE user_id::text = ?",
                [$userId],
            );
            $this->addFlash("success", "User deleted successfully.");
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to delete user: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_users");
    }

    #[
        Route(
            "/dashboard/inventory",
            name: "dashboard_blood_inventory",
            methods: ["GET"],
        ),
    ]
    public function bloodInventory(
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if (!in_array($userType, ["HOSPITAL_STAFF", "ADMIN"], true)) {
            if ($userType === "DONOR") {
                return $this->redirectToRoute("dashboard_donor_home");
            }
            return $this->redirectToRoute("dashboard_users");
        }

        $userId = (string) ($sessionUser["id"] ?? "");
        $selectedHospitalId = null;
        $hospitals = [];
        $currentHospital = null;

        // For HOSPITAL_STAFF: get their assigned hospital
        if ($userType === "HOSPITAL_STAFF") {
            try {
                $staffRecord = $connection->fetchAssociative(
                    "SELECT hospital_id FROM hospital_staff WHERE user_id::text = ? LIMIT 1",
                    [$userId],
                );
                if ($staffRecord) {
                    $selectedHospitalId =
                        (string) ($staffRecord["hospital_id"] ?? "");
                }
            } catch (Throwable) {
                // Staff has no hospital assignment
            }
        } else {
            // For ADMIN: check query param or use first hospital
            $requestedHospital = trim(
                (string) $request->query->get("hospital_id", ""),
            );
            if ($requestedHospital !== "") {
                $selectedHospitalId = $requestedHospital;
            }

            // Fetch all hospitals for dropdown
            try {
                $hospitalRows = $connection
                    ->executeQuery(
                        "SELECT hospital_id::text AS id, name FROM hospital ORDER BY name ASC",
                    )
                    ->fetchAllAssociative();

                $hospitals = array_map(static function (array $row): array {
                    return [
                        "id" => (string) ($row["id"] ?? ""),
                        "name" => (string) ($row["name"] ?? "Unnamed Hospital"),
                    ];
                }, $hospitalRows);

                // Default to first hospital if not selected
                if ($selectedHospitalId === null && !empty($hospitals)) {
                    $selectedHospitalId = $hospitals[0]["id"];
                }
            } catch (Throwable) {
                $hospitals = [];
            }
        }

        // Fetch current hospital details
        if ($selectedHospitalId !== null) {
            try {
                $hospitalRecord = $connection->fetchAssociative(
                    "SELECT hospital_id::text AS id, name FROM hospital WHERE hospital_id::text = ? LIMIT 1",
                    [$selectedHospitalId],
                );
                if ($hospitalRecord) {
                    $currentHospital = [
                        "id" => (string) ($hospitalRecord["id"] ?? ""),
                        "name" =>
                        (string) ($hospitalRecord["name"] ??
                            "Unknown Hospital"),
                    ];
                }
            } catch (Throwable) {
                $currentHospital = null;
            }
        }

        // Fetch all blood types
        $allBloodTypes = [];
        try {
            $bloodTypeRows = $connection
                ->executeQuery(
                    "SELECT blood_type_id FROM blood_type ORDER BY blood_type_id ASC",
                )
                ->fetchFirstColumn();

            $allBloodTypes = array_map(
                static fn(string $bt): string => (string) $bt,
                $bloodTypeRows,
            );
        } catch (Throwable) {
            $allBloodTypes = ["O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"];
        }

        // Fetch inventory for selected hospital
        $inventoryCards = [];
        if ($selectedHospitalId !== null) {
            try {
                $inventoryRows = $connection
                    ->executeQuery(
                        "SELECT blood_type_id, quantity_units, status, updated_at FROM blood_inventory WHERE hospital_id::text = ? ORDER BY blood_type_id ASC",
                        [$selectedHospitalId],
                    )
                    ->fetchAllAssociative();

                $inventoryMap = [];
                foreach ($inventoryRows as $row) {
                    $inventoryMap[(string) ($row["blood_type_id"] ?? "")] = $row;
                }

                // Build cards for all blood types
                foreach ($allBloodTypes as $bloodType) {
                    $inv = $inventoryMap[$bloodType] ?? null;
                    $units = (int) ($inv["quantity_units"] ?? 0);

                    // Determine status
                    if ($inv) {
                        $status = (string) ($inv["status"] ?? "CRITICAL");
                    } else {
                        $status =
                            $units <= 5
                            ? "CRITICAL"
                            : ($units <= 10
                                ? "LOW"
                                : "OPTIMAL");
                    }

                    $updatedAt =
                        $inv && !empty($inv["updated_at"])
                        ? (new DateTimeImmutable(
                            (string)$inv["updated_at"],
                        ))->format("Y-m-d H:i")
                        : "N/A";

                    $inventoryCards[] = [
                        "blood_type" => $bloodType,
                        "units" => $units,
                        "status" => $status,
                        "last_updated" => $updatedAt,
                    ];
                }
            } catch (Throwable) {
                // Fallback: create empty cards
                foreach ($allBloodTypes as $bloodType) {
                    $inventoryCards[] = [
                        "blood_type" => $bloodType,
                        "units" => 0,
                        "status" => "CRITICAL",
                        "last_updated" => "N/A",
                    ];
                }
            }
        }

        // Calculate stats
        $stats = [
            "critical" => count(
                array_filter(
                    $inventoryCards,
                    static fn(array $c): bool => $c["status"] === "CRITICAL",
                ),
            ),
            "low" => count(
                array_filter(
                    $inventoryCards,
                    static fn(array $c): bool => $c["status"] === "LOW",
                ),
            ),
            "optimal" => count(
                array_filter(
                    $inventoryCards,
                    static fn(array $c): bool => $c["status"] === "OPTIMAL",
                ),
            ),
            "total_units" => array_sum(
                array_map(
                    static fn(array $c): int => (int) ($c["units"] ?? 0),
                    $inventoryCards,
                ),
            ),
        ];

        return $this->render("dashboard/blood_inventory.html.twig", [
            "session_user" => $sessionUser,
            "user_type" => $userType,
            "current_hospital" => $currentHospital,
            "hospitals" => $hospitals,
            "inventory_cards" => $inventoryCards,
            "inventory_stats" => $stats,
        ]);
    }

    #[
        Route(
            "/dashboard/donor-eligibility",
            name: "dashboard_donor_eligibility",
            methods: ["GET"],
        ),
    ]
    public function donorEligibility(
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        $donorMode = $userType === "DONOR";

        $q = trim((string) $request->query->get("q", ""));
        $status = strtolower(
            trim((string) $request->query->get("status", "all")),
        );
        if (!in_array($status, ["all", "eligible", "not_eligible"], true)) {
            $status = "all";
        }

        $records = $donorMode
            ? $this->fetchDonorOwnRecord(
                $connection,
                (string) ($sessionUser["id"] ?? ""),
            )
            : $this->fetchEligibilityRecords($connection, $q, $status);

        $stats = $donorMode
            ? [
                "total" => count($records),
                "eligible" => count(
                    array_filter(
                        $records,
                        static fn(array $r): bool => (bool) ($r["is_currently_eligible"] ?? false),
                    ),
                ),
                "not_eligible" => count(
                    array_filter(
                        $records,
                        static fn(array $r): bool => !((bool) ($r["is_currently_eligible"] ?? false)),
                    ),
                ),
            ]
            : $this->fetchEligibilityStats($connection);

        $donorSummary = $donorMode
            ? $this->buildDonorSummary(
                $connection,
                (string) ($sessionUser["id"] ?? ""),
                $records[0] ?? null,
            )
            : null;

        return $this->render("dashboard/donor_eligibility.html.twig", [
            "session_user" => $sessionUser,
            "donor_mode" => $donorMode,
            "records" => $records,
            "q" => $q,
            "status" => $status,
            "stats" => $stats,
            "donor_summary" => $donorSummary,
        ]);
    }

    #[
        Route(
            "/dashboard/donor-eligibility/create",
            name: "dashboard_donor_eligibility_create",
            methods: ["POST"],
        ),
    ]
    public function createDonorEligibility(
        Request $request,
        Connection $connection,
        GeocodingService $geocodingService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $sessionUserType = strtoupper(
            (string) ($sessionUser["user_type"] ?? ""),
        );
        if ($sessionUserType === "DONOR") {
            $sessionUserId = (string) ($sessionUser["id"] ?? "");
            if ($sessionUserId === "") {
                $this->addFlash("error", "No active donor session found.");

                return $this->redirectToRoute("dashboard_donor_eligibility");
            }

            $calculation = $this->calculateDonorEligibilityFromRequest(
                $request,
                $connection,
                $sessionUserId,
            );
            if (($calculation["ok"] ?? false) !== true) {
                $this->addFlash(
                    "error",
                    (string) ($calculation["error"] ??
                        "Unable to calculate eligibility."),
                );

                return $this->redirectToRoute("dashboard_donor_eligibility");
            }

            try {
                $this->upsertEligibilityRecord(
                    $connection,
                    $geocodingService,
                    $sessionUserId,
                    (string) ($calculation["blood_type_cache"] ?? "N/A"),
                    (bool) ($calculation["is_currently_eligible"] ?? false),
                    (int) ($calculation["days_until_eligible"] ?? 0),
                    (string) ($calculation["city"] ?? ""),
                    (string) ($calculation["eligibility_details"] ?? ""),
                    (new DateTimeImmutable("today"))->format("Y-m-d"),
                    (string) ($calculation["last_donation_date_formatted"] ?? ""),
                );
                $this->addFlash(
                    "success",
                    "Eligibility check saved successfully.",
                );
            } catch (Throwable $e) {
                $this->addFlash(
                    "error",
                    "Unable to save eligibility check: " . $e->getMessage(),
                );
            }

            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        $userId = trim((string) $request->request->get("user_id", ""));
        $bloodType = trim(
            (string) $request->request->get("blood_type_cache", ""),
        );
        $eligible =
            (string) $request->request->get("is_currently_eligible", "1") ===
            "1";
        $daysUntil = trim(
            (string) $request->request->get("days_until_eligible", ""),
        );
        $lastCalculated = trim(
            (string) $request->request->get("last_calculated_at", ""),
        );
        $city = trim((string) $request->request->get("city", ""));
        $details = trim(
            (string) $request->request->get("eligibility_details", ""),
        );

        if ($userId === "") {
            $this->addFlash("error", "Donor user ID is required.");

            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        [
            $isValidManual,
            $manualError,
            $parsedDaysUntil,
        ] = $this->validateManualEligibilityInput(
            $bloodType,
            $eligible,
            $daysUntil,
        );
        if (!$isValidManual) {
            $this->addFlash("error", $manualError);

            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        if ($lastCalculated === "") {
            $lastCalculated = (new DateTimeImmutable("today"))->format("Y-m-d");
        }

        [$lat, $lng] = $this->resolveCoordinatesFromCity(
            $geocodingService,
            $city,
        );
        if ($city !== "" && ($lat === null || $lng === null)) {
            $this->addFlash(
                "error",
                "City was saved, but coordinates could not be resolved from Nominatim for this value.",
            );
        }

        try {
            $connection->executeStatement(
                "INSERT INTO donor_eligibility (user_id, blood_type_cache, is_currently_eligible, days_until_eligible, last_calculated_at, latitude_cache, longitude_cache, eligibility_details) VALUES (?::uuid, ?, ?::boolean, ?, ?, ?, ?, ?)",
                [
                    $userId,
                    $bloodType !== "" ? $bloodType : null,
                    $eligible ? "true" : "false",
                    $parsedDaysUntil,
                    $lastCalculated !== "" ? $lastCalculated : null,
                    $lat,
                    $lng,
                    $details !== "" ? $details : null,
                ],
            );

            $this->syncDonorCity(
                $connection,
                $userId,
                $city !== "" ? $city : null,
            );
            $this->addFlash(
                "success",
                "Eligibility record created successfully.",
            );
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to create eligibility record: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_donor_eligibility");
    }

    #[
        Route(
            "/dashboard/donor-eligibility/{userId}/update",
            name: "dashboard_donor_eligibility_update",
            methods: ["POST"],
        ),
    ]
    public function updateDonorEligibility(
        string $userId,
        Request $request,
        Connection $connection,
        GeocodingService $geocodingService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $sessionUserType = strtoupper(
            (string) ($sessionUser["user_type"] ?? ""),
        );
        $sessionUserId = (string) ($sessionUser["id"] ?? "");

        if ($sessionUserType === "DONOR" && $sessionUserId !== $userId) {
            $this->addFlash(
                "error",
                "You can only edit your own eligibility record.",
            );

            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        if ($sessionUserType === "DONOR") {
            $calculation = $this->calculateDonorEligibilityFromRequest(
                $request,
                $connection,
                $userId,
            );
            if (($calculation["ok"] ?? false) !== true) {
                $this->addFlash(
                    "error",
                    (string) ($calculation["error"] ??
                        "Unable to calculate eligibility."),
                );

                return $this->redirectToRoute("dashboard_donor_eligibility");
            }

            try {
                $this->upsertEligibilityRecord(
                    $connection,
                    $geocodingService,
                    $userId,
                    (string) ($calculation["blood_type_cache"] ?? "N/A"),
                    (bool) ($calculation["is_currently_eligible"] ?? false),
                    (int) ($calculation["days_until_eligible"] ?? 0),
                    (string) ($calculation["city"] ?? ""),
                    (string) ($calculation["eligibility_details"] ?? ""),
                    (new DateTimeImmutable("today"))->format("Y-m-d"),
                    (string) ($calculation["last_donation_date_formatted"] ?? ""),
                );
                $this->addFlash(
                    "success",
                    "Eligibility check saved successfully.",
                );
            } catch (Throwable $e) {
                $this->addFlash(
                    "error",
                    "Unable to save eligibility check: " . $e->getMessage(),
                );
            }

            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        $bloodType = trim(
            (string) $request->request->get("blood_type_cache", ""),
        );
        $eligible =
            (string) $request->request->get("is_currently_eligible", "1") ===
            "1";
        $daysUntil = trim(
            (string) $request->request->get("days_until_eligible", ""),
        );
        $lastCalculated = trim(
            (string) $request->request->get("last_calculated_at", ""),
        );
        $city = trim((string) $request->request->get("city", ""));
        $details = trim(
            (string) $request->request->get("eligibility_details", ""),
        );

        [
            $isValidManual,
            $manualError,
            $parsedDaysUntil,
        ] = $this->validateManualEligibilityInput(
            $bloodType,
            $eligible,
            $daysUntil,
        );
        if (!$isValidManual) {
            $this->addFlash("error", $manualError);

            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        if ($lastCalculated === "") {
            $lastCalculated = (new DateTimeImmutable("today"))->format("Y-m-d");
        }

        $existing =
            $connection->fetchAssociative(
                "SELECT latitude_cache, longitude_cache FROM donor_eligibility WHERE user_id::text = ? LIMIT 1",
                [$userId],
            ) ?:
            [];

        $lat = !empty($existing["latitude_cache"])
            ? (string) $existing["latitude_cache"]
            : null;
        $lng = !empty($existing["longitude_cache"])
            ? (string) $existing["longitude_cache"]
            : null;

        if ($city !== "") {
            [$geoLat, $geoLng] = $this->resolveCoordinatesFromCity(
                $geocodingService,
                $city,
            );
            if ($geoLat !== null && $geoLng !== null) {
                $lat = $geoLat;
                $lng = $geoLng;
            } else {
                $this->addFlash(
                    "error",
                    "City was saved, but coordinates could not be resolved from Nominatim for this value.",
                );
            }
        }

        try {
            $connection->executeStatement(
                "UPDATE donor_eligibility SET blood_type_cache = ?, is_currently_eligible = ?::boolean, days_until_eligible = ?, last_calculated_at = ?, latitude_cache = ?, longitude_cache = ?, eligibility_details = ? WHERE user_id::text = ?",
                [
                    $bloodType !== "" ? $bloodType : null,
                    $eligible ? "true" : "false",
                    $parsedDaysUntil,
                    $lastCalculated !== "" ? $lastCalculated : null,
                    $lat,
                    $lng,
                    $details !== "" ? $details : null,
                    $userId,
                ],
            );

            $this->syncDonorCity(
                $connection,
                $userId,
                $city !== "" ? $city : null,
            );
            $this->addFlash(
                "success",
                "Eligibility record updated successfully.",
            );
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to update eligibility record: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_donor_eligibility");
    }

    #[
        Route(
            "/dashboard/donor-eligibility/{userId}/delete",
            name: "dashboard_donor_eligibility_delete",
            methods: ["POST"],
        ),
    ]
    public function deleteDonorEligibility(
        string $userId,
        Request $request,
        Connection $connection,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        if (
            strtoupper((string) ($sessionUser["user_type"] ?? "")) === "DONOR"
        ) {
            $this->addFlash(
                "error",
                "You are not allowed to delete eligibility records.",
            );

            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        try {
            $connection->executeStatement(
                "DELETE FROM donor_eligibility WHERE user_id::text = ?",
                [$userId],
            );
            $this->addFlash(
                "success",
                "Eligibility record deleted successfully.",
            );
        } catch (Throwable $e) {
            $this->addFlash(
                "error",
                "Unable to delete eligibility record: " . $e->getMessage(),
            );
        }

        return $this->redirectToRoute("dashboard_donor_eligibility");
    }

    #[
        Route(
            "/dashboard/donor-eligibility/export-pdf",
            name: "dashboard_donor_eligibility_export_pdf",
            methods: ["GET"],
        ),
    ]
    public function exportEligibilityPdf(
        Request $request,
        \App\Repository\UserRepository $userRepository,
        \App\Service\EligibilityReportService $reportService,
    ): Response {
        $sessionUser = $request->getSession()->get("auth_user");
        if (!$sessionUser) {
            return $this->redirectToRoute("auth_index");
        }

        $userType = strtoupper((string) ($sessionUser["user_type"] ?? ""));
        if ($userType !== "DONOR") {
            $this->addFlash("error", "Only donors can export their eligibility report.");
            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        $userId = (string) ($sessionUser["id"] ?? "");
        $user = $userRepository->find($userId);

        if (!$user) {
            $this->addFlash("error", "User not found.");
            return $this->redirectToRoute("dashboard_donor_eligibility");
        }

        try {
            $pdfContent = $reportService->generateEligibilityReportPdf($user);
            
            $response = new Response($pdfContent);
            $response->headers->set('Content-Type', 'application/pdf');
            $response->headers->set('Content-Disposition', 'attachment; filename="BloodLink_Eligibility_Report_' . date('Y-m-d_His') . '.pdf"');
            
            return $response;
        } catch (\Exception $e) {
            $this->addFlash("error", "Unable to generate PDF: " . $e->getMessage());
            return $this->redirectToRoute("dashboard_donor_eligibility");
        }
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    private function fetchUsers(
        Connection $connection,
        string $q,
        string $type,
    ): array {
        try {
            $sql =
                "SELECT " .
                "u.user_id, u.email, u.first_name, u.last_name, u.phone, u.user_type, u.created_at, " .
                "d.blood_type_id AS donor_blood_type_id, d.last_donation_date, d.city AS donor_city, d.total_donations, " .
                "hs.role AS staff_role, hs.hospital_id::text AS staff_hospital_id, hs.department AS staff_department " .
                "FROM users u " .
                "LEFT JOIN donors d ON d.user_id = u.user_id " .
                "LEFT JOIN hospital_staff hs ON hs.user_id = u.user_id " .
                "WHERE 1=1";
            $params = [];

            if ($q !== "") {
                $sql .=
                    " AND (" .
                    "LOWER(COALESCE(u.first_name, '')) LIKE :q " .
                    "OR LOWER(COALESCE(u.last_name, '')) LIKE :q " .
                    "OR LOWER(COALESCE(u.email, '')) LIKE :q " .
                    "OR LOWER(COALESCE(u.phone, '')) LIKE :q" .
                    ")";
                $params["q"] = "%" . mb_strtolower($q) . "%";
            }

            if ($type !== "ALL") {
                $sql .= " AND u.user_type = :type";
                $params["type"] = $type;
            }

            $sql .= " ORDER BY u.created_at DESC NULLS LAST";

            return $connection
                ->executeQuery($sql, $params)
                ->fetchAllAssociative();
        } catch (Throwable) {
            return [];
        }
    }

    /**
     * @return array<int, string>
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
     * @return array<int, array{id:string, name:string}>
     */
    private function fetchHospitalOptions(Connection $connection): array
    {
        try {
            $rows = $connection
                ->executeQuery(
                    "SELECT hospital_id::text AS id, name FROM hospital ORDER BY name ASC",
                )
                ->fetchAllAssociative();
            if ($rows !== []) {
                return array_map(static function (array $row): array {
                    return [
                        "id" => (string) ($row["id"] ?? ""),
                        "name" => (string) ($row["name"] ?? "Unnamed Hospital"),
                    ];
                }, $rows);
            }
        } catch (Throwable) {
            // Fallback below.
        }

        try {
            $rows = $connection
                ->executeQuery(
                    "SELECT hospital_id::text AS id, name FROM hospitals ORDER BY name ASC",
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

    private function upsertDonorProfile(
        Connection $connection,
        string $userId,
        string $firstName,
        string $lastName,
        string $bloodTypeId,
        ?string $city,
        ?string $lastDonationDate,
        int $totalDonations,
    ): void {
        $exists = (bool) $connection->fetchOne(
            "SELECT 1 FROM donors WHERE user_id::text = ? LIMIT 1",
            [$userId],
        );

        if ($exists) {
            $connection->executeStatement(
                "UPDATE donors SET first_name = ?, last_name = ?, blood_type_id = ?, city = ?, last_donation_date = ?, total_donations = ? WHERE user_id::text = ?",
                [
                    $firstName,
                    $lastName,
                    $bloodTypeId,
                    $city,
                    $lastDonationDate,
                    $totalDonations,
                    $userId,
                ],
            );

            return;
        }

        $connection->executeStatement(
            "INSERT INTO donors (user_id, first_name, last_name, blood_type_id, city, last_donation_date, is_currently_eligible, total_donations, created_at) VALUES (?::uuid, ?, ?, ?, ?, ?, true, ?, NOW())",
            [
                $userId,
                $firstName,
                $lastName,
                $bloodTypeId,
                $city,
                $lastDonationDate,
                $totalDonations,
            ],
        );
    }

    private function upsertHospitalStaffProfile(
        Connection $connection,
        string $userId,
        string $firstName,
        string $lastName,
        string $role,
        string $hospitalId,
        ?string $department,
    ): void {
        $exists = (bool) $connection->fetchOne(
            "SELECT 1 FROM hospital_staff WHERE user_id::text = ? LIMIT 1",
            [$userId],
        );

        if ($exists) {
            try {
                $connection->executeStatement(
                    "UPDATE hospital_staff SET first_name = ?, last_name = ?, role = ?, hospital_id = ?::uuid, department = ? WHERE user_id::text = ?",
                    [
                        $firstName,
                        $lastName,
                        $role,
                        $hospitalId,
                        $department,
                        $userId,
                    ],
                );
            } catch (Throwable) {
                $connection->executeStatement(
                    "UPDATE hospital_staff SET first_name = ?, last_name = ?, role = ?, hospital_id = ?::uuid WHERE user_id::text = ?",
                    [$firstName, $lastName, $role, $hospitalId, $userId],
                );
            }

            return;
        }

        try {
            $connection->executeStatement(
                "INSERT INTO hospital_staff (user_id, first_name, last_name, role, hospital_id, department, created_at) VALUES (?::uuid, ?, ?, ?, ?::uuid, ?, NOW())",
                [
                    $userId,
                    $firstName,
                    $lastName,
                    $role,
                    $hospitalId,
                    $department,
                ],
            );
        } catch (Throwable) {
            $connection->executeStatement(
                "INSERT INTO hospital_staff (user_id, first_name, last_name, role, hospital_id, created_at) VALUES (?::uuid, ?, ?, ?, ?::uuid, NOW())",
                [$userId, $firstName, $lastName, $role, $hospitalId],
            );
        }
    }

    /**
     * @return array{total:int, donors:int, staff:int}
     */
    private function fetchUserStats(Connection $connection): array
    {
        try {
            $row = $connection->fetchAssociative(
                "SELECT " .
                    "COUNT(*) AS total, " .
                    "SUM(CASE WHEN user_type = 'DONOR' THEN 1 ELSE 0 END) AS donors, " .
                    "SUM(CASE WHEN user_type = 'HOSPITAL_STAFF' THEN 1 ELSE 0 END) AS staff " .
                    "FROM users",
            );

            return [
                "total" => (int) ($row["total"] ?? 0),
                "donors" => (int) ($row["donors"] ?? 0),
                "staff" => (int) ($row["staff"] ?? 0),
            ];
        } catch (Throwable) {
            return ["total" => 0, "donors" => 0, "staff" => 0];
        }
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    private function fetchEligibilityRecords(
        Connection $connection,
        string $q,
        string $status,
    ): array {
        try {
            $sql =
                "SELECT de.user_id, de.blood_type_cache, de.is_currently_eligible, de.days_until_eligible, de.last_calculated_at, de.latitude_cache, de.longitude_cache, de.eligibility_details, d.city AS donor_city " .
                "FROM donor_eligibility de " .
                "LEFT JOIN donors d ON d.user_id = de.user_id " .
                "WHERE 1=1";
            $params = [];

            if ($q !== "") {
                $sql .=
                    " AND (" .
                    "LOWER(COALESCE(de.user_id::text, '')) LIKE :q " .
                    "OR LOWER(COALESCE(de.blood_type_cache, '')) LIKE :q " .
                    "OR LOWER(COALESCE(d.city, '')) LIKE :q" .
                    ")";
                $params["q"] = "%" . mb_strtolower($q) . "%";
            }

            if ($status === "eligible") {
                $sql .= " AND de.is_currently_eligible = true";
            }

            if ($status === "not_eligible") {
                $sql .=
                    " AND (de.is_currently_eligible = false OR de.is_currently_eligible IS NULL)";
            }

            $sql .= " ORDER BY de.last_calculated_at DESC NULLS LAST";

            return $connection
                ->executeQuery($sql, $params)
                ->fetchAllAssociative();
        } catch (Throwable) {
            return [];
        }
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    private function fetchDonorOwnRecord(
        Connection $connection,
        string $userId,
    ): array {
        if ($userId === "") {
            return [];
        }

        try {
            $row = $connection->fetchAssociative(
                "SELECT de.user_id, de.blood_type_cache, de.is_currently_eligible, de.days_until_eligible, de.last_calculated_at, de.latitude_cache, de.longitude_cache, de.eligibility_details, d.city AS donor_city " .
                    "FROM donor_eligibility de " .
                    "LEFT JOIN donors d ON d.user_id = de.user_id " .
                    "WHERE de.user_id::text = ? " .
                    "ORDER BY de.last_calculated_at DESC NULLS LAST LIMIT 1",
                [$userId],
            );

            return $row ? [$row] : [];
        } catch (Throwable) {
            return [];
        }
    }

    /**
     * @return array{total:int, eligible:int, not_eligible:int}
     */
    private function fetchEligibilityStats(Connection $connection): array
    {
        try {
            $row = $connection->fetchAssociative(
                "SELECT " .
                    "COUNT(*) AS total, " .
                    "SUM(CASE WHEN is_currently_eligible = true THEN 1 ELSE 0 END) AS eligible, " .
                    "SUM(CASE WHEN is_currently_eligible = false OR is_currently_eligible IS NULL THEN 1 ELSE 0 END) AS not_eligible " .
                    "FROM donor_eligibility",
            );

            return [
                "total" => (int) ($row["total"] ?? 0),
                "eligible" => (int) ($row["eligible"] ?? 0),
                "not_eligible" => (int) ($row["not_eligible"] ?? 0),
            ];
        } catch (Throwable) {
            return ["total" => 0, "eligible" => 0, "not_eligible" => 0];
        }
    }

    /**
     * @param array<string, mixed>|null $ownRecord
     *
     * @return array<string, mixed>
     */
    private function buildDonorSummary(
        Connection $connection,
        string $userId,
        ?array $ownRecord,
    ): array {
        $summary = [
            "days_since_last_donation" => "N/A",
            "last_donation_date_iso" => "",
            "total_donations" => 0,
            "last_check_days" => "N/A",
            "last_check_date" => "date unavailable",
            "status_text" => "Not checked",
            "status_mood" => "neutral",
            "note" => "Run a pre-screening check to get your latest status.",
            "last_checked_on" => "Last checked on N/A",
        ];

        try {
            $donor = $connection->fetchAssociative(
                "SELECT last_donation_date, total_donations FROM donors WHERE user_id::text = ? LIMIT 1",
                [$userId],
            );

            if ($donor) {
                $summary["total_donations"] =
                    (int) ($donor["total_donations"] ?? 0);

                if (!empty($donor["last_donation_date"])) {
                    $lastDonation = new DateTimeImmutable(
                        (string) $donor["last_donation_date"],
                    );
                    $summary["days_since_last_donation"] = (string) $lastDonation->diff(
                        new DateTimeImmutable("today"),
                    )->days;
                    $summary["last_donation_date_iso"] = $lastDonation->format(
                        "Y-m-d",
                    );
                }
            }
        } catch (Throwable) {
            // Keep defaults.
        }

        if ($ownRecord === null) {
            return $summary;
        }

        $isEligible = (bool) ($ownRecord["is_currently_eligible"] ?? false);
        $summary["status_text"] = $isEligible
            ? "Likely eligible"
            : "Not eligible";
        $summary["status_mood"] = $isEligible ? "good" : "bad";

        try {
            if (!empty($ownRecord["last_calculated_at"])) {
                $lastCalculated = new DateTimeImmutable(
                    (string) $ownRecord["last_calculated_at"],
                );
                $summary["last_check_days"] =
                    (string) $lastCalculated->diff(
                        new DateTimeImmutable("today"),
                    )->days . " days";
                $summary["last_check_date"] = $lastCalculated->format("M d, Y");
                $summary["last_checked_on"] =
                    "Last checked on " . $lastCalculated->format("M d, Y");
            }
        } catch (Throwable) {
            // Keep defaults.
        }

        $bloodType = trim((string) ($ownRecord["blood_type_cache"] ?? ""));
        if ($bloodType === "") {
            $bloodType = "N/A";
        }

        if ($isEligible) {
            $summary["note"] =
                "This is a preliminary result. A doctor will make the final decision on donation day. Blood type " .
                $bloodType .
                " is on file.";
        } else {
            $wait = $ownRecord["days_until_eligible"] ?? "N/A";
            $summary["note"] =
                "This is a preliminary result. You are currently marked not eligible. Estimated wait: " .
                $wait .
                " day(s).";
        }

        return $summary;
    }

    private function generateUuidV4(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);

        $hex = bin2hex($bytes);

        return sprintf(
            "%s-%s-%s-%s-%s",
            substr($hex, 0, 8),
            substr($hex, 8, 4),
            substr($hex, 12, 4),
            substr($hex, 16, 4),
            substr($hex, 20, 12),
        );
    }

    /**
     * @return array{0: ?string, 1: ?string}
     */
    private function resolveCoordinatesFromCity(
        GeocodingService $geocodingService,
        string $city,
    ): array {
        if ($city === "") {
            return [null, null];
        }

        $coordinates = $geocodingService->geocodeCity($city);
        if ($coordinates === null) {
            return [null, null];
        }

        return [$coordinates["latitude"], $coordinates["longitude"]];
    }

    private function syncDonorCity(
        Connection $connection,
        string $userId,
        ?string $city,
    ): void {
        if ($city === null || trim($city) === "") {
            return;
        }

        try {
            $connection->executeStatement(
                "UPDATE donors SET city = ? WHERE user_id::text = ?",
                [$city, $userId],
            );
        } catch (Throwable) {
            // Keep eligibility updates resilient even if donor profile is missing.
        }
    }

    /**
     * @return array{0: bool, 1: string, 2: ?int}
     */
    private function validateManualEligibilityInput(
        string $bloodType,
        bool $eligible,
        string $daysUntilRaw,
    ): array {
        if (
            $bloodType !== "" &&
            !preg_match('/^(A|B|AB|O)[+-]$/', strtoupper(trim($bloodType)))
        ) {
            return [
                false,
                "Blood type format is invalid. Use A+, A-, B+, B-, AB+, AB-, O+ or O-.",
                null,
            ];
        }

        if ($daysUntilRaw === "") {
            return [false, "Days until eligible is required.", null];
        }

        if (!preg_match('/^-?\d+$/', $daysUntilRaw)) {
            return [false, "Days until eligible must be a valid number.", null];
        }

        $daysUntil = (int) $daysUntilRaw;
        if ($daysUntil < 0) {
            return [false, "Days until eligible cannot be negative.", null];
        }

        if ($eligible && $daysUntil !== 0) {
            return [
                false,
                "Eligible donors must have 0 days until eligible.",
                null,
            ];
        }

        if (!$eligible && $daysUntil <= 0) {
            return [
                false,
                "Not eligible donors must have days until eligible greater than 0.",
                null,
            ];
        }

        return [true, "", $daysUntil];
    }

    /**
     * @return array{ok: bool, error?: string, blood_type_cache?: string, is_currently_eligible?: bool, days_until_eligible?: int, city?: string, eligibility_details?: string, last_donation_date_formatted?: string}
     */
    private function calculateDonorEligibilityFromRequest(
        Request $request,
        Connection $connection,
        string $userId,
    ): array {
        $ageRaw = trim((string) $request->request->get("age", ""));
        $weightRaw = trim((string) $request->request->get("weight", ""));
        $lastDonationRaw = trim(
            (string) $request->request->get("last_donation_date", ""),
        );

        if ($ageRaw === "" || !preg_match('/^\d+$/', $ageRaw)) {
            return [
                "ok" => false,
                "error" => "Age is required and must be a valid number.",
            ];
        }
        if ($weightRaw === "" || !is_numeric($weightRaw)) {
            return [
                "ok" => false,
                "error" => "Weight is required and must be a valid number.",
            ];
        }

        $age = (int) $ageRaw;
        $weight = (float) $weightRaw;
        if ($weight <= 0) {
            return ["ok" => false, "error" => "Weight must be greater than 0."];
        }

        $today = new DateTimeImmutable("today");
        $lastDonationDate = null;
        if ($lastDonationRaw !== "") {
            try {
                $lastDonationDate = new DateTimeImmutable($lastDonationRaw);
            } catch (Throwable) {
                return [
                    "ok" => false,
                    "error" => "Last donation date must be a valid date.",
                ];
            }

            if ($lastDonationDate > $today) {
                return [
                    "ok" => false,
                    "error" => "Last donation date cannot be in the future.",
                ];
            }
        }

        $unwellToday = $this->requestBoolean($request, "unwell_today");
        $hasFever = $this->requestBoolean($request, "has_fever");
        $hasIllness = $this->requestBoolean($request, "has_illness");
        $pregnantRecentBirth = $this->requestBoolean(
            $request,
            "pregnant_recent_birth",
        );
        $heartDisease = $this->requestBoolean($request, "heart_disease");
        $anemia = $this->requestBoolean($request, "anemia");
        $chronicIllness = $this->requestBoolean($request, "chronic_illness");
        $onMedication = $this->requestBoolean($request, "on_medication");
        $bleedingDisorder = $this->requestBoolean($request, "bleeding_disorder");
        $infectiousDisease = $this->requestBoolean($request, "infectious_disease");
        $recentSurgery = $this->requestBoolean($request, "recent_surgery");
        $recentVaccine = $this->requestBoolean($request, "recent_vaccine");
        $recentTravel = $this->requestBoolean($request, "recent_travel");
        $recentTattoo = $this->requestBoolean($request, "recent_tattoo");
        $recentBloodTransfusion = $this->requestBoolean($request, "recent_blood_transfusion");
        
        $gender = trim((string) $request->request->get("gender", ""));
        $wellbeingNotes = trim((string) $request->request->get("wellbeing_notes", ""));

        $hardStops = [];
        $doctorReview = [];
        $daysUntilEligible = 0;
        $minDaysBetweenDonations = 56;

        if ($age < 18 || $age > 65) {
            $hardStops[] =
                "Donors must be between 18 and 65 years old to participate";
        }

        if ($weight < 50.0) {
            $hardStops[] = "A minimum weight of 50 kg is required to donate.";
        }

        if ($lastDonationDate !== null) {
            $elapsed = (int) $lastDonationDate->diff($today)->days;
            if ($elapsed < $minDaysBetweenDonations) {
                $daysUntilEligible = max(
                    $minDaysBetweenDonations - $elapsed,
                    1,
                );
                $hardStops[] =
                    "Last donation was too recent (wait " .
                    $daysUntilEligible .
                    " more day(s)).";
            }
        }

        if ($unwellToday) {
            $hardStops[] =
                "You reported not feeling well today.Donating while unwell can affect both your health and the quality of the donated blood. Please wait until you have fully recovered before attempting to donate.";
        }

        if ($hasFever) {
            $hardStops[] =
                "You have fever or elevated temperature. Blood donation is not possible until body temperature returns to normal.";
        }

        if ($hasIllness) {
            $hardStops[] =
                "You currently have acute illness. Please wait until fully recovered before donating.";
        }

        if ($pregnantRecentBirth) {
            $hardStops[] =
                "Donation is temporarily deferred during pregnancy and for 6 weeks following childbirth.";
        }

        if ($bleedingDisorder) {
            $hardStops[] =
                "Bleeding disorder or clotting issues prevent blood donation.";
        }

        if ($infectiousDisease) {
            $hardStops[] =
                "Infectious disease detected - blood donation is not permitted.";
        }

        if ($recentBloodTransfusion) {
            $hardStops[] =
                "Recent blood transfusion recorded - must wait minimum deferral period before donating.";
        }

        if ($heartDisease) {
            $doctorReview[] =
                "Heart disease or cardiac condition - must be reviewed by a doctor.";
        }

        if ($anemia) {
            $doctorReview[] =
                "Anemia or iron deficiency reported - doctor review required.";
        }

        if ($chronicIllness) {
            $doctorReview[] =
                "Chronic illness or disease reported - must be assessed by doctor.";
        }

        if ($onMedication) {
            $doctorReview[] =
                "Currently on medication, a doctor will review the case to determine if the medication affects eligibility.";
        }

        if ($recentSurgery) {
            $doctorReview[] = "Recent surgery - may affect donation eligibility depending on type and recovery time.";
        }

        if ($recentVaccine) {
            $doctorReview[] = "Recent vaccine - deferral period may apply depending on vaccine type.";
        }

        if ($recentTravel) {
            $doctorReview[] =
                "Recent travel abroad, travel history has been noted and will be reviewed by a doctor before donation day.";
        }

        if ($recentTattoo) {
            $doctorReview[] = "Recent tattoo or piercing - a 6-month waiting period applies.";
        }

        $outcome = "Likely Eligible";
        $eligible = true;
        $reasons = ["No blockers detected."];

        if ($hardStops !== []) {
            if ($daysUntilEligible <= 0 && $lastDonationDate !== null) {
                $daysUntilEligible = 1;
            }
            $outcome = "Not Recommended";
            $eligible = false;
            $reasons = $hardStops;
        } elseif ($doctorReview !== []) {
            $outcome = "Needs Doctor Review";
            $eligible = false;
            $daysUntilEligible = 1;
            $reasons = $doctorReview;
        }

        $donor =
            $connection->fetchAssociative(
                "SELECT blood_type_id, city FROM donors WHERE user_id::text = ? LIMIT 1",
                [$userId],
            ) ?:
            [];

        $bloodTypeCache = trim((string) ($donor["blood_type_id"] ?? ""));
        if ($bloodTypeCache === "") {
            $bloodTypeCache = "N/A";
        }

        $details = $this->buildDonorEligibilityDetails(
            $outcome,
            $age,
            $gender,
            $weightRaw,
            $lastDonationDate,
            $minDaysBetweenDonations,
            $unwellToday,
            $hasFever,
            $hasIllness,
            $pregnantRecentBirth,
            $heartDisease,
            $anemia,
            $chronicIllness,
            $onMedication,
            $bleedingDisorder,
            $infectiousDisease,
            $recentSurgery,
            $recentVaccine,
            $recentTravel,
            $recentTattoo,
            $recentBloodTransfusion,
            $wellbeingNotes,
            $reasons,
            $today,
        );

        return [
            "ok" => true,
            "blood_type_cache" => $bloodTypeCache,
            "is_currently_eligible" => $eligible,
            "days_until_eligible" => $daysUntilEligible,
            "city" => trim((string) ($donor["city"] ?? "")),
            "eligibility_details" => $details,
            "last_donation_date_formatted" => $lastDonationDate !== null ? $lastDonationDate->format("Y-m-d") : null,
        ];
    }

    private function requestBoolean(Request $request, string $name): bool
    {
        $value = strtolower(trim((string) $request->request->get($name, "")));

        return in_array($value, ["1", "true", "yes", "on"], true);
    }

    private function normalizeEmail(string $email): string
    {
        $normalized = trim($email);

        if ($normalized === "") {
            return "";
        }

        // Remove whitespace copied from rich text and optional mailto prefix.
        $normalized = preg_replace('/\s+/u', '', $normalized) ?? $normalized;
        if (str_starts_with(strtolower($normalized), 'mailto:')) {
            $normalized = substr($normalized, 7);
        }

        return $normalized;
    }

    private function isValidEmailAddress(string $email): bool
    {
        if ($email === "") {
            return false;
        }

        // Keep validation aligned with DB constraint chk_email_format.
        return preg_match(
            '/^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/',
            $email,
        ) === 1;
    }

    /**
     * @param array<int, string> $reasons
     */
    private function buildDonorEligibilityDetails(
        string $outcome,
        int $age,
        string $gender,
        string $weight,
        ?DateTimeImmutable $lastDonationDate,
        int $threshold,
        bool $unwellToday,
        bool $hasFever,
        bool $hasIllness,
        bool $pregnantRecentBirth,
        bool $heartDisease,
        bool $anemia,
        bool $chronicIllness,
        bool $onMedication,
        bool $bleedingDisorder,
        bool $infectiousDisease,
        bool $recentSurgery,
        bool $recentVaccine,
        bool $recentTravel,
        bool $recentTattoo,
        bool $recentBloodTransfusion,
        string $wellbeingNotes,
        array $reasons,
        DateTimeImmutable $today,
    ): string {
        $lines = [];
        $lines[] = "Outcome: " . $outcome;
        $lines[] = "";
        
        // Basic Details
        $lines[] = "=== BASIC DETAILS ===";
        $lines[] = "Age: " . $age;
        $lines[] = "Gender: " . ($gender ?: "Not specified");
        $lines[] = "Weight: " . $weight . " kg";
        $lines[] = "";
        
        // Donation History
        $lines[] = "=== DONATION HISTORY ===";
        if ($lastDonationDate !== null) {
            $elapsed = (int) $lastDonationDate->diff($today)->days;
            $lines[] =
                "Last donation: " .
                $lastDonationDate->format("Y-m-d") .
                " (elapsed " .
                $elapsed .
                " days, min " .
                $threshold .
                ")";
        } else {
            $lines[] = "Last donation: Not provided";
        }
        $lines[] = "";
        
        // Current Health Status
        $lines[] = "=== CURRENT HEALTH STATUS ===";
        $lines[] = "Not feeling well: " . ($unwellToday ? "Yes" : "No");
        $lines[] = "Have fever/elevated temperature: " . ($hasFever ? "Yes" : "No");
        $lines[] = "Have acute illness: " . ($hasIllness ? "Yes" : "No");
        $lines[] = "";
        
        // Pregnancy Status
        $lines[] = "=== PREGNANCY STATUS ===";
        $lines[] = "Pregnant or recent birth: " . ($pregnantRecentBirth ? "Yes" : "No");
        $lines[] = "";
        
        // Medical Conditions
        $lines[] = "=== MEDICAL CONDITIONS ===";
        $lines[] = "Heart disease/cardiac condition: " . ($heartDisease ? "Yes" : "No");
        $lines[] = "Anemia/iron deficiency: " . ($anemia ? "Yes" : "No");
        $lines[] = "Chronic illness or disease: " . ($chronicIllness ? "Yes" : "No");
        $lines[] = "On medication: " . ($onMedication ? "Yes" : "No");
        $lines[] = "Bleeding disorder/clotting issue: " . ($bleedingDisorder ? "Yes" : "No");
        $lines[] = "Infectious disease: " . ($infectiousDisease ? "Yes" : "No");
        $lines[] = "";
        
        // Recent Events
        $lines[] = "=== RECENT EVENTS (within last 6 months) ===";
        $lines[] = "Recent surgery/procedures: " . ($recentSurgery ? "Yes" : "No");
        $lines[] = "Recent vaccine: " . ($recentVaccine ? "Yes" : "No");
        $lines[] = "Recent travel abroad: " . ($recentTravel ? "Yes" : "No");
        $lines[] = "Recent tattoo/piercing: " . ($recentTattoo ? "Yes" : "No");
        $lines[] = "Recent blood transfusion: " . ($recentBloodTransfusion ? "Yes" : "No");
        $lines[] = "";
        
        // General Well-being Notes
        if ($wellbeingNotes !== "") {
            $lines[] = "=== GENERAL WELL-BEING NOTES ===";
            $lines[] = $wellbeingNotes;
            $lines[] = "";
        }
        
        // Reasons
        if ($reasons !== []) {
            $lines[] = "=== ASSESSMENT REASONS ===";
            foreach ($reasons as $reason) {
                $lines[] = "- " . $reason;
            }
        }

        return implode("\n", $lines);
    }

    private function upsertEligibilityRecord(
        Connection $connection,
        GeocodingService $geocodingService,
        string $userId,
        string $bloodType,
        bool $eligible,
        int $daysUntil,
        string $city,
        string $details,
        string $lastCalculated,
        string $lastDonationDate = "",
    ): void {
        $existing =
            $connection->fetchAssociative(
                "SELECT user_id, latitude_cache, longitude_cache FROM donor_eligibility WHERE user_id::text = ? LIMIT 1",
                [$userId],
            ) ?:
            [];

        $lat = !empty($existing["latitude_cache"])
            ? (string) $existing["latitude_cache"]
            : null;
        $lng = !empty($existing["longitude_cache"])
            ? (string) $existing["longitude_cache"]
            : null;

        if ($city !== "") {
            [$geoLat, $geoLng] = $this->resolveCoordinatesFromCity(
                $geocodingService,
                $city,
            );
            if ($geoLat !== null && $geoLng !== null) {
                $lat = $geoLat;
                $lng = $geoLng;
            }
        }

        $hasExisting = !empty($existing["user_id"]);
        if ($hasExisting) {
            $connection->executeStatement(
                "UPDATE donor_eligibility SET blood_type_cache = ?, is_currently_eligible = ?::boolean, days_until_eligible = ?, last_calculated_at = ?, latitude_cache = ?, longitude_cache = ?, eligibility_details = ? WHERE user_id::text = ?",
                [
                    $bloodType !== "" ? $bloodType : null,
                    $eligible ? 'true' : 'false',
                    $daysUntil,
                    $lastCalculated,
                    $lat,
                    $lng,
                    $details !== "" ? $details : null,
                    $userId,
                ],
            );
        } else {
            $connection->executeStatement(
                "INSERT INTO donor_eligibility (user_id, blood_type_cache, is_currently_eligible, days_until_eligible, last_calculated_at, latitude_cache, longitude_cache, eligibility_details) VALUES (?::uuid, ?, ?::boolean, ?, ?, ?, ?, ?)",
                [
                    $userId,
                    $bloodType !== "" ? $bloodType : null,
                    $eligible ? 'true' : 'false',
                    $lastCalculated,
                    $lat,
                    $lng,
                    $details !== "" ? $details : null,
                ],
            );
        }

        $this->syncDonorCity($connection, $userId, $city !== "" ? $city : null);

        if ($lastDonationDate !== "") {
            $connection->executeStatement(
                "UPDATE donors SET last_donation_date = ? WHERE user_id::text = ?",
                [$lastDonationDate, $userId],
            );
        }
    }
}
