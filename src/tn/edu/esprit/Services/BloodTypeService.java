package tn.edu.esprit.Services;

import java.util.List;
import java.util.UUID;

public interface BloodTypeService<T> {
    public void ajouter(T t);
    public void modifier(T t);
    public void supprimer(UUID id);
    public T getBloodType(T t);
    public T getBloodTypeById(UUID id);
    public List<T> getAllBloodTypes();
    public boolean exists(UUID id);
}
