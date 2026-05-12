package tn.edu.esprit.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.edu.esprit.entities.BloodInventory;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.entities.HospitalStaff;
import tn.edu.esprit.entities.InventoryStatus;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.HospitalServiceImpl;
import tn.edu.esprit.services.InventoryServiceImpl;
import tn.edu.esprit.services.ServiceHospitalStaff;

/**
 * InventoryViewController displays blood inventory for a hospital.
 *
 * DESIGN:
 * - Hospital Staff: Views inventory for their assigned hospital only
 * - Admin: Can select which hospital to view via dropdown
 * - Each hospital has ONE inventory row per blood type (consolidated model)
 */
public class InventoryViewController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(
        InventoryViewController.class.getName()
    );

    @FXML
    private GridPane bloodTypeGrid;

    @FXML
    private Label lblCurrentDate;

    @FXML
    private Label lblTotalStock;

    @FXML
    private Label lblCriticalCount;

    @FXML
    private Label lblLowCount;

    @FXML
    private HBox badgeCritical;

    @FXML
    private HBox badgeLow;

       @FXML
    private HBox hospitalSelectionBox; // For admin only

    @FXML
    private ComboBox<String> hospitalCombo; // For admin only

    @FXML
    private Label lblHospitalName; // Shows current hospital name

    private final InventoryServiceImpl inventoryService =
        new InventoryServiceImpl();
    private final HospitalServiceImpl hospitalService =
        new HospitalServiceImpl();
    private final ServiceHospitalStaff hospitalStaffService =
        new ServiceHospitalStaff();

    private List<BloodInventory> currentHospitalInventories;
    private UUID currentSelectedHospitalId;
    private Map<String, UUID> hospitalNameToId; // For dropdown mapping

    // The 8 standard blood types in display order
    private static final String[] BLOOD_TYPE_ORDER = {
        "A+",
        "A-",
        "B+",
        "B-",
        "AB+",
        "AB-",
        "O+",
        "O-",
    };

    // Maximum stock for progress bar calculation
    private static final int MAX_STOCK_FOR_BAR = 60;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Set today's date
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEEE dd MMMM yyyy",
                Locale.ENGLISH
            );
            String dateStr = today.format(formatter);
            lblCurrentDate.setText(
                dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1)
            );

            // Initialize hospital selection based on user type
            initializeHospitalSelection();

            // Load inventory for current hospital
            refreshData();
        } catch (Exception e) {
            LOGGER.log(
                Level.SEVERE,
                "Error initializing InventoryViewController",
                e
            );
            showErrorAlert(
                "Initialization Error",
                "Failed to initialize inventory view: " + e.getMessage()
            );
        }
    }

    // ============================================================
    // Hospital Selection
    // ============================================================

    /**
     * Initializes hospital selection based on user type.
     * - HOSPITAL_STAFF: Shows their hospital only
     * - ADMIN: Shows dropdown to select hospital
     */
    private void initializeHospitalSelection() {
        Users currentUser = AppSession.getCurrentUser();

        if (currentUser == null) {
            LOGGER.warning("No current user in session");
            showErrorAlert("Session Error", "No user logged in");
            return;
        }

        if (currentUser.getUserType() == UserType.ADMIN) {
            // Admin: Show hospital dropdown
            setupAdminHospitalSelection(currentUser);
        } else if (currentUser.getUserType() == UserType.HOSPITAL_STAFF) {
            // Hospital Staff: Show their hospital only
            setupHospitalStaffView(currentUser);
        } else {
            // Other user types (DONOR) shouldn't access inventory
            LOGGER.warning(
                "User type " +
                    currentUser.getUserType() +
                    " cannot access inventory"
            );
            showErrorAlert(
                "Access Denied",
                "Your user role cannot access the inventory management"
            );
        }
    }

    /**
     * Sets up the view for hospital staff - shows only their hospital's inventory.
     */
    private void setupHospitalStaffView(Users currentUser) {
        try {
            // Get hospital staff record to find their hospital
            HospitalStaff staff = new HospitalStaff();
            staff.setId(currentUser.getId());
            HospitalStaff staffRecord = hospitalStaffService.getOne(staff);

            if (staffRecord == null || staffRecord.getHospitalId() == null) {
                LOGGER.warning(
                    "No hospital assignment found for staff: " +
                        currentUser.getId()
                );
                showErrorAlert(
                    "Hospital Not Found",
                    "Your hospital assignment could not be found"
                );
                return;
            }

            currentSelectedHospitalId = UUID.fromString(
                staffRecord.getHospitalId()
            );

            // Hide hospital selection dropdown (staff can only see their hospital)
            if (hospitalSelectionBox != null) {
                hospitalSelectionBox.setVisible(false);
                hospitalSelectionBox.setManaged(false);
            }

            // Show hospital name
            Hospital hospital = hospitalService.getHospitalById(
                currentSelectedHospitalId
            );
            if (hospital != null && lblHospitalName != null) {
                lblHospitalName.setText("Hospital: " + hospital.getName());
            }

            LOGGER.info(
                "Hospital staff view initialized for hospital: " +
                    currentSelectedHospitalId
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up hospital staff view", e);
            showErrorAlert(
                "Error",
                "Failed to set up inventory view: " + e.getMessage()
            );
        }
    }

    /**
     * Sets up the view for admin users - shows dropdown to select hospital.
     */
    private void setupAdminHospitalSelection(Users currentUser) {
        try {
            // Show hospital selection dropdown
            if (hospitalSelectionBox != null) {
                hospitalSelectionBox.setVisible(true);
                hospitalSelectionBox.setManaged(true);
            }

            // Load all hospitals into dropdown
            List<Hospital> hospitals = hospitalService.getAllHospitals();

            if (hospitals == null || hospitals.isEmpty()) {
                LOGGER.warning("No hospitals found in system");
                showErrorAlert("No Hospitals", "No hospitals found in system");
                return;
            }

            hospitalNameToId = new LinkedHashMap<>();
            List<String> hospitalNames = new ArrayList<>();

            for (Hospital hospital : hospitals) {
                if (hospital.getHospitalId() != null && hospital.isActive()) {
                    String name = hospital.getName();
                    UUID id = hospital.getHospitalId();
                    hospitalNameToId.put(name, id);
                    hospitalNames.add(name);
                }
            }

            // Sort hospital names
            Collections.sort(hospitalNames);

            // Populate dropdown
            if (hospitalCombo != null) {
                hospitalCombo.getItems().clear();
                hospitalCombo.getItems().addAll(hospitalNames);

                // Select first hospital by default
                if (!hospitalNames.isEmpty()) {
                    hospitalCombo.setValue(hospitalNames.get(0));
                    currentSelectedHospitalId = hospitalNameToId.get(
                        hospitalNames.get(0)
                    );
                }

                // Add listener for dropdown changes
                hospitalCombo
                    .valueProperty()
                    .addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            currentSelectedHospitalId = hospitalNameToId.get(
                                newVal
                            );
                            if (lblHospitalName != null) {
                                lblHospitalName.setText("Hospital: " + newVal);
                            }
                            refreshData();
                        }
                    });
            }

            LOGGER.info("Admin view initialized with hospital dropdown");
        } catch (Exception e) {
            LOGGER.log(
                Level.SEVERE,
                "Error setting up admin hospital selection",
                e
            );
            showErrorAlert(
                "Error",
                "Failed to load hospitals: " + e.getMessage()
            );
        }
    }

    // ============================================================
    // Data Loading
    // ============================================================

    /**
     * Refreshes inventory data for the currently selected hospital.
     */
    public void refreshData() {
        try {
            if (currentSelectedHospitalId == null) {
                LOGGER.warning("No hospital selected");
                currentHospitalInventories = new ArrayList<>();
                buildBloodTypeGrid(currentHospitalInventories);
                updateHeaderStats(currentHospitalInventories);
                return;
            }

            // Get inventory for this specific hospital
            currentHospitalInventories =
                inventoryService.getInventoriesByHospital(
                    currentSelectedHospitalId
                );

            if (currentHospitalInventories == null) {
                currentHospitalInventories = new ArrayList<>();
            }

            LOGGER.info(
                "Loaded " +
                    currentHospitalInventories.size() +
                    " inventory rows for hospital: " +
                    currentSelectedHospitalId
            );

            buildBloodTypeGrid(currentHospitalInventories);
            updateHeaderStats(currentHospitalInventories);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refreshing inventory data", e);
            showErrorAlert(
                "Data Load Error",
                "Failed to load inventory: " + e.getMessage()
            );
        }
    }

    // ============================================================
    // Header Statistics
    // ============================================================

    /**
     * Updates the header statistics (total stock, critical count, low count).
     */
    private void updateHeaderStats(List<BloodInventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            lblTotalStock.setText("0 units in stock");
            badgeCritical.setVisible(false);
            badgeLow.setVisible(false);
            return;
        }

        // Calculate total stock
        int totalStock = inventories
            .stream()
            .mapToInt(inv ->
                inv.getQuantityUnits() != null ? inv.getQuantityUnits() : 0
            )
            .sum();
        lblTotalStock.setText(totalStock + " units in stock");

        // Count critical and low inventories
        long criticalCount = inventories
            .stream()
            .filter(inv -> inv.getStatus() == InventoryStatus.CRITICAL)
            .count();
        long lowCount = inventories
            .stream()
            .filter(inv -> inv.getStatus() == InventoryStatus.LOW)
            .count();

        // Display critical badge
        if (criticalCount > 0) {
            badgeCritical.setVisible(true);
            badgeCritical.setManaged(true);
            lblCriticalCount.setText(criticalCount + " critical");
        } else {
            badgeCritical.setVisible(false);
            badgeCritical.setManaged(false);
        }

        // Display low badge
        if (lowCount > 0) {
            badgeLow.setVisible(true);
            badgeLow.setManaged(true);
            lblLowCount.setText(lowCount + " low");
        } else {
            badgeLow.setVisible(false);
            badgeLow.setManaged(false);
        }
    }

    // ============================================================
    // Grid Building
    // ============================================================

    /**
     * Builds the blood type grid with cards for all 8 blood types.
     */
    private void buildBloodTypeGrid(List<BloodInventory> inventories) {
        bloodTypeGrid.getChildren().clear();

        // Create a map for quick lookup by blood type
        Map<String, BloodInventory> inventoryByBloodType = inventories
            .stream()
            .collect(
                Collectors.toMap(
                    BloodInventory::getBloodTypeId,
                    inv -> inv,
                    (existing, duplicate) -> existing
                )
            );

        int col = 0;
        int row = 0;

        // Display all blood types in order
        for (String bloodType : BLOOD_TYPE_ORDER) {
            BloodInventory inventory = inventoryByBloodType.getOrDefault(
                bloodType,
                createEmptyInventory(bloodType)
            );

            VBox card = createBloodTypeCard(inventory);
            bloodTypeGrid.add(card, col, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }

    /**
     * Creates an empty inventory for a blood type that has no stock.
     */
    private BloodInventory createEmptyInventory(String bloodType) {
        BloodInventory empty = new BloodInventory();
        empty.setBloodTypeId(bloodType);
        empty.setQuantityUnits(0);
        empty.setStatus(InventoryStatus.CRITICAL);
        return empty;
    }

    /**
     * Creates a single blood type card.
     */
    private VBox createBloodTypeCard(BloodInventory inventory) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.getStyleClass().add("inv-blood-card");

        String bloodType =
            inventory.getBloodTypeId() != null
                ? inventory.getBloodTypeId()
                : "?";
        int quantity =
            inventory.getQuantityUnits() != null
                ? inventory.getQuantityUnits()
                : 0;
        InventoryStatus status =
            inventory.getStatus() != null
                ? inventory.getStatus()
                : InventoryStatus.CRITICAL;

        // Determine colors based on status
        String borderColor;
        String statusText;
        String statusBgColor;
        String statusTextColor;
        String progressBarColor;

        switch (status) {
            case OPTIMAL:
                borderColor = "#e2e8f0";
                statusText = "Optimal";
                statusBgColor = "#dcfce7";
                statusTextColor = "#15803d";
                progressBarColor = "#16a34a";
                break;
            case LOW:
                borderColor = "#fbbf24";
                statusText = "Low";
                statusBgColor = "#fef9c3";
                statusTextColor = "#a16207";
                progressBarColor = "#f59e0b";
                break;
            case CRITICAL:
                borderColor = "#ef4444";
                statusText = "Critical";
                statusBgColor = "#fee2e2";
                statusTextColor = "#dc2626";
                progressBarColor = "#ef4444";
                break;
            default:
                borderColor = "#e2e8f0";
                statusText = "—";
                statusBgColor = "#f3f4f6";
                statusTextColor = "#6b7280";
                progressBarColor = "#94a3b8";
                break;
        }

        // Apply card border based on status
        if (
            status == InventoryStatus.LOW || status == InventoryStatus.CRITICAL
        ) {
            card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                    "-fx-border-color: " +
                    borderColor +
                    "; -fx-border-width: 1.5; -fx-border-radius: 12;"
            );
        } else {
            card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                    "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 12;"
            );
        }

        // ---- Row 1: Blood Type Badge + Status Badge ----
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setSpacing(8);

        Label bloodTypeBadge = new Label(bloodType);
        bloodTypeBadge.setStyle(
            "-fx-background-color: " +
                progressBarColor +
                "; -fx-background-radius: 8; " +
                "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-padding: 4 10 4 10; -fx-min-width: 42; -fx-alignment: center;"
        );

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label statusBadge = new Label(statusText);
        statusBadge.setStyle(
            "-fx-background-color: " +
                statusBgColor +
                "; -fx-background-radius: 12; " +
                "-fx-text-fill: " +
                statusTextColor +
                "; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-padding: 3 10 3 10;"
        );

        topRow.getChildren().addAll(bloodTypeBadge, spacer1, statusBadge);

        // ---- Row 2: Stock Row ----
        HBox stockRow = new HBox();
        stockRow.setAlignment(Pos.CENTER_LEFT);
        stockRow.setSpacing(6);
        stockRow.setPadding(new Insets(4, 0, 0, 0));

        Label stockIcon = new Label("🩸");
        stockIcon.setStyle("-fx-font-size: 12px;");

        Label stockLabel = new Label("Stock");
        stockLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12.5px;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Label stockValue = new Label(quantity + " units");
        stockValue.setStyle(
            "-fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-font-weight: bold;"
        );

        stockRow
            .getChildren()
            .addAll(stockIcon, stockLabel, spacer2, stockValue);

        // ---- Row 3: Progress Bar ----
        double progress = Math.min(1.0, (double) quantity / MAX_STOCK_FOR_BAR);

        StackPane progressBarContainer = new StackPane();
        progressBarContainer.setAlignment(Pos.CENTER_LEFT);
        progressBarContainer.setPrefHeight(6);
        progressBarContainer.setMinHeight(6);
        progressBarContainer.setMaxHeight(6);

        Region track = new Region();
        track.setStyle(
            "-fx-background-color: #f1f5f9; -fx-background-radius: 3;"
        );
        track.setMaxWidth(Double.MAX_VALUE);
        track.setPrefHeight(6);

        Region fill = new Region();
        fill.setStyle(
            "-fx-background-color: " +
                progressBarColor +
                "; -fx-background-radius: 3;"
        );
        fill.setPrefHeight(6);
        fill.setMaxHeight(6);

        progressBarContainer.getChildren().addAll(track, fill);
        fill
            .maxWidthProperty()
            .bind(progressBarContainer.widthProperty().multiply(progress));

        // ---- Row 4: Info Row ----
        HBox infoRow = new HBox();
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.setSpacing(6);
        infoRow.setPadding(new Insets(2, 0, 0, 0));

        Label infoIcon = new Label("ℹ");
        infoIcon.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        String infoText =
            "Last updated: " + formatTimestamp(inventory.getUpdatedAt());
        Label infoLabel = new Label(infoText);
        infoLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");

        infoRow.getChildren().addAll(infoIcon, infoLabel);

        // ---- Assemble Card ----
        card
            .getChildren()
            .addAll(topRow, stockRow, progressBarContainer, infoRow);

        return card;
    }

    /**
     * Formats a timestamp for display.
     */
    private String formatTimestamp(java.sql.Timestamp timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }
        return timestamp
            .toLocalDateTime()
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ============================================================
    // Action Handlers
    // ============================================================

    @FXML
    private void onUrgentAlert() {
        loadDashboardSection("/tn/edu/esprit/views/DashboardAlerts.fxml");
    }

    @FXML
    private void onRequestTransfer() {
        loadDashboardSection("/tn/edu/esprit/views/TransferList.fxml");
    }

    // ============================================================
    // Utility Methods
    // ============================================================

    /**
     * Shows an error alert dialog.
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadDashboardSection(String fxmlPath) {
        try {
            Node contentArea = bloodTypeGrid.getScene() != null
                ? bloodTypeGrid.getScene().lookup("#contentArea")
                : null;
            if (!(contentArea instanceof StackPane)) {
                showErrorAlert("Navigation Error", "Could not find dashboard content area.");
                return;
            }
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            ((StackPane) contentArea).getChildren().setAll(view);
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to open dashboard section " + fxmlPath, exception);
            showErrorAlert("Navigation Error", "Could not open this section: " + exception.getMessage());
        }
    }
}
