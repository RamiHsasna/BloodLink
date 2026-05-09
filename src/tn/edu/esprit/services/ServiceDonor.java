package tn.edu.esprit.services;

import tn.edu.esprit.entities.Donor;
import tn.edu.esprit.Tools.DataSource;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceDonor implements IService<Donor> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private Connection cnx;
    private final GeocodingService geocodingService;

    public ServiceDonor() {
        this.cnx = DataSource.getInstance().getConnection();
        this.geocodingService = new GeocodingService();
    }

    @Override
    public void ajouter(Donor d) {
        try {

                String city = normalizeText(d.getCity());
                Double latitude = d.getLatitude();
                Double longitude = d.getLongitude();

                if (city != null && (latitude == null || longitude == null)) {
                    Optional<GeocodingService.Coordinates> geocoded = geocodingService.geocodeCity(city);
                    if (geocoded.isPresent()) {
                        latitude = geocoded.get().getLatitude();
                        longitude = geocoded.get().getLongitude();
                    }
                }

                String req = "INSERT INTO donors "
                    + "(user_id, first_name, last_name, blood_type_id, city, last_donation_date, "
                    + "is_currently_eligible, latitude, longitude, total_donations, created_at) "
                    + "VALUES ("
                    + "'" + d.getUserId() + "', "
                    + toSqlString(d.getFirstName()) + ", "
                    + toSqlString(d.getLastName()) + ", "
                    + toSqlString(d.getBloodTypeId()) + ", "
                    + toSqlString(city) + ", "
                    + (d.getLastDonationDate() != null
                    ? "'" + d.getLastDonationDate().format(DATE_FORMATTER) + "'"
                    : "NULL") + ", "
                    + (d.isCurrentlyEligible() ? "true" : "false") + ", "
                    + (latitude != null ? latitude : "NULL") + ", "
                    + (longitude != null ? longitude : "NULL") + ", "
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
            Donor existing;
            existing = getOne(d);
            if (existing == null) {
                System.out.println(" Aucun Donor trouvé avec cet ID !");
                return;
            }

                String firstName = d.getFirstName() != null
                    ? d.getFirstName()
                    : existing.getFirstName();

                String lastName = d.getLastName() != null
                    ? d.getLastName()
                    : existing.getLastName();

                String bloodTypeId = d.getBloodTypeId() != null
                    ? d.getBloodTypeId()
                    : existing.getBloodTypeId();

                String incomingCity = normalizeText(d.getCity());
                String existingCity = normalizeText(existing.getCity());
                String city = incomingCity != null ? incomingCity : existingCity;


            String lastDonationDate = d.getLastDonationDate() != null
            ? "'" + d.getLastDonationDate().format(DATE_FORMATTER) + "'"
                    : (existing.getLastDonationDate() != null
            ? "'" + existing.getLastDonationDate().format(DATE_FORMATTER) + "'"
                    : "NULL");

                int totalDonations = d.getTotalDonations();

                boolean isCurrentlyEligible = d.isCurrentlyEligible();

            Double latitude = d.getLatitude() != null
                    ? d.getLatitude()
                    : existing.getLatitude();

            Double longitude = d.getLongitude() != null
                    ? d.getLongitude()
                    : existing.getLongitude();

            boolean cityChanged = incomingCity != null && !incomingCity.equalsIgnoreCase(existingCity);
            boolean missingCoords = latitude == null || longitude == null;
            if (city != null && (cityChanged || missingCoords)) {
                Optional<GeocodingService.Coordinates> geocoded = geocodingService.geocodeCity(city);
                if (geocoded.isPresent()) {
                    latitude = geocoded.get().getLatitude();
                    longitude = geocoded.get().getLongitude();
                }
            }

                String req = "UPDATE donors SET "
                    + "first_name = " + toSqlString(firstName) + ", "
                    + "last_name = " + toSqlString(lastName) + ", "
                    + "blood_type_id = " + toSqlString(bloodTypeId) + ", "
                    + "city = " + toSqlString(city) + ", "

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
    public void supprimer(String userId) {
        try {

            Statement stm = cnx.createStatement();

            // Supprimer Donor
            String reqDonor = "DELETE FROM donors WHERE user_id = '" + userId + "'";
            stm.executeUpdate(reqDonor);

            // Supprimer User parent (si donorId == userId dans ta logique)
            String reqUser = "DELETE FROM users WHERE user_id = '" + userId + "'";
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

                donor.setUserId(rs.getString("user_id"));
                donor.setFirstName(rs.getString("first_name"));
                donor.setLastName(rs.getString("last_name"));
                donor.setBloodTypeId(rs.getString("blood_type_id"));
                donor.setCity(rs.getString("city"));

                Date sqlDate = rs.getDate("last_donation_date");
                donor.setLastDonationDate(sqlDate != null ? sqlDate.toLocalDate() : null);

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
            String req = "SELECT * FROM donors";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Donor donor = new Donor();

                donor.setUserId(rs.getString("user_id"));
                donor.setFirstName(rs.getString("first_name"));
                donor.setLastName(rs.getString("last_name"));
                donor.setBloodTypeId(rs.getString("blood_type_id"));
                donor.setCity(rs.getString("city"));

                Date sqlDate = rs.getDate("last_donation_date");
                donor.setLastDonationDate(sqlDate != null ? sqlDate.toLocalDate() : null);

                donor.setCurrentlyEligible(rs.getBoolean("is_currently_eligible"));
                donor.setTotalDonations(rs.getInt("total_donations"));

                double lat = rs.getDouble("latitude");
                donor.setLatitude(!rs.wasNull() ? lat : null);

                double lon = rs.getDouble("longitude");
                donor.setLongitude(!rs.wasNull() ? lon : null);

                Timestamp ts = rs.getTimestamp("created_at");
                donor.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);

                donors.add(donor);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getAll Donors : " + ex.getMessage());
        }
        return donors;
    }

    private String toSqlString(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}

