package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.edu.esprit.entities.DonationsEvent;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.services.ServiceDonationsEvent;
import tn.edu.esprit.services.HospitalServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

public class DonationEventDialogController {

    public enum Mode {
        ADD, EDIT
    }

    @FXML private ComboBox<String> hospitalIdCombo;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField locationField;
    @FXML private TextField targetBloodTypesField;
    @FXML private TextField targetCollectionUnitsField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label errorLabel;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    private ServiceDonationsEvent serviceDonationEvent;
    private HospitalServiceImpl hospitalService;
    private Stage dialogStage;
    private Mode mode;
    private DonationsEvent currentEvent;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        try {
            serviceDonationEvent = new ServiceDonationsEvent();
            hospitalService = new HospitalServiceImpl();

            // Initialize status combo box
            statusCombo.getItems().addAll("PLANNED", "ACTIVE", "COMPLETED", "CANCELLED");

            // Load hospitals
            loadHospitals();

            // Clear error label initially
            errorLabel.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to initialize: " + e.getMessage());
        }
    }

    private void loadHospitals() {
        try {
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            for (Hospital hospital : hospitals) {
                String displayText = hospital.getHospitalId() + " - " + hospital.getName();
                hospitalIdCombo.getItems().add(displayText);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load hospitals: " + e.getMessage());
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.EDIT) {
            saveBtn.setText("Update");
        } else {
            saveBtn.setText("Add");
        }
    }

    public void setEvent(DonationsEvent event) {
        this.currentEvent = event;
        populateFields(event);
    }

    private void populateFields(DonationsEvent event) {
        // Select hospital in combo - find matching hospital
        String hospitalId = event.getHospitalId();
        for (String item : hospitalIdCombo.getItems()) {
            if (item.startsWith(hospitalId)) {
                hospitalIdCombo.setValue(item);
                break;
            }
        }

        nameField.setText(event.getName());
        descriptionField.setText(event.getDescription());
        if (event.getStartDate() != null) {
            startDatePicker.setValue(event.getStartDate().toLocalDate());
        }
        if (event.getEndDate() != null) {
            endDatePicker.setValue(event.getEndDate().toLocalDate());
        }
        locationField.setText(event.getLocation());
        targetBloodTypesField.setText(event.getTargetBloodTypes());
        targetCollectionUnitsField.setText(String.valueOf(event.getTargetCollectionUnits()));
        statusCombo.setValue(event.getStatus());
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            String hospitalSelection = hospitalIdCombo.getValue();
            String hospitalId = hospitalSelection.split(" - ")[0];

            if (mode == Mode.ADD) {
                DonationsEvent newEvent = new DonationsEvent();
                newEvent.setHospitalId(hospitalId);
                newEvent.setName(nameField.getText().trim());
                newEvent.setDescription(descriptionField.getText().trim());
                newEvent.setStartDate(startDatePicker.getValue().atStartOfDay());
                newEvent.setEndDate(endDatePicker.getValue().atTime(23, 59, 59));
                newEvent.setLocation(locationField.getText().trim());
                newEvent.setTargetBloodTypes(targetBloodTypesField.getText().trim());
                newEvent.setTargetCollectionUnits(Integer.parseInt(targetCollectionUnitsField.getText().trim()));
                newEvent.setStatus(statusCombo.getValue());

                serviceDonationEvent.ajouter(newEvent);
            } else if (mode == Mode.EDIT) {
                currentEvent.setHospitalId(hospitalId);
                currentEvent.setName(nameField.getText().trim());
                currentEvent.setDescription(descriptionField.getText().trim());
                currentEvent.setStartDate(startDatePicker.getValue().atStartOfDay());
                currentEvent.setEndDate(endDatePicker.getValue().atTime(23, 59, 59));
                currentEvent.setLocation(locationField.getText().trim());
                currentEvent.setTargetBloodTypes(targetBloodTypesField.getText().trim());
                currentEvent.setTargetCollectionUnits(Integer.parseInt(targetCollectionUnitsField.getText().trim()));
                currentEvent.setStatus(statusCombo.getValue());

                System.out.println("Modifying event with ID: " + currentEvent.getEventId());
                System.out.println("Hospital ID: " + currentEvent.getHospitalId());
                System.out.println("Event Name: " + currentEvent.getName());
                serviceDonationEvent.modifier(currentEvent);
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            dialogStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to save event: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        errorLabel.setText("");

        if (hospitalIdCombo.getValue() == null || hospitalIdCombo.getValue().trim().isEmpty()) {
            showError("Please select a hospital");
            return false;
        }

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showError("Please enter event name");
            return false;
        }

        if (startDatePicker.getValue() == null) {
            showError("Please select start date");
            return false;
        }

        if (endDatePicker.getValue() == null) {
            showError("Please select end date");
            return false;
        }

        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showError("Start date must be before end date");
            return false;
        }

        if (statusCombo.getValue() == null || statusCombo.getValue().trim().isEmpty()) {
            showError("Please select status");
            return false;
        }

        try {
            Integer.parseInt(targetCollectionUnitsField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Target collection units must be a valid number");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}
