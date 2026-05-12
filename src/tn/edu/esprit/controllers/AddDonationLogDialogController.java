package tn.edu.esprit.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.edu.esprit.entities.DonationLog;
import tn.edu.esprit.entities.DonationLogAction;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.services.DonationLogServiceImpl;
import tn.edu.esprit.services.RuntimeContextService;
import tn.edu.esprit.services.ServiceDonation;
import tn.edu.esprit.services.SessionScopeService;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

public class AddDonationLogDialogController implements Initializable {

    @FXML
    private ComboBox<String> cbDonation;
    @FXML
    private ComboBox<DonationLogAction> cbAction;
    @FXML
    private ComboBox<String> cbPreviousStatus;
    @FXML
    private ComboBox<String> cbNewStatus;
    @FXML
    private TextArea taNotes;

    @FXML
    private Label lblDonationIdError;
    @FXML
    private Label lblActionError;
    @FXML
    private Label lblStatusDetection;
    @FXML
    private Label lblStatusMessage;

    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSubmit;

    private DonationLogServiceImpl service;
    private ServiceDonation donationService;
    private SessionScopeService sessionScopeService;
    private RuntimeContextService runtimeContextService;
    private DashboardLogsController parentController;
    private final Map<String, String> donationIdByLabel = new LinkedHashMap<>();

