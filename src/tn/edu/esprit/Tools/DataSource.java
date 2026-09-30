/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tn.edu.esprit.Tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author abdelazizmezri
 */
public class DataSource {

    private Connection cnx;
    private static DataSource instance;

    private DataSource() {
        String url = requiredEnvironment("BLOODLINK_DB_URL");
        String user = requiredEnvironment("BLOODLINK_DB_USER");
        String password = requiredEnvironment("BLOODLINK_DB_PASSWORD");
        try {
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to DB !");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to connect to the configured database.", ex);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getConnection() {
        return this.cnx;
    }
}
