package tn.edu.esprit.services;

import tn.edu.esprit.entities.DonorAlert;

import java.util.List;

public interface DonorAlertService {
    void ajouter(DonorAlert donorAlert);

    void modifier(DonorAlert donorAlert);

    void supprimer(String donorAlertId);

    DonorAlert getById(String donorAlertId);

    List<DonorAlert> getAll();

    List<DonorAlert> getByDonorId(String donorId);

    List<DonorAlert> getByAlertId(String alertId);
}
