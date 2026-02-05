package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    // --- ENDPOINT PARA PRODUCTOS ---
    @GetMapping("/todos")
    public List<Map<String, Object>> listarTodos() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT p.*, prov.nombre as nombre_proveedor " +
                         "FROM productos p " +
                         "LEFT JOIN proveedores prov ON p.id_proveedor = prov.id_proveedor " +
                         "WHERE p.activo = TRUE ORDER BY p.nombre ASC";
            PreparedStatement stmt = c.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("id_producto", rs.getInt("id_producto"));
                p.put("codigo_barras", rs.getString("codigo_barras"));
                p.put("nombre", rs.getString("nombre"));
                p.put("categoria", rs.getString("categoria"));
                p.put("nombre_proveedor", rs.getString("nombre_proveedor"));
                p.put("id_proveedor", rs.getInt("id_proveedor"));
                p.put("precio_compra", rs.getDouble("precio_compra"));
                p.put("precio_venta", rs.getDouble("precio_venta"));
                p.put("existencias_act", rs.getInt("existencias_act"));
                p.put("existencias_min", rs.getInt("existencias_min"));
                
                // Convertir foto a Base64
                byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                if (fotoBytes != null) {
                    p.put("foto_producto", Base64.getEncoder().encodeToString(fotoBytes));
                } else {
                    p.put("foto_producto", null);
                }
                
                lista.add(p);
            }
            c.close();
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    @PostMapping("/guardar")
    public Map<String, Object> guardar(@RequestBody Map<String, Object> p) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql;
            boolean esUpdate = p.get("id_producto") != null && !p.get("id_producto").toString().isEmpty();

            // Procesar foto
            byte[] fotoBytes = null;
            if (p.get("foto_producto") != null && !p.get("foto_producto").toString().isEmpty()) {
                try {
                    fotoBytes = Base64.getDecoder().decode(p.get("foto_producto").toString());
                } catch (IllegalArgumentException e) {
                    fotoBytes = null;
                }
            }

            if (esUpdate) {
                if (fotoBytes != null) {
                    sql = "UPDATE productos SET codigo_barras=?, nombre=?, categoria=?, id_proveedor=?, precio_compra=?, precio_venta=?, existencias_act=?, existencias_min=?, foto_producto_blob=? WHERE id_producto=?";
                } else {
                    sql = "UPDATE productos SET codigo_barras=?, nombre=?, categoria=?, id_proveedor=?, precio_compra=?, precio_venta=?, existencias_act=?, existencias_min=? WHERE id_producto=?";
                }
            } else {
                if (fotoBytes != null) {
                    sql = "INSERT INTO productos (codigo_barras, nombre, categoria, id_proveedor, precio_compra, precio_venta, existencias_act, existencias_min, foto_producto_blob) VALUES (?,?,?,?,?,?,?,?,?)";
                } else {
                    sql = "INSERT INTO productos (codigo_barras, nombre, categoria, id_proveedor, precio_compra, precio_venta, existencias_act, existencias_min) VALUES (?,?,?,?,?,?,?,?)";
                }
            }

            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, p.get("codigo_barras").toString());
            ps.setString(2, p.get("nombre").toString());
            ps.setString(3, p.get("categoria").toString());
            
            if (p.get("id_proveedor") != null && !p.get("id_proveedor").toString().isEmpty()) {
                ps.setInt(4, Integer.parseInt(p.get("id_proveedor").toString()));
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }

            ps.setDouble(5, Double.parseDouble(p.get("precio_compra").toString()));
            ps.setDouble(6, Double.parseDouble(p.get("precio_venta").toString()));
            ps.setInt(7, Integer.parseInt(p.get("existencias_act").toString()));
            ps.setInt(8, Integer.parseInt(p.get("existencias_min").toString()));
            
            if (esUpdate) {
                if (fotoBytes != null) {
                    ps.setBytes(9, fotoBytes);
                    ps.setInt(10, Integer.parseInt(p.get("id_producto").toString()));
                } else {
                    ps.setInt(9, Integer.parseInt(p.get("id_producto").toString()));
                }
            } else {
                if (fotoBytes != null) {
                    ps.setBytes(9, fotoBytes);
                }
            }

            ps.executeUpdate();
            res.put("success", true);
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // --- ENDPOINT PARA PROVEEDORES (UNIFICADO) ---
    @GetMapping("/proveedores")
    public List<Map<String, Object>> listarProveedores() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT id_proveedor, nombre FROM proveedores WHERE activo = TRUE ORDER BY nombre ASC";
            ResultSet rs = c.prepareStatement(sql).executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id_proveedor"));
                m.put("nombre", rs.getString("nombre"));
                lista.add(m);
            }
            c.close();
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }
}
