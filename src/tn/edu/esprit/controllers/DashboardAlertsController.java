package tn.edu.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.edu.esprit.entities.Alert;
import tn.edu.esprit.entities.AlertSeverity;
import tn.edu.esprit.entities.DonorAlert;
import tn.edu.esprit.entities.DonorResponse;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AlertService;
import tn.edu.esprit.services.AlertServiceImpl;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.DonorAlertService;
import tn.edu.esprit.services.DonorAlertServiceImpl;

import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DashboardAlertsController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterSelector;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnBroadcast;
    @FXML
    private HBox statsRow;
    @FXML
    private ScrollPane scrollAlerts;
    @FXML
    private VBox containerAlerts;

    private AlertService alertService;
    private DonorAlertService donorAlertService;

    private ObservableList<Alert> allAlerts;
    private ObservableList<DonorAlert> allDonorAlerts;
    private boolean readOnlyDonor;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        alertService = new AlertServiceImpl();
        donorAlertService = new DonorAlertServiceImpl();

        Users currentUser = AppSession.getCurrentUser();
        readOnlyDonor = currentUser != null && currentUser.getUserType() == UserType.DONOR;
        if (btnBroadcast != null) {
            btnBroadcast.setVisible(!readOnlyDonor);
            btnBroadcast.setManaged(!readOnlyDonor);
        }

        setupFilterSelector();
        setupSearchListener();
        loadData();
        renderCards();
    }

    // ==================== FILTER ====================

    private void setupFilterSelector() {
        if (filterSelector == null)
            return;
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("Tous");
        for (AlertSeverity sev : AlertSeverity.values()) {
            options.add(sev.name());
        }
        options.add("RESOLUE");
        options.add("ACTIVE");
        filterSelector.setItems(options);
        filterSelector.getSelectionModel().selectFirst();
        filterSelector.setOnAction(e -> renderCards());
    }

    private String getActiveFilter() {
        if (filterSelector == null)
            return null;
        String val = filterSelector.getValue();
        if (val == null || val.equals("Tous"))
            return null;
        return val;
    }

    // ==================== SEARCH ====================

    private void setupSearchListener() {
        if (searchField == null)
            return;
        javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(300));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(ev -> renderCards());
            debounce.playFromStart();
        });
    }

    // ==================== DATA ====================

    private void loadData() {
        allAlerts = FXCollections.observableArrayList(alertService.getAll());
        allDonorAlerts = FXCollections.observableArrayList(donorAlertService.getAll());
    }

    // ==================== RENDERING ====================

    private void renderCards() {
        if (containerAlerts == null || allAlerts == null)
            return;
        containerAlerts.getChildren().clear();

        String search = searchField != null ? searchField.getText() : "";
        String activeFilter = getActiveFilter();

        List<Alert> filtered = filterList(allAlerts, search,
                alert -> safeContains(alert.getAlertId(), search)
                        || safeContains(alert.getTitle(), search)
                        || safeContains(alert.getSeverity() != null ? alert.getSeverity().name() : "", search)
                        || safeContains(alert.getHospitalId(), search)
                        || safeContains(alert.getMessage(), search));

        if (activeFilter != null) {
            if (activeFilter.equals("RESOLUE")) {
                filtered = filtered.stream().filter(Alert::isResolved).collect(Collectors.toList());
            } else if (activeFilter.equals("ACTIVE")) {
                filtered = filtered.stream().filter(a -> !a.isResolved()).collect(Collectors.toList());
            } else {
                final String af = activeFilter;
                filtered = filtered.stream()
                        .filter(a -> a.getSeverity() != null && a.getSeverity().name().equals(af))
                        .collect(Collectors.toList());
            }
        }

        updateStats(filtered);

        if (filtered.isEmpty()) {
            containerAlerts.getChildren()
                    .add(buildEmptyState("Aucune alerte", "Les alertes d'urgence apparaitront ici."));
            return;
        }

        for (Alert alert : filtered) {
            containerAlerts.getChildren().add(buildAlertCard(alert));
        }
    }

    private void updateStats(List<Alert> filtered) {
        if (statsRow == null)
            return;
        statsRow.getChildren().clear();

        long total = allAlerts.size();
        long active = allAlerts.stream().filter(a -> !a.isResolved()).count();
        long resolved = allAlerts.stream().filter(Alert::isResolved).count();
        long critical = allAlerts.stream()
                .filter(a -> a.getSeverity() == AlertSeverity.CRITICAL && !a.isResolved()).count();

        statsRow.getChildren().addAll(
                buildStatChip("Total", String.valueOf(total), "badge-info"),
                buildStatChip("Actives", String.valueOf(active), "badge-warning"),
                buildStatChip("Resolues", String.valueOf(resolved), "badge-success"),
                buildStatChip("Critiques", String.valueOf(critical), "badge-critical"));
    }

    // ==================== CARD BUILDER ====================

    private VBox buildAlertCard(Alert alert) {
        VBox card = new VBox(10);
        card.getStyleClass().add("log-card");

        // Header
        Label severityBadge = createBadge(alert.getSeverity() != null ? alert.getSeverity().name() : "—");
        Label statusBadge = new Label(alert.isResolved() ? "Resolue" : "Active");
        statusBadge.getStyleClass().addAll("badge", alert.isResolved() ? "badge-success" : "badge-critical");
        Label title = new Label(alert.getTitle() != null ? alert.getTitle() : "Sans titre");
        title.getStyleClass().add("log-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label date = new Label(formatTimestamp(alert.getCreatedAt()));
        date.getStyleClass().add("log-card-subtitle");
        HBox header = new HBox(10, severityBadge, statusBadge, title, spacer, date);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("log-card-header");

        // Body
        HBox hospitalRow = buildDetailRow("Hopital", truncateId(alert.getHospitalId()));
        HBox bloodTypeRow = buildDetailRow("Groupe sanguin",
                alert.getBloodTypeId() != null ? alert.getBloodTypeId() : "—");
        HBox quantityRow = buildDetailRow("Quantite", alert.getQuantityNeeded() + " unites");
        HBox radiusRow = buildDetailRow("Rayon", alert.getTargetRadiusKm() + " km");
        HBox messageRow = buildDetailRow("Message", alert.getMessage() != null ? alert.getMessage() : "—");

        // Donor responses summary
        long totalResponses = allDonorAlerts.stream()
                .filter(da -> da.getAlertId() != null && da.getAlertId().equals(alert.getAlertId())).count();
        long interested = allDonorAlerts.stream()
                .filter(da -> da.getAlertId() != null && da.getAlertId().equals(alert.getAlertId())
                        && da.getDonorResponse() == DonorResponse.INTERESTED)
                .count();
        HBox responsesRow = buildDetailRow("Reponses",
                interested + " interesses / " + totalResponses + " total");

        VBox body = new VBox(6, hospitalRow, bloodTypeRow, quantityRow, radiusRow, messageRow, responsesRow);
        body.getStyleClass().add("log-card-body");

        card.getChildren().addAll(header, body);

        if (!readOnlyDonor) {
            Region divider = new Region();
            divider.getStyleClass().add("log-card-divider");
            divider.setPrefHeight(1);

            Button btnEdit = new Button("✏ Modifier");
            btnEdit.getStyleClass().add("btn-log-edit");
            btnEdit.setOnAction(e -> handleEditAlert(alert));

            Button btnResolve;
            if (alert.isResolved()) {
                btnResolve = new Button("Resolue");
                btnResolve.getStyleClass().add("btn-log-resolve-done");
                btnResolve.setDisable(true);
            } else {
                btnResolve = new Button("✓ Resoudre");
                btnResolve.getStyleClass().add("btn-log-resolve");
                btnResolve.setOnAction(e -> handleResolveAlert(alert));
            }

            Button btnDelete = new Button("🗑 Supprimer");
            btnDelete.getStyleClass().add("btn-log-delete");
            btnDelete.setOnAction(e -> handleDeleteAlert(alert));

            Region footerSpacer = new Region();
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);
            HBox footer = new HBox(8, footerSpacer, btnEdit, btnResolve, btnDelete);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.getStyleClass().add("log-card-footer");

            card.getChildren().addAll(divider, footer);
        }
        return card;
    }

    // ==================== ACTIONS ====================

    @FXML
    private void handleBroadcast() {
        if (readOnlyDonor) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/edu/esprit/views/BroadcastAlertDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Diffuser une alerte");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleEditAlert(Alert alert) {
        if (readOnlyDonor) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/edu/esprit/views/BroadcastAlertDialog.fxml"));
            Parent root = loader.load();
            BroadcastAlertDialogController controller = loader.getController();
            controller.setEditMode(alert);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier l'alerte");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleResolveAlert(Alert alert) {
        if (readOnlyDonor) {
            return;
        }
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Resoudre l'alerte");
        confirm.setHeaderText("Marquer cette alerte comme resolue ?");
        confirm.setContentText(alert.getTitle());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                alert.setResolved(true);
                alert.setResolvedAt(new Timestamp(System.currentTimeMillis()));
                alertService.modifier(alert);
                refreshData();
            }
        });
    }

    private void handleDeleteAlert(Alert alert) {
        if (readOnlyDonor) {
            return;
        }
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer cette alerte ?");
        confirm.setContentText(alert.getTitle());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                alertService.supprimer(alert.getAlertId());
                refreshData();
            }
        });
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    private void refreshData() {
        loadData();
        renderCards();
    }

    // ==================== HELPERS ====================

    private Label createBadge(String text) {
        Label badge = new Label(text);
        String lower = text.toLowerCase();
        String variant;
        if (lower.contains("critical") || lower.contains("urgent")) {
            variant = "badge-critical";
        } else if (lower.contains("warning")) {
            variant = "badge-warning";
        } else if (lower.contains("info")) {
            variant = "badge-info";
        } else if (lower.contains("resolue") || lower.contains("resolved")) {
            variant = "badge-success";
        } else {
            variant = "badge-neutral";
        }
        badge.getStyleClass().addAll("badge", variant);
        return badge;
    }

    private HBox buildDetailRow(String label, String value) {
        Label lbl = new Label(label + ": ");
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: bold;");
        val.setWrapText(true);
        HBox row = new HBox(6, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildStatChip(String label, String value, String badgeClass) {
        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().addAll("badge", badgeClass);
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        HBox chip = new HBox(6, valueLbl, labelLbl);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("logs-stat-chip");
        return chip;
    }

    private VBox buildEmptyState(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");
        VBox box = new VBox(8, titleLabel, subtitleLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("logs-empty-state");
        return box;
    }

    private String truncateId(String id) {
        if (id == null)
            return "—";
        return id.length() > 8 ? id.substring(0, 8) + "..." : id;
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null)
            return "—";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(ts);
    }

    private boolean safeContains(String text, String search) {
        if (text == null || search == null || search.isEmpty())
            return false;
        return text.toLowerCase().contains(search.toLowerCase());
    }

    private <T> List<T> filterList(List<T> source, String search, Predicate<T> predicate) {
        if (search == null || search.trim().isEmpty()) {
            return new java.util.ArrayList<>(source);
        }
        return source.stream().filter(predicate).collect(Collectors.toList());
    }
}
