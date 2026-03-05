package tn.edu.esprit.services;

import tn.edu.esprit.entities.Alert;
import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.entities.DonorAlert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmergencyMatchingService {

    private final ServiceDonor donorService;
    private final DonorAlertServiceImpl donorAlertService;
    private final MetierIAService metierIAService;

    public static class MatchResult {
        public final String generatedMessage;
        public final int matchedDonorCount;
        public final boolean success;
        public final String errorMessage;
        public final boolean generatedByIa;

        public MatchResult(String generatedMessage, int matchedDonorCount, boolean success, String errorMessage,
                boolean generatedByIa) {
            this.generatedMessage = generatedMessage;
            this.matchedDonorCount = matchedDonorCount;
            this.success = success;
            this.errorMessage = errorMessage;
            this.generatedByIa = generatedByIa;
        }
    }

    @FunctionalInterface
    public interface MatchCallback {
        void onResult(MatchResult result);
    }

    public EmergencyMatchingService() {
        this.donorService = new ServiceDonor();
        this.donorAlertService = new DonorAlertServiceImpl();
        this.metierIAService = new MetierIAService();
    }

    public void triggerMatchingAsync(Alert alert, MatchCallback callback) {
        new Thread(() -> {
            try {
                System.out.println("Demarrage de la correspondance d'urgence pour l'alerte : " + alert.getTitle());

                List<Donor> allDonors = donorService.getAll(new Donor());
                List<Donor> compatibleDonors = filterCompatibleDonors(allDonors, alert.getBloodTypeId());
                System.out.println(compatibleDonors.size() + " donneur(s) compatible(s) trouve(s).");

                String generatedMessage = metierIAService.generateEmergencyMessage(alert, compatibleDonors.size());
                if (generatedMessage == null || generatedMessage.isBlank()) {
                    throw new IllegalStateException("metier_ia: message IA vide.");
                }
                boolean generatedByIa = true;
                System.out.println("Message genere (metier_ia) : "
                        + generatedMessage);

                for (Donor donor : compatibleDonors) {
                    DonorAlert donorAlert = new DonorAlert();
                    donorAlert.setDonorAlertId(UUID.randomUUID().toString());
                    donorAlert.setAlertId(alert.getAlertId());
                    donorAlert.setDonorId(donor.getUserId());
                    donorAlert.setNotificationSentAt(java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    donorAlertService.ajouter(donorAlert);
                }

                MatchResult result = new MatchResult(generatedMessage, compatibleDonors.size(), true, null,
                        generatedByIa);
                if (callback != null) {
                    javafx.application.Platform.runLater(() -> callback.onResult(result));
                }
            } catch (Exception e) {
                System.err.println("Erreur pendant la correspondance : " + e.getMessage());
                e.printStackTrace();
                MatchResult result = new MatchResult(null, 0, false, e.getMessage(), false);
                if (callback != null) {
                    javafx.application.Platform.runLater(() -> callback.onResult(result));
                }
            }
        }).start();
    }

    public void triggerMatchingAsync(Alert alert, Runnable onCompleteCallback) {
        triggerMatchingAsync(alert, result -> {
            if (onCompleteCallback != null) {
                onCompleteCallback.run();
            }
        });
    }

    private List<Donor> filterCompatibleDonors(List<Donor> allDonors, String requiredBloodType) {
        List<Donor> compatible = new ArrayList<>();
        for (Donor donor : allDonors) {
            String donorBloodType = donor.getBloodTypeId() != null ? donor.getBloodTypeId().trim() : "";
            if ((donorBloodType.equalsIgnoreCase(requiredBloodType) || donorBloodType.equalsIgnoreCase("O-"))
                    && donor.isCurrentlyEligible()) {
                compatible.add(donor);
            }
        }
        return compatible;
    }

}
