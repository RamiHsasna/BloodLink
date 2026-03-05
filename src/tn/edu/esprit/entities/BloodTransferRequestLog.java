package tn.edu.esprit.entities;

import java.sql.Timestamp;

public class BloodTransferRequestLog {
    private String logId;
    private int transferId;
    private TransferLogAction action;
    private String previousStatus;
    private String newStatus;
    private String changedBy;
    private String notes;
    private Timestamp createdAt;

    public BloodTransferRequestLog() {
    }

    public BloodTransferRequestLog(String logId, int transferId, TransferLogAction action, String previousStatus,
            String newStatus, String changedBy, String notes, Timestamp createdAt) {
        this.logId = logId;
        this.transferId = transferId;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public int getTransferId() {
        return transferId;
    }

    public void setTransferId(int transferId) {
        this.transferId = transferId;
    }

    public TransferLogAction getAction() {
        return action;
    }

    public void setAction(TransferLogAction action) {
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

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
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
        return "BloodTransferRequestLog{" +
                "logId='" + logId + '\'' +
                ", transferId=" + transferId +
                ", action=" + action +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", changedBy='" + changedBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
