package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.services.DonorEligibilityService;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DonorEligibilityDialogController {

    public enum Mode {
        ADD, EDIT
    }

    @FXML private Text dialogTitle;
    @FXML private TextField donorIdField;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private ComboBox<String> eligibilityStatusCombo;
    @FXML private TextField daysUntilEligibleField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Button saveButton;

    private DonorEligibilityService eligibilityService;
    private Stage dialogStage;
    private DonorEligibility recordToEdit;
    private Mode mode;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        eligibilityService = new DonorEligibilityService();

        // Initialize combos
        bloodTypeCombo.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        eligibilityStatusCombo.getItems().addAll("Eligible", "Not Eligible");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.EDIT) {
            dialogTitle.setText("Edit Donor Eligibility");
            saveButton.setText("Save Changes");
            donorIdField.setDisable(true); // Can't change donor ID
        }
    }

    public void setRecord(DonorEligibility record) {
        this.recordToEdit = record;
        if (record != null) {
            donorIdField.setText(record.getId());
            bloodTypeCombo.setValue(record.getBloodTypeCache());
            eligibilityStatusCombo.setValue(record.getIsCurrentlyEligible() != null && record.getIsCurrentlyEligible() ? "Eligible" : "Not Eligible");
            daysUntilEligibleField.setText(record.getDaysUntilEligible() != null ? String.valueOf(record.getDaysUntilEligible()) : "0");
            latitudeField.setText(record.getLatitudeCache() != null ? record.getLatitudeCache().toString() : "");
            longitudeField.setText(record.getLongitudeCache() != null ? record.getLongitudeCache().toString() : "");
        }
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            String donorId = donorIdField.getText().trim();
            String bloodType = bloodTypeCombo.getValue();
            boolean isEligible = eligibilityStatusCombo.getValue().equals("Eligible");
            int daysUntilEligible = Integer.parseInt(daysUntilEligibleField.getText().trim());

            BigDecimal latitude = null;
            BigDecimal longitude = null;

            if (!latitudeField.getText().trim().isEmpty()) {
                latitude = new BigDecimal(latitudeField.getText().trim());
            }
            if (!longitudeField.getText().trim().isEmpty()) {
                longitude = new BigDecimal(longitudeField.getText().trim());
            }

            if (mode == Mode.ADD) {
                DonorEligibility newRecord = new DonorEligibility();
                newRecord.setId(donorId);
                newRecord.setBloodTypeCache(bloodType);
                newRecord.setIsCurrentlyEligible(isEligible);
                newRecord.setDaysUntilEligible(daysUntilEligible);
                newRecord.setLatitudeCache(latitude);
                newRecord.setLongitudeCache(longitude);
                newRecord.setLastCalculatedAt(LocalDate.now());

                eligibilityService.ajouter(newRecord);

                // Verify
                DonorEligibility saved = eligibilityService.getOne(donorId);
                if (saved == null) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Record was not found after save. Check database constraints.");
                    return;
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Eligibility record added successfully!");
            } else {
                recordToEdit.setBloodTypeCache(bloodType);
                recordToEdit.setIsCurrentlyEligible(isEligible);
                recordToEdit.setDaysUntilEligible(daysUntilEligible);
                recordToEdit.setLatitudeCache(latitude);
                recordToEdit.setLongitudeCache(longitude);

                eligibilityService.modifier(recordToEdit);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Eligibility record updated successfully!");
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            dialogStage.close();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter valid numbers for days and coordinates.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save record: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (donorIdField.getText().trim().isEmpty()) {
            errors.append("- Donor ID is required\n");
        }

        if (bloodTypeCombo.getValue() == null) {
            errors.append("- Blood type is required\n");
        }

        if (eligibilityStatusCombo.getValue() == null) {
            errors.append("- Eligibility status is required\n");
        }

        if (daysUntilEligibleField.getText().trim().isEmpty()) {
            errors.append("- Days until eligible is required\n");
        } else {
            try {
                Integer.parseInt(daysUntilEligibleField.getText().trim());
            } catch (NumberFormatException e) {
                errors.append("- Days until eligible must be a valid number\n");
            }
        }

        if (!latitudeField.getText().trim().isEmpty()) {
            try {
                new BigDecimal(latitudeField.getText().trim());
            } catch (NumberFormatException e) {
                errors.append("- Latitude must be a valid decimal number\n");
            }
        }

        if (!longitudeField.getText().trim().isEmpty()) {
            try {
                new BigDecimal(longitudeField.getText().trim());
            } catch (NumberFormatException e) {
                errors.append("- Longitude must be a valid decimal number\n");
            }
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                     "Please fix the following errors:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
