package tn.edu.esprit.entities;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

public class BloodInventory {

    private Integer inventoryId;
    private UUID hospitalId;
    private String bloodTypeId;
    private String donorId;
    private Integer quantityUnitsInt;
    private BigDecimal quantityUnitsDecimal;
    private Date expirationDate;
    private Timestamp entryDate;
    private InventoryStatus status;
    private Timestamp updatedAt;

    public BloodInventory() {
        this.quantityUnitsInt = 0;
        this.quantityUnitsDecimal = BigDecimal.ZERO;
    }

    public BloodInventory(
        UUID hospitalId,
        String bloodTypeId,
        String donorId,
        Integer quantityUnitsInt,
        Date expirationDate
    ) {
        this();
        this.hospitalId = hospitalId;
        this.bloodTypeId = bloodTypeId;
        this.donorId = donorId;
        this.quantityUnitsInt = quantityUnitsInt;
        this.expirationDate = expirationDate;
    }

    public BloodInventory(
        UUID hospitalId,
        String bloodTypeId,
        String donorId,
        Integer quantityUnitsInt,
        BigDecimal quantityUnitsDecimal,
        Date expirationDate,
        InventoryStatus status
    ) {
        this.hospitalId = hospitalId;
        this.bloodTypeId = bloodTypeId;
        this.donorId = donorId;
        this.quantityUnitsInt = quantityUnitsInt;
        this.quantityUnitsDecimal = quantityUnitsDecimal;
        this.expirationDate = expirationDate;
        this.status = status;
    }

    public BloodInventory(
        Integer inventoryId,
        UUID hospitalId,
        String bloodTypeId,
        String donorId,
        Integer quantityUnitsInt,
        BigDecimal quantityUnitsDecimal,
        Date expirationDate,
        Timestamp entryDate,
        InventoryStatus status,
        Timestamp updatedAt
    ) {
        this.inventoryId = inventoryId;
        this.hospitalId = hospitalId;
        this.bloodTypeId = bloodTypeId;
        this.donorId = donorId;
        this.quantityUnitsInt = quantityUnitsInt;
        this.quantityUnitsDecimal = quantityUnitsDecimal;
        this.expirationDate = expirationDate;
        this.entryDate = entryDate;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    //Getters and Setters
    public Integer getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Integer inventoryId) {
        this.inventoryId = inventoryId;
    }

    public UUID getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(UUID hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getBloodTypeId() {
        return bloodTypeId;
    }

    public void setBloodTypeId(String bloodTypeId) {
        this.bloodTypeId = bloodTypeId;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public Integer getQuantityUnitsInt() {
        return quantityUnitsInt;
    }

    public void setQuantityUnitsInt(Integer quantityUnitsInt) {
        this.quantityUnitsInt = quantityUnitsInt;
    }

    public BigDecimal getQuantityUnitsDecimal() {
        return quantityUnitsDecimal;
    }

    public void setQuantityUnitsDecimal(BigDecimal quantityUnitsDecimal) {
        this.quantityUnitsDecimal = quantityUnitsDecimal;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Timestamp getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(Timestamp entryDate) {
        this.entryDate = entryDate;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
