package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import tn.edu.esprit.entities.HospitalStaff;
import tn.edu.esprit.Tools.DataSource;

public class ServiceHospitalStaff implements IService<HospitalStaff> {

    private Connection cnx;

    public ServiceHospitalStaff() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(HospitalStaff hs) {
        try {
            String req = "INSERT INTO hospital_staff "
                    + "(user_id, role, hospital_id, created_at) VALUES ('"
                    + hs.getId() + "', '"
                    + hs.getRole() + "', '"
                    + hs.getHospitalId() + "', NOW())";

            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);

            System.out.println("HospitalStaff ajouté avec succès !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout HospitalStaff : " + ex.getMessage());
        }
    }

    @Override
    public void modifier(HospitalStaff hs) {
        try {
            HospitalStaff existing = getOne(hs);
            if (existing == null) {
                System.out.println("⚠️ Aucun HospitalStaff trouvé avec cet ID !");
                return;
            }

            // keep existing if new value is null
            String role = hs.getRole() != null ? hs.getRole() : existing.getRole();
            String hospitalId = hs.getHospitalId() != null ? hs.getHospitalId() : existing.getHospitalId();

            String req = "UPDATE hospital_staff SET "
                    + "role = '" + role + "', "
                    + "hospital_id = '" + hospitalId + "' "
                    + "WHERE user_id = '" + hs.getId() + "'";

            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req);

            System.out.println(rows > 0 ? "HospitalStaff modifié avec succès !" : "Aucun HospitalStaff trouvé avec cet ID !");

        } catch (SQLException ex) {
            System.out.println("Erreur modification HospitalStaff : " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(String userId) {
        try {
            String req = "DELETE FROM hospital_staff WHERE user_id = '" + userId + "'";
            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req);

            System.out.println(rows > 0 ? "HospitalStaff supprimé !" : "Aucun trouvé.");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public List<HospitalStaff> getAll(HospitalStaff hs) {
        List<HospitalStaff> staffs = new ArrayList<>();
        try {
            String req = "SELECT * FROM hospital_staff";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                HospitalStaff h = new HospitalStaff();
                h.setId(rs.getString("user_id"));
                h.setRole(rs.getString("role"));
                h.setHospitalId(rs.getString("hospital_id"));
                h.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                staffs.add(h);
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return staffs;
    }

    @Override
    public HospitalStaff getOne(HospitalStaff t) {
        HospitalStaff staff = null;
        try {
            String req = "SELECT * FROM hospital_staff WHERE user_id = '" + t.getId() + "'";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            if (rs.next()) {
                staff = new HospitalStaff();
                staff.setId(rs.getString("user_id"));
                staff.setRole(rs.getString("role"));
                staff.setHospitalId(rs.getString("hospital_id"));
                staff.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return staff;
    }
}