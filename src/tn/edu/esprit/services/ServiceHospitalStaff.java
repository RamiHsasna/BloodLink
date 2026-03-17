package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
        String req = "INSERT INTO hospital_staff (user_id, role, hospital_id, department, first_name, last_name, created_at) VALUES (CAST(? AS uuid), ?, CAST(? AS uuid), ?, ?, ?, NOW())";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, hs.getId());
            ps.setString(2, hs.getRole());
            ps.setString(3, hs.getHospitalId());
            ps.setString(4, hs.getDepartment());
            ps.setString(5, hs.getFirstName());
            ps.setString(6, hs.getLastName());
            ps.executeUpdate();

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
                ajouter(hs);
                return;
            }

            String role = hs.getRole() != null ? hs.getRole() : existing.getRole();
            String hospitalId = hs.getHospitalId() != null ? hs.getHospitalId() : existing.getHospitalId();
            String department = hs.getDepartment();
            String firstName = hs.getFirstName() != null ? hs.getFirstName() : existing.getFirstName();
            String lastName = hs.getLastName() != null ? hs.getLastName() : existing.getLastName();

            String req = "UPDATE hospital_staff SET role = ?, hospital_id = CAST(? AS uuid), department = ?, first_name = ?, last_name = ? WHERE user_id = CAST(? AS uuid)";

            try (PreparedStatement ps = cnx.prepareStatement(req)) {
                ps.setString(1, role);
                ps.setString(2, hospitalId);
                ps.setString(3, department);
                ps.setString(4, firstName);
                ps.setString(5, lastName);
                ps.setString(6, hs.getId());

                int rows = ps.executeUpdate();
                System.out.println(rows > 0 ? "HospitalStaff modifié avec succès !" : "Aucun HospitalStaff trouvé avec cet ID !");
            }
        } catch (SQLException ex) {
            System.out.println("Erreur modification HospitalStaff : " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(String userId) {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM hospital_staff WHERE user_id = CAST(? AS uuid)")) {
            ps.setString(1, userId);
            int rows = ps.executeUpdate();

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
                h.setDepartment(rs.getString("department"));
                h.setFirstName(rs.getString("first_name"));
                h.setLastName(rs.getString("last_name"));
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
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM hospital_staff WHERE user_id = CAST(? AS uuid)")) {
            ps.setString(1, t.getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                staff = new HospitalStaff();
                staff.setId(rs.getString("user_id"));
                staff.setRole(rs.getString("role"));
                staff.setHospitalId(rs.getString("hospital_id"));
                staff.setDepartment(rs.getString("department"));
                staff.setFirstName(rs.getString("first_name"));
                staff.setLastName(rs.getString("last_name"));
                staff.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return staff;
    }
}