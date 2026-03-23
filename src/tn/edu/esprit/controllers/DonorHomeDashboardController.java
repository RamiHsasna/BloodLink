package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.DonorEligibilityService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DonorHomeDashboardController {

    @FXML private HBox eligibilityBannerCard;
    @FXML private Label eligibilityTitleLabel;
    @FXML private Label eligibilitySubtitleLabel;
    @FXML private Label eligibilityMetaLabel;
    @FXML private Button checkEligibilityBtn;

    private final DonorEligibilityService eligibilityService = new DonorEligibilityService();

    @FXML
    public void initialize() {
        refreshEligibilityCard();
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
            controller.setOnSaved(this::refreshEligibilityCard);

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

    private void setBannerStyle(String stateClass) {
        eligibilityBannerCard.getStyleClass().removeAll(
                "eligibility-banner-good",
                "eligibility-banner-bad",
                "eligibility-banner-neutral"
        );
        eligibilityBannerCard.getStyleClass().add(stateClass);
    }
}
