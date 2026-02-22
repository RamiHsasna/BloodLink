package tn.edu.esprit.services;

import tn.edu.esprit.entities.DonationsEvent;
import tn.edu.esprit.Tools.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDonationsEvent implements IServicedon<DonationsEvent> {

    private Connection cnx;

    public ServiceDonationsEvent() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(DonationsEvent e) {
        try {
            String req = "INSERT INTO donation_events (hospital_id, name, description, start_date, end_date, location, latitude, longitude, target_blood_types, target_collection_units, actual_collection_units, status) VALUES (CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, e.getHospitalId());
            pst.setString(2, e.getName());
            pst.setString(3, e.getDescription());
            pst.setTimestamp(4, Timestamp.valueOf(e.getStartDate()));
            pst.setTimestamp(5, Timestamp.valueOf(e.getEndDate()));
            pst.setString(6, e.getLocation());
            pst.setBigDecimal(7, e.getLatitude());
            pst.setBigDecimal(8, e.getLongitude());
            pst.setString(9, e.getTargetBloodTypes());
            pst.setInt(10, e.getTargetCollectionUnits() != null ? e.getTargetCollectionUnits() : 0);
            pst.setInt(11, e.getActualCollectionUnits() != null ? e.getActualCollectionUnits() : 0);
            pst.setString(12, e.getStatus());
            
            pst.executeUpdate();
            System.out.println("Event ajouté avec succès !");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void modifier(DonationsEvent e) {
        try {
            DonationsEvent existing = getOne(e);
            if (existing == null) {
                System.out.println("⚠️ Aucun événement trouvé !");
                return;
            }

            String name = e.getName() != null ? e.getName() : existing.getName();
            String desc = e.getDescription() != null ? e.getDescription() : existing.getDescription();
            Timestamp start = e.getStartDate() != null ? Timestamp.valueOf(e.getStartDate()) : Timestamp.valueOf(existing.getStartDate());
            Timestamp end = e.getEndDate() != null ? Timestamp.valueOf(e.getEndDate()) : Timestamp.valueOf(existing.getEndDate());
            String location = e.getLocation() != null ? e.getLocation() : existing.getLocation();
            java.math.BigDecimal lat = e.getLatitude() != null ? e.getLatitude() : existing.getLatitude();
            java.math.BigDecimal lng = e.getLongitude() != null ? e.getLongitude() : existing.getLongitude();
            String targets = e.getTargetBloodTypes() != null ? e.getTargetBloodTypes() : existing.getTargetBloodTypes();
            Integer targetUnits = e.getTargetCollectionUnits() != null ? e.getTargetCollectionUnits() : existing.getTargetCollectionUnits();
            Integer actualUnits = e.getActualCollectionUnits() != null ? e.getActualCollectionUnits() : existing.getActualCollectionUnits();
            String status = e.getStatus() != null ? e.getStatus() : existing.getStatus();

            String req = "UPDATE donation_events SET hospital_id=CAST(? AS uuid), name=?, description=?, start_date=?, end_date=?, location=?, latitude=?, longitude=?, target_blood_types=?, target_collection_units=?, actual_collection_units=?, status=? WHERE event_id=CAST(? AS uuid)";

            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, e.getHospitalId());
            pst.setString(2, name);
            pst.setString(3, desc);
            pst.setTimestamp(4, start);
            pst.setTimestamp(5, end);
            pst.setString(6, location);
            pst.setBigDecimal(7, lat);
            pst.setBigDecimal(8, lng);
            pst.setString(9, targets);
            pst.setInt(10, targetUnits != null ? targetUnits : 0);
            pst.setInt(11, actualUnits != null ? actualUnits : 0);
            pst.setString(12, status);
            pst.setString(13, e.getEventId());

            int rows = pst.executeUpdate();
            System.out.println(rows > 0 ? "✅ Event modifié !" : "⚠️ Event introuvable !");
            System.out.println("👉 Event ID being updated: " + e.getEventId());
            System.out.println("👉 Hospital ID: " + e.getHospitalId());
        } catch (SQLException ex) {
            System.out.println("❌ Erreur modification : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void supprimer(int id) {
        try {
            String req = "DELETE FROM donation_events WHERE event_id=CAST(? AS uuid)";
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, String.valueOf(id));
            int rows = pst.executeUpdate();
            System.out.println(rows > 0 ? "Event supprimé !" : "Aucun event trouvé.");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void supprimer(String eventId) {
        try {
            String req = "DELETE FROM donation_events WHERE event_id=CAST(? AS uuid)";
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, eventId);
            int rows = pst.executeUpdate();
            System.out.println(rows > 0 ? "Event supprimé !" : "Aucun event trouvé.");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public List<DonationsEvent> getAll(DonationsEvent t) {
        List<DonationsEvent> list = new ArrayList<>();
        try {
            String req = "SELECT * FROM donation_events";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                DonationsEvent e = new DonationsEvent();
                e.setEventId(rs.getString("event_id"));
                e.setHospitalId(rs.getString("hospital_id"));
                e.setName(rs.getString("name"));
                e.setDescription(rs.getString("description"));
                Timestamp startTimestamp = rs.getTimestamp("start_date");
                if (startTimestamp != null)
                    e.setStartDate(startTimestamp.toLocalDateTime());
                Timestamp endTimestamp = rs.getTimestamp("end_date");
                if (endTimestamp != null)
                    e.setEndDate(endTimestamp.toLocalDateTime());
                e.setLocation(rs.getString("location"));
                e.setLatitude(rs.getBigDecimal("latitude"));
                e.setLongitude(rs.getBigDecimal("longitude"));
                e.setTargetBloodTypes(rs.getString("target_blood_types"));
                e.setTargetCollectionUnits(rs.getInt("target_collection_units"));
                e.setActualCollectionUnits(rs.getInt("actual_collection_units"));
                e.setStatus(rs.getString("status"));
                Timestamp createdTimestamp = rs.getTimestamp("created_at");
                if (createdTimestamp != null)
                    e.setCreatedAt(createdTimestamp.toLocalDateTime());
                Timestamp updatedTimestamp = rs.getTimestamp("updated_at");
                if (updatedTimestamp != null)
                    e.setUpdatedAt(updatedTimestamp.toLocalDateTime());
                list.add(e);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    @Override
    public DonationsEvent getOne(DonationsEvent e) {
        DonationsEvent event = null;
        try {
            String req = "SELECT * FROM donation_events WHERE event_id=CAST(? AS uuid)";
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, e.getEventId());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                event = new DonationsEvent();
                event.setEventId(rs.getString("event_id"));
                event.setHospitalId(rs.getString("hospital_id"));
                event.setName(rs.getString("name"));
                event.setDescription(rs.getString("description"));
                Timestamp startTimestamp = rs.getTimestamp("start_date");
                if (startTimestamp != null)
                    event.setStartDate(startTimestamp.toLocalDateTime());
                Timestamp endTimestamp = rs.getTimestamp("end_date");
                if (endTimestamp != null)
                    event.setEndDate(endTimestamp.toLocalDateTime());
                event.setLocation(rs.getString("location"));
                event.setLatitude(rs.getBigDecimal("latitude"));
                event.setLongitude(rs.getBigDecimal("longitude"));
                event.setTargetBloodTypes(rs.getString("target_blood_types"));
                event.setTargetCollectionUnits(rs.getInt("target_collection_units"));
                event.setActualCollectionUnits(rs.getInt("actual_collection_units"));
                event.setStatus(rs.getString("status"));
                Timestamp createdTimestamp = rs.getTimestamp("created_at");
                if (createdTimestamp != null)
                    event.setCreatedAt(createdTimestamp.toLocalDateTime());
                Timestamp updatedTimestamp = rs.getTimestamp("updated_at");
                if (updatedTimestamp != null)
                    event.setUpdatedAt(updatedTimestamp.toLocalDateTime());
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return event;
    }
}