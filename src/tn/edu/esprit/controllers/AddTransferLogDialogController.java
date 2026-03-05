package tn.edu.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.edu.esprit.entities.BloodTransferRequestLog;
import tn.edu.esprit.entities.TransferLogAction;
import tn.edu.esprit.services.BloodTransferRequestLogServiceImpl;
import tn.edu.esprit.services.RuntimeContextService;

import java.net.URL;
import java.util.ResourceBundle;

public class AddTransferLogDialogController implements Initializable {

    @FXML
    private TextField txtTransferId;
    @FXML
    private ComboBox<TransferLogAction> cbAction;
    @FXML
    private TextField txtPreviousStatus;
    @FXML
    private TextField txtNewStatus;
    @FXML
    private TextArea taNotes;

    @FXML
    private Label lblTransferIdError;
    @FXML
    private Label lblActionError;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = new BloodTransferRequestLogServiceImpl();
        runtimeContextService = new RuntimeContextService();
        cbAction.setItems(FXCollections.observableArrayList(TransferLogAction.values()));
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
        txtPreviousStatus.setText(log.getPreviousStatus());
        txtNewStatus.setText(log.getNewStatus());
        taNotes.setText(log.getNotes());

        btnSubmit.setText("Mettre a jour le journal");
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
                lblTransferIdError.setText("L'ID du transfert doit etre un nombre valide.");
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
                if (!service.transferRequestExists(transferId)) {
                    lblTransferIdError.setText("Aucune demande de transfert trouvee pour cet ID.");
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

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}
