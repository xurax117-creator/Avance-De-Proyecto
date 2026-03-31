package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.DriverManager;

public class Conexion {
    public Connection conectar() {
        Connection con = null;
        try {
            String host = System.getenv("MYSQLHOST");
            String port = System.getenv("MYSQLPORT");
            String db   = System.getenv("MYSQL_DATABASE");
            String user = System.getenv("MYSQLUSER");
            String pass = System.getenv("MYSQLPASSWORD");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=America/Mexico_City";

            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
        return con;
    }
}