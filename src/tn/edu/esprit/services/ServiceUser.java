package tn.edu.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;
import tn.edu.esprit.Tools.DataSource;

public class ServiceUser implements IService<Users> {

    private Connection cnx;
    private final GeocodingService geocodingService;

    public ServiceUser() {
        this.cnx = DataSource.getInstance().getConnection();
        this.geocodingService = new GeocodingService();
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

    public Users authenticate(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        String trimmedEmail = email.trim();
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            return null;
        }

        String req = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) AND password_hash = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, trimmedEmail);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Users user = new Users();
                    user.setId(rs.getString("user_id"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setFirst_name(rs.getString("first_name"));
                    user.setLast_name(rs.getString("last_name"));
                    user.setPhone(rs.getString("phone"));
                    user.setUserType(UserType.valueOf(rs.getString("user_type")));
                    if (rs.getTimestamp("created_at") != null) {
                        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    return user;
                }
            }
        } catch (SQLException ex) {
            System.out.println("Erreur authentification User : " + ex.getMessage());
        }

        return null;
    }

    public boolean registerDonorAccount(String firstName, String lastName, String email, String phone, String city, String password, String bloodTypeId) {
        if (email == null || password == null) {
            return false;
        }

        String normalizedEmail = email.trim();
        if (normalizedEmail.isEmpty() || password.isEmpty()) {
            return false;
        }

        if (isEmailTaken(normalizedEmail)) {
            return false;
        }

        Users user = new Users();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(password);
        user.setFirst_name(firstName != null ? firstName.trim() : null);
        user.setLast_name(lastName != null ? lastName.trim() : null);
        user.setPhone(phone != null ? phone.trim() : null);
        user.setUserType(UserType.DONOR);
        user.setCreatedAt(LocalDateTime.now());

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            String insertUserSql = "INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone, user_type, created_at) VALUES (CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = cnx.prepareStatement(insertUserSql)) {
                ps.setString(1, user.getId());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPasswordHash());
                ps.setString(4, user.getFirst_name());
                ps.setString(5, user.getLast_name());
                ps.setString(6, user.getPhone());
                ps.setString(7, user.getUserType().name());
                ps.setObject(8, user.getCreatedAt());
                ps.executeUpdate();
            }

            syncDonorProfile(user, bloodTypeId, city);
            cnx.commit();
            return true;
        } catch (SQLException ex) {
            try {
                cnx.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("Erreur rollback register donor : " + rollbackEx.getMessage());
            }
            System.out.println("Erreur register donor : " + ex.getMessage());
            return false;
        } finally {
            try {
                cnx.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                System.out.println("Erreur restauration auto-commit : " + ex.getMessage());
            }
        }
    }

    public boolean isEmailTaken(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String req = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.out.println("Erreur verification email : " + ex.getMessage());
            return false;
        }
    }

    private void syncDonorProfile(Users u) throws SQLException {
        syncDonorProfile(u, null, null);
    }

    private void syncDonorProfile(Users u, String preferredBloodTypeId) throws SQLException {
        syncDonorProfile(u, preferredBloodTypeId, null);
    }

    private void syncDonorProfile(Users u, String preferredBloodTypeId, String preferredCity) throws SQLException {
        if (u == null || u.getId() == null || u.getUserType() != UserType.DONOR) {
            return;
        }

        if (donorExists(u.getId())) {
            return;
        }

        String bloodTypeId = preferredBloodTypeId;
        if (bloodTypeId == null || bloodTypeId.trim().isEmpty() || !bloodTypeExists(bloodTypeId.trim())) {
            bloodTypeId = getDefaultBloodTypeId();
        } else {
            bloodTypeId = bloodTypeId.trim();
        }

        String city = preferredCity;
        if (city != null) {
            city = city.trim();
            if (city.isEmpty()) {
                city = null;
            }
        }

        Double latitude = null;
        Double longitude = null;
        if (city != null) {
            Optional<GeocodingService.Coordinates> geocoded = geocodingService.geocodeCity(city);
            if (geocoded.isPresent()) {
                latitude = geocoded.get().getLatitude();
                longitude = geocoded.get().getLongitude();
            }
        }

        String req = "INSERT INTO donors (user_id, first_name, last_name, blood_type_id, city, latitude, longitude, is_currently_eligible, total_donations, created_at) VALUES (CAST(? AS uuid), ?, ?, ?, ?, ?, ?, true, 0, NOW())";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, u.getId());
            ps.setString(2, u.getFirst_name());
            ps.setString(3, u.getLast_name());
            if (bloodTypeId != null) {
                ps.setString(4, bloodTypeId);
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            if (city != null) {
                ps.setString(5, city);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            if (latitude != null) {
                ps.setDouble(6, latitude);
            } else {
                ps.setNull(6, Types.DOUBLE);
            }
            if (longitude != null) {
                ps.setDouble(7, longitude);
            } else {
                ps.setNull(7, Types.DOUBLE);
            }
            ps.executeUpdate();
        }
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
        String req = "SELECT 1 FROM donors WHERE user_id = CAST(? AS uuid)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
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

    private boolean bloodTypeExists(String bloodTypeId) throws SQLException {
        String req = "SELECT 1 FROM blood_type WHERE blood_type_id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, bloodTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
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