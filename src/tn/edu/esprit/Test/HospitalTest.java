package tn.edu.esprit.Test;

import tn.edu.esprit.Entities.Hospital;
import tn.edu.esprit.Services.HospitalServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HospitalTest {
    private static HospitalServiceImpl hospitalService;
    public static void main(String[] args) {
        hospitalService = new HospitalServiceImpl();

        testCreateHospital();
        testUpdateHospital();
        testDeleteHospital();
    }

    private static void testCreateHospital() {
        Hospital hospital = new Hospital();
        try {
            Hospital newHospital = new Hospital();
            newHospital.setName("Hôpital Test Centre");
            newHospital.setAddress("123 Rue de la Santé");
            newHospital.setCity("Tunis");
            newHospital.setLatitude(new BigDecimal("36.8065"));
            newHospital.setLongitude(new BigDecimal("10.1815"));
            newHospital.setPhone("+216 71 123 456");
            newHospital.setEmail("test@hospital.tn");
            newHospital.setActive(true);

            System.out.println("Creating hospital without ID (will be auto-generated):");
            System.out.println("Before insert - ID: " + newHospital.getHospitalId());

            hospitalService.ajouter(newHospital);

            System.out.println("After insert - Auto-generated ID: " + newHospital.getHospitalId());
            System.out.println("✓ Hospital created successfully with auto-generated UUID!\n");

        } catch (Exception e) {
            System.err.println("✗ Error creating hospital: " + e.getMessage());
        }
    }

    private static void testUpdateHospital() {

        try {
            // Get a hospital to update
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            if (!hospitals.isEmpty()) {
                Hospital hospitalToUpdate = hospitals.getFirst();
                UUID hospitalId = hospitalToUpdate.getHospitalId();

                System.out.println("Updating hospital: " + hospitalToUpdate.getName());
                System.out.println("Original phone: " + hospitalToUpdate.getPhone());

                // Update the hospital
                hospitalToUpdate.setPhone("+216 71 999 888");
                hospitalToUpdate.setEmail("updated@hospital.tn");
                hospitalService.modifier(hospitalToUpdate);

                // Retrieve the updated hospital
                Hospital updatedHospital = hospitalService.getHospitalById(hospitalId);
                System.out.println("Updated phone: " + updatedHospital.getPhone());
                System.out.println("Hospital updated successfully!");
            }

        } catch (Exception e) {
            System.err.println("Error in update operations: " + e.getMessage());
        }
    }
    private static void testDeleteHospital(){
        List<Hospital> hospitals = hospitalService.getAllHospitals();

        try{
            Hospital hospitalToDelete = hospitals.getLast(); // Get last one
            UUID idToDelete = hospitalToDelete.getHospitalId();

            System.out.println("Deleting hospital: " + hospitalToDelete.getName());
            hospitalService.supprimer(idToDelete);

            // Verify deletion
            Hospital deletedHospital = hospitalService.getHospitalById(idToDelete);
            if (deletedHospital == null) {
                System.out.println("Hospital deleted successfully!");
            } else {
                System.out.println("Hospital deletion failed!");
            }
        }catch(Exception e){
            System.err.println("Error deleting hospital: " + e.getMessage());
        }
    }
}
