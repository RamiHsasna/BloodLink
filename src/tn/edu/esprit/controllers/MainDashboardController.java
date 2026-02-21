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
    private HBox navHospitals;

    @FXML
    private HBox navInventory;

    @FXML
    private HBox navTransfers;

    @FXML
    private HBox navBloodTypes;

    private HBox activeNavItem;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeNavItem = navHospitals;
        // Load the hospitals view by default
        loadView("/tn/edu/esprit/views/HospitalList.fxml");
    }

    // ==================== NAVIGATION HANDLERS ====================

    @FXML
    private void onNavHospitals() {
        setActiveNav(navHospitals);
        loadView("/tn/edu/esprit/views/HospitalList.fxml");
    }

    @FXML
    private void onNavInventory() {
        setActiveNav(navInventory);
        loadView("/tn/edu/esprit/views/InventoryView.fxml");
    }

    @FXML
    private void onNavTransfers() {
        setActiveNav(navTransfers);
        // TODO: loadView("/tn/edu/esprit/views/TransferList.fxml");
        System.out.println("Transfers view not yet implemented.");
    }

    @FXML
    private void onNavBloodTypes() {
        setActiveNav(navBloodTypes);
        // TODO: loadView("/tn/edu/esprit/views/BloodTypeList.fxml");
        System.out.println("Blood Types view not yet implemented.");
    }

    // ==================== HELPERS ====================

    /**
     * Loads an FXML view into the main content area.
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(fxmlPath)
            );
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
