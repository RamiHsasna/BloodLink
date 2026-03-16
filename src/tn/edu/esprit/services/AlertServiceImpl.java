package tn.edu.esprit.services;

import tn.edu.esprit.Tools.DataSource;
import tn.edu.esprit.entities.Alert;
import tn.edu.esprit.entities.AlertSeverity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertServiceImpl implements AlertService {
    private Connection cnx;

    public AlertServiceImpl() {
        cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Alert alert) {
        String req = "INSERT INTO alerts (alert_id, hospital_id, staff_id, blood_type_id, severity, quantity_needed, title, message, resolved_at, is_resolved, target_radius_km) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, alert.getAlertId());
            pst.setString(2, alert.getHospitalId());
            pst.setString(3, alert.getStaffId());
            pst.setString(4, alert.getBloodTypeId());
            pst.setString(5, alert.getSeverity().name());
            pst.setInt(6, alert.getQuantityNeeded());
            pst.setString(7, alert.getTitle());
            pst.setString(8, alert.getMessage());
            pst.setTimestamp(9, alert.getResolvedAt());
            pst.setBoolean(10, alert.isResolved());
            pst.setInt(11, alert.getTargetRadiusKm());
            pst.executeUpdate();
            System.out.println("Alerte ajoutee avec succes !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec ajout alerte: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifier(Alert alert) {
        String req = "UPDATE alerts SET severity=?, quantity_needed=?, title=?, message=?, resolved_at=?, is_resolved=?, target_radius_km=? WHERE alert_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, alert.getSeverity().name());
            pst.setInt(2, alert.getQuantityNeeded());
            pst.setString(3, alert.getTitle());
            pst.setString(4, alert.getMessage());
            pst.setTimestamp(5, alert.getResolvedAt());
            pst.setBoolean(6, alert.isResolved());
            pst.setInt(7, alert.getTargetRadiusKm());
            pst.setString(8, alert.getAlertId());
            pst.executeUpdate();
            System.out.println("Alerte mise a jour avec succes !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec mise a jour alerte: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(String alertId) {
        String req = "DELETE FROM alerts WHERE alert_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, alertId);
            pst.executeUpdate();
            System.out.println("Alerte supprimee avec succes !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec suppression alerte: " + e.getMessage(), e);
        }
    }

    @Override
    public Alert getById(String alertId) {
        String req = "SELECT * FROM alerts WHERE alert_id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, alertId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return extractAlert(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    @Override
    public List<Alert> getAll() {
        List<Alert> list = new ArrayList<>();
        String req = "SELECT * FROM alerts ORDER BY created_at DESC";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                list.add(extractAlert(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public List<Alert> getByHospitalId(String hospitalId) {
        List<Alert> list = new ArrayList<>();
        String req = "SELECT * FROM alerts WHERE hospital_id=? ORDER BY created_at DESC";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, hospitalId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(extractAlert(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    private Alert extractAlert(ResultSet rs) throws SQLException {
        String bloodTypeId = rs.getString("blood_type_id");
        return new Alert(
                rs.getString("alert_id"),
                rs.getString("hospital_id"),
                rs.getString("staff_id"),
                bloodTypeId != null ? bloodTypeId.trim() : null,
                AlertSeverity.valueOf(rs.getString("severity")),
                rs.getInt("quantity_needed"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getTimestamp("resolved_at"),
                rs.getBoolean("is_resolved"),
                rs.getInt("target_radius_km"),
                rs.getTimestamp("created_at"));
    }
}
