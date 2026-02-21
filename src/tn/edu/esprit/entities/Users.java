package tn.edu.esprit.entities;

import java.time.LocalDateTime;

public class Users {

    private String id;              // UUID string now
    private String email;
    private String passwordHash;
    private String first_name;
    private String last_name;
    private String phone;           // Added phone
    private UserType userType;
    private LocalDateTime createdAt;

    public Users() {}

    public Users(String id, String email, String passwordHash,
                 String first_name, String last_name, String phone,
                 UserType userType, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.first_name = first_name;
        this.last_name = last_name;
        this.phone = phone;
        this.userType = userType;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    // Getters & Setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirst_name() {
        return first_name;
    }
    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }
    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserType getUserType() {
        return userType;
    }
    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}