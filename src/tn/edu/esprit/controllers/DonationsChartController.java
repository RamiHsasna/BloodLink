package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.services.ServiceDonation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DonationsChartController {

    @FXML private BarChart<String, Number> donationsChart;

    private final ServiceDonation service = new ServiceDonation();

    @FXML
    public void initialize() {
        loadChart();
    }

    private void loadChart() {
        // Month names in order
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        // Start with 0 for every month
        Map<String, Integer> data = new LinkedHashMap<>();
        for (String m : months) data.put(m, 0);

        // Count donations per month from database
        List<Donations> allDonations = service.getAll();
        for (Donations d : allDonations) {
            if (d.getDonationDate() != null) {
                String month = months[d.getDonationDate().getMonthValue() - 1];
                data.put(month, data.get(month) + 1);
            }
        }

        // Build the chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Donations");
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        // Style the chart
        donationsChart.setTitle("");
        donationsChart.setLegendVisible(false);
        donationsChart.setAnimated(true);
        donationsChart.getData().add(series);

        // Color the bars red
        donationsChart.setStyle(
                ".default-color0.chart-bar { -fx-bar-fill: #e53935; }"
        );
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) donationsChart.getScene().getWindow();
        stage.close();
    }
}
