package tn.edu.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.edu.esprit.entities.BloodTransferRequest;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.entities.HospitalStaff;
import tn.edu.esprit.entities.TransfertStatus;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.HospitalServiceImpl;
import tn.edu.esprit.services.ServiceHospitalStaff;
import tn.edu.esprit.services.TransfertServiceImpl;

public class TransferListController implements Initializable {

    @FXML
    private VBox cardsContainer;

    @FXML
    private VBox emptyState;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextField tfSearch;

    @FXML
    private ComboBox<String> cbStatusFilter;

    @FXML
    private ComboBox<String> cbBloodTypeFilter;

    @FXML
    private Button btnNewTransfer;

    @FXML
    private Label lblTotalCount;

    @FXML
    private Label lblPendingCount;

    @FXML
    private Label lblApprovedCount;

    @FXML
    private Label lblInTransitCount;

    @FXML
    private Label lblDeliveredCount;

    private final TransfertServiceImpl transferService =
        new TransfertServiceImpl();
    private final HospitalServiceImpl hospitalService =
        new HospitalServiceImpl();
    private final ServiceHospitalStaff hospitalStaffService =
        new ServiceHospitalStaff();

    private List<BloodTransferRequest> allTransfers;
    private Map<UUID, Hospital> hospitalCache = new HashMap<>();
    private UUID currentUserHospitalId;

    private static final String[] BLOOD_TYPES = {
        "A+",
        "A-",
        "B+",
        "B-",
        "AB+",
        "AB-",
        "O+",
        "O-",
    };

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Build hospital lookup cache
        loadHospitalCache();

        // Get current user's hospital ID for permission checks
        Users currentUser = AppSession.getCurrentUser();
        if (
            currentUser != null &&
            currentUser.getUserType() == UserType.HOSPITAL_STAFF
        ) {
            // Try to get hospital ID from current user - will be null for system admins
            currentUserHospitalId = null; // Will be determined on-demand via transfer's approving_hospital_id
        }

        // Populate status filter
        cbStatusFilter.getItems().add("All statuses");
        cbStatusFilter.getItems().add("Pending");
        cbStatusFilter.getItems().add("Approved");
        cbStatusFilter.getItems().add("In Transit");
        cbStatusFilter.getItems().add("Delivered");
        cbStatusFilter.getItems().add("Cancelled");
        cbStatusFilter.getItems().add("Denied");
        cbStatusFilter.setValue("All statuses");

        // Populate blood type filter
        cbBloodTypeFilter.getItems().add("All blood types");
        cbBloodTypeFilter.getItems().addAll(Arrays.asList(BLOOD_TYPES));
        cbBloodTypeFilter.setValue("All blood types");

