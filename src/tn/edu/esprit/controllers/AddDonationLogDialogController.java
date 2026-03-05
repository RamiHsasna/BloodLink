package tn.edu.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.edu.esprit.entities.DonationLog;
import tn.edu.esprit.entities.DonationLogAction;
import tn.edu.esprit.services.DonationLogServiceImpl;
import tn.edu.esprit.services.RuntimeContextService;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

public class AddDonationLogDialogController implements Initializable {

    @FXML
    private TextField txtDonationId;
    @FXML
    private ComboBox<DonationLogAction> cbAction;
    @FXML
    private TextField txtPreviousStatus;
    @FXML
    private TextField txtNewStatus;
    @FXML
    private TextArea taNotes;

    @FXML
    private Label lblDonationIdError;
    @FXML
    private Label lblActionError;
    @FXML
    private Label lblStatusMessage;

    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSubmit;

    private DonationLogServiceImpl service;
    private RuntimeContextService runtimeContextService;
    private DashboardLogsController parentController;

    // Edit mode fields
    private boolean isEditMode = false;
    private DonationLog editingLog = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = new DonationLogServiceImpl();
        runtimeContextService = new RuntimeContextService();
        cbAction.setItems(FXCollections.observableArrayList(DonationLogAction.values()));
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

        txtDonationId.setText(log.getDonationId());
        txtDonationId.setEditable(false);
        txtDonationId.setStyle("-fx-opacity: 0.7;");

        cbAction.setValue(log.getAction());
        txtPreviousStatus.setText(log.getPreviousStatus());
        txtNewStatus.setText(log.getNewStatus());
        taNotes.setText(log.getNotes());

        btnSubmit.setText("Mettre a jour l'entree de journal");
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        clearErrors();

        String donationId = txtDonationId.getText() != null ? txtDonationId.getText().trim() : "";
        DonationLogAction action = cbAction.getValue();
        String notes = taNotes.getText() != null ? taNotes.getText().trim() : "";

        boolean isValid = true;

        if (donationId.isEmpty()) {
            lblDonationIdError.setText("L'ID du don est obligatoire.");
            lblDonationIdError.setVisible(true);
            lblDonationIdError.setManaged(true);
            isValid = false;
        } else {
            try {
                UUID.fromString(donationId);
            } catch (IllegalArgumentException e) {
                lblDonationIdError.setText("L'ID du don doit etre un UUID valide.");
                lblDonationIdError.setVisible(true);
                lblDonationIdError.setManaged(true);
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
            String prevStatus = txtPreviousStatus.getText() != null ? txtPreviousStatus.getText().trim() : "";
            String newStat = txtNewStatus.getText() != null ? txtNewStatus.getText().trim() : "";

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
                    lblDonationIdError.setText("Aucun don trouve avec cet ID.");
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

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}
