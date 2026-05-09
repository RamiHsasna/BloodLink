package tn.edu.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import tn.edu.esprit.Tools.DataSource;
import tn.edu.esprit.entities.BloodInventory;
import tn.edu.esprit.entities.InventoryStatus;

/**
 * InventoryServiceImpl implements inventory management for consolidated blood stock.
 *
 * DESIGN PATTERN:
 * - Each hospital has exactly ONE inventory row per blood type.
 * - When a donation occurs, the stock for that (hospital_id, blood_type_id) is incremented.
 * - Status is automatically computed based on quantity thresholds.
 * - Uses upsert pattern to either create a new row or increment existing one.
 */
public class InventoryServiceImpl implements InventoryService<BloodInventory> {

    private static final Logger LOGGER = Logger.getLogger(
        InventoryServiceImpl.class.getName()
    );
    private final Connection connection;

    // ============================================================
    // SQL Queries
    // ============================================================

    private static final String INSERT_INVENTORY =
        "INSERT INTO blood_inventory (hospital_id, blood_type_id, quantity_units) " +
        "VALUES (?, ?, ?) " +
        "ON CONFLICT ON CONSTRAINT uq_hospital_blood_type " +
        "DO UPDATE SET quantity_units = blood_inventory.quantity_units + EXCLUDED.quantity_units";

    private static final String SELECT_BY_HOSPITAL_AND_BLOOD_TYPE =
        "SELECT inventory_id, hospital_id, blood_type_id, quantity_units, status, updated_at " +
        "FROM blood_inventory " +
        "WHERE hospital_id = ? AND blood_type_id = ?";

    private static final String SELECT_BY_ID =
        "SELECT inventory_id, hospital_id, blood_type_id, quantity_units, status, updated_at " +
        "FROM blood_inventory " +
        "WHERE inventory_id = ?";

    private static final String SELECT_ALL =
        "SELECT inventory_id, hospital_id, blood_type_id, quantity_units, status, updated_at " +
        "FROM blood_inventory " +
        "ORDER BY hospital_id, blood_type_id";

    private static final String SELECT_BY_HOSPITAL =
        "SELECT inventory_id, hospital_id, blood_type_id, quantity_units, status, updated_at " +
        "FROM blood_inventory " +
        "WHERE hospital_id = ? " +
        "ORDER BY blood_type_id";

    private static final String SELECT_BY_STATUS =
        "SELECT inventory_id, hospital_id, blood_type_id, quantity_units, status, updated_at " +
        "FROM blood_inventory " +
        "WHERE status = ? " +
        "ORDER BY quantity_units ASC";

    private static final String UPDATE_QUANTITY =
        "UPDATE blood_inventory SET quantity_units = ? WHERE inventory_id = ?";

    private static final String SELECT_FOR_UPDATE =
        "SELECT inventory_id, quantity_units FROM blood_inventory " +
        "WHERE hospital_id = ? AND blood_type_id = ? FOR UPDATE";

    private static final String DEDUCT_QUANTITY =
        "UPDATE blood_inventory SET quantity_units = quantity_units - ? " +
        "WHERE hospital_id = ? AND blood_type_id = ?";

    private static final String DELETE_INVENTORY =
        "DELETE FROM blood_inventory WHERE inventory_id = ?";

    private static final String EXISTS_BY_ID =
        "SELECT 1 FROM blood_inventory WHERE inventory_id = ?";

    // ============================================================
    // Constructor
    // ============================================================

    public InventoryServiceImpl() {
        this.connection = DataSource.getInstance().getConnection();
    }

    // ============================================================
    // Core CRUD Operations
    // ============================================================

    @Override
    public void ajouter(BloodInventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory cannot be null");
        }
        if (
            inventory.getHospitalId() == null ||
            inventory.getBloodTypeId() == null
        ) {
            throw new IllegalArgumentException(
                "Hospital ID and blood type ID cannot be null"
            );
        }

