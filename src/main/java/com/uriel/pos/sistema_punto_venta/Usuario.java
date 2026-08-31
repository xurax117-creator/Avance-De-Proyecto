package com.uriel.pos.sistema_punto_venta;

import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Usuario {

    public List<UsuarioData> obtenerTodos(int idSucursal) {
        List<UsuarioData> lista = new ArrayList<>();
        // No se selecciona "contraseña": es un hash BCrypt de un solo sentido y nunca debe
        // exponerse al frontend, se use o no ahí.
        String sql = "SELECT id_usuario, nombre_completo, alias, rol, activo, foto_perfil_blob FROM usuarios WHERE id_sucursal = ? ORDER BY id_usuario DESC";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, idSucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    byte[] fotoBytes = rs.getBytes("foto_perfil_blob");
                    lista.add(new UsuarioData(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_completo"),
                        rs.getString("alias"),
                        rs.getString("rol"),
                        rs.getBoolean("activo"),
                        fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public UsuarioData obtenerPorId(int id) {
        UsuarioData u = null;
        String sql = "SELECT id_usuario, nombre_completo, alias, rol, activo, foto_perfil_blob FROM usuarios WHERE id_usuario = ?";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] fotoBytes = rs.getBytes("foto_perfil_blob");
                    u = new UsuarioData(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_completo"),
                        rs.getString("alias"),
                        rs.getString("rol"),
                        rs.getBoolean("activo"),
                        fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null
                    );
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return u;
    }

    public boolean crearUsuario(String nombreCompleto, String alias, String contraseña, String rol, byte[] fotoPerfil, int idSucursal) {
        String sql = "INSERT INTO usuarios (nombre_completo, alias, contraseña, rol, activo, foto_perfil_blob, id_sucursal) VALUES (?, ?, ?, ?, TRUE, ?, ?)";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, nombreCompleto);
            stmt.setString(2, alias);
            stmt.setString(3, new BCryptPasswordEncoder().encode(contraseña));
            stmt.setString(4, rol);
            if (fotoPerfil != null && fotoPerfil.length > 0) stmt.setBytes(5, fotoPerfil);
            else stmt.setNull(5, Types.BLOB);
            stmt.setInt(6, idSucursal);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // contraseña es opcional aquí: si viene vacía o nula, se conserva la actual sin tocarla.
    public boolean actualizarUsuario(int id, String nombreCompleto, String alias, String contraseña, String rol, boolean activo, byte[] fotoPerfil) {
        boolean cambiarPassword = contraseña != null && !contraseña.isBlank();

        StringBuilder sql = new StringBuilder("UPDATE usuarios SET nombre_completo=?, alias=?, rol=?, activo=?");
        if (cambiarPassword) sql.append(", contraseña=?");
        if (fotoPerfil != null) sql.append(", foto_perfil_blob=?");
        sql.append(" WHERE id_usuario=?");

        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setString(idx++, nombreCompleto);
            stmt.setString(idx++, alias);
            stmt.setString(idx++, rol);
            stmt.setBoolean(idx++, activo);
            if (cambiarPassword) stmt.setString(idx++, new BCryptPasswordEncoder().encode(contraseña));
            if (fotoPerfil != null) stmt.setBytes(idx++, fotoPerfil);
            stmt.setInt(idx, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean eliminarUsuario(int id) {
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement("DELETE FROM usuarios WHERE id_usuario = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean cambiarActivo(int id, boolean activo) {
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement("UPDATE usuarios SET activo = ? WHERE id_usuario = ?")) {
            stmt.setBoolean(1, activo);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}

class UsuarioData {
    public int idUsuario;
    public String nombreCompleto;
    public String alias;
    public String rol;
    public boolean activo;
    public String fotoPerfil;

    public UsuarioData(int idUsuario, String nombreCompleto, String alias, String rol, boolean activo, String fotoPerfil) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.alias = alias;
        this.rol = rol;
        this.activo = activo;
        this.fotoPerfil = fotoPerfil;
    }
}
