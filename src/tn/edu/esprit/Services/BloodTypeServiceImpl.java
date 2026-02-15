package tn.edu.esprit.Services;

import tn.edu.esprit.Tools.DataSource;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class BloodTypeServiceImpl implements BloodTypeService {
    private static final Logger LOGGER = Logger.getLogger(
        BloodTypeServiceImpl.class.getName()
    );

    public BloodTypeServiceImpl(Connection connection) {
        Connection connection1 = DataSource.getInstance().getConnection();
    }


    @Override
    public void ajouter(Object o) {

    }

    @Override
    public void modifier(Object o) {

    }

    @Override
    public void supprimer(UUID id) {

    }

    @Override
    public Object getBloodType(Object o) {
        return null;
    }

    @Override
    public Object getBloodTypeById(UUID id) {
        return null;
    }

    @Override
    public List getAllBloodTypes() {
        return List.of();
    }

    @Override
    public boolean exists(UUID id) {
        return false;
    }
}
