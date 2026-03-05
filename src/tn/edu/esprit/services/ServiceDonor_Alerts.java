package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import tn.edu.esprit.entities.Donor_Alerts;
import tn.edu.esprit.tools.DataSource;

public class ServiceDonor_Alerts implements IService<Donor_Alerts> {

    Connection cnx;

    public ServiceDonor_Alerts() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Donor_Alerts t) {
        try {
            String req = "INSERT INTO `donor_alerts`( `alert_id`, `donor_id`, `is_notified`, `notification_sent_at`, `is_read`, `read_at`, `donor_response`, `response_at`, `created_at`) VALUES ('"
                    + t.getAlert_id() + "','" + t.getDonor_id() + "','" + t.getIs_notified() + "','"
                    + t.getNotification_sent_at() + "','" + t.getIs_read() + "','" + t.getRead_at() + "','"
                    + t.getDonor_response() + "','" + t.getResponse_at() + "','" + t.getCreated_at() + "')";
            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void modifier(Donor_Alerts t) {
        try {
            String req = "UPDATE `donor_alerts` SET `alert_id`='" + t.getAlert_id() + "',`donor_id`='"
                    + t.getDonor_id() + "',`is_notified`='" + t.getIs_notified() + "',`notification_sent_at`='"
                    + t.getNotification_sent_at() + "',`is_read`='" + t.getIs_read() + "',`read_at`='"
                    + t.getRead_at() + "',`donor_response`='" + t.getDonor_response() + "',`response_at`='"
                    + t.getResponse_at() + "',`created_at`='" + t.getCreated_at() + "' WHERE `donor_alert_id`="
                    + t.getDonor_alert_id();
            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        try {
            String req = "DELETE FROM `donor_alerts` WHERE `donor_alert_id`=" + id;
            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public Donor_Alerts getOne(Donor_Alerts t) {
        try {
            String req = "SELECT * FROM `donor_alerts` WHERE `donor_alert_id`=" + t.getDonor_alert_id();
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            if (rs.next()) {
                return new Donor_Alerts(rs.getInt("donor_alert_id"), rs.getInt("alert_id"), rs.getInt("donor_id"),
                        rs.getString("is_notified"), rs.getString("notification_sent_at"), rs.getString("is_read"),
                        rs.getString("read_at"), rs.getString("donor_response"), rs.getString("response_at"),
                        rs.getString("created_at"));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    @Override
    public List<Donor_Alerts> getAll(Donor_Alerts t) {
        try {
            String req = "SELECT * FROM `donor_alerts`";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            List<Donor_Alerts> donorAlertsList = new ArrayList<>();
            while (rs.next()) {
                donorAlertsList.add(new Donor_Alerts(rs.getInt("donor_alert_id"), rs.getInt("alert_id"),
                        rs.getInt("donor_id"), rs.getString("is_notified"), rs.getString("notification_sent_at"),
                        rs.getString("is_read"), rs.getString("read_at"), rs.getString("donor_response"),
                        rs.getString("response_at"), rs.getString("created_at")));
            }
            return donorAlertsList;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

}
