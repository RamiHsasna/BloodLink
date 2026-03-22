package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.edu.esprit.entities.BloodType;
import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.BloodTypeServiceImpl;
import tn.edu.esprit.services.DonorEligibilityService;
import tn.edu.esprit.services.ServiceDonor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DonorEligibilityCheckDialogController {

    @FXML private Spinner<Integer> ageSpinner;
    @FXML private TextField weightField;
    @FXML private DatePicker lastDonationDatePicker;

    @FXML private CheckBox unwellTodayCheck;
    @FXML private CheckBox pregnantRecentBirthCheck;
    @FXML private CheckBox onMedicationCheck;
    @FXML private CheckBox recentSurgeryCheck;
    @FXML private CheckBox recentVaccineCheck;
    @FXML private CheckBox recentTravelCheck;
    @FXML private CheckBox recentTattooCheck;

    @FXML private Label resultTitleLabel;
    @FXML private Label resultDescriptionLabel;
    @FXML private TextArea reasonsArea;
    @FXML private Button saveResultBtn;

    private final DonorEligibilityService eligibilityService = new DonorEligibilityService();
    private final ServiceDonor donorService = new ServiceDonor();
    private final BloodTypeServiceImpl bloodTypeService = new BloodTypeServiceImpl();

    private Stage dialogStage;
    private Runnable onSaved;
    private Donor currentDonor;
    private EligibilityOutcome lastOutcome;

    @FXML
    public void initialize() {
        ageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(16, 80, 18));
        configureLastDonationDatePicker();
        saveResultBtn.setDisable(true);
        loadCurrentDonor();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void handleCalculate() {
        Double weightKg = parseWeight(); // If weight is invalid, parseWeight will show an alert and return null, so we should not proceed with calculation.
        if (weightKg == null) {
            return;
        }

        int age = ageSpinner.getValue();
        LocalDate lastDonationDate = lastDonationDatePicker.getValue();

        if (lastDonationDate != null && lastDonationDate.isAfter(LocalDate.now())) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Last donation date cannot be in the future.");
            return;
        }
        


        List<String> hardStops = new ArrayList<>(); //list of reasons that would make donation not recommended at this time
        List<String> doctorReview = new ArrayList<>(); //list of reasons that would suggest a doctor should review the case before donation

        int daysUntilEligible = 0;
        int minDaysBetweenDonations = 56;

        if (age < 18 || age > 65) {
            hardStops.add("Donors must be between 18 and 65 years old to participate");
        }

        if (weightKg < 50.0) {
            hardStops.add("A minimum weight of 50 kg is required to donate.");
        }

        if (lastDonationDate != null) {
            long elapsed = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
            if (elapsed < minDaysBetweenDonations) {
                daysUntilEligible = (int) Math.max(minDaysBetweenDonations - elapsed, 1);
                hardStops.add("Last donation was too recent (wait " + daysUntilEligible + " more day(s)).");
            }
        }

        if (unwellTodayCheck.isSelected()) {
            hardStops.add("You reported not feeling well today.Donating while unwell can affect both your health and the quality of the donated blood. Please wait until you have fully recovered before attempting to donate.");
        }

        if (pregnantRecentBirthCheck.isSelected()) {
            hardStops.add("Donation is temporarily deferred during pregnancy and for 6 weeks following childbirth.");
        }

        if (onMedicationCheck.isSelected()) {
            doctorReview.add("Currently on medication, a doctor will review the case to determine if the medication affects eligibility.");
        }
        if (recentSurgeryCheck.isSelected()) {
            doctorReview.add("Recent surgery.");
        }
        if (recentVaccineCheck.isSelected()) {
            doctorReview.add("Recent vaccine.");
        }
        if (recentTravelCheck.isSelected()) {
            doctorReview.add("Recent travel abroad, travel history has been noted and will be reviewed by a doctor before donation day.");
        }
        if (recentTattooCheck.isSelected()) {
            doctorReview.add("Recent tattoo");
        }

        if (!hardStops.isEmpty()) {
            if (daysUntilEligible <= 0 && lastDonationDate != null) {
                    daysUntilEligible = 1;
            }
            lastOutcome = EligibilityOutcome.notRecommended(daysUntilEligible, hardStops);
        } else if (!doctorReview.isEmpty()) {
            lastOutcome = EligibilityOutcome.needsDoctorReview(1, doctorReview);
        } else {
            lastOutcome = EligibilityOutcome.likelyEligible();
        }

        showOutcome(lastOutcome);
        saveResultBtn.setDisable(false);
    }

    @FXML
    private void handleSaveResult() {
        if (lastOutcome == null) { //save button disabled without calculation
            showAlert(Alert.AlertType.WARNING, "Eligibility Check", "Please calculate eligibility first.");
            return;
        }

        Users user = AppSession.getCurrentUser(); 
        if (user == null || user.getId() == null || user.getId().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "No active donor session found.");
            return;
        }


