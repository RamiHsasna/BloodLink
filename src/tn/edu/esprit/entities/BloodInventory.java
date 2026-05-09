package tn.edu.esprit.entities;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * BloodInventory represents the consolidated stock of a specific blood type at a specific hospital.
 *
 * Key Design:
 * - One row per hospital per blood type (e.g., Hospital A + O+ = one inventory row)
 * - quantityUnits: total units in stock for this (hospital, blood_type) pair
 * - status: automatically derived from quantityUnits
 *   * quantity <= 5   → CRITICAL
 *   * quantity 6..10  → LOW
 *   * quantity > 10   → OPTIMAL
 * - Donor-specific details (donor_id, expiration_date) are NOT stored here;
 *   they belong to the donations table.
 */
public class BloodInventory {

    private Integer inventoryId;
    private UUID hospitalId;
    private String bloodTypeId;
    private Integer quantityUnits;
    private InventoryStatus status;
    private Timestamp updatedAt;

    // ============================================================
    // Constructors
    // ============================================================

    /**
     * Default constructor.
     */
    public BloodInventory() {
        this.quantityUnits = 0;
        this.status = computeStatus(this.quantityUnits);
    }

    /**
     * Constructor for creating a new inventory entry (without ID).
     */
    public BloodInventory(
        UUID hospitalId,
        String bloodTypeId,
        Integer quantityUnits
    ) {
        this.hospitalId = hospitalId;
        this.bloodTypeId = bloodTypeId;
        this.quantityUnits = quantityUnits != null ? quantityUnits : 0;
        this.status = computeStatus(this.quantityUnits);
    }

    /**
     * Full constructor including ID and timestamp (for loading from database).
     */
    public BloodInventory(
        Integer inventoryId,
        UUID hospitalId,
        String bloodTypeId,
        Integer quantityUnits,
        InventoryStatus status,
        Timestamp updatedAt
    ) {
        this.inventoryId = inventoryId;
        this.hospitalId = hospitalId;
        this.bloodTypeId = bloodTypeId;
        this.quantityUnits = quantityUnits != null ? quantityUnits : 0;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    // ============================================================
    // Business Logic Methods
    // ============================================================

    /**
     * Computes the inventory status based on the quantity of units.
     *
     * Rules:
     * - quantity <= 5   → CRITICAL
     * - quantity 6..10  → LOW
     * - quantity > 10   → OPTIMAL
     *
     * @param quantity the number of units in stock
     * @return the computed InventoryStatus
     */
    public static InventoryStatus computeStatus(Integer quantity) {
        if (quantity == null || quantity <= 5) {
            return InventoryStatus.CRITICAL;
        } else if (quantity <= 10) {
            return InventoryStatus.LOW;
        } else {
            return InventoryStatus.OPTIMAL;
        }
    }

    /**
     * Adds units to the current inventory and updates status accordingly.
     *
     * @param units the number of units to add
     */
    public void addUnits(Integer units) {
        if (units == null || units < 0) {
            throw new IllegalArgumentException(
                "Units to add must be non-negative"
            );
        }
        this.quantityUnits += units;
        this.status = computeStatus(this.quantityUnits);
    }

    /**
     * Deducts units from the current inventory and updates status accordingly.
     * Throws an exception if there are insufficient units.
     *
     * @param units the number of units to deduct
     * @throws IllegalArgumentException if units to deduct exceed available quantity
     */
    public void deductUnits(Integer units) {
        if (units == null || units < 0) {
            throw new IllegalArgumentException(
                "Units to deduct must be non-negative"
            );
        }
        if (this.quantityUnits < units) {
            throw new IllegalArgumentException(
                String.format(
                    "Insufficient inventory: have %d units, requested to deduct %d",
                    this.quantityUnits,
                    units
                )
            );
        }
        this.quantityUnits -= units;
        this.status = computeStatus(this.quantityUnits);
    }

    /**
     * Checks if the inventory status is critical.
     *
     * @return true if status is CRITICAL
     */
    public boolean isCritical() {
        return this.status == InventoryStatus.CRITICAL;
    }

    /**
     * Checks if the inventory status is low.
     *
     * @return true if status is LOW
     */
    public boolean isLow() {
        return this.status == InventoryStatus.LOW;
    }

    /**
     * Checks if the inventory status is optimal.
     *
     * @return true if status is OPTIMAL
     */
    public boolean isOptimal() {
        return this.status == InventoryStatus.OPTIMAL;
    }

    // ============================================================
    // Getters and Setters
    // ============================================================

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

    public Integer getQuantityUnits() {
        return quantityUnits;
    }

    public void setQuantityUnits(Integer quantityUnits) {
        this.quantityUnits = quantityUnits != null ? quantityUnits : 0;
        this.status = computeStatus(this.quantityUnits);
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

    // ============================================================
    // toString
    // ============================================================

    @Override
    public String toString() {
        return (
            "BloodInventory{" +
            "inventoryId=" +
            inventoryId +
            ", hospitalId=" +
            hospitalId +
            ", bloodTypeId='" +
            bloodTypeId +
            '\'' +
            ", quantityUnits=" +
            quantityUnits +
            ", status=" +
            status +
            ", updatedAt=" +
            updatedAt +
            '}'
        );
    }
}