        try (
            PreparedStatement ps = connection.prepareStatement(
                INSERT_INVENTORY,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setObject(1, inventory.getHospitalId());
            ps.setString(2, inventory.getBloodTypeId());
            ps.setInt(
                3,
                inventory.getQuantityUnits() != null
                    ? inventory.getQuantityUnits()
                    : 0
            );

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        inventory.setInventoryId(generatedKeys.getInt(1));
                    }
                }
                LOGGER.info(
                    "Inventory added/updated for hospital " +
                        inventory.getHospitalId() +
                        " blood type " +
                        inventory.getBloodTypeId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error adding inventory: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error adding inventory", e);
        }
    }

    @Override
    public void modifier(BloodInventory inventory) {
        if (inventory == null || inventory.getInventoryId() == null) {
            throw new IllegalArgumentException(
                "Inventory and its ID cannot be null"
            );
        }

        try (
            PreparedStatement ps = connection.prepareStatement(UPDATE_QUANTITY)
        ) {
            ps.setInt(
                1,
                inventory.getQuantityUnits() != null
                    ? inventory.getQuantityUnits()
                    : 0
            );
            ps.setInt(2, inventory.getInventoryId());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info(
                    "Inventory updated successfully: " +
                        inventory.getInventoryId()
                );
            } else {
                LOGGER.warning(
                    "No inventory found with id: " + inventory.getInventoryId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error updating inventory: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error updating inventory", e);
        }
    }

    @Override
    public void supprimer(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Inventory ID cannot be null");
        }

        try (
            PreparedStatement ps = connection.prepareStatement(DELETE_INVENTORY)
        ) {
            ps.setInt(1, id);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("Inventory deleted successfully: " + id);
            } else {
                LOGGER.warning("No inventory found with id: " + id);
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error deleting inventory: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error deleting inventory", e);
        }
    }

    // ============================================================
    // Query Operations
    // ============================================================

    @Override
    public BloodInventory getInventory(BloodInventory inventory) {
        if (inventory == null || inventory.getInventoryId() == null) {
            throw new IllegalArgumentException(
                "Inventory and its ID cannot be null"
            );
        }
        return getInventoryById(inventory.getInventoryId());
    }

    @Override
    public BloodInventory getInventoryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Inventory ID cannot be null");
        }

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInventory(rs);
                } else {
                    LOGGER.warning("No inventory found with id: " + id);
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error retrieving inventory by id: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error retrieving inventory by id", e);
        }
    }

    @Override
    public List<BloodInventory> getAllInventories() {
        List<BloodInventory> inventories = new ArrayList<>();

        try (
            PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                inventories.add(mapResultSetToInventory(rs));
            }
            LOGGER.info("Retrieved " + inventories.size() + " inventories");
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error retrieving all inventories: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error retrieving all inventories", e);
        }

        return inventories;
    }

    @Override
    public boolean exists(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Inventory ID cannot be null");
        }

        try (PreparedStatement ps = connection.prepareStatement(EXISTS_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error checking inventory existence: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error checking inventory existence", e);
        }
    }

    // ============================================================
    // Specialized Query Operations
    // ============================================================

    /**
     * Retrieves the inventory record for a specific hospital and blood type.
     * This is the primary query method for consolidated inventory.
     *
     * @param hospitalId the hospital UUID
     * @param bloodTypeId the blood type ID
     * @return the BloodInventory, or null if not found
     */
    public BloodInventory getInventoryByHospitalAndBloodType(
        UUID hospitalId,
        String bloodTypeId
    ) {
        if (hospitalId == null || bloodTypeId == null) {
            throw new IllegalArgumentException(
                "Hospital ID and blood type ID cannot be null"
            );
        }

        try (
            PreparedStatement ps = connection.prepareStatement(
                SELECT_BY_HOSPITAL_AND_BLOOD_TYPE
            )
        ) {
            ps.setObject(1, hospitalId);
            ps.setString(2, bloodTypeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInventory(rs);
                } else {
                    LOGGER.warning(
                        "No inventory found for hospital " +
                            hospitalId +
                            " blood type " +
                            bloodTypeId
                    );
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error retrieving inventory by hospital and blood type: " +
                    e.getMessage(),
                e
            );
            throw new RuntimeException(
                "Error retrieving inventory by hospital and blood type",
                e
            );
        }
    }

    /**
     * Retrieves all inventory records for a specific hospital.
     *
     * @param hospitalId the hospital UUID
     * @return a list of BloodInventory records for that hospital
     */
    public List<BloodInventory> getInventoriesByHospital(UUID hospitalId) {
        if (hospitalId == null) {
            throw new IllegalArgumentException("Hospital ID cannot be null");
        }

        List<BloodInventory> inventories = new ArrayList<>();

        try (
            PreparedStatement ps = connection.prepareStatement(
                SELECT_BY_HOSPITAL
            )
        ) {
            ps.setObject(1, hospitalId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    inventories.add(mapResultSetToInventory(rs));
                }
                LOGGER.info(
                    "Retrieved " +
                        inventories.size() +
                        " inventory records for hospital " +
                        hospitalId
                );
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error retrieving inventories by hospital: " + e.getMessage(),
                e
            );
            throw new RuntimeException(
                "Error retrieving inventories by hospital",
                e
            );
        }

        return inventories;
    }

    /**
     * Retrieves all inventory records with a specific status.
     *
     * @param status the InventoryStatus to filter by
     * @return a list of BloodInventory records with that status
     */
    public List<BloodInventory> getInventoriesByStatus(InventoryStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        List<BloodInventory> inventories = new ArrayList<>();

        try (
            PreparedStatement ps = connection.prepareStatement(SELECT_BY_STATUS)
        ) {
            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    inventories.add(mapResultSetToInventory(rs));
                }
                LOGGER.info(
                    "Retrieved " +
                        inventories.size() +
                        " inventory records with status " +
                        status
                );
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error retrieving inventories by status: " + e.getMessage(),
                e
            );
            throw new RuntimeException(
                "Error retrieving inventories by status",
                e
            );
        }

        return inventories;
    }

    // ============================================================
    // Stock Management Operations
    // ============================================================

    /**
     * Adds stock from a donation using an upsert pattern.
     * If the (hospital_id, blood_type_id) row doesn't exist, it's created.
     * Otherwise, the quantity is incremented.
     *
     * @param hospitalId the hospital UUID
     * @param bloodTypeId the blood type ID
     * @param units the number of units to add
     */
    public void addStockFromDonation(
        UUID hospitalId,
        String bloodTypeId,
        Integer units
    ) {
        if (hospitalId == null || bloodTypeId == null) {
            throw new IllegalArgumentException(
                "Hospital ID and blood type ID cannot be null"
            );
        }
        if (units == null || units <= 0) {
            throw new IllegalArgumentException("Units to add must be positive");
        }

        BloodInventory inventory = new BloodInventory(
            hospitalId,
            bloodTypeId,
            units
        );
        ajouter(inventory);

        LOGGER.info(
            "Added " +
                units +
                " units of " +
                bloodTypeId +
                " to hospital " +
                hospitalId
        );
    }

    /**
     * Deducts stock from inventory with validation.
     * Uses row-level locking (FOR UPDATE) to prevent race conditions.
     *
     * @param hospitalId the hospital UUID
     * @param bloodTypeId the blood type ID
     * @param units the number of units to deduct
     * @return true if deduction was successful, false if insufficient stock
     * @throws IllegalArgumentException if parameters are invalid
     */
    public boolean deductStock(
        UUID hospitalId,
        String bloodTypeId,
        Integer units
    ) {
        if (hospitalId == null || bloodTypeId == null) {
            throw new IllegalArgumentException(
                "Hospital ID and blood type ID cannot be null"
            );
        }
        if (units == null || units <= 0) {
            throw new IllegalArgumentException(
                "Units to deduct must be positive"
            );
        }

        try {
            // Start a transaction for row-level locking
            connection.setAutoCommit(false);

            // Lock the row for update and check current quantity
            try (
                PreparedStatement selectPs = connection.prepareStatement(
                    SELECT_FOR_UPDATE
                )
            ) {
                selectPs.setObject(1, hospitalId);
                selectPs.setString(2, bloodTypeId);

                try (ResultSet rs = selectPs.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        LOGGER.warning(
                            "No inventory found for hospital " +
                                hospitalId +
                                " blood type " +
                                bloodTypeId
                        );
                        return false;
                    }

                    Integer currentQuantity = rs.getInt("quantity_units");

                    if (currentQuantity < units) {
                        connection.rollback();
                        LOGGER.warning(
                            "Insufficient stock: have " +
                                currentQuantity +
                                ", requested " +
                                units
                        );
                        return false;
                    }

                    // Perform the deduction
                    try (
                        PreparedStatement deductPs =
                            connection.prepareStatement(DEDUCT_QUANTITY)
                    ) {
                        deductPs.setInt(1, units);
                        deductPs.setObject(2, hospitalId);
                        deductPs.setString(3, bloodTypeId);

                        int rowsAffected = deductPs.executeUpdate();
                        if (rowsAffected > 0) {
                            connection.commit();
                            LOGGER.info(
                                "Deducted " +
                                    units +
                                    " units of " +
                                    bloodTypeId +
                                    " from hospital " +
                                    hospitalId
                            );
                            return true;
                        } else {
                            connection.rollback();
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                LOGGER.log(
                    Level.SEVERE,
                    "Error rolling back transaction: " +
                        rollbackEx.getMessage(),
                    rollbackEx
                );
            }
            LOGGER.log(
                Level.SEVERE,
                "Error deducting stock: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error deducting stock", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                LOGGER.log(
                    Level.SEVERE,
                    "Error resetting autocommit: " + e.getMessage(),
                    e
                );
            }
        }
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    /**
     * Maps a ResultSet row to a BloodInventory object.
     *
     * @param rs the ResultSet positioned at the current row
     * @return a BloodInventory instance
     * @throws SQLException if an error occurs accessing the ResultSet
     */
    private BloodInventory mapResultSetToInventory(ResultSet rs)
        throws SQLException {
        Integer inventoryId = rs.getInt("inventory_id");
        String hospitalIdStr = rs.getString("hospital_id");
        UUID hospitalId =
            hospitalIdStr != null ? UUID.fromString(hospitalIdStr) : null;
        String bloodTypeId = rs.getString("blood_type_id");
        Integer quantityUnits = rs.getInt("quantity_units");
        String statusStr = rs.getString("status");
        InventoryStatus status =
            statusStr != null
                ? InventoryStatus.valueOf(statusStr)
                : InventoryStatus.CRITICAL;
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        BloodInventory inventory = new BloodInventory(
            inventoryId,
            hospitalId,
            bloodTypeId,
            quantityUnits,
            status,
            updatedAt
        );

        return inventory;
    }
}
