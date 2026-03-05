package tn.edu.esprit.services;

import tn.edu.esprit.Tools.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Resolves runtime IDs (user/staff/hospital) from the live database so inserts
 * never depend on hardcoded UUID placeholders.
 */
public class RuntimeContextService {

    public static final class AlertContext {
        private final String hospitalId;
        private final String staffId;

        public AlertContext(String hospitalId, String staffId) {
            this.hospitalId = hospitalId;
            this.staffId = staffId;
        }

        public String getHospitalId() {
            return hospitalId;
        }

        public String getStaffId() {
            return staffId;
        }
    }

    private final Connection cnx;

    public RuntimeContextService() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    public String resolveAuditUserId() {
        String envUserId = readFirstNonBlankEnv(
                "BLOODLINK_AUDIT_USER_ID",
                "BLOODLINK_STAFF_USER_ID",
                "BLOODLINK_USER_ID");

        if (envUserId != null) {
            if (userExists(envUserId)) {
                return envUserId;
            }
            throw new IllegalStateException(
                    "Utilisateur configure introuvable dans users: " + envUserId);
        }

        String fromHospitalStaff = querySingleString(
                "SELECT user_id::text FROM hospital_staff WHERE user_id IS NOT NULL LIMIT 1");
        if (fromHospitalStaff != null) {
            return fromHospitalStaff;
        }

        String fromUsers = querySingleString("SELECT user_id::text FROM users WHERE user_id IS NOT NULL LIMIT 1");
        if (fromUsers != null) {
            return fromUsers;
        }

        throw new IllegalStateException(
                "Aucun utilisateur disponible pour renseigner logged_by/changed_by. Verifiez table users.");
    }

    public AlertContext resolveAlertContext() {
        String envHospitalId = readFirstNonBlankEnv("BLOODLINK_HOSPITAL_ID");
        String envStaffId = readFirstNonBlankEnv("BLOODLINK_STAFF_USER_ID", "BLOODLINK_USER_ID");

        if (envHospitalId != null && envStaffId != null) {
            if (!hospitalExists(envHospitalId)) {
                throw new IllegalStateException("Hospital ID configure introuvable: " + envHospitalId);
            }
            if (!userExists(envStaffId)) {
                throw new IllegalStateException("Staff/User ID configure introuvable: " + envStaffId);
            }
            return new AlertContext(envHospitalId, envStaffId);
        }

        String[] fromHospitalStaff = querySinglePair(
                "SELECT hospital_id::text, user_id::text FROM hospital_staff " +
                        "WHERE hospital_id IS NOT NULL AND user_id IS NOT NULL LIMIT 1");
        if (fromHospitalStaff != null) {
            return new AlertContext(fromHospitalStaff[0], fromHospitalStaff[1]);
        }

        String hospitalId = envHospitalId != null ? envHospitalId
                : querySingleString("SELECT hospital_id::text FROM hospital WHERE hospital_id IS NOT NULL LIMIT 1");
        if (hospitalId == null) {
            hospitalId = querySingleString("SELECT hospital_id::text FROM hospitals WHERE hospital_id IS NOT NULL LIMIT 1");
        }
        if (hospitalId == null) {
            throw new IllegalStateException("Aucun hopital disponible (tables hospital/hospitals).");
        }

        String staffId = envStaffId != null ? envStaffId : resolveAuditUserId();
        return new AlertContext(hospitalId, staffId);
    }

    private boolean userExists(String userId) {
        return exists("SELECT 1 FROM users WHERE user_id = ?::uuid LIMIT 1", userId);
    }

    private boolean hospitalExists(String hospitalId) {
        if (exists("SELECT 1 FROM hospital WHERE hospital_id = ?::uuid LIMIT 1", hospitalId)) {
            return true;
        }
        return exists("SELECT 1 FROM hospitals WHERE hospital_id = ?::uuid LIMIT 1", hospitalId);
    }

    private boolean exists(String query, String value) {
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, value);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private String querySingleString(String query) {
        try (PreparedStatement pst = cnx.prepareStatement(query);
                ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    private String[] querySinglePair(String query) {
        try (PreparedStatement pst = cnx.prepareStatement(query);
                ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return new String[] { rs.getString(1), rs.getString(2) };
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    private String readFirstNonBlankEnv(String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}

