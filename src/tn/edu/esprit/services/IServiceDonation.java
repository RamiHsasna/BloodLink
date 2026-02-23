package tn.edu.esprit.services;

import tn.edu.esprit.entities.Donations;

import java.util.List;

public interface IServiceDonation {

    void ajouter(Object o);

    void modifier(Object o);

    void supprimer(String donationId);

    List<Donations> getAll();

    Donations getOne(String donationId);

    Object getDonation(Object o);

    List getAllDonations();

    boolean exists(String donationId);

    Object getDonationById(String donationId);
}