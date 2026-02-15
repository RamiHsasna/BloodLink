package tn.edu.esprit.Services;

import java.util.List;
import java.util.UUID;

public interface ServiceHospital<T> {
    public void ajouter(T t);
    public void modifier(T t);
    public void supprimer(UUID id);
    public T getHospital(T t);
    public T getHospitalById(UUID id);
    public List<T> getAllHospitals();
    public boolean exists(UUID id);
}
