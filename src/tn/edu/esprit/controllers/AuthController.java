package tn.edu.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.services.AppSession;
import tn.edu.esprit.services.ServiceUser;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML private Canvas logoCanvas;
    @FXML private Canvas illustrationCanvas;

    @FXML private Button tabSignIn;
    @FXML private Button tabSignUp;

    @FXML private VBox  signInPane;
    @FXML private TextField     signInEmail;
    @FXML private PasswordField signInPassword;
    @FXML private TextField     signInPasswordVisible;

    @FXML private VBox signUpPane;
    @FXML private VBox step0Pane;
    @FXML private VBox step1Pane;
    @FXML private VBox step2Pane;


    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField signUpEmail;
    @FXML private TextField phone;

    @FXML private ComboBox<String> bloodTypeCombo;

    @FXML private PasswordField newPassword;
    @FXML private TextField     newPasswordVisible;
    @FXML private PasswordField confirmPassword;
    @FXML private TextField     confirmPasswordVisible;

    @FXML private CheckBox termsCheck;

    @FXML private Region dot0;
    @FXML private Region dot1;

    private String selectedBloodType;

    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        drawLogo();
        drawIllustration();
        if (bloodTypeCombo != null) {
            bloodTypeCombo.getItems().setAll(Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    private void drawLogo() {
        GraphicsContext gc = logoCanvas.getGraphicsContext2D();
        double w = logoCanvas.getWidth();
        double h = logoCanvas.getHeight();

        // Circle background
        gc.setFill(Color.web("#ffffff33"));
        gc.fillOval(0, 0, w, h);

        // Blood drop (white)
        gc.setFill(Color.web("#ffffffee"));
        double[] xs = dropX(w / 2, w * 0.28, h * 0.52, h * 0.20);
        double[] ys = dropY(w / 2, w * 0.28, h * 0.52, h * 0.20);
        gc.fillPolygon(xs, ys, xs.length);

        // Plus cross
        gc.setStroke(Color.web("#b85c52"));
        gc.setLineWidth(2.0);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        double cx = w / 2, cy = h * 0.62;
        double arm = w * 0.16;
        gc.strokeLine(cx - arm, cy, cx + arm, cy);
        gc.strokeLine(cx, cy - arm, cx, cy + arm);
    }

    /** Simple teardrop approximation using a polygon */
    private double[] dropX(double cx, double r, double bottom, double tipOffset) {
        int pts = 32;
        double[] xs = new double[pts];
        for (int i = 0; i < pts; i++) {
            double t = (double) i / pts;
            double angle = Math.PI + t * 2 * Math.PI;
            double squeeze = 0.6 + 0.4 * Math.sin(t * Math.PI);
            xs[i] = cx + Math.cos(angle) * r * squeeze;
        }
        return xs;
    }

    private double[] dropY(double cx, double r, double bottom, double tipOffset) {
        int pts = 32;
        double[] ys = new double[pts];
        for (int i = 0; i < pts; i++) {
            double t = (double) i / pts;
            double angle = Math.PI + t * 2 * Math.PI;
            double cy = bottom - r;
            ys[i] = cy + Math.sin(angle) * r - tipOffset * (1 - Math.sin(t * Math.PI));
        }
        return ys;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Illustration canvas
    // ─────────────────────────────────────────────────────────────────────
    private void drawIllustration() {
        GraphicsContext gc = illustrationCanvas.getGraphicsContext2D();
        double W = illustrationCanvas.getWidth();
        double H = illustrationCanvas.getHeight();

        gc.clearRect(0, 0, W, H);

        // Concentric rings
        drawRing(gc, W / 2, H / 2, W * 0.44, Color.web("#ffffff0d"));
        drawRing(gc, W / 2, H / 2, W * 0.30, Color.web("#ffffff14"));

        // Hospital building
        double hx = W * 0.32, hy = H * 0.22, hw = W * 0.36, hh = H * 0.68;
        gc.setFill(Color.web("#ffffff1a"));
        gc.fillRoundRect(hx, hy, hw, hh, 10, 10);
        gc.setStroke(Color.web("#ffffff40"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(hx, hy, hw, hh, 10, 10);

        // Roof
        double rx = hx + hw * 0.12, ry = hy - H * 0.12, rw = hw * 0.76, rh = H * 0.14;
        gc.setFill(Color.web("#ffffff10"));
        gc.fillRoundRect(rx, ry, rw, rh, 8, 8);
        gc.setStroke(Color.web("#ffffff33"));
        gc.strokeRoundRect(rx, ry, rw, rh, 8, 8);

        // Cross on roof
        double crossCx = rx + rw / 2, crossCy = ry + rh / 2;
        gc.setStroke(Color.web("#ffffffdd"));
        gc.setLineWidth(3.0);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(crossCx - rw * 0.14, crossCy, crossCx + rw * 0.14, crossCy);
        gc.strokeLine(crossCx, crossCy - rh * 0.4, crossCx, crossCy + rh * 0.4);

        // Windows (2 rows, 3 cols)
        double[] winCols = {hx + hw * 0.12, hx + hw * 0.42, hx + hw * 0.72};
        double[] winRows = {hy + hh * 0.20, hy + hh * 0.46};
        double ww = hw * 0.18, wh = hh * 0.14;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                boolean lit = (r + c) % 2 == 0;
                gc.setFill(lit ? Color.web("#ffe8a030") : Color.web("#ffffff14"));
                gc.fillRoundRect(winCols[c], winRows[r], ww, wh, 4, 4);
                gc.setStroke(Color.web("#ffffff2e"));
                gc.setLineWidth(1.0);
                gc.strokeRoundRect(winCols[c], winRows[r], ww, wh, 4, 4);
            }
        }

        // Door
        double dx = hx + hw * 0.38, dy = hy + hh * 0.70, dw = hw * 0.22, dh = hh * 0.30;
        gc.setFill(Color.web("#ffffff10"));
        gc.fillRoundRect(dx, dy, dw, dh, 6, 6);
        gc.setStroke(Color.web("#ffffff35"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(dx, dy, dw, dh, 6, 6);
        gc.setFill(Color.web("#ffffff50"));
        gc.fillOval(dx + dw * 0.75, dy + dh * 0.48, 5, 5);

        // Left donor figure
        drawDonor(gc, W * 0.10, H * 0.42);

        // Right donor figure
        drawDonor(gc, W * 0.86, H * 0.42);

        // Dashed connector lines
        gc.setStroke(Color.web("#ffffff2e"));
        gc.setLineWidth(1.2);
        gc.setLineDashes(6, 4);
        drawCurve(gc, W * 0.16, H * 0.56, W * 0.28, H * 0.50, W * 0.32, H * 0.56);
        drawCurve(gc, W * 0.68, H * 0.56, W * 0.72, H * 0.50, W * 0.84, H * 0.56);
        gc.setLineDashes(null);

        // Floating drops
        drawMiniDrop(gc, W * 0.06, H * 0.14, 8);
        drawMiniDrop(gc, W * 0.90, H * 0.10, 7);
        drawMiniDrop(gc, W * 0.50, H * 0.04, 9);
        drawMiniDrop(gc, W * 0.28, H * 0.08, 6);
        drawMiniDrop(gc, W * 0.72, H * 0.06, 6);

        // ECG line
        drawECG(gc, W, H);
    }

    private void drawRing(GraphicsContext gc, double cx, double cy, double r, Color col) {
        gc.setStroke(col);
        gc.setLineWidth(1.0);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawDonor(GraphicsContext gc, double cx, double cy) {
        double r = 18;
        // Head circle
        gc.setFill(Color.web("#ffffff18"));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        gc.setStroke(Color.web("#ffffff38"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // Blood drop inside head
        gc.setFill(Color.web("#ffc8c8cc"));
        double dr = 6;
        gc.fillOval(cx - dr, cy - dr, dr * 2, dr * 2);

        // Body arc
        gc.setStroke(Color.web("#ffffff28"));
        gc.setLineWidth(1.4);
        gc.strokeArc(cx - 26, cy + r, 52, 40, 0, 180, javafx.scene.shape.ArcType.OPEN);
    }

    private void drawMiniDrop(GraphicsContext gc, double cx, double cy, double r) {
        gc.setFill(Color.web("#ffc8c878"));
        gc.beginPath();
        gc.moveTo(cx, cy - r * 1.4);
        gc.bezierCurveTo(cx + r, cy - r * 0.3, cx + r, cy + r * 0.5, cx, cy + r);
        gc.bezierCurveTo(cx - r, cy + r * 0.5, cx - r, cy - r * 0.3, cx, cy - r * 1.4);
        gc.closePath();
        gc.fill();
    }

    private void drawCurve(GraphicsContext gc, double x1, double y1, double cx, double cy, double x2, double y2) {
        gc.beginPath();
        gc.moveTo(x1, y1);
        gc.quadraticCurveTo(cx, cy, x2, y2);
        gc.stroke();
    }

    private void drawECG(GraphicsContext gc, double W, double H) {
        double y = H * 0.90;
        double[] px = {W*0.03, W*0.12, W*0.17, W*0.22, W*0.27, W*0.32,
                        W*0.68, W*0.73, W*0.78, W*0.83, W*0.88, W*0.97};
        double[] py = {y, y, y-H*0.08, y+H*0.08, y-H*0.06, y,
                        y, y-H*0.08, y+H*0.08, y-H*0.06, y, y};

        gc.setStroke(Color.web("#ffffff33"));
        gc.setLineWidth(1.8);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.setLineDashes(null);
        gc.beginPath();
        gc.moveTo(px[0], py[0]);
        for (int i = 1; i < px.length; i++) gc.lineTo(px[i], py[i]);
        gc.stroke();
    }

   // ─────────────────────────────────────────────────────────────────────

    @FXML private void showSignIn() {
        setPane(signInPane, true);
        setPane(signUpPane, false);
        tabSignIn.getStyleClass().add("tab-active");
        tabSignUp.getStyleClass().remove("tab-active");
    }

    @FXML private void showSignUp() {
        setPane(signInPane, false);
        setPane(signUpPane, true);
        tabSignUp.getStyleClass().add("tab-active");
        tabSignIn.getStyleClass().remove("tab-active");
    }

    private void setPane(VBox pane, boolean show) { //function to show or hide pane
        pane.setVisible(show); //visually show/hide
        pane.setManaged(show); //include/exclude from layout
        if (show) { //animation
            FadeTransition ft = new FadeTransition(Duration.millis(200), pane);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
    }


    @FXML private void goToStep1() {
        
        if (firstName.getText() == null || firstName.getText().trim().isEmpty()){
            showAlert("Please fill in first name");
            return;
        }
        if (lastName.getText() == null || lastName.getText().trim().isEmpty()){
            showAlert("Please fill in last name");
            return;
        }
        if (signUpEmail.getText() == null || signUpEmail.getText().trim().isEmpty()){
            showAlert("Please fill in email");
            return;
        }
        if (bloodTypeCombo == null || bloodTypeCombo.getValue() == null || bloodTypeCombo.getValue().isEmpty()) {
            showAlert("Please select a blood type.");
            return;
        }
        if (!termsCheck.isSelected()) {
            showAlert("Please accept the Terms of Service and Privacy Policy.");
            return;
        }

        selectedBloodType = bloodTypeCombo.getValue();

        step0Pane.setVisible(false); step0Pane.setManaged(false);
        step1Pane.setVisible(true);  step1Pane.setManaged(true);
        dot0.getStyleClass().remove("dot-active");
        dot1.getStyleClass().add("dot-active");
    }

    @FXML private void goToStep0() {
        step1Pane.setVisible(false); step1Pane.setManaged(false);
        step0Pane.setVisible(true);  step0Pane.setManaged(true);
        dot1.getStyleClass().remove("dot-active");
        dot0.getStyleClass().add("dot-active");
    }
    @FXML void goToStep2() {
        step0Pane.setVisible(false); step0Pane.setManaged(false);
        step1Pane.setVisible(false); step1Pane.setManaged(false);
        step2Pane.setVisible(true);  step2Pane.setManaged(true);
        dot1.getStyleClass().remove("dot-active");
        dot0.getStyleClass().add("dot-active");

    }


    @FXML private void toggleSignInPassword() {
        togglePwVisibility(signInPassword, signInPasswordVisible);
    }

    @FXML private void toggleNewPassword() {
        togglePwVisibility(newPassword, newPasswordVisible);
    }

    @FXML private void toggleConfirmPassword() {
        togglePwVisibility(confirmPassword, confirmPasswordVisible);
    }

    private void togglePwVisibility(PasswordField pf, TextField tf) {
        if (pf.isVisible()) {
            tf.setText(pf.getText());
            pf.setVisible(false);  pf.setManaged(false);
            tf.setVisible(true);   tf.setManaged(true);
        } else {
            pf.setText(tf.getText());
            tf.setVisible(false);  tf.setManaged(false);
            pf.setVisible(true);   pf.setManaged(true);
        }
    }

    //////////////////////////////////////////////////////////////////////////

    @FXML private void handleSignIn() {
        String email    = signInEmail.getText().trim();
        String password = signInPassword.isVisible()   // get password from the visible field
                          ? signInPassword.getText() // or from the visible text field if password is currently hidden
                          : signInPasswordVisible.getText();  

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        Users loggedIn = serviceUser.authenticate(email, password);
        if (loggedIn == null) {
            showAlert("Invalid email or password.");
            return;
        }

        AppSession.setCurrentUser(loggedIn);

        try {
            Parent dashboardRoot = FXMLLoader.load(getClass().getResource("/tn/edu/esprit/views/MainDashboard.fxml"));
            Stage stage = (Stage) signInPane.getScene().getWindow();
            Scene scene = new Scene(dashboardRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setTitle("Dashboard");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert("Login succeeded but dashboard could not be opened: " + e.getMessage());
        }
    }

    ///////////////////////////////////////////////////////////////////////

    @FXML private void handleCreateAccount() {
        String firstNameValue = firstName.getText() != null ? firstName.getText().trim() : "";
        String lastNameValue = lastName.getText() != null ? lastName.getText().trim() : "";
        String emailValue = signUpEmail.getText() != null ? signUpEmail.getText().trim() : "";
        String phoneValue = phone.getText() != null ? phone.getText().trim() : "";
        String bloodTypeValue = bloodTypeCombo != null && bloodTypeCombo.getValue() != null
                ? bloodTypeCombo.getValue().trim()
                : (selectedBloodType != null ? selectedBloodType.trim() : "");

        if (firstNameValue.isEmpty() || lastNameValue.isEmpty() || emailValue.isEmpty()) {
            showAlert("Please fill in first name, last name, and email.");
            return;
        }

        if (!emailValue.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showAlert("Please enter a valid email address.");
            return;
        }

        if (bloodTypeValue.isEmpty()) {
            showAlert("Please select a blood type.");
            return;
        }

        if (!termsCheck.isSelected()) {
            showAlert("Please accept the Terms of Service and Privacy Policy.");
            return;
        }
        String pw  = newPassword.isVisible() ? newPassword.getText() : newPasswordVisible.getText();
        String cpw = confirmPassword.isVisible() ? confirmPassword.getText() : confirmPasswordVisible.getText();
        if (!pw.equals(cpw)) {
            showAlert("Passwords do not match.");
            return;
        }
        if (pw.length() < 8) {
            showAlert("Password must be at least 8 characters.");
            return;
        }

        if (serviceUser.isEmailTaken(emailValue)) {
            showAlert("An account with this email already exists.");
            return;
        }

        boolean created = serviceUser.registerDonorAccount(
                firstNameValue,
                lastNameValue,
                emailValue,
                phoneValue,
                pw,
                bloodTypeValue
        );

        if (!created) {
            showAlert("Unable to create account right now. Please try again.");
            return;
        }

        Alert success = new Alert(Alert.AlertType.INFORMATION, "Account created successfully.", ButtonType.OK);
        success.setHeaderText(null);
        success.showAndWait();

        firstName.clear();
        lastName.clear();
        signUpEmail.clear();
        phone.clear();
        if (bloodTypeCombo != null) {
            bloodTypeCombo.getSelectionModel().clearSelection();
        }
        selectedBloodType = null;
        newPassword.clear();
        newPasswordVisible.clear();
        confirmPassword.clear();
        confirmPasswordVisible.clear();
        termsCheck.setSelected(false);

        signInEmail.setText(emailValue);
        showSignIn();
    }

/////////////////////////////////////////////////////////
    @FXML private void handleGoogle() {
        // TODO: integrate Google OAuth
        System.out.println("Google OAuth triggered");
    }

    @FXML private void forgotPassword() {
        // TODO: open forgot-password dialog
        System.out.println("Forgot password");
    }
///////////////////////////////////////////////////////

    @FXML private void openTerms()   { System.out.println("Open terms"); }
    @FXML private void openPrivacy() { System.out.println("Open privacy"); }

    
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }


}
