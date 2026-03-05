package tn.edu.esprit.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.edu.esprit.entities.*;
import tn.edu.esprit.services.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

public class DashboardLogsController implements Initializable {

    // Section names
    private static final String SEC_DONATION_LOGS = "Journaux de dons";
    private static final String SEC_TRANSFER_LOGS = "Journaux de transferts";
    private static final String SEC_DONOR_RESPONSES = "Reponses des donneurs";
    private static final String SEC_STATISTICS = "Statistiques";

    @FXML
    private ComboBox<String> sectionSelector;
    @FXML
    private ComboBox<String> filterSelector;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnAdd;
    @FXML
    private HBox statsRow;
    @FXML
    private StackPane contentStack;

    // Card containers
    @FXML
    private ScrollPane scrollDonationLogs;
    @FXML
    private VBox containerDonationLogs;
    @FXML
    private ScrollPane scrollTransferLogs;
    @FXML
    private VBox containerTransferLogs;
    @FXML
    private ScrollPane scrollDonorAlerts;
    @FXML
    private VBox containerDonorAlerts;
    @FXML
    private VBox containerStats;

    // Charts
    @FXML
    private PieChart pieChartSeverity;
    @FXML
    private BarChart<String, Number> barChartDonations;

    private DonationLogService donationLogService;
    private BloodTransferRequestLogService transferLogService;
    private AlertService alertService;
    private DonorAlertService donorAlertService;

    // Source lists
    private ObservableList<DonationLog> allDonationLogs;
    private ObservableList<BloodTransferRequestLog> allTransferLogs;
    private ObservableList<tn.edu.esprit.entities.Alert> allAlerts;
    private ObservableList<DonorAlert> allDonorAlerts;

    private ScrollPane[] sectionPanes;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        donationLogService = new DonationLogServiceImpl();
        transferLogService = new BloodTransferRequestLogServiceImpl();
        alertService = new AlertServiceImpl();
        donorAlertService = new DonorAlertServiceImpl();

