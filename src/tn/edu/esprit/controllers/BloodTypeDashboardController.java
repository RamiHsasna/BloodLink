package tn.edu.esprit.controllers;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.edu.esprit.entities.BloodType;
import tn.edu.esprit.services.BloodTypeServiceImpl;

public class BloodTypeDashboardController implements Initializable {

    @FXML private TextField searchField;
    @FXML private VBox cardsContainer;
    @FXML private Label lblTotal;
    @FXML private Label lblUniversalDonors;
    @FXML private Label lblUniversalRecipients;
    @FXML private Button btnAdd;

    private final BloodTypeServiceImpl bloodTypeService = new BloodTypeServiceImpl();
    private List<BloodType> allBloodTypes = List.of();

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(URL location, ResourceBundle resources) {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderCards());
        refreshData();
    }

    @FXML
    private void handleAdd() {
        openEditor(null);
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    @SuppressWarnings("unchecked")
    private void refreshData() {
        allBloodTypes = bloodTypeService.getAllBloodTypes();
        updateStats();
        renderCards();
    }

    private void updateStats() {
        lblTotal.setText(String.valueOf(allBloodTypes.size()));
        lblUniversalDonors.setText(String.valueOf(allBloodTypes.stream().filter(BloodType::isUniversalDonor).count()));
        lblUniversalRecipients.setText(String.valueOf(allBloodTypes.stream().filter(BloodType::isUniversalRecipient).count()));
    }

    private void renderCards() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<BloodType> filtered = allBloodTypes.stream()
                .filter(type -> query.isEmpty()
                        || contains(type.getBloodTypeId(), query)
                        || contains(type.getAboType(), query)
                        || contains(type.getRhFactor(), query)
                        || contains(type.getCompatibleDonors(), query))
                .collect(Collectors.toList());

        cardsContainer.getChildren().clear();
        if (filtered.isEmpty()) {
            cardsContainer.getChildren().add(buildEmptyState());
            return;
        }
        for (BloodType bloodType : filtered) {
            cardsContainer.getChildren().add(buildCard(bloodType));
        }
    }

    private VBox buildCard(BloodType bloodType) {
        VBox card = new VBox(12);
        card.getStyleClass().add("log-card");

        Label badge = new Label(bloodType.getBloodTypeId());
        badge.getStyleClass().addAll("badge", "badge-critical");

        Label title = new Label(formatTypeLabel(bloodType));
        title.getStyleClass().add("log-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, badge, title, spacer);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(6,
                detail("ABO", safe(bloodType.getAboType())),
                detail("Rh", safe(bloodType.getRhFactor())),
                detail("Compatible donors", safe(bloodType.getCompatibleDonors())),
                detail("Universal donor", bloodType.isUniversalDonor() ? "Yes" : "No"),
                detail("Universal recipient", bloodType.isUniversalRecipient() ? "Yes" : "No"));

        Button edit = new Button("Modify");
        edit.getStyleClass().add("btn-log-edit");
        edit.setOnAction(e -> openEditor(bloodType));

        Button delete = new Button("Delete");
        delete.getStyleClass().add("btn-log-delete");
        delete.setOnAction(e -> deleteBloodType(bloodType));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, footerSpacer, edit, delete);
        footer.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(header, body, footer);
        return card;
    }

    private HBox detail(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("log-card-detail-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("log-card-detail-value");
        valueNode.setWrapText(true);
        HBox row = new HBox(10, labelNode, valueNode);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildEmptyState() {
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.getStyleClass().add("logs-empty-state");
        Label title = new Label("No blood types found");
        title.getStyleClass().add("logs-empty-title");
        Label subtitle = new Label("Add blood type compatibility definitions or adjust the search.");
        subtitle.getStyleClass().add("logs-empty-subtitle");
        empty.getChildren().addAll(title, subtitle);
        return empty;
    }

    private void openEditor(BloodType existing) {
        Dialog<BloodType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Blood Type" : "Edit Blood Type");
        dialog.setHeaderText(null);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveType);

        TextField idField = new TextField(existing != null ? existing.getBloodTypeId() : "");
        idField.setPromptText("A+");
        idField.setDisable(existing != null);
        ComboBox<String> aboCombo = new ComboBox<>();
        aboCombo.getItems().addAll("A", "B", "AB", "O");
        aboCombo.setValue(existing != null ? existing.getAboType() : "A");
        ComboBox<String> rhCombo = new ComboBox<>();
        rhCombo.getItems().addAll("+", "-");
        rhCombo.setValue(existing != null ? existing.getRhFactor() : "+");
        CheckBox donorCheck = new CheckBox("Universal donor");
        donorCheck.setSelected(existing != null && existing.isUniversalDonor());
        CheckBox recipientCheck = new CheckBox("Universal recipient");
        recipientCheck.setSelected(existing != null && existing.isUniversalRecipient());
        TextArea compatibleArea = new TextArea(existing != null ? safe(existing.getCompatibleDonors()) : "");
        compatibleArea.setPromptText("O-, O+, A-");
        compatibleArea.setPrefRowCount(3);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(16));
        form.addRow(0, new Label("Blood type ID"), idField);
        form.addRow(1, new Label("ABO"), aboCombo);
        form.addRow(2, new Label("Rh"), rhCombo);
        form.addRow(3, new Label("Flags"), new VBox(6, donorCheck, recipientCheck));
        form.addRow(4, new Label("Compatible donors"), compatibleArea);
        dialog.getDialogPane().setContent(form);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!isValidBloodType(idField.getText(), aboCombo.getValue(), rhCombo.getValue())) {
                showError("Validation error", "Use a valid blood type ID matching ABO and Rh, for example A+, AB-, or O-.");
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            BloodType bloodType = existing != null ? existing : new BloodType();
            bloodType.setBloodTypeId(idField.getText().trim().toUpperCase());
            bloodType.setAboType(aboCombo.getValue());
            bloodType.setRhFactor(rhCombo.getValue());
            bloodType.setUniversalDonor(donorCheck.isSelected());
            bloodType.setUniversalRecipient(recipientCheck.isSelected());
            bloodType.setCompatibleDonors(compatibleArea.getText() != null ? compatibleArea.getText().trim() : "");
            return bloodType;
        });

        Optional<BloodType> result = dialog.showAndWait();
        result.ifPresent(bloodType -> {
            try {
                if (existing == null) {
                    bloodTypeService.ajouter(bloodType);
                } else {
                    bloodTypeService.modifier(bloodType);
                }
                refreshData();
            } catch (Exception exception) {
                showError("Save failed", exception.getMessage());
            }
        });
    }

    private void deleteBloodType(BloodType bloodType) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Blood Type");
        confirm.setHeaderText("Delete " + bloodType.getBloodTypeId() + "?");
        confirm.setContentText("This should only be done if no active records depend on this blood type.");
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                try {
                    bloodTypeService.supprimer(bloodType.getBloodTypeId());
                    refreshData();
                } catch (Exception exception) {
                    showError("Delete failed", exception.getMessage());
                }
            }
        });
    }

    private boolean isValidBloodType(String id, String abo, String rh) {
        if (id == null || abo == null || rh == null) {
            return false;
        }
        String normalized = id.trim().toUpperCase();
        return normalized.matches("^(A|B|AB|O)[+-]$") && normalized.equals(abo + rh);
    }

    private String formatTypeLabel(BloodType type) {
        return safe(type.getAboType()) + safe(type.getRhFactor());
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
