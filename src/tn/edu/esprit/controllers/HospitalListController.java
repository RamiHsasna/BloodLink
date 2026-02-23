package tn.edu.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.services.HospitalServiceImpl;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HospitalListController implements Initializable {

    @FXML private VBox cardsContainer;
    @FXML private VBox emptyState;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbCityFilter;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Button btnCreate;
    @FXML private Label lblTotalCount;
    @FXML private Label lblActiveCount;
    @FXML private Label lblInactiveCount;

    private final HospitalServiceImpl hospitalService = new HospitalServiceImpl();
    private List<Hospital> allHospitals;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up status filter options
        cbStatusFilter.setItems(FXCollections.observableArrayList("All Status", "Active", "Inactive"));
        cbStatusFilter.setValue("All Status");

        refreshData();
    }

    // ==================== DATA LOADING ====================

    /**
     * Reloads all hospitals from the database and rebuilds the entire view.
     */
    public void refreshData() {
        allHospitals = hospitalService.getAllHospitals();
        populateCityFilter();
        updateStats();
        applyFilters();
    }

    /**
     * Populates the city filter combo box with distinct cities from the hospital list.
     */
    private void populateCityFilter() {
        List<String> cities = allHospitals.stream()
                .map(Hospital::getCity)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        cities.add(0, "All Cities");

        String currentSelection = cbCityFilter.getValue();
        cbCityFilter.setItems(FXCollections.observableArrayList(cities));
        if (currentSelection != null && cities.contains(currentSelection)) {
            cbCityFilter.setValue(currentSelection);
        } else {
            cbCityFilter.setValue("All Cities");
        }
    }

    /**
     * Updates the stat chips at the top of the page.
     */
    private void updateStats() {
        int total = allHospitals.size();
        long active = allHospitals.stream().filter(Hospital::isActive).count();
        long inactive = total - active;

        lblTotalCount.setText(String.valueOf(total));
        lblActiveCount.setText(String.valueOf(active));
        lblInactiveCount.setText(String.valueOf(inactive));
    }

    // ==================== FILTERING & SEARCH ====================

    @FXML
    private void onSearchChanged() {
        applyFilters();
    }

    @FXML
    private void onCityFilterChanged() {
        applyFilters();
    }

    @FXML
    private void onStatusFilterChanged() {
        applyFilters();
    }

    /**
     * Applies search text, city filter, and status filter to the hospital list,
     * then rebuilds the card UI.
     */
    private void applyFilters() {
        String searchText = tfSearch.getText() != null ? tfSearch.getText().toLowerCase().trim() : "";
        String cityFilter = cbCityFilter.getValue();
        String statusFilter = cbStatusFilter.getValue();

        List<Hospital> filtered = allHospitals.stream()
                .filter(h -> {
                    // Search filter
                    if (!searchText.isEmpty()) {
                        boolean matchesName = h.getName() != null && h.getName().toLowerCase().contains(searchText);
                        boolean matchesCity = h.getCity() != null && h.getCity().toLowerCase().contains(searchText);
                        boolean matchesEmail = h.getEmail() != null && h.getEmail().toLowerCase().contains(searchText);
                        boolean matchesAddress = h.getAddress() != null && h.getAddress().toLowerCase().contains(searchText);
                        if (!matchesName && !matchesCity && !matchesEmail && !matchesAddress) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(h -> {
                    // City filter
                    if (cityFilter != null && !"All Cities".equals(cityFilter)) {
                        return cityFilter.equals(h.getCity());
                    }
                    return true;
                })
                .filter(h -> {
                    // Status filter
                    if (statusFilter != null && !"All Status".equals(statusFilter)) {
                        if ("Active".equals(statusFilter)) return h.isActive();
                        if ("Inactive".equals(statusFilter)) return !h.isActive();
                    }
                    return true;
                })
                .collect(Collectors.toList());

        buildCards(filtered);
    }

    // ==================== CARD BUILDING ====================

    /**
     * Rebuilds the card container with a card for each hospital.
     */
    private void buildCards(List<Hospital> hospitals) {
        // Remove everything except the emptyState node
        cardsContainer.getChildren().removeIf(node -> node != emptyState);

        if (hospitals.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);

            for (Hospital hospital : hospitals) {
                Node card = createHospitalCard(hospital);
                // Insert before the emptyState node
                int idx = cardsContainer.getChildren().indexOf(emptyState);
                if (idx >= 0) {
                    cardsContainer.getChildren().add(idx, card);
                } else {
                    cardsContainer.getChildren().add(card);
                }
            }
        }
    }

    /**
     * Creates a single hospital card node with all details and action buttons.
     */
    private Node createHospitalCard(Hospital hospital) {
        VBox card = new VBox();
        card.getStyleClass().add("hospital-card");
        card.setSpacing(0);

        // ---- Card Body ----
        VBox body = new VBox();
        body.getStyleClass().add("hospital-card-body");
        body.setSpacing(14);
        body.setPadding(new Insets(20, 22, 16, 22));

        // Top row: icon + name/city + status badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setSpacing(14);

        // Hospital icon circle
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("card-hospital-icon-wrapper");
        iconWrapper.setPrefSize(44, 44);
        iconWrapper.setMinSize(44, 44);
        iconWrapper.setMaxSize(44, 44);
        Label iconLabel = new Label("🏥");
        iconLabel.getStyleClass().add("card-hospital-icon");
        iconWrapper.getChildren().add(iconLabel);

        // Name + city
        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label nameLabel = new Label(hospital.getName() != null ? hospital.getName() : "Unnamed Hospital");
        nameLabel.getStyleClass().add("card-hospital-name");
        Label cityLabel = new Label(hospital.getCity() != null ? hospital.getCity() : "—");
        cityLabel.getStyleClass().add("card-hospital-city");
        nameBox.getChildren().addAll(nameLabel, cityLabel);

        // Status badge
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER);
        Label badgeText;
        if (hospital.isActive()) {
            badge.getStyleClass().add("status-badge-active");
            badgeText = new Label("● Active");
            badgeText.getStyleClass().add("status-badge-active-text");
        } else {
            badge.getStyleClass().add("status-badge-inactive");
            badgeText = new Label("● Inactive");
            badgeText.getStyleClass().add("status-badge-inactive-text");
        }
        badge.getChildren().add(badgeText);

        topRow.getChildren().addAll(iconWrapper, nameBox, badge);

        // Detail rows
        VBox details = new VBox(8);
        details.setPadding(new Insets(4, 0, 0, 0));

        if (hospital.getAddress() != null && !hospital.getAddress().isBlank()) {
            details.getChildren().add(createDetailRow("📍", hospital.getAddress()));
        }
        if (hospital.getPhone() != null && !hospital.getPhone().isBlank()) {
            details.getChildren().add(createDetailRow("📞", hospital.getPhone()));
        }
        if (hospital.getEmail() != null && !hospital.getEmail().isBlank()) {
            details.getChildren().add(createDetailRow("✉", hospital.getEmail()));
        }
        if (hospital.getLatitude() != null && hospital.getLongitude() != null) {
            String coords = hospital.getLatitude().toPlainString() + ", " + hospital.getLongitude().toPlainString();
            details.getChildren().add(createDetailRow("🌐", coords));
        }

        body.getChildren().addAll(topRow, details);

        // ---- Divider ----
        Separator divider = new Separator();
        divider.getStyleClass().add("card-divider");

        // ---- Card Actions Footer ----
        HBox actionsRow = new HBox();
        actionsRow.getStyleClass().add("card-actions-row");
        actionsRow.setAlignment(Pos.CENTER_RIGHT);
        actionsRow.setSpacing(10);
        actionsRow.setPadding(new Insets(12, 22, 14, 22));

        Button btnEdit = new Button("✏  Modify");
        btnEdit.getStyleClass().add("btn-card-edit");
        btnEdit.setOnAction(e -> onModifyHospital(hospital));

        Button btnDelete = new Button("🗑  Delete");
        btnDelete.getStyleClass().add("btn-card-delete");
        btnDelete.setOnAction(e -> onDeleteHospital(hospital));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Created date on the left
        Label dateLabel = new Label("");
        if (hospital.getCreatedAt() != null) {
            dateLabel.setText("Added " + hospital.getCreatedAt().toLocalDateTime().toLocalDate().toString());
        }
        dateLabel.getStyleClass().add("card-detail-text");
        dateLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #94a3b8;");

        actionsRow.getChildren().addAll(dateLabel, spacer, btnEdit, btnDelete);

        card.getChildren().addAll(body, divider, actionsRow);

        return card;
    }

    /**
     * Creates a single detail row with an icon and text.
     */
    private HBox createDetailRow(String icon, String text) {
        HBox row = new HBox(8);
        row.getStyleClass().add("card-detail-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("card-detail-icon");
        iconLabel.setMinWidth(20);

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("card-detail-text");
        textLabel.setWrapText(true);

        row.getChildren().addAll(iconLabel, textLabel);
        return row;
    }

    // ==================== CRUD ACTIONS ====================

    /**
     * Opens the hospital form modal in CREATE mode.
     */
    @FXML
    private void onCreateHospital() {
        openHospitalFormModal(null);
    }

    /**
     * Opens the hospital form modal in EDIT mode, pre-filled with the hospital's data.
     */
    private void onModifyHospital(Hospital hospital) {
        openHospitalFormModal(hospital);
    }

    /**
     * Shows a confirmation dialog before deleting a hospital.
     */
    private void onDeleteHospital(Hospital hospital) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Hospital");
        alert.setHeaderText("Delete \"" + hospital.getName() + "\"?");
        alert.setContentText(
                "This action cannot be undone. The hospital and all associated data will be permanently removed from the system."
        );

        // Style the dialog buttons
        ButtonType btnDelete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnCancel, btnDelete);

        // Apply danger styling to the delete button
        alert.showAndWait().ifPresent(response -> {
            if (response == btnDelete) {
                try {
                    hospitalService.supprimer(hospital.getHospitalId());
                    refreshData();
                } catch (Exception e) {
                    showErrorAlert("Delete Failed", "Could not delete hospital: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Opens the HospitalForm.fxml as a modal overlay on top of the current content.
     * If hospital is null, the form is in CREATE mode; otherwise, it's in EDIT mode.
     */
    private void openHospitalFormModal(Hospital hospital) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/edu/esprit/views/HospitalForm.fxml"));
            Node modalView = loader.load();

            // Get the form controller and set it up
            HospitalFormController formController = loader.getController();
            formController.setHospitalListController(this);

            if (hospital != null) {
                formController.setEditMode(hospital);
            }

            // Find the root StackPane (content area) to overlay the modal
            StackPane contentArea = findContentArea();
            if (contentArea != null) {
                formController.setParentContainer(contentArea);
                contentArea.getChildren().add(modalView);
            } else {
                // Fallback: try to find any ancestor StackPane
                System.err.println("Could not find content area StackPane for modal overlay.");
            }

        } catch (IOException e) {
            System.err.println("Failed to load HospitalForm.fxml");
            e.printStackTrace();
            showErrorAlert("Error", "Could not open the hospital form: " + e.getMessage());
        }
    }

        /**
     * Finds the main content area StackPane by its fx:id using scene lookup,
     * instead of traversing parents (which can hit ScrollPane internal StackPanes).
     */
    private StackPane findContentArea() {
        if (cardsContainer.getScene() != null) {
            Node found = cardsContainer.getScene().lookup("#contentArea");
            if (found instanceof StackPane) {
                return (StackPane) found;
            }
        }
        return null;
    }


    /**
     * Utility: shows an error alert dialog.
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