        setupSectionSelector();
        setupFilterSelector();
        setupSearchListener();
        loadData();
        loadCharts();
        renderCurrentSection();
    }

    // ==================== SECTION NAVIGATION ====================

    private void setupSectionSelector() {
        sectionPanes = new ScrollPane[] {
                scrollDonationLogs, scrollTransferLogs, scrollDonorAlerts
        };

        sectionSelector.setItems(FXCollections.observableArrayList(
                SEC_DONATION_LOGS, SEC_TRANSFER_LOGS,
                SEC_DONOR_RESPONSES, SEC_STATISTICS));
        sectionSelector.getSelectionModel().selectFirst();

        sectionSelector.setOnAction(e -> {
            updateFilterOptions();
            renderCurrentSection();
            updateAddButtonVisibility();
        });

        updateAddButtonVisibility();
    }

    private void setupFilterSelector() {
        if (filterSelector == null)
            return;
        filterSelector.setOnAction(e -> renderCurrentSection());
        updateFilterOptions();
    }

    private void updateFilterOptions() {
        if (filterSelector == null)
            return;
        int idx = getCurrentSectionIndex();
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("Tous");

        switch (idx) {
            case 0:
                for (DonationLogAction action : DonationLogAction.values()) {
                    options.add(action.name());
                }
                filterSelector.setPromptText("Filtrer par action");
                break;
            case 1:
                for (TransferLogAction action : TransferLogAction.values()) {
                    options.add(action.name());
                }
                filterSelector.setPromptText("Filtrer par action");
                break;
            case 2:
                for (DonorResponse resp : DonorResponse.values()) {
                    options.add(resp.name());
                }
                options.add("LU");
                options.add("NON_LU");
                filterSelector.setPromptText("Filtrer par reponse");
                break;
            default:
                filterSelector.setPromptText("Filtrer par...");
                break;
        }

        filterSelector.setItems(options);
        filterSelector.getSelectionModel().selectFirst();
        filterSelector.setVisible(idx < 3);
    }

    private String getActiveFilter() {
        if (filterSelector == null)
            return null;
        String val = filterSelector.getValue();
        if (val == null || val.equals("Tous"))
            return null;
        return val;
    }

    private void showSection(int index) {
        // Masquer toutes les sections
        for (ScrollPane sp : sectionPanes) {
            if (sp != null)
                sp.setVisible(false);
        }
        if (containerStats != null)
            containerStats.setVisible(false);

        if (index == 3) {
            // Statistiques
            if (containerStats != null)
                containerStats.setVisible(true);
        } else if (index >= 0 && index < sectionPanes.length && sectionPanes[index] != null) {
            sectionPanes[index].setVisible(true);
        }
    }

    private int getCurrentSectionIndex() {
        return sectionSelector.getSelectionModel().getSelectedIndex();
    }

    private void updateAddButtonVisibility() {
        if (btnAdd == null)
            return;
        int idx = getCurrentSectionIndex();
        // Afficher le bouton ajouter pour les 2 premieres sections
        btnAdd.setVisible(idx >= 0 && idx <= 1);

        switch (idx) {
            case 0:
                btnAdd.setText("+ Journal de don");
                break;
            case 1:
                btnAdd.setText("+ Journal de transfert");
                break;
            default:
                break;
        }
    }

    // ==================== DATA LOADING ====================

    private void loadData() {
        if (donationLogService != null) {
            allDonationLogs = FXCollections.observableArrayList(donationLogService.getAll());
        }
        if (transferLogService != null) {
            allTransferLogs = FXCollections.observableArrayList(transferLogService.getAll());
        }
        if (alertService != null) {
            allAlerts = FXCollections.observableArrayList(alertService.getAll());
        }
        if (donorAlertService != null) {
            allDonorAlerts = FXCollections.observableArrayList(donorAlertService.getAll());
        }
    }

    // ==================== CARD RENDERING ====================

    private void renderCurrentSection() {
        int idx = getCurrentSectionIndex();
        showSection(idx);

        String search = searchField != null ? searchField.getText() : "";
        String activeFilter = getActiveFilter();

        switch (idx) {
            case 0:
                renderDonationLogCards(search, activeFilter);
                break;
            case 1:
                renderTransferLogCards(search, activeFilter);
                break;
            case 2:
                renderDonorAlertCards(search, activeFilter);
                break;
            case 3: /* statistiques deja chargees */
                break;
        }
        updateStats(idx);
    }

    // --- Donation Log Cards ---
    private void renderDonationLogCards(String search, String activeFilter) {
        if (containerDonationLogs == null || allDonationLogs == null)
            return;
        containerDonationLogs.getChildren().clear();

        List<DonationLog> filtered = filterList(allDonationLogs, search, log -> safeContains(log.getLogId(), search)
                || safeContains(log.getDonationId(), search)
                || safeContains(log.getAction() != null ? log.getAction().name() : "", search)
                || safeContains(log.getPreviousStatus(), search)
                || safeContains(log.getNewStatus(), search)
                || safeContains(log.getNotes(), search));

        if (activeFilter != null) {
            filtered = filtered.stream()
                    .filter(log -> log.getAction() != null && log.getAction().name().equals(activeFilter))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (filtered.isEmpty()) {
            containerDonationLogs.getChildren()
                    .add(buildEmptyState("Aucun journal de don", "Les journaux de dons apparaitront ici."));
            return;
        }

        for (DonationLog log : filtered) {
            containerDonationLogs.getChildren().add(buildDonationLogCard(log));
        }
    }

    private VBox buildDonationLogCard(DonationLog log) {
        VBox card = new VBox(10);
        card.getStyleClass().add("log-card");

        // En-tete: badge action + ID
        Label actionBadge = createBadge(log.getAction() != null ? log.getAction().name() : "—");
        Label title = new Label("Don #" + truncateId(log.getDonationId()));
        title.getStyleClass().add("log-card-title");
        Label date = new Label(formatTimestamp(log.getCreatedAt()));
        date.getStyleClass().add("log-card-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(10, actionBadge, title, spacer, date);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("log-card-header");

        // Corps: transition de statut
        HBox statusRow = buildStatusTransition(log.getPreviousStatus(), log.getNewStatus());
        HBox loggedByRow = buildDetailRow("Saisi par", truncateId(log.getLoggedBy()));
        HBox notesRow = buildDetailRow("Notes", log.getNotes() != null ? log.getNotes() : "—");
        VBox body = new VBox(6, statusRow, loggedByRow, notesRow);
        body.getStyleClass().add("log-card-body");

        // Separateur + actions
        Region divider = new Region();
        divider.getStyleClass().add("log-card-divider");
        divider.setPrefHeight(1);

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.getStyleClass().add("btn-log-edit");
        btnEdit.setOnAction(e -> handleEditDonationLog(log));

        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.getStyleClass().add("btn-log-delete");
        btnDelete.setOnAction(e -> handleDeleteDonationLog(log));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox footer = new HBox(8, footerSpacer, btnEdit, btnDelete);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("log-card-footer");

        card.getChildren().addAll(header, body, divider, footer);
        return card;
    }

    // --- Transfer Log Cards ---
    private void renderTransferLogCards(String search, String activeFilter) {
        if (containerTransferLogs == null || allTransferLogs == null)
            return;
        containerTransferLogs.getChildren().clear();

        List<BloodTransferRequestLog> filtered = filterList(allTransferLogs, search,
                log -> safeContains(log.getLogId(), search)
                        || String.valueOf(log.getTransferId()).contains(search.toLowerCase())
                        || safeContains(log.getAction() != null ? log.getAction().name() : "", search)
                        || safeContains(log.getPreviousStatus(), search)
                        || safeContains(log.getNewStatus(), search)
                        || safeContains(log.getNotes(), search));

        if (activeFilter != null) {
            filtered = filtered.stream()
                    .filter(log -> log.getAction() != null && log.getAction().name().equals(activeFilter))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (filtered.isEmpty()) {
            containerTransferLogs.getChildren()
                    .add(buildEmptyState("Aucun journal de transfert", "Les journaux de transferts apparaitront ici."));
            return;
        }

        for (BloodTransferRequestLog log : filtered) {
            containerTransferLogs.getChildren().add(buildTransferLogCard(log));
        }
    }

    private VBox buildTransferLogCard(BloodTransferRequestLog log) {
        VBox card = new VBox(10);
        card.getStyleClass().add("log-card");

        Label actionBadge = createBadge(log.getAction() != null ? log.getAction().name() : "—");
        Label title = new Label("Transfert #" + log.getTransferId());
        title.getStyleClass().add("log-card-title");
        Label date = new Label(formatTimestamp(log.getCreatedAt()));
        date.getStyleClass().add("log-card-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(10, actionBadge, title, spacer, date);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("log-card-header");

        HBox statusRow = buildStatusTransition(log.getPreviousStatus(), log.getNewStatus());
        HBox changedByRow = buildDetailRow("Modifie par", truncateId(log.getChangedBy()));
        HBox notesRow = buildDetailRow("Notes", log.getNotes() != null ? log.getNotes() : "—");
        VBox body = new VBox(6, statusRow, changedByRow, notesRow);
        body.getStyleClass().add("log-card-body");

        Region divider = new Region();
        divider.getStyleClass().add("log-card-divider");
        divider.setPrefHeight(1);

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.getStyleClass().add("btn-log-edit");
        btnEdit.setOnAction(e -> handleEditTransferLog(log));

        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.getStyleClass().add("btn-log-delete");
        btnDelete.setOnAction(e -> handleDeleteTransferLog(log));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox footer = new HBox(8, footerSpacer, btnEdit, btnDelete);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("log-card-footer");

        card.getChildren().addAll(header, body, divider, footer);
        return card;
    }

    // --- Donor Alert Cards ---
    private void renderDonorAlertCards(String search, String activeFilter) {
        if (containerDonorAlerts == null || allDonorAlerts == null)
            return;
        containerDonorAlerts.getChildren().clear();

        List<DonorAlert> filtered = filterList(allDonorAlerts, search, da -> safeContains(da.getDonorAlertId(), search)
                || safeContains(da.getAlertId(), search)
                || safeContains(da.getDonorId(), search)
                || safeContains(da.getDonorResponse() != null ? da.getDonorResponse().name() : "", search));

        if (activeFilter != null) {
            if (activeFilter.equals("LU")) {
                filtered = filtered.stream().filter(da -> da.isRead()).collect(java.util.stream.Collectors.toList());
            } else if (activeFilter.equals("NON_LU")) {
                filtered = filtered.stream().filter(da -> !da.isRead()).collect(java.util.stream.Collectors.toList());
            } else {
                filtered = filtered.stream()
                        .filter(da -> da.getDonorResponse() != null
                                && da.getDonorResponse().name().equals(activeFilter))
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        if (filtered.isEmpty()) {
            containerDonorAlerts.getChildren()
                    .add(buildEmptyState("Aucune reponse de donneur", "Les reponses des donneurs apparaitront ici."));
            return;
        }

        for (DonorAlert da : filtered) {
            containerDonorAlerts.getChildren().add(buildDonorAlertCard(da));
        }
    }

    private VBox buildDonorAlertCard(DonorAlert da) {
        VBox card = new VBox(10);
        card.getStyleClass().add("log-card");

        String responseText = "EN_ATTENTE";
        if (da.getDonorResponse() != null && da.getDonorResponse() != DonorResponse.NO_RESPONSE) {
            responseText = da.getDonorResponse().name();
        }
        Label responseBadge = createBadge(responseText);
        Label readBadge = new Label(da.isRead() ? "Lu" : "Non lu");
        readBadge.getStyleClass().addAll("badge", da.isRead() ? "badge-info" : "badge-neutral");

        Label title = new Label("Notification #" + truncateId(da.getDonorAlertId()));
        title.getStyleClass().add("log-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(10, responseBadge, readBadge, title, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("log-card-header");

        HBox alertRefRow = buildDetailRow("Ref alerte", truncateId(da.getAlertId()));
        HBox donorRow = buildDetailRow("Donneur", truncateId(da.getDonorId()));
        HBox sentRow = buildDetailRow("Envoye le", formatTimestamp(da.getNotificationSentAt()));
        HBox readRow = buildDetailRow("Lu le", formatTimestamp(da.getReadAt()));
        VBox body = new VBox(6, alertRefRow, donorRow, sentRow, readRow);
        body.getStyleClass().add("log-card-body");

        Region divider = new Region();
        divider.getStyleClass().add("log-card-divider");
        divider.setPrefHeight(1);

        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.getStyleClass().add("btn-log-delete");
        btnDelete.setOnAction(e -> handleDeleteDonorAlert(da));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox footer = new HBox(8, footerSpacer, btnDelete);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("log-card-footer");

        card.getChildren().addAll(header, body, divider, footer);
        return card;
    }

    // ==================== STATS ROW ====================

    private void updateStats(int sectionIdx) {
        if (statsRow == null)
            return;
        statsRow.getChildren().clear();

        switch (sectionIdx) {
            case 0:
                statsRow.getChildren().addAll(
                        buildStatChip("📋", String.valueOf(allDonationLogs != null ? allDonationLogs.size() : 0),
                                "Total journaux"),
                        buildStatChip("✅", countByAction(allDonationLogs), "Collectes"),
                        buildStatChip("❌", countByRejection(allDonationLogs), "Rejetes"));
                break;
            case 1:
                statsRow.getChildren().addAll(
                        buildStatChip("🔄", String.valueOf(allTransferLogs != null ? allTransferLogs.size() : 0),
                                "Total transferts"),
                        buildStatChip("✅", countApproved(allTransferLogs), "Approuves"),
                        buildStatChip("❌", countCancelled(allTransferLogs), "Annules"));
                break;
            case 2:
                int total = allDonorAlerts != null ? allDonorAlerts.size() : 0;
                int read = 0, positive = 0;
                if (allDonorAlerts != null) {
                    for (DonorAlert da : allDonorAlerts) {
                        if (da.isRead())
                            read++;
                        if (da.getDonorResponse() == DonorResponse.INTERESTED)
                            positive++;
                    }
                }
                statsRow.getChildren().addAll(
                        buildStatChip("📬", String.valueOf(total), "Notifications"),
                        buildStatChip("👁", String.valueOf(read), "Lectures"),
                        buildStatChip("💚", String.valueOf(positive), "Interesses"));
                break;
            default:
                break;
        }
    }

    private String countByAction(ObservableList<DonationLog> logs) {
        if (logs == null)
            return "0";
        long count = logs.stream().filter(l -> l.getAction() != null && l.getAction().name().equals("COLLECTED"))
                .count();
        return String.valueOf(count);
    }

    private String countByRejection(ObservableList<DonationLog> logs) {
        if (logs == null)
            return "0";
        long count = logs.stream().filter(l -> l.getAction() != null && l.getAction().name().equals("REJECTED"))
                .count();
        return String.valueOf(count);
    }

    private String countApproved(ObservableList<BloodTransferRequestLog> logs) {
        if (logs == null)
            return "0";
        long count = logs.stream().filter(l -> l.getAction() != null && l.getAction().name().equals("APPROVED"))
                .count();
        return String.valueOf(count);
    }

    private String countCancelled(ObservableList<BloodTransferRequestLog> logs) {
        if (logs == null)
            return "0";
        long count = logs.stream().filter(l -> l.getAction() != null && l.getAction().name().equals("CANCELLED"))
                .count();
        return String.valueOf(count);
    }

    private HBox buildStatChip(String icon, String value, String label) {
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("logs-stat-icon");
        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("logs-stat-value");
        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("logs-stat-label");
        VBox textBox = new VBox(2, valueLbl, labelLbl);
        HBox chip = new HBox(10, iconLbl, textBox);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("logs-stat-chip");
        HBox.setHgrow(chip, javafx.scene.layout.Priority.ALWAYS);
        return chip;
    }

    // ==================== SEARCH ====================

    private void setupSearchListener() {
        if (searchField == null)
            return;
        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(e -> renderCurrentSection());
            debounce.playFromStart();
        });
    }

    // ==================== CARD UTILITY ====================

    private Label createBadge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("badge");
        String normalized = text.toLowerCase();
        if (normalized.contains("approved") || normalized.contains("completed")
                || normalized.contains("collected") || normalized.contains("delivered")
                || normalized.contains("interested") || normalized.contains("confirmed")
                || normalized.contains("released")) {
            badge.getStyleClass().add("badge-success");
        } else if (normalized.contains("pending") || normalized.contains("warning")
                || normalized.contains("screening") || normalized.contains("in_transit")
                || normalized.contains("dispatched") || normalized.contains("not_interested")
                || normalized.contains("already_donated")) {
            badge.getStyleClass().add("badge-warning");
        } else if (normalized.contains("rejected") || normalized.contains("failed")
                || normalized.contains("urgent") || normalized.contains("critical")
                || normalized.contains("cancelled") || normalized.contains("expired")
                || normalized.contains("discarded")) {
            badge.getStyleClass().add("badge-critical");
        } else if (normalized.contains("info") || normalized.contains("created")
                || normalized.contains("requested")) {
            badge.getStyleClass().add("badge-info");
        } else {
            badge.getStyleClass().add("badge-neutral");
        }
        return badge;
    }

    private HBox buildStatusTransition(String previous, String current) {
        Label prevLabel = createBadge(previous != null ? previous : "—");
        Label arrow = new Label("→");
        arrow.getStyleClass().add("log-status-arrow");
        Label newLabel = createBadge(current != null ? current : "—");
        Label label = new Label("Statut");
        label.getStyleClass().add("log-card-detail-label");
        HBox row = new HBox(8, label, prevLabel, arrow, newLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildDetailRow(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("log-card-detail-label");
        Label val = new Label(value != null ? value : "—");
        val.getStyleClass().add("log-card-detail-value");
        val.setWrapText(true);
        HBox row = new HBox(8, label, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildEmptyState(String title, String subtitle) {
        Label icon = new Label("📭");
        icon.getStyleClass().add("logs-empty-icon");
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("logs-empty-title");
        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.getStyleClass().add("logs-empty-subtitle");
        VBox box = new VBox(8, icon, titleLbl, subtitleLbl);
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
        return ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @FunctionalInterface
    private interface FilterPredicate<T> {
        boolean test(T item);
    }

    private <T> List<T> filterList(ObservableList<T> source, String filter, FilterPredicate<T> predicate) {
        if (source == null)
            return List.of();
        if (filter == null || filter.isEmpty())
            return source;
        return source.filtered(predicate::test);
    }

    private boolean safeContains(String value, String search) {
        if (search == null || search.isEmpty())
            return true;
        return value != null && value.toLowerCase().contains(search.toLowerCase());
    }

    // ==================== CRUD HANDLERS ====================

    @FXML
    void handleAddAction(ActionEvent event) {
        int idx = getCurrentSectionIndex();
        switch (idx) {
            case 0:
                handleAddDonationLog(event);
                break;
            case 1:
                handleAddTransferLog(event);
                break;
            default:
                break;
        }
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        if (btnRefresh == null) {
            refreshData();
            return;
        }

        btnRefresh.setDisable(true);
        String originalText = btnRefresh.getText() != null ? btnRefresh.getText() : "Rafraichir";
        btnRefresh.setText("Chargement...");

        PauseTransition restoreState = new PauseTransition(Duration.millis(500));
        restoreState.setOnFinished(e -> {
            btnRefresh.setText(originalText);
            btnRefresh.setDisable(false);
        });

        try {
            refreshData();
        } finally {
            restoreState.play();
        }
    }

    public void refreshData() {
        loadData();
        loadCharts();
        renderCurrentSection();
    }

    private void handleEditDonationLog(DonationLog log) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/edu/esprit/views/AddDonationLogDialog.fxml"));
            Parent parent = loader.load();
            AddDonationLogDialogController controller = loader.getController();
            controller.setParentController(this);
            controller.setEditMode(log);

            Stage stage = new Stage();
            stage.setTitle("Modifier journal de don");
            Scene scene = new Scene(parent);
            URL cssUrl = getClass().getResource("/tn/edu/esprit/styles/dashboard.css");
            if (cssUrl != null)
                scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la fenetre de modification: " + e.getMessage());
        }
    }

    private void handleEditTransferLog(BloodTransferRequestLog log) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/edu/esprit/views/AddTransferLogDialog.fxml"));
            Parent parent = loader.load();
            AddTransferLogDialogController controller = loader.getController();
            controller.setParentController(this);
            controller.setEditMode(log);

            Stage stage = new Stage();
            stage.setTitle("Modifier journal de transfert");
            Scene scene = new Scene(parent);
            URL cssUrl = getClass().getResource("/tn/edu/esprit/styles/dashboard.css");
            if (cssUrl != null)
                scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la fenetre de modification: " + e.getMessage());
        }
    }

    // ==================== DELETE HANDLERS ====================

    private void handleDeleteDonationLog(DonationLog log) {
        Optional<ButtonType> result = showConfirmation(
                "Supprimer journal de don",
                "Voulez-vous vraiment supprimer cette entree de journal de don ?\nID journal: " + log.getLogId());
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                donationLogService.supprimer(log.getLogId());
                refreshData();
            } catch (Exception e) {
                showAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    private void handleDeleteTransferLog(BloodTransferRequestLog log) {
        Optional<ButtonType> result = showConfirmation(
                "Supprimer journal de transfert",
                "Voulez-vous vraiment supprimer cette entree de journal de transfert ?\nID journal: "
                        + log.getLogId());
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                transferLogService.supprimer(log.getLogId());
                refreshData();
            } catch (Exception e) {
                showAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    private void handleDeleteDonorAlert(DonorAlert da) {
        Optional<ButtonType> result = showConfirmation(
                "Supprimer alerte donneur",
                "Voulez-vous vraiment supprimer cette notification ?\nID: " + da.getDonorAlertId());
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                donorAlertService.supprimer(da.getDonorAlertId());
                refreshData();
            } catch (Exception e) {
                showAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    // ==================== NAVIGATION HANDLERS ====================

    @FXML
    void handleAddDonationLog(ActionEvent event) {
        openModal("/tn/edu/esprit/views/AddDonationLogDialog.fxml", "Ajouter journal de don");
    }

    @FXML
    void handleAddTransferLog(ActionEvent event) {
        openModal("/tn/edu/esprit/views/AddTransferLogDialog.fxml", "Ajouter journal de transfert");
    }

    @FXML
    void handleBroadcastAlert(ActionEvent event) {
        openModal("/tn/edu/esprit/views/BroadcastAlertDialog.fxml", "Diffuser une alerte d'urgence");
    }

    private void openModal(String fxmlPath, String title) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                showAlert("Erreur", "Ressource introuvable: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent parent = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AddDonationLogDialogController) {
                ((AddDonationLogDialogController) controller).setParentController(this);
            } else if (controller instanceof AddTransferLogDialogController) {
                ((AddTransferLogDialogController) controller).setParentController(this);
            } else if (controller instanceof BroadcastAlertDialogController) {
                ((BroadcastAlertDialogController) controller).setParentController(this);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            Scene scene = new Scene(parent);

            URL cssUrl = getClass().getResource("/tn/edu/esprit/styles/dashboard.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la fenetre: " + e.getMessage());
        }
    }

    // ==================== UTILITY ====================

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirmation(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    // ==================== CHARTS ====================

    private void loadCharts() {
        loadSeverityPieChart();
        loadDonationBarChart();
    }

    private void loadSeverityPieChart() {
        if (pieChartSeverity == null)
            return;
        try {
            List<tn.edu.esprit.entities.Alert> alerts = alertService.getAll();
            Map<String, Integer> severityCounts = new HashMap<>();
            for (tn.edu.esprit.entities.Alert a : alerts) {
                String sev = a.getSeverity() != null ? a.getSeverity().name() : "INCONNU";
                severityCounts.put(sev, severityCounts.getOrDefault(sev, 0) + 1);
            }

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : severityCounts.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            pieChartSeverity.setData(pieData);
            pieChartSeverity.setTitle("Repartition des severites");
        } catch (Exception e) {
            System.err.println("Echec du chargement du graphique camembert: " + e.getMessage());
        }
    }

    private void loadDonationBarChart() {
        if (barChartDonations == null)
            return;
        try {
            List<DonationLog> logs = donationLogService.getAll();
            Map<String, Integer> actionCounts = new HashMap<>();
            for (DonationLog log : logs) {
                String action = log.getAction() != null ? log.getAction().name() : "INCONNU";
                actionCounts.put(action, actionCounts.getOrDefault(action, 0) + 1);
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Journaux de don");
            for (Map.Entry<String, Integer> entry : actionCounts.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            barChartDonations.getData().clear();
            barChartDonations.getData().add(series);
        } catch (Exception e) {
            System.err.println("Echec du chargement de l'histogramme: " + e.getMessage());
        }
    }

    // ==================== EXPORT ====================

    @FXML
    void handleExportCSV(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les donnees en CSV");
        chooser.setInitialFileName("bloodlink_export.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));

        Node source = contentStack != null ? contentStack : statsRow;
        if (source == null || source.getScene() == null)
            return;
        Stage stage = (Stage) source.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null)
            return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Section,ID,Action/Severite,Statut/Details,Notes,Horodatage");

            for (DonationLog log : donationLogService.getAll()) {
                pw.println("Journal Don,"
                        + esc(log.getLogId()) + ","
                        + esc(log.getAction() != null ? log.getAction().name() : "") + ","
                        + esc(log.getPreviousStatus()) + " → " + esc(log.getNewStatus()) + ","
                        + esc(log.getNotes()) + ","
                        + esc(log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""));
            }

            for (BloodTransferRequestLog log : transferLogService.getAll()) {
                pw.println("Journal Transfert,"
                        + esc(log.getLogId()) + ","
                        + esc(log.getAction() != null ? log.getAction().name() : "") + ","
                        + esc(log.getPreviousStatus()) + " → " + esc(log.getNewStatus()) + ","
                        + esc(log.getNotes()) + ","
                        + esc(log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""));
            }

            for (tn.edu.esprit.entities.Alert a : alertService.getAll()) {
                pw.println("Alerte,"
                        + esc(a.getAlertId()) + ","
                        + esc(a.getSeverity() != null ? a.getSeverity().name() : "") + ","
                        + esc(a.getTitle()) + ","
                        + esc(a.getMessage()) + ","
                        + esc(a.getCreatedAt() != null ? a.getCreatedAt().toString() : ""));
            }

            for (DonorAlert da : donorAlertService.getAll()) {
                pw.println("Alerte Donneur,"
                        + esc(da.getDonorAlertId()) + ","
                        + esc(da.getAlertId()) + ","
                        + esc(da.getDonorId()) + ","
                        + esc(da.getDonorResponse() != null ? da.getDonorResponse().name() : "NO_RESPONSE") + ","
                        + esc(da.getNotificationSentAt() != null ? da.getNotificationSentAt().toString() : ""));
            }

            showAlert("Export reussi", "CSV exporte vers :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Export echoue", "Erreur: " + e.getMessage());
        }
    }

    @FXML
    void handleExportPDF(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le rapport en PDF");
        chooser.setInitialFileName("Rapport_BloodLink.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        Node source = contentStack != null ? contentStack : statsRow;
        if (source == null || source.getScene() == null)
            return;
        Stage stage = (Stage) source.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null)
            return;

        try {
            List<DonationLog> donations = donationLogService.getAll();
            List<BloodTransferRequestLog> transfers = transferLogService.getAll();
            List<tn.edu.esprit.entities.Alert> alerts = alertService.getAll();

            List<String[]> donRows = new java.util.ArrayList<>();
            for (DonationLog d : donations) {
                donRows.add(new String[] {
                        d.getLogId() != null ? d.getLogId() : "",
                        d.getDonationId() != null ? d.getDonationId() : "",
                        d.getAction() != null ? d.getAction().name() : "",
                        d.getPreviousStatus() != null ? d.getPreviousStatus() : "",
                        d.getNewStatus() != null ? d.getNewStatus() : ""
                });
            }

            List<String[]> trRows = new java.util.ArrayList<>();
            for (BloodTransferRequestLog t : transfers) {
                trRows.add(new String[] {
                        t.getLogId() != null ? t.getLogId() : "",
                        String.valueOf(t.getTransferId()),
                        t.getAction() != null ? t.getAction().name() : "",
                        t.getPreviousStatus() != null ? t.getPreviousStatus() : "",
                        t.getNewStatus() != null ? t.getNewStatus() : ""
                });
            }

            List<String[]> alRows = new java.util.ArrayList<>();
            int resolvedCount = 0, criticalCount = 0;
            for (tn.edu.esprit.entities.Alert a : alerts) {
                alRows.add(new String[] {
                        a.getTitle() != null ? a.getTitle() : "",
                        a.getSeverity() != null ? a.getSeverity().name() : "",
                        a.getBloodTypeId() != null ? a.getBloodTypeId() : "",
                        String.valueOf(a.getQuantityNeeded()),
                        a.isResolved() ? "OUI" : "NON"
                });
                if (a.isResolved())
                    resolvedCount++;
                if (a.getSeverity() != null && a.getSeverity().name().equals("CRITICAL"))
                    criticalCount++;
            }

            new tn.edu.esprit.services.PdfReportGenerator()
                    .generateReport(file, donRows, trRows, alRows, resolvedCount, criticalCount);

            showAlert("Export reussi", "Rapport PDF enregistre dans :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export echoue", "Erreur: " + e.getMessage());
        }
    }

    private String esc(String val) {
        if (val == null)
            return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
