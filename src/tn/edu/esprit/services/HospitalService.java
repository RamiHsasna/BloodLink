package tn.edu.esprit.services;

import java.util.List;
import java.util.UUID;

public interface HospitalService<T> {
    public void ajouter(T t);
    public void modifier(T t);
    public void supprimer(UUID id);
    public T getHospital(T t);
    public T getHospitalById(UUID id);
    public List<T> getAllHospitals();
    public boolean exists(UUID id);
}
