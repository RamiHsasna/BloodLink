package tn.edu.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DonationsEvent {

    private String eventId;
    private String hospitalId;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String targetBloodTypes;
    private Integer targetCollectionUnits;
    private Integer actualCollectionUnits;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DonationsEvent() {}

    public DonationsEvent(String eventId, String hospitalId, String name, String description,
                          LocalDateTime startDate, LocalDateTime endDate, String location,
                          BigDecimal latitude, BigDecimal longitude, String targetBloodTypes,
                          Integer targetCollectionUnits, Integer actualCollectionUnits,
                          String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.eventId = eventId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.targetBloodTypes = targetBloodTypes;
        this.targetCollectionUnits = targetCollectionUnits;
        this.actualCollectionUnits = actualCollectionUnits;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters & Setters

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getTargetBloodTypes() { return targetBloodTypes; }
    public void setTargetBloodTypes(String targetBloodTypes) { this.targetBloodTypes = targetBloodTypes; }

    public Integer getTargetCollectionUnits() { return targetCollectionUnits; }
    public void setTargetCollectionUnits(Integer targetCollectionUnits) { this.targetCollectionUnits = targetCollectionUnits; }

    public Integer getActualCollectionUnits() { return actualCollectionUnits; }
    public void setActualCollectionUnits(Integer actualCollectionUnits) { this.actualCollectionUnits = actualCollectionUnits; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}