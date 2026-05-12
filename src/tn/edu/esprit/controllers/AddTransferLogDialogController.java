package tn.edu.esprit.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.edu.esprit.entities.BloodTransferRequestLog;
import tn.edu.esprit.entities.TransferLogAction;
import tn.edu.esprit.services.BloodTransferRequestLogServiceImpl;
import tn.edu.esprit.services.RuntimeContextService;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class AddTransferLogDialogController implements Initializable {

    @FXML
    private TextField txtTransferId;
    @FXML
    private ComboBox<TransferLogAction> cbAction;
    @FXML
    private ComboBox<String> cbPreviousStatus;
    @FXML
    private ComboBox<String> cbNewStatus;
    @FXML
    private TextArea taNotes;

    @FXML
    private Label lblTransferIdError;
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

    private BloodTransferRequestLogServiceImpl service;
    private RuntimeContextService runtimeContextService;
    private DashboardLogsController parentController;

    // Edit mode fields
    private boolean isEditMode = false;
    private BloodTransferRequestLog editingLog = null;
    private String detectedStatus;
    private String suggestedNewStatus;
    private String autoDetectedPreviousStatus;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = new BloodTransferRequestLogServiceImpl();
        runtimeContextService = new RuntimeContextService();
        cbAction.setItems(FXCollections.observableArrayList(TransferLogAction.values()));
        configureActionComboBox();
        loadStatusOptions();
        configureStatusComboBox(cbPreviousStatus);
        configureStatusComboBox(cbNewStatus);
        setupTransferDetection();
        cbAction.setOnAction(event -> suggestNewStatusFromAction());
    }

    public void setParentController(DashboardLogsController controller) {
        this.parentController = controller;
    }

    /**
     * Switches dialog to edit mode, pre-filling fields with existing log data.
     */
    public void setEditMode(BloodTransferRequestLog log) {
        this.isEditMode = true;
        this.editingLog = log;

        txtTransferId.setText(String.valueOf(log.getTransferId()));
        txtTransferId.setEditable(false);
        txtTransferId.setStyle("-fx-opacity: 0.7;");

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

        String transferText = txtTransferId.getText() != null ? txtTransferId.getText().trim() : "";
        TransferLogAction action = cbAction.getValue();
        String notes = taNotes.getText() != null ? taNotes.getText().trim() : "";

        boolean isValid = true;
        Integer transferId = null;

        if (transferText.isEmpty()) {
            lblTransferIdError.setText("L'ID du transfert est obligatoire.");
            lblTransferIdError.setVisible(true);
            lblTransferIdError.setManaged(true);
            isValid = false;
        } else {
            try {
                transferId = Integer.parseInt(transferText);
            } catch (NumberFormatException e) {
                lblTransferIdError.setText("L'ID du transfert doit être un nombre valide.");
                lblTransferIdError.setVisible(true);
                lblTransferIdError.setManaged(true);
                isValid = false;
            }
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
                if (!service.transferRequestExists(transferId)) {
                    lblTransferIdError.setText("Aucune demande de transfert trouvée pour cet ID.");
                    lblTransferIdError.setVisible(true);
                    lblTransferIdError.setManaged(true);
                    return;
                }

                BloodTransferRequestLog log = new BloodTransferRequestLog();
                log.setTransferId(transferId);
                log.setAction(action);
                log.setPreviousStatus(prevStatus.isEmpty() ? null : prevStatus);
                log.setNewStatus(newStat.isEmpty() ? null : newStat);
                log.setNotes(notes.isEmpty() ? null : notes);
                log.setChangedBy(runtimeContextService.resolveAuditUserId());
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
        lblTransferIdError.setVisible(false);
        lblTransferIdError.setManaged(false);
        lblActionError.setVisible(false);
        lblActionError.setManaged(false);
        lblStatusMessage.setVisible(false);
        lblStatusMessage.setManaged(false);
    }

    private void loadStatusOptions() {
        List<String> statuses = new ArrayList<>(service.getKnownTransferStatuses());
        statuses.sort(Comparator.comparing(this::formatStatusDisplay, String.CASE_INSENSITIVE_ORDER));
        cbPreviousStatus.setItems(FXCollections.observableArrayList(statuses));
        cbNewStatus.setItems(FXCollections.observableArrayList(statuses));
    }

    private void setupTransferDetection() {
        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        txtTransferId.textProperty().addListener((obs, oldValue, newValue) -> {
            if (isEditMode) {
                return;
            }
            debounce.setOnFinished(event -> detectTransferStatus(newValue));
            debounce.playFromStart();
        });
    }

    private void detectTransferStatus(String transferText) {
        clearDetectionMessageIfNeeded();
        clearAutoDetectedPreviousStatus();
        detectedStatus = null;

        String trimmed = transferText != null ? transferText.trim() : "";
        if (trimmed.isEmpty()) {
            return;
        }

        try {
            int transferId = Integer.parseInt(trimmed);
            String currentStatus = service.getCurrentTransferStatus(transferId);
            if (currentStatus == null || currentStatus.isBlank()) {
                setDetectionMessage("Transfert trouvé, mais aucun statut exploitable n'a été remonté. Sélectionnez-le manuellement si besoin.", true);
                return;
            }

            detectedStatus = currentStatus.trim();
            ensureStatusPresent(cbPreviousStatus, detectedStatus);
            ensureStatusPresent(cbNewStatus, detectedStatus);
            if (cbPreviousStatus.getValue() == null || cbPreviousStatus.getValue().isBlank()
                    || detectedStatus.equalsIgnoreCase(cbPreviousStatus.getValue())) {
                cbPreviousStatus.setValue(detectedStatus);
                autoDetectedPreviousStatus = detectedStatus;
            }
            setDetectionMessage("Statut actuel détecté automatiquement : " + formatStatusDisplay(detectedStatus) + ".", false);
            suggestNewStatusFromAction();
        } catch (NumberFormatException exception) {
            setDetectionMessage("Saisissez un identifiant numérique valide pour détecter automatiquement le statut actuel.", true);
        } catch (Exception exception) {
            setDetectionMessage("Détection impossible : " + exception.getMessage(), true);
        }
    }

    private void suggestNewStatusFromAction() {
        TransferLogAction action = cbAction.getValue();
        if (action == null) {
            return;
        }

        String mappedStatus = switch (action) {
            case REQUESTED -> "PENDING";
            case APPROVED -> "APPROVED";
            case REJECTED -> "REJECTED";
            case DISPATCHED, IN_TRANSIT -> "IN_TRANSIT";
            case RECEIVED, CONFIRMED -> "DELIVERED";
            case CANCELLED -> "CANCELLED";
            case EXPIRED -> "EXPIRED";
        };

        ensureStatusPresent(cbNewStatus, mappedStatus);
        if (cbNewStatus.getValue() == null || cbNewStatus.getValue().isBlank()
                || (suggestedNewStatus != null && suggestedNewStatus.equalsIgnoreCase(cbNewStatus.getValue()))) {
            cbNewStatus.setValue(mappedStatus);
        }
        suggestedNewStatus = mappedStatus;
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
            protected void updateItem(TransferLogAction item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatTransferAction(item));
            }
        });
        cbAction.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TransferLogAction item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatTransferAction(item));
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

    private String formatTransferAction(TransferLogAction action) {
        if (action == null) {
            return "";
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

    private String formatStatusDisplay(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        String label = switch (normalized) {
            case "PENDING" -> "En attente";
            case "APPROVED" -> "Approuvé";
            case "REJECTED" -> "Rejeté";
            case "IN_TRANSIT" -> "En transit";
            case "DELIVERED", "RECEIVED", "CONFIRMED" -> "Livré";
            case "CANCELLED" -> "Annulé";
            case "EXPIRED" -> "Expiré";
            case "COMPLETED" -> "Terminé";
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
