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

    //private String url = "jdbc:mysql://localhost:3306/esprit"; mysql connector
    // private String url = "jdbc:postgresql://localhost:5432/esprit"; //postgresql connector
    private String url =
        "jdbc:postgresql://<configured-at-runtime>";
    private String user = "<configured-at-runtime>";
    private String password = "<configured-at-runtime>";

    private DataSource() {
        try {
            // cnx = DriverManager.getConnection(url, user, password);
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
