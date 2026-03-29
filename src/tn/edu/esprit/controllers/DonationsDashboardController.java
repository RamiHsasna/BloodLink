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
    @FXML private ComboBox<String> bloodTypeFilter;
    @FXML private VBox donationsContainer;
    @FXML private Text totalDonationsLabel;
    @FXML private Text passedScreeningLabel;
    @FXML private Text failedScreeningLabel;
    @FXML private Button addDonationBtn;

    private ServiceDonation serviceDonation;
    private List<Donations> allDonations;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private boolean readOnlyDonor;

    @FXML
    public void initialize() {
        try {
            System.out.println("Initializing DonationsDashboardController...");
            serviceDonation = new ServiceDonation();
            Users currentUser = AppSession.getCurrentUser();
            readOnlyDonor = currentUser != null && currentUser.getUserType() == UserType.DONOR;

            if (addDonationBtn != null) {
                addDonationBtn.setVisible(!readOnlyDonor);
                addDonationBtn.setManaged(!readOnlyDonor);
            }

            if (statusFilter != null) {
                statusFilter.getItems().addAll("All Status", "COMPLETED", "CANCELLED");
                statusFilter.setValue("All Status");
                System.out.println("Status filter initialized");

                if (bloodTypeFilter != null) {
                    bloodTypeFilter.getItems().addAll(
                            "All Blood Types", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
                    );
                    bloodTypeFilter.setValue("All Blood Types");
                    bloodTypeFilter.setOnAction(e -> handleFilter());
                }
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

        // ── EXPIRY BANNER ──
        List<Donations> expired = donations.stream()
                .filter(this::isExpired)
                .collect(Collectors.toList());

        if (!expired.isEmpty()) {
            VBox banner = new VBox(6);
            banner.setStyle("-fx-background-color: #fce4e4; -fx-background-radius: 10; " +
                    "-fx-border-color: #e53935; -fx-border-radius: 10; " +
                    "-fx-border-width: 1.5; -fx-padding: 14;");

            Label bannerTitle = new Label("⚠️ Expired Donations — " + expired.size() + " donation(s) older than 42 days!");
            bannerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c62828;");

            VBox bannerList = new VBox(4);
            for (Donations d : expired) {
                Label item = new Label("• Donation ID: " + d.getDonationId() +
                        " — donated on " + d.getDonationDate().format(dateFormatter));
                item.setStyle("-fx-font-size: 13px; -fx-text-fill: #b71c1c;");
                bannerList.getChildren().add(item);
            }

            banner.getChildren().addAll(bannerTitle, bannerList);
            donationsContainer.getChildren().add(banner);
        }

        // ── DONATION CARDS ──
        for (Donations donation : donations) {
            donationsContainer.getChildren().add(createDonationCard(donation));
        }
    }

    private VBox createDonationCard(Donations donation) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card");
        card.setPadding(new Insets(20));

        if (isExpired(donation)) {
            card.setStyle("-fx-border-color: #e53935; -fx-border-width: 2; " +
                    "-fx-border-radius: 10; -fx-background-radius: 10;");

            Label expiryWarning = new Label("⚠️ This donation has expired — collected more than 42 days ago!");
            expiryWarning.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #c62828; -fx-background-color: #fce4e4; " +
                    "-fx-background-radius: 6; -fx-padding: 6 10 6 10;");
            expiryWarning.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(expiryWarning);
        }

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Region icon = new Region();
        icon.getStyleClass().add("user-avatar");
        icon.setPrefSize(50, 50);
        icon.setMinSize(50, 50);
        icon.setMaxSize(50, 50);

        VBox donationInfo = new VBox(5);
        HBox.setHgrow(donationInfo, Priority.ALWAYS);

        Text donationIdText = new Text("Donation ID: " + donation.getDonationId());
        donationIdText.getStyleClass().add("user-name");

        Text donorIdText = new Text("Donor: " + donation.getDonorId());
        donorIdText.getStyleClass().add("user-email");

        Label donorBadge = getDonorBadge(donation.getDonorId());
        donationInfo.getChildren().addAll(donationIdText, donorIdText, donorBadge);

        Label statusBadge = new Label(donation.getStatus());
        statusBadge.getStyleClass().add("user-type-badge");
        statusBadge.getStyleClass().add("badge-" + donation.getStatus().toLowerCase());

        header.getChildren().addAll(icon, donationInfo, statusBadge);

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);

        HBox hospitalBox = createDetailItem("\uD83C\uDFE5", donation.getHospitalId());
        detailsGrid.add(hospitalBox, 0, 0);

        HBox unitsBox = createDetailItem("\uD83E\uDE78", donation.getUnitsCollected() + " units");
        detailsGrid.add(unitsBox, 1, 0);

        HBox dateBox = createDetailItem("\uD83D\uDCC5", donation.getDonationDate().format(dateFormatter));
        detailsGrid.add(dateBox, 0, 1);

        String screeningStatus = donation.getScreeningPassed() != null && donation.getScreeningPassed() ? "✓ Passed" : "✗ Failed";
        HBox screeningBox = createDetailItem("✓", screeningStatus);
        detailsGrid.add(screeningBox, 1, 1);

        if (donation.getMedicalNotes() != null && !donation.getMedicalNotes().isEmpty()) {
            HBox notesBox = createDetailItem("\uD83D\uDCDD", donation.getMedicalNotes());
            detailsGrid.add(notesBox, 0, 2, 2, 1);
        }

        card.getChildren().addAll(header, new Separator(), detailsGrid);

        // ── Action buttons — always show QR, hide modify/delete for donors ──
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button pdfBtn = new Button("📄 Export PDF");
        pdfBtn.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 6 14 6 14; -fx-cursor: hand;");
        pdfBtn.setOnAction(e -> handleExportPDF(donation));
        actionButtons.getChildren().add(pdfBtn);

        Button timelineBtn = new Button("📅 Timeline");
        timelineBtn.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 6 14 6 14; -fx-cursor: hand;");
        timelineBtn.setOnAction(e -> handleShowTimeline(donation.getDonorId()));
        actionButtons.getChildren().add(timelineBtn);

        Button qrBtn = new Button("🔳 QR Code");
        qrBtn.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 6 14 6 14; -fx-cursor: hand;");
        qrBtn.setOnAction(e -> handleShowQR(donation));
        actionButtons.getChildren().add(qrBtn);

        if (!readOnlyDonor) {
            Button modifyBtn = new Button("Modify");
            modifyBtn.getStyleClass().add("btn-modify");
            modifyBtn.setOnAction(e -> handleModifyDonation(donation));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("btn-delete");
            deleteBtn.setOnAction(e -> handleDeleteDonation(donation));

            actionButtons.getChildren().addAll(modifyBtn, deleteBtn);
        }

        card.getChildren().add(actionButtons);
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
        String selectedBloodType = bloodTypeFilter != null ? bloodTypeFilter.getValue() : "All Blood Types";

        List<Donations> filtered = allDonations.stream()
                .filter(donation -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            donation.getDonationId().toLowerCase().contains(searchText) ||
                            donation.getDonorId().toLowerCase().contains(searchText) ||
                            donation.getHospitalId().toLowerCase().contains(searchText) ||
                            (donation.getMedicalNotes() != null &&
                                    donation.getMedicalNotes().toLowerCase().contains(searchText));

                    boolean matchesStatus = selectedStatus.equals("All Status") ||
                            donation.getStatus().equalsIgnoreCase(selectedStatus);

                    boolean matchesBloodType = "All Blood Types".equals(selectedBloodType) ||
                            (donation.getBloodTypeId() != null &&
                                    donation.getBloodTypeId().equalsIgnoreCase(selectedBloodType));

                    return matchesSearch && matchesStatus && matchesBloodType;
                })
                .collect(Collectors.toList());

        displayDonations(filtered);
    }

    @FXML
    private void handleFilter() {
        handleSearch();
    }

    @FXML
    private void handleViewChart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/tn/edu/esprit/views/DonationsChart.fxml"));
            Parent root = loader.load();

            Stage chartStage = new Stage();
            chartStage.setTitle("Donations Chart");
            chartStage.initModality(Modality.APPLICATION_MODAL);
            chartStage.setScene(new Scene(root));
            chartStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());
            chartStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open chart: " + e.getMessage());
        }
    }
    @FXML
    private void handleAIPrediction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/tn/edu/esprit/views/AIPrediction.fxml"));
            Parent root = loader.load();

            Stage aiStage = new Stage();
            aiStage.setTitle("🤖 AI Blood Demand Prediction");
            aiStage.initModality(Modality.APPLICATION_MODAL);
            aiStage.setScene(new Scene(root));
            aiStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());
            aiStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open AI prediction: " + e.getMessage());
        }
    }
    @FXML
    private void handleAddDonation() {
        if (readOnlyDonor) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/tn/edu/esprit/views/DonationsDialog.fxml"));
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
            controller.setOnSave(this::loadDonations);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open add donation dialog: " + e.getMessage());
        }
    }

    private void handleModifyDonation(Donations donation) {
        if (readOnlyDonor) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/tn/edu/esprit/views/DonationsDialog.fxml"));
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
            controller.setOnSave(this::loadDonations);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open edit donation dialog: " + e.getMessage());
        }
    }

    private void handleDeleteDonation(Donations donation) {
        if (readOnlyDonor) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Donation");
        alert.setHeaderText("Delete Donation " + donation.getDonationId() + "?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceDonation.supprimer(donation.getDonationId());
            loadDonations();
            showAlert("Success", "Donation deleted successfully!");
        }
    }

    private void handleShowQR(Donations donation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/tn/edu/esprit/views/DonationQR.fxml"));
            Parent root = loader.load();

            DonationQRController controller = loader.getController();
            controller.setDonation(donation);

            Stage qrStage = new Stage();
            qrStage.setTitle("Donation QR Code");
            qrStage.initModality(Modality.APPLICATION_MODAL);
            qrStage.setScene(new Scene(root));
            qrStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css")
                            .toExternalForm());
            qrStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open QR code: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private Label getDonorBadge(String donorId) {
        long count = allDonations.stream()
                .filter(d -> donorId != null && donorId.equals(d.getDonorId()))
                .count();

        Label badge = new Label();
        badge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-radius: 20; -fx-padding: 4 10 4 10;");

        if (count >= 5) {
            badge.setText("⭐ Champion");
            badge.setStyle(badge.getStyle() +
                    "-fx-background-color: #fff8e1; -fx-text-fill: #f57f17;");
        } else if (count >= 3) {
            badge.setText("🔥 Regular Donor");
            badge.setStyle(badge.getStyle() +
                    "-fx-background-color: #fce4e4; -fx-text-fill: #c62828;");
        } else {
            badge.setText("🥇 First Timer");
            badge.setStyle(badge.getStyle() +
                    "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;");
        }
        return badge;
    }
    private void handleShowTimeline(String donorId) {
        List<Donations> donorDonations = allDonations.stream()
                .filter(d -> donorId != null && donorId.equals(d.getDonorId()))
                .sorted((a, b) -> a.getDonationDate().compareTo(b.getDonationDate()))
                .collect(Collectors.toList());

        Stage timelineStage = new Stage();
        timelineStage.setTitle("📅 Donation History — Donor: " + donorId);
        timelineStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f0f4f8; -fx-padding: 24;");

        Label title = new Label("📅 Donation History");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a2535;");

        Label subtitle = new Label("Donor ID: " + donorId + " — " + donorDonations.size() + " total donations");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7c93; -fx-padding: 0 0 16 0;");

        VBox timelineBox = new VBox(0);
        timelineBox.setStyle("-fx-padding: 8 0 0 0;");

        for (int i = 0; i < donorDonations.size(); i++) {
            Donations d = donorDonations.get(i);
            boolean isLast = (i == donorDonations.size() - 1);

            HBox row = new HBox(0);
            row.setAlignment(Pos.TOP_LEFT);

            // ── Left side: dot + line ──
            VBox dotLine = new VBox(0);
            dotLine.setAlignment(Pos.TOP_CENTER);
            dotLine.setPrefWidth(40);

            Label dot = new Label("●");
            dot.setStyle("-fx-font-size: 18px; -fx-text-fill: #e53935;");

            if (!isLast) {
                Label line = new Label("");
                line.setPrefHeight(60);
                line.setPrefWidth(2);
                line.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 0;");
                dotLine.getChildren().addAll(dot, line);
            } else {
                dotLine.getChildren().add(dot);
            }

            // ── Right side: card ──
            VBox cardContent = new VBox(6);
            cardContent.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-padding: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
            HBox.setHgrow(cardContent, Priority.ALWAYS);
            cardContent.setMaxWidth(Double.MAX_VALUE);

            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label dateLabel = new Label("📅 " + d.getDonationDate().format(dateFormatter));
            dateLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a2535;");
            HBox.setHgrow(dateLabel, Priority.ALWAYS);

            String statusColor = "COMPLETED".equalsIgnoreCase(d.getStatus()) ? "#2e7d32" : "#f57f17";
            String statusBg = "COMPLETED".equalsIgnoreCase(d.getStatus()) ? "#e8f5e9" : "#fff8e1";
            Label statusLbl = new Label(d.getStatus());
            statusLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-background-color: " + statusBg + "; -fx-text-fill: " + statusColor + "; " +
                    "-fx-background-radius: 20; -fx-padding: 3 8 3 8;");

            topRow.getChildren().addAll(dateLabel, statusLbl);

            Label detailsLabel = new Label(
                    "🩸 Blood Type: " + (d.getBloodTypeId() != null ? d.getBloodTypeId() : "N/A") +
                            "   🧪 Units: " + d.getUnitsCollected() +
                            "   ✔️ Screening: " + (Boolean.TRUE.equals(d.getScreeningPassed()) ? "Passed" : "Failed"));
            detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a6278;");

            cardContent.getChildren().addAll(topRow, detailsLabel);

            if (d.getMedicalNotes() != null && !d.getMedicalNotes().isEmpty()) {
                Label notesLabel = new Label("📝 " + d.getMedicalNotes());
                notesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9aacbe; -fx-wrap-text: true;");
                notesLabel.setWrapText(true);
                cardContent.getChildren().add(notesLabel);
            }

            VBox cardWrapper = new VBox();
            cardWrapper.setPadding(new Insets(0, 0, 10, 12));
            HBox.setHgrow(cardWrapper, Priority.ALWAYS);
            cardWrapper.getChildren().add(cardContent);

            row.getChildren().addAll(dotLine, cardWrapper);
            timelineBox.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(timelineBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 10 24 10 24; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> timelineStage.close());

        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(16, 0, 0, 0));

        root.getChildren().addAll(title, subtitle, scroll, btnRow);

        Scene scene = new Scene(root, 550, 500);
        timelineStage.setScene(scene);
        timelineStage.showAndWait();
    }
    private boolean isExpired(Donations donation) {
        if (donation.getDonationDate() == null) return false;
        return donation.getDonationDate().isBefore(
                java.time.LocalDateTime.now().minusDays(42));
    }
    private void handleExportPDF(Donations donation) {
        try {
            // Ask user where to save the file
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save PDF");
            fileChooser.setInitialFileName("donation_" + donation.getDonationId() + ".pdf");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            java.io.File file = fileChooser.showSaveDialog(donationsContainer.getScene().getWindow());

            if (file == null) return;

            // Create PDF
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
            document.open();

            // ── Title ──
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 22,
                    com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(229, 57, 53));
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("🩸 BloodLink — Donation Record", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // ── Separator ──
            com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
            line.setLineColor(new com.itextpdf.text.BaseColor(229, 57, 53));
            document.add(new com.itextpdf.text.Chunk(line));
            document.add(com.itextpdf.text.Chunk.NEWLINE);

            // ── Details ──
            com.itextpdf.text.Font labelFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 11,
                    com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(74, 98, 120));
            com.itextpdf.text.Font valueFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 11,
                    com.itextpdf.text.Font.NORMAL, new com.itextpdf.text.BaseColor(26, 37, 53));

            // Table
            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(16);
            table.setWidths(new float[]{1f, 2f});

            String[][] rows = {
                    {"Donation ID",    donation.getDonationId()},
                    {"Donor ID",       donation.getDonorId()},
                    {"Hospital ID",    donation.getHospitalId()},
                    {"Blood Type",     donation.getBloodTypeId() != null ? donation.getBloodTypeId() : "N/A"},
                    {"Donation Date",  donation.getDonationDate() != null ? donation.getDonationDate().format(dateFormatter) : "N/A"},
                    {"Units Collected", String.valueOf(donation.getUnitsCollected())},
                    {"Volume (mL)",    donation.getVolumeCollected() != null ? donation.getVolumeCollected().toString() : "N/A"},
                    {"Status",         donation.getStatus()},
                    {"Screening",      Boolean.TRUE.equals(donation.getScreeningPassed()) ? "Passed" : "Failed"},
                    {"Medical Notes",  donation.getMedicalNotes() != null ? donation.getMedicalNotes() : "None"}
            };

            for (String[] row : rows) {
                com.itextpdf.text.pdf.PdfPCell labelCell = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(row[0], labelFont));
                labelCell.setBackgroundColor(new com.itextpdf.text.BaseColor(240, 244, 248));
                labelCell.setPadding(8);
                labelCell.setBorderColor(new com.itextpdf.text.BaseColor(221, 227, 236));

                com.itextpdf.text.pdf.PdfPCell valueCell = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(row[1], valueFont));
                valueCell.setPadding(8);
                valueCell.setBorderColor(new com.itextpdf.text.BaseColor(221, 227, 236));

                table.addCell(labelCell);
                table.addCell(valueCell);
            }
            document.add(table);

            // ── Footer ──
            com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 9,
                    com.itextpdf.text.Font.ITALIC, new com.itextpdf.text.BaseColor(154, 172, 190));
            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                    "Generated by BloodLink on " + java.time.LocalDateTime.now().format(dateFormatter),
                    footerFont);
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);

            document.close();
            showAlert("Success", "PDF saved successfully to:\n" + file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not export PDF: " + e.getMessage());
        }
    }
}