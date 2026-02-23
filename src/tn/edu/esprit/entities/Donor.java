package tn.edu.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Donor {

    private String userId;
    private String firstName;
    private String lastName;
    private String bloodTypeId;

    private LocalDate lastDonationDate;
    private boolean isCurrentlyEligible;
    private Double latitude;
    private Double longitude;
    private int totalDonations;
    private LocalDateTime createdAt;

    public Donor() {}

    public Donor(String userId, String bloodTypeId,
                 LocalDate lastDonationDate, boolean isCurrentlyEligible,
                 Double latitude, Double longitude, int totalDonations,
                 LocalDateTime createdAt) {
        this.userId = userId;
        this.bloodTypeId = bloodTypeId;
        this.lastDonationDate = lastDonationDate;
        this.isCurrentlyEligible = isCurrentlyEligible;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalDonations = totalDonations;
        this.createdAt = createdAt;
    }

    public Donor(String userId, String firstName, String lastName, String bloodTypeId,
                 LocalDate lastDonationDate, boolean isCurrentlyEligible,
                 Double latitude, Double longitude, int totalDonations,
                 LocalDateTime createdAt) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bloodTypeId = bloodTypeId;
        this.lastDonationDate = lastDonationDate;
        this.isCurrentlyEligible = isCurrentlyEligible;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalDonations = totalDonations;
        this.createdAt = createdAt;
    }

    // Getters & Setters

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getBloodTypeId() { return bloodTypeId; }
    public void setBloodTypeId(String bloodTypeId) { this.bloodTypeId = bloodTypeId; }



    public LocalDate getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(LocalDate lastDonationDate) { this.lastDonationDate = lastDonationDate; }

    public boolean isCurrentlyEligible() { return isCurrentlyEligible; }
    public void setCurrentlyEligible(boolean currentlyEligible) { isCurrentlyEligible = currentlyEligible; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public int getTotalDonations() { return totalDonations; }
    public void setTotalDonations(int totalDonations) { this.totalDonations = totalDonations; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}