//for saving in the db
        DonorEligibility record = new DonorEligibility(); 
        record.setId(user.getId());
        record.setIsCurrentlyEligible(lastOutcome.currentlyEligible);
        record.setDaysUntilEligible(lastOutcome.daysUntilEligible);
        record.setLastCalculatedAt(LocalDate.now());
        record.setBloodTypeCache(resolveBloodTypeDisplay());
        record.setEligibilityDetails(buildEligibilityDetails()); 


//only save location if possible
        if (currentDonor != null && currentDonor.getLatitude() != null) {
            record.setLatitudeCache(BigDecimal.valueOf(currentDonor.getLatitude()));
        }
        if (currentDonor != null && currentDonor.getLongitude() != null) {
            record.setLongitudeCache(BigDecimal.valueOf(currentDonor.getLongitude()));
        }

        eligibilityService.upsertEligibility(record);  //upsert = update + insert if not exists

        if (onSaved != null) {
            onSaved.run();
        }

        showAlert(Alert.AlertType.INFORMATION, "Saved", "Eligibility check saved successfully.");
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

//donor data linked to user
    private void loadCurrentDonor() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getId() == null) {
            return;
        }

        Donor lookup = new Donor();
        lookup.setUserId(user.getId());
        currentDonor = donorService.getOne(lookup);

