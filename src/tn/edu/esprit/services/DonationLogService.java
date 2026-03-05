package tn.edu.esprit.services;

import tn.edu.esprit.entities.DonationLog;
import tn.edu.esprit.entities.DonationLogAction;

import java.util.List;

public interface DonationLogService {
    void ajouter(DonationLog log);

    void modifier(DonationLog log);

    void supprimer(String logId);

    DonationLog getById(String logId);

    List<DonationLog> getAll();

    List<DonationLog> getByDonationId(String donationId);

    List<DonationLog> getByAction(DonationLogAction action);
}
