package tn.edu.esprit.services;

import tn.edu.esprit.entities.BloodTransferRequestLog;
import tn.edu.esprit.entities.TransferLogAction;

import java.util.List;

public interface BloodTransferRequestLogService {
    void ajouter(BloodTransferRequestLog log);

    void modifier(BloodTransferRequestLog log);

    void supprimer(String logId);

    BloodTransferRequestLog getById(String logId);

    List<BloodTransferRequestLog> getAll();

    List<BloodTransferRequestLog> getByTransferId(int transferId);

    List<BloodTransferRequestLog> getByAction(TransferLogAction action);
}
