package tn.edu.esprit.Entities;

import java.sql.Timestamp;
import java.util.Objects;

public class BloodType {

    private String bloodTypeId;
    private String aboType;
    private String rhFactor;
    private boolean isUniversalDonor;
    private boolean isUniversalRecipient;
    private String compatibleDonors;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Default constructor
    public BloodType() {}

    // Constructor with essential fields
    public BloodType(String bloodTypeId, String aboType, String rhFactor) {
        this.bloodTypeId = bloodTypeId;
        this.aboType = aboType;
        this.rhFactor = rhFactor;
    }

    // Full constructor
    public BloodType(
        String bloodTypeId,
        String aboType,
        String rhFactor,
        boolean isUniversalDonor,
        boolean isUniversalRecipient,
        String compatibleDonors
    ) {
        this.bloodTypeId = bloodTypeId;
        this.aboType = aboType;
        this.rhFactor = rhFactor;
        this.isUniversalDonor = isUniversalDonor;
        this.isUniversalRecipient = isUniversalRecipient;
        this.compatibleDonors = compatibleDonors;
    }

    // Getters and Setters
    public String getBloodTypeId() {
        return bloodTypeId;
    }

    public void setBloodTypeId(String bloodTypeId) {
        this.bloodTypeId = bloodTypeId;
    }

    public String getAboType() {
        return aboType;
    }

    public void setAboType(String aboType) {
        this.aboType = aboType;
    }

    public String getRhFactor() {
        return rhFactor;
    }

    public void setRhFactor(String rhFactor) {
        this.rhFactor = rhFactor;
    }

    public boolean isUniversalDonor() {
        return isUniversalDonor;
    }

    public void setUniversalDonor(boolean universalDonor) {
        isUniversalDonor = universalDonor;
    }

    public boolean isUniversalRecipient() {
        return isUniversalRecipient;
    }

    public void setUniversalRecipient(boolean universalRecipient) {
        isUniversalRecipient = universalRecipient;
    }

    public String getCompatibleDonors() {
        return compatibleDonors;
    }

    public void setCompatibleDonors(String compatibleDonors) {
        this.compatibleDonors = compatibleDonors;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // toString method
    @Override
    public String toString() {
        return (
            "BloodType{" +
            "bloodTypeId='" +
            bloodTypeId +
            '\'' +
            ", aboType='" +
            aboType +
            '\'' +
            ", rhFactor='" +
            rhFactor +
            '\'' +
            ", isUniversalDonor=" +
            isUniversalDonor +
            ", isUniversalRecipient=" +
            isUniversalRecipient +
            ", compatibleDonors='" +
            compatibleDonors +
            '\'' +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            '}'
        );
    }

    // equals method
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BloodType bloodType = (BloodType) obj;
        return Objects.equals(bloodTypeId, bloodType.bloodTypeId);
    }

    // hashCode method
    @Override
    public int hashCode() {
        return bloodTypeId != null ? bloodTypeId.hashCode() : 0;
    }
}
