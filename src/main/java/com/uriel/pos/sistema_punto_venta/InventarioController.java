package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

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

    @GetMapping("/todos-proveedores")
    public List<Map<String, Object>> listarTodosProveedores() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT id_proveedor, nombre, contacto, telefono, activo FROM proveedores ORDER BY nombre ASC";
            ResultSet rs = c.prepareStatement(sql).executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id_proveedor"));
                m.put("nombre", rs.getString("nombre"));
                m.put("contacto", rs.getString("contacto"));
                m.put("telefono", rs.getString("telefono"));
                m.put("activo", rs.getBoolean("activo"));
                lista.add(m);
            }
            c.close();
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    @PostMapping("/guardar-proveedor")
    public Map<String, Object> guardarProveedor(@RequestBody Map<String, Object> datos) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String id = datos.get("id") != null ? datos.get("id").toString() : "";
            String nombre = datos.get("nombre") != null ? datos.get("nombre").toString() : "";
            String contacto = datos.get("contacto") != null ? datos.get("contacto").toString() : "";
            String telefono = datos.get("telefono") != null ? datos.get("telefono").toString() : "";
            
            if (nombre.isEmpty()) {
                res.put("success", false);
                res.put("message", "El nombre es obligatorio");
                return res;
            }
            
            if (!id.isEmpty()) {
                // Actualizar
                String sql = "UPDATE proveedores SET nombre=?, contacto=?, telefono=? WHERE id_proveedor=?";
                PreparedStatement ps = c.prepareStatement(sql);
                ps.setString(1, nombre);
                ps.setString(2, contacto);
                ps.setString(3, telefono);
                ps.setInt(4, Integer.parseInt(id));
                ps.executeUpdate();
                res.put("success", true);
                res.put("message", "Proveedor actualizado correctamente");
            } else {
                // Insertar
                String sql = "INSERT INTO proveedores (nombre, contacto, telefono) VALUES (?, ?, ?)";
                PreparedStatement ps = c.prepareStatement(sql);
                ps.setString(1, nombre);
                ps.setString(2, contacto);
                ps.setString(3, telefono);
                ps.executeUpdate();
                res.put("success", true);
                res.put("message", "Proveedor guardado correctamente");
            }
            c.close();
        } catch (Exception e) { 
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    @PostMapping("/eliminar-proveedor/{id}")
    public Map<String, Object> eliminarProveedor(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            // Verificar si tiene productos asociados
            String checkSql = "SELECT COUNT(*) as count FROM productos WHERE id_proveedor = ?";
            PreparedStatement checkStmt = c.prepareStatement(checkSql);
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 0) {
                // Si tiene productos, solo desactivar
                String sql = "UPDATE proveedores SET activo = FALSE WHERE id_proveedor = ?";
                PreparedStatement ps = c.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                res.put("success", true);
                res.put("message", "Proveedor desactivado (tiene productos asociados)");
            } else {
                // Si no tiene productos, eliminar
                String sql = "DELETE FROM proveedores WHERE id_proveedor = ?";
                PreparedStatement ps = c.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                res.put("success", true);
                res.put("message", "Proveedor eliminado correctamente");
            }
            c.close();
        } catch (Exception e) { 
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    @PostMapping("/activar-proveedor/{id}")
    public Map<String, Object> activarProveedor(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "UPDATE proveedores SET activo = TRUE WHERE id_proveedor = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            res.put("success", true);
            res.put("message", "Proveedor activado correctamente");
            c.close();
        } catch (Exception e) { 
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    @PostMapping("/registrar-entrada")
    public Map<String, Object> registrarEntrada(@RequestBody Map<String, Object> datos) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            int idProducto = Integer.parseInt(datos.get("id_producto").toString());
            int cantidad = Integer.parseInt(datos.get("cantidad").toString());
            String nota = datos.get("nota") != null ? datos.get("nota").toString() : "";
            int idUsuario = Integer.parseInt(datos.get("id_usuario").toString());
            Double precioCompra = datos.get("precio_compra") != null && !datos.get("precio_compra").toString().isEmpty() 
                ? Double.parseDouble(datos.get("precio_compra").toString()) : null;
            
            // Primero obtener la existencia actual
            int existActual = 0;
            String sqlGet = "SELECT existencias_act FROM productos WHERE id_producto = ?";
            PreparedStatement psGet = c.prepareStatement(sqlGet);
            psGet.setInt(1, idProducto);
            ResultSet rs = psGet.executeQuery();
            if (rs.next()) {
                existActual = rs.getInt("existencias_act");
            }
            rs.close();
            psGet.close();
            
            // Actualizar el stock del producto
            String sqlUpdate = "UPDATE productos SET existencias_act = existencias_act + ?";
            if (precioCompra != null) {
                sqlUpdate += ", precio_compra = ?";
            }
            sqlUpdate += " WHERE id_producto = ?";
            
            PreparedStatement psUpdate = c.prepareStatement(sqlUpdate);
            psUpdate.setInt(1, cantidad);
            if (precioCompra != null) {
                psUpdate.setDouble(2, precioCompra);
                psUpdate.setInt(3, idProducto);
            } else {
                psUpdate.setInt(2, idProducto);
            }
            psUpdate.executeUpdate();
            psUpdate.close();
            
            // Registrar en el historial de entradas
            String sqlInsert = "INSERT INTO entradas_inventario (id_producto, cantidad, nota, id_usuario, fecha_entrada) VALUES (?, ?, ?, ?, NOW())";
            PreparedStatement psInsert = c.prepareStatement(sqlInsert);
            psInsert.setInt(1, idProducto);
            psInsert.setInt(2, cantidad);
            psInsert.setString(3, nota);
            psInsert.setInt(4, idUsuario);
            psInsert.executeUpdate();
            psInsert.close();
            
            res.put("success", true);
            res.put("nueva_existencia", existActual + cantidad);
            
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/historial-entradas")
    public List<Map<String, Object>> historialEntradas() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT e.*, p.nombre as nombre_producto, u.nombre as nombre_usuario " +
                        "FROM entradas_inventario e " +
                        "LEFT JOIN productos p ON e.id_producto = p.id_producto " +
                        "LEFT JOIN usuarios u ON e.id_usuario = u.id_usuario " +
                        "ORDER BY e.fecha_entrada DESC LIMIT 20";
            PreparedStatement stmt = c.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> e = new HashMap<>();
                e.put("id_entrada", rs.getInt("id_entrada"));
                e.put("id_producto", rs.getInt("id_producto"));
                e.put("nombre_producto", rs.getString("nombre_producto"));
                e.put("cantidad", rs.getInt("cantidad"));
                e.put("nota", rs.getString("nota"));
                e.put("nombre_usuario", rs.getString("nombre_usuario"));
                e.put("fecha_entrada", rs.getTimestamp("fecha_entrada").toString());
                lista.add(e);
            }
            c.close();
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // Endpoint para productos paginados
    @GetMapping("/paginado")
    public Map<String, Object> listarPaginado(@RequestParam(defaultValue = "1") int pagina,
                                               @RequestParam(defaultValue = "20") int limite,
                                               @RequestParam(defaultValue = "") String filtro) {
        Map<String, Object> res = new HashMap<>();
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            // Contar total de productos (incluyendo desactivados para inventario)
            String sqlCount = "SELECT COUNT(*) as total FROM productos";
            if (!filtro.isEmpty()) {
                sqlCount += " WHERE nombre LIKE ? OR codigo_barras LIKE ?";
            }
            PreparedStatement psCount = c.prepareStatement(sqlCount);
            if (!filtro.isEmpty()) {
                psCount.setString(1, "%" + filtro + "%");
                psCount.setString(2, "%" + filtro + "%");
            }
            ResultSet rsCount = psCount.executeQuery();
            int totalRegistros = 0;
            if (rsCount.next()) {
                totalRegistros = rsCount.getInt("total");
            }
            rsCount.close();
            psCount.close();
            
            // Calcular offset
            int offset = (pagina - 1) * limite;
            
            // Obtener productos paginados (en inventario muestra todos incluyendo desactivados)
            String sql = "SELECT p.*, prov.nombre as nombre_proveedor, " +
                         "(SELECT COUNT(*) FROM productos p2 WHERE p2.nombre < p.nombre OR (p2.nombre = p.nombre AND p2.id_producto < p.id_producto)) + 1 as numero_producto " +
                         "FROM productos p " +
                         "LEFT JOIN proveedores prov ON p.id_proveedor = prov.id_proveedor ";
            
            if (!filtro.isEmpty()) {
                sql += " WHERE (p.nombre LIKE ? OR p.codigo_barras LIKE ?) ";
            }
            sql += " ORDER BY p.nombre ASC LIMIT ? OFFSET ?";
            
            PreparedStatement stmt = c.prepareStatement(sql);
            int paramIndex = 1;
            if (!filtro.isEmpty()) {
                stmt.setString(paramIndex++, "%" + filtro + "%");
                stmt.setString(paramIndex++, "%" + filtro + "%");
            }
            stmt.setInt(paramIndex++, limite);
            stmt.setInt(paramIndex++, offset);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("numero_producto", rs.getInt("numero_producto"));
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
                p.put("activo", rs.getBoolean("activo"));
                
                byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                if (fotoBytes != null) {
                    p.put("foto_producto", Base64.getEncoder().encodeToString(fotoBytes));
                } else {
                    p.put("foto_producto", null);
                }
                
                lista.add(p);
            }
            c.close();
            
            res.put("productos", lista);
            res.put("total_registros", totalRegistros);
            res.put("pagina_actual", pagina);
            res.put("limite", limite);
            res.put("total_paginas", (int) Math.ceil((double) totalRegistros / limite));
            
        } catch (Exception e) {
            e.printStackTrace();
            res.put("error", e.getMessage());
        }
        return res;
    }

    // Endpoint para buscar productos (búsqueda parcial) - solo productos activos
    @GetMapping("/buscar")
    public List<Map<String, Object>> buscarProductos(@RequestParam String q) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT p.id_producto, p.codigo_barras, p.nombre, p.precio_venta, p.existencias_act, p.foto_producto_blob " +
                         "FROM productos p " +
                         "WHERE p.activo = TRUE AND (p.nombre LIKE ? OR p.codigo_barras LIKE ?) " +
                         "ORDER BY p.nombre ASC LIMIT 20";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, "%" + q + "%");
            stmt.setString(2, "%" + q + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("id_producto", rs.getInt("id_producto"));
                p.put("codigo_barras", rs.getString("codigo_barras"));
                p.put("nombre", rs.getString("nombre"));
                p.put("precio_venta", rs.getDouble("precio_venta"));
                p.put("existencias_act", rs.getInt("existencias_act"));
                
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

    // Endpoint para eliminar producto (borrado físico)
    @DeleteMapping("/eliminar/{id}")
    public Map<String, Object> eliminarProducto(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            // Verificar si el producto tiene ventas asociadas
            String sqlCheck = "SELECT COUNT(*) FROM detalle_venta WHERE id_producto = ?";
            PreparedStatement psCheck = c.prepareStatement(sqlCheck);
            psCheck.setInt(1, id);
            ResultSet rs = psCheck.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            psCheck.close();
            
            if (count > 0) {
                // Si tiene ventas, solo desactivar
                String sqlUpdate = "UPDATE productos SET activo = FALSE WHERE id_producto = ?";
                PreparedStatement psUpdate = c.prepareStatement(sqlUpdate);
                psUpdate.setInt(1, id);
                psUpdate.executeUpdate();
                psUpdate.close();
                res.put("success", true);
                res.put("message", "Producto desactivado (ya tenía ventas asociadas)");
            } else {
                // Si no tiene ventas, borrar físicamente
                String sqlDelete = "DELETE FROM productos WHERE id_producto = ?";
                PreparedStatement psDelete = c.prepareStatement(sqlDelete);
                psDelete.setInt(1, id);
                psDelete.executeUpdate();
                psDelete.close();
                res.put("success", true);
                res.put("message", "Producto eliminado correctamente");
            }
            
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // Endpoint para desactivar producto
    @PostMapping("/desactivar/{id}")
    public Map<String, Object> desactivarProducto(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "UPDATE productos SET activo = FALSE WHERE id_producto = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            c.close();
            res.put("success", true);
            res.put("message", "Producto desactivado");
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // Endpoint para activar producto
    @PostMapping("/activar/{id}")
    public Map<String, Object> activarProducto(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "UPDATE productos SET activo = TRUE WHERE id_producto = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            c.close();
            res.put("success", true);
            res.put("message", "Producto activado");
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }
}
