package tn.edu.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.edu.esprit.entities.Alert;
import tn.edu.esprit.entities.AlertSeverity;
import tn.edu.esprit.services.AlertServiceImpl;
import tn.edu.esprit.services.EmergencyMatchingService;
import tn.edu.esprit.services.RuntimeContextService;

import java.net.URL;
import java.util.ResourceBundle;

public class BroadcastAlertDialogController implements Initializable {

    @FXML
    private TextField txtTitle;
    @FXML
    private TextField txtBloodType;
    @FXML
    private ComboBox<AlertSeverity> cbSeverity;
    @FXML
    private TextField txtQuantity;
    @FXML
    private TextField txtRadius;
    @FXML
    private TextArea taMessage;

    @FXML
    private Label lblTitleError;
    @FXML
    private Label lblBloodTypeError;
    @FXML
    private Label lblSeverityError;
    @FXML
    private Label lblQuantityError;
    @FXML
    private Label lblRadiusError;
    @FXML
    private Label lblMessageError;
    @FXML
    private Label lblStatusMessage;

    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSubmit;

    // Panneau resultat de correspondance
    @FXML
    private VBox matchingResultPanel;
    @FXML
    private Label lblMatchingTitle;
    @FXML
    private Label lblMatchingDonorCount;
    @FXML
    private Label lblMatchingMessage;
    @FXML
    private HBox matchingProcessingBox;
    @FXML
    private ProgressIndicator matchingSpinner;

    private AlertServiceImpl service;
    private RuntimeContextService runtimeContextService;
    private DashboardLogsController parentController;

