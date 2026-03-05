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
import tn.edu.esprit.entities.Donor_Alerts;
import tn.edu.esprit.services.ServiceDonor_Alerts;

public class DonorAlertsCrudController {

    private static final String[] RESPONSE_VALUES = { "INTERESTED", "NOT_INTERESTED", "ALREADY_DONATED",
            "NO_RESPONSE" };

    private final ServiceDonor_Alerts service = new ServiceDonor_Alerts();
    private Runnable onBack;
    private List<UUID> alertIds = new ArrayList<>();
    private List<UUID> donorIds = new ArrayList<>();

    @FXML
    private TableView<Donor_Alerts> table;
    @FXML
    private TableColumn<Donor_Alerts, UUID> donorAlertIdCol;
    @FXML
    private TableColumn<Donor_Alerts, UUID> alertIdCol;
    @FXML
    private TableColumn<Donor_Alerts, UUID> donorIdCol;
    @FXML
    private TableColumn<Donor_Alerts, Boolean> isNotifiedCol;
    @FXML
    private TableColumn<Donor_Alerts, LocalDate> notificationSentAtCol;
    @FXML
    private TableColumn<Donor_Alerts, Boolean> isReadCol;
    @FXML
    private TableColumn<Donor_Alerts, LocalDate> readAtCol;
    @FXML
    private TableColumn<Donor_Alerts, String> donorResponseCol;
    @FXML
    private TextField donorAlertIdField;
    @FXML
    private ComboBox<UUID> alertIdComboBox;
    @FXML
    private ComboBox<UUID> donorIdComboBox;
    @FXML
    private CheckBox isNotifiedCheckBox;
    @FXML
    private DatePicker notificationSentAtPicker;
    @FXML
    private CheckBox isReadCheckBox;
    @FXML
    private DatePicker readAtPicker;
    @FXML
    private ComboBox<String> donorResponseComboBox;

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void initialize() {
        donorResponseComboBox.getItems().setAll(RESPONSE_VALUES);
        loadReferenceData();
        configureTable();
        loadData();
    }

    private void loadReferenceData() {
        alertIds = service.getAlertIds();
        donorIds = service.getDonorIds();
        setupUuidComboBox(alertIdComboBox, alertIds);
        setupUuidComboBox(donorIdComboBox, donorIds);
    }

    private void configureTable() {
        donorAlertIdCol.setCellValueFactory(new PropertyValueFactory<>("donor_alert_id"));
        alertIdCol.setCellValueFactory(new PropertyValueFactory<>("alert_id"));
        donorIdCol.setCellValueFactory(new PropertyValueFactory<>("donor_id"));
        isNotifiedCol.setCellValueFactory(new PropertyValueFactory<>("is_notified"));
        notificationSentAtCol.setCellValueFactory(new PropertyValueFactory<>("notification_sent_at"));
        isReadCol.setCellValueFactory(new PropertyValueFactory<>("is_read"));
        readAtCol.setCellValueFactory(new PropertyValueFactory<>("read_at"));
        donorResponseCol.setCellValueFactory(new PropertyValueFactory<>("donor_response"));

        table.getColumns().setAll(donorAlertIdCol, alertIdCol, donorIdCol, isNotifiedCol, notificationSentAtCol,
                isReadCol, readAtCol, donorResponseCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> fillFields(selected));
    }

    @FXML
    private void handleAdd() {
        addDonorAlert();
    }

    @FXML
    private void handleUpdate() {
        updateDonorAlert();
    }

    @FXML
    private void handleDelete() {
        deleteDonorAlert();
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
        List<Donor_Alerts> donorAlerts = service.getAll(new Donor_Alerts());
        if (donorAlerts == null) {
            donorAlerts = new ArrayList<>();
        }
        table.setItems(FXCollections.observableArrayList(donorAlerts));
        table.refresh();
    }

    private void addDonorAlert() {
        UUID alertId = readUuidSelection(alertIdComboBox, alertIds, "Alert ID");
        if (alertId == null) {
            return;
        }

        UUID donorId = readUuidSelection(donorIdComboBox, donorIds, "Donor ID");
        if (donorId == null) {
            return;
        }

        if (!validateFields()) {
            return;
        }

        Donor_Alerts donorAlert = new Donor_Alerts(alertId, donorId, isNotifiedCheckBox.isSelected(),
                notificationSentAtPicker.getValue(), isReadCheckBox.isSelected(), readAtPicker.getValue(),
                donorResponseComboBox.getValue());

        service.ajouter(donorAlert);
        loadData();
        clearFields();
        showInfo("Donor alert added.");
    }

    private void updateDonorAlert() {
        UUID id = readIdFromFormOrSelection();
        if (id == null) {
            return;
        }

        UUID alertId = readUuidSelection(alertIdComboBox, alertIds, "Alert ID");
        if (alertId == null) {
            return;
        }

        UUID donorId = readUuidSelection(donorIdComboBox, donorIds, "Donor ID");
        if (donorId == null) {
            return;
        }

        if (!validateFields()) {
            return;
        }

        Donor_Alerts donorAlert = new Donor_Alerts(id, alertId, donorId, isNotifiedCheckBox.isSelected(),
                notificationSentAtPicker.getValue(), isReadCheckBox.isSelected(), readAtPicker.getValue(),
                donorResponseComboBox.getValue());

        service.modifier(donorAlert);
        loadData();
        clearFields();
        showInfo("Donor alert updated.");
    }

    private void deleteDonorAlert() {
        UUID id = readIdFromFormOrSelection();
        if (id == null) {
            return;
        }
        service.supprimer(id);
        loadData();
        clearFields();
        showInfo("Donor alert deleted.");
    }

    private boolean validateFields() {
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

    private UUID readIdFromFormOrSelection() {
        String idText = donorAlertIdField.getText().trim();
        if (!idText.isEmpty()) {
            try {
                return UUID.fromString(idText);
            } catch (IllegalArgumentException ex) {
                showError("Donor Alert ID must be a valid UUID.");
                return null;
            }
        }

        Donor_Alerts selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return selected.getDonor_alert_id();
        }

        showError("Select a row or enter a Donor Alert ID.");
        return null;
    }

    private void fillFields(Donor_Alerts selected) {
        if (selected == null) {
            return;
        }
        donorAlertIdField.setText(selected.getDonor_alert_id() == null ? "" : selected.getDonor_alert_id().toString());
        alertIdComboBox.setValue(selected.getAlert_id());
        donorIdComboBox.setValue(selected.getDonor_id());
        isNotifiedCheckBox.setSelected(selected.getIs_notified());
        notificationSentAtPicker.setValue(selected.getNotification_sent_at());
        isReadCheckBox.setSelected(selected.getIs_read());
        readAtPicker.setValue(selected.getRead_at());
        donorResponseComboBox.setValue(selected.getDonor_response());
    }

    private void clearFields() {
        donorAlertIdField.clear();
        alertIdComboBox.getSelectionModel().clearSelection();
        donorIdComboBox.getSelectionModel().clearSelection();
        isNotifiedCheckBox.setSelected(false);
        notificationSentAtPicker.setValue(null);
        isReadCheckBox.setSelected(false);
        readAtPicker.setValue(null);
        donorResponseComboBox.getSelectionModel().clearSelection();
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
