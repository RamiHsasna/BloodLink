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
import tn.edu.esprit.entities.DonationsEvent;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.ServiceDonationsEvent;
import tn.edu.esprit.services.SessionScopeService;

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
    private SessionScopeService sessionScopeService;
    private List<DonationsEvent> allEvents;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private boolean readOnlyDonor;

    @FXML
    public void initialize() {
        try {
            System.out.println("Initializing DonationEventDashboardController...");
        serviceDonationEvent = new ServiceDonationsEvent();
        sessionScopeService = new SessionScopeService();
            Users currentUser = AppSession.getCurrentUser();
            readOnlyDonor = currentUser != null && currentUser.getUserType() == UserType.DONOR;

            if (addEventBtn != null) {
                addEventBtn.setVisible(!readOnlyDonor);
                addEventBtn.setManaged(!readOnlyDonor);
            }

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
            allEvents = sessionScopeService.filterVisibleDonationEvents(allEvents);
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

        // ── UPCOMING BANNER ──
        List<DonationsEvent> upcoming = events.stream()
                .filter(this::isUpcomingSoon)
                .collect(Collectors.toList());

        if (!upcoming.isEmpty()) {
            VBox banner = new VBox(6);
            banner.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 10; " +
                    "-fx-border-color: #f57f17; -fx-border-radius: 10; " +
                    "-fx-border-width: 1.5; -fx-padding: 14;");

            Label bannerTitle = new Label("🔔 Upcoming Events in the Next 7 Days");
            bannerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e65100;");

            VBox bannerList = new VBox(4);
            for (DonationsEvent e : upcoming) {
                Label item = new Label("• " + e.getName() + " — starts " +
                        e.getStartDate().format(dateFormatter));
                item.setStyle("-fx-font-size: 13px; -fx-text-fill: #bf360c;");
                bannerList.getChildren().add(item);
            }

            banner.getChildren().addAll(bannerTitle, bannerList);
            eventsContainer.getChildren().add(banner);
        }

        // ── EVENT CARDS ──
        for (DonationsEvent event : events) {
            eventsContainer.getChildren().add(createEventCard(event));
        }
    }

    private VBox createEventCard(DonationsEvent event) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card");
        card.setPadding(new Insets(20));

        if (isUpcomingSoon(event)) {
            card.setStyle("-fx-border-color: #f57f17; -fx-border-width: 2; " +
                    "-fx-border-radius: 10; -fx-background-radius: 10;");
        }

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
        HBox locationBox = createDetailItem("\uD83D\uDCCD", event.getLocation() != null ? event.getLocation() : "N/A");
        detailsGrid.add(locationBox, 0, 0);

        // Target Collections
        HBox targetBox = createDetailItem("\uD83E\uDE78", event.getTargetCollectionUnits() + " units target");
        detailsGrid.add(targetBox, 1, 0);

        // Start Date
        HBox startBox = createDetailItem("\uD83D\uDCC5", "Starts: " + event.getStartDate().format(dateFormatter));
        detailsGrid.add(startBox, 0, 1);

        // End Date
        HBox endBox = createDetailItem("\uD83D\uDCC5", "Ends: " + event.getEndDate().format(dateFormatter));
        detailsGrid.add(endBox, 1, 1);

        // Target Blood Types
        if (event.getTargetBloodTypes() != null && !event.getTargetBloodTypes().isEmpty()) {
            HBox bloodTypesBox = createDetailItem("\uD83D\uDD2C", "Target: " + event.getTargetBloodTypes());
            detailsGrid.add(bloodTypesBox, 0, 2, 2, 1);
        }

        // Action Buttons
        card.getChildren().addAll(header, new Separator(), detailsGrid);

