package tn.edu.esprit.services;

import tn.edu.esprit.entities.Alert;

import java.util.List;

public interface AlertService {
    void ajouter(Alert alert);

    void modifier(Alert alert);

    void supprimer(String alertId);

    Alert getById(String alertId);

    List<Alert> getAll();

    List<Alert> getByHospitalId(String hospitalId);
}
