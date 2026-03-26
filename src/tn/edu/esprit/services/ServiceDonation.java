package tn.edu.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import tn.edu.esprit.Tools.DataSource;
import tn.edu.esprit.entities.Donations;

/**
 * ServiceDonation manages donation records and integrates with the inventory system.
 *
 * WORKFLOW:
 * 1. When a donation is recorded (ajouter), the donation row is inserted into the donations table.
 * 2. After successful insertion, the inventory service is called to increment the stock
 *    for that hospital and blood type.
 * 3. The inventory update happens via addStockFromDonation, which uses an upsert pattern
 *    to either create a new inventory row or increment the existing one.
 */
public class ServiceDonation implements IServiceDonation {

    private static final Logger LOGGER = Logger.getLogger(
        ServiceDonation.class.getName()
    );

    private Connection cnx;
    private InventoryServiceImpl inventoryService;

    public ServiceDonation() {
        this.cnx = DataSource.getInstance().getConnection();
        this.inventoryService = new InventoryServiceImpl();
    }

    /**
     * Adds a new donation and updates the inventory accordingly.
     *
     * @param o the Donations object to add
     */
    @Override
    public void ajouter(Object o) {
        Donations d = (Donations) o;
        String req =
            "INSERT INTO donations (user_id, hospital_id, donation_event_id, blood_type_id, donation_date, units_collected, volume_collected, status, screening_passed, medical_notes) " +
            "VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, d.getDonorId());
            pst.setString(2, d.getHospitalId());

            if (
                d.getDonationEventId() != null &&
                !d.getDonationEventId().isEmpty()
            ) {
                pst.setString(3, d.getDonationEventId());
            } else {
                pst.setNull(3, Types.OTHER);
            }

            pst.setString(4, d.getBloodTypeId());
            pst.setTimestamp(5, Timestamp.valueOf(d.getDonationDate()));
            pst.setInt(6, d.getUnitsCollected());

            if (d.getVolumeCollected() != null) {
                pst.setBigDecimal(7, d.getVolumeCollected());
            } else {
                pst.setNull(7, Types.DECIMAL);
            }

            pst.setString(8, d.getStatus());

            if (d.getScreeningPassed() != null) {
                pst.setBoolean(9, d.getScreeningPassed());
            } else {
                pst.setNull(9, Types.BOOLEAN);
            }

            if (d.getMedicalNotes() != null) {
                pst.setString(10, d.getMedicalNotes());
            } else {
                pst.setNull(10, Types.VARCHAR);
            }

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.info("Donation recorded successfully");

                // Update inventory after successful donation insertion
                try {
                    UUID hospitalId = UUID.fromString(d.getHospitalId());
                    String bloodTypeId = d.getBloodTypeId();
                    Integer units = d.getUnitsCollected();

                    inventoryService.addStockFromDonation(
                        hospitalId,
                        bloodTypeId,
                        units
                    );

                    LOGGER.info(
                        "Inventory updated: added " +
                            units +
                            " units of " +
                            bloodTypeId +
                            " to hospital " +
                            hospitalId
                    );
                } catch (Exception inventoryError) {
                    LOGGER.log(
                        Level.WARNING,
                        "Warning: Donation was recorded, but inventory update failed. " +
                            "Please manually verify inventory. Error: " +
                            inventoryError.getMessage(),
                        inventoryError
                    );
                    // Do not re-throw; the donation is recorded, inventory should be reconciled separately
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(
                Level.SEVERE,
                "Error recording donation: " + ex.getMessage(),
                ex
            );
            throw new RuntimeException("Error recording donation", ex);
        }
    }

    /**
     * Modifies an existing donation record.
     *
     * @param o the Donations object to modify
     */
    @Override
    public void modifier(Object o) {
        Donations d = (Donations) o;
        String req =
            "UPDATE donations " +
            "SET user_id=CAST(? AS uuid), hospital_id=CAST(? AS uuid), donation_event_id=CAST(? AS uuid), " +
            "blood_type_id=?, donation_date=?, units_collected=?, volume_collected=?, status=?, " +
            "screening_passed=?, medical_notes=? " +
            "WHERE donation_id=CAST(? AS uuid)";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, d.getDonorId());
            pst.setString(2, d.getHospitalId());

            if (
                d.getDonationEventId() != null &&
                !d.getDonationEventId().isEmpty()
            ) {
                pst.setString(3, d.getDonationEventId());
            } else {
                pst.setNull(3, Types.OTHER);
            }

            pst.setString(4, d.getBloodTypeId());
            pst.setTimestamp(5, Timestamp.valueOf(d.getDonationDate()));
            pst.setInt(6, d.getUnitsCollected());

            if (d.getVolumeCollected() != null) {
                pst.setBigDecimal(7, d.getVolumeCollected());
            } else {
                pst.setNull(7, Types.DECIMAL);
            }

            pst.setString(8, d.getStatus());

