package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
        ensureDetailsColumn();
    }

    private void ensureDetailsColumn() {
        try (Statement stm = cnx.createStatement()) {
            stm.execute("ALTER TABLE donor_eligibility ADD COLUMN IF NOT EXISTS eligibility_details TEXT");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout colonne eligibility_details : " + ex.getMessage());
        }
    }

    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    @Override
    public void ajouter(DonorEligibility de) {
        try {
            String req = "INSERT INTO donor_eligibility "
                    + "(user_id, is_currently_eligible, days_until_eligible, last_calculated_at, "
                    + "latitude_cache, longitude_cache, blood_type_cache, eligibility_details) VALUES ('"
                    + de.getId() + "', "
                    + (de.getIsCurrentlyEligible() != null ? de.getIsCurrentlyEligible() : true) + ", "
                    + (de.getDaysUntilEligible() != null ? de.getDaysUntilEligible() : 0) + ", "
                    + "NOW(), "
                    + (de.getLatitudeCache() != null ? de.getLatitudeCache() : "NULL") + ", "
                    + (de.getLongitudeCache() != null ? de.getLongitudeCache() : "NULL") + ", '"
                    + escapeSql(de.getBloodTypeCache() != null ? de.getBloodTypeCache() : "") + "', '"
                    + escapeSql(de.getEligibilityDetails() != null ? de.getEligibilityDetails() : "") + "')";

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
            DonorEligibility existing = getOne(de.getId());
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
            if (de.getEligibilityDetails() != null) {
                if (!first) req.append(", ");
                req.append("eligibility_details = '").append(escapeSql(de.getEligibilityDetails())).append("'");
                first = false;
            }

            if (!first) req.append(", ");
            req.append("last_calculated_at = NOW() ");

            req.append("WHERE user_id = '").append(de.getId()).append("'");

            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req.toString());
            System.out.println(rows > 0 ? "DonorEligibility modifié !" : "Aucun record modifié !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification DonorEligibility : " + ex.getMessage());
        }
    }

    public void supprimer(String id) {
        try {
            String req = "DELETE FROM donor_eligibility WHERE user_id = '" + id + "'";
            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req);
            System.out.println(rows > 0 ? "DonorEligibility supprimé !" : "Aucun record trouvé !");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression DonorEligibility : " + ex.getMessage());
        }
    }

    @Override
    public DonorEligibility getOne(DonorEligibility de) {
        return getOne(de.getId());
    }

    public DonorEligibility getOne(String id) {
        DonorEligibility de = null;
        try {
            String req = "SELECT * FROM donor_eligibility WHERE user_id = '" + id + "'";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            if (rs.next()) {
                de = new DonorEligibility();
                de.setDonorEligibilityId(rs.getObject("donor_eligibility_id", UUID.class));
                de.setId(rs.getString("user_id"));
                de.setIsCurrentlyEligible(rs.getBoolean("is_currently_eligible"));
                de.setDaysUntilEligible(rs.getObject("days_until_eligible", Integer.class));
                if (rs.getDate("last_calculated_at") != null) {
                    de.setLastCalculatedAt(rs.getDate("last_calculated_at").toLocalDate());
                }
                de.setLatitudeCache(rs.getBigDecimal("latitude_cache"));
                de.setLongitudeCache(rs.getBigDecimal("longitude_cache"));
                de.setBloodTypeCache(rs.getString("blood_type_cache"));
                de.setEligibilityDetails(rs.getString("eligibility_details"));
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getOne DonorEligibility : " + ex.getMessage());
        }
        return de;
    }

    public void upsertEligibility(DonorEligibility de) {
        if (de == null || de.getId() == null || de.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Donor eligibility payload and donor id are required.");
        }

        DonorEligibility existing = getOne(de.getId());
        if (existing == null) {
            ajouter(de);
            return;
        }

        try {
            String req = "UPDATE donor_eligibility SET "
                    + "is_currently_eligible = ?, "
                    + "days_until_eligible = ?, "
                    + "last_calculated_at = NOW(), "
                    + "latitude_cache = ?, "
                    + "longitude_cache = ?, "
                        + "blood_type_cache = ?, "
                        + "eligibility_details = ? "
                    + "WHERE user_id = ?";

            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setBoolean(1, de.getIsCurrentlyEligible() != null ? de.getIsCurrentlyEligible() : false);
            pst.setInt(2, de.getDaysUntilEligible() != null ? de.getDaysUntilEligible() : 0);

            if (de.getLatitudeCache() != null) {
                pst.setBigDecimal(3, de.getLatitudeCache());
            } else {
                pst.setNull(3, java.sql.Types.DECIMAL);
            }

            if (de.getLongitudeCache() != null) {
                pst.setBigDecimal(4, de.getLongitudeCache());
            } else {
                pst.setNull(4, java.sql.Types.DECIMAL);
            }

            pst.setString(5, de.getBloodTypeCache() != null ? de.getBloodTypeCache() : "");
            pst.setString(6, de.getEligibilityDetails() != null ? de.getEligibilityDetails() : "");
            pst.setString(7, de.getId());

            pst.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Erreur upsert DonorEligibility : " + ex.getMessage());
        }
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
                de.setId(rs.getString("user_id"));
                de.setIsCurrentlyEligible(rs.getBoolean("is_currently_eligible"));
                de.setDaysUntilEligible(rs.getObject("days_until_eligible", Integer.class));
                if (rs.getDate("last_calculated_at") != null) {
                    de.setLastCalculatedAt(rs.getDate("last_calculated_at").toLocalDate());
                }
                de.setLatitudeCache(rs.getBigDecimal("latitude_cache"));
                de.setLongitudeCache(rs.getBigDecimal("longitude_cache"));
                de.setBloodTypeCache(rs.getString("blood_type_cache"));
                de.setEligibilityDetails(rs.getString("eligibility_details"));
                list.add(de);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getAll DonorEligibility : " + ex.getMessage());
        }
        return list;
    }
}