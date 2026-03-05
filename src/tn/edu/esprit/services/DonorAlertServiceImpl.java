package tn.edu.esprit.services;

import tn.edu.esprit.Tools.DataSource;
import tn.edu.esprit.entities.DonorAlert;
import tn.edu.esprit.entities.DonorResponse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonorAlertServiceImpl implements DonorAlertService {
    private Connection cnx;

    public DonorAlertServiceImpl() {
        cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(DonorAlert donorAlert) {
        String req = "INSERT INTO donor_alerts (donor_alert_id, alert_id, donor_id, notification_sent_at, read_at, donor_response, is_notified, is_read) " +
                "VALUES (COALESCE(?::uuid, gen_random_uuid()), ?::uuid, ?::uuid, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            String donorResponse = donorAlert.getDonorResponse() != null ? donorAlert.getDonorResponse().name() : null;
            pst.setString(1, donorAlert.getDonorAlertId());
            pst.setString(2, donorAlert.getAlertId());
            pst.setString(3, donorAlert.getDonorId());
            pst.setTimestamp(4, donorAlert.getNotificationSentAt());
            pst.setTimestamp(5, donorAlert.getReadAt());
            pst.setString(6, donorResponse);
            pst.setBoolean(7, donorAlert.isNotified());
            pst.setBoolean(8, donorAlert.isRead());
            pst.executeUpdate();
            System.out.println("Alerte donneur ajoutee avec succes !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec ajout donor_alert: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifier(DonorAlert donorAlert) {
        String req = "UPDATE donor_alerts SET notification_sent_at=?, read_at=?, donor_response=?, is_notified=?, is_read=? WHERE donor_alert_id=?::uuid";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            String donorResponse = donorAlert.getDonorResponse() != null ? donorAlert.getDonorResponse().name() : null;
            pst.setTimestamp(1, donorAlert.getNotificationSentAt());
            pst.setTimestamp(2, donorAlert.getReadAt());
            pst.setString(3, donorResponse);
            pst.setBoolean(4, donorAlert.isNotified());
            pst.setBoolean(5, donorAlert.isRead());
            pst.setString(6, donorAlert.getDonorAlertId());
            pst.executeUpdate();
            System.out.println("Alerte donneur mise a jour avec succes !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec mise a jour donor_alert: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(String donorAlertId) {
        String req = "DELETE FROM donor_alerts WHERE donor_alert_id=?::uuid";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, donorAlertId);
            pst.executeUpdate();
            System.out.println("Alerte donneur supprimee avec succes !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec suppression donor_alert: " + e.getMessage(), e);
        }
    }

    @Override
    public DonorAlert getById(String donorAlertId) {
        String req = "SELECT * FROM donor_alerts WHERE donor_alert_id=?::uuid";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, donorAlertId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return extractDonorAlert(rs);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public List<DonorAlert> getAll() {
        List<DonorAlert> list = new ArrayList<>();
        String req = "SELECT * FROM donor_alerts";
        // Order by doesn't have created_at in the donor_alerts table based on schema,
        // maybe we don't order by default
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                list.add(extractDonorAlert(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public List<DonorAlert> getByDonorId(String donorId) {
        List<DonorAlert> list = new ArrayList<>();
        String req = "SELECT * FROM donor_alerts WHERE donor_id=?::uuid";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, donorId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(extractDonorAlert(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public List<DonorAlert> getByAlertId(String alertId) {
        List<DonorAlert> list = new ArrayList<>();
        String req = "SELECT * FROM donor_alerts WHERE alert_id=?::uuid";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, alertId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(extractDonorAlert(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    private DonorAlert extractDonorAlert(ResultSet rs) throws SQLException {
        String responseRaw = rs.getString("donor_response");
        DonorResponse response = null;
        if (responseRaw != null && !responseRaw.isEmpty()) {
            try {
                response = DonorResponse.valueOf(responseRaw);
            } catch (IllegalArgumentException e) {
                // Ignore parsing errors for responses
            }
        }

        return new DonorAlert(
                rs.getString("donor_alert_id"),
                rs.getString("alert_id"),
                rs.getString("donor_id"),
                rs.getTimestamp("notification_sent_at"),
                rs.getTimestamp("read_at"),
                response,
                rs.getBoolean("is_notified"),
                rs.getBoolean("is_read"));
    }
}
