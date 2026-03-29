package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.entities.DonationsEvent;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.ServiceDonation;
import tn.edu.esprit.services.SessionScopeService;
import tn.edu.esprit.services.ServiceUser;
import tn.edu.esprit.services.ServiceDonationsEvent;
import tn.edu.esprit.services.HospitalServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class DonationsDialogController {

    public enum Mode {
        ADD, EDIT
    }

    @FXML private Text dialogTitle;
    @FXML private ComboBox<String> donorIdCombo;
    @FXML private ComboBox<String> hospitalIdCombo;
    @FXML private ComboBox<String> donationEventIdCombo;
    @FXML private TextField bloodTypeIdField;
    @FXML private DatePicker donationDatePicker;
    @FXML private TextField unitsCollectedField;
    @FXML private TextField volumeCollectedField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> screeningCombo;
    @FXML private TextArea medicalNotesField;
    @FXML private Button saveButton;

    private ServiceDonation serviceDonation;
    private ServiceUser serviceUser;
    private HospitalServiceImpl hospitalService;
    private ServiceDonationsEvent serviceDonationsEvent;
    private Stage dialogStage;
    private Donations donationToEdit;
    private Mode mode;
    private Runnable onSaveCallback;
    private List<Users> donorList;
    private SessionScopeService sessionScopeService;

    @FXML
    public void initialize() {
        serviceDonation = new ServiceDonation();
        serviceUser = new ServiceUser();
        hospitalService = new HospitalServiceImpl();
        serviceDonationsEvent = new ServiceDonationsEvent();
        sessionScopeService = new SessionScopeService();

        // Initialize status combo
        statusCombo.getItems().addAll("COMPLETED", "CANCELLED");

        // Initialize screening combo
        screeningCombo.getItems().addAll("Passed", "Failed", "Pending");

        // Load data
        loadDonors();
        loadHospitals();
        loadDonationEvents();
        applyRoleScopedDefaults();
    }

    private void loadDonors() {
        try {
            donorList = serviceUser.getAll(null);
            if (donorList != null) {
                // Filter only donors
                donorList.removeIf(user -> user.getUserType() != UserType.DONOR);

                // Populate combo box with donor names and IDs
                for (Users donor : donorList) {
                    donorIdCombo.getItems().add(donor.getId() + " - " + donor.getFirst_name() + " " + donor.getLast_name());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadHospitals() {
        try {
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            if (hospitals != null) {
                for (Hospital hospital : hospitals) {
                    String displayText = hospital.getHospitalId() + " - " + hospital.getName();
                    hospitalIdCombo.getItems().add(displayText);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDonationEvents() {
        try {
            DonationsEvent dummy = new DonationsEvent();
            List<DonationsEvent> events = serviceDonationsEvent.getAll(dummy);
            if (events != null) {
                if (sessionScopeService.isHospitalStaff() && sessionScopeService.hasHospitalScope()) {
                    events.removeIf(event -> event.getHospitalId() == null
                            || !event.getHospitalId().equalsIgnoreCase(sessionScopeService.getCurrentHospitalId()));
                }
                for (DonationsEvent event : events) {
                    String displayText = event.getEventId() + " - " + event.getName();
                    donationEventIdCombo.getItems().add(displayText);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.EDIT) {
            dialogTitle.setText("Edit Donation");
            saveButton.setText("Save Changes");
        }
    }

    public void setDonation(Donations donation) {
        this.donationToEdit = donation;
        if (donation != null) {
            // Find and select the donor in the combo box
            String donorId = donation.getDonorId();
            for (String item : donorIdCombo.getItems()) {
                if (item.startsWith(donorId)) {
                    donorIdCombo.setValue(item);
                    break;
                }
            }

            // Set hospital combo box
            String hospitalId = donation.getHospitalId();
            for (String item : hospitalIdCombo.getItems()) {
                if (item.startsWith(hospitalId)) {
                    hospitalIdCombo.setValue(item);
                    break;
                }
            }

            // Set donation event combo box
            String eventId = donation.getDonationEventId();
            for (String item : donationEventIdCombo.getItems()) {
                if (item.startsWith(eventId)) {
                    donationEventIdCombo.setValue(item);
                    break;
                }
            }
            bloodTypeIdField.setText(donation.getBloodTypeId());
            donationDatePicker.setValue(donation.getDonationDate().toLocalDate());
            unitsCollectedField.setText(String.valueOf(donation.getUnitsCollected()));
            if (donation.getVolumeCollected() != null) {
                volumeCollectedField.setText(donation.getVolumeCollected().toString());
            }
            statusCombo.setValue(donation.getStatus());

            String screeningStatus = donation.getScreeningPassed() != null
                ? (donation.getScreeningPassed() ? "Passed" : "Failed")
                : "Pending";
            screeningCombo.setValue(screeningStatus);

            if (donation.getMedicalNotes() != null) {
                medicalNotesField.setText(donation.getMedicalNotes());
            }
        }
        applyRoleScopedDefaults();
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
            // Extract donor ID from combo box selection (format: "ID - Name")
            String selectedDonor = donorIdCombo.getValue();
            String donorId = selectedDonor.split(" - ")[0].trim();
            String selectedHospital = hospitalIdCombo.getValue();
            String hospitalId = selectedHospital.split(" - ")[0].trim();
            String selectedEvent = donationEventIdCombo.getValue();
            String donationEventId = selectedEvent != null ? selectedEvent.split(" - ")[0].trim() : null;
            String bloodTypeId = bloodTypeIdField.getText().trim();
            LocalDateTime donationDate = donationDatePicker.getValue().atTime(LocalTime.now());
            int unitsCollected = Integer.parseInt(unitsCollectedField.getText().trim());
            BigDecimal volumeCollected = volumeCollectedField.getText().trim().isEmpty()
                ? null
                : new BigDecimal(volumeCollectedField.getText().trim());
            String status = statusCombo.getValue();
            Boolean screeningPassed = screeningCombo.getValue() != null && screeningCombo.getValue().equals("Passed")
                ? true
                : (screeningCombo.getValue() != null && screeningCombo.getValue().equals("Failed") ? false : false);
            String medicalNotes = medicalNotesField.getText().trim().isEmpty()
                ? null
                : medicalNotesField.getText().trim();

            if (mode == Mode.ADD) {
                Donations newDonation = new Donations(
                    UUID.randomUUID().toString(),
                    donorId,
                    hospitalId,
                    donationEventId,
                    bloodTypeId,
                    donationDate,
                    unitsCollected,
                    volumeCollected,
                    status,
                    screeningPassed,
                    medicalNotes,
                    LocalDateTime.now()
                );

                serviceDonation.ajouter(newDonation);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Donation added successfully!");
            } else {
                donationToEdit.setDonorId(donorId);
                donationToEdit.setHospitalId(hospitalId);
                donationToEdit.setDonationEventId(donationEventId);
                donationToEdit.setBloodTypeId(bloodTypeId);
                donationToEdit.setDonationDate(donationDate);
                donationToEdit.setUnitsCollected(unitsCollected);
                donationToEdit.setVolumeCollected(volumeCollected);
                donationToEdit.setStatus(status);
                donationToEdit.setScreeningPassed(screeningPassed);
                donationToEdit.setMedicalNotes(medicalNotes);

                serviceDonation.modifier(donationToEdit);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Donation updated successfully!");
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            dialogStage.close();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                     "Units collected and volume must be valid numbers");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save donation: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void applyRoleScopedDefaults() {
        if (!sessionScopeService.isHospitalStaff() || !sessionScopeService.hasHospitalScope()) {
            return;
        }

        String currentHospitalId = sessionScopeService.getCurrentHospitalId();
        for (String item : hospitalIdCombo.getItems()) {
            if (item.startsWith(currentHospitalId)) {
                hospitalIdCombo.setValue(item);
                break;
            }
        }
        hospitalIdCombo.setDisable(true);
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (donorIdCombo.getValue() == null || donorIdCombo.getValue().isEmpty()) {
            errors.append("- Donor is required\n");
        }

        if (hospitalIdCombo.getValue() == null || hospitalIdCombo.getValue().isEmpty()) {
            errors.append("- Hospital is required\n");
        }

        if (donationEventIdCombo.getValue() == null || donationEventIdCombo.getValue().isEmpty()) {
            errors.append("- Donation Event is required\n");
        }

        if (bloodTypeIdField.getText().trim().isEmpty()) {
            errors.append("- Blood Type ID is required\n");
        }

        if (donationDatePicker.getValue() == null) {
            errors.append("- Donation Date is required\n");
        }

        if (unitsCollectedField.getText().trim().isEmpty()) {
            errors.append("- Units Collected is required\n");
        } else {
            try {
                int units = Integer.parseInt(unitsCollectedField.getText().trim());
                if (units <= 0) {
                    errors.append("- Units Collected must be greater than 0\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- Units Collected must be a valid number\n");
            }
        }

        if (!volumeCollectedField.getText().trim().isEmpty()) {
            try {
                new BigDecimal(volumeCollectedField.getText().trim());
            } catch (NumberFormatException e) {
                errors.append("- Volume Collected must be a valid decimal number\n");
            }
        }

        if (statusCombo.getValue() == null) {
            errors.append("- Status is required\n");
        }

        if (screeningCombo.getValue() == null) {
            errors.append("- Screening result is required\n");
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
