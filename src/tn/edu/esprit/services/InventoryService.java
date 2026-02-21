package tn.edu.esprit.services;

import java.util.List;

public interface InventoryService<T> {
    public void ajouter(T t);
    public void modifier(T t);
    public void supprimer(Integer id);
    public T getInventory(T t);
    public T getInventoryById(Integer id);
    public List<T> getAllInventories();
    public boolean exists(Integer id);
}