    // Edit mode fields
    private boolean isEditMode = false;
    private Alert editingAlert = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = new AlertServiceImpl();
        runtimeContextService = new RuntimeContextService();
        cbSeverity.setItems(FXCollections.observableArrayList(AlertSeverity.values()));
    }

    public void setParentController(DashboardLogsController controller) {
        this.parentController = controller;
    }

    /**
     * Switches dialog to edit mode, pre-filling fields with existing alert data.
     */
    public void setEditMode(Alert alert) {
        this.isEditMode = true;
        this.editingAlert = alert;

        txtTitle.setText(alert.getTitle());
        txtBloodType.setText(alert.getBloodTypeId());
        cbSeverity.setValue(alert.getSeverity());
        txtQuantity.setText(String.valueOf(alert.getQuantityNeeded()));
        txtRadius.setText(String.valueOf(alert.getTargetRadiusKm()));
        taMessage.setText(alert.getMessage());

        btnSubmit.setText("Mettre a jour l'alerte");
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        clearErrors();

        String title = txtTitle.getText() != null ? txtTitle.getText().trim() : "";
        String bloodType = txtBloodType.getText() != null ? txtBloodType.getText().trim() : "";
        AlertSeverity severity = cbSeverity.getValue();
        String quantityText = txtQuantity.getText() != null ? txtQuantity.getText().trim() : "";
        String radiusText = txtRadius.getText() != null ? txtRadius.getText().trim() : "";
        String message = taMessage.getText() != null ? taMessage.getText().trim() : "";
        String normalizedBloodType = bloodType.replace(" ", "").toUpperCase();

        boolean isValid = true;

        if (title.isEmpty()) {
            lblTitleError.setText("Le titre est obligatoire.");
            lblTitleError.setVisible(true);
            lblTitleError.setManaged(true);
            isValid = false;
        }

        if (bloodType.isEmpty()) {
            lblBloodTypeError.setText("Le groupe sanguin est obligatoire.");
            lblBloodTypeError.setVisible(true);
            lblBloodTypeError.setManaged(true);
            isValid = false;
        } else if (normalizedBloodType.length() > 3) {
            lblBloodTypeError.setText("Le groupe sanguin doit contenir 3 caracteres max (ex.: O+, AB-).");
            lblBloodTypeError.setVisible(true);
            lblBloodTypeError.setManaged(true);
            isValid = false;
        }

        if (severity == null) {
            lblSeverityError.setText("Le niveau de severite est obligatoire.");
            lblSeverityError.setVisible(true);
            lblSeverityError.setManaged(true);
            isValid = false;
        }

        if (message.isEmpty()) {
            lblMessageError.setText("Le message est obligatoire.");
            lblMessageError.setVisible(true);
            lblMessageError.setManaged(true);
            isValid = false;
        }

        int quantity = 0;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblQuantityError.setText("Saisissez un nombre positif valide.");
            lblQuantityError.setVisible(true);
            lblQuantityError.setManaged(true);
            isValid = false;
        }

        int radius = 0;
        try {
            radius = Integer.parseInt(radiusText);
            if (radius <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblRadiusError.setText("Saisissez un nombre positif valide.");
            lblRadiusError.setVisible(true);
            lblRadiusError.setManaged(true);
            isValid = false;
        }

        if (!isValid)
            return;

        try {
            if (isEditMode && editingAlert != null) {
                // UPDATE mode — update existing alert
                editingAlert.setTitle(title);
                editingAlert.setBloodTypeId(normalizedBloodType);
                editingAlert.setSeverity(severity);
                editingAlert.setQuantityNeeded(quantity);
                editingAlert.setTargetRadiusKm(radius);
                editingAlert.setMessage(message);

                service.modifier(editingAlert);

                if (parentController != null) {
                    parentController.refreshData();
                }

                closeDialog();
            } else {
                // CREATE mode — new alert + matching
                Alert alert = new Alert();
                alert.setAlertId(java.util.UUID.randomUUID().toString());
                RuntimeContextService.AlertContext context = runtimeContextService.resolveAlertContext();
                alert.setHospitalId(context.getHospitalId());
                alert.setStaffId(context.getStaffId());

                alert.setTitle(title);
                alert.setBloodTypeId(normalizedBloodType);
                alert.setSeverity(severity);
                alert.setQuantityNeeded(quantity);
                alert.setTargetRadiusKm(radius);
                alert.setMessage(message);
                alert.setResolved(false);

                service.ajouter(alert);

                if (parentController != null) {
                    parentController.refreshData();
                }

                // Afficher l'indicateur de traitement
                btnSubmit.setDisable(true);
                btnSubmit.setText("Traitement...");
                showProcessing(true);

                // Lancer la correspondance automatique
                EmergencyMatchingService matchingService = new EmergencyMatchingService();
                matchingService.triggerMatchingAsync(alert, result -> {
                    showProcessing(false);
                    showMatchingResult(result);

                    if (parentController != null) {
                        parentController.refreshData();
                    }
                });
            }

        } catch (Exception e) {
            lblStatusMessage.setText("Erreur: " + e.getMessage());
            lblStatusMessage.setStyle("-fx-text-fill: #ef4444;");
            lblStatusMessage.setVisible(true);
            lblStatusMessage.setManaged(true);
        }
    }

    private void showProcessing(boolean show) {
        if (matchingProcessingBox != null) {
            matchingProcessingBox.setVisible(show);
            matchingProcessingBox.setManaged(show);
        }
    }

    private void showMatchingResult(EmergencyMatchingService.MatchResult result) {
        if (matchingResultPanel == null)
            return;

        if (result.success) {
            lblMatchingTitle.setText("Correspondance terminee");
            lblMatchingDonorCount.setText(
                    result.matchedDonorCount + " donneur(s) compatibles trouves - notifications SMS envoyees.");
            String generationSource = "metier_ia";
            lblMatchingMessage.setText("Message genere (" + generationSource + "):\n\""
                    + (result.generatedMessage != null ? result.generatedMessage : "N/D") + "\"");

            matchingResultPanel.setStyle(
                    "-fx-background-color: #f0fdf4; -fx-border-color: #86efac; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16;");
            lblMatchingTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #166534;");
        } else {
            lblMatchingTitle.setText("Erreur de correspondance");
            lblMatchingDonorCount.setText("Le processus de correspondance a rencontre un probleme.");
            lblMatchingMessage
                    .setText("Erreur: " + (result.errorMessage != null ? result.errorMessage : "Erreur inconnue"));

            matchingResultPanel.setStyle(
                    "-fx-background-color: #fef2f2; -fx-border-color: #fca5a5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16;");
            lblMatchingTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #991b1b;");
        }

        matchingResultPanel.setVisible(true);
        matchingResultPanel.setManaged(true);

        btnSubmit.setText("Termine");
        btnCancel.setText("Fermer");
    }

    private void clearErrors() {
        lblTitleError.setVisible(false);
        lblTitleError.setManaged(false);
        lblBloodTypeError.setVisible(false);
        lblBloodTypeError.setManaged(false);
        lblSeverityError.setVisible(false);
        lblSeverityError.setManaged(false);
        lblQuantityError.setVisible(false);
        lblQuantityError.setManaged(false);
        lblRadiusError.setVisible(false);
        lblRadiusError.setManaged(false);
        lblMessageError.setVisible(false);
        lblMessageError.setManaged(false);
        lblStatusMessage.setVisible(false);
        lblStatusMessage.setManaged(false);
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}
