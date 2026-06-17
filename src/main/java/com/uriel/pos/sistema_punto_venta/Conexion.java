package com.uriel.pos.sistema_punto_venta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Conexion {

    private static final HikariDataSource pool;

    static {
        HikariConfig config = new HikariConfig();

        String password = System.getenv("MYSQLPASSWORD");

        String url;
        String user;
        if (password == null) {
            // Entorno local — MYSQLPASSWORD no existe en esta máquina
            url      = "jdbc:mysql://127.0.0.1:3306/railway?useSSL=false&serverTimezone=America/Mexico_City";
            user     = "root";
            password = "1234";
        } else {
            // Producción (Railway) — todas las variables de entorno están presentes
            String host     = System.getenv("MYSQLHOST");
            String port     = System.getenv("MYSQLPORT");
            String database = System.getenv("MYSQL_DATABASE");
            user = System.getenv("MYSQLUSER");
            url  = "jdbc:mysql://" + host + ":" + port + "/" + database
                   + "?useSSL=false&serverTimezone=America/Mexico_City";
        }

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        pool = new HikariDataSource(config);
    }

    public Connection conectar() {
        try {
            return pool.getConnection();
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}
