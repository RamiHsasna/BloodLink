package tn.edu.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.ServiceUser;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> userTypeFilter;
    @FXML private VBox usersContainer;
    @FXML private Text totalUsersLabel;
    @FXML private Text donorsCountLabel;
    @FXML private Text staffCountLabel;
    @FXML private Button addUserBtn;
    @FXML private StackPane contentArea;
    @FXML private VBox userManagementContent;
    @FXML private Button usersNavBtn;
    @FXML private Button eligibilityNavBtn;

    private ServiceUser serviceUser;
    private List<Users> allUsers;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private Parent eligibilityContent;

    @FXML
    public void initialize() {
        serviceUser = new ServiceUser();
        
        // Initialize filter combo box
        userTypeFilter.getItems().addAll("All User Types", "DONOR", "HOSPITAL_STAFF");
        userTypeFilter.setValue("All User Types");
        
        loadUsers();
        
        // Show user management by default
        showUserManagement();
    }

    @FXML
    private void handleUsersNav() {
        showUserManagement();
    }

    @FXML
    private void handleEligibilityNav() {
        showEligibilityManagement();
    }

    private void showUserManagement() {
        // Update navigation buttons
        usersNavBtn.getStyleClass().add("nav-button-active");
        eligibilityNavBtn.getStyleClass().remove("nav-button-active");
        
        // Show user management content
        userManagementContent.setVisible(true);
        userManagementContent.setManaged(true);
        
        if (eligibilityContent != null) {
            eligibilityContent.setVisible(false);
            eligibilityContent.setManaged(false);
        }
    }

    private void showEligibilityManagement() {
        // Update navigation buttons
        usersNavBtn.getStyleClass().remove("nav-button-active");
        eligibilityNavBtn.getStyleClass().add("nav-button-active");
        
        // Hide user management content
        userManagementContent.setVisible(false);
        userManagementContent.setManaged(false);
        
        // Load eligibility content if not already loaded
        if (eligibilityContent == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("DonorEligibilityDashboard.fxml"));
                eligibilityContent = loader.load();
                contentArea.getChildren().add(eligibilityContent);
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Could not load donor eligibility interface: " + e.getMessage());
                return;
            }
        }
        
        eligibilityContent.setVisible(true);
        eligibilityContent.setManaged(true);
    }

    private void loadUsers() {
        allUsers = serviceUser.getAll(null);
        displayUsers(allUsers);
        updateStats();
    }

    private void displayUsers(List<Users> users) {
        usersContainer.getChildren().clear();
        
        if (users.isEmpty()) {
            Label emptyLabel = new Label("No users found");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 40px;");
            usersContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Users user : users) {
            usersContainer.getChildren().add(createUserCard(user));
        }
    }

    private VBox createUserCard(Users user) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card");
        card.setPadding(new Insets(20));

        // Header with Avatar and Name
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        Region avatar = new Region();
        avatar.getStyleClass().add("user-avatar");
        avatar.setPrefSize(50, 50);
        avatar.setMinSize(50, 50);
        avatar.setMaxSize(50, 50);

        // User Info
        VBox userInfo = new VBox(5);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        Text nameText = new Text(user.getFirst_name() + " " + user.getLast_name());
        nameText.getStyleClass().add("user-name");

        Text emailText = new Text(user.getEmail());
        emailText.getStyleClass().add("user-email");

        userInfo.getChildren().addAll(nameText, emailText);

        // User Type Badge
        Label typeBadge = new Label(formatUserType(user.getUserType()));
        typeBadge.getStyleClass().add("user-type-badge");
        if (user.getUserType() == UserType.DONOR) {
            typeBadge.getStyleClass().add("badge-donor");
        } else {
            typeBadge.getStyleClass().add("badge-staff");
        }

        header.getChildren().addAll(avatar, userInfo, typeBadge);

        // Details Grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);

        // Phone
        HBox phoneBox = createDetailItem("☎", user.getPhone() != null ? user.getPhone() : "N/A");
        detailsGrid.add(phoneBox, 0, 0);

        // User ID
        HBox idBox = createDetailItem("🆔", user.getId().substring(0, Math.min(8, user.getId().length())) + "...");
        detailsGrid.add(idBox, 1, 0);

        // Created At
        HBox dateBox = createDetailItem("📅", "Added " + user.getCreatedAt().format(dateFormatter));
        detailsGrid.add(dateBox, 0, 1, 2, 1);

        // Action Buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button modifyBtn = new Button("Modify");
        modifyBtn.getStyleClass().add("btn-modify");
        modifyBtn.setOnAction(e -> handleModifyUser(user));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-delete");
        deleteBtn.setOnAction(e -> handleDeleteUser(user));

        actionButtons.getChildren().addAll(modifyBtn, deleteBtn);

        card.getChildren().addAll(header, new Separator(), detailsGrid, actionButtons);
        
        return card;
    }

    private HBox createDetailItem(String icon, String text) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Text iconText = new Text(icon);
        iconText.getStyleClass().add("detail-icon");

        Text valueText = new Text(text);
        valueText.getStyleClass().add("detail-text");

        box.getChildren().addAll(iconText, valueText);
        return box;
    }

    private String formatUserType(UserType type) {
        return type == UserType.DONOR ? "Donor" : "Hospital Staff";
    }

    private void updateStats() {
        totalUsersLabel.setText(String.valueOf(allUsers.size()));
        
        long donorsCount = allUsers.stream()
                .filter(u -> u.getUserType() == UserType.DONOR)
                .count();
        donorsCountLabel.setText(String.valueOf(donorsCount));
        
        long staffCount = allUsers.stream()
                .filter(u -> u.getUserType() == UserType.HOSPITAL_STAFF)
                .count();
        staffCountLabel.setText(String.valueOf(staffCount));
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedType = userTypeFilter.getValue();

        List<Users> filtered = allUsers.stream()
                .filter(user -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            user.getFirst_name().toLowerCase().contains(searchText) ||
                            user.getLast_name().toLowerCase().contains(searchText) ||
                            user.getEmail().toLowerCase().contains(searchText) ||
                            (user.getPhone() != null && user.getPhone().contains(searchText));

                    boolean matchesType = selectedType.equals("All User Types") ||
                            user.getUserType().name().equals(selectedType);

                    return matchesSearch && matchesType;
                })
                .collect(Collectors.toList());

        displayUsers(filtered);
    }

    @FXML
    private void handleFilter() {
        handleSearch();
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserDialog.fxml"));
            Parent root = loader.load();
            
            UserDialogController controller = loader.getController();
            controller.setMode(UserDialogController.Mode.ADD);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add User");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            
            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadUsers();
            });
            
            dialogStage.showAndWait();
            loadUsers();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open add user dialog: " + e.getMessage());
        }
    }

    private void handleModifyUser(Users user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserDialog.fxml"));
            Parent root = loader.load();
            
            UserDialogController controller = loader.getController();
            controller.setMode(UserDialogController.Mode.EDIT);
            controller.setUser(user);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit User");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.getScene().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            
            controller.setDialogStage(dialogStage);
            controller.setOnSave(() -> {
                loadUsers();
            });
            
            dialogStage.showAndWait();
            loadUsers();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open edit user dialog: " + e.getMessage());
        }
    }

    private void handleDeleteUser(Users user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete " + user.getFirst_name() + " " + user.getLast_name() + "?");
        alert.setContentText("This action cannot be undone. Are you sure you want to delete this user?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceUser.supprimer(user.getId());
            loadUsers();
            showAlert("Success", "User deleted successfully!");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
