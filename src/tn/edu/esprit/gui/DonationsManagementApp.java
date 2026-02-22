package tn.edu.esprit.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DonationsManagementApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("DonationsDashboard.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            
            primaryStage.setTitle("BloodLink - Donations Management Dashboard");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
