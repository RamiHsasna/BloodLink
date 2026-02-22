package tn.edu.esprit.controllers;

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
import tn.edu.esprit.entities.TransfertStatus;
import tn.edu.esprit.services.HospitalServiceImpl;
import tn.edu.esprit.services.TransfertServiceImpl;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TransferListController implements Initializable {

    @FXML private VBox cardsContainer;
    @FXML private VBox emptyState;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private ComboBox<String> cbBloodTypeFilter;
    @FXML private Button btnNewTransfer;
    @FXML private Label lblTotalCount;
    @FXML private Label lblPendingCount;
    @FXML private Label lblApprovedCount;
    @FXML private Label lblInTransitCount;
    @FXML private Label lblDeliveredCount;

    private final TransfertServiceImpl transferService = new TransfertServiceImpl();
    private final HospitalServiceImpl hospitalService = new HospitalServiceImpl();

    private List<BloodTransferRequest> allTransfers;
    private Map<UUID, Hospital> hospitalCache = new HashMap<>();

    private static final String[] BLOOD_TYPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Build hospital lookup cache
        loadHospitalCache();

        // Populate status filter
        cbStatusFilter.getItems().add("Tous les statuts");
        cbStatusFilter.getItems().add("En attente");
        cbStatusFilter.getItems().add("Approuve");
        cbStatusFilter.getItems().add("En transit");
        cbStatusFilter.getItems().add("Livre");
        cbStatusFilter.getItems().add("Annule");
        cbStatusFilter.setValue("Tous les statuts");

        // Populate blood type filter
        cbBloodTypeFilter.getItems().add("Tous les groupes");
        cbBloodTypeFilter.getItems().addAll(Arrays.asList(BLOOD_TYPES));
        cbBloodTypeFilter.setValue("Tous les groupes");

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
            System.err.println("Error loading hospitals for cache: " + e.getMessage());
        }
    }

    private String getHospitalName(UUID hospitalId) {
        if (hospitalId == null) return "—";
        Hospital h = hospitalCache.get(hospitalId);
        return h != null ? h.getName() : hospitalId.toString().substring(0, 8) + "...";
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
        String searchText = tfSearch.getText() != null ? tfSearch.getText().trim().toLowerCase() : "";
        String statusFilter = cbStatusFilter.getValue();
        String bloodTypeFilter = cbBloodTypeFilter.getValue();

        List<BloodTransferRequest> filtered = allTransfers.stream()
                .filter(t -> matchesStatusFilter(t, statusFilter))
                .filter(t -> matchesBloodTypeFilter(t, bloodTypeFilter))
                .filter(t -> matchesSearch(t, searchText))
                .collect(Collectors.toList());

        updateStats(allTransfers);
        buildCards(filtered);
    }

    private boolean matchesStatusFilter(BloodTransferRequest t, String filter) {
        if (filter == null || filter.equals("Tous les statuts")) return true;
        TransfertStatus status = t.getStatus();
        if (status == null) return false;
        switch (filter) {
            case "En attente": return status == TransfertStatus.PENDING;
            case "Approuve": return status == TransfertStatus.APPROVED;
            case "En transit": return status == TransfertStatus.IN_TRANSIT;
            case "Livre": return status == TransfertStatus.DELIVERED;
            case "Annule": return status == TransfertStatus.CANCELLED;
            default: return true;
        }
    }

    private boolean matchesBloodTypeFilter(BloodTransferRequest t, String filter) {
        if (filter == null || filter.equals("Tous les groupes")) return true;
        return filter.equals(t.getBloodTypeId());
    }

    private boolean matchesSearch(BloodTransferRequest t, String searchText) {
        if (searchText.isEmpty()) return true;
        String requestingName = getHospitalName(t.getRequestingHospitalId()).toLowerCase();
        String approvingName = getHospitalName(t.getApprovingHospitalId()).toLowerCase();
        String bloodType = t.getBloodTypeId() != null ? t.getBloodTypeId().toLowerCase() : "";
        String reason = t.getReason() != null ? t.getReason().toLowerCase() : "";
        return requestingName.contains(searchText)
                || approvingName.contains(searchText)
                || bloodType.contains(searchText)
                || reason.contains(searchText);
    }

    // ==================== STATS ====================

    private void updateStats(List<BloodTransferRequest> transfers) {
        lblTotalCount.setText(String.valueOf(transfers.size()));

        long pending = transfers.stream().filter(t -> t.getStatus() == TransfertStatus.PENDING).count();
        long approved = transfers.stream().filter(t -> t.getStatus() == TransfertStatus.APPROVED).count();
        long inTransit = transfers.stream().filter(t -> t.getStatus() == TransfertStatus.IN_TRANSIT).count();
        long delivered = transfers.stream().filter(t -> t.getStatus() == TransfertStatus.DELIVERED).count();

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
                "-fx-border-color: " + cardLeftBorder + " #e2e8f0 #e2e8f0 " + cardLeftBorder + "; " +
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
        lblRequesting.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        lblRequesting.setMaxWidth(200);
        lblRequesting.setEllipsisString("...");

        // Arrow
        Label arrow = new Label("\u2192");
        arrow.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        // Approving hospital
        String appName = getHospitalName(transfer.getApprovingHospitalId());
        Label lblApproving = new Label(appName);
        lblApproving.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        lblApproving.setMaxWidth(200);
        lblApproving.setEllipsisString("...");

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);

        // Status badge
        Label statusBadge = new Label(statusText);
        statusBadge.setStyle(
                "-fx-background-color: " + statusBg + "; -fx-background-radius: 12; " +
                "-fx-text-fill: " + statusTextColor + "; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-padding: 4 12 4 12;"
        );

        // Blood type badge
        Label bloodBadge = new Label(transfer.getBloodTypeId() != null ? transfer.getBloodTypeId() : "—");
        bloodBadge.setStyle(
                "-fx-background-color: #dc2626; -fx-background-radius: 8; " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 3 10 3 10; -fx-min-width: 36; -fx-alignment: center;"
        );

        // Quantity badge
        String qtyText = (transfer.getQuantityUnitsRequested() != null ? transfer.getQuantityUnitsRequested() : 0) + " u.";
        Label qtyBadge = new Label(qtyText);
        qtyBadge.setStyle(
                "-fx-background-color: #f1f5f9; -fx-background-radius: 8; " +
                "-fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 3 10 3 10;"
        );

        topRow.getChildren().addAll(transferIcon, lblRequesting, arrow, lblApproving, spacerTop, statusBadge, bloodBadge, qtyBadge);

        // ---- Row 2: Reason ----
        HBox reasonRow = new HBox();
        reasonRow.setAlignment(Pos.CENTER_LEFT);
        reasonRow.setSpacing(8);

        String reasonStr = transfer.getReason() != null && !transfer.getReason().isEmpty()
                ? transfer.getReason() : "Aucun motif specifie";
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
            HBox requestedBox = createDateChip("\uD83D\uDCC5",
                    "Demande le " + transfer.getRequestedAt().toLocalDateTime().format(DATE_FMT));
            dateRow.getChildren().add(requestedBox);
        }

        // Expected delivery
        if (transfer.getDeliveryExpectedAt() != null) {
            HBox expectedBox = createDateChip("\uD83D\uDE9A",
                    "Livraison prevue " + transfer.getDeliveryExpectedAt().toLocalDateTime().format(DATE_FMT));
            dateRow.getChildren().add(expectedBox);
        }

        // Actual delivery
        if (transfer.getActualDeliveryAt() != null) {
            HBox deliveredBox = createDateChip("\u2705",
                    "Livre le " + transfer.getActualDeliveryAt().toLocalDateTime().format(DATE_FMT));
            dateRow.getChildren().add(deliveredBox);
        }

        // Approved quantity if differs from requested
        if (transfer.getQuantityUnitsApproved() != null && transfer.getQuantityUnitsApproved() > 0
                && !transfer.getQuantityUnitsApproved().equals(transfer.getQuantityUnitsRequested())) {
            HBox approvedQtyBox = createDateChip("\u2696",
                    "Approuve: " + transfer.getQuantityUnitsApproved() + " unites");
            dateRow.getChildren().add(approvedQtyBox);
        }

        body.getChildren().addAll(topRow, reasonRow, dateRow);

        // ---- Notes row (if present) ----
        if (transfer.getNotes() != null && !transfer.getNotes().trim().isEmpty()) {
            HBox notesRow = new HBox();
            notesRow.setAlignment(Pos.CENTER_LEFT);
            notesRow.setSpacing(6);
            Label notesIcon = new Label("\uD83D\uDCDD");
            notesIcon.setStyle("-fx-font-size: 11px;");
            Label notesLabel = new Label(transfer.getNotes());
            notesLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-font-style: italic;");
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

        // Only show edit/delete for PENDING or APPROVED transfers
        if (transfer.getStatus() == TransfertStatus.PENDING || transfer.getStatus() == TransfertStatus.APPROVED) {
            Button btnEdit = new Button("\u270F  Modifier");
            btnEdit.getStyleClass().add("btn-card-edit");
            btnEdit.setOnAction(e -> onEditTransfer(transfer));

            Button btnDelete = new Button("\uD83D\uDDD1  Supprimer");
            btnDelete.getStyleClass().add("btn-card-delete");
            btnDelete.setOnAction(e -> onDeleteTransfer(transfer));

            actionsRow.getChildren().addAll(btnEdit, btnDelete);
        }

        // Status action buttons based on current state
        if (transfer.getStatus() == TransfertStatus.PENDING) {
            Button btnApprove = new Button("\u2705 Approuver");
            btnApprove.setStyle(
                    "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnApprove.setOnAction(e -> onApproveTransfer(transfer));

            Button btnCancel = new Button("\u274C Annuler");
            btnCancel.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnCancel.setOnAction(e -> onCancelTransfer(transfer));
            actionsRow.getChildren().addAll(btnApprove, btnCancel);
        } else if (transfer.getStatus() == TransfertStatus.APPROVED) {
            Button btnShip = new Button("\uD83D\uDE9A En transit");
            btnShip.setStyle(
                    "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnShip.setOnAction(e -> onMarkInTransit(transfer));
            actionsRow.getChildren().add(btnShip);
        } else if (transfer.getStatus() == TransfertStatus.IN_TRANSIT) {
            Button btnDeliver = new Button("\uD83D\uDCE6 Marquer livre");
            btnDeliver.setStyle(
                    "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            btnDeliver.setOnAction(e -> onMarkDelivered(transfer));
            actionsRow.getChildren().add(btnDeliver);
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

    // ==================== STATUS HELPERS ====================

    private String getStatusText(TransfertStatus status) {
        if (status == null) return "—";
        switch (status) {
            case PENDING: return "En attente";
            case APPROVED: return "Approuve";
            case IN_TRANSIT: return "En transit";
            case DELIVERED: return "Livre";
            case CANCELLED: return "Annule";
            default: return status.name();
        }
    }

    private String getStatusBgColor(TransfertStatus status) {
        if (status == null) return "#f3f4f6";
        switch (status) {
            case PENDING: return "#fef9c3";
            case APPROVED: return "#dbeafe";
            case IN_TRANSIT: return "#e0e7ff";
            case DELIVERED: return "#dcfce7";
            case CANCELLED: return "#fee2e2";
            default: return "#f3f4f6";
        }
    }

    private String getStatusTextColor(TransfertStatus status) {
        if (status == null) return "#6b7280";
        switch (status) {
            case PENDING: return "#a16207";
            case APPROVED: return "#1d4ed8";
            case IN_TRANSIT: return "#4338ca";
            case DELIVERED: return "#15803d";
            case CANCELLED: return "#dc2626";
            default: return "#6b7280";
        }
    }

    private String getStatusDotColor(TransfertStatus status) {
        if (status == null) return "#9ca3af";
        switch (status) {
            case PENDING: return "#eab308";
            case APPROVED: return "#3b82f6";
            case IN_TRANSIT: return "#6366f1";
            case DELIVERED: return "#22c55e";
            case CANCELLED: return "#ef4444";
            default: return "#9ca3af";
        }
    }

    private String getCardLeftBorderColor(TransfertStatus status) {
        if (status == null) return "#e2e8f0";
        switch (status) {
            case PENDING: return "#eab308";
            case APPROVED: return "#3b82f6";
            case IN_TRANSIT: return "#6366f1";
            case DELIVERED: return "#22c55e";
            case CANCELLED: return "#ef4444";
            default: return "#e2e8f0";
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
        confirm.setTitle("Supprimer le transfert");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous vraiment supprimer cette demande de transfert #" + transfer.getTransferId() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                transferService.supprimer(transfer.getTransferId());
                refreshData();
            } catch (Exception e) {
                showError("Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private void onApproveTransfer(BloodTransferRequest transfer) {
        transfer.setStatus(TransfertStatus.APPROVED);
        transfer.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        if (transfer.getQuantityUnitsApproved() == null || transfer.getQuantityUnitsApproved() == 0) {
            transfer.setQuantityUnitsApproved(transfer.getQuantityUnitsRequested());
        }
        try {
            transferService.modifier(transfer);
            refreshData();
        } catch (Exception e) {
            showError("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

    private void onCancelTransfer(BloodTransferRequest transfer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler le transfert");
        confirm.setHeaderText("Confirmer l'annulation");
        confirm.setContentText("Voulez-vous vraiment annuler cette demande de transfert ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            transfer.setStatus(TransfertStatus.CANCELLED);
            try {
                transferService.modifier(transfer);
                refreshData();
            } catch (Exception e) {
                showError("Erreur lors de l'annulation: " + e.getMessage());
            }
        }
    }

    private void onMarkInTransit(BloodTransferRequest transfer) {
        transfer.setStatus(TransfertStatus.IN_TRANSIT);
        try {
            transferService.modifier(transfer);
            refreshData();
        } catch (Exception e) {
            showError("Erreur lors de la mise en transit: " + e.getMessage());
        }
    }

    private void onMarkDelivered(BloodTransferRequest transfer) {
        transfer.setStatus(TransfertStatus.DELIVERED);
        transfer.setActualDeliveryAt(new Timestamp(System.currentTimeMillis()));
        try {
            transferService.modifier(transfer);
            refreshData();
        } catch (Exception e) {
            showError("Erreur lors de la confirmation de livraison: " + e.getMessage());
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
                showError("Impossible d'ouvrir le formulaire: conteneur introuvable.");
            }

        } catch (IOException e) {
            System.err.println("Failed to load TransferForm.fxml: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors de l'ouverture du formulaire: " + e.getMessage());
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
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
