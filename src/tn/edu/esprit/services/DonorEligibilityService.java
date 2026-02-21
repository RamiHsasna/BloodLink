package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import tn.edu.esprit.entities.DonorEligibility;
import tn.edu.esprit.Tools.DataSource;

public class DonorEligibilityService implements IService<DonorEligibility> {
    private Connection cnx;

    public DonorEligibilityService() {
        cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(DonorEligibility de) {
        try {
            String req = "INSERT INTO donor_eligibility "
                    + "(donor_id, is_currently_eligible, days_until_eligible, last_calculated_at, "
                    + "latitude_cache, longitude_cache, blood_type_cache) VALUES ('"
                    + de.getDonorId() + "', "
                    + (de.getIsCurrentlyEligible() != null ? de.getIsCurrentlyEligible() : true) + ", "
                    + (de.getDaysUntilEligible() != null ? de.getDaysUntilEligible() : 0) + ", "
                    + "NOW(), "
                    + (de.getLatitudeCache() != null ? de.getLatitudeCache() : "NULL") + ", "
                    + (de.getLongitudeCache() != null ? de.getLongitudeCache() : "NULL") + ", '"
                    + (de.getBloodTypeCache() != null ? de.getBloodTypeCache() : "") + "')";

            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);
            System.out.println("DonorEligibility ajouté !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout DonorEligibility : " + ex.getMessage());
        }
    }

    @Override
    public void modifier(DonorEligibility de) {
        try {
            DonorEligibility existing = getOne(de.getDonorId());
            if (existing == null) {
                System.out.println("Aucun DonorEligibility trouvé pour ce donor_id !");
                return;
            }

            StringBuilder req = new StringBuilder("UPDATE donor_eligibility SET ");
            boolean first = true;

            if (de.getIsCurrentlyEligible() != null) {
                req.append("is_currently_eligible = ").append(de.getIsCurrentlyEligible());
                first = false;
            }
            if (de.getDaysUntilEligible() != null) {
                if (!first) req.append(", ");
                req.append("days_until_eligible = ").append(de.getDaysUntilEligible());
                first = false;
            }
            if (de.getLatitudeCache() != null) {
                if (!first) req.append(", ");
                req.append("latitude_cache = ").append(de.getLatitudeCache());
                first = false;
            }
            if (de.getLongitudeCache() != null) {
                if (!first) req.append(", ");
                req.append("longitude_cache = ").append(de.getLongitudeCache());
                first = false;
            }
            if (de.getBloodTypeCache() != null) {
                if (!first) req.append(", ");
                req.append("blood_type_cache = '").append(de.getBloodTypeCache()).append("'");
                first = false;
            }

            if (!first) req.append(", ");
            req.append("last_calculated_at = NOW() ");

            req.append("WHERE donor_id = '").append(de.getDonorId()).append("'");

            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req.toString());
            System.out.println(rows > 0 ? "DonorEligibility modifié !" : "Aucun record modifié !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification DonorEligibility : " + ex.getMessage());
        }
    }

    public void supprimer(String donorId) {
        try {
            String req = "DELETE FROM donor_eligibility WHERE donor_id = '" + donorId + "'";
            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req);
            System.out.println(rows > 0 ? "DonorEligibility supprimé !" : "Aucun record trouvé !");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression DonorEligibility : " + ex.getMessage());
        }
    }

    @Override
    public DonorEligibility getOne(DonorEligibility de) {
        return getOne(de.getDonorId());
    }

    public DonorEligibility getOne(String donorId) {
        DonorEligibility de = null;
        try {
            String req = "SELECT * FROM donor_eligibility WHERE donor_id = '" + donorId + "'";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            if (rs.next()) {
                de = new DonorEligibility();
                de.setDonorEligibilityId(rs.getObject("donor_eligibility_id", UUID.class));
                de.setDonorId(rs.getString("donor_id"));
                de.setIsCurrentlyEligible(rs.getBoolean("is_currently_eligible"));
                de.setDaysUntilEligible(rs.getObject("days_until_eligible", Integer.class));
                if (rs.getDate("last_calculated_at") != null) {
                    de.setLastCalculatedAt(rs.getDate("last_calculated_at").toLocalDate());
                }
                de.setLatitudeCache(rs.getBigDecimal("latitude_cache"));
                de.setLongitudeCache(rs.getBigDecimal("longitude_cache"));
                de.setBloodTypeCache(rs.getString("blood_type_cache"));
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getOne DonorEligibility : " + ex.getMessage());
        }
        return de;
    }

    @Override
    public List<DonorEligibility> getAll(DonorEligibility t) {
        List<DonorEligibility> list = new ArrayList<>();
        try {
            String req = "SELECT * FROM donor_eligibility";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                DonorEligibility de = new DonorEligibility();
                de.setDonorEligibilityId(rs.getObject("donor_eligibility_id", UUID.class));
                de.setDonorId(rs.getString("donor_id"));
                de.setIsCurrentlyEligible(rs.getBoolean("is_currently_eligible"));
                de.setDaysUntilEligible(rs.getObject("days_until_eligible", Integer.class));
                if (rs.getDate("last_calculated_at") != null) {
                    de.setLastCalculatedAt(rs.getDate("last_calculated_at").toLocalDate());
                }
                de.setLatitudeCache(rs.getBigDecimal("latitude_cache"));
                de.setLongitudeCache(rs.getBigDecimal("longitude_cache"));
                de.setBloodTypeCache(rs.getString("blood_type_cache"));
                list.add(de);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getAll DonorEligibility : " + ex.getMessage());
        }
        return list;
    }
}