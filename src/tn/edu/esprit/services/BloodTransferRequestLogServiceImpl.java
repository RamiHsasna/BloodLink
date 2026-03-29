package tn.edu.esprit.services;

import tn.edu.esprit.Tools.DataSource;
import tn.edu.esprit.entities.BloodTransferRequestLog;
import tn.edu.esprit.entities.TransferLogAction;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BloodTransferRequestLogServiceImpl implements BloodTransferRequestLogService {
    private Connection cnx;

    public BloodTransferRequestLogServiceImpl() {
        cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(BloodTransferRequestLog log) {
        String req = "INSERT INTO blood_transfer_request_log (transfer_id, action, previous_status, new_status, changed_by, notes) VALUES (?, ?, ?, ?, ?::uuid, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, log.getTransferId());
            pst.setString(2, log.getAction().name());
            pst.setString(3, log.getPreviousStatus());
            pst.setString(4, log.getNewStatus());
            pst.setString(5, log.getChangedBy());
            pst.setString(6, log.getNotes());
            pst.executeUpdate();
            System.out.println("BloodTransferRequestLog ajouté !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec ajout journal transfert: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifier(BloodTransferRequestLog log) {
        String req = "UPDATE blood_transfer_request_log SET action=?, previous_status=?, new_status=?, notes=? WHERE log_id=?::uuid";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, log.getAction().name());
            pst.setString(2, log.getPreviousStatus());
            pst.setString(3, log.getNewStatus());
            pst.setString(4, log.getNotes());
            pst.setString(5, log.getLogId());
            pst.executeUpdate();
            System.out.println("BloodTransferRequestLog modifié !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec modification journal transfert: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(String logId) {
        String req = "DELETE FROM blood_transfer_request_log WHERE log_id=?::uuid";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, logId);
            pst.executeUpdate();
            System.out.println("BloodTransferRequestLog supprimé !");
        } catch (SQLException e) {
            throw new IllegalStateException("Echec suppression journal transfert: " + e.getMessage(), e);
        }
    }

    public boolean transferRequestExists(int transferId) {
        String req = "SELECT 1 FROM blood_transfer_request WHERE transfer_id = ? LIMIT 1";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, transferId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Verification transfert impossible: " + e.getMessage(), e);
        }
    }

    public String getCurrentTransferStatus(int transferId) {
        String req = "SELECT status FROM blood_transfer_request WHERE transfer_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, transferId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lecture du statut du transfert impossible: " + e.getMessage(), e);
        }
    }

    public List<String> getKnownTransferStatuses() {
        Set<String> statuses = new LinkedHashSet<>();
        String req = "SELECT status AS value FROM blood_transfer_request WHERE status IS NOT NULL " +
                "UNION SELECT previous_status AS value FROM blood_transfer_request_log WHERE previous_status IS NOT NULL " +
                "UNION SELECT new_status AS value FROM blood_transfer_request_log WHERE new_status IS NOT NULL " +
                "ORDER BY value";
        try (PreparedStatement pst = cnx.prepareStatement(req);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                String status = rs.getString("value");
                if (status != null && !status.isBlank()) {
                    statuses.add(status.trim());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Chargement des statuts de transfert impossible: " + e.getMessage(), e);
        }
        return new ArrayList<>(statuses);
    }

    @Override
    public BloodTransferRequestLog getById(String logId) {
        String req = "SELECT * FROM blood_transfer_request_log WHERE log_id=?::uuid";
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
    public List<BloodTransferRequestLog> getAll() {
        List<BloodTransferRequestLog> list = new ArrayList<>();
        String req = "SELECT * FROM blood_transfer_request_log ORDER BY created_at DESC";
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
    public List<BloodTransferRequestLog> getByTransferId(int transferId) {
        List<BloodTransferRequestLog> list = new ArrayList<>();
        String req = "SELECT * FROM blood_transfer_request_log WHERE transfer_id=? ORDER BY created_at DESC";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, transferId);
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
    public List<BloodTransferRequestLog> getByAction(TransferLogAction action) {
        List<BloodTransferRequestLog> list = new ArrayList<>();
        String req = "SELECT * FROM blood_transfer_request_log WHERE action=? ORDER BY created_at DESC";
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

    private BloodTransferRequestLog extractLog(ResultSet rs) throws SQLException {
        String actionValue = rs.getString("action");
        return new BloodTransferRequestLog(
                rs.getString("log_id"),
                rs.getInt("transfer_id"),
                normalizeTransferAction(actionValue),
                rs.getString("previous_status"),
                rs.getString("new_status"),
                rs.getString("changed_by"),
                rs.getString("notes"),
                rs.getTimestamp("created_at"));
    }

    private TransferLogAction normalizeTransferAction(String actionValue) {
        if (actionValue == null || actionValue.isBlank()) {
            return TransferLogAction.REQUESTED;
        }

        String normalized = actionValue.trim().toUpperCase();
        switch (normalized) {
            case "REQUEST_CREATED":
            case "PENDING_REVIEW":
                normalized = "REQUESTED";
                break;
            case "REQUEST_APPROVED":
                normalized = "APPROVED";
                break;
            case "DELIVERY_CONFIRMED":
                normalized = "CONFIRMED";
                break;
            default:
                break;
        }

        try {
            return TransferLogAction.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return TransferLogAction.REQUESTED;
        }
    }
}
