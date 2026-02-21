package tn.edu.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import tn.edu.esprit.entities.BloodType;
import tn.edu.esprit.Tools.DataSource;

public class BloodTypeServiceImpl implements BloodTypeService {

    private static final Logger LOGGER = Logger.getLogger(
        BloodTypeServiceImpl.class.getName()
    );
    private final Connection connection;

    //SQL queries
    private static final String INSERT_BLOODTYPE =
        "INSERT INTO blood_type (blood_type_id,abo_type,rh_factor,is_universal_donor,is_universal_recipient,compatible_donors)" +
        "VALUES(?,?,?,?,?,?)";

    private static final String UPDATE_BLOODTYPE =
        "UPDATE blood_type SET blood_type_id = ?, abo_type = ?, rh_factor = ?, is_universal_donor = ?, is_universal_recipient = ?,  compatible_donors = ? WHERE blood_type_id = ?";

    private static final String DELETE_HOSPITAL =
        "DELETE FROM blood_type WHERE blood_type_id = ?";

    private static final String SELECT_ALL =
        "SELECT * FROM blood_type ORDER BY blood_type_id";

    private static final String SELECT_BY_ID =
        "SELECT * FROM blood_type WHERE blood_type_id = ?";

    private static final String EXISTS_BY_ID =
        "SELECT * FROM blood_type WHERE blood_type_id = ?";

    public BloodTypeServiceImpl() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Object o) {
        BloodType bloodType = (BloodType) o;
        if (bloodType == null) {
            throw new IllegalArgumentException("Blood type cannot be null");
        }
        try (
            PreparedStatement ps = connection.prepareStatement(
                INSERT_BLOODTYPE,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setString(1, bloodType.getBloodTypeId());
            ps.setString(2, bloodType.getAboType());
            ps.setString(3, bloodType.getRhFactor());
            ps.setBoolean(4, bloodType.isUniversalDonor());
            ps.setBoolean(5, bloodType.isUniversalRecipient());
            ps.setString(6, bloodType.getCompatibleDonors());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info(
                    "Blood type added successfully :" +
                        bloodType.getBloodTypeId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error adding blood type", e);
        }
    }

    @Override
    public void modifier(Object o) {
        BloodType bloodType = (BloodType) o;
        if (bloodType == null) {
            throw new IllegalArgumentException("Blood type cannot be null");
        }
        try (
            PreparedStatement ps = connection.prepareStatement(UPDATE_BLOODTYPE)
        ) {
            ps.setString(1, bloodType.getBloodTypeId());
            ps.setString(2, bloodType.getAboType());
            ps.setString(3, bloodType.getRhFactor());
            ps.setBoolean(4, bloodType.isUniversalDonor());
            ps.setBoolean(5, bloodType.isUniversalRecipient());
            ps.setString(6, bloodType.getCompatibleDonors());
            ps.setString(7, bloodType.getBloodTypeId()); // WHERE clause

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info(
                    "Blood type updated successfully: " +
                        bloodType.getBloodTypeId()
                );
            } else {
                LOGGER.warning(
                    "No blood type found with id: " + bloodType.getBloodTypeId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error updating blood type", e);
        }
    }

    @Override
    public void supprimer(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Blood type ID cannot be null");
        }
        try (
            PreparedStatement ps = connection.prepareStatement(DELETE_HOSPITAL)
        ) {
            ps.setString(1, id);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("Blood type deleted successfully: " + id);
            } else {
                LOGGER.warning("No blood type found with id: " + id);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error deleting blood type", e);
        }
    }

    @Override
    public Object getBloodType(Object o) {
        BloodType bloodType = (BloodType) o;
        if (bloodType == null || bloodType.getBloodTypeId() == null) {
            throw new IllegalArgumentException(
                "Blood type and its ID cannot be null"
            );
        }
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setString(1, bloodType.getBloodTypeId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBloodType(rs);
                } else {
                    LOGGER.warning(
                        "No blood type found with id: " +
                            bloodType.getBloodTypeId()
                    );
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error retrieving blood type", e);
        }
    }

    @Override
    public List getAllBloodTypes() {
        List<BloodType> bloodTypes = new ArrayList<>();
        try (
            PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                bloodTypes.add(mapResultSetToBloodType(rs));
            }
            LOGGER.info("Retrieved " + bloodTypes.size() + " blood types");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error retrieving all blood types", e);
        }
        return bloodTypes;
    }

    @Override
    public boolean exists(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Blood type ID cannot be null");
        }
        try (PreparedStatement ps = connection.prepareStatement(EXISTS_BY_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error checking blood type existence",
                e
            );
        }
    }

    private BloodType mapResultSetToBloodType(ResultSet rs)
        throws SQLException {
        BloodType bloodType = new BloodType();
        bloodType.setBloodTypeId(rs.getString("blood_type_id"));
        bloodType.setAboType(rs.getString("abo_type"));
        bloodType.setRhFactor(rs.getString("rh_factor"));
        bloodType.setUniversalDonor(rs.getBoolean("is_universal_donor"));
        bloodType.setUniversalRecipient(
            rs.getBoolean("is_universal_recipient")
        );
        bloodType.setCompatibleDonors(rs.getString("compatible_donors"));
        bloodType.setCreatedAt(rs.getTimestamp("created_at"));
        bloodType.setUpdatedAt(rs.getTimestamp("updated_at"));
        return bloodType;
    }
}
