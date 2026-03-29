package tn.edu.esprit.services;

import tn.edu.esprit.Tools.DataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonorHistoryService {

    private static final String SELECT_DONATION_HISTORY =
            "SELECT d.donation_id, d.hospital_id, d.blood_type_id, d.donation_date, d.units_collected, " +
                    "d.volume_collected, d.status, d.screening_passed, d.medical_notes, d.created_at, " +
                    "h.name AS hospital_name, bt.abo_type, bt.rh_factor " +
                    "FROM donations d " +
                    "LEFT JOIN hospital h ON h.hospital_id = d.hospital_id " +
                    "LEFT JOIN blood_type bt ON bt.blood_type_id = d.blood_type_id " +
                    "WHERE d.user_id = CAST(? AS uuid) " +
                    "ORDER BY COALESCE(d.donation_date, d.created_at) DESC";

    private static final String SELECT_TRANSFER_HISTORY =
            "WITH donor_donations AS ( " +
                    "    SELECT d.donation_id, d.hospital_id, d.blood_type_id, " +
                    "           COALESCE(d.donation_date, d.created_at) AS donation_at, " +
                    "           h.name AS donation_hospital_name, " +
                    "           CONCAT(COALESCE(bt.abo_type, ''), COALESCE(bt.rh_factor, '')) AS donation_blood_type_label " +
                    "    FROM donations d " +
                    "    LEFT JOIN hospital h ON h.hospital_id = d.hospital_id " +
                    "    LEFT JOIN blood_type bt ON bt.blood_type_id = d.blood_type_id " +
                    "    WHERE d.user_id = CAST(? AS uuid) " +
                    "), ranked_transfers AS ( " +
                    "    SELECT tr.transfer_id, tr.requesting_hospital_id, tr.approving_hospital_id, tr.blood_type_id, " +
                    "           tr.quantity_units_requested, tr.quantity_units_approved, tr.status, tr.reason, tr.notes, " +
                    "           tr.requested_at, tr.approved_at, tr.actual_delivery_at, dd.donation_id, dd.donation_at, " +
                    "           dd.donation_hospital_name, dd.donation_blood_type_label, " +
                    "           ROW_NUMBER() OVER (PARTITION BY tr.transfer_id ORDER BY dd.donation_at DESC NULLS LAST) AS donor_rank " +
                    "    FROM donor_donations dd " +
                    "    JOIN blood_transfer_request tr " +
                    "      ON tr.approving_hospital_id = dd.hospital_id " +
                    "     AND tr.blood_type_id = dd.blood_type_id " +
                    "     AND COALESCE(tr.actual_delivery_at, tr.approved_at, tr.requested_at) >= dd.donation_at " +
                    ") " +
                    "SELECT rt.transfer_id, rt.donation_id, rt.donation_hospital_name, rt.donation_blood_type_label, " +
                    "       req.name AS destination_hospital_name, app.name AS source_hospital_name, " +
                    "       CONCAT(COALESCE(bt.abo_type, ''), COALESCE(bt.rh_factor, '')) AS transfer_blood_type_label, " +
                    "       rt.status, rt.quantity_units_requested, rt.quantity_units_approved, rt.requested_at, " +
                    "       rt.approved_at, rt.actual_delivery_at, rt.reason, rt.notes " +
                    "FROM ranked_transfers rt " +
                    "LEFT JOIN hospital req ON req.hospital_id = rt.requesting_hospital_id " +
                    "LEFT JOIN hospital app ON app.hospital_id = rt.approving_hospital_id " +
                    "LEFT JOIN blood_type bt ON bt.blood_type_id = rt.blood_type_id " +
                    "WHERE rt.donor_rank = 1 " +
                    "ORDER BY COALESCE(rt.actual_delivery_at, rt.approved_at, rt.requested_at) DESC";

    private final Connection connection;

    public DonorHistoryService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<DonationHistoryItem> getDonationHistory(String donorId) {
        List<DonationHistoryItem> results = new ArrayList<>();
        if (donorId == null || donorId.isBlank()) {
            return results;
        }

        try (PreparedStatement statement = connection.prepareStatement(SELECT_DONATION_HISTORY)) {
            statement.setString(1, donorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new DonationHistoryItem(
                            resultSet.getString("donation_id"),
                            resultSet.getString("hospital_id"),
                            resultSet.getString("hospital_name"),
                            resultSet.getString("blood_type_id"),
                            buildBloodTypeLabel(resultSet.getString("abo_type"), resultSet.getString("rh_factor")),
                            toLocalDateTime(resultSet.getTimestamp("donation_date")),
                            resultSet.getInt("units_collected"),
                            resultSet.getBigDecimal("volume_collected"),
                            resultSet.getString("status"),
                            (Boolean) resultSet.getObject("screening_passed"),
                            resultSet.getString("medical_notes"),
                            toLocalDateTime(resultSet.getTimestamp("created_at"))));
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Impossible de charger l'historique des dons du donneur.", exception);
        }

        return results;
    }

    public List<TransferHistoryItem> getTransferHistory(String donorId) {
        List<TransferHistoryItem> results = new ArrayList<>();
        if (donorId == null || donorId.isBlank()) {
            return results;
        }

        try (PreparedStatement statement = connection.prepareStatement(SELECT_TRANSFER_HISTORY)) {
            statement.setString(1, donorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new TransferHistoryItem(
                            resultSet.getInt("transfer_id"),
                            resultSet.getString("donation_id"),
                            resultSet.getString("source_hospital_name"),
                            resultSet.getString("destination_hospital_name"),
                            resultSet.getString("transfer_blood_type_label"),
                            resultSet.getString("donation_blood_type_label"),
                            resultSet.getString("status"),
                            (Integer) resultSet.getObject("quantity_units_requested"),
                            (Integer) resultSet.getObject("quantity_units_approved"),
                            toLocalDateTime(resultSet.getTimestamp("requested_at")),
                            toLocalDateTime(resultSet.getTimestamp("approved_at")),
                            toLocalDateTime(resultSet.getTimestamp("actual_delivery_at")),
                            resultSet.getString("reason"),
                            resultSet.getString("notes"),
                            resultSet.getString("donation_hospital_name")));
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Impossible de charger l'historique estimé des transferts du donneur.", exception);
        }

        return results;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String buildBloodTypeLabel(String aboType, String rhFactor) {
        String abo = aboType != null ? aboType.trim() : "";
        String rh = rhFactor != null ? rhFactor.trim() : "";
        String label = (abo + rh).trim();
        return label.isEmpty() ? "N/A" : label;
    }

    public static class DonationHistoryItem {
        private final String donationId;
        private final String hospitalId;
        private final String hospitalName;
        private final String bloodTypeId;
        private final String bloodTypeLabel;
        private final LocalDateTime donationDate;
        private final int unitsCollected;
        private final BigDecimal volumeCollected;
        private final String status;
        private final Boolean screeningPassed;
        private final String medicalNotes;
        private final LocalDateTime createdAt;

        public DonationHistoryItem(
                String donationId,
                String hospitalId,
                String hospitalName,
                String bloodTypeId,
                String bloodTypeLabel,
                LocalDateTime donationDate,
                int unitsCollected,
                BigDecimal volumeCollected,
                String status,
                Boolean screeningPassed,
                String medicalNotes,
                LocalDateTime createdAt) {
            this.donationId = donationId;
            this.hospitalId = hospitalId;
            this.hospitalName = hospitalName;
            this.bloodTypeId = bloodTypeId;
            this.bloodTypeLabel = bloodTypeLabel;
            this.donationDate = donationDate;
            this.unitsCollected = unitsCollected;
            this.volumeCollected = volumeCollected;
            this.status = status;
            this.screeningPassed = screeningPassed;
            this.medicalNotes = medicalNotes;
            this.createdAt = createdAt;
        }

        public String getDonationId() {
            return donationId;
        }

        public String getHospitalId() {
            return hospitalId;
        }

        public String getHospitalName() {
            return hospitalName;
        }

        public String getBloodTypeId() {
            return bloodTypeId;
        }

        public String getBloodTypeLabel() {
            return bloodTypeLabel;
        }

        public LocalDateTime getDonationDate() {
            return donationDate;
        }

        public int getUnitsCollected() {
            return unitsCollected;
        }

        public BigDecimal getVolumeCollected() {
            return volumeCollected;
        }

        public String getStatus() {
            return status;
        }

        public Boolean getScreeningPassed() {
            return screeningPassed;
        }

        public String getMedicalNotes() {
            return medicalNotes;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    public static class TransferHistoryItem {
        private final int transferId;
        private final String donationId;
        private final String sourceHospitalName;
        private final String destinationHospitalName;
        private final String transferBloodTypeLabel;
        private final String donationBloodTypeLabel;
        private final String status;
        private final Integer quantityUnitsRequested;
        private final Integer quantityUnitsApproved;
        private final LocalDateTime requestedAt;
        private final LocalDateTime approvedAt;
        private final LocalDateTime actualDeliveryAt;
        private final String reason;
        private final String notes;
        private final String donationHospitalName;

        public TransferHistoryItem(
                int transferId,
                String donationId,
                String sourceHospitalName,
                String destinationHospitalName,
                String transferBloodTypeLabel,
                String donationBloodTypeLabel,
                String status,
                Integer quantityUnitsRequested,
                Integer quantityUnitsApproved,
                LocalDateTime requestedAt,
                LocalDateTime approvedAt,
                LocalDateTime actualDeliveryAt,
                String reason,
                String notes,
                String donationHospitalName) {
            this.transferId = transferId;
            this.donationId = donationId;
            this.sourceHospitalName = sourceHospitalName;
            this.destinationHospitalName = destinationHospitalName;
            this.transferBloodTypeLabel = transferBloodTypeLabel;
            this.donationBloodTypeLabel = donationBloodTypeLabel;
            this.status = status;
            this.quantityUnitsRequested = quantityUnitsRequested;
            this.quantityUnitsApproved = quantityUnitsApproved;
            this.requestedAt = requestedAt;
            this.approvedAt = approvedAt;
            this.actualDeliveryAt = actualDeliveryAt;
            this.reason = reason;
            this.notes = notes;
            this.donationHospitalName = donationHospitalName;
        }

        public int getTransferId() {
            return transferId;
        }

        public String getDonationId() {
            return donationId;
        }

        public String getSourceHospitalName() {
            return sourceHospitalName;
        }

        public String getDestinationHospitalName() {
            return destinationHospitalName;
        }

        public String getTransferBloodTypeLabel() {
            return transferBloodTypeLabel;
        }

        public String getDonationBloodTypeLabel() {
            return donationBloodTypeLabel;
        }

        public String getStatus() {
            return status;
        }

        public Integer getQuantityUnitsRequested() {
            return quantityUnitsRequested;
        }

        public Integer getQuantityUnitsApproved() {
            return quantityUnitsApproved;
        }

        public LocalDateTime getRequestedAt() {
            return requestedAt;
        }

        public LocalDateTime getApprovedAt() {
            return approvedAt;
        }

        public LocalDateTime getActualDeliveryAt() {
            return actualDeliveryAt;
        }

        public String getReason() {
            return reason;
        }

        public String getNotes() {
            return notes;
        }

        public String getDonationHospitalName() {
            return donationHospitalName;
        }
    }
}
