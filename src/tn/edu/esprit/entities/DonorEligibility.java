package tn.edu.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class DonorEligibility {

    private UUID donorEligibilityId;   // donor_eligibility_id
    private String donorId;            // donor_id
    private Boolean isCurrentlyEligible;
    private Integer daysUntilEligible;
    private LocalDate lastCalculatedAt;
    private BigDecimal latitudeCache;
    private BigDecimal longitudeCache;
    private String bloodTypeCache;

    public DonorEligibility() {
    }

    public DonorEligibility(UUID donorEligibilityId, String donorId, Boolean isCurrentlyEligible, Integer daysUntilEligible,
                            LocalDate lastCalculatedAt, BigDecimal latitudeCache, BigDecimal longitudeCache, String bloodTypeCache) {
        this.donorEligibilityId = donorEligibilityId;
        this.donorId = donorId;
        this.isCurrentlyEligible = isCurrentlyEligible;
        this.daysUntilEligible = daysUntilEligible;
        this.lastCalculatedAt = lastCalculatedAt;
        this.latitudeCache = latitudeCache;
        this.longitudeCache = longitudeCache;
        this.bloodTypeCache = bloodTypeCache;
    }

    // Getters & Setters
    public UUID getDonorEligibilityId() {
        return donorEligibilityId;
    }

    public void setDonorEligibilityId(UUID donorEligibilityId) {
        this.donorEligibilityId = donorEligibilityId;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public Boolean getIsCurrentlyEligible() {
        return isCurrentlyEligible;
    }

    public void setIsCurrentlyEligible(Boolean isCurrentlyEligible) {
        this.isCurrentlyEligible = isCurrentlyEligible;
    }

    public Integer getDaysUntilEligible() {
        return daysUntilEligible;
    }

    public void setDaysUntilEligible(Integer daysUntilEligible) {
        this.daysUntilEligible = daysUntilEligible;
    }

    public LocalDate getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(LocalDate lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }

    public BigDecimal getLatitudeCache() {
        return latitudeCache;
    }

    public void setLatitudeCache(BigDecimal latitudeCache) {
        this.latitudeCache = latitudeCache;
    }

    public BigDecimal getLongitudeCache() {
        return longitudeCache;
    }

    public void setLongitudeCache(BigDecimal longitudeCache) {
        this.longitudeCache = longitudeCache;
    }

    public String getBloodTypeCache() {
        return bloodTypeCache;
    }

    public void setBloodTypeCache(String bloodTypeCache) {
        this.bloodTypeCache = bloodTypeCache;
    }
}