package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.ServiceUser;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDialogController {

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

    private ServiceUser serviceUser;
    private Stage dialogStage;
    private Users userToEdit;
    private Mode mode;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        serviceUser = new ServiceUser();

        // Initialize user type combo
        userTypeCombo.getItems().addAll("Donor", "Hospital Staff");
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

                Users newUser = new Users(
                    UUID.randomUUID().toString(),
                    email,
                    password, // In production, this should be hashed
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
                    showAlert(Alert.AlertType.ERROR, "Error", "User was not found after save. Check database constraints.");
                    return;
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
                    showAlert(Alert.AlertType.ERROR, "Error", "User update failed. Check database constraints.");
                    return;
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            dialogStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save user: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (firstNameField.getText().trim().isEmpty()) {
            errors.append("- First name is required\n");
        }

        if (lastNameField.getText().trim().isEmpty()) {
            errors.append("- Last name is required\n");
        }

        if (emailField.getText().trim().isEmpty()) {
            errors.append("- Email is required\n");
        } else if (!isValidEmail(emailField.getText().trim())) {
            errors.append("- Email format is invalid\n");
        }

        if (phoneField.getText().trim().isEmpty()) {
            errors.append("- Phone is required\n");
        }

        if (mode == Mode.ADD && passwordField.getText().isEmpty()) {
            errors.append("- Password is required\n");
        }

        if (userTypeCombo.getValue() == null) {
            errors.append("- User type is required\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                     "Please fix the following errors:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
