package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.DonorEligibilityService;
import tn.edu.esprit.services.DonorHistoryService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DonorHomeDashboardController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    private HBox eligibilityBannerCard;
    @FXML
    private Label eligibilityTitleLabel;
    @FXML
    private Label eligibilitySubtitleLabel;
    @FXML
    private Label eligibilityMetaLabel;
    @FXML
    private Button checkEligibilityBtn;
    @FXML
    private Label donationHistorySummaryLabel;
    @FXML
    private Label donationHistoryCountLabel;
    @FXML
    private VBox donationHistoryContainer;
    @FXML
    private Label transferHistorySummaryLabel;
    @FXML
    private Label transferHistoryCountLabel;
    @FXML
    private VBox transferHistoryContainer;

    private final DonorEligibilityService eligibilityService = new DonorEligibilityService();
    private final DonorHistoryService donorHistoryService = new DonorHistoryService();

    @FXML
    public void initialize() {
        refreshEligibilityCard();
        loadHistoryCards();
    }

    @FXML
    private void handleOpenEligibilityCheck() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/DonorEligibilityCheckDialog.fxml"));
            Parent root = loader.load();

            DonorEligibilityCheckDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Donor Eligibility Check");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(checkEligibilityBtn.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(
                    getClass().getResource("/tn/edu/esprit/styles/dashboard.css").toExternalForm());

            controller.setDialogStage(dialogStage);
            controller.setOnSaved(() -> {
                refreshEligibilityCard();
                loadHistoryCards();
            });

            dialogStage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshEligibilityCard() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getId() == null) {
            return;
        }

        DonorEligibility record = eligibilityService.getOne(user.getId());
        setBannerStyle("eligibility-banner-neutral");

        if (record == null) {
            eligibilityTitleLabel.setText("Eligibility check needed");
            eligibilitySubtitleLabel.setText("You have not checked your eligibility yet.");
            eligibilityMetaLabel.setText("Please complete the quick medical questionnaire before your next donation.");
            checkEligibilityBtn.setText("Check now");
            return;
        }

        checkEligibilityBtn.setText("Check again");
        String bloodType = (record.getBloodTypeCache() != null && !record.getBloodTypeCache().trim().isEmpty())
                ? record.getBloodTypeCache()
                : "N/A";

        long daysAgo = -1;
        if (record.getLastCalculatedAt() != null) {
            daysAgo = ChronoUnit.DAYS.between(record.getLastCalculatedAt(), LocalDate.now());
        }
        String checkInfo = daysAgo >= 0
                ? "Last checked " + daysAgo + " day(s) ago"
                : "Last check date unavailable";

        if (record.getIsCurrentlyEligible() != null && record.getIsCurrentlyEligible()) {
            setBannerStyle("eligibility-banner-good");
            eligibilityTitleLabel.setText("You are likely eligible to donate");
            eligibilitySubtitleLabel.setText(checkInfo + " - Blood type: " + bloodType);
            eligibilityMetaLabel.setText("You can still re-check if your condition changed today.");
            return;
        }

        setBannerStyle("eligibility-banner-bad");
        eligibilityTitleLabel.setText("Donation is not recommended right now");

        int days = record.getDaysUntilEligible() != null ? record.getDaysUntilEligible() : 0;
        if (days > 0) {
            eligibilitySubtitleLabel.setText("Estimated wait: " + days + " day(s) - Blood type: " + bloodType);
        } else {
            eligibilitySubtitleLabel.setText(checkInfo + " - Blood type: " + bloodType);
        }
        eligibilityMetaLabel.setText("You can re-check anytime or consult the medical team for final validation.");
    }

    private void loadHistoryCards() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getId() == null) {
            renderEmptyState(donationHistoryContainer, "No donation history available yet.",
                    "Donation history appears once your account is connected to at least one donation.");
            renderEmptyState(transferHistoryContainer, "No transfer history available yet.",
                    "Transfer history will appear when a matching transfer exists for your donation profile.");
            updateHistorySummary(0, 0);
            return;
        }

        try {
            List<DonorHistoryService.DonationHistoryItem> donations = donorHistoryService.getDonationHistory(user.getId());
            List<DonorHistoryService.TransferHistoryItem> transfers = donorHistoryService.getTransferHistory(user.getId());

            renderDonationHistory(donations);
            renderTransferHistory(transfers);
            updateHistorySummary(donations.size(), transfers.size());
        } catch (Exception exception) {
            renderEmptyState(donationHistoryContainer, "Unable to load donations right now.", exception.getMessage());
            renderEmptyState(transferHistoryContainer, "Unable to load transfer history right now.", exception.getMessage());
            updateHistorySummary(0, 0);
        }
    }

    private void updateHistorySummary(int donationCount, int transferCount) {
        if (donationHistoryCountLabel != null) {
            donationHistoryCountLabel.setText(String.valueOf(donationCount));
        }
        if (transferHistoryCountLabel != null) {
            transferHistoryCountLabel.setText(String.valueOf(transferCount));
        }
        if (donationHistorySummaryLabel != null) {
            donationHistorySummaryLabel.setText(donationCount == 0
                    ? "No donations recorded for your account yet."
                    : donationCount + " donation(s) recorded in the current database.");
        }
        if (transferHistorySummaryLabel != null) {
            transferHistorySummaryLabel.setText(transferCount == 0
                    ? "No transfer match found yet for your donation profile."
                    : transferCount + " estimated transfer(s) matched to your donation profile.");
        }
    }

    private void renderDonationHistory(List<DonorHistoryService.DonationHistoryItem> donations) {
        if (donationHistoryContainer == null) {
            return;
        }
        donationHistoryContainer.getChildren().clear();

        if (donations == null || donations.isEmpty()) {
            renderEmptyState(donationHistoryContainer, "No donation history available yet.",
                    "Your completed and planned donations will appear here.");
            return;
        }

        for (DonorHistoryService.DonationHistoryItem donation : donations) {
            donationHistoryContainer.getChildren().add(buildDonationCard(donation));
        }
    }

    private void renderTransferHistory(List<DonorHistoryService.TransferHistoryItem> transfers) {
        if (transferHistoryContainer == null) {
            return;
        }
        transferHistoryContainer.getChildren().clear();

        if (transfers == null || transfers.isEmpty()) {
            renderEmptyState(transferHistoryContainer, "No transfer history available yet.",
                    "Matching transfers will appear once hospitals move units of the same blood type after your donation.");
            return;
        }

        for (DonorHistoryService.TransferHistoryItem transfer : transfers) {
            transferHistoryContainer.getChildren().add(buildTransferCard(transfer));
        }
    }

    private VBox buildDonationCard(DonorHistoryService.DonationHistoryItem donation) {
        VBox card = new VBox(10);
        card.getStyleClass().add("history-card");

        Label title = new Label(resolveHospitalName(donation.getHospitalName()));
        title.getStyleClass().add("history-card-title");
        Label date = new Label(formatDateTime(resolveDonationDate(donation)));
        date.getStyleClass().add("history-card-subtitle");
        Label statusBadge = createStatusBadge(donation.getStatus());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, spacer, statusBadge);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox line1 = buildMetaRow("Blood type", donation.getBloodTypeLabel());
        HBox line2 = buildMetaRow("Collected units", donation.getUnitsCollected() + " unit(s)");
        HBox line3 = buildMetaRow("Screening", formatScreening(donation.getScreeningPassed()));
        HBox line4 = buildMetaRow("Volume", donation.getVolumeCollected() != null
                ? donation.getVolumeCollected().stripTrailingZeros().toPlainString() + " ml"
                : "N/A");

        VBox details = new VBox(6, date, line1, line2, line3, line4);
        if (donation.getMedicalNotes() != null && !donation.getMedicalNotes().isBlank()) {
            Label notes = new Label("Notes: " + donation.getMedicalNotes().trim());
            notes.getStyleClass().add("history-meta");
            notes.setWrapText(true);
            details.getChildren().add(notes);
        }

        card.getChildren().addAll(header, details);
        return card;
    }

    private VBox buildTransferCard(DonorHistoryService.TransferHistoryItem transfer) {
        VBox card = new VBox(10);
        card.getStyleClass().add("history-card");

        Label title = new Label(resolveHospitalName(transfer.getDestinationHospitalName()));
        title.getStyleClass().add("history-card-title");
        Label statusBadge = createStatusBadge(transfer.getStatus());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, spacer, statusBadge);
        header.setAlignment(Pos.CENTER_LEFT);

        Label timestamp = new Label(formatDateTime(resolveTransferDate(transfer)));
        timestamp.getStyleClass().add("history-card-subtitle");

        HBox line1 = buildMetaRow("Source hospital", resolveHospitalName(transfer.getSourceHospitalName()));
        HBox line2 = buildMetaRow("Blood type", fallback(transfer.getTransferBloodTypeLabel(), transfer.getDonationBloodTypeLabel()));
        HBox line3 = buildMetaRow("Approved units", formatUnits(transfer.getQuantityUnitsApproved(), transfer.getQuantityUnitsRequested()));
        HBox line4 = buildMetaRow("Matched donation", "Donation #" + truncateId(transfer.getDonationId()));
        HBox line5 = buildMetaRow("Linked donation hospital", resolveHospitalName(transfer.getDonationHospitalName()));

        VBox details = new VBox(6, timestamp, line1, line2, line3, line4, line5);
        if (transfer.getReason() != null && !transfer.getReason().isBlank()) {
            Label reason = new Label("Reason: " + transfer.getReason().trim());
            reason.getStyleClass().add("history-meta");
            reason.setWrapText(true);
            details.getChildren().add(reason);
        }
        if (transfer.getNotes() != null && !transfer.getNotes().isBlank()) {
            Label notes = new Label("Notes: " + transfer.getNotes().trim());
            notes.getStyleClass().add("history-meta");
            notes.setWrapText(true);
            details.getChildren().add(notes);
        }

        card.getChildren().addAll(header, details);
        return card;
    }

    private HBox buildMetaRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("history-meta-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("history-meta-value");
        value.setWrapText(true);

        HBox row = new HBox(8, label, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label createStatusBadge(String status) {
        String normalized = status != null ? status.trim().toUpperCase() : "UNKNOWN";
        Label badge = new Label(normalized);
        badge.getStyleClass().addAll("history-chip", mapStatusChip(normalized));
        return badge;
    }

    private String mapStatusChip(String status) {
        if (status.contains("COMPLETED") || status.contains("DELIVERED") || status.contains("APPROVED")) {
            return "history-chip-success";
        }
        if (status.contains("PENDING") || status.contains("TRANSIT") || status.contains("SCREENING")) {
            return "history-chip-warning";
        }
        if (status.contains("REJECTED") || status.contains("FAILED") || status.contains("CANCELLED")) {
            return "history-chip-danger";
        }
        return "history-chip-neutral";
    }

    private void renderEmptyState(VBox container, String title, String subtitle) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();

        Label icon = new Label("📭");
        icon.getStyleClass().add("logs-empty-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("logs-empty-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("logs-empty-subtitle");
        subtitleLabel.setWrapText(true);

        VBox state = new VBox(8, icon, titleLabel, subtitleLabel);
        state.getStyleClass().add("logs-empty-state");
        state.setAlignment(Pos.CENTER);
        container.getChildren().add(state);
    }

    private LocalDateTime resolveDonationDate(DonorHistoryService.DonationHistoryItem donation) {
        return donation.getDonationDate() != null ? donation.getDonationDate() : donation.getCreatedAt();
    }

    private LocalDateTime resolveTransferDate(DonorHistoryService.TransferHistoryItem transfer) {
        if (transfer.getActualDeliveryAt() != null) {
            return transfer.getActualDeliveryAt();
        }
        if (transfer.getApprovedAt() != null) {
            return transfer.getApprovedAt();
        }
        return transfer.getRequestedAt();
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMATTER) : "Date unavailable";
    }

    private String formatScreening(Boolean screeningPassed) {
        if (screeningPassed == null) {
            return "N/A";
        }
        return screeningPassed ? "Passed" : "Needs review";
    }

    private String formatUnits(Integer approvedUnits, Integer requestedUnits) {
        if (approvedUnits != null && approvedUnits > 0) {
            return approvedUnits + " approved";
        }
        if (requestedUnits != null && requestedUnits > 0) {
            return requestedUnits + " requested";
        }
        return "N/A";
    }

    private String resolveHospitalName(String hospitalName) {
        return hospitalName != null && !hospitalName.isBlank() ? hospitalName.trim() : "Unknown hospital";
    }

    private String fallback(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary.trim();
        }
        return "N/A";
    }

    private String truncateId(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.length() > 8 ? value.substring(0, 8) + "..." : value;
    }

    private void setBannerStyle(String stateClass) {
        eligibilityBannerCard.getStyleClass().removeAll(
                "eligibility-banner-good",
                "eligibility-banner-bad",
                "eligibility-banner-neutral"
        );
        eligibilityBannerCard.getStyleClass().add(stateClass);
    }
}
