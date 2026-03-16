package tn.edu.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainDashboardController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox sidebar;

    // Navigation items
    @FXML
    private HBox navUsers;

    @FXML
    private HBox navHospitals;

    @FXML
    private HBox navDonations;

    @FXML
    private HBox navDonationEvents;

    @FXML
    private HBox navInventory;

    @FXML
    private HBox navTransfers;

    @FXML
    private HBox navBloodTypes;

    @FXML
    private HBox navEligibility;

    @FXML
    private HBox navAlerts;

    @FXML
    private HBox navAuditLogs;

    private HBox activeNavItem;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeNavItem = navHospitals;
        // Load the hospitals view by default
        loadView("/tn/edu/esprit/views/HospitalList.fxml");
    }

    // ==================== NAVIGATION HANDLERS ====================

    @FXML
    private void onNavUsers() {
        setActiveNav(navUsers);
        loadView("/tn/edu/esprit/views/UserDashboard.fxml");
    }

    @FXML
    private void onNavHospitals() {
        setActiveNav(navHospitals);
        loadView("/tn/edu/esprit/views/HospitalList.fxml");
    }

    @FXML
    private void onNavDonations() {
        setActiveNav(navDonations);
        loadView("/tn/edu/esprit/views/DonationsDashboard.fxml");
    }

    @FXML
    private void onNavDonationEvents() {
        setActiveNav(navDonationEvents);
        loadView("/tn/edu/esprit/views/DonationEventDashboard.fxml");
    }

    @FXML
    private void onNavInventory() {
        setActiveNav(navInventory);
        loadView("/tn/edu/esprit/views/InventoryView.fxml");
    }

    @FXML
    private void onNavTransfers() {
        setActiveNav(navTransfers);
        loadView("/tn/edu/esprit/views/TransferList.fxml");
    }

    @FXML
    private void onNavBloodTypes() {
        setActiveNav(navBloodTypes);
        // TODO: loadView("/tn/edu/esprit/views/BloodTypeList.fxml");
        System.out.println("Blood Types view not yet implemented.");
    }

    @FXML
    private void onNavEligibility() {
        setActiveNav(navEligibility);
        loadView("/tn/edu/esprit/views/DonorEligibilityDashboard.fxml");
    }

    @FXML
    private void onNavAlerts() {
        setActiveNav(navAlerts);
        loadView("/tn/edu/esprit/views/DashboardAlerts.fxml");
    }

    @FXML
    private void onNavAuditLogs() {
        setActiveNav(navAuditLogs);
        loadView("/tn/edu/esprit/views/DashboardLogs.fxml");
    }

    // ==================== HELPERS ====================

    /**
     * Loads an FXML view into the main content area.
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            System.err.println("Failed to load view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Updates sidebar styling so only the clicked nav item is highlighted.
     */
    private void setActiveNav(HBox selectedItem) {
        if (activeNavItem != null) {
            activeNavItem.getStyleClass().remove("nav-item-active");
        }
        selectedItem.getStyleClass().add("nav-item-active");
        activeNavItem = selectedItem;
    }

    /**
     * Public accessor so child controllers can load views into the content area
     * (e.g. navigating from one page to another programmatically).
     */
    public StackPane getContentArea() {
        return contentArea;
    }
}
