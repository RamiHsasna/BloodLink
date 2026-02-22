package tn.edu.esprit.services;

import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.Tools.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDonation implements IServiceDonation {

    private Connection cnx;

    public ServiceDonation() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Object o) {
        Donations d = (Donations) o;
        String req = "INSERT INTO donations (user_id, hospital_id, donation_event_id, blood_type_id, donation_date, units_collected, volume_collected, status, screening_passed, medical_notes) VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, d.getDonorId());
            pst.setString(2, d.getHospitalId());
            if (d.getDonationEventId() != null && !d.getDonationEventId().isEmpty()) {
                pst.setString(3, d.getDonationEventId());
            } else {
                pst.setNull(3, Types.OTHER);
            }
            pst.setString(4, d.getBloodTypeId());
            pst.setTimestamp(5, Timestamp.valueOf(d.getDonationDate()));
            pst.setInt(6, d.getUnitsCollected());
            if (d.getVolumeCollected() != null)
                pst.setBigDecimal(7, d.getVolumeCollected());
            else
                pst.setNull(7, Types.DECIMAL);
            pst.setString(8, d.getStatus());
            if (d.getScreeningPassed() != null)
                pst.setBoolean(9, d.getScreeningPassed());
            else
                pst.setNull(9, Types.BOOLEAN);
            if (d.getMedicalNotes() != null)
                pst.setString(10, d.getMedicalNotes());
            else
                pst.setNull(10, Types.VARCHAR);

            pst.executeUpdate();
            System.out.println("Donation ajoutée avec succès !");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void modifier(Object o) {
        Donations d = (Donations) o;
        String req = "UPDATE donations SET user_id=CAST(? AS uuid), hospital_id=CAST(? AS uuid), donation_event_id=CAST(? AS uuid), blood_type_id=?, donation_date=?, units_collected=?, volume_collected=?, status=?, screening_passed=?, medical_notes=? WHERE donation_id=CAST(? AS uuid)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, d.getDonorId());
            pst.setString(2, d.getHospitalId());
            if (d.getDonationEventId() != null && !d.getDonationEventId().isEmpty()) {
                pst.setString(3, d.getDonationEventId());
            } else {
                pst.setNull(3, Types.OTHER);
            }
            pst.setString(4, d.getBloodTypeId());
            pst.setTimestamp(5, Timestamp.valueOf(d.getDonationDate()));
            pst.setInt(6, d.getUnitsCollected());
            if (d.getVolumeCollected() != null)
                pst.setBigDecimal(7, d.getVolumeCollected());
            else
                pst.setNull(7, Types.DECIMAL);
            pst.setString(8, d.getStatus());
            if (d.getScreeningPassed() != null)
                pst.setBoolean(9, d.getScreeningPassed());
            else
                pst.setNull(9, Types.BOOLEAN);
            if (d.getMedicalNotes() != null)
                pst.setString(10, d.getMedicalNotes());
            else
                pst.setNull(10, Types.VARCHAR);
            pst.setString(11, d.getDonationId() != null ? d.getDonationId() : "");

            pst.executeUpdate();
            System.out.println("Donation modifiée !");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void supprimer(String donationId) {
        String req = "DELETE FROM donations WHERE donation_id=CAST(? AS uuid)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, donationId);
            int rows = pst.executeUpdate();
            System.out.println(rows > 0 ? "Donation supprimée !" : "Aucune donation trouvée.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<Donations> getAll() {
        List<Donations> list = new ArrayList<>();
        String req = "SELECT * FROM donations";
        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(req)) {

            while (rs.next()) {
                Donations d = new Donations();
                d.setDonationId(rs.getString("donation_id"));
                d.setDonorId(rs.getString("user_id"));
                d.setHospitalId(rs.getString("hospital_id"));
                d.setDonationEventId(rs.getString("donation_event_id"));
                d.setBloodTypeId(rs.getString("blood_type_id"));
                Timestamp donationTimestamp = rs.getTimestamp("donation_date");
                if (donationTimestamp != null)
                    d.setDonationDate(donationTimestamp.toLocalDateTime());
                d.setUnitsCollected(rs.getInt("units_collected"));
                d.setVolumeCollected(rs.getBigDecimal("volume_collected"));
                d.setStatus(rs.getString("status"));
                if (rs.getObject("screening_passed") != null)
                    d.setScreeningPassed(rs.getBoolean("screening_passed"));
                d.setMedicalNotes(rs.getString("medical_notes"));
                Timestamp createdTimestamp = rs.getTimestamp("created_at");
                if (createdTimestamp != null)
                    d.setCreatedAt(createdTimestamp.toLocalDateTime());
                list.add(d);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    @Override
    public Donations getOne(String donationId) {
        Donations d = null;
        String req = "SELECT * FROM donations WHERE donation_id=CAST(? AS uuid)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, donationId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                d = new Donations();
                d.setDonationId(rs.getString("donation_id"));
                d.setDonorId(rs.getString("user_id"));
                d.setHospitalId(rs.getString("hospital_id"));
                d.setDonationEventId(rs.getString("donation_event_id"));
                d.setBloodTypeId(rs.getString("blood_type_id"));
                Timestamp donationTimestamp = rs.getTimestamp("donation_date");
                if (donationTimestamp != null)
                    d.setDonationDate(donationTimestamp.toLocalDateTime());
                d.setUnitsCollected(rs.getInt("units_collected"));
                d.setVolumeCollected(rs.getBigDecimal("volume_collected"));
                d.setStatus(rs.getString("status"));
                if (rs.getObject("screening_passed") != null)
                    d.setScreeningPassed(rs.getBoolean("screening_passed"));
                d.setMedicalNotes(rs.getString("medical_notes"));
                Timestamp createdTimestamp = rs.getTimestamp("created_at");
                if (createdTimestamp != null)
                    d.setCreatedAt(createdTimestamp.toLocalDateTime());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return d;
    }

    // Interface methods
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