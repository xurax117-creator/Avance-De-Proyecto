package com.uriel.pos.sistema_punto_venta;

import java.sql.*;
import java.util.*;

public class ReporteDAO {

    public List<Map<String, Object>> obtenerVentas(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String fechaHoraInicio = inicio + " " + horaInicio + ":00";
        String fechaHoraFin    = fin    + " " + horaFin    + ":59";

        try (Connection c = new Conexion().conectar()) {
            int totalRegistros = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) as total FROM ventas WHERE fecha BETWEEN ? AND ?")) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalRegistros = rs.getInt("total"); }
            }

            double totalGeneral = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(total), 0) as total_general FROM ventas WHERE fecha BETWEEN ? AND ?")) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalGeneral = rs.getDouble("total_general"); }
            }

            // ROW_NUMBER() reemplaza la subconsulta correlacionada O(n²) anterior
            String sql = "SELECT id_venta, fecha, nombre, total, numero_venta FROM (" +
                         "SELECT v.id_venta, v.fecha, u.nombre_completo as nombre, v.total, " +
                         "ROW_NUMBER() OVER (ORDER BY v.id_venta DESC) as numero_venta " +
                         "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                         "WHERE v.fecha BETWEEN ? AND ?) sub " +
                         "ORDER BY id_venta DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, fechaHoraInicio);
                ps.setString(2, fechaHoraFin);
                ps.setInt(3, tamanoPagina);
                ps.setInt(4, (pagina - 1) * tamanoPagina);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("idVenta",     rs.getInt("id_venta"));
                        map.put("numeroVenta", rs.getInt("numero_venta"));
                        map.put("fecha",       rs.getString("fecha"));
                        map.put("nombre",      rs.getString("nombre"));
                        map.put("total",       rs.getDouble("total"));
                        lista.add(map);
                    }
                }
            }

            Map<String, Object> pagInfo = new HashMap<>();
            pagInfo.put("totalRegistros", totalRegistros);
            pagInfo.put("totalGeneral",   totalGeneral);
            pagInfo.put("paginaActual",   pagina);
            pagInfo.put("tamanoPagina",   tamanoPagina);
            pagInfo.put("totalPaginas",   Math.max(1, (int) Math.ceil((double) totalRegistros / tamanoPagina)));
            lista.add(pagInfo);
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Map<String, Object>> obtenerVentas(String inicio, String fin) {
        return obtenerVentas(inicio, "00:00", fin, "23:59", 1, 1000);
    }

    public List<Map<String, Object>> obtenerDetalleVenta(int idVenta) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT p.nombre, dv.cantidad, dv.precio_unitario, dv.subtotal " +
                     "FROM detalle_venta dv JOIN productos p ON dv.id_producto = p.id_producto " +
                     "WHERE dv.id_venta = ?";
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("producto", rs.getString("nombre"));
                    map.put("cantidad", rs.getDouble("cantidad"));
                    map.put("precio",   rs.getDouble("precio_unitario"));
                    map.put("subtotal", rs.getDouble("subtotal"));
                    lista.add(map);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina, String busqueda) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String fechaHoraInicio = inicio + " " + horaInicio + ":00";
        String fechaHoraFin    = fin    + " " + horaFin    + ":59";
        boolean filtrar = busqueda != null && !busqueda.isBlank();
        String like = filtrar ? "%" + busqueda.trim() + "%" : null;
        String extraWhere = filtrar ? " AND (p.nombre LIKE ? OR p.codigo_barras LIKE ?)" : "";

        try (Connection c = new Conexion().conectar()) {
            int totalRegistros = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(DISTINCT p.id_producto) as total FROM detalle_venta dv " +
                    "JOIN productos p ON dv.id_producto = p.id_producto " +
                    "JOIN ventas v ON dv.id_venta = v.id_venta WHERE v.fecha BETWEEN ? AND ?" + extraWhere)) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin);
                if (filtrar) { ps.setString(3, like); ps.setString(4, like); }
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalRegistros = rs.getInt("total"); }
            }

            double totalGeneral = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(total), 0) as total_general FROM ventas WHERE fecha BETWEEN ? AND ?")) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalGeneral = rs.getDouble("total_general"); }
            }

            String sql = "SELECT p.nombre, SUM(dv.cantidad) as cant, SUM(dv.cantidad * dv.precio_unitario) as total " +
                         "FROM detalle_venta dv JOIN productos p ON dv.id_producto = p.id_producto " +
                         "JOIN ventas v ON dv.id_venta = v.id_venta " +
                         "WHERE v.fecha BETWEEN ? AND ?" + extraWhere +
                         " GROUP BY p.id_producto ORDER BY cant DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin);
                if (filtrar) {
                    ps.setString(3, like); ps.setString(4, like);
                    ps.setInt(5, tamanoPagina); ps.setInt(6, (pagina - 1) * tamanoPagina);
                } else {
                    ps.setInt(3, tamanoPagina); ps.setInt(4, (pagina - 1) * tamanoPagina);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("nombre",   rs.getString("nombre"));
                        map.put("cantidad", rs.getDouble("cant"));
                        map.put("total",    rs.getDouble("total"));
                        lista.add(map);
                    }
                }
            }

            Map<String, Object> pagInfo = new HashMap<>();
            pagInfo.put("totalRegistros", totalRegistros);
            pagInfo.put("totalGeneral",   totalGeneral);
            pagInfo.put("paginaActual",   pagina);
            pagInfo.put("tamanoPagina",   tamanoPagina);
            pagInfo.put("totalPaginas",   Math.max(1, (int) Math.ceil((double) totalRegistros / tamanoPagina)));
            lista.add(pagInfo);
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina) {
        return obtenerTopProductos(inicio, horaInicio, fin, horaFin, pagina, tamanoPagina, null);
    }

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String fin) {
        return obtenerTopProductos(inicio, "00:00", fin, "23:59", 1, 1000, null);
    }

    public List<Map<String, Object>> obtenerVentasPorCajero(String inicio, String horaInicio, String fin, String horaFin) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String fechaHoraInicio = inicio + " " + horaInicio + ":00";
        String fechaHoraFin    = fin    + " " + horaFin    + ":59";
        String sql = "SELECT u.nombre_completo as nombre, COUNT(v.id_venta) as num_ventas, SUM(v.total) as total_vendido " +
                     "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                     "WHERE v.fecha BETWEEN ? AND ? GROUP BY u.id_usuario ORDER BY total_vendido DESC";
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("nombre",    rs.getString("nombre"));
                    map.put("numVentas", rs.getInt("num_ventas"));
                    map.put("total",     rs.getDouble("total_vendido"));
                    lista.add(map);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public Map<String, Object> obtenerValorInventario() {
        Map<String, Object> resultado = new HashMap<>();
        String sql = "SELECT COALESCE(SUM(existencias_act * precio_compra), 0) as total_compra, " +
                     "COALESCE(SUM(existencias_act * precio_venta), 0) as total_venta " +
                     "FROM productos WHERE existencias_act > 0";
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                resultado.put("totalCompra", rs.getDouble("total_compra"));
                resultado.put("totalVenta",  rs.getDouble("total_venta"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return resultado;
    }
}
