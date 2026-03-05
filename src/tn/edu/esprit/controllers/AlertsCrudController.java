package tn.edu.esprit.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.edu.esprit.entities.Alerts;
import tn.edu.esprit.services.ServiceAlerts;

public class AlertsCrudController {

    private static final String[] SEVERITY_VALUES = { "INFO", "WARNING", "URGENT", "CRITICAL" };

    private final ServiceAlerts service = new ServiceAlerts();
    private Runnable onBack;
    private List<UUID> hospitalIds = new ArrayList<>();
    private List<UUID> staffIds = new ArrayList<>();

    @FXML
    private TableView<Alerts> table;
    @FXML
    private TableColumn<Alerts, UUID> alertIdCol;
    @FXML
    private TableColumn<Alerts, UUID> hospitalIdCol;
    @FXML
    private TableColumn<Alerts, UUID> staffIdCol;
    @FXML
    private TableColumn<Alerts, String> bloodTypeIdCol;
    @FXML
    private TableColumn<Alerts, String> severityCol;
    @FXML
    private TableColumn<Alerts, Integer> quantityNeededCol;
    @FXML
    private TableColumn<Alerts, String> titleCol;
    @FXML
    private TableColumn<Alerts, String> messageCol;
    @FXML
    private TableColumn<Alerts, Boolean> isResolvedCol;
    @FXML
    private TableColumn<Alerts, Integer> targetRadiusCol;
    @FXML
    private TableColumn<Alerts, LocalDate> createdAtCol;
    @FXML
    private TableColumn<Alerts, LocalDate> resolvedAtCol;
    @FXML
    private TextField alertIdField;
    @FXML
    private ComboBox<UUID> hospitalIdComboBox;
    @FXML
    private ComboBox<UUID> staffIdComboBox;
    @FXML
    private TextField bloodTypeIdField;
    @FXML
    private ComboBox<String> severityComboBox;
    @FXML
    private TextField quantityNeededField;
    @FXML
    private TextField titleField;
    @FXML
    private TextField messageField;
    @FXML
    private CheckBox isResolvedCheckBox;
    @FXML
    private TextField targetRadiusField;
    @FXML
    private DatePicker createdAtPicker;
    @FXML
    private DatePicker resolvedAtPicker;

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void initialize() {
        severityComboBox.getItems().setAll(SEVERITY_VALUES);
        loadReferenceData();
        configureTable();
        loadData();
    }

    private void loadReferenceData() {
        hospitalIds = service.getHospitalIds();
        staffIds = service.getStaffIds();
        setupUuidComboBox(hospitalIdComboBox, hospitalIds);
        setupUuidComboBox(staffIdComboBox, staffIds);
    }

