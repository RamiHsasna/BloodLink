/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tn.edu.esprit.services;
import tn.edu.esprit.entities.Donations;
import tn.edu.esprit.entities.DonationsEvent;


import java.util.List;

/**
 *
 * @author abdelazizmezri
 */
public interface IServicedon <T> {
    public void ajouter(T t);
    public void modifier(T t);
    public void supprimer(int id);
    public T getOne(T t);
    public List<T> getAll(T t);
}