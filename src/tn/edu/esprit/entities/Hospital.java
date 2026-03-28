package tn.edu.esprit.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

/**
 * Hospital entity representing hospital information
 * Maps to hospital table in the database
 * Uses UUID as primary key for distributed system compatibility
 *
 */
public class Hospital {
    private UUID hospitalId;
    private String name;
    private String address;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private String email;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Default constructor
    public Hospital() {
        this.isActive = true; // Default value
        this.latitude = BigDecimal.ZERO;
        this.longitude = BigDecimal.ZERO;
    }

    // Constructor with essential fields
    public Hospital(String name, String address, String city) {
        this();
        this.name = name;
        this.address = address;
        this.city = city;
    }

    // Constructor with location
    public Hospital(String name, String address, String city,
                   BigDecimal latitude, BigDecimal longitude) {
        this(name, address, city);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Full constructor
    public Hospital(UUID hospitalId, String name, String address, String city,
                   BigDecimal latitude, BigDecimal longitude, String phone,
                   String email, boolean isActive) {
        this.hospitalId = hospitalId;
        this.name = name;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.email = email;
        this.isActive = isActive;
    }

    // Getters and Setters
    public UUID getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(UUID hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    // Business methods
    /**
     * Calculate distance to another hospital using Haversine formula
     * @param other Another hospital
     * @return Distance in kilometers
     */
    public double calculateDistanceTo(Hospital other) {
        if (this.latitude == null || this.longitude == null ||
            other.latitude == null || other.longitude == null) {
            return -1; // Invalid coordinates
        }

        final int EARTH_RADIUS = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(other.latitude.doubleValue() - this.latitude.doubleValue());
        double lonDistance = Math.toRadians(other.longitude.doubleValue() - this.longitude.doubleValue());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.latitude.doubleValue())) * Math.cos(Math.toRadians(other.latitude.doubleValue()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * Check if hospital is in the same city as another hospital
     */
    public boolean isInSameCity(Hospital other) {
        return this.city != null && this.city.equalsIgnoreCase(other.city);
    }

    // toString method
    @Override
    public String toString() {
        return "Hospital{" +
                "hospitalId=" + hospitalId +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // equals method
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Hospital hospital = (Hospital) obj;
        return Objects.equals(hospitalId, hospital.hospitalId);
    }

    // hashCode method
    @Override
    public int hashCode() {
        return hospitalId != null ? hospitalId.hashCode() : 0;
    }
}
