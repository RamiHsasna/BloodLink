package tn.edu.esprit.Test;

import java.util.List;
import tn.edu.esprit.entities.BloodType;
import tn.edu.esprit.services.BloodTypeServiceImpl;

public class BloodTypeTest {

    private static BloodTypeServiceImpl bloodTypeService;

    public static void main(String[] args) {
        bloodTypeService = new BloodTypeServiceImpl();
        testCreateBloodType();
        testUpdateBloodType();
        testReadBloodType();
        testDeleteBloodType();
    }

    public static void testCreateBloodType() {
        try {
            BloodType bloodType = new BloodType();
            bloodType.setBloodTypeId("A+");
            bloodType.setAboType("A");
            bloodType.setRhFactor("POSITIVE");
            bloodType.setUniversalDonor(false);
            bloodType.setUniversalRecipient(false);
            bloodType.setCompatibleDonors("O,A");

            bloodTypeService.ajouter(bloodType);
            System.out.println("BloodType created successfully");
        } catch (Exception e) {
            System.err.println("Error creating bloodType: " + e.getMessage());
        }
    }

    public static void testUpdateBloodType() {
        try {
            // Update the "A+" blood type we just created
            BloodType bloodType = new BloodType();
            bloodType.setBloodTypeId("A+");
            bloodType.setAboType("A");
            bloodType.setRhFactor("POSITIVE");
            bloodType.setUniversalDonor(false);
            bloodType.setUniversalRecipient(false);
            bloodType.setCompatibleDonors("O+,O-,A+,A-"); // updated compatible donors

            bloodTypeService.modifier(bloodType);
            System.out.println("BloodType updated successfully");

            // Verify the update by reading it back
            BloodType lookup = new BloodType();
            lookup.setBloodTypeId("A+");
            BloodType updated = (BloodType) bloodTypeService.getBloodType(
                lookup
            );
            if (updated != null) {
                System.out.println("Verified updated blood type: " + updated);
                System.out.println(
                    "Compatible donors after update: " +
                        updated.getCompatibleDonors()
                );
            }
        } catch (Exception e) {
            System.err.println("Error updating bloodType: " + e.getMessage());
        }
    }

    public static void testReadBloodType() {
        try {
            // Read a single blood type by passing a BloodType object with the ID set
            BloodType lookup = new BloodType();
            lookup.setBloodTypeId("A+");
            BloodType result = (BloodType) bloodTypeService.getBloodType(
                lookup
            );
            if (result != null) {
                System.out.println("Read single blood type: " + result);
            } else {
                System.out.println("Blood type 'A+' not found");
            }

            // Read all blood types
            List<BloodType> allBloodTypes = bloodTypeService.getAllBloodTypes();
            System.out.println(
                "Total blood types in database: " + allBloodTypes.size()
            );
            for (BloodType bt : allBloodTypes) {
                System.out.println("  -> " + bt);
            }

            // Check existence
            boolean exists = bloodTypeService.exists("A+");
            System.out.println("Blood type 'A+' exists: " + exists);

            boolean notExists = bloodTypeService.exists("Z-");
            System.out.println("Blood type 'Z-' exists: " + notExists);
        } catch (Exception e) {
            System.err.println("Error reading bloodType: " + e.getMessage());
        }
    }

    public static void testDeleteBloodType() {
        try {
            // Delete the "A+" blood type
            bloodTypeService.supprimer("A+");
            System.out.println("BloodType deleted successfully");

            // Verify deletion
            boolean stillExists = bloodTypeService.exists("A+");
            System.out.println(
                "Blood type 'A+' still exists after delete: " + stillExists
            );
        } catch (Exception e) {
            System.err.println("Error deleting bloodType: " + e.getMessage());
        }
    }
}
