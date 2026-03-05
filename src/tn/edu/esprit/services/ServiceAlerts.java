package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import tn.edu.esprit.entities.Alerts;
import tn.edu.esprit.tools.DataSource;

public class ServiceAlerts implements IService<Alerts, UUID> {

    Connection cnx;

    public ServiceAlerts() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    private String toSqlString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "NULL";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private String toSqlUuid(UUID value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.toString() + "'";
    }

    private String toSqlInt(Integer value) {
        if (value == null) {
            return "NULL";
        }
        return value.toString();
    }

    private String toSqlDate(LocalDate value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.toString() + "'";
    }

    private LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toLocalDate();
    }

    @Override
    public void ajouter(Alerts t) {
        try {
            String targetRadiusSql = t.getTarget_radius_km() == null ? "DEFAULT" : t.getTarget_radius_km().toString();
            String createdAtSql = t.getCreated_at() == null ? "DEFAULT" : toSqlDate(t.getCreated_at());
            String req = "INSERT INTO alerts (hospital_id, staff_id, blood_type_id, severity, quantity_needed, title, "
                    + "message, is_resolved, target_radius_km, created_at, resolved_at) VALUES ("
                + toSqlUuid(t.getHospital_id()) + "," + toSqlUuid(t.getStaff_id()) + ","
                    + toSqlString(t.getBlood_type_id()) + "," + toSqlString(t.getSeverity()) + ","
                    + t.getQuantity_needed() + "," + toSqlString(t.getTitle()) + ","
                    + toSqlString(t.getMessage()) + "," + t.getIs_resolved() + ","
                    + targetRadiusSql + "," + createdAtSql + ","
                + toSqlDate(t.getResolved_at()) + ")";
            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void modifier(Alerts t) {
        try {
            String req = "UPDATE alerts SET hospital_id=" + toSqlUuid(t.getHospital_id()) + ",staff_id="
                    + toSqlUuid(t.getStaff_id()) + ",blood_type_id=" + toSqlString(t.getBlood_type_id())
                    + ",severity=" + toSqlString(t.getSeverity()) + ",quantity_needed=" + t.getQuantity_needed()
                    + ",title=" + toSqlString(t.getTitle()) + ",message=" + toSqlString(t.getMessage())
                    + ",is_resolved=" + t.getIs_resolved() + ",target_radius_km="
                    + toSqlInt(t.getTarget_radius_km()) + ",created_at=" + toSqlDate(t.getCreated_at())
                    + ",resolved_at=" + toSqlDate(t.getResolved_at()) + " WHERE alert_id="
                    + toSqlUuid(t.getAlert_id());
            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void supprimer(UUID id) {
        try {
            String req = "DELETE FROM alerts WHERE alert_id=" + toSqlUuid(id);
            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public Alerts getOne(Alerts t) {
        try {
            String req = "SELECT * FROM alerts WHERE alert_id=" + toSqlUuid(t.getAlert_id());
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            if (rs.next()) {
                Integer targetRadius = rs.getObject("target_radius_km") == null ? null : rs.getInt("target_radius_km");
                return new Alerts(UUID.fromString(rs.getString("alert_id")),
                        UUID.fromString(rs.getString("hospital_id")), UUID.fromString(rs.getString("staff_id")),
                        rs.getString("blood_type_id"), rs.getString("severity"), rs.getInt("quantity_needed"),
                        rs.getString("title"), rs.getString("message"), rs.getBoolean("is_resolved"), targetRadius,
                        toLocalDate(rs, "created_at"), toLocalDate(rs, "resolved_at"));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    @Override
    public List<Alerts> getAll(Alerts t) {
        List<Alerts> alerts = new ArrayList();
        try {
            String req = "SELECT * FROM alerts";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                Integer targetRadius = rs.getObject("target_radius_km") == null ? null : rs.getInt("target_radius_km");
                alerts.add(new Alerts(UUID.fromString(rs.getString("alert_id")),
                        UUID.fromString(rs.getString("hospital_id")),
                        UUID.fromString(rs.getString("staff_id")), rs.getString("blood_type_id"),
                        rs.getString("severity"), rs.getInt("quantity_needed"), rs.getString("title"),
                        rs.getString("message"), rs.getBoolean("is_resolved"), targetRadius,
                        toLocalDate(rs, "created_at"), toLocalDate(rs, "resolved_at")));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return alerts;
    }

    public List<UUID> getHospitalIds() {
        return fetchUuidList("SELECT hospital_id FROM hospitals");
    }

    public List<UUID> getStaffIds() {
        return fetchUuidList("SELECT staff_id FROM hospital_staff");
    }

    private List<UUID> fetchUuidList(String query) {
        List<UUID> ids = new ArrayList<>();
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(query);
            while (rs.next()) {
                String value = rs.getString(1);
                if (value != null && !value.trim().isEmpty()) {
                    try {
                        ids.add(UUID.fromString(value));
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Invalid UUID: " + value);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return ids;
    }

}
