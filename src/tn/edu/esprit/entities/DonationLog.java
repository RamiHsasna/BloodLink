package tn.edu.esprit.entities;

import java.sql.Timestamp;

public class DonationLog {
    private String logId;
    private String donationId;
    private DonationLogAction action;
    private String previousStatus;
    private String newStatus;
    private String loggedBy;
    private String notes;
    private Timestamp createdAt;

    public DonationLog() {
    }

    public DonationLog(String logId, String donationId, DonationLogAction action, String previousStatus,
            String newStatus, String loggedBy, String notes, Timestamp createdAt) {
        this.logId = logId;
        this.donationId = donationId;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.loggedBy = loggedBy;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getDonationId() {
        return donationId;
    }

    public void setDonationId(String donationId) {
        this.donationId = donationId;
    }

    public DonationLogAction getAction() {
        return action;
    }

    public void setAction(DonationLogAction action) {
        this.action = action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DonationLog{" +
                "logId='" + logId + '\'' +
                ", donationId='" + donationId + '\'' +
                ", action=" + action +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", loggedBy='" + loggedBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
