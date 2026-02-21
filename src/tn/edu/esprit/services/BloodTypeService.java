package tn.edu.esprit.services;

import java.util.List;

public interface BloodTypeService<T> {
    public void ajouter(T t);
    public void modifier(T t);
    public void supprimer(String id);
    public T getBloodType(T t);
    public List<T> getAllBloodTypes();
    public boolean exists(String id);
}
