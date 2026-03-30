package tn.edu.esprit.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import tn.edu.esprit.entities.Alert;
import tn.edu.esprit.entities.BloodInventory;
import tn.edu.esprit.entities.BloodTransferRequest;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.entities.DonationsEvent;
import tn.edu.esprit.entities.HospitalStaff;
import tn.edu.esprit.entities.UserType;
import tn.edu.esprit.entities.Users;

public class SessionScopeService {

    private final Users currentUser;
    private final HospitalStaff currentHospitalStaff;

    public SessionScopeService() {
        this.currentUser = AppSession.getCurrentUser();
        this.currentHospitalStaff = loadHospitalStaff(currentUser);
    }

    public Users getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getUserType() == UserType.ADMIN;
    }

    public boolean isDonor() {
        return currentUser != null && currentUser.getUserType() == UserType.DONOR;
    }

    public boolean isHospitalStaff() {
        return currentUser != null && currentUser.getUserType() == UserType.HOSPITAL_STAFF;
    }

    public HospitalStaff getCurrentHospitalStaff() {
        return currentHospitalStaff;
    }

    public String getCurrentHospitalId() {
        return currentHospitalStaff != null ? currentHospitalStaff.getHospitalId() : null;
    }

    public UUID getCurrentHospitalUuid() {
        String hospitalId = getCurrentHospitalId();
        if (hospitalId == null || hospitalId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(hospitalId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public boolean hasHospitalScope() {
        return getCurrentHospitalId() != null && !getCurrentHospitalId().isBlank();
    }

    public boolean canAccessAdminModules() {
        return isAdmin();
    }

    public boolean canAccessAuditLogs() {
        return isAdmin();
    }

    public boolean belongsToCurrentHospital(String hospitalId) {
        return hospitalId != null && hasHospitalScope() && hospitalId.equalsIgnoreCase(getCurrentHospitalId());
    }

    public boolean belongsToCurrentHospital(UUID hospitalId) {
        UUID currentHospitalUuid = getCurrentHospitalUuid();
        return hospitalId != null && currentHospitalUuid != null && hospitalId.equals(currentHospitalUuid);
    }

    public List<Donations> filterVisibleDonations(List<Donations> donations) {
        if (donations == null) {
            return List.of();
        }
        return donations.stream().filter(this::canViewDonation).collect(Collectors.toList());
    }

    public boolean canViewDonation(Donations donation) {
        if (donation == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        if (isDonor()) {
            return getCurrentUserId() != null && getCurrentUserId().equalsIgnoreCase(donation.getDonorId());
        }
        return isHospitalStaff() && belongsToCurrentHospital(donation.getHospitalId());
    }

    public boolean canManageDonation(Donations donation) {
        return isAdmin() || (isHospitalStaff() && donation != null && belongsToCurrentHospital(donation.getHospitalId()));
    }

    public List<DonationsEvent> filterVisibleDonationEvents(List<DonationsEvent> events) {
        if (events == null) {
            return List.of();
        }
        return events.stream().filter(this::canViewDonationEvent).collect(Collectors.toList());
    }

    public boolean canViewDonationEvent(DonationsEvent event) {
        if (event == null) {
            return false;
        }
        if (isAdmin() || isDonor()) {
            return true;
        }
        return isHospitalStaff() && belongsToCurrentHospital(event.getHospitalId());
    }

    public boolean canManageDonationEvent(DonationsEvent event) {
        return isAdmin() || (isHospitalStaff() && event != null && belongsToCurrentHospital(event.getHospitalId()));
    }

    public List<Alert> filterVisibleAlerts(List<Alert> alerts) {
        if (alerts == null) {
            return List.of();
        }
        return alerts.stream().filter(this::canViewAlert).collect(Collectors.toList());
    }

    public boolean canViewAlert(Alert alert) {
        if (alert == null) {
            return false;
        }
        if (isAdmin() || isDonor()) {
            return true;
        }
        return isHospitalStaff() && belongsToCurrentHospital(alert.getHospitalId());
    }

    public boolean canManageAlert(Alert alert) {
        return isAdmin() || (isHospitalStaff() && alert != null && belongsToCurrentHospital(alert.getHospitalId()));
    }

    public List<BloodTransferRequest> filterVisibleTransfers(List<BloodTransferRequest> transfers) {
        if (transfers == null) {
            return List.of();
        }
        return transfers.stream().filter(this::canViewTransfer).collect(Collectors.toList());
    }

    public boolean canViewTransfer(BloodTransferRequest transfer) {
        if (transfer == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        return isHospitalStaff() && (belongsToCurrentHospital(transfer.getRequestingHospitalId())
                || belongsToCurrentHospital(transfer.getApprovingHospitalId()));
    }

    public boolean canCreateTransfer() {
        return isAdmin() || isHospitalStaff();
    }

    public boolean canEditTransfer(BloodTransferRequest transfer) {
        return isAdmin() || (isHospitalStaff() && transfer != null && belongsToCurrentHospital(transfer.getRequestingHospitalId()));
    }

    public boolean canDeleteTransfer(BloodTransferRequest transfer) {
        return canEditTransfer(transfer);
    }

    public boolean canApproveTransfer(BloodTransferRequest transfer) {
        return isAdmin() || (isHospitalStaff() && transfer != null && belongsToCurrentHospital(transfer.getApprovingHospitalId()));
    }

    public boolean canCancelTransfer(BloodTransferRequest transfer) {
        return isAdmin() || (isHospitalStaff() && transfer != null
                && (belongsToCurrentHospital(transfer.getRequestingHospitalId())
                        || belongsToCurrentHospital(transfer.getApprovingHospitalId())));
    }

    public boolean canMarkTransferInTransit(BloodTransferRequest transfer) {
        return isAdmin() || (isHospitalStaff() && transfer != null && belongsToCurrentHospital(transfer.getApprovingHospitalId()));
    }

    public boolean canMarkTransferDelivered(BloodTransferRequest transfer) {
        return isAdmin() || (isHospitalStaff() && transfer != null && belongsToCurrentHospital(transfer.getRequestingHospitalId()));
    }

    public List<BloodInventory> filterVisibleInventories(List<BloodInventory> inventories) {
        if (inventories == null) {
            return List.of();
        }
        return inventories.stream().filter(this::canViewInventory).collect(Collectors.toList());
    }

    public boolean canViewInventory(BloodInventory inventory) {
        if (inventory == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        return isHospitalStaff() && belongsToCurrentHospital(inventory.getHospitalId());
    }

    private HospitalStaff loadHospitalStaff(Users user) {
        if (user == null || user.getUserType() != UserType.HOSPITAL_STAFF || user.getId() == null) {
            return null;
        }

        HospitalStaff probe = new HospitalStaff();
        probe.setId(user.getId());
        return new ServiceHospitalStaff().getOne(probe);
    }
}
