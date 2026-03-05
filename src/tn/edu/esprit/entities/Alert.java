package tn.edu.esprit.entities;

import java.sql.Timestamp;

public class Alert {
    private String alertId;
    private String hospitalId;
    private String staffId;
    private String bloodTypeId;
    private AlertSeverity severity;
    private int quantityNeeded;
    private String title;
    private String message;
    private Timestamp resolvedAt;
    private boolean isResolved;
    private int targetRadiusKm;
    private Timestamp createdAt;

    public Alert() {
    }

    public Alert(String alertId, String hospitalId, String staffId, String bloodTypeId, AlertSeverity severity,
            int quantityNeeded, String title, String message, Timestamp resolvedAt, boolean isResolved,
            int targetRadiusKm, Timestamp createdAt) {
        this.alertId = alertId;
        this.hospitalId = hospitalId;
        this.staffId = staffId;
        this.bloodTypeId = bloodTypeId;
        this.severity = severity;
        this.quantityNeeded = quantityNeeded;
        this.title = title;
        this.message = message;
        this.resolvedAt = resolvedAt;
        this.isResolved = isResolved;
        this.targetRadiusKm = targetRadiusKm;
        this.createdAt = createdAt;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getBloodTypeId() {
        return bloodTypeId;
    }

    public void setBloodTypeId(String bloodTypeId) {
        this.bloodTypeId = bloodTypeId;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public int getQuantityNeeded() {
        return quantityNeeded;
    }

    public void setQuantityNeeded(int quantityNeeded) {
        this.quantityNeeded = quantityNeeded;
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

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean isResolved) {
        this.isResolved = isResolved;
    }

    public int getTargetRadiusKm() {
        return targetRadiusKm;
    }

    public void setTargetRadiusKm(int targetRadiusKm) {
        this.targetRadiusKm = targetRadiusKm;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "alertId='" + alertId + '\'' +
                ", severity=" + severity +
                ", title='" + title + '\'' +
                ", isResolved=" + isResolved +
                ", createdAt=" + createdAt +
                '}';
    }
}
