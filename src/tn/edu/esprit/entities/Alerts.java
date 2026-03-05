package tn.edu.esprit.entities;

import java.time.LocalDate;
import java.util.UUID;

public class Alerts {

    private UUID alert_id;
    private UUID hospital_id;
    private UUID staff_id;
    private String blood_type_id;
    private String severity;
    private int quantity_needed;
    private String title;
    private String message;
    private boolean is_resolved;
    private Integer target_radius_km;
    private LocalDate created_at;
    private LocalDate resolved_at;

    public Alerts() {
    }

    public Alerts(UUID alert_id, UUID hospital_id, UUID staff_id, String blood_type_id, String severity,
            int quantity_needed, String title, String message, boolean is_resolved, Integer target_radius_km,
            LocalDate created_at, LocalDate resolved_at) {
        this.alert_id = alert_id;
        this.hospital_id = hospital_id;
        this.staff_id = staff_id;
        this.blood_type_id = blood_type_id;
        this.severity = severity;
        this.quantity_needed = quantity_needed;
        this.title = title;
        this.message = message;
        this.is_resolved = is_resolved;
        this.target_radius_km = target_radius_km;
        this.created_at = created_at;
        this.resolved_at = resolved_at;
    }

    public Alerts(UUID hospital_id, UUID staff_id, String blood_type_id, String severity,
            int quantity_needed, String title, String message, boolean is_resolved, Integer target_radius_km,
            LocalDate created_at, LocalDate resolved_at) {
        this(null, hospital_id, staff_id, blood_type_id, severity, quantity_needed, title, message, is_resolved,
                target_radius_km, created_at, resolved_at);
    }

    public UUID getAlert_id() {
        return alert_id;
    }

    public void setAlert_id(UUID alert_id) {
        this.alert_id = alert_id;
    }

    public UUID getHospital_id() {
        return hospital_id;
    }

    public void setHospital_id(UUID hospital_id) {
        this.hospital_id = hospital_id;
    }

    public UUID getStaff_id() {
        return staff_id;
    }

    public void setStaff_id(UUID staff_id) {
        this.staff_id = staff_id;
    }

    public String getBlood_type_id() {
        return blood_type_id;
    }

    public void setBlood_type_id(String blood_type_id) {
        this.blood_type_id = blood_type_id;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getQuantity_needed() {
        return quantity_needed;
    }

    public void setQuantity_needed(int quantity_needed) {
        this.quantity_needed = quantity_needed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getIs_resolved() {
        return is_resolved;
    }

    public void setIs_resolved(boolean is_resolved) {
        this.is_resolved = is_resolved;
    }

    public Integer getTarget_radius_km() {
        return target_radius_km;
    }

    public void setTarget_radius_km(Integer target_radius_km) {
        this.target_radius_km = target_radius_km;
    }

    public LocalDate getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDate created_at) {
        this.created_at = created_at;
    }

    public LocalDate getResolved_at() {
        return resolved_at;
    }

    public void setResolved_at(LocalDate resolved_at) {
        this.resolved_at = resolved_at;
    }

    @Override
    public String toString() {
        return "Alerts{" + "alert_id=" + alert_id + ", hospital_id=" + hospital_id + ", staff_id=" + staff_id
                + ", blood_type_id=" + blood_type_id + ", severity=" + severity + ", quantity_needed="
                + quantity_needed + ", title=" + title + ", message=" + message + ", is_resolved=" + is_resolved
                + ", target_radius_km=" + target_radius_km + ", created_at=" + created_at + ", resolved_at="
                + resolved_at + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.alert_id != null ? this.alert_id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Alerts other = (Alerts) obj;
        if (this.alert_id == null) {
            return other.alert_id == null;
        }
        return this.alert_id.equals(other.alert_id);
    }

}
