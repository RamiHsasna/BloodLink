package tn.edu.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.services.MetierIAService;
import tn.edu.esprit.services.ServiceDonation;

import java.util.*;
import java.util.stream.Collectors;

public class AIPredictionController {

    @FXML private VBox predictionContainer;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label statusLabel;
    @FXML private Button analyzeBtn;
    @FXML private ScrollPane scrollPane;

    private final ServiceDonation service = new ServiceDonation();
    private final MetierIAService metierIAService = new MetierIAService();

    @FXML
    public void initialize() {
        loadingSpinner.setVisible(false);
        statusLabel.setText("Click 'Analyze' to get AI predictions for next month's blood demand.");
    }

    @FXML
    private void handleAnalyze() {
        // Show loading
        loadingSpinner.setVisible(true);
        analyzeBtn.setDisable(true);
        statusLabel.setText("🤖 AI is analyzing your donation data...");
        predictionContainer.getChildren().clear();

        // Run in background thread so UI doesn't freeze
        Thread thread = new Thread(() -> {
            try {
                // Step 1: Get donation data
                List<Donations> donations = service.getAll();

                // Step 2: Build summary for AI
                String summary = buildDonationSummary(donations);

                // Step 3: Ask configured AI provider
                String prediction = metierIAService.generateDonationPrediction(summary);

                // Step 4: Show result on UI thread
                Platform.runLater(() -> {
                    displayPrediction(prediction);
                    loadingSpinner.setVisible(false);
                    analyzeBtn.setDisable(false);
                    statusLabel.setText("✅ AI analysis complete!");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    analyzeBtn.setDisable(false);
                    statusLabel.setText("❌ Error: " + e.getMessage());
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String buildDonationSummary(List<Donations> donations) {
        // Count per blood type
        Map<String, Long> bloodTypeCounts = donations.stream()
                .filter(d -> d.getBloodTypeId() != null)
                .collect(Collectors.groupingBy(Donations::getBloodTypeId, Collectors.counting()));

        // Count per month
        Map<String, Long> monthCounts = new LinkedHashMap<>();
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"};
        for (String m : months) monthCounts.put(m, 0L);
        for (Donations d : donations) {
            if (d.getDonationDate() != null) {
                String month = months[d.getDonationDate().getMonthValue() - 1];
                monthCounts.put(month, monthCounts.get(month) + 1);
            }
        }

        // Count screening passed/failed
        long passed = donations.stream()
                .filter(d -> Boolean.TRUE.equals(d.getScreeningPassed())).count();
        long failed = donations.stream()
                .filter(d -> Boolean.FALSE.equals(d.getScreeningPassed())).count();

        // Build summary string
        StringBuilder sb = new StringBuilder();
        sb.append("Total donations: ").append(donations.size()).append("\n");
        sb.append("Screening passed: ").append(passed).append("\n");
        sb.append("Screening failed: ").append(failed).append("\n\n");
        sb.append("Donations by blood type:\n");
        bloodTypeCounts.forEach((type, count) ->
                sb.append("- ").append(type).append(": ").append(count).append("\n"));
        sb.append("\nDonations by month:\n");
        monthCounts.forEach((month, count) ->
                sb.append("- ").append(month).append(": ").append(count).append("\n"));

        return sb.toString();
    }

    private void displayPrediction(String prediction) {
        predictionContainer.getChildren().clear();

        // Split by sections and display each one as a card
        String[] sections = prediction.split("\n\n");
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                    "-fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

            String[] lines = section.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                Label lbl = new Label(line);
                lbl.setWrapText(true);
                lbl.setMaxWidth(Double.MAX_VALUE);

                if (i == 0) {
                    // First line is the section title
                    lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; " +
                            "-fx-text-fill: #1a2535;");
                } else {
                    lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a6278;");
                }
                card.getChildren().add(lbl);
            }
            predictionContainer.getChildren().add(card);
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) analyzeBtn.getScene().getWindow();
        stage.close();
    }
}
