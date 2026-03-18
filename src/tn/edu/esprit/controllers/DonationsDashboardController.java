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
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.ServiceDonation;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DonationsDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private VBox donationsContainer;
    @FXML private Text totalDonationsLabel;
    @FXML private Text passedScreeningLabel;
    @FXML private Text failedScreeningLabel;
    @FXML private Button addDonationBtn;

    private ServiceDonation serviceDonation;
    private List<Donations> allDonations;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        try {
            System.out.println("Initializing DonationsDashboardController...");
            serviceDonation = new ServiceDonation();

            // Initialize filter combo box
            if (statusFilter != null) {
                statusFilter.getItems().addAll("All Status", "COMPLETED", "CANCELLED");
                statusFilter.setValue("All Status");
                System.out.println("Status filter initialized");
            } else {
                System.out.println("ERROR: statusFilter is null");
            }

            loadDonations();
            System.out.println("Donations loaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception in initialize: " + e.getMessage());
            showAlert("Error", "Failed to initialize donations dashboard: " + e.getMessage());
        }
    }

    private void loadDonations() {
        try {
            System.out.println("Loading donations from database...");
            allDonations = serviceDonation.getAll();
            System.out.println("Donations retrieved: " + (allDonations != null ? allDonations.size() : "null"));

            if (allDonations == null) {
                allDonations = new java.util.ArrayList<>();
            }

            Users currentUser = AppSession.getCurrentUser();
            if (currentUser != null && currentUser.getUserType() == UserType.DONOR && currentUser.getId() != null) {
                allDonations = allDonations.stream()
                        .filter(d -> d.getDonorId() != null && d.getDonorId().equals(currentUser.getId()))
                        .collect(Collectors.toList());
            }

            displayDonations(allDonations);
            updateStats();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception in loadDonations: " + e.getMessage());
            showAlert("Error", "Failed to load donations: " + e.getMessage());
        }
    }

    private void displayDonations(List<Donations> donations) {
        donationsContainer.getChildren().clear();

        if (donations.isEmpty()) {
            Label emptyLabel = new Label("No donations found");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 40px;");
            donationsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Donations donation : donations) {
            donationsContainer.getChildren().add(createDonationCard(donation));
        }
    }

    private VBox createDonationCard(Donations donation) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card");
        card.setPadding(new Insets(20));

        // Header with Donation Info
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Region icon = new Region();
        icon.getStyleClass().add("user-avatar");
        icon.setPrefSize(50, 50);
        icon.setMinSize(50, 50);
        icon.setMaxSize(50, 50);

        // Donation Info
        VBox donationInfo = new VBox(5);
        HBox.setHgrow(donationInfo, Priority.ALWAYS);

        Text donationIdText = new Text("Donation ID: " + donation.getDonationId());
        donationIdText.getStyleClass().add("user-name");

        Text donorIdText = new Text("Donor: " + donation.getDonorId());
        donorIdText.getStyleClass().add("user-email");

        donationInfo.getChildren().addAll(donationIdText, donorIdText);

        // Status Badge
        Label statusBadge = new Label(donation.getStatus());
        statusBadge.getStyleClass().add("user-type-badge");
        statusBadge.getStyleClass().add("badge-" + donation.getStatus().toLowerCase());

        header.getChildren().addAll(icon, donationInfo, statusBadge);

        // Details Grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);

        // Hospital
        HBox hospitalBox = createDetailItem("\uD83C\uDFE5", donation.getHospitalId());
        detailsGrid.add(hospitalBox, 0, 0);

        // Units Collected
        HBox unitsBox = createDetailItem("\uD83E\uDE78", donation.getUnitsCollected() + " units");
        detailsGrid.add(unitsBox, 1, 0);

        // Donation Date
        HBox dateBox = createDetailItem("\uD83D\uDCC5", donation.getDonationDate().format(dateFormatter));
        detailsGrid.add(dateBox, 0, 1);

        // Screening Status
        String screeningStatus = donation.getScreeningPassed() != null && donation.getScreeningPassed() ? "✓ Passed" : "✗ Failed";
        HBox screeningBox = createDetailItem("✓", screeningStatus);
        detailsGrid.add(screeningBox, 1, 1);

        // Medical Notes
        if (donation.getMedicalNotes() != null && !donation.getMedicalNotes().isEmpty()) {
            HBox notesBox = createDetailItem("\uD83D\uDCDD", donation.getMedicalNotes());
            detailsGrid.add(notesBox, 0, 2, 2, 1);
        }

        // Action Buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button modifyBtn = new Button("Modify");
        modifyBtn.getStyleClass().add("btn-modify");
        modifyBtn.setOnAction(e -> handleModifyDonation(donation));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-delete");
        deleteBtn.setOnAction(e -> handleDeleteDonation(donation));

        actionButtons.getChildren().addAll(modifyBtn, deleteBtn);

        card.getChildren().addAll(header, new Separator(), detailsGrid, actionButtons);

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
        totalDonationsLabel.setText(String.valueOf(allDonations.size()));

        long passedCount = allDonations.stream()
                .filter(d -> d.getScreeningPassed() != null && d.getScreeningPassed())
                .count();
        passedScreeningLabel.setText(String.valueOf(passedCount));

        long failedCount = allDonations.stream()
                .filter(d -> d.getScreeningPassed() != null && !d.getScreeningPassed())
                .count();
        failedScreeningLabel.setText(String.valueOf(failedCount));
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedStatus = statusFilter.getValue();

        List<Donations> filtered = allDonations.stream()
                .filter(donation -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            donation.getDonationId().toLowerCase().contains(searchText) ||
                            donation.getDonorId().toLowerCase().contains(searchText) ||
                            donation.getHospitalId().toLowerCase().contains(searchText) ||
                            (donation.getMedicalNotes() != null && donation.getMedicalNotes().toLowerCase().contains(searchText));

                    boolean matchesStatus = selectedStatus.equals("All Status") ||
                            donation.getStatus().equalsIgnoreCase(selectedStatus);

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        displayDonations(filtered);
    }

    @FXML
    private void handleFilter() {
        handleSearch();
    }

    @FXML
    private void handleAddDonation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonationsDialog.fxml"));
            Parent root = loader.load();

            DonationsDialogController controller = loader.getController();
            controller.setMode(DonationsDialogController.Mode.ADD);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Donation");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadDonations();
            });

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open add donation dialog: " + e.getMessage());
        }
    }

    private void handleModifyDonation(Donations donation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonationsDialog.fxml"));
            Parent root = loader.load();

            DonationsDialogController controller = loader.getController();
            controller.setMode(DonationsDialogController.Mode.EDIT);
            controller.setDonation(donation);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Donation");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadDonations();
            });

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open edit donation dialog: " + e.getMessage());
        }
    }

    private void handleDeleteDonation(Donations donation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Donation");
        alert.setHeaderText("Delete Donation " + donation.getDonationId() + "?");
        alert.setContentText("This action cannot be undone. Are you sure you want to delete this donation?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceDonation.supprimer(donation.getDonationId());
            loadDonations();
            showAlert("Success", "Donation deleted successfully!");
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
