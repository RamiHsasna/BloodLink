package tn.edu.esprit.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardLogsController implements Initializable {

    // Section names
    private static final String SEC_DONATION_LOGS = "Journaux de dons";
    private static final String SEC_TRANSFER_LOGS = "Journaux de transferts";
    private static final String SEC_DONOR_RESPONSES = "Réponses des donneurs";
    private static final String SEC_STATISTICS = "Statistiques";

    @FXML
    private ComboBox<String> sectionSelector;
    @FXML
    private ComboBox<String> filterSelector;
    @FXML
    private TextField searchField;
    @FXML
    private DatePicker dateFrom;
    @FXML
    private DatePicker dateTo;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnResetFilters;
    @FXML
    private Button btnAiAnalysis;
    @FXML
    private Button btnAdd;
    @FXML
    private HBox statsRow;
    @FXML
    private StackPane contentStack;
    @FXML
    private VBox aiAnalysisCard;
    @FXML
    private Label aiAnalysisContextLabel;
    @FXML
    private Label aiAnalysisStatusLabel;
    @FXML
    private TextArea aiAnalysisOutput;
    @FXML
    private ProgressIndicator aiLoadingIndicator;

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
    private AuditLogAIService auditLogAIService;
    private SessionScopeService sessionScopeService;

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
        auditLogAIService = new AuditLogAIService();
        sessionScopeService = new SessionScopeService();

        if (!sessionScopeService.canAccessAuditLogs()) {
            showAuditAccessDenied();
            return;
        }

        setupSectionSelector();
        setupFilterSelector();
        setupDateFilters();
        setupSearchListener();
        initializeAiCard();
        loadData();
        loadCharts();
        renderCurrentSection();
    }

    private void showAuditAccessDenied() {
        if (sectionSelector != null) {
            sectionSelector.setDisable(true);
        }
        if (filterSelector != null) {
            filterSelector.setDisable(true);
        }
        if (searchField != null) {
            searchField.setDisable(true);
        }
        if (dateFrom != null) {
            dateFrom.setDisable(true);
        }
        if (dateTo != null) {
            dateTo.setDisable(true);
        }
        if (btnRefresh != null) {
            btnRefresh.setDisable(true);
        }
        if (btnResetFilters != null) {
            btnResetFilters.setDisable(true);
        }
        if (btnAiAnalysis != null) {
            btnAiAnalysis.setDisable(true);
        }
        if (btnAdd != null) {
            btnAdd.setDisable(true);
            btnAdd.setVisible(false);
            btnAdd.setManaged(false);
        }
        if (statsRow != null) {
            statsRow.getChildren().clear();
        }
        if (containerDonationLogs != null) {
            containerDonationLogs.getChildren().setAll(
                    buildEmptyState("Accès administrateur requis",
                            "Les journaux d'audit sont réservés aux administrateurs dans cette version."));
        }
        if (scrollDonationLogs != null) {
            scrollDonationLogs.setVisible(true);
        }
        if (scrollTransferLogs != null) {
            scrollTransferLogs.setVisible(false);
        }
        if (scrollDonorAlerts != null) {
            scrollDonorAlerts.setVisible(false);
        }
        if (containerStats != null) {
            containerStats.setVisible(false);
        }
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
            markAiAnalysisStale();
        });

        updateAddButtonVisibility();
    }

    private void setupFilterSelector() {
        if (filterSelector == null)
            return;
        filterSelector.setOnAction(e -> {
            renderCurrentSection();
            markAiAnalysisStale();
        });
        updateFilterOptions();
    }

    private void setupDateFilters() {
        if (dateFrom != null) {
            dateFrom.setOnAction(e -> {
                renderCurrentSection();
                markAiAnalysisStale();
            });
        }
        if (dateTo != null) {
            dateTo.setOnAction(e -> {
                renderCurrentSection();
                markAiAnalysisStale();
            });
        }
    }

    private void initializeAiCard() {
        if (aiAnalysisCard != null) {
            aiAnalysisCard.setManaged(false);
            aiAnalysisCard.setVisible(false);
        }
        if (aiLoadingIndicator != null) {
            aiLoadingIndicator.setManaged(false);
            aiLoadingIndicator.setVisible(false);
        }
        if (aiAnalysisOutput != null) {
            aiAnalysisOutput.setText("");
        }
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
                    options.add(formatDonationAction(action));
                }
                filterSelector.setPromptText("Filtrer par action");
                break;
            case 1:
                for (TransferLogAction action : TransferLogAction.values()) {
                    options.add(formatTransferAction(action));
                }
                filterSelector.setPromptText("Filtrer par action");
                break;
            case 2:
                for (DonorResponse resp : DonorResponse.values()) {
                    options.add(formatDonorResponse(resp));
                }
                options.add("Lus");
                options.add("Non lus");
                filterSelector.setPromptText("Filtrer par réponse");
                break;
            default:
                filterSelector.setPromptText("Filtrer par...");
                break;
        }

        filterSelector.setItems(options);
        filterSelector.getSelectionModel().selectFirst();
        filterSelector.setVisible(idx < 3);
        filterSelector.setManaged(idx < 3);
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
        // Afficher le bouton ajouter pour les 2 premières sections
        boolean showAdd = idx >= 0 && idx <= 1;
        btnAdd.setVisible(showAdd);
        btnAdd.setManaged(showAdd);

        switch (idx) {
            case 0:
                btnAdd.setText("Ajouter un journal de don");
                break;
            case 1:
                btnAdd.setText("Ajouter un journal de transfert");
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
                renderDonationLogCards(getFilteredDonationLogs(search, activeFilter));
                break;
            case 1:
                renderTransferLogCards(getFilteredTransferLogs(search, activeFilter));
                break;
            case 2:
                renderDonorAlertCards(getFilteredDonorAlerts(search, activeFilter));
                break;
            case 3: /* statistiques déjà chargées */
                break;
        }
        updateStats(idx);
    }

    // --- Donation Log Cards ---
    private List<DonationLog> getFilteredDonationLogs(String search, String activeFilter) {
        List<DonationLog> filtered = filterList(allDonationLogs, search, log -> safeContains(log.getLogId(), search)
                || safeContains(log.getDonationId(), search)
                || safeContains(log.getAction() != null ? log.getAction().name() : "", search)
                || safeContains(formatDonationAction(log.getAction()), search)
                || safeContains(log.getPreviousStatus(), search)
                || safeContains(formatStatusLabel(log.getPreviousStatus()), search)
                || safeContains(log.getNewStatus(), search)
                || safeContains(formatStatusLabel(log.getNewStatus()), search)
                || safeContains(log.getNotes(), search));

        if (activeFilter != null) {
            filtered = filtered.stream()
                    .filter(log -> log.getAction() != null && formatDonationAction(log.getAction()).equals(activeFilter))
                    .collect(Collectors.toList());
        }

        return filtered.stream()
                .filter(log -> matchesDateRange(log.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private void renderDonationLogCards(List<DonationLog> filtered) {
        if (containerDonationLogs == null || allDonationLogs == null)
            return;
        containerDonationLogs.getChildren().clear();

        if (filtered.isEmpty()) {
            containerDonationLogs.getChildren()
                    .add(buildEmptyState("Aucun journal de don", "Les journaux de dons apparaîtront ici."));
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
        Label actionBadge = createBadge(log.getAction() != null ? formatDonationAction(log.getAction()) : "—");
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
    private List<BloodTransferRequestLog> getFilteredTransferLogs(String search, String activeFilter) {
        List<BloodTransferRequestLog> filtered = filterList(allTransferLogs, search,
                log -> safeContains(log.getLogId(), search)
                        || safeContains(String.valueOf(log.getTransferId()), search)
                        || safeContains(log.getAction() != null ? log.getAction().name() : "", search)
                        || safeContains(formatTransferAction(log.getAction()), search)
                        || safeContains(log.getPreviousStatus(), search)
                        || safeContains(formatStatusLabel(log.getPreviousStatus()), search)
                        || safeContains(log.getNewStatus(), search)
                        || safeContains(formatStatusLabel(log.getNewStatus()), search)
                        || safeContains(log.getNotes(), search));

        if (activeFilter != null) {
            filtered = filtered.stream()
                    .filter(log -> log.getAction() != null && formatTransferAction(log.getAction()).equals(activeFilter))
                    .collect(Collectors.toList());
        }

        return filtered.stream()
                .filter(log -> matchesDateRange(log.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private void renderTransferLogCards(List<BloodTransferRequestLog> filtered) {
        if (containerTransferLogs == null || allTransferLogs == null)
            return;
        containerTransferLogs.getChildren().clear();

        if (filtered.isEmpty()) {
            containerTransferLogs.getChildren()
                    .add(buildEmptyState("Aucun journal de transfert", "Les journaux de transfert apparaîtront ici."));
            return;
        }

        for (BloodTransferRequestLog log : filtered) {
            containerTransferLogs.getChildren().add(buildTransferLogCard(log));
        }
    }

    private VBox buildTransferLogCard(BloodTransferRequestLog log) {
        VBox card = new VBox(10);
        card.getStyleClass().add("log-card");

        Label actionBadge = createBadge(log.getAction() != null ? formatTransferAction(log.getAction()) : "—");
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
        HBox changedByRow = buildDetailRow("Modifié par", truncateId(log.getChangedBy()));
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
    private List<DonorAlert> getFilteredDonorAlerts(String search, String activeFilter) {
        List<DonorAlert> filtered = filterList(allDonorAlerts, search, da -> safeContains(da.getDonorAlertId(), search)
                || safeContains(da.getAlertId(), search)
                || safeContains(da.getDonorId(), search)
                || safeContains(da.getDonorResponse() != null ? da.getDonorResponse().name() : "", search)
                || safeContains(formatDonorResponse(da.getDonorResponse()), search));

        if (activeFilter != null) {
            if (activeFilter.equals("Lus")) {
                filtered = filtered.stream().filter(DonorAlert::isRead).collect(Collectors.toList());
            } else if (activeFilter.equals("Non lus")) {
                filtered = filtered.stream().filter(da -> !da.isRead()).collect(Collectors.toList());
            } else {
                filtered = filtered.stream()
                        .filter(da -> da.getDonorResponse() != null
                                && formatDonorResponse(da.getDonorResponse()).equals(activeFilter))
                        .collect(Collectors.toList());
            }
        }

        return filtered.stream()
                .filter(da -> matchesDateRange(extractDonorAlertTimestamp(da)))
                .collect(Collectors.toList());
    }

    private void renderDonorAlertCards(List<DonorAlert> filtered) {
        if (containerDonorAlerts == null || allDonorAlerts == null)
            return;
        containerDonorAlerts.getChildren().clear();

        if (filtered.isEmpty()) {
            containerDonorAlerts.getChildren()
                    .add(buildEmptyState("Aucune réponse de donneur", "Les réponses des donneurs apparaîtront ici."));
            return;
        }

        for (DonorAlert da : filtered) {
            containerDonorAlerts.getChildren().add(buildDonorAlertCard(da));
        }
    }

    private VBox buildDonorAlertCard(DonorAlert da) {
        VBox card = new VBox(10);
        card.getStyleClass().add("log-card");

        String responseText = formatDonorResponse(DonorResponse.NO_RESPONSE);
        if (da.getDonorResponse() != null && da.getDonorResponse() != DonorResponse.NO_RESPONSE) {
            responseText = formatDonorResponse(da.getDonorResponse());
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

        HBox alertRefRow = buildDetailRow("Réf. alerte", truncateId(da.getAlertId()));
        HBox donorRow = buildDetailRow("Donneur", truncateId(da.getDonorId()));
        HBox sentRow = buildDetailRow("Envoyé le", formatTimestamp(da.getNotificationSentAt()));
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

        String search = searchField != null ? searchField.getText() : "";
        String activeFilter = getActiveFilter();
        List<DonationLog> filteredDonationLogs = getFilteredDonationLogs(search, sectionIdx == 0 ? activeFilter : null);
        List<BloodTransferRequestLog> filteredTransferLogs = getFilteredTransferLogs(search, sectionIdx == 1 ? activeFilter : null);
        List<DonorAlert> filteredDonorAlerts = getFilteredDonorAlerts(search, sectionIdx == 2 ? activeFilter : null);

        switch (sectionIdx) {
            case 0:
                statsRow.getChildren().addAll(
                        buildStatChip("📋", String.valueOf(filteredDonationLogs.size()),
                                "Total des journaux"),
                        buildStatChip("✅", countByAction(filteredDonationLogs), "Collectes"),
                        buildStatChip("❌", countByRejection(filteredDonationLogs), "Rejets"));
                break;
            case 1:
                statsRow.getChildren().addAll(
                        buildStatChip("🔄", String.valueOf(filteredTransferLogs.size()),
                                "Total transferts"),
                        buildStatChip("✅", countApproved(filteredTransferLogs), "Approuvés"),
                        buildStatChip("❌", countCancelled(filteredTransferLogs), "Annulés"));
                break;
            case 2:
                int total = filteredDonorAlerts.size();
                int read = 0, positive = 0;
                for (DonorAlert da : filteredDonorAlerts) {
                    if (da.isRead()) {
                        read++;
                    }
                    if (da.getDonorResponse() == DonorResponse.INTERESTED) {
                        positive++;
                    }
                }
                statsRow.getChildren().addAll(
                        buildStatChip("📬", String.valueOf(total), "Notifications"),
                        buildStatChip("👁", String.valueOf(read), "Lectures"),
                        buildStatChip("💚", String.valueOf(positive), "Intéressés"));
                break;
            case 3:
                statsRow.getChildren().addAll(
                        buildStatChip("📋", String.valueOf(getFilteredDonationLogs(search, null).size()), "Dons filtrés"),
                        buildStatChip("🔄", String.valueOf(getFilteredTransferLogs(search, null).size()), "Transferts filtrés"),
                        buildStatChip("📬", String.valueOf(getFilteredDonorAlerts(search, null).size()), "Réponses filtrées"));
                break;
            default:
                break;
        }
    }

    private String countByAction(List<DonationLog> logs) {
        if (logs == null)
            return "0";
        long count = logs.stream().filter(l -> l.getAction() != null && l.getAction().name().equals("COLLECTED"))
                .count();
        return String.valueOf(count);
    }

    private String countByRejection(List<DonationLog> logs) {
        if (logs == null)
            return "0";
        long count = logs.stream().filter(l -> l.getAction() != null && l.getAction().name().equals("REJECTED"))
                .count();
        return String.valueOf(count);
    }

    private String countApproved(List<BloodTransferRequestLog> logs) {
        if (logs == null)
            return "0";
        long count = logs.stream().filter(l -> l.getAction() != null && l.getAction().name().equals("APPROVED"))
                .count();
        return String.valueOf(count);
    }

    private String countCancelled(List<BloodTransferRequestLog> logs) {
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
            debounce.setOnFinished(e -> {
                renderCurrentSection();
                markAiAnalysisStale();
            });
            debounce.playFromStart();
        });
    }

    // ==================== CARD UTILITY ====================

    private Label createBadge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("badge");
        String normalized = normalizeForBadge(text);
        if (normalized.contains("approuve") || normalized.contains("termine")
                || normalized.contains("collecte") || normalized.contains("livre")
                || normalized.contains("interesse") || normalized.contains("confirme")
                || normalized.contains("libere")) {
            badge.getStyleClass().add("badge-success");
        } else if (normalized.contains("en attente") || normalized.contains("avertissement")
                || normalized.contains("depistage") || normalized.contains("transit")
                || normalized.contains("expedition") || normalized.contains("non interesse")
                || normalized.contains("deja donne")) {
            badge.getStyleClass().add("badge-warning");
        } else if (normalized.contains("rejete") || normalized.contains("refuse")
                || normalized.contains("urgent") || normalized.contains("critique")
                || normalized.contains("annule") || normalized.contains("expire")
                || normalized.contains("detruit")) {
            badge.getStyleClass().add("badge-critical");
        } else if (normalized.contains("info") || normalized.contains("cree")
                || normalized.contains("demande")) {
            badge.getStyleClass().add("badge-info");
        } else {
            badge.getStyleClass().add("badge-neutral");
        }
        return badge;
    }

    private HBox buildStatusTransition(String previous, String current) {
        Label prevLabel = createBadge(previous != null ? formatStatusLabel(previous) : "—");
        Label arrow = new Label("→");
        arrow.getStyleClass().add("log-status-arrow");
        Label newLabel = createBadge(current != null ? formatStatusLabel(current) : "—");
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
        if (value == null) {
            return false;
        }
        return normalizeForBadge(value).contains(normalizeForBadge(search));
    }

    private boolean matchesDateRange(Timestamp timestamp) {
        LocalDate from = dateFrom != null ? dateFrom.getValue() : null;
        LocalDate to = dateTo != null ? dateTo.getValue() : null;
        if (from == null && to == null) {
            return true;
        }
        if (timestamp == null) {
            return false;
        }

        LocalDate value = timestamp.toLocalDateTime().toLocalDate();
        if (from != null && value.isBefore(from)) {
            return false;
        }
        if (to != null && value.isAfter(to)) {
            return false;
        }
        return true;
    }

    private Timestamp extractDonorAlertTimestamp(DonorAlert alert) {
        if (alert == null) {
            return null;
        }
        return alert.getNotificationSentAt() != null ? alert.getNotificationSentAt() : alert.getReadAt();
    }

    private String buildAnalysisContext() {
        int idx = getCurrentSectionIndex();
        String sectionName = switch (idx) {
            case 0 -> SEC_DONATION_LOGS;
            case 1 -> SEC_TRANSFER_LOGS;
            case 2 -> SEC_DONOR_RESPONSES;
            default -> SEC_STATISTICS;
        };

        String search = searchField != null && !searchField.getText().isBlank()
                ? searchField.getText().trim()
                : "aucune recherche";
        String filter = getActiveFilter() != null ? getActiveFilter() : "tous";
        String range = formatDateRange();

        return "Section: " + sectionName
                + " | Recherche: " + search
                + " | Filtre: " + filter
                + " | Période: " + range;
    }

    private String formatDateRange() {
        LocalDate from = dateFrom != null ? dateFrom.getValue() : null;
        LocalDate to = dateTo != null ? dateTo.getValue() : null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (from == null && to == null) {
            return "toutes les dates";
        }
        if (from != null && to != null) {
            return "du " + from.format(formatter) + " au " + to.format(formatter);
        }
        if (from != null) {
            return "à partir du " + from.format(formatter);
        }
        return "jusqu'au " + to.format(formatter);
    }

    private String formatDonationAction(DonationLogAction action) {
        if (action == null) {
            return "—";
        }
        return switch (action) {
            case CREATED -> "Création du don";
            case SCREENING_STARTED -> "Dépistage lancé";
            case SCREENING_PASSED -> "Dépistage validé";
            case SCREENING_FAILED -> "Dépistage refusé";
            case COLLECTED -> "Collecte effectuée";
            case REJECTED -> "Don rejeté";
            case QUARANTINED -> "Mise en quarantaine";
            case RELEASED -> "Libération du don";
            case USED -> "Don utilisé";
            case EXPIRED -> "Don expiré";
            case DISCARDED -> "Don détruit";
        };
    }

    private String formatTransferAction(TransferLogAction action) {
        if (action == null) {
            return "—";
        }
        return switch (action) {
            case REQUESTED -> "Demande créée";
            case APPROVED -> "Demande approuvée";
            case REJECTED -> "Demande rejetée";
            case DISPATCHED -> "Expédition lancée";
            case IN_TRANSIT -> "Transfert en transit";
            case RECEIVED -> "Réception confirmée";
            case CONFIRMED -> "Livraison confirmée";
            case CANCELLED -> "Transfert annulé";
            case EXPIRED -> "Demande expirée";
        };
    }

    private String formatDonorResponse(DonorResponse response) {
        if (response == null) {
            return "Aucune réponse";
        }
        return switch (response) {
            case INTERESTED -> "Intéressé";
            case NOT_INTERESTED -> "Non intéressé";
            case ALREADY_DONATED -> "Déjà donné";
            case NO_RESPONSE -> "Aucune réponse";
        };
    }

    private String formatAlertSeverity(AlertSeverity severity) {
        if (severity == null) {
            return "Inconnu";
        }
        return switch (severity) {
            case INFO -> "Information";
            case WARNING -> "Avertissement";
            case URGENT -> "Urgent";
            case CRITICAL -> "Critique";
        };
    }

    private String formatStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "CREATED" -> "Créé";
            case "SCREENING", "SCREENING_STARTED" -> "Dépistage en cours";
            case "SCREENING_PASSED" -> "Dépistage validé";
            case "SCREENING_FAILED" -> "Dépistage refusé";
            case "COMPLETED", "COLLECTED" -> "Terminé";
            case "REJECTED" -> "Rejeté";
            case "QUARANTINED" -> "En quarantaine";
            case "RELEASED" -> "Libéré";
            case "USED" -> "Utilisé";
            case "EXPIRED" -> "Expiré";
            case "DISCARDED" -> "Détruit";
            case "PENDING" -> "En attente";
            case "APPROVED" -> "Approuvé";
            case "IN_TRANSIT" -> "En transit";
            case "DELIVERED", "RECEIVED", "CONFIRMED" -> "Livré";
            case "CANCELLED" -> "Annulé";
            case "NO_RESPONSE" -> "Aucune réponse";
            case "INTERESTED" -> "Intéressé";
            case "NOT_INTERESTED" -> "Non intéressé";
            case "ALREADY_DONATED" -> "Déjà donné";
            default -> humanizeValue(status);
        };
    }

    private String humanizeValue(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }

        String[] parts = value.trim().replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String normalizeForBadge(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');
    }

    private void markAiAnalysisStale() {
        if (aiAnalysisCard == null || !aiAnalysisCard.isVisible()) {
            return;
        }
        if (aiAnalysisContextLabel != null) {
            aiAnalysisContextLabel.setText(buildAnalysisContext());
        }
        if (aiAnalysisStatusLabel != null) {
            aiAnalysisStatusLabel.setText("Les filtres ont changé. Relancez l'analyse pour mettre à jour la synthèse.");
        }
    }

    private void setAiCardVisible(boolean visible) {
        if (aiAnalysisCard == null) {
            return;
        }
        aiAnalysisCard.setVisible(visible);
        aiAnalysisCard.setManaged(visible);
    }

    private void setAiLoading(boolean loading) {
        if (aiLoadingIndicator != null) {
            aiLoadingIndicator.setVisible(loading);
            aiLoadingIndicator.setManaged(loading);
        }
        if (btnAiAnalysis != null) {
            btnAiAnalysis.setDisable(loading);
        }
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
        String originalText = btnRefresh.getText() != null ? btnRefresh.getText() : "Rafraîchir";
        btnRefresh.setText("Chargement...");

        PauseTransition restoreState = new PauseTransition(Duration.millis(500));
        restoreState.setOnFinished(e -> {
            btnRefresh.setText(originalText);
            btnRefresh.setDisable(false);
        });

        try {
            refreshData();
            markAiAnalysisStale();
        } finally {
            restoreState.play();
        }
    }

    @FXML
    void handleResetFilters(ActionEvent event) {
        if (searchField != null) {
            searchField.clear();
        }
        if (filterSelector != null && !filterSelector.getItems().isEmpty()) {
            filterSelector.getSelectionModel().selectFirst();
        }
        if (dateFrom != null) {
            dateFrom.setValue(null);
        }
        if (dateTo != null) {
            dateTo.setValue(null);
        }
        renderCurrentSection();
        markAiAnalysisStale();
    }

    @FXML
    void handleAiAnalysis(ActionEvent event) {
        String search = searchField != null ? searchField.getText() : "";
        String activeFilter = getActiveFilter();
        int sectionIdx = getCurrentSectionIndex();

        List<DonationLog> donationLogs = sectionIdx == 1 || sectionIdx == 2
                ? List.of()
                : getFilteredDonationLogs(search, sectionIdx == 0 ? activeFilter : null);
        List<BloodTransferRequestLog> transferLogs = sectionIdx == 0 || sectionIdx == 2
                ? List.of()
                : getFilteredTransferLogs(search, sectionIdx == 1 ? activeFilter : null);
        List<DonorAlert> donorAlerts = sectionIdx == 0 || sectionIdx == 1
                ? List.of()
                : getFilteredDonorAlerts(search, sectionIdx == 2 ? activeFilter : null);

        String context = buildAnalysisContext()
                + " | Volume: "
                + donationLogs.size() + " dons, "
                + transferLogs.size() + " transferts, "
                + donorAlerts.size() + " réponses";

        setAiCardVisible(true);
        setAiLoading(true);
        if (aiAnalysisContextLabel != null) {
            aiAnalysisContextLabel.setText(context);
        }
        if (aiAnalysisStatusLabel != null) {
            aiAnalysisStatusLabel.setText("Analyse en cours...");
        }
        if (aiAnalysisOutput != null) {
            aiAnalysisOutput.setText("");
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return auditLogAIService.analyzeAuditLogs(donationLogs, transferLogs, donorAlerts, context);
            }
        };

        task.setOnSucceeded(workerStateEvent -> {
            setAiLoading(false);
            if (aiAnalysisStatusLabel != null) {
                aiAnalysisStatusLabel.setText("Synthèse générée à partir des filtres actuellement visibles.");
            }
            if (aiAnalysisOutput != null) {
                aiAnalysisOutput.setText(task.getValue());
            }
        });

        task.setOnFailed(workerStateEvent -> {
            setAiLoading(false);
            if (aiAnalysisStatusLabel != null) {
                aiAnalysisStatusLabel.setText("L'analyse IA a échoué. Un résumé local peut toujours être relancé.");
            }
            if (aiAnalysisOutput != null) {
                Throwable error = task.getException();
                aiAnalysisOutput.setText(error != null ? error.getMessage() : "Analyse indisponible.");
            }
        });

        Thread thread = new Thread(task, "bloodlink-audit-ai");
        thread.setDaemon(true);
        thread.start();
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
            showAlert("Erreur", "Impossible de charger la fenêtre de modification : " + e.getMessage());
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
            showAlert("Erreur", "Impossible de charger la fenêtre de modification : " + e.getMessage());
        }
    }

    // ==================== DELETE HANDLERS ====================

    private void handleDeleteDonationLog(DonationLog log) {
        Optional<ButtonType> result = showConfirmation(
                "Supprimer journal de don",
                "Voulez-vous vraiment supprimer cette entrée de journal de don ?\nID journal: " + log.getLogId());
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
                "Voulez-vous vraiment supprimer cette entrée de journal de transfert ?\nID journal: "
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
            showAlert("Erreur", "Impossible de charger la fenêtre : " + e.getMessage());
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
                String sev = formatAlertSeverity(a.getSeverity());
                severityCounts.put(sev, severityCounts.getOrDefault(sev, 0) + 1);
            }

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : severityCounts.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            pieChartSeverity.setData(pieData);
            pieChartSeverity.setTitle("Répartition des sévérités");
        } catch (Exception e) {
            System.err.println("Échec du chargement du graphique camembert : " + e.getMessage());
        }
    }

    private void loadDonationBarChart() {
        if (barChartDonations == null)
            return;
        try {
            List<DonationLog> logs = donationLogService.getAll();
            Map<String, Integer> actionCounts = new HashMap<>();
            for (DonationLog log : logs) {
                String action = formatDonationAction(log.getAction());
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
            System.err.println("Échec du chargement de l'histogramme : " + e.getMessage());
        }
    }

    // ==================== EXPORT ====================

    @FXML
    void handleExportCSV(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les données en CSV");
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
            pw.println("Section,ID,Action/Sévérité,Statut/Détails,Notes,Horodatage");

            for (DonationLog log : donationLogService.getAll()) {
                pw.println("Journal de don,"
                        + esc(log.getLogId()) + ","
                        + esc(formatDonationAction(log.getAction())) + ","
                        + esc(formatStatusLabel(log.getPreviousStatus())) + " → " + esc(formatStatusLabel(log.getNewStatus())) + ","
                        + esc(log.getNotes()) + ","
                        + esc(log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""));
            }

            for (BloodTransferRequestLog log : transferLogService.getAll()) {
                pw.println("Journal de transfert,"
                        + esc(log.getLogId()) + ","
                        + esc(formatTransferAction(log.getAction())) + ","
                        + esc(formatStatusLabel(log.getPreviousStatus())) + " → " + esc(formatStatusLabel(log.getNewStatus())) + ","
                        + esc(log.getNotes()) + ","
                        + esc(log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""));
            }

            for (tn.edu.esprit.entities.Alert a : alertService.getAll()) {
                pw.println("Alerte,"
                        + esc(a.getAlertId()) + ","
                        + esc(formatAlertSeverity(a.getSeverity())) + ","
                        + esc(a.getTitle()) + ","
                        + esc(a.getMessage()) + ","
                        + esc(a.getCreatedAt() != null ? a.getCreatedAt().toString() : ""));
            }

            for (DonorAlert da : donorAlertService.getAll()) {
                pw.println("Alerte donneur,"
                        + esc(da.getDonorAlertId()) + ","
                        + esc(da.getAlertId()) + ","
                        + esc(da.getDonorId()) + ","
                        + esc(formatDonorResponse(da.getDonorResponse())) + ","
                        + esc(da.getNotificationSentAt() != null ? da.getNotificationSentAt().toString() : ""));
            }

            showAlert("Export réussi", "CSV exporté vers :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Export échoué", "Erreur : " + e.getMessage());
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
                        formatDonationAction(d.getAction()),
                        d.getPreviousStatus() != null ? formatStatusLabel(d.getPreviousStatus()) : "",
                        d.getNewStatus() != null ? formatStatusLabel(d.getNewStatus()) : ""
                });
            }

            List<String[]> trRows = new java.util.ArrayList<>();
            for (BloodTransferRequestLog t : transfers) {
                trRows.add(new String[] {
                        t.getLogId() != null ? t.getLogId() : "",
                        String.valueOf(t.getTransferId()),
                        formatTransferAction(t.getAction()),
                        t.getPreviousStatus() != null ? formatStatusLabel(t.getPreviousStatus()) : "",
                        t.getNewStatus() != null ? formatStatusLabel(t.getNewStatus()) : ""
                });
            }

            List<String[]> alRows = new java.util.ArrayList<>();
            int resolvedCount = 0, criticalCount = 0;
            for (tn.edu.esprit.entities.Alert a : alerts) {
                alRows.add(new String[] {
                        a.getTitle() != null ? a.getTitle() : "",
                        formatAlertSeverity(a.getSeverity()),
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

            showAlert("Export réussi", "Rapport PDF enregistré dans :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export échoué", "Erreur : " + e.getMessage());
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
