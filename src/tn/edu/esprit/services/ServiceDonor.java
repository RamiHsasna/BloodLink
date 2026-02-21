package tn.edu.esprit.services;

import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.Tools.DataSource;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceDonor implements IService<Donor> {

    private Connection cnx;

    public ServiceDonor() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Donor d) {
        try {

            String req = "INSERT INTO donors "
                    + "(donor_id, user_id, blood_type_id, last_donation_date, "
                    + "is_currently_eligible, latitude, longitude, total_donations, created_at) "
                    + "VALUES ('"
                    + d.getDonorId() + "', '"
                    + d.getUserId() + "', '"
                    + d.getBloodTypeId() + "', "
                    + (d.getLastDonationDate() != null
                    ? "'" + d.getLastDonationDate().toString() + "'"
                    : "NULL") + ", "
                    + (d.isCurrentlyEligible() ? "true" : "false") + ", "
                    + (d.getLatitude() != null ? d.getLatitude() : "NULL") + ", "
                    + (d.getLongitude() != null ? d.getLongitude() : "NULL") + ", "
                    + d.getTotalDonations() + ", "
                    + "NOW())";

            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);

            System.out.println("Donor ajouté avec succès !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout Donor : " + ex.getMessage());
        }
    }

    @Override
    public void modifier(Donor d) {
        try {
            Donor existing = getOne(d);
            if (existing == null) {
                System.out.println(" Aucun Donor trouvé avec cet ID !");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            String bloodTypeId = d.getBloodTypeId() != null
                    ? d.getBloodTypeId()
                    : existing.getBloodTypeId();

            String lastDonationDate = d.getLastDonationDate() != null
                    ? "'" + sdf.format(d.getLastDonationDate()) + "'"
                    : (existing.getLastDonationDate() != null
                    ? "'" + sdf.format(existing.getLastDonationDate()) + "'"
                    : "NULL");

            int totalDonations = d.getTotalDonations() != 0
                    ? d.getTotalDonations()
                    : existing.getTotalDonations();

            boolean isCurrentlyEligible = d.isCurrentlyEligible() != existing.isCurrentlyEligible()
                    ? d.isCurrentlyEligible()
                    : existing.isCurrentlyEligible();

            Double latitude = d.getLatitude() != null
                    ? d.getLatitude()
                    : existing.getLatitude();

            Double longitude = d.getLongitude() != null
                    ? d.getLongitude()
                    : existing.getLongitude();

            String req = "UPDATE donors SET "
                    + "blood_type_id = '" + bloodTypeId + "', "
                    + "last_donation_date = " + lastDonationDate + ", "
                    + "is_currently_eligible = " + isCurrentlyEligible + ", "
                    + "latitude = " + (latitude != null ? latitude : "NULL") + ", "
                    + "longitude = " + (longitude != null ? longitude : "NULL") + ","
                    + "total_donations = " + totalDonations + " "
                    + "WHERE user_id = '" + d.getUserId() + "'";

            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req);

            if (rows > 0) {
                System.out.println(" Donor modifié avec succès !");
            } else {
                System.out.println(" Aucun Donor trouvé avec cet ID !");
            }

        } catch (SQLException ex) {
            System.out.println("Erreur modification Donor : " + ex.getMessage());
        }
    }


    @Override
    public void supprimer(String donorId) {
        try {

            Statement stm = cnx.createStatement();

            // Supprimer Donor
            String reqDonor = "DELETE FROM donors WHERE donor_id = '" + donorId + "'";
            stm.executeUpdate(reqDonor);

            // Supprimer User parent (si donorId == userId dans ta logique)
            String reqUser = "DELETE FROM users WHERE user_id = '" + donorId + "'";
            stm.executeUpdate(reqUser);

            System.out.println("Donor supprimé !");

        } catch (SQLException ex) {
            System.out.println("Erreur suppression Donor : " + ex.getMessage());
        }
    }


    @Override
    public Donor getOne(Donor t) {
        Donor donor = null;
        try {
            String req = "SELECT * FROM donors WHERE user_id = '" + t.getUserId() + "'";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            if (rs.next()) {
                donor = new Donor();

                donor.setDonorId(rs.getString("donor_id"));
                donor.setUserId(rs.getString("user_id"));
                donor.setBloodTypeId(rs.getString("blood_type_id"));

                donor.setLastDonationDate(rs.getDate("last_donation_date").toLocalDate());

                donor.setCurrentlyEligible(rs.getBoolean("is_currently_eligible"));

                donor.setTotalDonations(rs.getInt("total_donations"));

                double lat = rs.getDouble("latitude");
                donor.setLatitude(!rs.wasNull() ? lat : null);

                double lon = rs.getDouble("longitude");
                donor.setLongitude(!rs.wasNull() ? lon : null);

                donor.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return donor;
    }

    @Override
    public List<Donor> getAll(Donor d) {
        List<Donor> donors = new ArrayList<>();
        try {
            String req = "SELECT * FROM donors ";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Donor donor = new Donor();

                donor.setDonorId(rs.getString("donor_id"));
                donor.setUserId(rs.getString("user_id"));
                donor.setBloodTypeId(rs.getString("blood_type_id"));

                donor.setLastDonationDate(rs.getDate("last_donation_date").toLocalDate());

                donor.setCurrentlyEligible(rs.getBoolean("is_currently_eligible"));

                donor.setTotalDonations(rs.getInt("total_donations"));

                double lat = rs.getDouble("latitude");
                donor.setLatitude(!rs.wasNull() ? lat : null);

                double lon = rs.getDouble("longitude");
                donor.setLongitude(!rs.wasNull() ? lon : null);

                donor.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                donors.add(donor);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return donors;
    }

    }