package tn.edu.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.services.ServiceDonation;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AIPredictionController {

    @FXML private VBox predictionContainer;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label statusLabel;
    @FXML private Button analyzeBtn;
    @FXML private ScrollPane scrollPane;

    private final ServiceDonation service = new ServiceDonation();

    // ⚠️ Replace with your actual Claude API key
    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

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

                // Step 3: Ask Claude AI
                String prediction = askClaude(summary);

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

    private String askClaude(String donationSummary) throws Exception {
        String prompt = "You are a medical AI assistant for a blood bank management system called BloodLink. " +
                "Analyze the following blood donation data and provide predictions for next month. " +
                "Be specific, practical, and format your response clearly with sections.\n\n" +
                "DONATION DATA:\n" + donationSummary + "\n\n" +
                "Please provide:\n" +
                "1. 🩸 BLOOD TYPE DEMAND PREDICTION - Which blood types will be most needed next month and why\n" +
                "2. ⚠️ CRITICAL SHORTAGES - Which blood types are at risk of shortage\n" +
                "3. 📈 TREND ANALYSIS - What trends do you see in the donation patterns\n" +
                "4. 💡 RECOMMENDATIONS - Specific actions the blood bank should take\n\n" +
                "Keep your response concise and actionable.";

        // Build JSON request
        String jsonBody = "{"
                + "\"model\": \"claude-sonnet-4-20250514\","
                + "\"max_tokens\": 1024,"
                + "\"messages\": [{\"role\": \"user\", \"content\": \""
                + prompt.replace("\"", "\\\"").replace("\n", "\\n")
                + "\"}]"
                + "}";

        // Make HTTP request
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", API_KEY);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        // Parse response — extract text content
        String json = response.toString();
        int start = json.indexOf("\"text\":\"") + 8;
        int end = json.indexOf("\"}", start);
        if (start > 8 && end > start) {
            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
        }
        return "Could not parse AI response. Please try again.";
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