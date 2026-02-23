package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.services.HospitalServiceImpl;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class HospitalFormController implements Initializable {

    // ---- FXML Fields ----
    @FXML private VBox modalCard;
    @FXML private Label lblFormTitle;
    @FXML private Label lblFormSubtitle;
    @FXML private TextField tfName;
    @FXML private TextField tfAddress;
    @FXML private TextField tfCity;
    @FXML private TextField tfPhone;
    @FXML private TextField tfEmail;
    @FXML private TextField tfLatitude;
    @FXML private TextField tfLongitude;
    @FXML private CheckBox cbActive;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
    @FXML private Button btnClose;
    @FXML private Label lblStatusMessage;

    // Error labels
    @FXML private Label lblNameError;
    @FXML private Label lblAddressError;
    @FXML private Label lblCityError;
    @FXML private Label lblPhoneError;
    @FXML private Label lblEmailError;
    @FXML private Label lblLatitudeError;
    @FXML private Label lblLongitudeError;

    // ---- State ----
    private final HospitalServiceImpl hospitalService = new HospitalServiceImpl();
    private HospitalListController hospitalListController;
    private StackPane parentContainer;
    private Hospital editingHospital = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Default is CREATE mode — titles are already set in FXML
    }

    // ==================== PUBLIC SETUP METHODS ====================

    /**
     * Links back to the HospitalListController so we can refresh the list after submit.
     */
    public void setHospitalListController(HospitalListController controller) {
        this.hospitalListController = controller;
    }

    /**
     * Sets the parent StackPane so we can remove this modal overlay when closing.
     */
    public void setParentContainer(StackPane parentContainer) {
        this.parentContainer = parentContainer;
    }

    /**
     * Switches the form to EDIT mode and pre-fills all fields with the hospital's data.
     */
    public void setEditMode(Hospital hospital) {
        this.isEditMode = true;
        this.editingHospital = hospital;

        lblFormTitle.setText("Edit Hospital");
        lblFormSubtitle.setText("Update the hospital's information in the BloodLink network.");
        btnSubmit.setText("Save Changes");

        // Pre-fill form fields
        tfName.setText(hospital.getName() != null ? hospital.getName() : "");
        tfAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : "");
        tfCity.setText(hospital.getCity() != null ? hospital.getCity() : "");
        tfPhone.setText(hospital.getPhone() != null ? hospital.getPhone() : "");
        tfEmail.setText(hospital.getEmail() != null ? hospital.getEmail() : "");
        tfLatitude.setText(hospital.getLatitude() != null ? hospital.getLatitude().toPlainString() : "");
        tfLongitude.setText(hospital.getLongitude() != null ? hospital.getLongitude().toPlainString() : "");
        cbActive.setSelected(hospital.isActive());
    }

    // ==================== ACTIONS ====================

    /**
     * Handles the submit button click — validates input and either creates or updates.
     */
    @FXML
    private void onSubmit() {
        clearAllErrors();

        if (!validateForm()) {
            return;
        }

        try {
            if (isEditMode) {
                updateHospital();
            } else {
                createHospital();
            }

            // Refresh the hospital list and close the modal
            if (hospitalListController != null) {
                hospitalListController.refreshData();
            }
            closeModal();

        } catch (Exception e) {
            showStatusMessage("Error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Closes the modal overlay.
     */
    @FXML
    private void onCancel() {
        closeModal();
    }

    // ==================== CRUD OPERATIONS ====================

    private void createHospital() {
        Hospital hospital = buildHospitalFromForm();
        hospitalService.ajouter(hospital);
    }

    private void updateHospital() {
        // Keep the same ID and timestamps, update everything else from the form
        editingHospital.setName(tfName.getText().trim());
        editingHospital.setAddress(tfAddress.getText().trim());
        editingHospital.setCity(tfCity.getText().trim());
        editingHospital.setPhone(tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim());
        editingHospital.setEmail(tfEmail.getText().trim().isEmpty() ? null : tfEmail.getText().trim());
        editingHospital.setActive(cbActive.isSelected());

        String latText = tfLatitude.getText().trim();
        String lonText = tfLongitude.getText().trim();
        editingHospital.setLatitude(latText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(latText));
        editingHospital.setLongitude(lonText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(lonText));

        hospitalService.modifier(editingHospital);
    }

    /**
     * Builds a new Hospital entity from the current form field values.
     */
    private Hospital buildHospitalFromForm() {
        Hospital hospital = new Hospital();
        hospital.setName(tfName.getText().trim());
        hospital.setAddress(tfAddress.getText().trim());
        hospital.setCity(tfCity.getText().trim());
        hospital.setPhone(tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim());
        hospital.setEmail(tfEmail.getText().trim().isEmpty() ? null : tfEmail.getText().trim());
        hospital.setActive(cbActive.isSelected());

        String latText = tfLatitude.getText().trim();
        String lonText = tfLongitude.getText().trim();
        hospital.setLatitude(latText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(latText));
        hospital.setLongitude(lonText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(lonText));

        return hospital;
    }

    // ==================== VALIDATION ====================

    /**
     * Validates all form fields. Returns true if valid, false otherwise.
     * Shows inline error messages next to invalid fields.
     */
    private boolean validateForm() {
        boolean valid = true;

        // Name — required
        if (tfName.getText() == null || tfName.getText().trim().isEmpty()) {
            showFieldError(tfName, lblNameError, "Hospital name is required.");
            valid = false;
        } else if (tfName.getText().trim().length() < 2) {
            showFieldError(tfName, lblNameError, "Name must be at least 2 characters.");
            valid = false;
        }

        // Address — required
        if (tfAddress.getText() == null || tfAddress.getText().trim().isEmpty()) {
            showFieldError(tfAddress, lblAddressError, "Address is required.");
            valid = false;
        }

        // City — required
        if (tfCity.getText() == null || tfCity.getText().trim().isEmpty()) {
            showFieldError(tfCity, lblCityError, "City is required.");
            valid = false;
        }

        // Phone — optional, but if provided must look reasonable
        String phone = tfPhone.getText() != null ? tfPhone.getText().trim() : "";
        if (!phone.isEmpty() && !phone.matches("^[+]?[0-9\\s\\-().]{6,20}$")) {
            showFieldError(tfPhone, lblPhoneError, "Please enter a valid phone number.");
            valid = false;
        }

        // Email — optional, but if provided must be a valid format
        String email = tfEmail.getText() != null ? tfEmail.getText().trim() : "";
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            showFieldError(tfEmail, lblEmailError, "Please enter a valid email address.");
            valid = false;
        }

        // Latitude — optional, but if provided must be a valid decimal in range
        String latText = tfLatitude.getText() != null ? tfLatitude.getText().trim() : "";
        if (!latText.isEmpty()) {
            try {
                double lat = Double.parseDouble(latText);
                if (lat < -90 || lat > 90) {
                    showFieldError(tfLatitude, lblLatitudeError, "Latitude must be between -90 and 90.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                showFieldError(tfLatitude, lblLatitudeError, "Please enter a valid number.");
                valid = false;
            }
        }

        // Longitude — optional, but if provided must be a valid decimal in range
        String lonText = tfLongitude.getText() != null ? tfLongitude.getText().trim() : "";
        if (!lonText.isEmpty()) {
            try {
                double lon = Double.parseDouble(lonText);
                if (lon < -180 || lon > 180) {
                    showFieldError(tfLongitude, lblLongitudeError, "Longitude must be between -180 and 180.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                showFieldError(tfLongitude, lblLongitudeError, "Please enter a valid number.");
                valid = false;
            }
        }

        return valid;
    }

    // ==================== UI HELPERS ====================

    /**
     * Shows an inline error message below a field and applies the error style class.
     */
    private void showFieldError(TextField field, Label errorLabel, String message) {
        if (field != null) {
            field.getStyleClass().add("form-input-error");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /**
     * Clears all field error styles and hides error labels.
     */
    private void clearAllErrors() {
        clearFieldError(tfName, lblNameError);
        clearFieldError(tfAddress, lblAddressError);
        clearFieldError(tfCity, lblCityError);
        clearFieldError(tfPhone, lblPhoneError);
        clearFieldError(tfEmail, lblEmailError);
        clearFieldError(tfLatitude, lblLatitudeError);
        clearFieldError(tfLongitude, lblLongitudeError);

        lblStatusMessage.setVisible(false);
        lblStatusMessage.setManaged(false);
    }

    /**
     * Clears the error style and hides the error label for a single field.
     */
    private void clearFieldError(TextField field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("form-input-error");
        }
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Shows a status message at the bottom of the form (success or error).
     */
    private void showStatusMessage(String message, boolean isError) {
        lblStatusMessage.setText(message);
        lblStatusMessage.getStyleClass().removeAll("form-status-success", "form-status-error");
        lblStatusMessage.getStyleClass().add(isError ? "form-status-error" : "form-status-success");
        lblStatusMessage.setVisible(true);
        lblStatusMessage.setManaged(true);
    }

    /**
     * Removes this modal overlay from the parent StackPane.
     */
    private void closeModal() {
        if (parentContainer != null) {
            // The modal overlay is the last child added to the StackPane
            if (!parentContainer.getChildren().isEmpty()) {
                parentContainer.getChildren().remove(parentContainer.getChildren().size() - 1);
            }
        }
    }
}
