package tn.edu.esprit.services;

import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.Tools.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HospitalServiceImpl implements HospitalService {
    private static final Logger LOGGER = Logger.getLogger(
        HospitalServiceImpl.class.getName()
    );
    private final Connection connection;
    // SQL Queries
    private static final String INSERT_HOSPITAL =
        "INSERT INTO hospital (name, address, city, latitude, longitude, phone, email, is_active) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_HOSPITAL =
        "UPDATE hospital SET name = ?, address = ?, city = ?, latitude = ?, longitude = ?, " +
        "phone = ?, email = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE hospital_id = ?";

    private static final String DELETE_HOSPITAL =
        "DELETE FROM hospital WHERE hospital_id = ?";

    private static final String SELECT_ALL =
        "SELECT * FROM hospital ORDER BY name";

    private static final String SELECT_BY_ID = "SELECT * FROM hospital WHERE hospital_id = ?";

    private static final String EXISTS_BY_ID =
        "SELECT 1 FROM hospital WHERE hospital_id = ?";


    public HospitalServiceImpl() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Object o) {
        Hospital hospital =  (Hospital) o;
        if(hospital == null){
            throw new IllegalArgumentException("Hospital cannot be null");
        }
        try (
            PreparedStatement ps = connection.prepareStatement(
                INSERT_HOSPITAL,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setString(1, hospital.getName());
            ps.setString(2, hospital.getAddress());
            ps.setString(3, hospital.getCity());
            ps.setBigDecimal(4, hospital.getLatitude());
            ps.setBigDecimal(5, hospital.getLongitude());
            ps.setString(6, hospital.getPhone());
            ps.setString(7, hospital.getEmail());
            ps.setBoolean(8, hospital.isActive());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                // Get the generated UUID
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        UUID generatedId = (UUID) generatedKeys.getObject(1);
                        hospital.setHospitalId(generatedId);
                        LOGGER.info(
                            "Hospital added successfully with ID: " +
                                generatedId
                        );
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error adding hospital: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error adding hospital", e);
        }
    }

    @Override
    public void modifier(Object o) {
        Hospital hospital = (Hospital) o;
        if (hospital == null || hospital.getHospitalId() == null) {
            throw new IllegalArgumentException(
                "Hospital and Hospital ID cannot be null"
            );
        }
        try (
            PreparedStatement ps = connection.prepareStatement(UPDATE_HOSPITAL)
        ) {
            ps.setString(1, hospital.getName());
            ps.setString(2, hospital.getAddress());
            ps.setString(3, hospital.getCity());
            ps.setBigDecimal(4, hospital.getLatitude());
            ps.setBigDecimal(5, hospital.getLongitude());
            ps.setString(6, hospital.getPhone());
            ps.setString(7, hospital.getEmail());
            ps.setBoolean(8, hospital.isActive());
            ps.setObject(9, hospital.getHospitalId());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException(
                    "Hospital not found with ID: " + hospital.getHospitalId()
                );
            }
            LOGGER.info(
                "Hospital updated successfully: " + hospital.getHospitalId()
            );
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error updating hospital: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error updating hospital", e);
        }
    }

    @Override
    public void supprimer(UUID hospitalId) {
        if (hospitalId == null) {
            throw new IllegalArgumentException("Hospital ID cannot be null");
        }

        try (
            PreparedStatement ps = connection.prepareStatement(DELETE_HOSPITAL)
        ) {
            ps.setObject(1, hospitalId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException(
                    "Hospital not found with ID: " + hospitalId
                );
            }
            LOGGER.info("Hospital deleted successfully: " + hospitalId);
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error deleting hospital: " + e.getMessage(),
                e
            );
            throw new RuntimeException("Error deleting hospital", e);
        }
    }

    @Override
    public Object getHospital(Object o) {
        Hospital hospital = (Hospital) o;
        if (hospital == null || hospital.getHospitalId() == null) {
            return null;
        }
        return hospital.getHospitalId();
    }

    @Override
    public List getAllHospitals() {
        List<Hospital> hospitals = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hospitals.add(mapResultSetToHospital(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error getting all hospitals: " + e.getMessage(),
                e
            );
        }
        return hospitals;
    }
    @Override
    public boolean exists(UUID hospitalId) {
        if (hospitalId == null) {
            return false;
        }

        try (PreparedStatement ps = connection.prepareStatement(EXISTS_BY_ID)) {
            ps.setObject(1, hospitalId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error checking hospital existence: " + e.getMessage(),
                e
            );
            return false;
        }
    }

    @Override
    public Hospital getHospitalById(UUID hospitalId) {
        if (hospitalId == null) {
            return null;
        }

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setObject(1, hospitalId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHospital(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(
                Level.SEVERE,
                "Error getting hospital by ID: " + e.getMessage(),
                e
            );
        }
        return null;
    }

    private Hospital mapResultSetToHospital(ResultSet rs) throws SQLException {
        Hospital hospital = new Hospital();

        hospital.setHospitalId((UUID) rs.getObject("hospital_id"));
        hospital.setName(rs.getString("name"));
        hospital.setAddress(rs.getString("address"));
        hospital.setCity(rs.getString("city"));
        hospital.setLatitude(rs.getBigDecimal("latitude"));
        hospital.setLongitude(rs.getBigDecimal("longitude"));
        hospital.setPhone(rs.getString("phone"));
        hospital.setEmail(rs.getString("email"));
        hospital.setActive(rs.getBoolean("is_active"));
        hospital.setCreatedAt(rs.getTimestamp("created_at"));
        hospital.setUpdatedAt(rs.getTimestamp("updated_at"));

        return hospital;
    }
}
