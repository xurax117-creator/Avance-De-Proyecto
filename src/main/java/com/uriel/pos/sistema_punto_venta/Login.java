package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Login {

    public Map<String, Object> autenticar(String alias, String pass, int idSucursal) {
        Map<String, Object> datosUsuario = new HashMap<>();
        String sql = "SELECT id_usuario, nombre_completo, rol, contraseña FROM usuarios WHERE alias = ? AND activo = TRUE AND id_sucursal = ?";

        try {
            Conexion con = new Conexion();
            try (Connection c = con.conectar();
                 PreparedStatement stmt = c.prepareStatement(sql)) {

                stmt.setString(1, alias);
                stmt.setInt(2, idSucursal);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String hashAlmacenado = rs.getString("contraseña");
                        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                        if (hashAlmacenado != null && hashAlmacenado.startsWith("$2a$")) {
                            if (encoder.matches(pass, hashAlmacenado)) {
                                datosUsuario.put("id", rs.getInt("id_usuario"));
                                datosUsuario.put("nombre", rs.getString("nombre_completo"));
                                datosUsuario.put("rol", rs.getString("rol"));
                            }
                        } else {
                            if (pass.equals(hashAlmacenado)) {
                                datosUsuario.put("id", rs.getInt("id_usuario"));
                                datosUsuario.put("nombre", rs.getString("nombre_completo"));
                                datosUsuario.put("rol", rs.getString("rol"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datosUsuario;
    }
}
