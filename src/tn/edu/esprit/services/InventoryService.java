package tn.edu.esprit.services;

import java.util.List;
import java.util.UUID;
import tn.edu.esprit.entities.InventoryStatus;

/**
 * InventoryService defines operations for managing consolidated blood inventory.
 *
 * CONSOLIDATED INVENTORY MODEL:
 * - Each hospital has exactly ONE inventory row per blood type.
 * - The quantity_units field holds the aggregated stock for (hospital, blood_type).
 * - Status is automatically computed from quantity_units by a database trigger.
 * - Donor-specific details (donor_id, expiration_date) are in the donations table.
 *
 * @param <T> the BloodInventory type
 */
public interface InventoryService<T> {
    // ============================================================
    // Core CRUD Operations
    // ============================================================

    /**
     * Adds a new inventory record.
     *
     * @param t the BloodInventory to add
     */
    void ajouter(T t);

    /**
     * Modifies an existing inventory record.
     *
     * @param t the BloodInventory to modify
     */
    void modifier(T t);

    /**
     * Deletes an inventory record by ID.
     *
     * @param id the inventory ID
     */
    void supprimer(Integer id);

    /**
     * Retrieves an inventory record using an inventory object.
     *
     * @param t the inventory object containing the ID to lookup
     * @return the retrieved inventory, or null if not found
     */
    T getInventory(T t);

    /**
     * Retrieves an inventory record by its ID.
     *
     * @param id the inventory ID
     * @return the retrieved inventory, or null if not found
     */
    T getInventoryById(Integer id);

    /**
     * Retrieves all inventory records.
     *
     * @return a list of all inventory records
     */
    List<T> getAllInventories();

    /**
     * Checks if an inventory record exists by ID.
     *
     * @param id the inventory ID
     * @return true if the inventory exists, false otherwise
     */
    boolean exists(Integer id);

    // ============================================================
    // Specialized Query Operations for Consolidated Inventory
    // ============================================================

    /**
     * Retrieves the inventory for a specific hospital and blood type.
     * This is the primary query for the consolidated inventory model.
     * Returns exactly one row if it exists (due to unique constraint).
     *
     * @param hospitalId  the hospital UUID
     * @param bloodTypeId the blood type ID (e.g., "O+", "A-", etc.)
     * @return the BloodInventory, or null if not found
     */
    T getInventoryByHospitalAndBloodType(UUID hospitalId, String bloodTypeId);

    /**
     * Retrieves all inventory records for a specific hospital.
     * Returns at most 8 rows (one per blood type).
     *
     * @param hospitalId the hospital UUID
     * @return a list of BloodInventory records for that hospital, ordered by blood type
     */
    List<T> getInventoriesByHospital(UUID hospitalId);

    /**
     * Retrieves all inventory records with a specific status.
     * Useful for alert systems and reporting.
     *
     * @param status the InventoryStatus to filter by (OPTIMAL, LOW, or CRITICAL)
     * @return a list of BloodInventory records with that status, ordered by quantity ascending
     */
    List<T> getInventoriesByStatus(InventoryStatus status);

    // ============================================================
    // Stock Management Operations
    // ============================================================

    /**
     * Adds stock from a donation using an upsert pattern.
     *
     * LOGIC:
     * - If a row exists for (hospital_id, blood_type_id), increments quantity_units.
     * - If no row exists, creates one with the given units.
     * - The database trigger automatically recomputes status based on new quantity.
     *
     * Called by ServiceDonation after a donation is successfully recorded.
     *
     * @param hospitalId  the hospital UUID
     * @param bloodTypeId the blood type ID (e.g., "O+")
     * @param units       the number of units to add (must be positive)
     * @throws IllegalArgumentException if any parameter is null or units <= 0
     */
    void addStockFromDonation(
        UUID hospitalId,
        String bloodTypeId,
        Integer units
    );

    /**
     * Safely deducts stock from inventory with row-level locking.
     *
     * LOGIC:
     * - Uses SELECT ... FOR UPDATE to lock the row.
     * - Validates that sufficient stock exists.
     * - If validation passes, updates quantity_units.
     * - The database trigger automatically recomputes status based on new quantity.
     * - Wrapped in a transaction for atomicity.
     *
     * Used for transfers, blood usage, or inventory corrections.
     *
     * @param hospitalId  the hospital UUID
     * @param bloodTypeId the blood type ID (e.g., "O+")
     * @param units       the number of units to deduct (must be positive)
     * @return true if deduction succeeded, false if insufficient stock or row not found
     * @throws IllegalArgumentException if any parameter is null or units <= 0
     */
    boolean deductStock(UUID hospitalId, String bloodTypeId, Integer units);
}
