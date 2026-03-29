
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
import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.DonorEligibilityService;
import tn.edu.esprit.services.ServiceDonation;
import tn.edu.esprit.services.ServiceDonor;
import tn.edu.esprit.services.SessionScopeService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DonorEligibilityDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> eligibilityFilter;
    @FXML private HBox searchFilterBar;
    @FXML private HBox statsBar;
    @FXML private VBox donorResponsesCard;
    @FXML private Button donorFillFormBtn;
    @FXML private Text donorHeaderSubtitle;
    @FXML private Text donorLastCheckDaysValue;
    @FXML private Text donorLastCheckDateValue;
    @FXML private Text donorDaysSinceDonationValue;
    @FXML private Text donorDonationGapHintValue;
    @FXML private Text donorTotalDonationsValue;
    @FXML private Text donorLastStatusDateText;
    @FXML private Label donorCurrentStatusBadge;
    @FXML private Label donorStatusNote;
    @FXML private VBox eligibilityContainer;
    @FXML private ScrollPane recordsScrollPane;
    @FXML private Text totalRecordsLabel;
    @FXML private Text eligibleCountLabel;
    @FXML private Text notEligibleCountLabel;
    @FXML private Button addEligibilityBtn;

    private DonorEligibilityService eligibilityService;
    private ServiceDonor donorService;
    private List<DonorEligibility> allRecords;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter prettyDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private boolean readOnlyDonor;
    private String currentUserId;
    private SessionScopeService sessionScopeService;
    private Set<String> allowedDonorIds = Set.of();

    @FXML
    public void initialize() {
        eligibilityService = new DonorEligibilityService();
        donorService = new ServiceDonor();
        sessionScopeService = new SessionScopeService();
        Users currentUser = AppSession.getCurrentUser();
        readOnlyDonor = currentUser != null && currentUser.getUserType() == UserType.DONOR;
        currentUserId = currentUser != null ? currentUser.getId() : null;
        allowedDonorIds = resolveAllowedDonorIds();

//hide admin features if donor
        if (addEligibilityBtn != null) {
            addEligibilityBtn.setVisible(!readOnlyDonor);
            addEligibilityBtn.setManaged(!readOnlyDonor);
        }

        if (searchFilterBar != null) {
            searchFilterBar.setVisible(!readOnlyDonor);
            searchFilterBar.setManaged(!readOnlyDonor);
        }
        if (donorResponsesCard != null) {
            donorResponsesCard.setVisible(readOnlyDonor);
            donorResponsesCard.setManaged(readOnlyDonor);
        }
        if (statsBar != null) {
            statsBar.setVisible(!readOnlyDonor);
            statsBar.setManaged(!readOnlyDonor);
        }
        if (recordsScrollPane != null) {
            recordsScrollPane.setVisible(!readOnlyDonor);
            recordsScrollPane.setManaged(!readOnlyDonor);
        }

        // Initialize filter combo box
        eligibilityFilter.getItems().addAll("All Eligibility", "Eligible", "Not Eligible");
        eligibilityFilter.setValue("All Eligibility");

        loadRecords();
    }