        refreshData();
    }

    // ==================== DATA LOADING ====================

    @SuppressWarnings("unchecked")
    public void refreshData() {
        allTransfers = transferService.getAllTransferts();
        // Sort by most recent first
        allTransfers.sort((a, b) -> {
            Timestamp ta = a.getRequestedAt();
            Timestamp tb = b.getRequestedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        applyFilters();
    }

    private void loadHospitalCache() {
        try {
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            hospitalCache.clear();
            for (Hospital h : hospitals) {
                if (h.getHospitalId() != null) {
                    hospitalCache.put(h.getHospitalId(), h);
                }
            }
        } catch (Exception e) {
            System.err.println(
                "Error loading hospitals for cache: " + e.getMessage()
            );
        }
    }

    private String getHospitalName(UUID hospitalId) {
        if (hospitalId == null) return "—";
        Hospital h = hospitalCache.get(hospitalId);
        return h != null
            ? h.getName()
            : hospitalId.toString().substring(0, 8) + "...";
    }

    // ==================== FILTERING ====================

    @FXML
    private void onSearchChanged() {
        applyFilters();
    }

    @FXML
    private void onStatusFilterChanged() {
        applyFilters();
    }

    @FXML
    private void onBloodTypeFilterChanged() {
        applyFilters();
    }

    private void applyFilters() {
        String searchText =
            tfSearch.getText() != null
                ? tfSearch.getText().trim().toLowerCase()
                : "";
        String statusFilter = cbStatusFilter.getValue();
        String bloodTypeFilter = cbBloodTypeFilter.getValue();

        List<BloodTransferRequest> filtered = allTransfers
            .stream()
            .filter(t -> matchesStatusFilter(t, statusFilter))
            .filter(t -> matchesBloodTypeFilter(t, bloodTypeFilter))
            .filter(t -> matchesSearch(t, searchText))
            .collect(Collectors.toList());

        updateStats(allTransfers);
        buildCards(filtered);
    }

    private boolean matchesStatusFilter(BloodTransferRequest t, String filter) {
        if (filter == null || filter.equals("All statuses")) return true;
        TransfertStatus status = t.getStatus();
        if (status == null) return false;
        switch (filter) {
            case "Pending":
                return status == TransfertStatus.PENDING;
            case "Approved":
                return status == TransfertStatus.APPROVED;
            case "In Transit":
                return status == TransfertStatus.IN_TRANSIT;
            case "Delivered":
                return status == TransfertStatus.DELIVERED;
            case "Cancelled":
                return status == TransfertStatus.CANCELLED;
            case "Declined":
                return status == TransfertStatus.DENIED;
            default:
                return true;
        }
    }

    private boolean matchesBloodTypeFilter(
        BloodTransferRequest t,
        String filter
    ) {
        if (filter == null || filter.equals("All blood types")) return true;
        return filter.equals(t.getBloodTypeId());
    }

    private boolean matchesSearch(BloodTransferRequest t, String searchText) {
        if (searchText.isEmpty()) return true;
        String requestingName = getHospitalName(
            t.getRequestingHospitalId()
        ).toLowerCase();
        String approvingName = getHospitalName(
            t.getApprovingHospitalId()
        ).toLowerCase();
        String bloodType =
            t.getBloodTypeId() != null ? t.getBloodTypeId().toLowerCase() : "";
        String reason =
            t.getReason() != null ? t.getReason().toLowerCase() : "";
        return (
            requestingName.contains(searchText) ||
            approvingName.contains(searchText) ||
            bloodType.contains(searchText) ||
            reason.contains(searchText)
        );
    }

    // ==================== STATS ====================

    private void updateStats(List<BloodTransferRequest> transfers) {
        lblTotalCount.setText(String.valueOf(transfers.size()));

        long pending = transfers
            .stream()
            .filter(t -> t.getStatus() == TransfertStatus.PENDING)
            .count();
        long approved = transfers
            .stream()
            .filter(t -> t.getStatus() == TransfertStatus.APPROVED)
            .count();
        long inTransit = transfers
            .stream()
            .filter(t -> t.getStatus() == TransfertStatus.IN_TRANSIT)
            .count();
        long delivered = transfers
            .stream()
            .filter(t -> t.getStatus() == TransfertStatus.DELIVERED)
            .count();

        lblPendingCount.setText(String.valueOf(pending));
        lblApprovedCount.setText(String.valueOf(approved));
        lblInTransitCount.setText(String.valueOf(inTransit));
        lblDeliveredCount.setText(String.valueOf(delivered));
    }

    // ==================== CARD BUILDING ====================

    private void buildCards(List<BloodTransferRequest> transfers) {
        cardsContainer.getChildren().clear();

        if (transfers.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            cardsContainer.getChildren().add(emptyState);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        for (BloodTransferRequest transfer : transfers) {
            VBox card = createTransferCard(transfer);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createTransferCard(BloodTransferRequest transfer) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.getStyleClass().add("hospital-card");

        // Determine status styling
        String statusText = getStatusText(transfer.getStatus());
        String statusBg = getStatusBgColor(transfer.getStatus());
        String statusTextColor = getStatusTextColor(transfer.getStatus());
        String statusDotColor = getStatusDotColor(transfer.getStatus());
        String cardLeftBorder = getCardLeftBorderColor(transfer.getStatus());

        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 4; -fx-border-radius: 12; " +
                "-fx-border-color: " +
                cardLeftBorder +
                " #e2e8f0 #e2e8f0 " +
                cardLeftBorder +
                "; " +
                "-fx-border-width: 1 1 1 4; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        VBox body = new VBox();
        body.setSpacing(12);
        body.setPadding(new Insets(18, 20, 14, 20));

        // ---- Row 1: Hospital flow + Status + Blood type ----
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setSpacing(10);

        // Transfer icon
        Label transferIcon = new Label("\uD83D\uDD04");
        transferIcon.setStyle("-fx-font-size: 16px;");

        // Requesting hospital
        String reqName = getHospitalName(transfer.getRequestingHospitalId());
        Label lblRequesting = new Label(reqName);
        lblRequesting.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;"
        );
        lblRequesting.setMaxWidth(200);
        lblRequesting.setEllipsisString("...");

        // Arrow
        Label arrow = new Label("\u2192");
        arrow.setStyle(
            "-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;"
        );

        // Approving hospital
        String appName = getHospitalName(transfer.getApprovingHospitalId());
        Label lblApproving = new Label(appName);
        lblApproving.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;"
        );
        lblApproving.setMaxWidth(200);
        lblApproving.setEllipsisString("...");

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);

        // Status badge
        Label statusBadge = new Label(statusText);
        statusBadge.setStyle(
            "-fx-background-color: " +
                statusBg +
                "; -fx-background-radius: 12; " +
                "-fx-text-fill: " +
                statusTextColor +
                "; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-padding: 4 12 4 12;"
        );

        // Blood type badge
        Label bloodBadge = new Label(
            transfer.getBloodTypeId() != null ? transfer.getBloodTypeId() : "—"
        );
        bloodBadge.setStyle(
            "-fx-background-color: #dc2626; -fx-background-radius: 8; " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 3 10 3 10; -fx-min-width: 36; -fx-alignment: center;"
        );

        // Quantity badge - show approved quantity if available, otherwise requested
        String qtyText;
        String qtyBadgeStyle;

        if (
            transfer.getQuantityUnitsApproved() != null &&
            transfer.getQuantityUnitsApproved() > 0
        ) {
            // Show approved quantity
            qtyText = transfer.getQuantityUnitsApproved() + " u.";

            // Highlight partial approvals with orange/amber color
            if (
                transfer.getQuantityUnitsRequested() != null &&
                transfer.getQuantityUnitsApproved() <
                transfer.getQuantityUnitsRequested()
            ) {
                qtyBadgeStyle =
                    "-fx-background-color: #fed7aa; -fx-background-radius: 8; " +
                    "-fx-text-fill: #b45309; -fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-padding: 3 10 3 10;";
            } else {
                qtyBadgeStyle =
                    "-fx-background-color: #f1f5f9; -fx-background-radius: 8; " +
                    "-fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-padding: 3 10 3 10;";
            }
        } else {
            // No approval yet, show requested quantity
            qtyText =
                (transfer.getQuantityUnitsRequested() != null
                    ? transfer.getQuantityUnitsRequested()
                    : 0) +
                " u.";
            qtyBadgeStyle =
                "-fx-background-color: #f1f5f9; -fx-background-radius: 8; " +
                "-fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 3 10 3 10;";
        }

        Label qtyBadge = new Label(qtyText);
        qtyBadge.setStyle(qtyBadgeStyle);

        topRow
            .getChildren()
            .addAll(
                transferIcon,
                lblRequesting,
                arrow,
                lblApproving,
                spacerTop,
                statusBadge,
                bloodBadge,
                qtyBadge
            );

        // ---- Row 2: Reason ----
        HBox reasonRow = new HBox();
        reasonRow.setAlignment(Pos.CENTER_LEFT);
        reasonRow.setSpacing(8);

        String reasonStr =
            transfer.getReason() != null && !transfer.getReason().isEmpty()
                ? transfer.getReason()
                : "No reason specified";
        Label reasonLabel = new Label(reasonStr);
        reasonLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12.5px;");
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(reasonLabel, Priority.ALWAYS);
        reasonRow.getChildren().add(reasonLabel);

        // ---- Row 3: Date info ----
        HBox dateRow = new HBox();
        dateRow.setAlignment(Pos.CENTER_LEFT);
        dateRow.setSpacing(16);
        dateRow.setPadding(new Insets(2, 0, 0, 0));

        // Requested at
        if (transfer.getRequestedAt() != null) {
            HBox requestedBox = createDateChip(
                "\uD83D\uDCC5",
                "Requested on " +
                    transfer.getRequestedAt().toLocalDateTime().format(DATE_FMT)
            );
            dateRow.getChildren().add(requestedBox);
        }

        // Expected delivery
        if (transfer.getDeliveryExpectedAt() != null) {
            HBox expectedBox = createDateChip(
                "\uD83D\uDE9A",
                "Expected delivery " +
                    transfer
                        .getDeliveryExpectedAt()
                        .toLocalDateTime()
                        .format(DATE_FMT)
            );
            dateRow.getChildren().add(expectedBox);
        }

        // Actual delivery
        if (transfer.getActualDeliveryAt() != null) {
            HBox deliveredBox = createDateChip(
                "\u2705",
                "Delivered on " +
                    transfer
                        .getActualDeliveryAt()
                        .toLocalDateTime()
                        .format(DATE_FMT)
            );
            dateRow.getChildren().add(deliveredBox);
        }

        // Approved quantity if differs from requested
        if (
            transfer.getQuantityUnitsApproved() != null &&
            transfer.getQuantityUnitsApproved() > 0 &&
            !transfer
                .getQuantityUnitsApproved()
                .equals(transfer.getQuantityUnitsRequested())
        ) {
            HBox approvedQtyBox = createDateChip(
                "\u2696",
                "Approved: " + transfer.getQuantityUnitsApproved() + " units"
            );
            dateRow.getChildren().add(approvedQtyBox);
        }

        body.getChildren().addAll(topRow, reasonRow, dateRow);

        // ---- Notes row (if present) ----
        if (
            transfer.getNotes() != null && !transfer.getNotes().trim().isEmpty()
        ) {
            HBox notesRow = new HBox();
            notesRow.setAlignment(Pos.CENTER_LEFT);
            notesRow.setSpacing(6);
            Label notesIcon = new Label("\uD83D\uDCDD");
            notesIcon.setStyle("-fx-font-size: 11px;");
            Label notesLabel = new Label(transfer.getNotes());
            notesLabel.setStyle(
                "-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-font-style: italic;"
            );
            notesLabel.setWrapText(true);
            notesLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(notesLabel, Priority.ALWAYS);
            notesRow.getChildren().addAll(notesIcon, notesLabel);
            body.getChildren().add(notesRow);
        }

        // ---- Divider + Actions row ----
        Separator divider = new Separator();
        divider.getStyleClass().add("card-divider");

        HBox actionsRow = new HBox();
        actionsRow.setAlignment(Pos.CENTER_RIGHT);
        actionsRow.setSpacing(8);
        actionsRow.setPadding(new Insets(10, 20, 14, 20));

        // Get current user for permission checking
        Users currentUser = AppSession.getCurrentUser();

        // Show edit button only if user has permission
        if (canEdit(transfer, currentUser)) {
            Button btnEdit = new Button("\u270F  Edit");
            btnEdit.getStyleClass().add("btn-card-edit");
            btnEdit.setOnAction(e -> onEditTransfer(transfer));
            actionsRow.getChildren().add(btnEdit);
        }

        // Show delete button only if user has permission to cancel
        if (canCancel(transfer, currentUser)) {
            Button btnDelete = new Button("\uD83D\uDDD1  Delete");
            btnDelete.getStyleClass().add("btn-card-delete");
            btnDelete.setOnAction(e -> onDeleteTransfer(transfer));
            actionsRow.getChildren().add(btnDelete);
        }

        // Status action buttons based on current state and permissions
        if (transfer.getStatus() == TransfertStatus.PENDING) {
            // Show approve button only for approving staff and admins
            if (canApprove(transfer, currentUser)) {
                Button btnApprove = new Button("\u2705 Approve");
                btnApprove.setStyle(
                    "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
                );
                btnApprove.setOnAction(e -> onApproveTransferDialog(transfer));
                actionsRow.getChildren().add(btnApprove);
            }

            // Show decline button only for approving staff and admins
            if (canApprove(transfer, currentUser)) {
                Button btnDecline = new Button("\u26A0  Decline");
                btnDecline.setStyle(
                    "-fx-background-color: #fecaca; -fx-text-fill: #7f1d1d; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
                );
                btnDecline.setOnAction(e -> onDeclineTransferDialog(transfer));
                actionsRow.getChildren().add(btnDecline);
            }

            // Show cancel button only for requesting staff and admins
            if (canCancel(transfer, currentUser)) {
                Button btnCancel = new Button("\u274C Cancel");
                btnCancel.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
                );
                btnCancel.setOnAction(e -> onCancelTransfer(transfer));
                actionsRow.getChildren().add(btnCancel);
            }
        } else if (
            transfer.getStatus() == TransfertStatus.APPROVED ||
            transfer.getStatus() == TransfertStatus.IN_TRANSIT
        ) {
            // Show in transit button only for approving staff and admins (from APPROVED state)
            if (
                transfer.getStatus() == TransfertStatus.APPROVED &&
                canMarkInTransit(transfer, currentUser)
            ) {
                Button btnShip = new Button("\uD83D\uDE9A In Transit");
                btnShip.setStyle(
                    "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
                );
                btnShip.setOnAction(e -> onMarkInTransit(transfer));
                actionsRow.getChildren().add(btnShip);
            }

            // Show deliver button only for authorized users (from IN_TRANSIT state)
            if (
                transfer.getStatus() == TransfertStatus.IN_TRANSIT &&
                canMarkDelivered(transfer, currentUser)
            ) {
                Button btnDeliver = new Button("\uD83D\uDCE6 Mark Delivered");
                btnDeliver.setStyle(
                    "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
                );
                btnDeliver.setOnAction(e -> onMarkDelivered(transfer));
                actionsRow.getChildren().add(btnDeliver);
            }

            // Show cancel button for APPROVED or IN_TRANSIT state (for requesting staff or admins)
            if (canCancel(transfer, currentUser)) {
                Button btnCancel = new Button("\u274C Cancel");
                btnCancel.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 12px; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
                );
                btnCancel.setOnAction(e -> onCancelTransfer(transfer));
                actionsRow.getChildren().add(btnCancel);
            }
        }

        card.getChildren().addAll(body, divider, actionsRow);
        return card;
    }

    private HBox createDateChip(String icon, String text) {
        HBox chip = new HBox();
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setSpacing(4);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px;");

        chip.getChildren().addAll(iconLabel, textLabel);
        return chip;
    }

    private String getStatusText(TransfertStatus status) {
        if (status == null) {
            return "Unknown";
        }

        switch (status) {
            case PENDING:
                return "Pending";
            case APPROVED:
                return "Approved";
            case IN_TRANSIT:
                return "In Transit";
            case DELIVERED:
                return "Delivered";
            case CANCELLED:
                return "Cancelled";
            case DENIED:
                return "Denied";
            default:
                return status.name();
        }
    }

    private String getStatusBgColor(TransfertStatus status) {
        if (status == null) return "#f3f4f6";
        switch (status) {
            case PENDING:
                return "#fef9c3";
            case APPROVED:
                return "#dbeafe";
            case IN_TRANSIT:
                return "#e0e7ff";
            case DELIVERED:
                return "#dcfce7";
            case CANCELLED:
                return "#fee2e2";
            case DENIED:
                return "#fecaca";
            default:
                return "#f3f4f6";
        }
    }

    private String getStatusTextColor(TransfertStatus status) {
        if (status == null) return "#6b7280";
        switch (status) {
            case PENDING:
                return "#a16207";
            case APPROVED:
                return "#1d4ed8";
            case IN_TRANSIT:
                return "#4338ca";
            case DELIVERED:
                return "#15803d";
            case CANCELLED:
                return "#dc2626";
            case DENIED:
                return "#7f1d1d";
            default:
                return "#6b7280";
        }
    }

    private String getStatusDotColor(TransfertStatus status) {
        if (status == null) return "#9ca3af";
        switch (status) {
            case PENDING:
                return "#eab308";
            case APPROVED:
                return "#3b82f6";
            case IN_TRANSIT:
                return "#6366f1";
            case DELIVERED:
                return "#22c55e";
            case CANCELLED:
                return "#ef4444";
            case DENIED:
                return "#dc2626";
            default:
                return "#9ca3af";
        }
    }

    private String getCardLeftBorderColor(TransfertStatus status) {
        if (status == null) return "#d1d5db";
        switch (status) {
            case PENDING:
                return "#eab308";
            case APPROVED:
                return "#3b82f6";
            case IN_TRANSIT:
                return "#818cf8";
            case DELIVERED:
                return "#22c55e";
            case CANCELLED:
                return "#ef4444";
            case DENIED:
                return "#dc2626";
            default:
                return "#d1d5db";
        }
    }

    // ==================== ACTIONS ====================

    @FXML
    private void onNewTransfer() {
        openTransferForm(null);
    }

    private void onEditTransfer(BloodTransferRequest transfer) {
        openTransferForm(transfer);
    }

    private void onDeleteTransfer(BloodTransferRequest transfer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Transfer");
        confirm.setHeaderText("Confirm Deletion");
        confirm.setContentText(
            "Are you sure you want to delete transfer request #" +
                transfer.getTransferId() +
                "?"
        );

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                transferService.supprimer(transfer.getTransferId());
                refreshData();
            } catch (Exception e) {
                showError("Error during deletion: " + e.getMessage());
            }
        }
    }

    private void onApproveTransferDialog(BloodTransferRequest transfer) {
        try {
            // Validate transfer ID exists
            if (transfer == null || transfer.getTransferId() == null) {
                showError(
                    "Error: Transfer ID is missing. Cannot proceed with approval."
                );
                return;
            }

            // Fetch fresh transfer data from database
            Integer transferId = transfer.getTransferId();
            BloodTransferRequest currentTransfer =
                (BloodTransferRequest) transferService.getTransfertById(
                    transferId
                );

            if (currentTransfer == null) {
                showError(
                    "Error: Transfer not found in database. It may have been deleted."
                );
                return;
            }

            // Validate transfer is still in PENDING status
            if (currentTransfer.getStatus() != TransfertStatus.PENDING) {
                showError(
                    "Error: Transfer is no longer in PENDING status. Current status: " +
                        currentTransfer.getStatus()
                );
                return;
            }

            transfer = currentTransfer;

            // Create approval dialog
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Approve Transfer Request");
            dialog.setHeaderText("Approve Blood Transfer Request");

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));

            // Hospital info
            Label lblReqHospital = new Label(
                "Requesting Hospital: " +
                    getHospitalName(transfer.getRequestingHospitalId())
            );
            lblReqHospital.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px;"
            );

            Label lblBloodType = new Label(
                "Blood Type: " + transfer.getBloodTypeId()
            );
            lblBloodType.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #dc2626;"
            );

            Label lblQuantityRequested = new Label(
                "Quantity Requested: " +
                    transfer.getQuantityUnitsRequested() +
                    " units"
            );
            lblQuantityRequested.setStyle("-fx-font-size: 12px;");

            // Quantity selection
            Label lblQuantityToApprove = new Label("Quantity to Approve:");
            lblQuantityToApprove.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px;"
            );

            Spinner<Integer> spApprovedQuantity = new Spinner<>(
                1,
                transfer.getQuantityUnitsRequested(),
                transfer.getQuantityUnitsRequested()
            );
            spApprovedQuantity.setPrefWidth(100);
            spApprovedQuantity.setEditable(true);

            Label lblQuantityInfo = new Label(
                "Note: You can approve a quantity equal to or less than the requested amount."
            );
            lblQuantityInfo.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-wrap-text: true;"
            );
            lblQuantityInfo.setWrapText(true);

            // Reason display
            Label lblReason = new Label(
                "Reason: " +
                    (transfer.getReason() != null
                        ? transfer.getReason()
                        : "No reason specified")
            );
            lblReason.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            lblReason.setWrapText(true);

            // Notes field
            Label lblNotesLabel = new Label("Approval Notes (optional):");
            lblNotesLabel.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px;"
            );

            TextArea taApprovalNotes = new TextArea();
            taApprovalNotes.setPrefRowCount(3);
            taApprovalNotes.setWrapText(true);
            taApprovalNotes.setStyle("-fx-control-inner-background: #f8fafc;");

            content
                .getChildren()
                .addAll(
                    lblReqHospital,
                    lblBloodType,
                    lblQuantityRequested,
                    new Separator(),
                    lblQuantityToApprove,
                    spApprovedQuantity,
                    lblQuantityInfo,
                    lblReason,
                    new Separator(),
                    lblNotesLabel,
                    taApprovalNotes
                );

            dialog.getDialogPane().setContent(content);
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

            // Set result converter to extract spinner value when OK is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    return spApprovedQuantity.getValue();
                }
                return null;
            });

            // Handle result
            Optional<Integer> result = dialog.showAndWait();
            if (result.isPresent()) {
                Integer approvedQuantity = result.get();

                // Update the transfer with approval data
                transfer.setStatus(TransfertStatus.APPROVED);
                transfer.setQuantityUnitsApproved(approvedQuantity);
                transfer.setApprovingStaffId(
                    AppSession.getCurrentUser().getId()
                );
                transfer.setApprovedAt(
                    new Timestamp(System.currentTimeMillis())
                );

                // Preserve existing notes or add new ones
                String newNotes = taApprovalNotes.getText().trim();
                if (!newNotes.isEmpty()) {
                    transfer.setNotes(newNotes);
                }

                System.out.println(
                    "Approving transfer ID: " +
                        transfer.getTransferId() +
                        " with approved quantity: " +
                        approvedQuantity
                );

                transferService.modifier(transfer);
                refreshData();
            }
        } catch (Exception e) {
            showError("Error during approval: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onDeclineTransferDialog(BloodTransferRequest transfer) {
        try {
            // Validate transfer ID exists
            if (transfer == null || transfer.getTransferId() == null) {
                showError(
                    "Error: Transfer ID is missing. Cannot proceed with decline."
                );
                return;
            }

            // Fetch fresh transfer data from database
            Integer transferId = transfer.getTransferId();
            BloodTransferRequest currentTransfer =
                (BloodTransferRequest) transferService.getTransfertById(
                    transferId
                );

            if (currentTransfer == null) {
                showError(
                    "Error: Transfer not found in database. It may have been deleted."
                );
                return;
            }

            // Validate transfer is still in PENDING status
            if (currentTransfer.getStatus() != TransfertStatus.PENDING) {
                showError(
                    "Error: Transfer is no longer in PENDING status. Current status: " +
                        currentTransfer.getStatus()
                );
                return;
            }

            transfer = currentTransfer;

            // Create decline dialog
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Decline Transfer Request");
            dialog.setHeaderText("Decline Blood Transfer Request");

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));

            // Hospital info
            Label lblReqHospital = new Label(
                "Requesting Hospital: " +
                    getHospitalName(transfer.getRequestingHospitalId())
            );
            lblReqHospital.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px;"
            );

            Label lblBloodType = new Label(
                "Blood Type: " + transfer.getBloodTypeId()
            );
            lblBloodType.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #dc2626;"
            );

            Label lblQuantityRequested = new Label(
                "Quantity Requested: " +
                    transfer.getQuantityUnitsRequested() +
                    " units"
            );
            lblQuantityRequested.setStyle("-fx-font-size: 12px;");

            // Reason display
            Label lblReason = new Label(
                "Request Reason: " +
                    (transfer.getReason() != null
                        ? transfer.getReason()
                        : "No reason specified")
            );
            lblReason.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            lblReason.setWrapText(true);

            // Decline reason field
            Label lblDeclineReasonLabel = new Label(
                "Reason for Declining (required):"
            );
            lblDeclineReasonLabel.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px;"
            );

            TextArea taDeclineReason = new TextArea();
            taDeclineReason.setPrefRowCount(4);
            taDeclineReason.setWrapText(true);
            taDeclineReason.setStyle("-fx-control-inner-background: #f8fafc;");

            content
                .getChildren()
                .addAll(
                    lblReqHospital,
                    lblBloodType,
                    lblQuantityRequested,
                    lblReason,
                    new Separator(),
                    lblDeclineReasonLabel,
                    taDeclineReason
                );

            dialog.getDialogPane().setContent(content);
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

            // Set result converter to extract decline reason when OK is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    String reason = taDeclineReason.getText().trim();
                    if (reason.isEmpty()) {
                        showError(
                            "Please provide a reason for declining the request."
                        );
                        return null;
                    }
                    return reason;
                }
                return null;
            });

            // Handle result
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String declineReason = result.get();

                if (declineReason == null || declineReason.isEmpty()) {
                    return;
                }

                // Update the transfer with decline data
                transfer.setStatus(TransfertStatus.DENIED);
                transfer.setApprovingStaffId(
                    AppSession.getCurrentUser().getId()
                );
                transfer.setNotes("Declined: " + declineReason);

                System.out.println(
                    "Declining transfer ID: " +
                        transfer.getTransferId() +
                        " with reason: " +
                        declineReason
                );

                transferService.modifier(transfer);
                refreshData();
            }
        } catch (Exception e) {
            showError("Error during decline: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onCancelTransfer(BloodTransferRequest transfer) {
        try {
            if (transfer == null || transfer.getTransferId() == null) {
                showError("Error: Transfer ID is missing. Cannot cancel.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Cancel Transfer");
            confirm.setHeaderText("Confirm Cancellation");
            confirm.setContentText(
                "Are you sure you want to cancel this transfer request? This action cannot be undone."
            );

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Fetch fresh data to ensure we have the latest state
                BloodTransferRequest currentTransfer =
                    (BloodTransferRequest) transferService.getTransfertById(
                        transfer.getTransferId()
                    );

                if (currentTransfer == null) {
                    showError("Error: Transfer not found in database.");
                    return;
                }

                currentTransfer.setStatus(TransfertStatus.CANCELLED);
                transferService.modifier(currentTransfer);
                refreshData();
            }
        } catch (Exception e) {
            showError("Error during cancellation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onMarkInTransit(BloodTransferRequest transfer) {
        try {
            if (transfer == null || transfer.getTransferId() == null) {
                showError(
                    "Error: Transfer ID is missing. Cannot mark as in transit."
                );
                return;
            }

            // Fetch fresh data to ensure we have the latest state
            BloodTransferRequest currentTransfer =
                (BloodTransferRequest) transferService.getTransfertById(
                    transfer.getTransferId()
                );

            if (currentTransfer == null) {
                showError("Error: Transfer not found in database.");
                return;
            }

            if (currentTransfer.getStatus() != TransfertStatus.APPROVED) {
                showError(
                    "Error: Transfer must be in APPROVED status to mark as in transit. Current status: " +
                        currentTransfer.getStatus()
                );
                return;
            }

            currentTransfer.setStatus(TransfertStatus.IN_TRANSIT);
            transferService.modifier(currentTransfer);
            refreshData();
        } catch (Exception e) {
            showError("Error marking as in transit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onMarkDelivered(BloodTransferRequest transfer) {
        try {
            if (transfer == null || transfer.getTransferId() == null) {
                showError(
                    "Error: Transfer ID is missing. Cannot mark as delivered."
                );
                return;
            }

            // Fetch fresh data to ensure we have the latest state
            BloodTransferRequest currentTransfer =
                (BloodTransferRequest) transferService.getTransfertById(
                    transfer.getTransferId()
                );

            if (currentTransfer == null) {
                showError("Error: Transfer not found in database.");
                return;
            }

            if (currentTransfer.getStatus() != TransfertStatus.IN_TRANSIT) {
                showError(
                    "Error: Transfer must be in IN_TRANSIT status to mark as delivered. Current status: " +
                        currentTransfer.getStatus()
                );
                return;
            }

            currentTransfer.setStatus(TransfertStatus.DELIVERED);
            currentTransfer.setActualDeliveryAt(
                new Timestamp(System.currentTimeMillis())
            );
            transferService.modifier(currentTransfer);
            refreshData();
        } catch (Exception e) {
            showError("Error confirming delivery: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== MODAL ====================

    private void openTransferForm(BloodTransferRequest existingTransfer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/edu/esprit/views/TransferForm.fxml")
            );
            Node formView = loader.load();

            TransferFormController formController = loader.getController();
            formController.setTransferListController(this);

            // Find the contentArea StackPane
            StackPane contentArea = findContentArea();
            if (contentArea != null) {
                formController.setParentContainer(contentArea);
                if (existingTransfer != null) {
                    formController.setEditMode(existingTransfer);
                }
                contentArea.getChildren().add(formView);
            } else {
                showError("Unable to open the form: container not found.");
            }
        } catch (IOException e) {
            System.err.println(
                "Failed to load TransferForm.fxml: " + e.getMessage()
            );
            e.printStackTrace();
            showError("Error opening the form: " + e.getMessage());
        }
    }

    private StackPane findContentArea() {
        try {
            if (cardsContainer.getScene() != null) {
                Node found = cardsContainer.getScene().lookup("#contentArea");
                if (found instanceof StackPane) {
                    return (StackPane) found;
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding contentArea: " + e.getMessage());
        }
        return null;
    }

    // ==================== UTILITY ====================

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== ROLE-BASED PERMISSION HELPERS ====================

    /**
     * Check if current user is the requesting staff member
     */
    private boolean isRequestingStaff(
        BloodTransferRequest transfer,
        Users user
    ) {
        if (transfer == null || user == null) {
            return false;
        }
        return (
            transfer.getRequestingStaffId() != null &&
            transfer.getRequestingStaffId().equals(user.getId())
        );
    }

    /**
     * Check if current user is the approving staff member
     */
    private boolean isApprovingStaff(
        BloodTransferRequest transfer,
        Users user
    ) {
        if (transfer == null || user == null) {
            return false;
        }

        // System admin is always considered approving staff
        if (isSystemAdmin(user)) {
            return true;
        }

        // For PENDING transfers, check if user's hospital is the approving hospital
        if (
            transfer.getStatus() == TransfertStatus.PENDING &&
            transfer.getApprovingHospitalId() != null
        ) {
            UUID userHospitalId = getCurrentUserHospitalId();
            if (userHospitalId == null) {
                return false;
            }
            return userHospitalId.equals(transfer.getApprovingHospitalId());
        }

        // For approved transfers, check if user is the one who approved
        if (transfer.getApprovingStaffId() != null) {
            return transfer.getApprovingStaffId().equals(user.getId());
        }

        return false;
    }

    /**
     * Check if current user is a system admin
     */
    private boolean isSystemAdmin(Users user) {
        if (user == null) {
            return false;
        }
        return user.getUserType() == UserType.ADMIN;
    }

    /**
     * Check if current user can approve a transfer
     */
    private boolean canApprove(BloodTransferRequest transfer, Users user) {
        if (transfer == null || user == null) {
            return false;
        }
        // Cannot approve if you're the one who requested it
        if (isRequestingStaff(transfer, user)) {
            return false;
        }
        // Only PENDING transfers can be approved
        if (transfer.getStatus() != TransfertStatus.PENDING) {
            return false;
        }
        // Only approving staff from the receiving hospital can approve
        return isApprovingStaff(transfer, user);
    }

    /**
     * Check if current user can edit a transfer
     */
    private boolean canEdit(BloodTransferRequest transfer, Users user) {
        if (transfer == null || user == null) {
            return false;
        }
        // Only PENDING transfers can be edited
        if (transfer.getStatus() != TransfertStatus.PENDING) {
            return false;
        }
        // Requesting staff can edit PENDING (they created it)
        if (isRequestingStaff(transfer, user)) {
            return true;
        }
        // System admin can edit PENDING
        if (isSystemAdmin(user)) {
            return true;
        }
        return false;
    }

    /**
     * Check if current user can cancel a transfer
     */
    private boolean canCancel(BloodTransferRequest transfer, Users user) {
        if (transfer == null || user == null) {
            return false;
        }
        // Cannot cancel DELIVERED, DENIED, or already CANCELLED transfers
        if (
            transfer.getStatus() == TransfertStatus.DELIVERED ||
            transfer.getStatus() == TransfertStatus.DENIED ||
            transfer.getStatus() == TransfertStatus.CANCELLED
        ) {
            return false;
        }
        // Requesting staff (who created it) can cancel at any stage (PENDING, APPROVED, IN_TRANSIT)
        if (isRequestingStaff(transfer, user)) {
            return true;
        }
        // System admin can cancel anything (except DELIVERED/DENIED/CANCELLED)
        if (isSystemAdmin(user)) {
            return true;
        }
        return false;
    }

    /**
     * Check if current user can mark transfer as in transit
     */
    private boolean canMarkInTransit(
        BloodTransferRequest transfer,
        Users user
    ) {
        if (transfer == null || user == null) {
            return false;
        }
        // Only APPROVED transfers can be marked in transit
        if (transfer.getStatus() != TransfertStatus.APPROVED) {
            return false;
        }
        // Approving staff or system admin can mark in transit
        return isApprovingStaff(transfer, user) || isSystemAdmin(user);
    }

    /**
     * Check if current user can mark transfer as delivered
     */
    private boolean canMarkDelivered(
        BloodTransferRequest transfer,
        Users user
    ) {
        if (transfer == null || user == null) {
            return false;
        }
        // Only IN_TRANSIT transfers can be marked delivered
        if (transfer.getStatus() != TransfertStatus.IN_TRANSIT) {
            return false;
        }
        // Both requesting hospital staff and approving hospital staff can mark as delivered, or admin
        return (
            isRequestingStaff(transfer, user) ||
            isApprovingStaff(transfer, user) ||
            isSystemAdmin(user)
        );
    }

    /**
     * Get the current user's hospital ID for permission checking
     * Returns null if user is a system admin or hospital info cannot be found
     */
    private UUID getCurrentUserHospitalId() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        try {
            // Create a HospitalStaff object with the user ID to query
            HospitalStaff searchStaff = new HospitalStaff();
            searchStaff.setId(currentUser.getId());

            // Get the staff record for this user
            HospitalStaff staff = hospitalStaffService.getOne(searchStaff);

            if (staff != null && staff.getHospitalId() != null) {
                try {
                    return UUID.fromString(staff.getHospitalId());
                } catch (IllegalArgumentException e) {
                    // Invalid UUID format
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println(
                "Error getting user hospital ID: " + e.getMessage()
            );
        }

        return null;
    }
}
