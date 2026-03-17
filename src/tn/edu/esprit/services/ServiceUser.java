package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.Tools.DataSource;

public class ServiceUser implements IService<Users> {

    private Connection cnx;

    public ServiceUser() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Users u) {
        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            String req = "INSERT INTO users "
                    + "(user_id, email, password_hash, first_name, last_name, phone, user_type, created_at) VALUES ('"
                    + u.getId() + "', '"
                    + u.getEmail() + "', '"
                    + u.getPasswordHash() + "', '"
                    + u.getFirst_name() + "', '"
                    + u.getLast_name() + "', '"
                    + (u.getPhone() != null ? u.getPhone() : "") + "', '"
                    + u.getUserType().name() + "', '"
                    + u.getCreatedAt() + "')";
            Statement stm = cnx.createStatement();
            stm.executeUpdate(req);

            syncDonorProfile(u);
            syncHospitalStaffProfile(u);

            cnx.commit();
            System.out.println("User ajouté avec succès !");
        } catch (SQLException ex) {
            try {
                cnx.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("Erreur rollback ajout User : " + rollbackEx.getMessage());
            }
            System.out.println("Erreur ajout User : " + ex.getMessage());
        } finally {
            try {
                cnx.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                System.out.println("Erreur restauration auto-commit : " + ex.getMessage());
            }
        }
    }

    @Override
    public void modifier(Users u) {
        try {
            Users existing = getOne(u);
            if (existing == null) {
                System.out.println("⚠️ Aucun utilisateur trouvé avec cet ID !");
                return;
            }

            String email = u.getEmail() != null ? u.getEmail() : existing.getEmail();
            String password = u.getPasswordHash() != null ? u.getPasswordHash() : existing.getPasswordHash();
            UserType userType = u.getUserType() != null ? u.getUserType() : existing.getUserType();
            String firstName = u.getFirst_name() != null ? u.getFirst_name() : existing.getFirst_name();
            String lastName = u.getLast_name() != null ? u.getLast_name() : existing.getLast_name();
            String phone = u.getPhone() != null ? u.getPhone() : existing.getPhone();

            String req = "UPDATE users SET "
                    + "email = '" + email + "', "
                    + "password_hash = '" + password + "', "
                    + "user_type = '" + userType.name() + "', "
                    + "first_name = '" + firstName + "', "
                    + "last_name = '" + lastName + "', "
                    + "phone = '" + phone + "' "
                    + "WHERE user_id = '" + u.getId() + "'";

            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req);

            if (rows > 0) {
                Users updated = new Users();
                updated.setId(u.getId());
                updated.setFirst_name(firstName);
                updated.setLast_name(lastName);
                updated.setUserType(userType);
                syncDonorProfile(updated);
                syncHospitalStaffProfile(updated);
            }

            System.out.println(rows > 0 ? "✅ Utilisateur modifié avec succès !" : "⚠️ Aucun utilisateur trouvé avec cet ID !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification User : " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(String id) {
        try {
            String req = "DELETE FROM users WHERE user_id = '" + id + "'";
            Statement stm = cnx.createStatement();
            int rows = stm.executeUpdate(req);
            System.out.println(rows > 0 ? "User supprimé !" : "Aucun user trouvé.");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression User : " + ex.getMessage());
        }
    }

    @Override
    public List<Users> getAll(Users t) {
        List<Users> users = new ArrayList<>();
        try {
            String req = "SELECT * FROM users";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Users u = new Users();
                u.setId(rs.getString("user_id"));
                u.setEmail(rs.getString("email"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setFirst_name(rs.getString("first_name"));
                u.setLast_name(rs.getString("last_name"));
                u.setPhone(rs.getString("phone"));
                u.setUserType(UserType.valueOf(rs.getString("user_type")));
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                users.add(u);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getAll Users : " + ex.getMessage());
        }
        return users;
    }

    @Override
    public Users getOne(Users u) {
        Users user = null;
        try {
            String req = "SELECT * FROM users WHERE user_id = '" + u.getId() + "'";
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            if (rs.next()) {
                user = new Users();
                user.setId(rs.getString("user_id"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setFirst_name(rs.getString("first_name"));
                user.setLast_name(rs.getString("last_name"));
                user.setPhone(rs.getString("phone"));
                user.setUserType(UserType.valueOf(rs.getString("user_type")));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
        } catch (SQLException ex) {
            System.out.println("Erreur getOne User : " + ex.getMessage());
        }
        return user;
    }

    private void syncDonorProfile(Users u) throws SQLException {
        if (u == null || u.getId() == null || u.getUserType() != UserType.DONOR) {
            return;
        }

        if (donorExists(u.getId())) {
            return;
        }

        String bloodTypeId = getDefaultBloodTypeId();
        String req = "INSERT INTO donors (user_id, first_name, last_name, blood_type_id, is_currently_eligible, total_donations, created_at) VALUES ('"
                + u.getId() + "', '"
                + u.getFirst_name() + "', '"
                + u.getLast_name() + "', "
                + (bloodTypeId != null ? "'" + bloodTypeId + "'" : "NULL") + ", "
                + "true, 0, NOW())";

        Statement stm = cnx.createStatement();
        stm.executeUpdate(req);
    }

    private void syncHospitalStaffProfile(Users u) throws SQLException {
        if (u == null || u.getId() == null || u.getUserType() != UserType.HOSPITAL_STAFF) {
            return;
        }

        if (hospitalStaffExists(u.getId())) {
            return;
        }

        String defaultHospitalId = getDefaultHospitalId();
        String defaultRole = "MANAGER";
        String req = "INSERT INTO hospital_staff (user_id, role, hospital_id, first_name, last_name, created_at) VALUES ('"
                + u.getId() + "', '"
                + defaultRole + "', "
            + (defaultHospitalId != null ? "'" + defaultHospitalId + "'" : "NULL") + ", "
            + (u.getFirst_name() != null ? "'" + u.getFirst_name() + "'" : "NULL") + ", "
            + (u.getLast_name() != null ? "'" + u.getLast_name() + "'" : "NULL") + ", NOW())";

        Statement stm = cnx.createStatement();
        stm.executeUpdate(req);
    }

    private boolean donorExists(String userId) throws SQLException {
        String req = "SELECT 1 FROM donors WHERE user_id = '" + userId + "'";
        Statement stm = cnx.createStatement();
        ResultSet rs = stm.executeQuery(req);
        return rs.next();
    }

    private boolean hospitalStaffExists(String userId) throws SQLException {
        String req = "SELECT 1 FROM hospital_staff WHERE user_id = '" + userId + "'";
        Statement stm = cnx.createStatement();
        ResultSet rs = stm.executeQuery(req);
        return rs.next();
    }

    private String getDefaultBloodTypeId() throws SQLException {
        String req = "SELECT blood_type_id FROM blood_type ORDER BY blood_type_id LIMIT 1";
        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(req)) {
            if (rs.next()) {
                return rs.getString("blood_type_id");
            }
        }
        return null;
    }

    private String getDefaultHospitalId() {
        String hospitalId = querySingleId("SELECT hospital_id::text FROM hospital ORDER BY hospital_id LIMIT 1");
        if (hospitalId != null) {
            return hospitalId;
        }
        return querySingleId("SELECT hospital_id::text FROM hospitals ORDER BY hospital_id LIMIT 1");
    }

    private String querySingleId(String query) {
        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(query)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException ignored) {
            // Ignore and try fallback query when available.
        }
        return null;
    }
}