package tn.edu.esprit.services;

import java.sql.*;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import tn.edu.esprit.Tools.DataSource;
import tn.edu.esprit.entities.BloodTransferRequest;
import tn.edu.esprit.entities.TransfertStatus;

public class TransfertServiceImpl implements TransfertService {

    private static final Logger LOGGER = Logger.getLogger(
        TransfertServiceImpl.class.getName()
    );
    private final Connection connection;
    private final InventoryServiceImpl inventoryService;

    // SQL queries
    private static final String INSERT_TRANSFERT =
        "INSERT INTO blood_transfer_request (requesting_hospital_id, approving_hospital_id, requesting_staff_id, " +
        "approving_staff_id, blood_type_id, quantity_units_requested, quantity_units_approved, status, reason, " +
        "delivery_expected_at, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_TRANSFERT =
        "UPDATE blood_transfer_request SET requesting_hospital_id = ?, approving_hospital_id = ?, requesting_staff_id = ?, " +
        "approving_staff_id = ?, blood_type_id = ?, quantity_units_requested = ?, quantity_units_approved = ?, status = ?, " +
        "reason = ?, delivery_expected_at = ?, approved_at = ?, actual_delivery_at = ?, notes = ? WHERE transfer_id = ?";

    private static final String DELETE_TRANSFERT =
        "DELETE FROM blood_transfer_request WHERE transfer_id = ?";

    private static final String SELECT_ALL =
        "SELECT * FROM blood_transfer_request ORDER BY requested_at DESC";

    private static final String SELECT_BY_ID =
        "SELECT * FROM blood_transfer_request WHERE transfer_id = ?";

    private static final String SELECT_BY_REQUESTING_HOSPITAL =
        "SELECT * FROM blood_transfer_request WHERE requesting_hospital_id = ? ORDER BY requested_at DESC";

    private static final String SELECT_BY_APPROVING_HOSPITAL =
        "SELECT * FROM blood_transfer_request WHERE approving_hospital_id = ? ORDER BY requested_at DESC";

    private static final String SELECT_PENDING_FOR_HOSPITAL =
        "SELECT * FROM blood_transfer_request WHERE approving_hospital_id = ? AND status = 'PENDING' ORDER BY requested_at DESC";

    private static final String SELECT_BY_STATUS =
        "SELECT * FROM blood_transfer_request WHERE status = ? ORDER BY requested_at DESC";

    private static final String EXISTS_BY_ID =
        "SELECT 1 FROM blood_transfer_request WHERE transfer_id = ?";

    public TransfertServiceImpl() {
        this.connection = DataSource.getInstance().getConnection();
        this.inventoryService = new InventoryServiceImpl();
    }