//show only the donor's own record, and all for the admin 
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
            if (sessionScopeService.isHospitalStaff()) {
                allRecords = allRecords
                    .stream()
                    .filter(record ->
                        record != null &&
                        record.getId() != null &&
                        allowedDonorIds.contains(record.getId())
                    )
                    .collect(Collectors.toList());
            }
        }
        displayRecords(allRecords);
        updateStats();
        updateDonorResponsesPanel();
    }

    private void updateDonorResponsesPanel() {
        if (!readOnlyDonor || donorCurrentStatusBadge == null) {
            return;
        }

        Donor donor = getCurrentDonor();
        populateDonationStatCards(donor);

//elligibility status 
        if (allRecords == null || allRecords.isEmpty()) {
            applyStatusBadge("Not checked", "donor-elig-badge-neutral");
            donorLastStatusDateText.setText("Last checked on N/A");
            donorLastCheckDaysValue.setText("N/A");
            donorLastCheckDateValue.setText("ago • date unavailable");
            donorStatusNote.setText("Run a pre-screening check to get your latest status. Age and weight are captured during the form.");
            return;
        }

        DonorEligibility ownRecord = allRecords.get(0);
        updateLastCheckCard(ownRecord);

        boolean isEligible = ownRecord.getIsCurrentlyEligible() != null && ownRecord.getIsCurrentlyEligible();
        String statusText = isEligible ? "Likely eligible" : "Not eligible";
        applyStatusBadge(statusText, isEligible ? "donor-elig-badge-good" : "donor-elig-badge-bad");

        if (ownRecord.getLastCalculatedAt() != null) {
            donorLastStatusDateText.setText("Last checked on " + ownRecord.getLastCalculatedAt().format(prettyDateFormatter));
        } else {
            donorLastStatusDateText.setText("Last checked on N/A");
        }

        String details = ownRecord.getEligibilityDetails();
        String age = extractFieldValue(details, "Age:");
        String weight = extractFieldValue(details, "Weight:");

        String bloodType = ownRecord.getBloodTypeCache() != null && !ownRecord.getBloodTypeCache().trim().isEmpty()
                ? ownRecord.getBloodTypeCache()
                : "N/A";

        String note;
        if (isEligible) {
            note = "This is a preliminary result. A doctor will make the final decision on donation day. "
                    + "Blood type " + bloodType + " is on file";
        } else {
            String daysUntilEligible = ownRecord.getDaysUntilEligible() != null
                    ? String.valueOf(ownRecord.getDaysUntilEligible())
                    : "N/A";
            note = "This is a preliminary result. You are currently marked not eligible. "
                    + "Estimated wait: " + daysUntilEligible + " day(s).";
        }

        if (!"N/A".equals(age) || !"N/A".equals(weight)) {
            note += " Profile snapshot: Age " + age + ", Weight " + weight + ".";
        }
        donorStatusNote.setText(note);
    }


    @FXML
    //opens the eligibility check form dialog for the donor to fill out or review, only for donors!!!
    private void handleDonorFillForm() {
        if (!readOnlyDonor) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonorEligibilityCheckDialog.fxml"));
            Parent root = loader.load();

            DonorEligibilityCheckDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Donor Eligibility Check");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(donorResponsesCard.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

            controller.setDialogStage(dialogStage);
            controller.setOnSaved(this::loadRecords);

            dialogStage.showAndWait();
            loadRecords();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open eligibility form: " + e.getMessage());
        }
    }

    private Donor getCurrentDonor() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            return null;
        }

        Donor lookup = new Donor();
        lookup.setUserId(currentUserId);
        return donorService.getOne(lookup);
    }

    private void populateDonationStatCards(Donor donor) {
        if (donor == null) {
            donorDaysSinceDonationValue.setText("N/A");
            donorDonationGapHintValue.setText("min. 56 required");
            donorTotalDonationsValue.setText("0");
            donorHeaderSubtitle.setText("Review your status and run a pre-screening check before joining a campaign.");
            return;
        }

        donorTotalDonationsValue.setText(String.valueOf(Math.max(donor.getTotalDonations(), 0)));

        if (donor.getLastDonationDate() == null) {
            donorDaysSinceDonationValue.setText("N/A");
            donorDonationGapHintValue.setText("no prior donation date");
        } else {
            long elapsed = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
            donorDaysSinceDonationValue.setText(String.valueOf(Math.max(elapsed, 0)));
            donorDonationGapHintValue.setText("min. 56 required");
        }

        donorHeaderSubtitle.setText("Review your status and run a pre-screening check before joining a campaign.");
    }

    private void updateLastCheckCard(DonorEligibility ownRecord) {
        if (ownRecord == null || ownRecord.getLastCalculatedAt() == null) {
            donorLastCheckDaysValue.setText("N/A");
            donorLastCheckDateValue.setText("ago • date unavailable");
            return;
        }

        long daysAgo = ChronoUnit.DAYS.between(ownRecord.getLastCalculatedAt(), LocalDate.now());
        donorLastCheckDaysValue.setText(daysAgo + " days");
        donorLastCheckDateValue.setText("ago • " + ownRecord.getLastCalculatedAt().format(prettyDateFormatter));
    }

    private void applyStatusBadge(String text, String moodClass) {
        if (donorCurrentStatusBadge == null) {
            return;
        }

        donorCurrentStatusBadge.setText(text);
        donorCurrentStatusBadge.getStyleClass().removeAll(
                "donor-elig-badge-good",
                "donor-elig-badge-bad",
                "donor-elig-badge-neutral"
        );
        donorCurrentStatusBadge.getStyleClass().add(moodClass);
    }

    private String extractFieldValue(String details, String prefix) {
        if (details == null || prefix == null) {
            return "N/A";
        }

        String[] lines = details.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith(prefix)) {
                continue;
            }

            String value = trimmed.substring(prefix.length()).trim();
            int markerIndex = value.indexOf("(");
            if (markerIndex > 0) {
                value = value.substring(0, markerIndex).trim();
            }
            if (prefix.equals("Weight:") && !value.equalsIgnoreCase("N/A") && !value.toLowerCase().contains("kg")) {
                value = value + " kg";
            }
            return value.isEmpty() ? "N/A" : value;
        }

        return "N/A";
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

        String detailsText = (record.getEligibilityDetails() != null && !record.getEligibilityDetails().trim().isEmpty())
            ? record.getEligibilityDetails().trim()
            : "Detailed questionnaire responses are not available for this record yet.";

        Label detailsLabel = new Label("Eligibility Details\n" + detailsText);
        detailsLabel.setWrapText(true);
        detailsLabel.setMaxWidth(Double.MAX_VALUE);
        detailsLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #334155; -fx-background-color: #f8fafc; "
            + "-fx-padding: 10 12 10 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e2e8f0;");
        card.getChildren().add(detailsLabel);

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
            controller.setAllowedDonorIds(allowedDonorIds);

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
            controller.setAllowedDonorIds(allowedDonorIds);

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

    private Set<String> resolveAllowedDonorIds() {
        if (readOnlyDonor) {
            return currentUserId != null ? Set.of(currentUserId) : Set.of();
        }

        if (!sessionScopeService.isHospitalStaff()) {
            return Set.of();
        }

        String hospitalId = sessionScopeService.getCurrentHospitalId();
        if (hospitalId == null || hospitalId.isBlank()) {
            return Set.of();
        }

        ServiceDonation donationService = new ServiceDonation();
        return donationService.getAll()
            .stream()
            .filter(donation ->
                donation != null &&
                donation.getHospitalId() != null &&
                hospitalId.equalsIgnoreCase(donation.getHospitalId()) &&
                donation.getDonorId() != null
            )
            .map(donation -> donation.getDonorId())
            .collect(Collectors.toCollection(HashSet::new));
    }
}
