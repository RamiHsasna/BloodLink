package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.edu.esprit.entities.BloodType;
import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.entities.HospitalStaff;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.BloodTypeServiceImpl;
import tn.edu.esprit.services.HospitalServiceImpl;
import tn.edu.esprit.services.ServiceDonor;
import tn.edu.esprit.services.ServiceHospitalStaff;
import tn.edu.esprit.services.ServiceUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserDialogController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    public enum Mode {
        ADD, EDIT
    }

    @FXML private Text dialogTitle;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private VBox passwordBox;
    @FXML private ComboBox<String> userTypeCombo;
    @FXML private Button saveButton;

    // Inline error labels
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordHint;
    @FXML private Label userTypeError;

    // Donor-specific fields
    @FXML private VBox donorFieldsBox;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private Label bloodTypeError;
    @FXML private DatePicker lastDonationDatePicker;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Spinner<Integer> totalDonationsSpinner;

    // Hospital staff-specific fields
    @FXML private VBox hospitalStaffFieldsBox;
    @FXML private ComboBox<String> staffRoleCombo;
    @FXML private ComboBox<String> hospitalCombo;
    @FXML private TextField departmentField;
    @FXML private Label staffRoleError;
    @FXML private Label hospitalError;

    private ServiceUser serviceUser;
    private ServiceDonor serviceDonor;
    private ServiceHospitalStaff serviceHospitalStaff;
    private List<BloodType> bloodTypes;
    private final Map<String, String> hospitalDisplayToId = new HashMap<>();
    private Stage dialogStage;
    private Users userToEdit;
    private Mode mode;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        serviceUser = new ServiceUser();
        serviceDonor = new ServiceDonor();
        serviceHospitalStaff = new ServiceHospitalStaff();

        // Initialize user type combo
        userTypeCombo.getItems().addAll("Donor", "Hospital Staff");

        // Load blood types for donor combo
        BloodTypeServiceImpl bloodTypeService = new BloodTypeServiceImpl();
        bloodTypes = bloodTypeService.getAllBloodTypes();
        for (BloodType bt : bloodTypes) {
            bloodTypeCombo.getItems().add(bt.getAboType() + bt.getRhFactor());
        }

        staffRoleCombo.getItems().addAll("MANAGER", "TECHNICIAN");
        loadHospitalOptions();

        // Total donations spinner (0–999)
        totalDonationsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 0));
        totalDonationsSpinner.setEditable(true);

        // --- Real-time validation listeners ---
        addRequiredFieldListener(firstNameField, firstNameError, "First name is required");
        addRequiredFieldListener(lastNameField, lastNameError, "Last name is required");
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            String val = newVal != null ? newVal.trim() : "";
            if (val.isEmpty()) {
                showFieldError(phoneField, phoneError, "Phone is required");
            } else if (!isValidPhone(val)) {
                showFieldError(phoneField, phoneError, "Invalid phone format");
            } else {
                clearFieldError(phoneField, phoneError);
            }
        });
        phoneField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String val = phoneField.getText() != null ? phoneField.getText().trim() : "";
                if (val.isEmpty()) {
                    showFieldError(phoneField, phoneError, "Phone is required");
                } else if (!isValidPhone(val)) {
                    showFieldError(phoneField, phoneError, "Invalid phone format");
                }
            }
        });

        // Email: required + format check
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            String val = newVal != null ? newVal.trim() : "";
            if (val.isEmpty()) {
                showFieldError(emailField, emailError, "Email is required");
            } else if (!isValidEmail(val)) {
                showFieldError(emailField, emailError, "Invalid email format");
            } else {
                clearFieldError(emailField, emailError);
            }
        });
        emailField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String val = emailField.getText() != null ? emailField.getText().trim() : "";
                if (val.isEmpty()) {
                    showFieldError(emailField, emailError, "Email is required");
                } else if (!isValidEmail(val)) {
                    showFieldError(emailField, emailError, "Invalid email format");
                }
            }
        });

        // Password: live length counter
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            if (len == 0) {
                passwordHint.setText("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
                passwordHint.getStyleClass().removeAll("password-hint-ok", "password-hint-warn");
                passwordHint.getStyleClass().add("password-hint-warn");
                passwordField.getStyleClass().remove("field-error");
            } else if (len < MIN_PASSWORD_LENGTH) {
                passwordHint.setText(len + " / " + MIN_PASSWORD_LENGTH + " characters — " + (MIN_PASSWORD_LENGTH - len) + " more needed");
                passwordHint.getStyleClass().removeAll("password-hint-ok", "password-hint-warn");
                passwordHint.getStyleClass().add("password-hint-warn");
                if (!passwordField.getStyleClass().contains("field-error")) {
                    passwordField.getStyleClass().add("field-error");
                }
            } else {
                passwordHint.setText("Password length is good (" + len + " characters)");
                passwordHint.getStyleClass().removeAll("password-hint-ok", "password-hint-warn");
                passwordHint.getStyleClass().add("password-hint-ok");
                passwordField.getStyleClass().remove("field-error");
            }
        });

        // User type combo — show/hide donor and hospital staff sections
        userTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clearFieldError(userTypeCombo, userTypeError);
            }
            boolean isDonor = "Donor".equals(newVal);
            boolean isHospitalStaff = "Hospital Staff".equals(newVal);
            donorFieldsBox.setVisible(isDonor);
            donorFieldsBox.setManaged(isDonor);
            hospitalStaffFieldsBox.setVisible(isHospitalStaff);
            hospitalStaffFieldsBox.setManaged(isHospitalStaff);
        });

        // Blood type combo validation
        bloodTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clearFieldError(bloodTypeCombo, bloodTypeError);
            }
        });

        staffRoleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clearFieldError(staffRoleCombo, staffRoleError);
            }
        });

        hospitalCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clearFieldError(hospitalCombo, hospitalError);
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.EDIT) {
            dialogTitle.setText("Edit User");
            saveButton.setText("Save Changes");
            // Hide password field when editing
            passwordBox.setVisible(false);
            passwordBox.setManaged(false);
        }
    }

    public void setUser(Users user) {
        this.userToEdit = user;
        if (user != null) {
            firstNameField.setText(user.getFirst_name());
            lastNameField.setText(user.getLast_name());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone());

            String userTypeDisplay = user.getUserType() == UserType.DONOR ? "Donor" : "Hospital Staff";
            userTypeCombo.setValue(userTypeDisplay);

            if (user.getUserType() == UserType.DONOR) {
                loadDonorData(user.getId());
            } else if (user.getUserType() == UserType.HOSPITAL_STAFF) {
                loadHospitalStaffData(user.getId());
            }
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
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String userTypeStr = userTypeCombo.getValue();
            UserType userType = userTypeStr.equals("Donor") ? UserType.DONOR : UserType.HOSPITAL_STAFF;

            if (mode == Mode.ADD) {
                String password = passwordField.getText();
                String userId = UUID.randomUUID().toString();

                Users newUser = new Users(
                    userId,
                    email,
                    password,
                    firstName,
                    lastName,
                    phone,
                    userType,
                    LocalDateTime.now()
                );

                serviceUser.ajouter(newUser);
                Users saved = serviceUser.getAll(null)
                        .stream()
                        .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                        .findFirst()
                        .orElse(null);
                if (saved == null) {
                    showAlert(Alert.AlertType.ERROR, "Failed","Could not save the user. The email address may already be in use. Please try again.");
                    return;
                }

                // If donor, update the auto-created donor row with form data
                if (userType == UserType.DONOR) {
                    updateDonorWithFormData(userId, firstName, lastName);
                } else {
                    if (!updateHospitalStaffWithFormData(userId)) {
                        return;
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Success", "User added successfully!");
            } else {
                userToEdit.setFirst_name(firstName);
                userToEdit.setLast_name(lastName);
                userToEdit.setEmail(email);
                userToEdit.setPhone(phone);
                userToEdit.setUserType(userType);

                serviceUser.modifier(userToEdit);
                Users updated = serviceUser.getOne(userToEdit);
                if (!isUserUpdated(updated, userToEdit)) {
                    showAlert(Alert.AlertType.ERROR, "Failed", "Could not update the user. The email address may already be in use. Please try again.");
                    return;
                }

                // If edited to donor type, update donor row with form data
                if (userType == UserType.DONOR && donorFieldsBox.isVisible()) {
                    updateDonorWithFormData(userToEdit.getId(), firstName, lastName);
                } else if (userType == UserType.HOSPITAL_STAFF && hospitalStaffFieldsBox.isVisible()) {
                    if (!updateHospitalStaffWithFormData(userToEdit.getId())) {
                        return;
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            dialogStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Something went wrong while saving the user. Please check your input and try again.");
        }
    }

    private void updateDonorWithFormData(String userId, String firstName, String lastName) {
        try {
            Donor existingDonor = new Donor();
            existingDonor.setUserId(userId);
            Donor currentDonor = serviceDonor.getOne(existingDonor);

            String selectedBloodTypeDisplay = bloodTypeCombo.getValue();
            String bloodTypeId = null;
            if (selectedBloodTypeDisplay != null) {
                for (BloodType bt : bloodTypes) {
                    if ((bt.getAboType() + bt.getRhFactor()).equals(selectedBloodTypeDisplay)) {
                        bloodTypeId = bt.getBloodTypeId();
                        break;
                    }
                }
            }

            LocalDate lastDonationDate = lastDonationDatePicker.getValue();

            Double latitude = null;
            if (latitudeField.getText() != null && !latitudeField.getText().trim().isEmpty()) {
                latitude = Double.parseDouble(latitudeField.getText().trim());
            }
            Double longitude = null;
            if (longitudeField.getText() != null && !longitudeField.getText().trim().isEmpty()) {
                longitude = Double.parseDouble(longitudeField.getText().trim());
            }

            int totalDonations = totalDonationsSpinner.getValue() != null ? totalDonationsSpinner.getValue() : 0;

            Donor donor = new Donor();
            donor.setUserId(userId);
            donor.setFirstName(firstName);
            donor.setLastName(lastName);
            donor.setBloodTypeId(bloodTypeId);
            donor.setLastDonationDate(lastDonationDate);
            donor.setCurrentlyEligible(currentDonor == null || currentDonor.isCurrentlyEligible());
            donor.setLatitude(latitude);
            donor.setLongitude(longitude);
            donor.setTotalDonations(totalDonations);

            serviceDonor.modifier(donor);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid latitude or longitude value. Donor location was not saved.");
        }
    }

    private void loadDonorData(String userId) {
        Donor probe = new Donor();
        probe.setUserId(userId);
        Donor donor = serviceDonor.getOne(probe);
        if (donor == null) {
            return;
        }

        if (donor.getBloodTypeId() != null) {
            for (BloodType bloodType : bloodTypes) {
                if (donor.getBloodTypeId().equals(bloodType.getBloodTypeId())) {
                    bloodTypeCombo.setValue(bloodType.getAboType() + bloodType.getRhFactor());
                    break;
                }
            }
        }

        lastDonationDatePicker.setValue(donor.getLastDonationDate());
        latitudeField.setText(donor.getLatitude() != null ? String.valueOf(donor.getLatitude()) : "");
        longitudeField.setText(donor.getLongitude() != null ? String.valueOf(donor.getLongitude()) : "");
        totalDonationsSpinner.getValueFactory().setValue(donor.getTotalDonations());
    }

    private boolean updateHospitalStaffWithFormData(String userId) {
        String selectedHospital = hospitalCombo.getValue();
        String hospitalId = hospitalDisplayToId.get(selectedHospital);

        HospitalStaff staff = new HospitalStaff();
        staff.setId(userId);
        staff.setRole(staffRoleCombo.getValue());
        staff.setHospitalId(hospitalId);
        staff.setFirstName(firstNameField.getText() != null ? firstNameField.getText().trim() : null);
        staff.setLastName(lastNameField.getText() != null ? lastNameField.getText().trim() : null);

        String department = departmentField.getText();
        if (department != null) {
            department = department.trim();
        }
        staff.setDepartment((department == null || department.isEmpty()) ? null : department);

        serviceHospitalStaff.modifier(staff);

        HospitalStaff savedStaff = serviceHospitalStaff.getOne(staff);
        if (!isHospitalStaffUpdated(savedStaff, staff)) {
            showAlert(Alert.AlertType.ERROR, "Failed", "Could not update the hospital staff profile. Please try again.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean validateInput() {
        boolean valid = true;

        if (firstNameField.getText().trim().isEmpty()) {
            showFieldError(firstNameField, firstNameError, "First name is required");
            valid = false;
        } else {
            clearFieldError(firstNameField, firstNameError);
        }

        if (lastNameField.getText().trim().isEmpty()) {
            showFieldError(lastNameField, lastNameError, "Last name is required");
            valid = false;
        } else {
            clearFieldError(lastNameField, lastNameError);
        }

        if (emailField.getText().trim().isEmpty()) {
            showFieldError(emailField, emailError, "Email is required");
            valid = false;
        } else if (!isValidEmail(emailField.getText().trim())) {
            showFieldError(emailField, emailError, "Invalid email format");
            valid = false;
        } else {
            clearFieldError(emailField, emailError);
        }

        if (phoneField.getText().trim().isEmpty()) {
            showFieldError(phoneField, phoneError, "Phone is required");
            valid = false;
        } else if (!isValidPhone(phoneField.getText().trim())) {
            showFieldError(phoneField, phoneError, "Invalid phone format");
            valid = false;
        } else {
            clearFieldError(phoneField, phoneError);
        }

        if (mode == Mode.ADD) {
            String pw = passwordField.getText();
            if (pw == null || pw.isEmpty()) {
                if (!passwordField.getStyleClass().contains("field-error")) {
                    passwordField.getStyleClass().add("field-error");
                }
                passwordHint.setText("Password is required");
                passwordHint.getStyleClass().removeAll("password-hint-ok", "password-hint-warn");
                passwordHint.getStyleClass().add("password-hint-warn");
                valid = false;
            } else if (pw.length() < MIN_PASSWORD_LENGTH) {
                valid = false;
            }
        }

        if (userTypeCombo.getValue() == null) {
            showFieldError(userTypeCombo, userTypeError, "User type is required");
            valid = false;
        } else {
            clearFieldError(userTypeCombo, userTypeError);
        }

        // Donor-specific validation
        if ("Donor".equals(userTypeCombo.getValue())) {
            if (bloodTypeCombo.getValue() == null) {
                showFieldError(bloodTypeCombo, bloodTypeError, "Blood type is required for donors");
                valid = false;
            } else {
                clearFieldError(bloodTypeCombo, bloodTypeError);
            }
        }

        // Hospital staff-specific validation
        if ("Hospital Staff".equals(userTypeCombo.getValue())) {
            if (staffRoleCombo.getValue() == null) {
                showFieldError(staffRoleCombo, staffRoleError, "Role is required for hospital staff");
                valid = false;
            } else {
                clearFieldError(staffRoleCombo, staffRoleError);
            }

            if (hospitalCombo.getValue() == null || hospitalDisplayToId.get(hospitalCombo.getValue()) == null) {
                showFieldError(hospitalCombo, hospitalError, "Hospital is required for hospital staff");
                valid = false;
            } else {
                clearFieldError(hospitalCombo, hospitalError);
            }
        }

        return valid;
    }

    private void loadHospitalOptions() {
        hospitalDisplayToId.clear();
        hospitalCombo.getItems().clear();

        HospitalServiceImpl hospitalService = new HospitalServiceImpl();
        List<Hospital> hospitals = hospitalService.getAllHospitals();
        for (Hospital hospital : hospitals) {
            if (hospital.getHospitalId() == null) {
                continue;
            }
            String id = hospital.getHospitalId().toString();
            String name = hospital.getName() != null ? hospital.getName() : "Unnamed Hospital";
            String display = name + " (" + id + ")";
            hospitalDisplayToId.put(display, id);
            hospitalCombo.getItems().add(display);
        }
    }

    private void loadHospitalStaffData(String userId) {
        HospitalStaff probe = new HospitalStaff();
        probe.setId(userId);
        HospitalStaff staff = serviceHospitalStaff.getOne(probe);
        if (staff == null) {
            return;
        }

        if (staff.getRole() != null) {
            if (!staffRoleCombo.getItems().contains(staff.getRole())) {
                staffRoleCombo.getItems().add(0, staff.getRole());
            }
            staffRoleCombo.setValue(staff.getRole());
        }

        if (staff.getDepartment() != null) {
            departmentField.setText(staff.getDepartment());
        }

        if (staff.getHospitalId() != null) {
            for (Map.Entry<String, String> entry : hospitalDisplayToId.entrySet()) {
                if (staff.getHospitalId().equals(entry.getValue())) {
                    hospitalCombo.setValue(entry.getKey());
                    break;
                }
            }
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("^\\d+$");
    }

    private boolean isUserUpdated(Users actual, Users expected) {
        if (actual == null || expected == null) {
            return false;
        }

        if (!safeEquals(actual.getFirst_name(), expected.getFirst_name())) {
            return false;
        }
        if (!safeEquals(actual.getLast_name(), expected.getLast_name())) {
            return false;
        }
        if (!safeEquals(actual.getEmail(), expected.getEmail())) {
            return false;
        }
        if (!safeEquals(actual.getPhone(), expected.getPhone())) {
            return false;
        }
        return actual.getUserType() == expected.getUserType();
    }

    private boolean safeEquals(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private boolean isHospitalStaffUpdated(HospitalStaff actual, HospitalStaff expected) {
        if (actual == null || expected == null) {
            return false;
        }

        if (!safeEquals(actual.getRole(), expected.getRole())) {
            return false;
        }
        if (!safeEquals(actual.getHospitalId(), expected.getHospitalId())) {
            return false;
        }
        if (!safeEquals(actual.getDepartment(), expected.getDepartment())) {
            return false;
        }
        if (!safeEquals(actual.getFirstName(), expected.getFirstName())) {
            return false;
        }
        return safeEquals(actual.getLastName(), expected.getLastName());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // --- Inline validation helpers ---

    private void addRequiredFieldListener(TextField field, Label errorLabel, String message) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                clearFieldError(field, errorLabel);
            }
        });
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && (field.getText() == null || field.getText().trim().isEmpty())) {
                showFieldError(field, errorLabel, message);
            }
        });
    }

    private void showFieldError(Control field, Label errorLabel, String message) {
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearFieldError(Control field, Label errorLabel) {
        field.getStyleClass().remove("field-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