    private void configureTable() {
        alertIdCol.setCellValueFactory(new PropertyValueFactory<>("alert_id"));
        hospitalIdCol.setCellValueFactory(new PropertyValueFactory<>("hospital_id"));
        staffIdCol.setCellValueFactory(new PropertyValueFactory<>("staff_id"));
        bloodTypeIdCol.setCellValueFactory(new PropertyValueFactory<>("blood_type_id"));
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        quantityNeededCol.setCellValueFactory(new PropertyValueFactory<>("quantity_needed"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        messageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        isResolvedCol.setCellValueFactory(new PropertyValueFactory<>("is_resolved"));
        targetRadiusCol.setCellValueFactory(new PropertyValueFactory<>("target_radius_km"));
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("created_at"));
        resolvedAtCol.setCellValueFactory(new PropertyValueFactory<>("resolved_at"));

        table.getColumns().setAll(alertIdCol, hospitalIdCol, staffIdCol, bloodTypeIdCol, severityCol,
                quantityNeededCol, titleCol, messageCol, isResolvedCol, targetRadiusCol, createdAtCol, resolvedAtCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> fillFields(selected));
    }

    @FXML
    private void handleAdd() {
        addAlert();
    }

    @FXML
    private void handleUpdate() {
        updateAlert();
    }

    @FXML
    private void handleDelete() {
        deleteAlert();
    }

    @FXML
    private void handleClear() {
        clearFields();
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    private void loadData() {
        List<Alerts> alerts = service.getAll(new Alerts());
        if (alerts == null) {
            alerts = new ArrayList<>();
        }
        table.setItems(FXCollections.observableArrayList(alerts));
        table.refresh();
    }

    private void addAlert() {
        if (!validateFields()) {
            return;
        }

        UUID hospitalId = readUuidSelection(hospitalIdComboBox, hospitalIds, "Hospital ID");
        if (hospitalId == null) {
            return;
        }

        UUID staffId = readUuidSelection(staffIdComboBox, staffIds, "Staff ID");
        if (staffId == null) {
            return;
        }

        Integer quantityNeeded = parseRequiredInt(quantityNeededField, "Quantity Needed");
        if (quantityNeeded == null) {
            return;
        }

        Integer targetRadius = parseOptionalInt(targetRadiusField, "Target Radius (km)");
        if (!targetRadiusField.getText().trim().isEmpty() && targetRadius == null) {
            return;
        }

        Alerts alert = new Alerts(hospitalId, staffId, bloodTypeIdField.getText().trim(),
            severityComboBox.getValue(), quantityNeeded,
                titleField.getText().trim(), messageField.getText().trim(), isResolvedCheckBox.isSelected(),
            targetRadius, createdAtPicker.getValue(), resolvedAtPicker.getValue());

        service.ajouter(alert);
        loadData();
        clearFields();
        showInfo("Alert added.");
    }

    private void updateAlert() {
        UUID id = readIdFromFormOrSelection();
        if (id == null) {
            return;
        }
        if (!validateFields()) {
            return;
        }

        UUID hospitalId = readUuidSelection(hospitalIdComboBox, hospitalIds, "Hospital ID");
        if (hospitalId == null) {
            return;
        }

        UUID staffId = readUuidSelection(staffIdComboBox, staffIds, "Staff ID");
        if (staffId == null) {
            return;
        }

        Integer quantityNeeded = parseRequiredInt(quantityNeededField, "Quantity Needed");
        if (quantityNeeded == null) {
            return;
        }

        Integer targetRadius = parseOptionalInt(targetRadiusField, "Target Radius (km)");
        if (!targetRadiusField.getText().trim().isEmpty() && targetRadius == null) {
            return;
        }

        Alerts alert = new Alerts(id, hospitalId, staffId, bloodTypeIdField.getText().trim(),
            severityComboBox.getValue(), quantityNeeded,
                titleField.getText().trim(), messageField.getText().trim(), isResolvedCheckBox.isSelected(),
            targetRadius, createdAtPicker.getValue(), resolvedAtPicker.getValue());

        service.modifier(alert);
        loadData();
        clearFields();
        showInfo("Alert updated.");
    }

    private void deleteAlert() {
        UUID id = readIdFromFormOrSelection();
        if (id == null) {
            return;
        }
        service.supprimer(id);
        loadData();
        clearFields();
        showInfo("Alert deleted.");
    }

    private boolean validateFields() {
        if (bloodTypeIdField.getText().trim().isEmpty()
                || quantityNeededField.getText().trim().isEmpty()
                || titleField.getText().trim().isEmpty()
                || messageField.getText().trim().isEmpty()) {
            showError("Blood Type ID, Severity, Quantity, Title, and Message are required.");
            return false;
        }

        if (severityComboBox.getValue() == null) {
            showError("Severity is required.");
            return false;
        }

        return true;
    }

    private UUID readUuidSelection(ComboBox<UUID> comboBox, List<UUID> allowed, String label) {
        UUID selected = comboBox.getValue();
        if (selected != null) {
            return selected;
        }

        String text = comboBox.getEditor().getText();
        if (text == null || text.trim().isEmpty()) {
            showError(label + " is required.");
            return null;
        }

        try {
            UUID parsed = UUID.fromString(text.trim());
            if (!allowed.contains(parsed)) {
                showError(label + " must be selected from existing IDs.");
                return null;
            }
            comboBox.setValue(parsed);
            return parsed;
        } catch (IllegalArgumentException ex) {
            showError(label + " must be a valid UUID.");
            return null;
        }
    }

    private void setupUuidComboBox(ComboBox<UUID> comboBox, List<UUID> ids) {
        javafx.collections.ObservableList<UUID> items = FXCollections.observableArrayList(ids);
        javafx.collections.transformation.FilteredList<UUID> filtered =
                new javafx.collections.transformation.FilteredList<>(items, value -> true);

        comboBox.setItems(filtered);
        comboBox.setEditable(true);
        comboBox.setConverter(new javafx.util.StringConverter<UUID>() {
            @Override
            public String toString(UUID value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public UUID fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                try {
                    return UUID.fromString(text.trim());
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        });

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            String filter = newValue == null ? "" : newValue.toLowerCase();
            filtered.setPredicate(value -> value.toString().toLowerCase().contains(filter));
        });
    }

    private Integer parseRequiredInt(TextField field, String label) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            showError(label + " is required.");
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            showError(label + " must be a number.");
            return null;
        }
    }

    private Integer parseOptionalInt(TextField field, String label) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            showError(label + " must be a number.");
            return null;
        }
    }

    private UUID readIdFromFormOrSelection() {
        String idText = alertIdField.getText().trim();
        if (!idText.isEmpty()) {
            try {
                return UUID.fromString(idText);
            } catch (IllegalArgumentException ex) {
                showError("Alert ID must be a valid UUID.");
                return null;
            }
        }

        Alerts selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return selected.getAlert_id();
        }

        showError("Select a row or enter an Alert ID.");
        return null;
    }

    private void fillFields(Alerts selected) {
        if (selected == null) {
            return;
        }
        alertIdField.setText(selected.getAlert_id() == null ? "" : selected.getAlert_id().toString());
        hospitalIdComboBox.setValue(selected.getHospital_id());
        staffIdComboBox.setValue(selected.getStaff_id());
        bloodTypeIdField.setText(selected.getBlood_type_id());
        severityComboBox.setValue(selected.getSeverity());
        quantityNeededField.setText(String.valueOf(selected.getQuantity_needed()));
        titleField.setText(selected.getTitle());
        messageField.setText(selected.getMessage());
        isResolvedCheckBox.setSelected(selected.getIs_resolved());
        targetRadiusField.setText(selected.getTarget_radius_km() == null ? "" : String.valueOf(selected.getTarget_radius_km()));
        createdAtPicker.setValue(selected.getCreated_at());
        resolvedAtPicker.setValue(selected.getResolved_at());
    }

    private void clearFields() {
        alertIdField.clear();
        hospitalIdComboBox.getSelectionModel().clearSelection();
        staffIdComboBox.getSelectionModel().clearSelection();
        bloodTypeIdField.clear();
        severityComboBox.getSelectionModel().clearSelection();
        quantityNeededField.clear();
        titleField.clear();
        messageField.clear();
        isResolvedCheckBox.setSelected(false);
        targetRadiusField.clear();
        createdAtPicker.setValue(null);
        resolvedAtPicker.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void showInfo(String message) {
        showAlert("Success", message, Alert.AlertType.INFORMATION);
    }

    private void showError(String message) {
        showAlert("Error", message, Alert.AlertType.ERROR);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
