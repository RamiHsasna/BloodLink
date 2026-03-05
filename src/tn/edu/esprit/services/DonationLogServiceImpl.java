package tn.edu.esprit.services;

import tn.edu.esprit.Tools.DataSource;
import tn.edu.esprit.entities.DonationLog;
import tn.edu.esprit.entities.DonationLogAction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonationLogServiceImpl implements DonationLogService {
    private Connection cnx;

    public DonationLogServiceImpl() {
        cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(DonationLog log) {
        String req = "INSERT INTO donation_log (donation_id, action, previous_status, new_status, logged_by, notes) VALUES (?::uuid, ?, ?, ?, ?::uuid, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, log.getDonationId());
            pst.setString(2, log.getAction().name());
            pst.setString(3, log.getPreviousStatus());
            pst.setString(4, log.getNewStatus());
            pst.setString(5, log.getLoggedBy());
            pst.setString(6, log.getNotes());
            pst.executeUpdate();
            System.out.println("DonationLog ajouté avec succès !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec ajout DonationLog: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifier(DonationLog log) {
        String req = "UPDATE donation_log SET action=?, previous_status=?, new_status=?, notes=? WHERE log_id=?::uuid";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, log.getAction().name());
            pst.setString(2, log.getPreviousStatus());
            pst.setString(3, log.getNewStatus());
            pst.setString(4, log.getNotes());
            pst.setString(5, log.getLogId());
            pst.executeUpdate();
            System.out.println("DonationLog modifié !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec modification DonationLog: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(String logId) {
        String req = "DELETE FROM donation_log WHERE log_id=?::uuid";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, logId);
            pst.executeUpdate();
            System.out.println("DonationLog supprimé !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec suppression DonationLog: " + e.getMessage(), e);
        }
    }

    public boolean donationExists(String donationId) {
        String req = "SELECT 1 FROM donations WHERE donation_id = ?::uuid LIMIT 1";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, donationId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Verification donation impossible: " + e.getMessage(), e);
        }
    }

    @Override
    public DonationLog getById(String logId) {
        String req = "SELECT * FROM donation_log WHERE log_id=?::uuid";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, logId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return extractLog(rs);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public List<DonationLog> getAll() {
        List<DonationLog> list = new ArrayList<>();
        String req = "SELECT * FROM donation_log ORDER BY created_at DESC";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                list.add(extractLog(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public List<DonationLog> getByDonationId(String donationId) {
        List<DonationLog> list = new ArrayList<>();
        String req = "SELECT * FROM donation_log WHERE donation_id=?::uuid ORDER BY created_at DESC";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, donationId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(extractLog(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public List<DonationLog> getByAction(DonationLogAction action) {
        List<DonationLog> list = new ArrayList<>();
        String req = "SELECT * FROM donation_log WHERE action=? ORDER BY created_at DESC";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, action.name());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(extractLog(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    private DonationLog extractLog(ResultSet rs) throws SQLException {
        return new DonationLog(
                rs.getString("log_id"),
                rs.getString("donation_id"),
                DonationLogAction.valueOf(rs.getString("action")),
                rs.getString("previous_status"),
                rs.getString("new_status"),
                rs.getString("logged_by"),
                rs.getString("notes"),
                rs.getTimestamp("created_at"));
    }
}
