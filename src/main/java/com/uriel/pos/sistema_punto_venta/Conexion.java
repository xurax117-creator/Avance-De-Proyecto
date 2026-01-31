package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.DriverManager;

public class Conexion {

    public Connection conectar() {
        Connection con = null;
        try {
            String url = "jdbc:mysql://localhost:3306/DB_Punto_De_Venta";
            String user = "root";
            String pass = "1234";

            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }
}