package tn.edu.esprit.Services;

import java.util.List;

public interface TransfertService<T> {
    public void ajouter(T t);
    public void modifier(T t);
    public void supprimer(Integer id);
    public T getTransfert(T t);
    public T getTransfertById(Integer id);
    public List<T> getAllTransferts();
    public boolean exists(Integer id);
}