// ── PROGRESS BAR ──
        if (event.getTargetCollectionUnits() != null && event.getTargetCollectionUnits() > 0) {
            int target = event.getTargetCollectionUnits();
            int actual = event.getActualCollectionUnits() != null ? event.getActualCollectionUnits() : 0;
            double progress = Math.min((double) actual / target, 1.0);
            int percent = (int)(progress * 100);

            VBox progressBox = new VBox(6);
            progressBox.setPadding(new Insets(4, 0, 0, 0));

            HBox progressHeader = new HBox();
            progressHeader.setAlignment(Pos.CENTER_LEFT);
            Label progressLabel = new Label("🩸 Collection Progress");
            progressLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4a6278;");
            Label progressPercent = new Label(actual + " / " + target + " units (" + percent + "%)");
            progressPercent.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a6278;");
            HBox.setHgrow(progressLabel, Priority.ALWAYS);
            progressHeader.getChildren().addAll(progressLabel, progressPercent);

            ProgressBar progressBar = new ProgressBar(progress);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(12);
            if (percent >= 100) {
                progressBar.setStyle("-fx-accent: #2e7d32;");
            } else if (percent >= 60) {
                progressBar.setStyle("-fx-accent: #f57f17;");
            } else {
                progressBar.setStyle("-fx-accent: #e53935;");
            }

            progressBox.getChildren().addAll(progressHeader, progressBar);
            card.getChildren().add(progressBox);
        }

        if (!readOnlyDonor) {
            HBox actionButtons = new HBox(10);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);

            Button mapBtn = new Button("📍 View Map");
            mapBtn.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; " +
                    "-fx-font-weight: bold; -fx-background-radius: 8; " +
                    "-fx-padding: 6 14 6 14; -fx-cursor: hand;");
            mapBtn.setOnAction(e -> handleShowMap(event));
            actionButtons.getChildren().add(mapBtn);

            Button modifyBtn = new Button("Modify");
            modifyBtn.getStyleClass().add("btn-modify");
            modifyBtn.setOnAction(e -> handleModifyEvent(event));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("btn-delete");
            deleteBtn.setOnAction(e -> handleDeleteEvent(event));

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
        if (readOnlyDonor) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonationEventDialog.fxml"));
            Parent root = loader.load();

            DonationEventDialogController controller = loader.getController();
            controller.setMode(DonationEventDialogController.Mode.ADD);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Donation Event");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

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
        if (readOnlyDonor) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonationEventDialog.fxml"));
            Parent root = loader.load();

            DonationEventDialogController controller = loader.getController();
            controller.setMode(DonationEventDialogController.Mode.EDIT);
            controller.setEvent(event);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Donation Event");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

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
        if (readOnlyDonor) {
            return;
        }
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
    private boolean isUpcomingSoon(DonationsEvent event) {
        if (event.getStartDate() == null) return false;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime in7Days = now.plusDays(7);
        return event.getStartDate().isAfter(now) && event.getStartDate().isBefore(in7Days);
    }
    private void handleShowMap(DonationsEvent event) {
        if (event.getLatitude() == null || event.getLongitude() == null) {
            showAlert("No Location", "This event has no coordinates saved!");
            return;
        }

        Stage mapStage = new Stage();
        mapStage.setTitle("📍 " + event.getName() + " — Location");
        mapStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f0f4f8;");

        // ── Header ──
        VBox header = new VBox(4);
        header.setStyle("-fx-background-color: #1a2535; -fx-padding: 16 20 16 20;");

        Label titleLabel = new Label("📍 " + event.getName());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label coordsLabel = new Label("Lat: " + event.getLatitude() + "   Lng: " + event.getLongitude());
        coordsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8a9bb0;");

        Label locationLabel = new Label("📍 " + (event.getLocation() != null ? event.getLocation() : "N/A"));
        locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8a9bb0;");

        header.getChildren().addAll(titleLabel, locationLabel, coordsLabel);

        // ── Map ──
        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        webView.setPrefSize(620, 450);

        String lat = event.getLatitude().toString();
        String lng = event.getLongitude().toString();

        String html = "<!DOCTYPE html><html><head>"
                + "<style>body,html{margin:0;padding:0;width:100%;height:100%;}</style>"
                + "</head><body>"
                + "<iframe width='100%' height='100%' frameborder='0' style='border:0' "
                + "src='https://maps.google.com/maps?q=" + lat + "," + lng
                + "&z=15&output=embed' allowfullscreen>"
                + "</iframe>"
                + "</body></html>";

        webView.getEngine().loadContent(html);
        VBox.setVgrow(webView, Priority.ALWAYS);

        // ── Footer buttons ──
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-padding: 12 16 12 16; -fx-background-color: #f0f4f8;");

        Button openBrowserBtn = new Button("🌐 Open in Browser");
        openBrowserBtn.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 8 16 8 16; -fx-cursor: hand;");
        openBrowserBtn.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                        new java.net.URI("https://maps.google.com/?q=" + lat + "," + lng));
            } catch (Exception ex) {
                showAlert("Error", "Could not open browser: " + ex.getMessage());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 8 16 8 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> mapStage.close());

        btnRow.getChildren().addAll(openBrowserBtn, closeBtn);

        root.getChildren().addAll(header, webView, btnRow);

        Scene scene = new Scene(root, 640, 540);
        mapStage.setScene(scene);
        mapStage.showAndWait();
    }
}
