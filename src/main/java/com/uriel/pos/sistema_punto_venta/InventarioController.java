package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    @GetMapping("/todos")
    public List<Map<String, Object>> listarTodos(@RequestParam(defaultValue = "1") int sucursal) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT p.*, prov.nombre as nombre_proveedor " +
                     "FROM productos p " +
                     "LEFT JOIN proveedores prov ON p.id_proveedor = prov.id_proveedor " +
                     "WHERE p.activo = TRUE AND p.id_sucursal = ? ORDER BY p.nombre ASC";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, sucursal);
            try (ResultSet rs = stmt.executeQuery()) {
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
                    p.put("activo", rs.getBoolean("activo"));
                    byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                    p.put("foto_producto", fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null);
                    lista.add(p);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    @PostMapping("/guardar")
    public Map<String, Object> guardar(@RequestBody Map<String, Object> p) {
        Map<String, Object> res = new HashMap<>();
        boolean esUpdate = p.get("id_producto") != null && !p.get("id_producto").toString().isEmpty();
        int sucursal = p.get("sucursal") != null ? Integer.parseInt(p.get("sucursal").toString()) : 1;

        byte[] fotoBytes = null;
        if (p.get("foto_producto") != null && !p.get("foto_producto").toString().isEmpty()) {
            try {
                fotoBytes = Base64.getDecoder().decode(p.get("foto_producto").toString());
            } catch (IllegalArgumentException ignored) {}
        }

        String sql;
        if (esUpdate) {
            sql = fotoBytes != null
                ? "UPDATE productos SET codigo_barras=?, nombre=?, categoria=?, id_proveedor=?, precio_compra=?, precio_venta=?, existencias_act=?, existencias_min=?, foto_producto_blob=? WHERE id_producto=?"
                : "UPDATE productos SET codigo_barras=?, nombre=?, categoria=?, id_proveedor=?, precio_compra=?, precio_venta=?, existencias_act=?, existencias_min=? WHERE id_producto=?";
        } else {
            sql = fotoBytes != null
                ? "INSERT INTO productos (codigo_barras, nombre, categoria, id_proveedor, precio_compra, precio_venta, existencias_act, existencias_min, foto_producto_blob, id_sucursal) VALUES (?,?,?,?,?,?,?,?,?,?)"
                : "INSERT INTO productos (codigo_barras, nombre, categoria, id_proveedor, precio_compra, precio_venta, existencias_act, existencias_min, id_sucursal) VALUES (?,?,?,?,?,?,?,?,?)";
        }

        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.get("codigo_barras").toString());
            ps.setString(2, p.get("nombre").toString());
            ps.setString(3, p.get("categoria").toString());
            if (p.get("id_proveedor") != null && !p.get("id_proveedor").toString().isEmpty()) {
                ps.setInt(4, Integer.parseInt(p.get("id_proveedor").toString()));
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setDouble(5, Double.parseDouble(p.get("precio_compra").toString()));
            ps.setDouble(6, Double.parseDouble(p.get("precio_venta").toString()));
            ps.setInt(7, Integer.parseInt(p.get("existencias_act").toString()));
            ps.setInt(8, Integer.parseInt(p.get("existencias_min").toString()));
            if (esUpdate) {
                if (fotoBytes != null) { ps.setBytes(9, fotoBytes); ps.setInt(10, Integer.parseInt(p.get("id_producto").toString())); }
                else                   { ps.setInt(9, Integer.parseInt(p.get("id_producto").toString())); }
            } else {
                if (fotoBytes != null) { ps.setBytes(9, fotoBytes); ps.setInt(10, sucursal); }
                else                   { ps.setInt(9, sucursal); }
            }
            ps.executeUpdate();
            res.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/proveedores")
    public List<Map<String, Object>> listarProveedores(@RequestParam(defaultValue = "1") int sucursal) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_proveedor, nombre FROM proveedores WHERE activo = TRUE AND id_sucursal = ? ORDER BY nombre ASC";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, sucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_proveedor"));
                    m.put("nombre", rs.getString("nombre"));
                    lista.add(m);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    @GetMapping("/todos-proveedores")
    public List<Map<String, Object>> listarTodosProveedores(@RequestParam(defaultValue = "1") int sucursal) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_proveedor, nombre, contacto, telefono, activo FROM proveedores WHERE id_sucursal = ? ORDER BY nombre ASC";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, sucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_proveedor"));
                    m.put("nombre", rs.getString("nombre"));
                    m.put("contacto", rs.getString("contacto"));
                    m.put("telefono", rs.getString("telefono"));
                    m.put("activo", rs.getBoolean("activo"));
                    lista.add(m);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    @PostMapping("/guardar-proveedor")
    public Map<String, Object> guardarProveedor(@RequestBody Map<String, Object> datos) {
        Map<String, Object> res = new HashMap<>();
        String id      = datos.get("id")       != null ? datos.get("id").toString()       : "";
        String nombre  = datos.get("nombre")   != null ? datos.get("nombre").toString()   : "";
        String contacto= datos.get("contacto") != null ? datos.get("contacto").toString() : "";
        String telefono= datos.get("telefono") != null ? datos.get("telefono").toString() : "";
        int sucursal   = datos.get("sucursal") != null ? Integer.parseInt(datos.get("sucursal").toString()) : 1;

        if (nombre.isEmpty()) {
            res.put("success", false);
            res.put("message", "El nombre es obligatorio");
            return res;
        }

        if (!id.isEmpty()) {
            String sql = "UPDATE proveedores SET nombre=?, contacto=?, telefono=? WHERE id_proveedor=?";
            try (Connection c = new Conexion().conectar();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nombre); ps.setString(2, contacto);
                ps.setString(3, telefono); ps.setInt(4, Integer.parseInt(id));
                ps.executeUpdate();
                res.put("success", true);
                res.put("message", "Proveedor actualizado correctamente");
            } catch (Exception e) {
                e.printStackTrace();
                res.put("success", false);
                res.put("message", "Error: " + e.getMessage());
            }
        } else {
            String sql = "INSERT INTO proveedores (nombre, contacto, telefono, id_sucursal) VALUES (?, ?, ?, ?)";
            try (Connection c = new Conexion().conectar();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nombre); ps.setString(2, contacto); ps.setString(3, telefono); ps.setInt(4, sucursal);
                ps.executeUpdate();
                res.put("success", true);
                res.put("message", "Proveedor guardado correctamente");
            } catch (Exception e) {
                e.printStackTrace();
                res.put("success", false);
                res.put("message", "Error: " + e.getMessage());
            }
        }
        return res;
    }

    @PostMapping("/eliminar-proveedor/{id}")
    public Map<String, Object> eliminarProveedor(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try (Connection c = new Conexion().conectar()) {
            String checkSql = "SELECT COUNT(*) as count FROM productos WHERE id_proveedor = ?";
            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("count") > 0) {
                        try (PreparedStatement upd = c.prepareStatement("UPDATE proveedores SET activo = FALSE WHERE id_proveedor = ?")) {
                            upd.setInt(1, id); upd.executeUpdate();
                        }
                        res.put("success", true);
                        res.put("message", "Proveedor desactivado (tiene productos asociados)");
                    } else {
                        try (PreparedStatement del = c.prepareStatement("DELETE FROM proveedores WHERE id_proveedor = ?")) {
                            del.setInt(1, id); del.executeUpdate();
                        }
                        res.put("success", true);
                        res.put("message", "Proveedor eliminado correctamente");
                    }
                }
            }
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
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE proveedores SET activo = TRUE WHERE id_proveedor = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
            res.put("success", true);
            res.put("message", "Proveedor activado correctamente");
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
        try (Connection c = new Conexion().conectar()) {
            int idProducto = Integer.parseInt(datos.get("id_producto").toString());
            int cantidad   = Integer.parseInt(datos.get("cantidad").toString());
            String nota    = datos.get("nota") != null ? datos.get("nota").toString() : "";
            int idUsuario  = Integer.parseInt(datos.get("id_usuario").toString());
            int sucursal   = datos.get("sucursal") != null ? Integer.parseInt(datos.get("sucursal").toString()) : 1;
            Double precioCompra = datos.get("precio_compra") != null && !datos.get("precio_compra").toString().isEmpty()
                ? Double.parseDouble(datos.get("precio_compra").toString()) : null;

            int existActual = 0;
            try (PreparedStatement psGet = c.prepareStatement("SELECT existencias_act FROM productos WHERE id_producto = ?")) {
                psGet.setInt(1, idProducto);
                try (ResultSet rs = psGet.executeQuery()) {
                    if (rs.next()) existActual = rs.getInt("existencias_act");
                }
            }

            String sqlUpdate = "UPDATE productos SET existencias_act = existencias_act + ?" +
                               (precioCompra != null ? ", precio_compra = ?" : "") +
                               " WHERE id_producto = ?";
            try (PreparedStatement psUpdate = c.prepareStatement(sqlUpdate)) {
                psUpdate.setInt(1, cantidad);
                if (precioCompra != null) { psUpdate.setDouble(2, precioCompra); psUpdate.setInt(3, idProducto); }
                else                      { psUpdate.setInt(2, idProducto); }
                psUpdate.executeUpdate();
            }

            String sqlInsert = "INSERT INTO entradas_inventario (id_producto, cantidad, nota, id_usuario, fecha_entrada, id_sucursal) VALUES (?, ?, ?, ?, CONVERT_TZ(NOW(), '+02:00', '-04:00'), ?)";
            try (PreparedStatement psInsert = c.prepareStatement(sqlInsert)) {
                psInsert.setInt(1, idProducto); psInsert.setInt(2, cantidad);
                psInsert.setString(3, nota);    psInsert.setInt(4, idUsuario);
                psInsert.setInt(5, sucursal);
                psInsert.executeUpdate();
            }

            res.put("success", true);
            res.put("nueva_existencia", existActual + cantidad);
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/historial-entradas")
    public List<Map<String, Object>> historialEntradas(@RequestParam(defaultValue = "1") int sucursal) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT e.*, p.nombre as nombre_producto, u.nombre_completo as nombre_usuario " +
                     "FROM entradas_inventario e " +
                     "LEFT JOIN productos p ON e.id_producto = p.id_producto " +
                     "LEFT JOIN usuarios u ON e.id_usuario = u.id_usuario " +
                     "WHERE e.id_sucursal = ? ORDER BY e.fecha_entrada DESC LIMIT 20";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, sucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("id_entrada",      rs.getInt("id_entrada"));
                    e.put("id_producto",     rs.getInt("id_producto"));
                    e.put("nombre_producto", rs.getString("nombre_producto"));
                    e.put("cantidad",        rs.getInt("cantidad"));
                    e.put("nota",            rs.getString("nota"));
                    e.put("nombre_usuario",  rs.getString("nombre_usuario"));
                    e.put("fecha_entrada",   rs.getTimestamp("fecha_entrada").toString());
                    lista.add(e);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    @GetMapping("/paginado")
    public Map<String, Object> listarPaginado(@RequestParam(defaultValue = "1") int pagina,
                                               @RequestParam(defaultValue = "20") int limite,
                                               @RequestParam(defaultValue = "") String filtro,
                                               @RequestParam(defaultValue = "0") int idProveedor,
                                               @RequestParam(defaultValue = "1") int sucursal) {
        Map<String, Object> res = new HashMap<>();
        List<Map<String, Object>> lista = new ArrayList<>();
        boolean tieneFiltro   = !filtro.isEmpty();
        boolean tieneProveedor = idProveedor > 0;

        StringBuilder where = new StringBuilder(" WHERE p.id_sucursal = ?");
        if (tieneFiltro)   where.append(" AND (p.nombre LIKE ? OR p.codigo_barras LIKE ?)");
        if (tieneProveedor) where.append(" AND p.id_proveedor = ?");

        try (Connection c = new Conexion().conectar()) {
            int totalRegistros = 0;
            try (PreparedStatement psCount = c.prepareStatement("SELECT COUNT(*) as total FROM productos p" + where)) {
                int idx = 1;
                psCount.setInt(idx++, sucursal);
                if (tieneFiltro)    { psCount.setString(idx++, "%" + filtro + "%"); psCount.setString(idx++, "%" + filtro + "%"); }
                if (tieneProveedor) { psCount.setInt(idx, idProveedor); }
                try (ResultSet rs = psCount.executeQuery()) {
                    if (rs.next()) totalRegistros = rs.getInt("total");
                }
            }

            String sql = "SELECT p.*, prov.nombre as nombre_proveedor " +
                         "FROM productos p " +
                         "LEFT JOIN proveedores prov ON p.id_proveedor = prov.id_proveedor" +
                         where + " ORDER BY p.nombre ASC LIMIT ? OFFSET ?";
            try (PreparedStatement stmt = c.prepareStatement(sql)) {
                int idx = 1;
                stmt.setInt(idx++, sucursal);
                if (tieneFiltro)    { stmt.setString(idx++, "%" + filtro + "%"); stmt.setString(idx++, "%" + filtro + "%"); }
                if (tieneProveedor) { stmt.setInt(idx++, idProveedor); }
                stmt.setInt(idx++, limite);
                stmt.setInt(idx, (pagina - 1) * limite);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> p = new HashMap<>();
                        p.put("id_producto",     rs.getInt("id_producto"));
                        p.put("codigo_barras",   rs.getString("codigo_barras"));
                        p.put("nombre",          rs.getString("nombre"));
                        p.put("categoria",       rs.getString("categoria"));
                        p.put("nombre_proveedor",rs.getString("nombre_proveedor"));
                        p.put("id_proveedor",    rs.getInt("id_proveedor"));
                        p.put("precio_compra",   rs.getDouble("precio_compra"));
                        p.put("precio_venta",    rs.getDouble("precio_venta"));
                        p.put("existencias_act", rs.getInt("existencias_act"));
                        p.put("existencias_min", rs.getInt("existencias_min"));
                        p.put("activo",          rs.getBoolean("activo"));
                        byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                        p.put("foto_producto", fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null);
                        lista.add(p);
                    }
                }
            }

            res.put("productos",       lista);
            res.put("total_registros", totalRegistros);
            res.put("pagina_actual",   pagina);
            res.put("limite",          limite);
            res.put("total_paginas",   (int) Math.ceil((double) totalRegistros / limite));
        } catch (Exception e) {
            e.printStackTrace();
            res.put("error", e.getMessage());
        }
        return res;
    }

    @GetMapping("/buscar")
    public List<Map<String, Object>> buscarProductos(@RequestParam String q, @RequestParam(defaultValue = "1") int sucursal) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.codigo_barras, p.nombre, p.precio_venta, p.existencias_act, p.foto_producto_blob, p.activo " +
                     "FROM productos p " +
                     "WHERE (p.nombre LIKE ? OR p.codigo_barras LIKE ? OR p.codigo_barras_secundario LIKE ?) AND p.id_sucursal = ? " +
                     "ORDER BY p.nombre ASC LIMIT 20";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            String like = "%" + q + "%";
            stmt.setString(1, like); stmt.setString(2, like); stmt.setString(3, like); stmt.setInt(4, sucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id_producto",    rs.getInt("id_producto"));
                    p.put("codigo_barras",  rs.getString("codigo_barras"));
                    p.put("nombre",         rs.getString("nombre"));
                    p.put("precio_venta",   rs.getDouble("precio_venta"));
                    p.put("existencias_act",rs.getInt("existencias_act"));
                    p.put("activo",         rs.getBoolean("activo"));
                    byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                    p.put("foto_producto", fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null);
                    lista.add(p);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    @DeleteMapping("/eliminar/{id}")
    public Map<String, Object> eliminarProducto(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try (Connection c = new Conexion().conectar()) {
            int count = 0;
            try (PreparedStatement psCheck = c.prepareStatement("SELECT COUNT(*) FROM detalle_venta WHERE id_producto = ?")) {
                psCheck.setInt(1, id);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) count = rs.getInt(1);
                }
            }
            if (count > 0) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE productos SET activo = FALSE WHERE id_producto = ?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                res.put("success", true);
                res.put("message", "Producto desactivado (ya tenía ventas asociadas)");
            } else {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM productos WHERE id_producto = ?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                res.put("success", true);
                res.put("message", "Producto eliminado correctamente");
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/desactivar/{id}")
    public Map<String, Object> desactivarProducto(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE productos SET activo = FALSE WHERE id_producto = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
            res.put("success", true);
            res.put("message", "Producto desactivado");
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/activar/{id}")
    public Map<String, Object> activarProducto(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE productos SET activo = TRUE WHERE id_producto = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
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
