package tn.edu.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import tn.edu.esprit.entities.BloodTransferRequest;
import tn.edu.esprit.entities.TransfertStatus;
import tn.edu.esprit.Tools.DataSource;

public class TransfertServiceImpl implements TransfertService {

    private static final Logger LOGGER = Logger.getLogger(
        TransfertServiceImpl.class.getName()
    );
    private final Connection connection;

    // SQL queries
    private static final String INSERT_TRANSFERT =
        "INSERT INTO blood_transfer_request (requesting_hospital_id, approving_hospital_id, requesting_staff_id, " +
        "approving_staff_id, blood_type_id, quantity_units_requested, quantity_units_approved, status, reason, " +
        "delivery_expected_at, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_TRANSFERT =
        "UPDATE blood_transfer_request SET requesting_hospital_id = ?, approving_hospital_id = ?, requesting_staff_id = ?, " +
        "approving_staff_id = ?, blood_type_id = ?, quantity_units_requested = ?, quantity_units_approved = ?, status = ?, " +
        "reason = ?, delivery_expected_at = ?, notes = ? WHERE transfer_id = ?";

    private static final String DELETE_TRANSFERT =
        "DELETE FROM blood_transfer_request WHERE transfer_id = ?";

    private static final String SELECT_ALL =
        "SELECT * FROM blood_transfer_request ORDER BY transfer_id";

    private static final String SELECT_BY_ID =
        "SELECT * FROM blood_transfer_request WHERE transfer_id = ?";

    private static final String EXISTS_BY_ID =
        "SELECT 1 FROM blood_transfer_request WHERE transfer_id = ?";

    public TransfertServiceImpl() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Object o) {
        BloodTransferRequest transfert = (BloodTransferRequest) o;
        if (transfert == null) {
            throw new IllegalArgumentException(
                "Transfer request cannot be null"
            );
        }
        try (
            PreparedStatement ps = connection.prepareStatement(
                INSERT_TRANSFERT,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setObject(1, transfert.getRequestingHospitalId());
            ps.setObject(2, transfert.getApprovingHospitalId());
            ps.setString(3, transfert.getRequestingStaffId());
            ps.setString(4, transfert.getApprovingStaffId());
            ps.setString(5, transfert.getBloodTypeId());
            ps.setInt(6, transfert.getQuantityUnitsRequested());
            ps.setInt(7, transfert.getQuantityUnitsApproved());
            ps.setString(
                8,
                transfert.getStatus() != null
                    ? transfert.getStatus().name()
                    : null
            );
            ps.setString(9, transfert.getReason());
            ps.setTimestamp(10, transfert.getDeliveryExpectedAt());
            ps.setString(11, transfert.getNotes());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transfert.setTransferId(generatedKeys.getInt(1));
                    }
                }
                LOGGER.info(
                    "Transfer request added successfully: " +
                        transfert.getTransferId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error adding transfer request", e);
        }
    }

    @Override
    public void modifier(Object o) {
        BloodTransferRequest transfert = (BloodTransferRequest) o;
        if (transfert == null) {
            throw new IllegalArgumentException(
                "Transfer request cannot be null"
            );
        }
        try (
            PreparedStatement ps = connection.prepareStatement(UPDATE_TRANSFERT)
        ) {
            ps.setObject(1, transfert.getRequestingHospitalId());
            ps.setObject(2, transfert.getApprovingHospitalId());
            ps.setString(3, transfert.getRequestingStaffId());
            ps.setString(4, transfert.getApprovingStaffId());
            ps.setString(5, transfert.getBloodTypeId());
            ps.setInt(6, transfert.getQuantityUnitsRequested());
            ps.setInt(7, transfert.getQuantityUnitsApproved());
            ps.setString(
                8,
                transfert.getStatus() != null
                    ? transfert.getStatus().name()
                    : null
            );
            ps.setString(9, transfert.getReason());
            ps.setTimestamp(10, transfert.getDeliveryExpectedAt());
            ps.setString(11, transfert.getNotes());
            ps.setInt(12, transfert.getTransferId()); // WHERE clause

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info(
                    "Transfer request updated successfully: " +
                        transfert.getTransferId()
                );
            } else {
                LOGGER.warning(
                    "No transfer request found with id: " +
                        transfert.getTransferId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error updating transfer request", e);
        }
    }

    @Override
    public void supprimer(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }
        try (
            PreparedStatement ps = connection.prepareStatement(DELETE_TRANSFERT)
        ) {
            ps.setInt(1, id);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("Transfer request deleted successfully: " + id);
            } else {
                LOGGER.warning("No transfer request found with id: " + id);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error deleting transfer request", e);
        }
    }

    @Override
    public Object getTransfert(Object o) {
        BloodTransferRequest transfert = (BloodTransferRequest) o;
        if (transfert == null || transfert.getTransferId() == null) {
            throw new IllegalArgumentException(
                "Transfer request and its ID cannot be null"
            );
        }
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, transfert.getTransferId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransfert(rs);
                } else {
                    LOGGER.warning(
                        "No transfer request found with id: " +
                            transfert.getTransferId()
                    );
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error retrieving transfer request", e);
        }
    }

    @Override
    public Object getTransfertById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransfert(rs);
                } else {
                    LOGGER.warning("No transfer request found with id: " + id);
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error retrieving transfer request by id",
                e
            );
        }
    }

    @Override
    public List getAllTransferts() {
        List<BloodTransferRequest> transferts = new ArrayList<>();
        try (
            PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                transferts.add(mapResultSetToTransfert(rs));
            }
            LOGGER.info(
                "Retrieved " + transferts.size() + " transfer requests"
            );
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error retrieving all transfer requests",
                e
            );
        }
        return transferts;
    }

    @Override
    public boolean exists(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }
        try (PreparedStatement ps = connection.prepareStatement(EXISTS_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error checking transfer request existence",
                e
            );
        }
    }

    private BloodTransferRequest mapResultSetToTransfert(ResultSet rs)
        throws SQLException {
        BloodTransferRequest transfert = new BloodTransferRequest();
        transfert.setTransferId(rs.getInt("transfer_id"));

        String requestingHospitalIdStr = rs.getString("requesting_hospital_id");
        if (requestingHospitalIdStr != null) {
            transfert.setRequestingHospitalId(
                UUID.fromString(requestingHospitalIdStr)
            );
        }

        String approvingHospitalIdStr = rs.getString("approving_hospital_id");
        if (approvingHospitalIdStr != null) {
            transfert.setApprovingHospitalId(
                UUID.fromString(approvingHospitalIdStr)
            );
        }

        transfert.setRequestingStaffId(rs.getString("requesting_staff_id"));
        transfert.setApprovingStaffId(rs.getString("approving_staff_id"));
        transfert.setBloodTypeId(rs.getString("blood_type_id"));
        transfert.setQuantityUnitsRequested(
            rs.getInt("quantity_units_requested")
        );
        transfert.setQuantityUnitsApproved(
            rs.getInt("quantity_units_approved")
        );

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            transfert.setStatus(TransfertStatus.valueOf(statusStr));
        }

        transfert.setReason(rs.getString("reason"));
        transfert.setRequestedAt(rs.getTimestamp("requested_at"));
        transfert.setApprovedAt(rs.getTimestamp("approved_at"));
        transfert.setDeliveryExpectedAt(
            rs.getTimestamp("delivery_expected_at")
        );
        transfert.setActualDeliveryAt(rs.getTimestamp("actual_delivery_at"));
        transfert.setNotes(rs.getString("notes"));

        return transfert;
    }
}
