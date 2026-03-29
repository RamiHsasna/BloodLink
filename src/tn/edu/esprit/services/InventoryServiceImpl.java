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

public class InventoryServiceImpl implements InventoryService {

    private static final Logger LOGGER = Logger.getLogger(
        InventoryServiceImpl.class.getName()
    );
    private final Connection connection;

    // SQL queries aligned with the live Supabase schema.
    private static final String INSERT_INVENTORY =
        "INSERT INTO blood_inventory (hospital_id, blood_type_id, quantity_units, status, updated_at) " +
        "VALUES (?, ?, ?, ?, NOW())";

    private static final String UPDATE_INVENTORY =
        "UPDATE blood_inventory SET hospital_id = ?, blood_type_id = ?, quantity_units = ?, status = ?, updated_at = NOW() WHERE inventory_id = ?";

    private static final String DELETE_INVENTORY =
        "DELETE FROM blood_inventory WHERE inventory_id = ?";

    private static final String SELECT_ALL =
        "SELECT * FROM blood_inventory ORDER BY inventory_id";

    private static final String SELECT_BY_ID =
        "SELECT * FROM blood_inventory WHERE inventory_id = ?";

    private static final String EXISTS_BY_ID =
        "SELECT 1 FROM blood_inventory WHERE inventory_id = ?";

    public InventoryServiceImpl() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Object o) {
        BloodInventory inventory = (BloodInventory) o;
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory cannot be null");
        }
        try (
            PreparedStatement ps = connection.prepareStatement(
                INSERT_INVENTORY,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setObject(1, inventory.getHospitalId());
            ps.setString(2, inventory.getBloodTypeId());
            ps.setInt(3, safeQuantity(inventory));
            ps.setString(
                4,
                inventory.getStatus() != null
                    ? inventory.getStatus().name()
                    : null
            );

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        inventory.setInventoryId(generatedKeys.getInt(1));
                    }
                }
                LOGGER.info(
                    "Inventory added successfully: " +
                        inventory.getInventoryId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error adding inventory", e);
        }
    }

    @Override
    public void modifier(Object o) {
        BloodInventory inventory = (BloodInventory) o;
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory cannot be null");
        }
        try (
            PreparedStatement ps = connection.prepareStatement(UPDATE_INVENTORY)
        ) {
            ps.setObject(1, inventory.getHospitalId());
            ps.setString(2, inventory.getBloodTypeId());
            ps.setInt(3, safeQuantity(inventory));
            ps.setString(
                4,
                inventory.getStatus() != null
                    ? inventory.getStatus().name()
                    : null
            );
            ps.setInt(5, inventory.getInventoryId()); // WHERE clause

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
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error deleting inventory", e);
        }
    }

    @Override
    public Object getInventory(Object o) {
        BloodInventory inventory = (BloodInventory) o;
        if (inventory == null || inventory.getInventoryId() == null) {
            throw new IllegalArgumentException(
                "Inventory and its ID cannot be null"
            );
        }
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, inventory.getInventoryId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInventory(rs);
                } else {
                    LOGGER.warning(
                        "No inventory found with id: " +
                            inventory.getInventoryId()
                    );
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error retrieving inventory", e);
        }
    }

    @Override
    public Object getInventoryById(Integer id) {
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
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error retrieving inventory by id", e);
        }
    }

    @Override
    public List getAllInventories() {
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
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error checking inventory existence", e);
        }
    }

    private BloodInventory mapResultSetToInventory(ResultSet rs)
        throws SQLException {
        BloodInventory inventory = new BloodInventory();
        inventory.setInventoryId(rs.getInt("inventory_id"));

        String hospitalIdStr = rs.getString("hospital_id");
        if (hospitalIdStr != null) {
            inventory.setHospitalId(UUID.fromString(hospitalIdStr));
        }

        inventory.setBloodTypeId(rs.getString("blood_type_id"));
        int quantityUnits = rs.getInt("quantity_units");
        if (rs.wasNull()) {
            quantityUnits = 0;
        }
        inventory.setQuantityUnitsInt(quantityUnits);
        inventory.setQuantityUnitsDecimal(java.math.BigDecimal.valueOf(quantityUnits));
        inventory.setExpirationDate(null);
        inventory.setEntryDate(rs.getTimestamp("updated_at"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            inventory.setStatus(parseStatus(statusStr, quantityUnits));
        } else {
            inventory.setStatus(deriveStatus(quantityUnits));
        }

        inventory.setUpdatedAt(rs.getTimestamp("updated_at"));
        return inventory;
    }

    private int safeQuantity(BloodInventory inventory) {
        if (inventory.getQuantityUnitsInt() != null) {
            return inventory.getQuantityUnitsInt();
        }
        if (inventory.getQuantityUnitsDecimal() != null) {
            return inventory.getQuantityUnitsDecimal().intValue();
        }
        return 0;
    }

    private InventoryStatus parseStatus(String statusValue, int quantityUnits) {
        try {
            return InventoryStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return deriveStatus(quantityUnits);
        }
    }

    private InventoryStatus deriveStatus(int quantityUnits) {
        if (quantityUnits <= 5) {
            return InventoryStatus.CRITICAL;
        }
        if (quantityUnits <= 15) {
            return InventoryStatus.LOW;
        }
        return InventoryStatus.OPTIMAL;
    }
}