//prevent futur date 
        if (currentDonor != null && currentDonor.getLastDonationDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate donorLastDonationDate = currentDonor.getLastDonationDate();
            if (!donorLastDonationDate.isAfter(today)) {
                lastDonationDatePicker.setValue(donorLastDonationDate);
            }
        }
    }

    private void configureLastDonationDatePicker() {
        // Prevent selecting future dates for last donation.
        lastDonationDatePicker.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item == null || item.isAfter(LocalDate.now()));
            }
        });
    }

    private String resolveBloodTypeDisplay() {
        if (currentDonor == null || currentDonor.getBloodTypeId() == null) {
            return "N/A";
        }

        List<BloodType> bloodTypes = bloodTypeService.getAllBloodTypes();
        for (BloodType bloodType : bloodTypes) {
            if (bloodType != null && currentDonor.getBloodTypeId().equals(bloodType.getBloodTypeId())) {
                String abo = bloodType.getAboType() != null ? bloodType.getAboType().trim().toUpperCase() : "";
                String rh = normalizeRh(bloodType.getRhFactor());
                if (!abo.isEmpty() && rh != null) {
                    return abo + rh;
                }
            }
        }

        return currentDonor.getBloodTypeId();
    }

    private String normalizeRh(String rh) {
        if (rh == null) {
            return null;
        }

        String normalized = rh.trim().toUpperCase();
        if ("+".equals(normalized) || "POS".equals(normalized) || "POSITIVE".equals(normalized)) {
            return "+";
        }
        if ("-".equals(normalized) || "NEG".equals(normalized) || "NEGATIVE".equals(normalized)) {
            return "-";
        }
        return null;
    }

    private Double parseWeight() {
        String rawWeight = weightField.getText() != null ? weightField.getText().trim() : "";
        if (rawWeight.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Weight is required.");
            return null;
        }

        try {
            double weight = Double.parseDouble(rawWeight);
            if (weight <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Weight must be greater than 0.");
                return null;
            }
            return weight;
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Weight must be a valid number.");
            return null;
        }
    }

    private void showOutcome(EligibilityOutcome outcome) {
        resultTitleLabel.getStyleClass().removeAll("text-success", "text-danger");
        reasonsArea.clear();

        if (outcome.type == OutcomeType.LIKELY_ELIGIBLE) {
            resultTitleLabel.setText("Likely Eligible");
            resultTitleLabel.getStyleClass().add("text-success");
            resultDescriptionLabel.setText("No major blockers detected from your answers.");
            reasonsArea.setText("You can proceed to donation center screening.");
            return;
        }

        if (outcome.type == OutcomeType.NEEDS_DOCTOR_REVIEW) {
            resultTitleLabel.setText("Needs Doctor Review");
            resultTitleLabel.getStyleClass().add("text-danger");
            resultDescriptionLabel.setText("A doctor should review your case before donation.");
            reasonsArea.setText(String.join("\n", outcome.reasons));
            return;
        }

        resultTitleLabel.setText("Not Recommended Now");
        resultTitleLabel.getStyleClass().add("text-danger");
        resultDescriptionLabel.setText("Donation is not recommended at this time based on your answers.");
        reasonsArea.setText(String.join("\n", outcome.reasons));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String buildEligibilityDetails() {
        StringBuilder details = new StringBuilder();

        int age = ageSpinner.getValue() != null ? ageSpinner.getValue() : 0;
        String weight = weightField.getText() != null ? weightField.getText().trim() : "N/A";
        LocalDate lastDonationDate = lastDonationDatePicker.getValue();

        details.append("Outcome: ").append(formatOutcomeLabel(lastOutcome.type)).append("\n");
        details.append("Age: ").append(age).append("\n");

        details.append("Weight: ").append(weight).append(" kg");
        try {
            if (!"N/A".equals(weight)) {
                Double.parseDouble(weight);
            }
        } catch (NumberFormatException ignored) {
            details.append(" (Invalid input)");
        }
        details.append("\n");

        if (lastDonationDate != null) {
            long elapsed = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
            int threshold = 56;
            details.append("Last donation: ").append(lastDonationDate)
                    .append(" (elapsed ").append(elapsed).append(" days, min ").append(threshold).append(")")
                    .append("\n");
        } else {
            details.append("Last donation: Not provided\n");
        }
//report to summarize donor answers in a readable format, this will be saved in the db for reference and to help doctors understand the case if review is needed
        details.append("Not feeling well today: ").append(toYesNo(unwellTodayCheck.isSelected())).append("\n");
        details.append("Pregnant/recent birth: ").append(toYesNo(pregnantRecentBirthCheck.isSelected())).append("\n");
        details.append("On medication: ").append(toYesNo(onMedicationCheck.isSelected())).append("\n");
        details.append("Recent surgery: ").append(toYesNo(recentSurgeryCheck.isSelected())).append("\n");
        details.append("Recent vaccine: ").append(toYesNo(recentVaccineCheck.isSelected())).append("\n");
        details.append("Recent travel abroad: ").append(toYesNo(recentTravelCheck.isSelected())).append("\n");
        details.append("Recent tattoo/piercing: ").append(toYesNo(recentTattooCheck.isSelected())).append("\n");

        if (lastOutcome != null && lastOutcome.reasons != null && !lastOutcome.reasons.isEmpty()) {
            details.append("Reasons:\n");
            for (String reason : lastOutcome.reasons) {
                details.append("- ").append(reason).append("\n");
            }
        }

        return details.toString().trim();
    }

    private String formatOutcomeLabel(OutcomeType type) {
        if (type == null) {
            return "Unknown";
        }
        if (type == OutcomeType.LIKELY_ELIGIBLE) {
            return "Likely Eligible";
        }
        if (type == OutcomeType.NEEDS_DOCTOR_REVIEW) {
            return "Needs Doctor Review";
        }
        return "Not Recommended";
    }

    private String toYesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private static class EligibilityOutcome {
        private final OutcomeType type;
        private final boolean currentlyEligible;
        private final int daysUntilEligible;
        private final List<String> reasons;

        private EligibilityOutcome(OutcomeType type, boolean currentlyEligible, int daysUntilEligible, List<String> reasons) {
            this.type = type;
            this.currentlyEligible = currentlyEligible;
            this.daysUntilEligible = daysUntilEligible;
            this.reasons = reasons;
        }

        private static EligibilityOutcome likelyEligible() {
            return new EligibilityOutcome(OutcomeType.LIKELY_ELIGIBLE, true, 0, List.of("No blockers detected."));
        }

        private static EligibilityOutcome needsDoctorReview(int daysUntilEligible, List<String> reasons) {
            return new EligibilityOutcome(OutcomeType.NEEDS_DOCTOR_REVIEW, false, daysUntilEligible, reasons);
        }

        private static EligibilityOutcome notRecommended(int daysUntilEligible, List<String> reasons) {
            return new EligibilityOutcome(OutcomeType.NOT_RECOMMENDED, false, daysUntilEligible, reasons);
        }
    }

    private enum OutcomeType {
        LIKELY_ELIGIBLE,
        NEEDS_DOCTOR_REVIEW,
        NOT_RECOMMENDED
    }
}