            if (d.getScreeningPassed() != null) {
                pst.setBoolean(9, d.getScreeningPassed());
            } else {
                pst.setNull(9, Types.BOOLEAN);
            }

            if (d.getMedicalNotes() != null) {
                pst.setString(10, d.getMedicalNotes());
            } else {
                pst.setNull(10, Types.VARCHAR);
            }

            pst.setString(
                11,
                d.getDonationId() != null ? d.getDonationId() : ""
            );

            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info(
                    "Donation modified successfully: " + d.getDonationId()
                );
            } else {
                LOGGER.warning(
                    "No donation found with id: " + d.getDonationId()
                );
            }
        } catch (SQLException ex) {
            LOGGER.log(
                Level.SEVERE,
                "Error modifying donation: " + ex.getMessage(),
                ex
            );
            throw new RuntimeException("Error modifying donation", ex);
        }
    }

    /**
     * Deletes a donation record by ID.
     *
     * @param donationId the ID of the donation to delete
     */
    @Override
    public void supprimer(String donationId) {
        String req = "DELETE FROM donations WHERE donation_id=CAST(? AS uuid)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, donationId);
            int rows = pst.executeUpdate();

            if (rows > 0) {
                LOGGER.info("Donation deleted successfully: " + donationId);
            } else {
                LOGGER.warning("No donation found with id: " + donationId);
            }
        } catch (SQLException ex) {
            LOGGER.log(
                Level.SEVERE,
                "Error deleting donation: " + ex.getMessage(),
                ex
            );
            throw new RuntimeException("Error deleting donation", ex);
        }
    }

    /**
     * Retrieves all donations.
     *
     * @return a list of all Donations
     */
    @Override
    public List<Donations> getAll() {
        List<Donations> list = new ArrayList<>();
        String req = "SELECT * FROM donations ORDER BY donation_date DESC";

        try (
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req)
        ) {
            while (rs.next()) {
                Donations d = mapResultSetToDonation(rs);
                list.add(d);
            }
            LOGGER.info("Retrieved " + list.size() + " donations");
        } catch (SQLException ex) {
            LOGGER.log(
                Level.SEVERE,
                "Error retrieving all donations: " + ex.getMessage(),
                ex
            );
            throw new RuntimeException("Error retrieving all donations", ex);
        }

        return list;
    }

    /**
     * Retrieves a single donation by ID.
     *
     * @param donationId the ID of the donation
     * @return the Donations object, or null if not found
     */
    @Override
    public Donations getOne(String donationId) {
        Donations d = null;
        String req =
            "SELECT * FROM donations WHERE donation_id=CAST(? AS uuid)";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, donationId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                d = mapResultSetToDonation(rs);
            } else {
                LOGGER.warning("No donation found with id: " + donationId);
            }
        } catch (SQLException ex) {
            LOGGER.log(
                Level.SEVERE,
                "Error retrieving donation by id: " + ex.getMessage(),
                ex
            );
            throw new RuntimeException("Error retrieving donation by id", ex);
        }

        return d;
    }

    /**
     * Helper method to map a ResultSet row to a Donations object.
     *
     * @param rs the ResultSet positioned at the current row
     * @return a Donations instance
     * @throws SQLException if an error occurs accessing the ResultSet
     */
    private Donations mapResultSetToDonation(ResultSet rs) throws SQLException {
        Donations d = new Donations();
        d.setDonationId(rs.getString("donation_id"));
        d.setDonorId(rs.getString("user_id"));
        d.setHospitalId(rs.getString("hospital_id"));
        d.setDonationEventId(rs.getString("donation_event_id"));
        d.setBloodTypeId(rs.getString("blood_type_id"));

        Timestamp donationTimestamp = rs.getTimestamp("donation_date");
        if (donationTimestamp != null) {
            d.setDonationDate(donationTimestamp.toLocalDateTime());
        }

        d.setUnitsCollected(rs.getInt("units_collected"));
        d.setVolumeCollected(rs.getBigDecimal("volume_collected"));
        d.setStatus(rs.getString("status"));

        if (rs.getObject("screening_passed") != null) {
            d.setScreeningPassed(rs.getBoolean("screening_passed"));
        }

        d.setMedicalNotes(rs.getString("medical_notes"));

        Timestamp createdTimestamp = rs.getTimestamp("created_at");
        if (createdTimestamp != null) {
            d.setCreatedAt(createdTimestamp.toLocalDateTime());
        }

        return d;
    }

    // ============================================================
    // Interface Implementation Methods
    // ============================================================

    @Override
    public Object getDonation(Object o) {
        Donations d = (Donations) o;
        return getOne(d.getDonationId());
    }

    @Override
    public List getAllDonations() {
        return getAll();
    }

    @Override
    public boolean exists(String donationId) {
        return getOne(donationId) != null;
    }

    @Override
    public Object getDonationById(String donationId) {
        return getOne(donationId);
    }
}
