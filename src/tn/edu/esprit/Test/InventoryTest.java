package tn.edu.esprit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import tn.edu.esprit.entities.BloodInventory;
import tn.edu.esprit.entities.Hospital;
import tn.edu.esprit.entities.InventoryStatus;
import tn.edu.esprit.services.HospitalServiceImpl;
import tn.edu.esprit.services.InventoryServiceImpl;

public class InventoryTest {

    private static InventoryServiceImpl inventoryService;
    private static HospitalServiceImpl hospitalService;

    public static void main(String[] args) {
        inventoryService = new InventoryServiceImpl();
        hospitalService = new HospitalServiceImpl();

        //testCreateInventory();
        //testReadInventory();
        //testUpdateInventory();
        testDeleteInventory();
    }

    private static void testCreateInventory() {
        try {
            // Grab an existing hospital to use as a valid foreign key
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            if (hospitals.isEmpty()) {
                System.err.println(
                    "No hospitals found in the database. Please create a hospital first."
                );
                return;
            }
            UUID hospitalId = hospitals.getFirst().getHospitalId();

            BloodInventory newInventory = new BloodInventory();
            newInventory.setHospitalId(hospitalId);
            newInventory.setBloodTypeId("A+");
            newInventory.setDonorId("DONOR-011");
            newInventory.setQuantityUnitsInt(10);
            newInventory.setQuantityUnitsDecimal(new BigDecimal("10.50"));
            newInventory.setExpirationDate(
                Date.valueOf(LocalDate.now().plusDays(42))
            );
            newInventory.setStatus(InventoryStatus.OPTIMAL);

            System.out.println(
                "Creating inventory without ID (will be auto-generated):"
            );
            System.out.println(
                "Before insert - ID: " + newInventory.getInventoryId()
            );

            inventoryService.ajouter(newInventory);

            System.out.println(
                "After insert - Auto-generated ID: " +
                    newInventory.getInventoryId()
            );
            System.out.println("Inventory created successfully!\n");
        } catch (Exception e) {
            System.err.println("sError creating inventory: " + e.getMessage());
        }
    }

    private static void testReadInventory() {
        try {
            // Read all inventories
            List<BloodInventory> inventories =
                inventoryService.getAllInventories();
            System.out.println(
                "Total inventories in database: " + inventories.size()
            );
            for (BloodInventory inv : inventories) {
                System.out.println(
                    "  -> ID: " +
                        inv.getInventoryId() +
                        " | Blood Type: " +
                        inv.getBloodTypeId() +
                        " | Qty: " +
                        inv.getQuantityUnitsInt() +
                        " | Status: " +
                        inv.getStatus() +
                        " | Expires: " +
                        inv.getExpirationDate()
                );
            }

            // Read a single inventory by ID
            if (!inventories.isEmpty()) {
                Integer firstId = inventories.getFirst().getInventoryId();
                BloodInventory fetched =
                    (BloodInventory) inventoryService.getInventoryById(firstId);
                if (fetched != null) {
                    System.out.println(
                        "Read single inventory by ID " +
                            firstId +
                            ": Blood Type = " +
                            fetched.getBloodTypeId()
                    );
                }

                // Check existence
                boolean exists = inventoryService.exists(firstId);
                System.out.println(
                    "Inventory with ID " + firstId + " exists: " + exists
                );

                boolean notExists = inventoryService.exists(999999);
                System.out.println(
                    "Inventory with ID 999999 exists: " + notExists
                );
            }

            System.out.println("✓ Inventory read operations completed!\n");
        } catch (Exception e) {
            System.err.println("✗ Error reading inventory: " + e.getMessage());
        }
    }

    private static void testUpdateInventory() {
        try {
            List<BloodInventory> inventories =
                inventoryService.getAllInventories();
            if (!inventories.isEmpty()) {
                BloodInventory inventoryToUpdate = (BloodInventory) inventoryService.getInventoryById(10);
                Integer inventoryId = inventoryToUpdate.getInventoryId();

                System.out.println("Updating inventory ID: " + inventoryId);
                System.out.println(
                    "Original quantity: " +
                        inventoryToUpdate.getQuantityUnitsInt()
                );
                System.out.println(
                    "Original status: " + inventoryToUpdate.getStatus()
                );

                // Update fields
                inventoryToUpdate.setQuantityUnitsInt(25);
                inventoryToUpdate.setQuantityUnitsDecimal(
                    new BigDecimal("25.75")
                );
                inventoryToUpdate.setStatus(InventoryStatus.LOW);
                inventoryToUpdate.setExpirationDate(
                    Date.valueOf(LocalDate.now().plusDays(30))
                );

                inventoryService.modifier(inventoryToUpdate);

                // Verify the update
                BloodInventory updatedInventory =
                    (BloodInventory) inventoryService.getInventoryById(
                        inventoryId
                    );
                System.out.println(
                    "Updated quantity: " +
                        updatedInventory.getQuantityUnitsInt()
                );
                System.out.println(
                    "Updated status: " + updatedInventory.getStatus()
                );
                System.out.println("✓ Inventory updated successfully!\n");
            } else {
                System.out.println("No inventories to update.");
            }
        } catch (Exception e) {
            System.err.println("✗ Error updating inventory: " + e.getMessage());
        }
    }

    private static void testDeleteInventory() {
        try {
            List<BloodInventory> inventories =
                inventoryService.getAllInventories();
            if (!inventories.isEmpty()) {
                BloodInventory inventoryToDelete = inventories.getLast();
                Integer idToDelete = inventoryToDelete.getInventoryId();

                System.out.println("Deleting inventory ID: " + idToDelete);
                inventoryService.supprimer(idToDelete);

                // Verify deletion
                BloodInventory deletedInventory =
                    (BloodInventory) inventoryService.getInventoryById(
                        idToDelete
                    );
                if (deletedInventory == null) {
                    System.out.println("✓ Inventory deleted successfully!\n");
                } else {
                    System.out.println("✗ Inventory deletion failed!\n");
                }
            } else {
                System.out.println("No inventories to delete.");
            }
        } catch (Exception e) {
            System.err.println("✗ Error deleting inventory: " + e.getMessage());
        }
    }
}
