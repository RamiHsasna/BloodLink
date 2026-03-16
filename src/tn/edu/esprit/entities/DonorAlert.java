package tn.edu.esprit.entities;

import java.sql.Timestamp;

public class DonorAlert {
    private String donorAlertId;
    private String alertId;
    private String donorId;
    private Timestamp notificationSentAt;
    private Timestamp readAt;
    private DonorResponse donorResponse;
    private boolean isNotified;
    private boolean isRead;

    public DonorAlert() {
    }

    public DonorAlert(String donorAlertId, String alertId, String donorId, Timestamp notificationSentAt,
            Timestamp readAt, DonorResponse donorResponse, boolean isNotified, boolean isRead) {
        this.donorAlertId = donorAlertId;
        this.alertId = alertId;
        this.donorId = donorId;
        this.notificationSentAt = notificationSentAt;
        this.readAt = readAt;
        this.donorResponse = donorResponse;
        this.isNotified = isNotified;
        this.isRead = isRead;
    }

    public String getDonorAlertId() {
        return donorAlertId;
    }

    public void setDonorAlertId(String donorAlertId) {
        this.donorAlertId = donorAlertId;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public Timestamp getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(Timestamp notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }

    public Timestamp getReadAt() {
        return readAt;
    }

    public void setReadAt(Timestamp readAt) {
        this.readAt = readAt;
    }

    public DonorResponse getDonorResponse() {
        return donorResponse;
    }

    public void setDonorResponse(DonorResponse donorResponse) {
        this.donorResponse = donorResponse;
    }

    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified(boolean isNotified) {
        this.isNotified = isNotified;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "DonorAlert{" +
                "donorAlertId='" + donorAlertId + '\'' +
                ", alertId='" + alertId + '\'' +
                ", donorId='" + donorId + '\'' +
                ", donorResponse=" + donorResponse +
                ", isRead=" + isRead +
                '}';
    }
}
