package tn.edu.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Donations {

    private String donationId;
    private String donorId;
    private String hospitalId;
    private String donationEventId;
    private String bloodTypeId;
    private LocalDateTime donationDate;
    private int unitsCollected;
    private BigDecimal volumeCollected;
    private String status;
    private Boolean screeningPassed;
    private String medicalNotes;
    private LocalDateTime createdAt;

    public Donations() {}

    public Donations(String donationId, String donorId, String hospitalId, String donationEventId,
                     String bloodTypeId, LocalDateTime donationDate, int unitsCollected,
                     BigDecimal volumeCollected, String status, Boolean screeningPassed,
                     String medicalNotes, LocalDateTime createdAt) {
        this.donationId = donationId;
        this.donorId = donorId;
        this.hospitalId = hospitalId;
        this.donationEventId = donationEventId;
        this.bloodTypeId = bloodTypeId;
        this.donationDate = donationDate;
        this.unitsCollected = unitsCollected;
        this.volumeCollected = volumeCollected;
        this.status = status;
        this.screeningPassed = screeningPassed;
        this.medicalNotes = medicalNotes;
        this.createdAt = createdAt;
    }

    // Getters & Setters

    public String getDonationId() { return donationId; }
    public void setDonationId(String donationId) { this.donationId = donationId; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getDonationEventId() { return donationEventId; }
    public void setDonationEventId(String donationEventId) { this.donationEventId = donationEventId; }

    public String getBloodTypeId() { return bloodTypeId; }
    public void setBloodTypeId(String bloodTypeId) { this.bloodTypeId = bloodTypeId; }

    public LocalDateTime getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDateTime donationDate) { this.donationDate = donationDate; }

    public int getUnitsCollected() { return unitsCollected; }
    public void setUnitsCollected(int unitsCollected) { this.unitsCollected = unitsCollected; }

    public BigDecimal getVolumeCollected() { return volumeCollected; }
    public void setVolumeCollected(BigDecimal volumeCollected) { this.volumeCollected = volumeCollected; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getScreeningPassed() { return screeningPassed; }
    public void setScreeningPassed(Boolean screeningPassed) { this.screeningPassed = screeningPassed; }

    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}