package tn.edu.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.services.QRCodeGenerator;

import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import java.io.File;

public class DonationQRController {

    @FXML private ImageView qrImageView;
    @FXML private Text donationIdText;
    @FXML private Text donorIdText;
    @FXML private Text bloodTypeText;
    @FXML private Text statusText;

    private Image qrImage;

    public void setDonation(Donations donation) {
        // Build QR content
        String content = "Donation ID: " + donation.getDonationId() + "\n" +
                "Donor ID: "    + donation.getDonorId()    + "\n" +
                "Blood Type: "  + donation.getBloodTypeId()+ "\n" +
                "Hospital: "    + donation.getHospitalId() + "\n" +
                "Units: "       + donation.getUnitsCollected() + "\n" +
                "Status: "      + donation.getStatus()     + "\n" +
                "Date: "        + (donation.getDonationDate() != null ?
                donation.getDonationDate().toString() : "N/A");

        // Generate QR code
        qrImage = QRCodeGenerator.generateQRCode(content, 250, 250);
        if (qrImage != null) {
            qrImageView.setImage(qrImage);
        }

        // Fill labels
        donationIdText.setText("Donation ID: " + donation.getDonationId());
        donorIdText.setText("Donor: "          + donation.getDonorId());
        bloodTypeText.setText("Blood Type: "   + donation.getBloodTypeId());
        statusText.setText("Status: "          + donation.getStatus());
    }

    @FXML
    private void handleSave() {
        if (qrImage == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save QR Code");
        fileChooser.setInitialFileName("donation_qr.png");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fileChooser.showSaveDialog(qrImageView.getScene().getWindow());
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(qrImage, null), "png", file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) qrImageView.getScene().getWindow();
        stage.close();
    }
}