    /**
     * Creates a new transfer request (staff only)
     * Only allows PENDING status transfers to be created
     */
    @Override
    public void ajouter(Object o) {
        BloodTransferRequest transfert = (BloodTransferRequest) o;
        if (transfert == null) {
            throw new IllegalArgumentException(
                "Transfer request cannot be null"
            );
        }

        // Validate that a new transfer is in PENDING status
        if (
            transfert.getStatus() != null &&
            transfert.getStatus() != TransfertStatus.PENDING
        ) {
            throw new IllegalArgumentException(
                "New transfer requests must start in PENDING status"
            );
        }

        // Validate required fields
        if (transfert.getRequestingHospitalId() == null) {
            throw new IllegalArgumentException(
                "Requesting hospital cannot be null"
            );
        }
        if (transfert.getRequestingStaffId() == null) {
            throw new IllegalArgumentException(
                "Requesting staff ID cannot be null"
            );
        }
        if (transfert.getBloodTypeId() == null) {
            throw new IllegalArgumentException("Blood type cannot be null");
        }
        if (
            transfert.getQuantityUnitsRequested() == null ||
            transfert.getQuantityUnitsRequested() <= 0
        ) {
            throw new IllegalArgumentException(
                "Quantity requested must be greater than 0"
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
            ps.setString(4, null); // approving_staff_id null until approved
            ps.setString(5, transfert.getBloodTypeId());
            ps.setInt(6, transfert.getQuantityUnitsRequested());
            ps.setNull(7, Types.INTEGER); // quantity_units_approved = null initially
            ps.setString(8, TransfertStatus.PENDING.name());
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
                    "Transfer request created successfully: " +
                        transfert.getTransferId()
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error creating transfer request", e);
        }
    }

    /**
     * Updates an existing transfer request with full business logic
     * Handles status transitions and inventory updates
     */
    @Override
    public void modifier(Object o) {
        if (o == null) {
            throw new IllegalArgumentException(
                "Transfer request cannot be null"
            );
        }

        BloodTransferRequest transfer = (BloodTransferRequest) o;

        if (transfer.getTransferId() == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }

        try {
            // Fetch existing transfer to validate state transitions
            BloodTransferRequest existing =
                (BloodTransferRequest) getTransfertById(
                    transfer.getTransferId()
                );
            if (existing == null) {
                throw new IllegalArgumentException(
                    "Transfer request not found: " + transfer.getTransferId()
                );
            }

            // Handle status transitions and business logic
            handleStatusTransition(existing, transfer);

            try (
                PreparedStatement ps = connection.prepareStatement(
                    UPDATE_TRANSFERT
                )
            ) {
                ps.setObject(
                    1,
                    transfer.getRequestingHospitalId() != null
                        ? transfer.getRequestingHospitalId()
                        : existing.getRequestingHospitalId()
                );
                ps.setObject(
                    2,
                    transfer.getApprovingHospitalId() != null
                        ? transfer.getApprovingHospitalId()
                        : existing.getApprovingHospitalId()
                );
                ps.setString(
                    3,
                    transfer.getRequestingStaffId() != null
                        ? transfer.getRequestingStaffId()
                        : existing.getRequestingStaffId()
                );
                ps.setString(
                    4,
                    transfer.getApprovingStaffId() != null
                        ? transfer.getApprovingStaffId()
                        : existing.getApprovingStaffId()
                );
                ps.setString(
                    5,
                    transfer.getBloodTypeId() != null
                        ? transfer.getBloodTypeId()
                        : existing.getBloodTypeId()
                );
                ps.setInt(
                    6,
                    transfer.getQuantityUnitsRequested() != null
                        ? transfer.getQuantityUnitsRequested()
                        : existing.getQuantityUnitsRequested()
                );

                // Handle NULL for quantity_units_approved
                if (transfer.getQuantityUnitsApproved() != null) {
                    ps.setInt(7, transfer.getQuantityUnitsApproved());
                } else if (existing.getQuantityUnitsApproved() != null) {
                    ps.setInt(7, existing.getQuantityUnitsApproved());
                } else {
                    ps.setNull(7, java.sql.Types.INTEGER);
                }

                ps.setString(
                    8,
                    transfer.getStatus() != null
                        ? transfer.getStatus().name()
                        : existing.getStatus().name()
                );
                ps.setString(
                    9,
                    transfer.getReason() != null
                        ? transfer.getReason()
                        : existing.getReason()
                );
                ps.setTimestamp(
                    10,
                    transfer.getDeliveryExpectedAt() != null
                        ? transfer.getDeliveryExpectedAt()
                        : existing.getDeliveryExpectedAt()
                );
                ps.setTimestamp(
                    11,
                    transfer.getApprovedAt() != null
                        ? transfer.getApprovedAt()
                        : existing.getApprovedAt()
                );
                ps.setTimestamp(
                    12,
                    transfer.getActualDeliveryAt() != null
                        ? transfer.getActualDeliveryAt()
                        : existing.getActualDeliveryAt()
                );
                ps.setString(
                    13,
                    transfer.getNotes() != null
                        ? transfer.getNotes()
                        : existing.getNotes()
                );
                ps.setInt(14, transfer.getTransferId());

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    LOGGER.info(
                        "Transfer request updated successfully: " +
                            transfer.getTransferId()
                    );
                } else {
                    LOGGER.warning(
                        "No transfer request found with id: " +
                            transfer.getTransferId()
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Error updating transfer request", e);
        }
    }

    /**
     * Handles status transitions and triggers appropriate business logic
     */
    private void handleStatusTransition(
        BloodTransferRequest existing,
        BloodTransferRequest updated
    ) {
        TransfertStatus oldStatus = existing.getStatus();
        TransfertStatus newStatus = updated.getStatus();

        if (oldStatus == null || newStatus == null) {
            return;
        }

        // If status hasn't changed, no special handling needed
        if (oldStatus == newStatus) {
            return;
        }

        // Validate state transitions
        switch (oldStatus) {
            case PENDING:
                if (
                    newStatus != TransfertStatus.APPROVED &&
                    newStatus != TransfertStatus.CANCELLED &&
                    newStatus != TransfertStatus.DENIED
                ) {
                    throw new IllegalArgumentException(
                        "PENDING transfer can only transition to APPROVED, DENIED, or CANCELLED"
                    );
                }
                if (newStatus == TransfertStatus.APPROVED) {
                    // Set approval timestamp and approving staff ID
                    if (updated.getApprovedAt() == null) {
                        updated.setApprovedAt(
                            new Timestamp(System.currentTimeMillis())
                        );
                    }
                    // Ensure quantity approved is set
                    if (
                        updated.getQuantityUnitsApproved() == null ||
                        updated.getQuantityUnitsApproved() == 0
                    ) {
                        updated.setQuantityUnitsApproved(
                            existing.getQuantityUnitsRequested()
                        );
                    }
                }
                break;
            case APPROVED:
                if (
                    newStatus != TransfertStatus.IN_TRANSIT &&
                    newStatus != TransfertStatus.CANCELLED
                ) {
                    throw new IllegalArgumentException(
                        "APPROVED transfer can only transition to IN_TRANSIT or CANCELLED"
                    );
                }
                break;
            case IN_TRANSIT:
                if (
                    newStatus != TransfertStatus.DELIVERED &&
                    newStatus != TransfertStatus.CANCELLED
                ) {
                    throw new IllegalArgumentException(
                        "IN_TRANSIT transfer can only transition to DELIVERED or CANCELLED"
                    );
                }
                break;
            case DELIVERED:
                throw new IllegalArgumentException(
                    "DELIVERED transfer cannot be modified"
                );
            case CANCELLED:
                throw new IllegalArgumentException(
                    "CANCELLED transfer cannot be modified"
                );
            case DENIED:
                throw new IllegalArgumentException(
                    "DENIED transfer cannot be modified"
                );
        }

        // When transitioning to DELIVERED, update inventory
        if (
            newStatus == TransfertStatus.DELIVERED &&
            oldStatus != TransfertStatus.DELIVERED
        ) {
            processTransferDelivery(existing, updated);
        }
    }

    /**
     * Processes the inventory update when a transfer is marked as DELIVERED
     */
    private void processTransferDelivery(
        BloodTransferRequest existing,
        BloodTransferRequest updated
    ) {
        try {
            // Get the actual quantity delivered
            Integer quantityDelivered =
                updated.getQuantityUnitsApproved() != null &&
                updated.getQuantityUnitsApproved() > 0
                    ? updated.getQuantityUnitsApproved()
                    : existing.getQuantityUnitsRequested();

            // Use database function to add stock to requesting hospital
            addStockFromTransfer(
                existing.getRequestingHospitalId(),
                existing.getBloodTypeId(),
                quantityDelivered
            );

            // Use database function to deduct stock from approving hospital
            boolean deductionSuccess = deductStockForTransfer(
                existing.getApprovingHospitalId(),
                existing.getBloodTypeId(),
                quantityDelivered
            );

            if (!deductionSuccess) {
                throw new RuntimeException(
                    "Insufficient stock at approving hospital for transfer delivery"
                );
            }

            // Set actual delivery timestamp
            if (updated.getActualDeliveryAt() == null) {
                updated.setActualDeliveryAt(
                    new Timestamp(System.currentTimeMillis())
                );
            }

            LOGGER.info(
                "Transfer delivery processed: " +
                    quantityDelivered +
                    " units of " +
                    existing.getBloodTypeId() +
                    " transferred from hospital " +
                    existing.getApprovingHospitalId() +
                    " to " +
                    existing.getRequestingHospitalId()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing transfer delivery", e);
            throw new RuntimeException(
                "Error processing transfer delivery: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Adds stock to a hospital's inventory via the database function
     */
    private void addStockFromTransfer(
        UUID hospitalId,
        String bloodTypeId,
        Integer units
    ) {
        try (
            CallableStatement cs = connection.prepareCall(
                "{CALL fn_add_donation_to_inventory(?, ?, ?)}"
            )
        ) {
            cs.setObject(1, hospitalId);
            cs.setString(2, bloodTypeId);
            cs.setInt(3, units);
            cs.execute();
            LOGGER.info(
                "Added " +
                    units +
                    " units of " +
                    bloodTypeId +
                    " to hospital " +
                    hospitalId
            );
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding stock to hospital", e);
            throw new RuntimeException(
                "Error adding stock to hospital: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Deducts stock from a hospital's inventory via the database function
     */
    private boolean deductStockForTransfer(
        UUID hospitalId,
        String bloodTypeId,
        Integer units
    ) {
        try (
            CallableStatement cs = connection.prepareCall(
                "{? = CALL fn_deduct_from_inventory(?, ?, ?)}"
            )
        ) {
            cs.registerOutParameter(1, Types.BOOLEAN);
            cs.setObject(2, hospitalId);
            cs.setString(3, bloodTypeId);
            cs.setInt(4, units);
            cs.execute();
            boolean success = cs.getBoolean(1);
            LOGGER.info(
                "Deducted " +
                    units +
                    " units of " +
                    bloodTypeId +
                    " from hospital " +
                    hospitalId +
                    ": " +
                    success
            );
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deducting stock from hospital", e);
            throw new RuntimeException(
                "Error deducting stock from hospital: " + e.getMessage(),
                e
            );
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

    /**
     * Get transfers for a specific hospital (both as requesting and approving)
     */
    public List<BloodTransferRequest> getTransfersForHospital(UUID hospitalId) {
        if (hospitalId == null) {
            throw new IllegalArgumentException("Hospital ID cannot be null");
        }

        List<BloodTransferRequest> requesting =
            getTransfersByRequestingHospital(hospitalId);
        List<BloodTransferRequest> approving = getTransfersByApprovingHospital(
            hospitalId
        );

        // Combine lists and sort by date
        List<BloodTransferRequest> combined = new ArrayList<>(requesting);
        combined.addAll(approving);

        return combined
            .stream()
            .sorted((a, b) -> {
                if (a.getRequestedAt() == null) return 1;
                if (b.getRequestedAt() == null) return -1;
                return b.getRequestedAt().compareTo(a.getRequestedAt());
            })
            .collect(Collectors.toList());
    }

    /**
     * Get pending transfers for a hospital that needs approval
     */
    public List<BloodTransferRequest> getPendingTransfersForApproval(
        UUID hospitalId
    ) {
        if (hospitalId == null) {
            throw new IllegalArgumentException("Hospital ID cannot be null");
        }

        List<BloodTransferRequest> transfers = new ArrayList<>();
        try (
            PreparedStatement ps = connection.prepareStatement(
                SELECT_PENDING_FOR_HOSPITAL
            )
        ) {
            ps.setObject(1, hospitalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapResultSetToTransfert(rs));
                }
            }
            LOGGER.info(
                "Retrieved " +
                    transfers.size() +
                    " pending transfer requests for hospital " +
                    hospitalId
            );
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error retrieving pending transfers for hospital",
                e
            );
        }
        return transfers;
    }

    /**
     * Get transfers by requesting hospital
     */
    public List<BloodTransferRequest> getTransfersByRequestingHospital(
        UUID hospitalId
    ) {
        if (hospitalId == null) {
            throw new IllegalArgumentException("Hospital ID cannot be null");
        }

        List<BloodTransferRequest> transfers = new ArrayList<>();
        try (
            PreparedStatement ps = connection.prepareStatement(
                SELECT_BY_REQUESTING_HOSPITAL
            )
        ) {
            ps.setObject(1, hospitalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapResultSetToTransfert(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error retrieving transfers by requesting hospital",
                e
            );
        }
        return transfers;
    }

    /**
     * Get transfers by approving hospital
     */
    public List<BloodTransferRequest> getTransfersByApprovingHospital(
        UUID hospitalId
    ) {
        if (hospitalId == null) {
            throw new IllegalArgumentException("Hospital ID cannot be null");
        }

        List<BloodTransferRequest> transfers = new ArrayList<>();
        try (
            PreparedStatement ps = connection.prepareStatement(
                SELECT_BY_APPROVING_HOSPITAL
            )
        ) {
            ps.setObject(1, hospitalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapResultSetToTransfert(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error retrieving transfers by approving hospital",
                e
            );
        }
        return transfers;
    }

    /**
     * Get transfers by status (for admin dashboard)
     */
    public List<BloodTransferRequest> getTransfersByStatus(
        TransfertStatus status
    ) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        List<BloodTransferRequest> transfers = new ArrayList<>();
        try (
            PreparedStatement ps = connection.prepareStatement(SELECT_BY_STATUS)
        ) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapResultSetToTransfert(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(
                "Error retrieving transfers by status",
                e
            );
        }
        return transfers;
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

        // Handle NULL for quantity_units_approved
        int quantityApproved = rs.getInt("quantity_units_approved");
        transfert.setQuantityUnitsApproved(
            rs.wasNull() ? null : quantityApproved
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
