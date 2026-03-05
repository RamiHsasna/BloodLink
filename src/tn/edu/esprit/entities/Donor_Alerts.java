package tn.edu.esprit.entities;

import java.time.LocalDate;
import java.util.UUID;

public class Donor_Alerts {

    private UUID donor_alert_id;
    private UUID alert_id;
    private UUID donor_id;
    private boolean is_notified;
    private LocalDate notification_sent_at;
    private boolean is_read;
    private LocalDate read_at;
    private String donor_response;

    public Donor_Alerts() {
    }

    public Donor_Alerts(UUID donor_alert_id, UUID alert_id, UUID donor_id, boolean is_notified,
            LocalDate notification_sent_at, boolean is_read, LocalDate read_at, String donor_response) {
        this.donor_alert_id = donor_alert_id;
        this.alert_id = alert_id;
        this.donor_id = donor_id;
        this.is_notified = is_notified;
        this.notification_sent_at = notification_sent_at;
        this.is_read = is_read;
        this.read_at = read_at;
        this.donor_response = donor_response;
    }

    public Donor_Alerts(UUID alert_id, UUID donor_id, boolean is_notified, LocalDate notification_sent_at,
            boolean is_read, LocalDate read_at, String donor_response) {
        this(null, alert_id, donor_id, is_notified, notification_sent_at, is_read, read_at, donor_response);
    }

    public UUID getDonor_alert_id() {
        return donor_alert_id;
    }

    public void setDonor_alert_id(UUID donor_alert_id) {
        this.donor_alert_id = donor_alert_id;
    }

    public UUID getAlert_id() {
        return alert_id;
    }

    public void setAlert_id(UUID alert_id) {
        this.alert_id = alert_id;
    }

    public UUID getDonor_id() {
        return donor_id;
    }

    public void setDonor_id(UUID donor_id) {
        this.donor_id = donor_id;
    }

    public boolean getIs_notified() {
        return is_notified;
    }

    public void setIs_notified(boolean is_notified) {
        this.is_notified = is_notified;
    }

    public LocalDate getNotification_sent_at() {
        return notification_sent_at;
    }

    public void setNotification_sent_at(LocalDate notification_sent_at) {
        this.notification_sent_at = notification_sent_at;
    }

    public boolean getIs_read() {
        return is_read;
    }

    public void setIs_read(boolean is_read) {
        this.is_read = is_read;
    }

    public LocalDate getRead_at() {
        return read_at;
    }

    public void setRead_at(LocalDate read_at) {
        this.read_at = read_at;
    }

    public String getDonor_response() {
        return donor_response;
    }

    public void setDonor_response(String donor_response) {
        this.donor_response = donor_response;
    }

    @Override
    public String toString() {
        return "Donor_Alerts{" + "donor_alert_id=" + donor_alert_id + ", alert_id=" + alert_id + ", donor_id="
                + donor_id + ", is_notified=" + is_notified + ", notification_sent_at=" + notification_sent_at
                + ", is_read=" + is_read + ", read_at=" + read_at + ", donor_response=" + donor_response + '}';

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.donor_alert_id != null ? this.donor_alert_id.hashCode() : 0);
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
        final Donor_Alerts other = (Donor_Alerts) obj;
        if (this.donor_alert_id == null) {
            return other.donor_alert_id == null;
        }
        return this.donor_alert_id.equals(other.donor_alert_id);
    }

}
