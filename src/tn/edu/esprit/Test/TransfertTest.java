package tn.edu.esprit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import tn.edu.esprit.Entities.BloodTransferRequest;
import tn.edu.esprit.Entities.Hospital;
import tn.edu.esprit.Entities.TransfertStatus;
import tn.edu.esprit.Services.HospitalServiceImpl;
import tn.edu.esprit.Services.TransfertServiceImpl;

public class TransfertTest {

    private static TransfertServiceImpl transfertService;
    private static HospitalServiceImpl hospitalService;

    public static void main(String[] args) {
        transfertService = new TransfertServiceImpl();
        hospitalService = new HospitalServiceImpl();

        testCreateTransfert();
        testReadTransfert();
        testUpdateTransfert();
        testDeleteTransfert();
    }

    private static void testCreateTransfert() {
        try {
            // Grab existing hospitals to use as valid foreign keys
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            if (hospitals.size() < 2) {
                System.err.println(
                    "✗ At least 2 hospitals are needed in the database. Please create hospitals first."
                );
                return;
            }
            UUID requestingHospitalId = hospitals.getFirst().getHospitalId();
            UUID approvingHospitalId = hospitals.get(1).getHospitalId();

            BloodTransferRequest newTransfert = new BloodTransferRequest();
            newTransfert.setRequestingHospitalId(requestingHospitalId);
            newTransfert.setApprovingHospitalId(approvingHospitalId);
            newTransfert.setRequestingStaffId("STAFF-001");
            newTransfert.setApprovingStaffId("STAFF-002");
            newTransfert.setBloodTypeId("A+");
            newTransfert.setQuantityUnitsRequested(5);
            newTransfert.setQuantityUnitsApproved(0);
            newTransfert.setStatus(TransfertStatus.PENDING);
            newTransfert.setReason("Emergency surgery requiring A+ blood");
            newTransfert.setDeliveryExpectedAt(
                Timestamp.valueOf(LocalDateTime.now().plusDays(2))
            );
            newTransfert.setNotes("Urgent request - priority delivery");

            System.out.println(
                "Creating transfer request without ID (will be auto-generated):"
            );
            System.out.println(
                "Before insert - ID: " + newTransfert.getTransferId()
            );

            transfertService.ajouter(newTransfert);

            System.out.println(
                "After insert - Auto-generated ID: " +
                    newTransfert.getTransferId()
            );
            System.out.println("✓ Transfer request created successfully!\n");
        } catch (Exception e) {
            System.err.println(
                "✗ Error creating transfer request: " + e.getMessage()
            );
        }
    }

    private static void testReadTransfert() {
        try {
            // Read all transfer requests
            List<BloodTransferRequest> transferts =
                transfertService.getAllTransferts();
            System.out.println(
                "Total transfer requests in database: " + transferts.size()
            );
            for (BloodTransferRequest t : transferts) {
                System.out.println(
                    "  -> ID: " +
                        t.getTransferId() +
                        " | Blood Type: " +
                        t.getBloodTypeId() +
                        " | Requested: " +
                        t.getQuantityUnitsRequested() +
                        " | Approved: " +
                        t.getQuantityUnitsApproved() +
                        " | Status: " +
                        t.getStatus() +
                        " | Reason: " +
                        t.getReason()
                );
            }

            // Read a single transfer request by ID
            if (!transferts.isEmpty()) {
                Integer firstId = transferts.getFirst().getTransferId();
                BloodTransferRequest fetched =
                    (BloodTransferRequest) transfertService.getTransfertById(
                        firstId
                    );
                if (fetched != null) {
                    System.out.println(
                        "Read single transfer by ID " +
                            firstId +
                            ": Status = " +
                            fetched.getStatus() +
                            ", Reason = " +
                            fetched.getReason()
                    );
                }

                // Check existence
                boolean exists = transfertService.exists(firstId);
                System.out.println(
                    "Transfer with ID " + firstId + " exists: " + exists
                );

                boolean notExists = transfertService.exists(999999);
                System.out.println(
                    "Transfer with ID 999999 exists: " + notExists
                );
            }

            System.out.println(
                "✓ Transfer request read operations completed!\n"
            );
        } catch (Exception e) {
            System.err.println(
                "✗ Error reading transfer request: " + e.getMessage()
            );
        }
    }

    private static void testUpdateTransfert() {
        try {
            List<BloodTransferRequest> transferts =
                transfertService.getAllTransferts();
            if (!transferts.isEmpty()) {
                BloodTransferRequest transfertToUpdate = transferts.getFirst();
                Integer transfertId = transfertToUpdate.getTransferId();

                System.out.println(
                    "Updating transfer request ID: " + transfertId
                );
                System.out.println(
                    "Original status: " + transfertToUpdate.getStatus()
                );
                System.out.println(
                    "Original quantity approved: " +
                        transfertToUpdate.getQuantityUnitsApproved()
                );
                System.out.println(
                    "Original notes: " + transfertToUpdate.getNotes()
                );

                // Simulate approval of the transfer request
                transfertToUpdate.setStatus(TransfertStatus.APPROVED);
                transfertToUpdate.setQuantityUnitsApproved(
                    transfertToUpdate.getQuantityUnitsRequested()
                );
                transfertToUpdate.setNotes(
                    "Approved - full quantity granted for emergency surgery"
                );
                transfertToUpdate.setDeliveryExpectedAt(
                    Timestamp.valueOf(LocalDateTime.now().plusDays(1))
                );

                transfertService.modifier(transfertToUpdate);

                // Verify the update
                BloodTransferRequest updatedTransfert =
                    (BloodTransferRequest) transfertService.getTransfertById(
                        transfertId
                    );
                System.out.println(
                    "Updated status: " + updatedTransfert.getStatus()
                );
                System.out.println(
                    "Updated quantity approved: " +
                        updatedTransfert.getQuantityUnitsApproved()
                );
                System.out.println(
                    "Updated notes: " + updatedTransfert.getNotes()
                );
                System.out.println(
                    "✓ Transfer request updated successfully!\n"
                );
            } else {
                System.out.println("No transfer requests to update.");
            }
        } catch (Exception e) {
            System.err.println(
                "✗ Error updating transfer request: " + e.getMessage()
            );
        }
    }

    private static void testDeleteTransfert() {
        try {
            List<BloodTransferRequest> transferts =
                transfertService.getAllTransferts();
            if (!transferts.isEmpty()) {
                BloodTransferRequest transfertToDelete = transferts.getLast();
                Integer idToDelete = transfertToDelete.getTransferId();

                System.out.println(
                    "Deleting transfer request ID: " + idToDelete
                );
                transfertService.supprimer(idToDelete);

                // Verify deletion
                BloodTransferRequest deletedTransfert =
                    (BloodTransferRequest) transfertService.getTransfertById(
                        idToDelete
                    );
                if (deletedTransfert == null) {
                    System.out.println(
                        "✓ Transfer request deleted successfully!\n"
                    );
                } else {
                    System.out.println("✗ Transfer request deletion failed!\n");
                }
            } else {
                System.out.println("No transfer requests to delete.");
            }
        } catch (Exception e) {
            System.err.println(
                "✗ Error deleting transfer request: " + e.getMessage()
            );
        }
    }
}
