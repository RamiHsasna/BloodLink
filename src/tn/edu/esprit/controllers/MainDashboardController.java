package tn.edu.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.SessionScopeService;

public class MainDashboardController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox sidebar;

    // Navigation items
    @FXML
    private HBox navHome;

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

    @FXML
    private HBox navLogout;

    @FXML
    private Label brandSubtitleLabel;

    @FXML
    private Label sidebarSectionLabel;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label profileRoleLabel;

    @FXML
    private Label profileInitialLabel;

    private HBox activeNavItem;
    private String activeViewPath;
    private final Map<String, Node> viewCache = new HashMap<>();
    private SessionScopeService sessionScopeService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Users currentUser = AppSession.getCurrentUser();
        sessionScopeService = new SessionScopeService();
        configureSidebarHeader(currentUser);
        applyRoleAccess(currentUser);

        if (currentUser != null && currentUser.getUserType() == UserType.DONOR) {
            activeNavItem = navHome;
            setActiveNav(navHome);
            loadView("/tn/edu/esprit/views/DonorHomeDashboard.fxml");
            return;
        }

        activeNavItem = navDonations;
        setActiveNav(navDonations);
        loadView("/tn/edu/esprit/views/DonationsDashboard.fxml");
    }

    // ==================== NAVIGATION HANDLERS ====================

    @FXML
    private void onNavHome() {
        setActiveNav(navHome);
        loadView("/tn/edu/esprit/views/DonorHomeDashboard.fxml");
    }

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
        showUnavailableView(
                "Groupes sanguins",
                "Ce module n'est pas encore implémenté dans cette version. Les autres écrans restent accessibles sans bloquer le tableau de bord.");
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

    @FXML
    private void onNavLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Sign out of BloodLink?");
        confirm.setContentText("You will be redirected to the login page.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        AppSession.clear();

        try {
            Parent authRoot = FXMLLoader.load(getClass().getResource("/tn/edu/esprit/views/AuthView.fxml"));
            Stage stage = (Stage) sidebar.getScene().getWindow();
            Scene scene = new Scene(authRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setTitle("BloodLink - Sign In");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            Alert error = new Alert(Alert.AlertType.ERROR, "Could not open login view: " + e.getMessage(), ButtonType.OK);
            error.setHeaderText(null);
            error.showAndWait();
        }
    }

    // ==================== HELPERS ====================

    /**
     * Loads an FXML view into the main content area.
     */
    private void loadView(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            return;
        }

        if (!isViewAllowedForCurrentUser(fxmlPath)) {
            showUnavailableView(
                    "Accès refusé",
                    "Votre rôle ne vous permet pas d'ouvrir cet écran dans cette session.");
            return;
        }

        if (fxmlPath.equals(activeViewPath) && !contentArea.getChildren().isEmpty()) {
            return;
        }

        try {
            Node view = viewCache.get(fxmlPath);
            if (view == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                view = loader.load();
                viewCache.put(fxmlPath, view);
            }
            contentArea.getChildren().setAll(view);
            activeViewPath = fxmlPath;
        } catch (Exception e) {
            System.err.println("Failed to load view: " + fxmlPath);
            e.printStackTrace();
            showUnavailableView(
                    "Écran indisponible",
                    "Impossible de charger cette vue pour le moment. Vérifiez la console pour le détail technique.");
        }
    }

    private void showUnavailableView(String title, String description) {
        Label icon = new Label("📄");
        icon.getStyleClass().add("logs-empty-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("empty-state-description");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(420);

        VBox placeholder = new VBox(10, icon, titleLabel, descriptionLabel);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setFillWidth(true);
        placeholder.getStyleClass().add("empty-state");
        VBox.setVgrow(placeholder, Priority.ALWAYS);

        StackPane wrapper = new StackPane(placeholder);
        wrapper.setAlignment(Pos.CENTER);
        contentArea.getChildren().setAll(wrapper);
        activeViewPath = "__placeholder__:" + title;
    }

    private boolean isViewAllowedForCurrentUser(String fxmlPath) {
        if (sessionScopeService == null) {
            sessionScopeService = new SessionScopeService();
        }

        if (sessionScopeService.isAdmin()) {
            return true;
        }

        if (sessionScopeService.isDonor()) {
            return fxmlPath.endsWith("DonorHomeDashboard.fxml")
                    || fxmlPath.endsWith("DonationsDashboard.fxml")
                    || fxmlPath.endsWith("DonationEventDashboard.fxml")
                    || fxmlPath.endsWith("DonorEligibilityDashboard.fxml")
                    || fxmlPath.endsWith("DashboardAlerts.fxml");
        }

        if (sessionScopeService.isHospitalStaff()) {
            return fxmlPath.endsWith("DonationsDashboard.fxml")
                    || fxmlPath.endsWith("DonationEventDashboard.fxml")
                    || fxmlPath.endsWith("InventoryView.fxml")
                    || fxmlPath.endsWith("TransferList.fxml")
                    || fxmlPath.endsWith("DonorEligibilityDashboard.fxml")
                    || fxmlPath.endsWith("DashboardAlerts.fxml");
        }

        return false;
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

    private void applyRoleAccess(Users currentUser) {
        boolean isDonor = currentUser != null && currentUser.getUserType() == UserType.DONOR;
        setNavVisibility(navHome, isDonor);

        if (currentUser != null && currentUser.getUserType() == UserType.ADMIN) {
            setNavVisibility(navUsers, true);
            setNavVisibility(navHospitals, true);
            setNavVisibility(navDonations, true);
            setNavVisibility(navDonationEvents, true);
            setNavVisibility(navInventory, true);
            setNavVisibility(navTransfers, true);
            setNavVisibility(navBloodTypes, true);
            setNavVisibility(navEligibility, true);
            setNavVisibility(navAlerts, true);
            setNavVisibility(navAuditLogs, true);
            return;
        }

        // Hidden for both roles requested in auth flow.
        setNavVisibility(navUsers, false);
        setNavVisibility(navHospitals, false);
        setNavVisibility(navBloodTypes, false);
        setNavVisibility(navAuditLogs, false);

        // Default: hospital staff set requested modules.
        setNavVisibility(navDonations, true);
        setNavVisibility(navDonationEvents, true);
        setNavVisibility(navInventory, true);
        setNavVisibility(navTransfers, true);
        setNavVisibility(navEligibility, true);
        setNavVisibility(navAlerts, true);

        if (currentUser != null && currentUser.getUserType() == UserType.DONOR) {
            // Donor access: Donations (own only in donations controller), events, eligibility, alerts.
            setNavVisibility(navInventory, false);
            setNavVisibility(navTransfers, false);
        }
    }

    private void configureSidebarHeader(Users currentUser) {
        if (currentUser == null) {
            if (brandSubtitleLabel != null) {
                brandSubtitleLabel.setText("Dashboard");
            }
            if (sidebarSectionLabel != null) {
                sidebarSectionLabel.setText("NAVIGATION");
            }
            if (profileNameLabel != null) {
                profileNameLabel.setText("User");
            }
            if (profileRoleLabel != null) {
                profileRoleLabel.setText("Member");
            }
            if (profileInitialLabel != null) {
                profileInitialLabel.setText("U");
            }
            return;
        }

        String displayName = ((currentUser.getFirst_name() != null ? currentUser.getFirst_name().trim() : "")
                + " "
                + (currentUser.getLast_name() != null ? currentUser.getLast_name().trim() : "")).trim();
        if (displayName.isEmpty()) {
            displayName = currentUser.getEmail() != null ? currentUser.getEmail() : "User";
        }

        String roleLabel = "Member";
        String dashboardSubtitle = "Dashboard";
        if (currentUser.getUserType() == UserType.ADMIN) {
            roleLabel = "Administrator";
            dashboardSubtitle = "Admin Dashboard";
        } else if (currentUser.getUserType() == UserType.HOSPITAL_STAFF) {
            roleLabel = "Hospital Staff";
            dashboardSubtitle = "Staff Dashboard";
        } else if (currentUser.getUserType() == UserType.DONOR) {
            roleLabel = "Donor";
            dashboardSubtitle = "Donor Dashboard";
        }

        if (brandSubtitleLabel != null) {
            brandSubtitleLabel.setText(dashboardSubtitle);
        }
        if (sidebarSectionLabel != null) {
            sidebarSectionLabel.setText("NAVIGATION");
        }
        if (profileNameLabel != null) {
            profileNameLabel.setText(displayName);
        }
        if (profileRoleLabel != null) {
            profileRoleLabel.setText(roleLabel);
        }
        if (profileInitialLabel != null) {
            profileInitialLabel.setText(displayName.substring(0, 1).toUpperCase());
        }
    }

    private void setNavVisibility(HBox navItem, boolean visible) {
        if (navItem == null) {
            return;
        }
        navItem.setVisible(visible);
        navItem.setManaged(visible);
    }

    /**
     * Public accessor so child controllers can load views into the content area
     * (e.g. navigating from one page to another programmatically).
     */
    public StackPane getContentArea() {
        return contentArea;
    }
}
