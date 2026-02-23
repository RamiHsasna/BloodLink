package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import tn.edu.esprit.entities.BloodTransferRequest;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.entities.TransfertStatus;
import tn.edu.esprit.services.HospitalServiceImpl;
import tn.edu.esprit.services.TransfertServiceImpl;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class TransferFormController implements Initializable {

    // ---- FXML Fields ----
    @FXML private VBox modalCard;
    @FXML private Label lblFormTitle;
    @FXML private Label lblFormSubtitle;
    @FXML private ComboBox<Hospital> cbRequestingHospital;
    @FXML private ComboBox<Hospital> cbApprovingHospital;
    @FXML private ComboBox<String> cbBloodType;
    @FXML private Spinner<Integer> spQuantity;
    @FXML private TextArea taReason;
    @FXML private DatePicker dpExpectedDelivery;
    @FXML private TextArea taNotes;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
    @FXML private Button btnClose;
    @FXML private Label lblStatusMessage;

    // Error labels
    @FXML private Label lblRequestingHospitalError;
    @FXML private Label lblApprovingHospitalError;
    @FXML private Label lblBloodTypeError;
    @FXML private Label lblQuantityError;
    @FXML private Label lblReasonError;
    @FXML private Label lblExpectedDeliveryError;

    // ---- State ----
    private final TransfertServiceImpl transferService = new TransfertServiceImpl();
    private final HospitalServiceImpl hospitalService = new HospitalServiceImpl();

    private TransferListController transferListController;
    private StackPane parentContainer;
    private BloodTransferRequest editingTransfer = null;
    private boolean isEditMode = false;

    private List<Hospital> allHospitals = new ArrayList<>();

    private static final String[] BLOOD_TYPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadHospitals();
        setupHospitalComboBoxes();
        setupBloodTypeComboBox();
        setupQuantitySpinner();
    }

    // ==================== SETUP ====================

    private void loadHospitals() {
        try {
            allHospitals = hospitalService.getAllHospitals();
        } catch (Exception e) {
            System.err.println("Error loading hospitals: " + e.getMessage());
            allHospitals = new ArrayList<>();
        }
    }

    private void setupHospitalComboBoxes() {
        StringConverter<Hospital> hospitalConverter = new StringConverter<Hospital>() {
            @Override
            public String toString(Hospital hospital) {
                if (hospital == null) return null;
                String city = hospital.getCity() != null ? " (" + hospital.getCity() + ")" : "";
                return hospital.getName() + city;
            }

            @Override
            public Hospital fromString(String string) {
                // Not used for non-editable combo
                return null;
            }
        };

        cbRequestingHospital.setConverter(hospitalConverter);
        cbApprovingHospital.setConverter(hospitalConverter);

        // Only show active hospitals
        List<Hospital> activeHospitals = new ArrayList<>();
        for (Hospital h : allHospitals) {
            if (h.isActive()) {
                activeHospitals.add(h);
            }
        }

        cbRequestingHospital.getItems().addAll(activeHospitals);
        cbApprovingHospital.getItems().addAll(activeHospitals);

        // When requesting hospital changes, filter it out from approving list
        cbRequestingHospital.setOnAction(e -> {
            Hospital selected = cbRequestingHospital.getValue();
            Hospital currentApproving = cbApprovingHospital.getValue();

            cbApprovingHospital.getItems().clear();
            for (Hospital h : activeHospitals) {
                if (selected == null || !h.getHospitalId().equals(selected.getHospitalId())) {
                    cbApprovingHospital.getItems().add(h);
                }
            }

            // Restore previous selection if still valid
            if (currentApproving != null && (selected == null
                    || !currentApproving.getHospitalId().equals(selected.getHospitalId()))) {
                cbApprovingHospital.setValue(currentApproving);
            }
        });
    }

    private void setupBloodTypeComboBox() {
        cbBloodType.getItems().addAll(BLOOD_TYPES);
    }

    private void setupQuantitySpinner() {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1);
        spQuantity.setValueFactory(valueFactory);
    }

    // ==================== PUBLIC SETUP METHODS ====================

    /**
     * Links back to the TransferListController so we can refresh the list after submit.
     */
    public void setTransferListController(TransferListController controller) {
        this.transferListController = controller;
    }

    /**
     * Sets the parent StackPane so we can remove this modal overlay when closing.
     */
    public void setParentContainer(StackPane parentContainer) {
        this.parentContainer = parentContainer;
    }

    /**
     * Switches the form to EDIT mode and pre-fills all fields with the transfer's data.
     */
    public void setEditMode(BloodTransferRequest transfer) {
        this.isEditMode = true;
        this.editingTransfer = transfer;

        lblFormTitle.setText("Modifier le Transfert");
        lblFormSubtitle.setText("Modifier les informations de la demande de transfert #" + transfer.getTransferId() + ".");
        btnSubmit.setText("Enregistrer les modifications");

        // Pre-fill requesting hospital
        if (transfer.getRequestingHospitalId() != null) {
            for (Hospital h : cbRequestingHospital.getItems()) {
                if (h.getHospitalId().equals(transfer.getRequestingHospitalId())) {
                    cbRequestingHospital.setValue(h);
                    break;
                }
            }
        }

        // Pre-fill approving hospital
        if (transfer.getApprovingHospitalId() != null) {
            for (Hospital h : cbApprovingHospital.getItems()) {
                if (h.getHospitalId().equals(transfer.getApprovingHospitalId())) {
                    cbApprovingHospital.setValue(h);
                    break;
                }
            }
        }

        // Pre-fill blood type
        if (transfer.getBloodTypeId() != null) {
            cbBloodType.setValue(transfer.getBloodTypeId());
        }

        // Pre-fill quantity
        if (transfer.getQuantityUnitsRequested() != null) {
            spQuantity.getValueFactory().setValue(transfer.getQuantityUnitsRequested());
        }

        // Pre-fill reason
        if (transfer.getReason() != null) {
            taReason.setText(transfer.getReason());
        }

        // Pre-fill expected delivery date
        if (transfer.getDeliveryExpectedAt() != null) {
            dpExpectedDelivery.setValue(transfer.getDeliveryExpectedAt().toLocalDateTime().toLocalDate());
        }

        // Pre-fill notes
        if (transfer.getNotes() != null) {
            taNotes.setText(transfer.getNotes());
        }
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
                updateTransfer();
            } else {
                createTransfer();
            }

            // Refresh the transfer list and close the modal
            if (transferListController != null) {
                transferListController.refreshData();
            }
            closeModal();

        } catch (Exception e) {
            showStatusMessage("Erreur: " + e.getMessage(), true);
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

    private void createTransfer() {
        BloodTransferRequest transfer = buildTransferFromForm();
        transferService.ajouter(transfer);
    }

    private void updateTransfer() {
        // Preserve the existing ID, status, timestamps etc. and update editable fields
        Hospital requesting = cbRequestingHospital.getValue();
        Hospital approving = cbApprovingHospital.getValue();

        editingTransfer.setRequestingHospitalId(requesting.getHospitalId());
        editingTransfer.setApprovingHospitalId(approving != null ? approving.getHospitalId() : null);
        editingTransfer.setBloodTypeId(cbBloodType.getValue());
        editingTransfer.setQuantityUnitsRequested(spQuantity.getValue());
        editingTransfer.setReason(taReason.getText() != null ? taReason.getText().trim() : null);
        editingTransfer.setNotes(taNotes.getText() != null && !taNotes.getText().trim().isEmpty()
                ? taNotes.getText().trim() : null);

        if (dpExpectedDelivery.getValue() != null) {
            LocalDateTime expectedDateTime = LocalDateTime.of(dpExpectedDelivery.getValue(), LocalTime.MIDNIGHT);
            editingTransfer.setDeliveryExpectedAt(Timestamp.valueOf(expectedDateTime));
        } else {
            editingTransfer.setDeliveryExpectedAt(null);
        }

        transferService.modifier(editingTransfer);
    }

    /**
     * Builds a new BloodTransferRequest entity from the current form field values.
     */
    private BloodTransferRequest buildTransferFromForm() {
        BloodTransferRequest transfer = new BloodTransferRequest();

        Hospital requesting = cbRequestingHospital.getValue();
        Hospital approving = cbApprovingHospital.getValue();

        transfer.setRequestingHospitalId(requesting.getHospitalId());
        transfer.setApprovingHospitalId(approving != null ? approving.getHospitalId() : null);
        transfer.setBloodTypeId(cbBloodType.getValue());
        transfer.setQuantityUnitsRequested(spQuantity.getValue());
        transfer.setQuantityUnitsApproved(0);
        transfer.setStatus(TransfertStatus.PENDING);
        transfer.setReason(taReason.getText() != null ? taReason.getText().trim() : null);
        transfer.setNotes(taNotes.getText() != null && !taNotes.getText().trim().isEmpty()
                ? taNotes.getText().trim() : null);

        if (dpExpectedDelivery.getValue() != null) {
            LocalDateTime expectedDateTime = LocalDateTime.of(dpExpectedDelivery.getValue(), LocalTime.MIDNIGHT);
            transfer.setDeliveryExpectedAt(Timestamp.valueOf(expectedDateTime));
        }

        // Staff IDs — placeholder; in a real app these would come from the logged-in user session
        transfer.setRequestingStaffId(null);
        transfer.setApprovingStaffId(null);

        return transfer;
    }

    // ==================== VALIDATION ====================

    /**
     * Validates all form fields. Returns true if valid, false otherwise.
     */
    private boolean validateForm() {
        boolean valid = true;

        // Requesting hospital — required
        if (cbRequestingHospital.getValue() == null) {
            showFieldError(null, lblRequestingHospitalError, "Veuillez selectionner l'hopital demandeur.");
            valid = false;
        }

        // Approving hospital — required
        if (cbApprovingHospital.getValue() == null) {
            showFieldError(null, lblApprovingHospitalError, "Veuillez selectionner l'hopital fournisseur.");
            valid = false;
        }

        // Same hospital check
        if (cbRequestingHospital.getValue() != null && cbApprovingHospital.getValue() != null) {
            if (cbRequestingHospital.getValue().getHospitalId()
                    .equals(cbApprovingHospital.getValue().getHospitalId())) {
                showFieldError(null, lblApprovingHospitalError,
                        "L'hopital fournisseur doit etre different de l'hopital demandeur.");
                valid = false;
            }
        }

        // Blood type — required
        if (cbBloodType.getValue() == null || cbBloodType.getValue().isEmpty()) {
            showFieldError(null, lblBloodTypeError, "Veuillez selectionner un groupe sanguin.");
            valid = false;
        }

        // Quantity — must be at least 1
        if (spQuantity.getValue() == null || spQuantity.getValue() < 1) {
            showFieldError(null, lblQuantityError, "La quantite doit etre d'au moins 1 unite.");
            valid = false;
        }

        // Reason — required
        String reason = taReason.getText() != null ? taReason.getText().trim() : "";
        if (reason.isEmpty()) {
            showFieldError(null, lblReasonError, "Veuillez indiquer le motif de la demande.");
            valid = false;
        } else if (reason.length() < 5) {
            showFieldError(null, lblReasonError, "Le motif doit contenir au moins 5 caracteres.");
            valid = false;
        }

        // Expected delivery — optional, but if set must be in the future
        if (dpExpectedDelivery.getValue() != null) {
            if (dpExpectedDelivery.getValue().isBefore(LocalDate.now())) {
                showFieldError(null, lblExpectedDeliveryError,
                        "La date de livraison doit etre aujourd'hui ou dans le futur.");
                valid = false;
            }
        }

        return valid;
    }

    // ==================== UI HELPERS ====================

    /**
     * Shows an inline error message below a field.
     */
    private void showFieldError(Control field, Label errorLabel, String message) {
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
        clearFieldError(null, lblRequestingHospitalError);
        clearFieldError(null, lblApprovingHospitalError);
        clearFieldError(null, lblBloodTypeError);
        clearFieldError(null, lblQuantityError);
        clearFieldError(null, lblReasonError);
        clearFieldError(null, lblExpectedDeliveryError);

        lblStatusMessage.setVisible(false);
        lblStatusMessage.setManaged(false);
    }

    /**
     * Clears the error style and hides the error label for a single field.
     */
    private void clearFieldError(Control field, Label errorLabel) {
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
            if (!parentContainer.getChildren().isEmpty()) {
                parentContainer.getChildren().remove(parentContainer.getChildren().size() - 1);
            }
        }
    }
}
