package com.uriel.pos.sistema_punto_venta;

import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Usuario {

    public List<UsuarioData> obtenerTodos() {
        List<UsuarioData> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "SELECT id_usuario, nombre_completo, alias, contraseña, rol, activo, foto_perfil_blob FROM usuarios ORDER BY id_usuario DESC";
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                byte[] fotoBytes = rs.getBytes("foto_perfil_blob");
                String fotoBase64 = fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null;
                
                UsuarioData u = new UsuarioData(
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_completo"),
                    rs.getString("alias"),
                    rs.getString("contraseña"),
                    rs.getString("rol"),
                    rs.getBoolean("activo"),
                    fotoBase64
                );
                lista.add(u);
            }
            
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public UsuarioData obtenerPorId(int id) {
        UsuarioData u = null;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "SELECT id_usuario, nombre_completo, alias, contraseña, rol, activo, foto_perfil_blob FROM usuarios WHERE id_usuario = ?";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                byte[] fotoBytes = rs.getBytes("foto_perfil_blob");
                String fotoBase64 = fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null;
                
                u = new UsuarioData(
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_completo"),
                    rs.getString("alias"),
                    rs.getString("contraseña"),
                    rs.getString("rol"),
                    rs.getBoolean("activo"),
                    fotoBase64
                );
            }
            
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return u;
    }

    public UsuarioData login(String alias, String contraseña) {
        UsuarioData u = null;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "SELECT id_usuario, nombre_completo, alias, contraseña, rol, activo, foto_perfil_blob FROM usuarios WHERE alias = ? AND contraseña = ? AND activo = TRUE";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, alias);
            stmt.setString(2, contraseña);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                byte[] fotoBytes = rs.getBytes("foto_perfil_blob");
                String fotoBase64 = fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null;
                
                u = new UsuarioData(
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_completo"),
                    rs.getString("alias"),
                    rs.getString("contraseña"),
                    rs.getString("rol"),
                    rs.getBoolean("activo"),
                    fotoBase64
                );
            }
            
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return u;
    }

    public boolean crearUsuario(String nombreCompleto, String alias, String contraseña, String rol, byte[] fotoPerfil) {
        boolean resultado = false;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String contraseñaCifrada = encoder.encode(contraseña);
            
            String sql = "INSERT INTO usuarios (nombre_completo, alias, contraseña, rol, activo, foto_perfil_blob) VALUES (?, ?, ?, ?, TRUE, ?)";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, nombreCompleto);
            stmt.setString(2, alias);
            stmt.setString(3, contraseñaCifrada);
            stmt.setString(4, rol);
            if (fotoPerfil != null && fotoPerfil.length > 0) {
                stmt.setBytes(5, fotoPerfil);
            } else {
                stmt.setNull(5, Types.BLOB);
            }
            
            resultado = stmt.executeUpdate() > 0;
            
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }

    public boolean actualizarUsuario(int id, String nombreCompleto, String alias, String contraseña, String rol, boolean activo, byte[] fotoPerfil) {
        boolean resultado = false;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String contraseñaCifrada = encoder.encode(contraseña);
            
            String sql;
            PreparedStatement stmt;
            
            if (fotoPerfil != null) {
                sql = "UPDATE usuarios SET nombre_completo = ?, alias = ?, contraseña = ?, rol = ?, activo = ?, foto_perfil_blob = ? WHERE id_usuario = ?";
                stmt = c.prepareStatement(sql);
                stmt.setString(1, nombreCompleto);
                stmt.setString(2, alias);
                stmt.setString(3, contraseñaCifrada);
                stmt.setString(4, rol);
                stmt.setBoolean(5, activo);
                stmt.setBytes(6, fotoPerfil);
                stmt.setInt(7, id);
            } else {
                sql = "UPDATE usuarios SET nombre_completo = ?, alias = ?, contraseña = ?, rol = ?, activo = ? WHERE id_usuario = ?";
                stmt = c.prepareStatement(sql);
                stmt.setString(1, nombreCompleto);
                stmt.setString(2, alias);
                stmt.setString(3, contraseñaCifrada);
                stmt.setString(4, rol);
                stmt.setBoolean(5, activo);
                stmt.setInt(6, id);
            }
            
            resultado = stmt.executeUpdate() > 0;
            
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }

    public boolean eliminarUsuario(int id) {
        boolean resultado = false;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, id);
            
            resultado = stmt.executeUpdate() > 0;
            
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }

    public boolean cambiarActivo(int id, boolean activo) {
        boolean resultado = false;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "UPDATE usuarios SET activo = ? WHERE id_usuario = ?";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setBoolean(1, activo);
            stmt.setInt(2, id);
            
            resultado = stmt.executeUpdate() > 0;
            
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }
}

class UsuarioData {
    public int idUsuario;
    public String nombreCompleto;
    public String alias;
    public String contraseña;
    public String rol;
    public boolean activo;
    public String fotoPerfil;

    public UsuarioData(int idUsuario, String nombreCompleto, String alias, String contraseña, String rol, boolean activo, String fotoPerfil) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.alias = alias;
        this.contraseña = contraseña;
        this.rol = rol;
        this.activo = activo;
        this.fotoPerfil = fotoPerfil;
    }
}
