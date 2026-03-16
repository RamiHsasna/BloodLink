package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.edu.esprit.entities.BloodType;
import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.services.BloodTypeServiceImpl;
import tn.edu.esprit.services.DonorEligibilityService;
import tn.edu.esprit.services.ServiceDonor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonorEligibilityDialogController {

    public enum Mode {
        ADD, EDIT
    }

    @FXML private Text dialogTitle;
    @FXML private ComboBox<String> donorIdCombo;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private ComboBox<String> eligibilityStatusCombo;
    @FXML private TextField daysUntilEligibleField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Button saveButton;

    private DonorEligibilityService eligibilityService;
    private ServiceDonor donorService;
    private List<BloodType> bloodTypes;
    private final Map<String, Donor> donorDisplayToDonor = new HashMap<>();
    private final Map<String, String> bloodTypeIdToDisplay = new HashMap<>();
    private Stage dialogStage;
    private DonorEligibility recordToEdit;
    private Mode mode;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        eligibilityService = new DonorEligibilityService();
        donorService = new ServiceDonor();

        // Initialize combos
        loadBloodTypes();
        loadDonorChoices();
        eligibilityStatusCombo.getItems().addAll("Eligible", "Not Eligible");

        donorIdCombo.valueProperty().addListener((obs, oldVal, newVal) -> populateDonorDependentFields(newVal));
        eligibilityStatusCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateDaysFieldState(newVal));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.EDIT) {
            dialogTitle.setText("Edit Donor Eligibility");
            saveButton.setText("Save Changes");
            donorIdCombo.setDisable(true); // Can't change donor ID
        }
    }

    public void setRecord(DonorEligibility record) {
        this.recordToEdit = record;
        if (record != null) {
            String donorDisplay = findDonorDisplay(record.getId());
            donorIdCombo.setValue(donorDisplay != null ? donorDisplay : record.getId());
            bloodTypeCombo.setValue(normalizeBloodTypeValue(record.getBloodTypeCache()));
            eligibilityStatusCombo.setValue(record.getIsCurrentlyEligible() != null && record.getIsCurrentlyEligible() ? "Eligible" : "Not Eligible");
            daysUntilEligibleField.setText(record.getDaysUntilEligible() != null ? String.valueOf(record.getDaysUntilEligible()) : "0");
            latitudeField.setText(record.getLatitudeCache() != null ? record.getLatitudeCache().toString() : "");
            longitudeField.setText(record.getLongitudeCache() != null ? record.getLongitudeCache().toString() : "");
            updateDaysFieldState(eligibilityStatusCombo.getValue());
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
            Donor selectedDonor = donorDisplayToDonor.get(donorIdCombo.getValue());
            String donorId = selectedDonor != null ? selectedDonor.getUserId() : donorIdCombo.getValue();
            String bloodType = normalizeBloodTypeValue(bloodTypeCombo.getValue());
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

        Donor selectedDonor = donorDisplayToDonor.get(donorIdCombo.getValue());

        if (donorIdCombo.getValue() == null || selectedDonor == null) {
            errors.append("- Donor ID is required\n");
        }

        if (bloodTypeCombo.getValue() == null) {
            errors.append("- Blood type is required\n");
        } else {
            String normalizedBloodType = normalizeBloodTypeValue(bloodTypeCombo.getValue());
            if (normalizedBloodType == null || !normalizedBloodType.matches("^(A|B|AB|O)[+-]$")) {
                errors.append("- Blood type format is invalid\n");
            }
        }

        if (eligibilityStatusCombo.getValue() == null) {
            errors.append("- Eligibility status is required\n");
        }

        if (daysUntilEligibleField.getText().trim().isEmpty()) {
            errors.append("- Days until eligible is required\n");
        } else {
            try {
                int days = Integer.parseInt(daysUntilEligibleField.getText().trim());
                if (days < 0) {
                    errors.append("- Days until eligible cannot be negative\n");
                }
                if (eligibilityStatusCombo.getValue() != null && eligibilityStatusCombo.getValue().equals("Eligible") && days != 0) {
                    errors.append("- Eligible donors must have 0 days until eligible\n");
                }
                if (eligibilityStatusCombo.getValue() != null && eligibilityStatusCombo.getValue().equals("Not Eligible") && days <= 0) {
                    errors.append("- Not eligible donors must have days until eligible greater than 0\n");
                }
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

        if (selectedDonor != null) {
            String selectedBloodType = normalizeBloodTypeValue(bloodTypeCombo.getValue());
            String donorBloodType = getDonorBloodTypeDisplay(selectedDonor);
            if (selectedBloodType != null && donorBloodType != null && !selectedBloodType.equals(donorBloodType)) {
                errors.append("- Selected blood type does not match the chosen donor\n");
            }

            if (!latitudeField.getText().trim().isEmpty() && selectedDonor.getLatitude() != null) {
                BigDecimal donorLatitude = BigDecimal.valueOf(selectedDonor.getLatitude());
                BigDecimal formLatitude = new BigDecimal(latitudeField.getText().trim());
                if (formLatitude.compareTo(donorLatitude) != 0) {
                    errors.append("- Latitude does not match the chosen donor\n");
                }
            }

            if (!longitudeField.getText().trim().isEmpty() && selectedDonor.getLongitude() != null) {
                BigDecimal donorLongitude = BigDecimal.valueOf(selectedDonor.getLongitude());
                BigDecimal formLongitude = new BigDecimal(longitudeField.getText().trim());
                if (formLongitude.compareTo(donorLongitude) != 0) {
                    errors.append("- Longitude does not match the chosen donor\n");
                }
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

    private void loadBloodTypes() {
        BloodTypeServiceImpl bloodTypeService = new BloodTypeServiceImpl();
        bloodTypes = bloodTypeService.getAllBloodTypes();
        bloodTypeCombo.getItems().clear();
        bloodTypeIdToDisplay.clear();

        for (BloodType bloodType : bloodTypes) {
            String display = normalizeBloodTypeDisplay(bloodType);
            if (display == null) {
                continue;
            }
            bloodTypeCombo.getItems().add(display);
            bloodTypeIdToDisplay.put(bloodType.getBloodTypeId(), display);
        }
    }

    private void loadDonorChoices() {
        donorIdCombo.getItems().clear();
        donorDisplayToDonor.clear();

        List<Donor> donors = donorService.getAll(null);
        for (Donor donor : donors) {
            String firstName = donor.getFirstName() != null ? donor.getFirstName() : "";
            String lastName = donor.getLastName() != null ? donor.getLastName() : "";
            String displayName = (firstName + " " + lastName).trim();
            String display = donor.getUserId();
            if (!displayName.isEmpty()) {
                display += " - " + displayName;
            }
            donorIdCombo.getItems().add(display);
            donorDisplayToDonor.put(display, donor);
        }
    }

    private void populateDonorDependentFields(String donorDisplay) {
        Donor donor = donorDisplayToDonor.get(donorDisplay);
        if (donor == null) {
            return;
        }

        String donorBloodType = getDonorBloodTypeDisplay(donor);
        if (donorBloodType != null) {
            bloodTypeCombo.setValue(donorBloodType);
        }

        latitudeField.setText(donor.getLatitude() != null ? String.valueOf(donor.getLatitude()) : "");
        longitudeField.setText(donor.getLongitude() != null ? String.valueOf(donor.getLongitude()) : "");
        eligibilityStatusCombo.setValue(donor.isCurrentlyEligible() ? "Eligible" : "Not Eligible");
        if (donor.isCurrentlyEligible()) {
            daysUntilEligibleField.setText("0");
        } else if (daysUntilEligibleField.getText() == null || daysUntilEligibleField.getText().trim().isEmpty() || "0".equals(daysUntilEligibleField.getText().trim())) {
            daysUntilEligibleField.setText("1");
        }
    }

    private String getDonorBloodTypeDisplay(Donor donor) {
        if (donor == null || donor.getBloodTypeId() == null) {
            return null;
        }
        return bloodTypeIdToDisplay.get(donor.getBloodTypeId());
    }

    private String findDonorDisplay(String donorId) {
        for (Map.Entry<String, Donor> entry : donorDisplayToDonor.entrySet()) {
            if (entry.getValue() != null && donorId.equals(entry.getValue().getUserId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void updateDaysFieldState(String eligibilityStatus) {
        boolean isEligible = "Eligible".equals(eligibilityStatus);
        daysUntilEligibleField.setDisable(isEligible);
        if (isEligible) {
            daysUntilEligibleField.setText("0");
        } else if (daysUntilEligibleField.getText() == null || daysUntilEligibleField.getText().trim().isEmpty() || "0".equals(daysUntilEligibleField.getText().trim())) {
            daysUntilEligibleField.setText("1");
        }
    }

    private String normalizeBloodTypeDisplay(BloodType bloodType) {
        if (bloodType == null) {
            return null;
        }

        String abo = bloodType.getAboType() != null ? bloodType.getAboType().trim().toUpperCase() : "";
        String rh = normalizeRhFactor(bloodType.getRhFactor());
        if (abo.isEmpty() || rh == null) {
            return null;
        }
        return abo + rh;
    }

    private String normalizeBloodTypeValue(String bloodTypeValue) {
        if (bloodTypeValue == null) {
            return null;
        }

        String normalized = bloodTypeValue.trim().toUpperCase().replace(" ", "");
        normalized = normalized.replace("POSITIVE", "+");
        normalized = normalized.replace("NEGATIVE", "-");
        normalized = normalized.replace("POS", "+");
        normalized = normalized.replace("NEG", "-");

        if (normalized.matches("^(A|B|AB|O)[+-]$")) {
            return normalized;
        }
        return normalized;
    }

    private String normalizeRhFactor(String rhFactor) {
        if (rhFactor == null) {
            return null;
        }

        String normalizedRh = rhFactor.trim().toUpperCase();
        if (normalizedRh.equals("+") || normalizedRh.equals("POS") || normalizedRh.equals("POSITIVE")) {
            return "+";
        }
        if (normalizedRh.equals("-") || normalizedRh.equals("NEG") || normalizedRh.equals("NEGATIVE")) {
            return "-";
        }
        return null;
    }
}
