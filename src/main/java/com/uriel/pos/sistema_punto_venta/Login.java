package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class Login {

    public Map<String, Object> autenticar(String alias, String pass) {
        Map<String, Object> datosUsuario = new HashMap<>();
        String sql = "SELECT id_usuario, nombre_completo, rol FROM usuarios WHERE alias = ? AND contraseña = ? AND activo = TRUE";
        
        try {
            Conexion con = new Conexion();
            try (Connection c = con.conectar();
                 PreparedStatement stmt = c.prepareStatement(sql)) {
                
                stmt.setString(1, alias);
                stmt.setString(2, pass);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        datosUsuario.put("id", rs.getInt("id_usuario"));
                        datosUsuario.put("nombre", rs.getString("nombre_completo"));
                        datosUsuario.put("rol", rs.getString("rol"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datosUsuario;
    }
}