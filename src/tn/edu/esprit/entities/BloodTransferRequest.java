package tn.edu.esprit.entities;

import java.sql.Timestamp;
import java.util.UUID;

public class BloodTransferRequest {

    private Integer transferId;
    private UUID requestingHospitalId;
    private UUID approvingHospitalId;
    private String requestingStaffId;
    private String approvingStaffId;
    private String bloodTypeId;
    private Integer quantityUnitsRequested;
    private Integer quantityUnitsApproved;
    private TransfertStatus status;
    private String reason;
    private Timestamp requestedAt;
    private Timestamp approvedAt;
    private Timestamp deliveryExpectedAt;
    private Timestamp actualDeliveryAt;
    private String notes;

    public BloodTransferRequest() {
        this.quantityUnitsApproved = 0;
        this.status = TransfertStatus.valueOf(TransfertStatus.PENDING.name());
    }

    public BloodTransferRequest(
        UUID requestingHospitalId,
        String requestingStaffId,
        String bloodTypeId,
        Integer quantityUnitsRequested,
        String reason
    ) {
        this();
        this.requestingHospitalId = requestingHospitalId;
        this.requestingStaffId = requestingStaffId;
        this.bloodTypeId = bloodTypeId;
        this.quantityUnitsRequested = quantityUnitsRequested;
        this.reason = reason;
    }

    // Constructor for approval
    public BloodTransferRequest(
        UUID requestingHospitalId,
        UUID approvingHospitalId,
        String requestingStaffId,
        String approvingStaffId,
        String bloodTypeId,
        Integer quantityUnitsRequested,
        Integer quantityUnitsApproved,
        TransfertStatus status,
        String reason
    ) {
        this.requestingHospitalId = requestingHospitalId;
        this.approvingHospitalId = approvingHospitalId;
        this.requestingStaffId = requestingStaffId;
        this.approvingStaffId = approvingStaffId;
        this.bloodTypeId = bloodTypeId;
        this.quantityUnitsRequested = quantityUnitsRequested;
        this.quantityUnitsApproved = quantityUnitsApproved;
        this.status = status;
        this.reason = reason;
    }

    // Full constructor (including ID - for existing records)
    public BloodTransferRequest(
        Integer transferId,
        UUID requestingHospitalId,
        UUID approvingHospitalId,
        String requestingStaffId,
        String approvingStaffId,
        String bloodTypeId,
        Integer quantityUnitsRequested,
        Integer quantityUnitsApproved,
        TransfertStatus status,
        String reason,
        Timestamp requestedAt,
        Timestamp approvedAt,
        Timestamp deliveryExpectedAt,
        Timestamp actualDeliveryAt,
        String notes
    ) {
        this.transferId = transferId;
        this.requestingHospitalId = requestingHospitalId;
        this.approvingHospitalId = approvingHospitalId;
        this.requestingStaffId = requestingStaffId;
        this.approvingStaffId = approvingStaffId;
        this.bloodTypeId = bloodTypeId;
        this.quantityUnitsRequested = quantityUnitsRequested;
        this.quantityUnitsApproved = quantityUnitsApproved;
        this.status = status;
        this.reason = reason;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.deliveryExpectedAt = deliveryExpectedAt;
        this.actualDeliveryAt = actualDeliveryAt;
        this.notes = notes;
    }

    // Getters and Setters
    public Integer getTransferId() {
        return transferId;
    }

    public void setTransferId(Integer transferId) {
        this.transferId = transferId;
    }

    public UUID getRequestingHospitalId() {
        return requestingHospitalId;
    }

    public void setRequestingHospitalId(UUID requestingHospitalId) {
        this.requestingHospitalId = requestingHospitalId;
    }

    public UUID getApprovingHospitalId() {
        return approvingHospitalId;
    }

    public void setApprovingHospitalId(UUID approvingHospitalId) {
        this.approvingHospitalId = approvingHospitalId;
    }

    public String getRequestingStaffId() {
        return requestingStaffId;
    }

    public void setRequestingStaffId(String requestingStaffId) {
        this.requestingStaffId = requestingStaffId;
    }

    public String getApprovingStaffId() {
        return approvingStaffId;
    }

    public void setApprovingStaffId(String approvingStaffId) {
        this.approvingStaffId = approvingStaffId;
    }

    public String getBloodTypeId() {
        return bloodTypeId;
    }

    public void setBloodTypeId(String bloodTypeId) {
        this.bloodTypeId = bloodTypeId;
    }

    public Integer getQuantityUnitsRequested() {
        return quantityUnitsRequested;
    }

    public void setQuantityUnitsRequested(Integer quantityUnitsRequested) {
        this.quantityUnitsRequested = quantityUnitsRequested;
    }

    public Integer getQuantityUnitsApproved() {
        return quantityUnitsApproved;
    }

    public void setQuantityUnitsApproved(Integer quantityUnitsApproved) {
        this.quantityUnitsApproved = quantityUnitsApproved;
    }

    public TransfertStatus getStatus() {
        return status;
    }

    public void setStatus(TransfertStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Timestamp requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Timestamp getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Timestamp approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Timestamp getDeliveryExpectedAt() {
        return deliveryExpectedAt;
    }

    public void setDeliveryExpectedAt(Timestamp deliveryExpectedAt) {
        this.deliveryExpectedAt = deliveryExpectedAt;
    }

    public Timestamp getActualDeliveryAt() {
        return actualDeliveryAt;
    }

    public void setActualDeliveryAt(Timestamp actualDeliveryAt) {
        this.actualDeliveryAt = actualDeliveryAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
