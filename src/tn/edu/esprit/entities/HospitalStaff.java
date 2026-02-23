package tn.edu.esprit.entities;

import java.time.LocalDateTime;

public class HospitalStaff {

    private String id;          // corresponds to user_id
    private String hospitalId;  // corresponds to hospital_id
    private String role;
    private String department;  // nullable
    private LocalDateTime createdAt;

    public HospitalStaff() {
    }

    public HospitalStaff(String id, String hospitalId, String role, String department, LocalDateTime createdAt) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.role = role;
        this.department = department;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}