package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.DonorEligibilityService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DonorEligibilityDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> eligibilityFilter;
    @FXML private VBox eligibilityContainer;
    @FXML private Text totalRecordsLabel;
    @FXML private Text eligibleCountLabel;
    @FXML private Text notEligibleCountLabel;
    @FXML private Button addEligibilityBtn;

    private DonorEligibilityService eligibilityService;
    private List<DonorEligibility> allRecords;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private boolean readOnlyDonor;
    private String currentUserId;

    @FXML
    public void initialize() {
        eligibilityService = new DonorEligibilityService();
        Users currentUser = AppSession.getCurrentUser();
        readOnlyDonor = currentUser != null && currentUser.getUserType() == UserType.DONOR;
        currentUserId = currentUser != null ? currentUser.getId() : null;

        if (addEligibilityBtn != null) {
            addEligibilityBtn.setVisible(!readOnlyDonor);
            addEligibilityBtn.setManaged(!readOnlyDonor);
        }

        // Initialize filter combo box
        eligibilityFilter.getItems().addAll("All Eligibility", "Eligible", "Not Eligible");
        eligibilityFilter.setValue("All Eligibility");

        loadRecords();
    }

    private void loadRecords() {
        if (readOnlyDonor) {
            allRecords = new ArrayList<>();
            if (currentUserId != null && !currentUserId.trim().isEmpty()) {
                DonorEligibility ownRecord = eligibilityService.getOne(currentUserId);
                if (ownRecord != null) {
                    allRecords.add(ownRecord);
                }
            }
        } else {
            allRecords = eligibilityService.getAll(null);
        }
        displayRecords(allRecords);
        updateStats();
    }

    private void displayRecords(List<DonorEligibility> records) {
        eligibilityContainer.getChildren().clear();

        if (records.isEmpty()) {
            Label emptyLabel = new Label("No eligibility records found");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 40px;");
            eligibilityContainer.getChildren().add(emptyLabel);
            return;
        }

        for (DonorEligibility record : records) {
            eligibilityContainer.getChildren().add(createRecordCard(record));
        }
    }

    private VBox createRecordCard(DonorEligibility record) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card");
        card.setPadding(new Insets(20));

        // Header with Status
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Status Icon
        Region statusIcon = new Region();
        statusIcon.getStyleClass().add("user-avatar");
        if (record.getIsCurrentlyEligible() != null && record.getIsCurrentlyEligible()) {
            statusIcon.setStyle("-fx-background-color: linear-gradient(to bottom right, #10b981, #059669);");
        } else {
            statusIcon.setStyle("-fx-background-color: linear-gradient(to bottom right, #ef4444, #dc2626);");
        }
        statusIcon.setPrefSize(50, 50);
        statusIcon.setMinSize(50, 50);
        statusIcon.setMaxSize(50, 50);

        // Record Info
        VBox recordInfo = new VBox(5);
        HBox.setHgrow(recordInfo, Priority.ALWAYS);

        Text donorIdText = new Text("Donor ID: " + (record.getId() != null ? record.getId().substring(0, Math.min(13, record.getId().length())) + "..." : "N/A"));
        donorIdText.getStyleClass().add("user-name");

        Text bloodTypeText = new Text("Blood Type: " + (record.getBloodTypeCache() != null ? record.getBloodTypeCache() : "N/A"));
        bloodTypeText.getStyleClass().add("user-email");

        recordInfo.getChildren().addAll(donorIdText, bloodTypeText);

        // Status Badge
        Label statusBadge = new Label(record.getIsCurrentlyEligible() != null && record.getIsCurrentlyEligible() ? "Eligible" : "Not Eligible");
        statusBadge.getStyleClass().add("user-type-badge");
        if (record.getIsCurrentlyEligible() != null && record.getIsCurrentlyEligible()) {
            statusBadge.getStyleClass().add("badge-donor");
        } else {
            statusBadge.getStyleClass().add("badge-staff");
        }

        header.getChildren().addAll(statusIcon, recordInfo, statusBadge);

        // Details Grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);

        // Days Until Eligible
        HBox daysBox = createDetailItem("\u23F0", "Days Until Eligible: " +
            (record.getDaysUntilEligible() != null ? record.getDaysUntilEligible() : "N/A"));
        detailsGrid.add(daysBox, 0, 0);

        // Last Calculated
        HBox dateBox = createDetailItem("\uD83D\uDCC5", "Last Calculated: " +
            (record.getLastCalculatedAt() != null ? record.getLastCalculatedAt().format(dateFormatter) : "N/A"));
        detailsGrid.add(dateBox, 1, 0);

        // Location Cache
        HBox locationBox = createDetailItem("\uD83D\uDCCD", "Location: " +
            (record.getLatitudeCache() != null && record.getLongitudeCache() != null
                ? record.getLatitudeCache() + ", " + record.getLongitudeCache()
                : "N/A"));
        detailsGrid.add(locationBox, 0, 1, 2, 1);

        // Action Buttons
        card.getChildren().addAll(header, new Separator(), detailsGrid);

        if (!readOnlyDonor) {
            HBox actionButtons = new HBox(10);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);

            Button modifyBtn = new Button("Modify");
            modifyBtn.getStyleClass().add("btn-modify");
            modifyBtn.setOnAction(e -> handleModifyEligibility(record));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("btn-delete");
            deleteBtn.setOnAction(e -> handleDeleteEligibility(record));

            actionButtons.getChildren().addAll(modifyBtn, deleteBtn);
            card.getChildren().add(actionButtons);
        }

        return card;
    }

    private HBox createDetailItem(String icon, String text) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Text iconText = new Text(icon);
        iconText.getStyleClass().add("detail-icon");

        Text valueText = new Text(text);
        valueText.getStyleClass().add("detail-text");

        box.getChildren().addAll(iconText, valueText);
        return box;
    }

    private void updateStats() {
        totalRecordsLabel.setText(String.valueOf(allRecords.size()));

        long eligibleCount = allRecords.stream()
                .filter(r -> r.getIsCurrentlyEligible() != null && r.getIsCurrentlyEligible())
                .count();
        eligibleCountLabel.setText(String.valueOf(eligibleCount));

        long notEligibleCount = allRecords.stream()
                .filter(r -> r.getIsCurrentlyEligible() == null || !r.getIsCurrentlyEligible())
                .count();
        notEligibleCountLabel.setText(String.valueOf(notEligibleCount));
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedFilter = eligibilityFilter.getValue();

        List<DonorEligibility> filtered = allRecords.stream()
                .filter(record -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            (record.getId() != null && record.getId().toLowerCase().contains(searchText)) ||
                            (record.getBloodTypeCache() != null && record.getBloodTypeCache().toLowerCase().contains(searchText));

                    boolean matchesFilter = selectedFilter.equals("All Eligibility") ||
                            (selectedFilter.equals("Eligible") && record.getIsCurrentlyEligible() != null && record.getIsCurrentlyEligible()) ||
                            (selectedFilter.equals("Not Eligible") && (record.getIsCurrentlyEligible() == null || !record.getIsCurrentlyEligible()));

                    return matchesSearch && matchesFilter;
                })
                .collect(Collectors.toList());

        displayRecords(filtered);
    }

    @FXML
    private void handleFilter() {
        handleSearch();
    }

    @FXML
    private void handleAddEligibility() {
        if (readOnlyDonor) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonorEligibilityDialog.fxml"));
            Parent root = loader.load();

            DonorEligibilityDialogController controller = loader.getController();
            controller.setMode(DonorEligibilityDialogController.Mode.ADD);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Donor Eligibility");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadRecords();
            });

            dialogStage.showAndWait();
            loadRecords();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open add eligibility dialog: " + e.getMessage());
        }
    }

    private void handleModifyEligibility(DonorEligibility record) {
        if (readOnlyDonor) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonorEligibilityDialog.fxml"));
            Parent root = loader.load();

            DonorEligibilityDialogController controller = loader.getController();
            controller.setMode(DonorEligibilityDialogController.Mode.EDIT);
            controller.setRecord(record);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Donor Eligibility");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadRecords();
            });

            dialogStage.showAndWait();
            loadRecords();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open edit eligibility dialog: " + e.getMessage());
        }
    }

    private void handleDeleteEligibility(DonorEligibility record) {
        if (readOnlyDonor) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Eligibility Record");
        alert.setHeaderText("Delete eligibility record for donor " + record.getId() + "?");
        alert.setContentText("This action cannot be undone. Are you sure you want to delete this record?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            eligibilityService.supprimer(record.getId());
            loadRecords();
            showAlert("Success", "Eligibility record deleted successfully!");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
