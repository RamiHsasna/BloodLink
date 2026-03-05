package tn.edu.esprit.controllers;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.edu.esprit.entities.BloodInventory;
import tn.edu.esprit.entities.InventoryStatus;
import tn.edu.esprit.services.InventoryServiceImpl;

public class InventoryViewController implements Initializable {

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

    private final InventoryServiceImpl inventoryService =
        new InventoryServiceImpl();
    private List<BloodInventory> allInventories;

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

    // Maximum stock for progress bar calculation (adjust to your needs)
    private static final int MAX_STOCK_FOR_BAR = 60;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set today's date in English format
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
            "EEEE dd MMMM yyyy",
            Locale.ENGLISH
        );
        String dateStr = today.format(formatter);
        // Capitalize first letter
        lblCurrentDate.setText(
            dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1)
        );

        refreshData();
    }

    // ==================== DATA LOADING ====================

    public void refreshData() {
        allInventories = inventoryService.getAllInventories();
        buildBloodTypeGrid(allInventories);
        updateHeaderStats(allInventories);
    }

    // ==================== HEADER STATS ====================

    private void updateHeaderStats(List<BloodInventory> inventories) {
        int totalStock = inventories
            .stream()
            .mapToInt(inv ->
                inv.getQuantityUnitsInt() != null
                    ? inv.getQuantityUnitsInt()
                    : 0
            )
            .sum();
        lblTotalStock.setText(totalStock + " units in stock");

        // Aggregate by blood type to determine status counts
        Map<String, BloodTypeSummary> summaries = aggregateByBloodType(
            inventories
        );

        long criticalCount = summaries
            .values()
            .stream()
            .filter(s -> s.status == InventoryStatus.CRITICAL)
            .count();
        long lowCount = summaries
            .values()
            .stream()
            .filter(s -> s.status == InventoryStatus.LOW)
            .count();

        if (criticalCount > 0) {
            badgeCritical.setVisible(true);
            badgeCritical.setManaged(true);
            lblCriticalCount.setText(criticalCount + " critical");
        } else {
            badgeCritical.setVisible(false);
            badgeCritical.setManaged(false);
        }

        if (lowCount > 0) {
            badgeLow.setVisible(true);
            badgeLow.setManaged(true);
            lblLowCount.setText(lowCount + " low");
        } else {
            badgeLow.setVisible(false);
            badgeLow.setManaged(false);
        }
    }

    // ==================== GRID BUILDING ====================

    private void buildBloodTypeGrid(List<BloodInventory> inventories) {
        bloodTypeGrid.getChildren().clear();

        Map<String, BloodTypeSummary> summaries = aggregateByBloodType(
            inventories
        );

        int col = 0;
        int row = 0;
        for (String bloodType : BLOOD_TYPE_ORDER) {
            BloodTypeSummary summary = summaries.getOrDefault(
                bloodType,
                new BloodTypeSummary(
                    bloodType,
                    0,
                    null,
                    InventoryStatus.CRITICAL
                )
            );

            VBox card = createBloodTypeCard(summary);
            bloodTypeGrid.add(card, col, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }

    /**
     * Creates a single blood type card matching the screenshot design:
     * - Blood type badge (colored) + Status badge (Optimal/Low/Critical)
     * - Stock row with quantity
     * - Progress bar
     * - Expiry date row with days remaining
     * - Card border color depends on status
     */
    private VBox createBloodTypeCard(BloodTypeSummary summary) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.getStyleClass().add("inv-blood-card");

        // Determine status-based colors
        String borderColor;
        String statusText;
        String statusBgColor;
        String statusTextColor;
        String progressBarColor;

        switch (summary.status) {
            case OPTIMAL:
                borderColor = "#e2e8f0"; // default subtle border
                statusText = "Optimal";
                statusBgColor = "#dcfce7";
                statusTextColor = "#15803d";
                progressBarColor = "#16a34a";
                break;
            case LOW:
                borderColor = "#fbbf24"; // yellow border
                statusText = "Low";
                statusBgColor = "#fef9c3";
                statusTextColor = "#a16207";
                progressBarColor = "#f59e0b";
                break;
            case CRITICAL:
                borderColor = "#ef4444"; // red border
                statusText = "Critical";
                statusBgColor = "#fee2e2";
                statusTextColor = "#dc2626";
                progressBarColor = "#ef4444";
                break;
            case EXPIRED:
                borderColor = "#6b7280"; // gray border
                statusText = "Expired";
                statusBgColor = "#f3f4f6";
                statusTextColor = "#6b7280";
                progressBarColor = "#6b7280";
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
            summary.status == InventoryStatus.LOW ||
            summary.status == InventoryStatus.CRITICAL ||
            summary.status == InventoryStatus.EXPIRED
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

        // Blood type colored badge
        Label bloodTypeBadge = new Label(summary.bloodType);
        bloodTypeBadge.setStyle(
            "-fx-background-color: " +
                progressBarColor +
                "; -fx-background-radius: 8; " +
                "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-padding: 4 10 4 10; -fx-min-width: 42; -fx-alignment: center;"
        );

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // Status badge
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

        Label stockValue = new Label(summary.totalQuantity + " units");
        stockValue.setStyle(
            "-fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-font-weight: bold;"
        );

        stockRow
            .getChildren()
            .addAll(stockIcon, stockLabel, spacer2, stockValue);

        // ---- Row 3: Progress Bar ----
        double progress = Math.min(
            1.0,
            (double) summary.totalQuantity / MAX_STOCK_FOR_BAR
        );

        StackPane progressBarContainer = new StackPane();
        progressBarContainer.setAlignment(Pos.CENTER_LEFT);
        progressBarContainer.setPrefHeight(6);
        progressBarContainer.setMinHeight(6);
        progressBarContainer.setMaxHeight(6);

        // Background track
        Region track = new Region();
        track.setStyle(
            "-fx-background-color: #f1f5f9; -fx-background-radius: 3;"
        );
        track.setMaxWidth(Double.MAX_VALUE);
        track.setPrefHeight(6);

        // Fill bar
        Region fill = new Region();
        fill.setStyle(
            "-fx-background-color: " +
                progressBarColor +
                "; -fx-background-radius: 3;"
        );
        fill.setPrefHeight(6);
        fill.setMaxHeight(6);

        progressBarContainer.getChildren().addAll(track, fill);

        // Bind fill width to a fraction of the container width
        fill
            .maxWidthProperty()
            .bind(progressBarContainer.widthProperty().multiply(progress));

        // ---- Row 4: Expiry Row ----
        HBox expiryRow = new HBox();
        expiryRow.setAlignment(Pos.CENTER_LEFT);
        expiryRow.setSpacing(6);
        expiryRow.setPadding(new Insets(2, 0, 0, 0));

        Label expiryIcon = new Label("⏱");
        expiryIcon.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        String expiryText = "—";
        String daysText = "";
        String daysColor = "#64748b";

        if (summary.nearestExpiry != null) {
            LocalDate expiryDate = summary.nearestExpiry.toLocalDate();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            expiryText = "Expires " + expiryDate.format(fmt);

            long daysUntil = ChronoUnit.DAYS.between(
                LocalDate.now(),
                expiryDate
            );
            daysText = (daysUntil >= 0 ? "" : "") + daysUntil + "d";

            if (daysUntil <= 0) {
                daysColor = "#dc2626"; // red - expired or expiring today
            } else if (daysUntil <= 3) {
                daysColor = "#f59e0b"; // yellow - expiring soon
            } else {
                daysColor = "#16a34a"; // green - plenty of time
            }
        }

        Label expiryLabel = new Label(expiryText);
        expiryLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");

        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);

        Label daysLabel = new Label(daysText);
        daysLabel.setStyle(
            "-fx-text-fill: " +
                daysColor +
                "; -fx-font-size: 12px; -fx-font-weight: bold;"
        );

        expiryRow
            .getChildren()
            .addAll(expiryIcon, expiryLabel, spacer3, daysLabel);

        // ---- Assemble Card ----
        card
            .getChildren()
            .addAll(topRow, stockRow, progressBarContainer, expiryRow);

        return card;
    }

    // ==================== DATA AGGREGATION ====================

    /**
     * Groups inventory items by blood type and computes:
     * - Total quantity (sum of quantityUnitsInt)
     * - Nearest expiration date
     * - Overall status (worst status among items, or derived from quantity)
     */
    private Map<String, BloodTypeSummary> aggregateByBloodType(
        List<BloodInventory> inventories
    ) {
        Map<String, List<BloodInventory>> grouped = inventories
            .stream()
            .filter(inv -> inv.getBloodTypeId() != null)
            .collect(Collectors.groupingBy(BloodInventory::getBloodTypeId));

        Map<String, BloodTypeSummary> summaries = new LinkedHashMap<>();

        for (Map.Entry<
            String,
            List<BloodInventory>
        > entry : grouped.entrySet()) {
            String bloodType = entry.getKey();
            List<BloodInventory> items = entry.getValue();

            int totalQty = items
                .stream()
                .mapToInt(inv ->
                    inv.getQuantityUnitsInt() != null
                        ? inv.getQuantityUnitsInt()
                        : 0
                )
                .sum();

            // Find nearest expiration date
            Date nearestExpiry = items
                .stream()
                .map(BloodInventory::getExpirationDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

            // Determine overall status - use the worst status present, or derive from quantity
            InventoryStatus worstStatus = deriveStatus(items, totalQty);

            summaries.put(
                bloodType,
                new BloodTypeSummary(
                    bloodType,
                    totalQty,
                    nearestExpiry,
                    worstStatus
                )
            );
        }

        return summaries;
    }

    /**
     * Derives the overall status for a blood type group.
     * Uses explicit status from items if available, otherwise derives from total quantity.
     */
    private InventoryStatus deriveStatus(
        List<BloodInventory> items,
        int totalQty
    ) {
        // Check if any item has an explicit status set
        boolean hasCritical = items
            .stream()
            .anyMatch(i -> i.getStatus() == InventoryStatus.CRITICAL);
        boolean hasExpired = items
            .stream()
            .anyMatch(i -> i.getStatus() == InventoryStatus.EXPIRED);
        boolean hasLow = items
            .stream()
            .anyMatch(i -> i.getStatus() == InventoryStatus.LOW);

        if (hasExpired) return InventoryStatus.EXPIRED;
        if (hasCritical) return InventoryStatus.CRITICAL;
        if (hasLow) return InventoryStatus.LOW;

        // Derive from quantity thresholds
        if (totalQty <= 5) return InventoryStatus.CRITICAL;
        if (totalQty <= 15) return InventoryStatus.LOW;
        return InventoryStatus.OPTIMAL;
    }

    // ==================== QUICK ACTION HANDLERS ====================

    @FXML
    private void onUrgentAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Urgent Alert");
        alert.setHeaderText("Urgent Notification");
        alert.setContentText(
            "This feature will send an urgent notification to all eligible donors."
        );
        alert.showAndWait();
    }

    @FXML
    private void onRequestTransfer() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Transfer");
        alert.setHeaderText("Inter-hospital Transfer");
        alert.setContentText(
            "This feature will allow you to request a stock transfer from another hospital."
        );
        alert.showAndWait();
    }

    @FXML
    private void onAddStock() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add Stock");
        alert.setHeaderText("Register New Blood Units");
        alert.setContentText(
            "This feature will allow you to register new blood units into the inventory."
        );
        alert.showAndWait();
    }

    @FXML
    private void onGenerateReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Generate Report");
        alert.setHeaderText("Data Export");
        alert.setContentText(
            "This feature will allow you to generate and export an inventory report."
        );
        alert.showAndWait();
    }

    // ==================== INNER CLASS ====================

    /**
     * Holds aggregated data for a single blood type to display in the grid.
     */
    private static class BloodTypeSummary {

        final String bloodType;
        final int totalQuantity;
        final Date nearestExpiry;
        final InventoryStatus status;

        BloodTypeSummary(
            String bloodType,
            int totalQuantity,
            Date nearestExpiry,
            InventoryStatus status
        ) {
            this.bloodType = bloodType;
            this.totalQuantity = totalQuantity;
            this.nearestExpiry = nearestExpiry;
            this.status = status != null ? status : InventoryStatus.CRITICAL;
        }
    }
}
