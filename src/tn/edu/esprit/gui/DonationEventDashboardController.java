package tn.edu.esprit.gui;

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
import tn.edu.esprit.entities.DonationsEvent;
import tn.edu.esprit.services.ServiceDonationsEvent;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DonationEventDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private VBox eventsContainer;
    @FXML private Text totalEventsLabel;
    @FXML private Text activeEventsLabel;
    @FXML private Text completedEventsLabel;
    @FXML private Button addEventBtn;

    private ServiceDonationsEvent serviceDonationEvent;
    private List<DonationsEvent> allEvents;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        try {
            System.out.println("Initializing DonationEventDashboardController...");
            serviceDonationEvent = new ServiceDonationsEvent();
            
            // Initialize filter combo box
            if (statusFilter != null) {
                statusFilter.getItems().addAll("All Status", "PLANNED", "ACTIVE", "COMPLETED", "CANCELLED");
                statusFilter.setValue("All Status");
                System.out.println("Status filter initialized");
            } else {
                System.out.println("ERROR: statusFilter is null");
            }
            
            loadEvents();
            System.out.println("Events loaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception in initialize: " + e.getMessage());
            showAlert("Error", "Failed to initialize donation events dashboard: " + e.getMessage());
        }
    }

    private void loadEvents() {
        try {
            System.out.println("Loading donation events from database...");
            DonationsEvent dummy = new DonationsEvent();
            allEvents = serviceDonationEvent.getAll(dummy);
            System.out.println("Events retrieved: " + (allEvents != null ? allEvents.size() : "null"));
            
            if (allEvents == null) {
                allEvents = new java.util.ArrayList<>();
            }
            displayEvents(allEvents);
            updateStats();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception in loadEvents: " + e.getMessage());
            showAlert("Error", "Failed to load events: " + e.getMessage());
        }
    }

    private void displayEvents(List<DonationsEvent> events) {
        eventsContainer.getChildren().clear();
        
        if (events.isEmpty()) {
            Label emptyLabel = new Label("No donation events found");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 40px;");
            eventsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (DonationsEvent event : events) {
            eventsContainer.getChildren().add(createEventCard(event));
        }
    }

    private VBox createEventCard(DonationsEvent event) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card");
        card.setPadding(new Insets(20));

        // Header with Event Info
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Region icon = new Region();
        icon.getStyleClass().add("user-avatar");
        icon.setPrefSize(50, 50);
        icon.setMinSize(50, 50);
        icon.setMaxSize(50, 50);

        // Event Info
        VBox eventInfo = new VBox(5);
        HBox.setHgrow(eventInfo, Priority.ALWAYS);

        Text eventNameText = new Text(event.getName());
        eventNameText.getStyleClass().add("user-name");

        Text hospitalText = new Text("Hospital: " + event.getHospitalId());
        hospitalText.getStyleClass().add("user-email");

        eventInfo.getChildren().addAll(eventNameText, hospitalText);

        // Status Badge
        Label statusBadge = new Label(event.getStatus());
        statusBadge.getStyleClass().add("user-type-badge");
        statusBadge.getStyleClass().add("badge-" + event.getStatus().toLowerCase());

        header.getChildren().addAll(icon, eventInfo, statusBadge);

        // Details Grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);

        // Location
        HBox locationBox = createDetailItem("📍", event.getLocation() != null ? event.getLocation() : "N/A");
        detailsGrid.add(locationBox, 0, 0);

        // Target Collections
        HBox targetBox = createDetailItem("🩸", event.getTargetCollectionUnits() + " units target");
        detailsGrid.add(targetBox, 1, 0);

        // Start Date
        HBox startBox = createDetailItem("📅", "Starts: " + event.getStartDate().format(dateFormatter));
        detailsGrid.add(startBox, 0, 1);

        // End Date
        HBox endBox = createDetailItem("📅", "Ends: " + event.getEndDate().format(dateFormatter));
        detailsGrid.add(endBox, 1, 1);

        // Target Blood Types
        if (event.getTargetBloodTypes() != null && !event.getTargetBloodTypes().isEmpty()) {
            HBox bloodTypesBox = createDetailItem("🔬", "Target: " + event.getTargetBloodTypes());
            detailsGrid.add(bloodTypesBox, 0, 2, 2, 1);
        }

        // Action Buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button modifyBtn = new Button("Modify");
        modifyBtn.getStyleClass().add("btn-modify");
        modifyBtn.setOnAction(e -> handleModifyEvent(event));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-delete");
        deleteBtn.setOnAction(e -> handleDeleteEvent(event));

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
        totalEventsLabel.setText(String.valueOf(allEvents.size()));
        
        long activeCount = allEvents.stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()) || "PLANNED".equalsIgnoreCase(e.getStatus()))
                .count();
        activeEventsLabel.setText(String.valueOf(activeCount));
        
        long completedCount = allEvents.stream()
                .filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus()))
                .count();
        completedEventsLabel.setText(String.valueOf(completedCount));
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedStatus = statusFilter.getValue();

        List<DonationsEvent> filtered = allEvents.stream()
                .filter(event -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            event.getName().toLowerCase().contains(searchText) ||
                            event.getHospitalId().toLowerCase().contains(searchText) ||
                            (event.getLocation() != null && event.getLocation().toLowerCase().contains(searchText)) ||
                            (event.getTargetBloodTypes() != null && event.getTargetBloodTypes().toLowerCase().contains(searchText));

                    boolean matchesStatus = selectedStatus.equals("All Status") ||
                            event.getStatus().equalsIgnoreCase(selectedStatus);

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        displayEvents(filtered);
    }

    @FXML
    private void handleFilter() {
        handleSearch();
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("DonationEventDialog.fxml"));
            Parent root = loader.load();
            
            DonationEventDialogController controller = loader.getController();
            controller.setMode(DonationEventDialogController.Mode.ADD);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Donation Event");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            
            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadEvents();
            });
            
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open add event dialog: " + e.getMessage());
        }
    }

    private void handleModifyEvent(DonationsEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("DonationEventDialog.fxml"));
            Parent root = loader.load();
            
            DonationEventDialogController controller = loader.getController();
            controller.setMode(DonationEventDialogController.Mode.EDIT);
            controller.setEvent(event);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Donation Event");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            
            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadEvents();
            });
            
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open edit event dialog: " + e.getMessage());
        }
    }

    private void handleDeleteEvent(DonationsEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Event");
        alert.setHeaderText("Delete Event \"" + event.getName() + "\"?");
        alert.setContentText("This action cannot be undone. Are you sure you want to delete this event?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceDonationEvent.supprimer(event.getEventId());
                loadEvents();
                showAlert("Success", "Event deleted successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete event: " + e.getMessage());
            }
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
