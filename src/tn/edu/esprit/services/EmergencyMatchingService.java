package tn.edu.esprit.services;

import tn.edu.esprit.entities.Alert;
import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.entities.DonorAlert;
import tn.edu.esprit.entities.Hospital;

import java.time.LocalDateTime;
import java.util.*;

public class EmergencyMatchingService {

    private final ServiceDonor donorService;
    private final DonorAlertServiceImpl donorAlertService;
    private final MetierIAService metierIAService;
    private final HospitalServiceImpl hospitalService;

    private static final Map<String, Set<String>> BLOOD_COMPATIBILITY_MAP = Map.of(
            "O-", Set.of("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"),
            "O+", Set.of("O+", "A+", "B+", "AB+"),
            "A-", Set.of("A-", "A+", "AB-", "AB+"),
            "A+", Set.of("A+", "AB+"),
            "B-", Set.of("B-", "B+", "AB-", "AB+"),
            "B+", Set.of("B+", "AB+"),
            "AB-", Set.of("AB-", "AB+"),
            "AB+", Set.of("AB+")
    );

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
        this.hospitalService = new HospitalServiceImpl();
    }

    public void triggerMatchingAsync(Alert alert, MatchCallback callback) {
        new Thread(() -> {
            try {
                System.out.println("Demarrage de la correspondance d'urgence pour l'alerte : " + alert.getTitle());

                List<Donor> allDonors = donorService.getAll(new Donor());
                List<Donor> compatibleDonors = filterCompatibleDonors(allDonors, alert);
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
                    donorAlert.setNotified(true);
                    donorAlert.setRead(false);
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



    private List<Donor> filterCompatibleDonors(List<Donor> allDonors, Alert alert) {
        List<Donor> compatible = new ArrayList<>();
        String requiredBloodType = alert.getBloodTypeId() != null ? alert.getBloodTypeId().trim() : "";
        Hospital hospital = resolveHospital(alert.getHospitalId());
        Double hospitalLat = hospital != null && hospital.getLatitude() != null
            ? hospital.getLatitude().doubleValue()
            : null;
        Double hospitalLon = hospital != null && hospital.getLongitude() != null
            ? hospital.getLongitude().doubleValue()
            : null;
        int radiusKm = alert.getTargetRadiusKm();
        boolean useRadius = radiusKm > 0 && hospitalLat != null && hospitalLon != null;
        if (radiusKm > 0 && !useRadius) {
            throw new IllegalStateException("Coordonnees hopital indisponibles pour appliquer le rayon cible.");
        }

        for (Donor donor : allDonors) {
            String donorBloodType = donor.getBloodTypeId() != null ? donor.getBloodTypeId().trim() : "";
            if (!donor.isCurrentlyEligible()) {
                continue;
            }
            if (! isBloodTypeCompatible(donorBloodType,  alert.getBloodTypeId())) {
                continue;
            }
            if (useRadius) {
                Double donorLat = donor.getLatitude();
                Double donorLon = donor.getLongitude();
                if (donorLat == null || donorLon == null) {
                    continue;
                }
                double distance = haversineKm(hospitalLat, hospitalLon, donorLat, donorLon);
                if (distance > radiusKm) {
                    continue;
                }
            }
            compatible.add(donor);
        }
        return compatible;
    }

    private boolean isBloodTypeCompatible(String donorBloodType, String requiredBloodType) {
        String donorType = donorBloodType != null ? donorBloodType.trim().toUpperCase() : "";
        String requiredType = requiredBloodType != null ? requiredBloodType.trim().toUpperCase() : "";
        if (donorType.isEmpty() || requiredType.isEmpty()) {
            return false;
        }
        Set<String> compatibleRecipients = BLOOD_COMPATIBILITY_MAP.get(donorType);

        // mohamed = doneur  O-
        // if donorType = "O-" ==> compatibleRecipients = {"O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"}
        // ==> mohamed ynejjem ya3ti eddam mte3ou lel 3bed hekom lkol
        // if bloodType of the Alert exist in compatibleRecipients ==> donnor ynejjem yetbarra3
        // ==> l'aret lezemha todhhor lel donor hedha (mohamed)

        return compatibleRecipients != null && compatibleRecipients.contains(requiredType);
    }


    private Hospital resolveHospital(String hospitalId) {
        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            return null;
        }
        try {
            return hospitalService.getHospitalById(java.util.UUID.fromString(hospitalId.trim()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

}
