/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tn.edu.esprit.Tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import tn.edu.esprit.services.AppConfig;

/**
 *
 * @author abdelazizmezri
 */
public class DataSource {

    private Connection cnx;
    private static DataSource instance;

    private String url = AppConfig.get("BLOODLINK_DB_URL", "DB_URL", "DATABASE_URL");
    private String user = AppConfig.get("BLOODLINK_DB_USER", "DB_USER", "PGUSER");
    private String password = AppConfig.get("BLOODLINK_DB_PASSWORD", "DB_PASSWORD", "PGPASSWORD");

    private DataSource() {
        try {
            if (url == null || user == null || password == null) {
                throw new SQLException("Missing database configuration. Set BLOODLINK_DB_URL, BLOODLINK_DB_USER, and BLOODLINK_DB_PASSWORD.");
            }
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to DB !");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
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