    // Edit mode fields
    private boolean isEditMode = false;
    private DonationLog editingLog = null;
    private String detectedStatus;
    private String suggestedNewStatus;
    private String autoDetectedPreviousStatus;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = new DonationLogServiceImpl();
        donationService = new ServiceDonation();
        sessionScopeService = new SessionScopeService();
        runtimeContextService = new RuntimeContextService();
        loadDonationOptions();
        configureDonationComboBox();
        cbAction.setItems(FXCollections.observableArrayList(DonationLogAction.values()));
        configureActionComboBox();
        loadStatusOptions();
        configureStatusComboBox(cbPreviousStatus);
        configureStatusComboBox(cbNewStatus);
        setupDonationDetection();
        cbAction.setOnAction(event -> suggestNewStatusFromAction());
    }

    public void setParentController(DashboardLogsController controller) {
        this.parentController = controller;
    }

    /**
     * Switches dialog to edit mode, pre-filling fields with existing log data.
     */
    public void setEditMode(DonationLog log) {
        this.isEditMode = true;
        this.editingLog = log;

        selectDonationById(log.getDonationId());
        cbDonation.setDisable(true);
        cbDonation.setStyle("-fx-opacity: 0.7;");

        cbAction.setValue(log.getAction());
        setStatusSelection(cbPreviousStatus, log.getPreviousStatus());
        setStatusSelection(cbNewStatus, log.getNewStatus());
        taNotes.setText(log.getNotes());
        autoDetectedPreviousStatus = log.getPreviousStatus();
        suggestedNewStatus = log.getNewStatus();
        setDetectionMessage("Mode modification : les statuts proviennent du journal existant.", false);

        btnSubmit.setText("Mettre à jour le journal");
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        clearErrors();

        String donationId = getSelectedDonationId();
        DonationLogAction action = cbAction.getValue();
        String notes = taNotes.getText() != null ? taNotes.getText().trim() : "";

        boolean isValid = true;

        if (donationId == null || donationId.isBlank()) {
            lblDonationIdError.setText("Sélectionnez un don.");
            lblDonationIdError.setVisible(true);
            lblDonationIdError.setManaged(true);
            isValid = false;
        }

        if (action == null) {
            lblActionError.setText("L'action est obligatoire.");
            lblActionError.setVisible(true);
            lblActionError.setManaged(true);
            isValid = false;
        }

        if (!isValid)
            return;

        try {
            String prevStatus = cbPreviousStatus.getValue() != null ? cbPreviousStatus.getValue().trim() : "";
            String newStat = cbNewStatus.getValue() != null ? cbNewStatus.getValue().trim() : "";

            if (isEditMode && editingLog != null) {
                // UPDATE mode
                editingLog.setAction(action);
                editingLog.setPreviousStatus(prevStatus.isEmpty() ? null : prevStatus);
                editingLog.setNewStatus(newStat.isEmpty() ? null : newStat);
                editingLog.setNotes(notes.isEmpty() ? null : notes);
                service.modifier(editingLog);
            } else {
                // CREATE mode
                if (!service.donationExists(donationId)) {
                    lblDonationIdError.setText("Aucun don trouvé avec cet ID.");
                    lblDonationIdError.setVisible(true);
                    lblDonationIdError.setManaged(true);
                    return;
                }

                DonationLog log = new DonationLog();
                log.setDonationId(donationId);
                log.setAction(action);
                log.setPreviousStatus(prevStatus.isEmpty() ? null : prevStatus);
                log.setNewStatus(newStat.isEmpty() ? null : newStat);
                log.setNotes(notes.isEmpty() ? null : notes);
                log.setLoggedBy(runtimeContextService.resolveAuditUserId());
                service.ajouter(log);
            }

            if (parentController != null) {
                parentController.refreshData();
            }

            closeDialog();
        } catch (Exception e) {
            lblStatusMessage.setText("Erreur: " + e.getMessage());
            lblStatusMessage.setStyle("-fx-text-fill: #ef4444;");
            lblStatusMessage.setVisible(true);
            lblStatusMessage.setManaged(true);
        }
    }

    private void clearErrors() {
        lblDonationIdError.setVisible(false);
        lblDonationIdError.setManaged(false);
        lblActionError.setVisible(false);
        lblActionError.setManaged(false);
        lblStatusMessage.setVisible(false);
        lblStatusMessage.setManaged(false);
    }

    private void loadStatusOptions() {
        List<String> statuses = new ArrayList<>(service.getKnownDonationStatuses());
        statuses.sort(Comparator.comparing(this::formatStatusDisplay, String.CASE_INSENSITIVE_ORDER));
        cbPreviousStatus.setItems(FXCollections.observableArrayList(statuses));
        cbNewStatus.setItems(FXCollections.observableArrayList(statuses));
    }

    private void loadDonationOptions() {
        donationIdByLabel.clear();
        List<Donations> donations = sessionScopeService.filterVisibleDonations(donationService.getAll());
        donations.sort(Comparator
                .comparing(Donations::getDonationDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Donations::getDonationId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        for (Donations donation : donations) {
            String donationId = donation.getDonationId();
            if (donationId == null || donationId.isBlank()) {
                continue;
            }
            String label = formatDonationOption(donation);
            donationIdByLabel.put(label, donationId);
        }

        cbDonation.setItems(FXCollections.observableArrayList(donationIdByLabel.keySet()));
    }

    private void configureDonationComboBox() {
        cbDonation.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
        cbDonation.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
    }

    private void setupDonationDetection() {
        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        cbDonation.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (isEditMode) {
                return;
            }
            debounce.setOnFinished(event -> detectDonationStatus(getSelectedDonationId(newValue)));
            debounce.playFromStart();
        });
    }

    private void detectDonationStatus(String donationText) {
        clearDetectionMessageIfNeeded();
        clearAutoDetectedPreviousStatus();
        detectedStatus = null;

        String donationId = donationText != null ? donationText.trim() : "";
        if (donationId.isEmpty()) {
            return;
        }

        try {
            UUID.fromString(donationId);
        } catch (IllegalArgumentException exception) {
            setDetectionMessage("Saisissez un UUID valide pour détecter automatiquement le statut actuel.", true);
            return;
        }

        try {
            String currentStatus = service.getCurrentDonationStatus(donationId);
            if (currentStatus == null || currentStatus.isBlank()) {
                setDetectionMessage("Don trouvé, mais aucun statut exploitable n'a été remonté. Sélectionnez-le manuellement si besoin.", true);
                return;
            }

            detectedStatus = currentStatus.trim();
            ensureStatusPresent(cbPreviousStatus, detectedStatus);
            ensureStatusPresent(cbNewStatus, detectedStatus);
            if (cbPreviousStatus.getValue() == null
                    || cbPreviousStatus.getValue().isBlank()
                    || previousStatusMatchesDetected()) {
                cbPreviousStatus.setValue(detectedStatus);
                autoDetectedPreviousStatus = detectedStatus;
            }
            setDetectionMessage("Statut actuel détecté automatiquement : " + formatStatusDisplay(detectedStatus) + ".", false);
            suggestNewStatusFromAction();
        } catch (Exception exception) {
            setDetectionMessage("Détection impossible : " + exception.getMessage(), true);
        }
    }

    private void suggestNewStatusFromAction() {
        DonationLogAction action = cbAction.getValue();
        if (action == null) {
            return;
        }

        String mappedStatus = switch (action) {
            case CREATED -> "CREATED";
            case SCREENING_STARTED -> "SCREENING";
            case SCREENING_PASSED -> "SCREENING_PASSED";
            case SCREENING_FAILED -> "SCREENING_FAILED";
            case COLLECTED -> "COMPLETED";
            case REJECTED -> "REJECTED";
            case QUARANTINED -> "QUARANTINED";
            case RELEASED -> "RELEASED";
            case USED -> "USED";
            case EXPIRED -> "EXPIRED";
            case DISCARDED -> "DISCARDED";
        };

        if (mappedStatus == null || mappedStatus.isBlank()) {
            return;
        }

        ensureStatusPresent(cbNewStatus, mappedStatus);
        if (cbNewStatus.getValue() == null || cbNewStatus.getValue().isBlank()
                || (suggestedNewStatus != null && suggestedNewStatus.equalsIgnoreCase(cbNewStatus.getValue()))) {
            cbNewStatus.setValue(mappedStatus);
        }
        suggestedNewStatus = mappedStatus;
    }

    private boolean previousStatusMatchesDetected() {
        return detectedStatus != null && detectedStatus.equalsIgnoreCase(cbPreviousStatus.getValue());
    }

    private void clearAutoDetectedPreviousStatus() {
        if (autoDetectedPreviousStatus != null
                && Objects.equals(cbPreviousStatus.getValue(), autoDetectedPreviousStatus)) {
            cbPreviousStatus.setValue(null);
        }
        autoDetectedPreviousStatus = null;
    }

    private void ensureStatusPresent(ComboBox<String> comboBox, String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        if (!comboBox.getItems().contains(status)) {
            comboBox.getItems().add(status);
            FXCollections.sort(comboBox.getItems());
        }
    }

    private void configureActionComboBox() {
        cbAction.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(DonationLogAction item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDonationAction(item));
            }
        });
        cbAction.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DonationLogAction item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDonationAction(item));
            }
        });
    }

    private void configureStatusComboBox(ComboBox<String> comboBox) {
        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatStatusDisplay(item));
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatStatusDisplay(item));
            }
        });
    }

    private String formatDonationAction(DonationLogAction action) {
        if (action == null) {
            return "";
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

    private String formatStatusDisplay(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        String label = switch (normalized) {
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
            case "DELIVERED" -> "Livré";
            case "CANCELLED" -> "Annulé";
            default -> humanizeStatus(normalized);
        };

        return label + " (" + normalized + ")";
    }

    private String humanizeStatus(String status) {
        String[] parts = status.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private void setStatusSelection(ComboBox<String> comboBox, String value) {
        if (value == null || value.isBlank()) {
            comboBox.setValue(null);
            return;
        }
        ensureStatusPresent(comboBox, value.trim());
        comboBox.setValue(value.trim());
    }

    private String getSelectedDonationId() {
        return getSelectedDonationId(cbDonation.getValue());
    }

    private String getSelectedDonationId(String label) {
        return label == null ? null : donationIdByLabel.get(label);
    }

    private void selectDonationById(String donationId) {
        if (donationId == null || donationId.isBlank()) {
            cbDonation.setValue(null);
            return;
        }
        for (Map.Entry<String, String> entry : donationIdByLabel.entrySet()) {
            if (donationId.equalsIgnoreCase(entry.getValue())) {
                cbDonation.setValue(entry.getKey());
                return;
            }
        }
        String label = "Don #" + shortId(donationId) + " - journal existant";
        donationIdByLabel.put(label, donationId);
        cbDonation.getItems().add(label);
        cbDonation.setValue(label);
    }

    private String formatDonationOption(Donations donation) {
        String id = shortId(donation.getDonationId());
        String status = donation.getStatus() == null || donation.getStatus().isBlank()
                ? "statut inconnu"
                : donation.getStatus();
        String bloodType = donation.getBloodTypeId() == null || donation.getBloodTypeId().isBlank()
                ? "groupe inconnu"
                : donation.getBloodTypeId();
        String date = donation.getDonationDate() == null
                ? "date inconnue"
                : donation.getDonationDate().toLocalDate().toString();
        return "Don #" + id + " - " + bloodType + " - " + status + " - " + date;
    }

    private String shortId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= 8 ? value : value.substring(0, 8);
    }

    private void setDetectionMessage(String message, boolean isError) {
        if (lblStatusDetection == null) {
            return;
        }
        lblStatusDetection.setText(message);
        lblStatusDetection.setStyle(isError ? "-fx-text-fill: #b45309;" : "-fx-text-fill: #2563eb;");
        lblStatusDetection.setVisible(true);
        lblStatusDetection.setManaged(true);
    }

    private void clearDetectionMessageIfNeeded() {
        if (lblStatusDetection == null) {
            return;
        }
        lblStatusDetection.setText("");
        lblStatusDetection.setVisible(false);
        lblStatusDetection.setManaged(false);
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}
