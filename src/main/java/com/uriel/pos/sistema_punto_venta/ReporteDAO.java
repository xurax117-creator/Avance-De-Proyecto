package com.uriel.pos.sistema_punto_venta;

import java.sql.*;
import java.util.*;

public class ReporteDAO {

    public List<Map<String, Object>> obtenerVentas(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina, int sucursal, String categoria) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String fechaHoraInicio = inicio + " " + horaInicio + ":00";
        String fechaHoraFin    = fin    + " " + horaFin    + ":59";

        boolean filtrarCat = categoria != null && !categoria.isBlank();
        String catSubquery = filtrarCat
            ? " AND v.id_venta IN (SELECT DISTINCT dv2.id_venta FROM detalle_venta dv2 JOIN productos p2 ON dv2.id_producto = p2.id_producto WHERE p2.categoria = ?)"
            : "";

        try (Connection c = new Conexion().conectar()) {
            int totalRegistros = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) as total FROM ventas v WHERE v.fecha BETWEEN ? AND ? AND v.id_sucursal = ?" + catSubquery)) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, sucursal);
                if (filtrarCat) ps.setString(4, categoria);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalRegistros = rs.getInt("total"); }
            }

            double totalGeneral = 0, totalEfectivo = 0, totalTarjeta = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(total - total_devuelto),0) as tg, COALESCE(SUM(monto_efectivo),0) as ef, COALESCE(SUM(monto_tarjeta),0) as tj FROM ventas v WHERE v.fecha BETWEEN ? AND ? AND v.id_sucursal = ?" + catSubquery)) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, sucursal);
                if (filtrarCat) ps.setString(4, categoria);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalGeneral  = rs.getDouble("tg");
                        totalEfectivo = rs.getDouble("ef");
                        totalTarjeta  = rs.getDouble("tj");
                    }
                }
            }

            String sql = "SELECT id_venta, fecha, nombre, total, total_devuelto, monto_efectivo, monto_tarjeta, numero_venta FROM (" +
                         "SELECT v.id_venta, v.fecha, u.nombre_completo as nombre, v.total, v.total_devuelto, v.monto_efectivo, v.monto_tarjeta, " +
                         "ROW_NUMBER() OVER (ORDER BY v.id_venta DESC) as numero_venta " +
                         "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                         "WHERE v.fecha BETWEEN ? AND ? AND v.id_sucursal = ?" + catSubquery + ") sub " +
                         "ORDER BY id_venta DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, fechaHoraInicio);
                ps.setString(2, fechaHoraFin);
                ps.setInt(3, sucursal);
                int idx = 4;
                if (filtrarCat) ps.setString(idx++, categoria);
                ps.setInt(idx++, tamanoPagina);
                ps.setInt(idx,   (pagina - 1) * tamanoPagina);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double totalDevuelto = rs.getDouble("total_devuelto");
                        Map<String, Object> map = new HashMap<>();
                        map.put("idVenta",       rs.getInt("id_venta"));
                        map.put("numeroVenta",   rs.getInt("numero_venta"));
                        map.put("fecha",         rs.getString("fecha"));
                        map.put("nombre",        rs.getString("nombre"));
                        map.put("total",         rs.getDouble("total") - totalDevuelto);
                        map.put("totalDevuelto", totalDevuelto);
                        map.put("montoEfectivo", rs.getDouble("monto_efectivo"));
                        map.put("montoTarjeta",  rs.getDouble("monto_tarjeta"));
                        lista.add(map);
                    }
                }
            }

            Map<String, Object> pagInfo = new HashMap<>();
            pagInfo.put("totalRegistros", totalRegistros);
            pagInfo.put("totalGeneral",   totalGeneral);
            pagInfo.put("totalEfectivo",  totalEfectivo);
            pagInfo.put("totalTarjeta",   totalTarjeta);
            pagInfo.put("paginaActual",   pagina);
            pagInfo.put("tamanoPagina",   tamanoPagina);
            pagInfo.put("totalPaginas",   Math.max(1, (int) Math.ceil((double) totalRegistros / tamanoPagina)));
            lista.add(pagInfo);
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Map<String, Object>> obtenerVentas(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina, int sucursal) {
        return obtenerVentas(inicio, horaInicio, fin, horaFin, pagina, tamanoPagina, sucursal, "");
    }

    public List<Map<String, Object>> obtenerVentas(String inicio, String fin) {
        return obtenerVentas(inicio, "00:00", fin, "23:59", 1, 1000, 1, "");
    }

    public List<String> obtenerCategorias(int sucursal) {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT COALESCE(NULLIF(TRIM(categoria), ''), 'General') as cat " +
                     "FROM productos WHERE id_sucursal = ? ORDER BY cat";
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sucursal);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cats.add(rs.getString("cat"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return cats;
    }

    public List<Map<String, Object>> obtenerDetalleVenta(int idVenta) {
        List<Map<String, Object>> lista = new ArrayList<>();
        // La cantidad y el subtotal se muestran netos (ya descontando lo devuelto),
        // para que el ticket refleje lo que realmente se quedó vendido.
        String sql = "SELECT dv.id_detalle, p.nombre, dv.cantidad, dv.precio_unitario, " +
                     "COALESCE(SUM(dd.cantidad), 0) AS cantidad_devuelta " +
                     "FROM detalle_venta dv " +
                     "JOIN productos p ON dv.id_producto = p.id_producto " +
                     "LEFT JOIN detalle_devolucion dd ON dd.id_detalle_venta = dv.id_detalle " +
                     "WHERE dv.id_venta = ? " +
                     "GROUP BY dv.id_detalle, p.nombre, dv.cantidad, dv.precio_unitario " +
                     "ORDER BY dv.id_detalle";
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double cantidadOriginal = rs.getDouble("cantidad");
                    double cantidadDevuelta = rs.getDouble("cantidad_devuelta");
                    double cantidadNeta = cantidadOriginal - cantidadDevuelta;
                    double precioUnitario = rs.getDouble("precio_unitario");

                    Map<String, Object> map = new HashMap<>();
                    map.put("producto",         rs.getString("nombre"));
                    map.put("cantidad",         cantidadNeta);
                    map.put("cantidadDevuelta", cantidadDevuelta);
                    map.put("precio",           precioUnitario);
                    map.put("subtotal",         cantidadNeta * precioUnitario);
                    lista.add(map);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina, String busqueda, int sucursal) {
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
                    "JOIN ventas v ON dv.id_venta = v.id_venta WHERE v.fecha BETWEEN ? AND ? AND v.id_sucursal = ?" + extraWhere)) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, sucursal);
                if (filtrar) { ps.setString(4, like); ps.setString(5, like); }
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalRegistros = rs.getInt("total"); }
            }

            double totalGeneral = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(total), 0) as total_general FROM ventas WHERE fecha BETWEEN ? AND ? AND id_sucursal = ?")) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, sucursal);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalGeneral = rs.getDouble("total_general"); }
            }

            String sql = "SELECT p.nombre, SUM(dv.cantidad) as cant, SUM(dv.cantidad * dv.precio_unitario) as total " +
                         "FROM detalle_venta dv JOIN productos p ON dv.id_producto = p.id_producto " +
                         "JOIN ventas v ON dv.id_venta = v.id_venta " +
                         "WHERE v.fecha BETWEEN ? AND ? AND v.id_sucursal = ?" + extraWhere +
                         " GROUP BY p.id_producto ORDER BY cant DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, sucursal);
                if (filtrar) {
                    ps.setString(4, like); ps.setString(5, like);
                    ps.setInt(6, tamanoPagina); ps.setInt(7, (pagina - 1) * tamanoPagina);
                } else {
                    ps.setInt(4, tamanoPagina); ps.setInt(5, (pagina - 1) * tamanoPagina);
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

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina, String busqueda) {
        return obtenerTopProductos(inicio, horaInicio, fin, horaFin, pagina, tamanoPagina, busqueda, 1);
    }

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String horaInicio, String fin, String horaFin, int pagina, int tamanoPagina) {
        return obtenerTopProductos(inicio, horaInicio, fin, horaFin, pagina, tamanoPagina, null, 1);
    }

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String fin) {
        return obtenerTopProductos(inicio, "00:00", fin, "23:59", 1, 1000, null, 1);
    }

    public List<Map<String, Object>> obtenerVentasPorCajero(String inicio, String horaInicio, String fin, String horaFin, int sucursal) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String fechaHoraInicio = inicio + " " + horaInicio + ":00";
        String fechaHoraFin    = fin    + " " + horaFin    + ":59";
        String sql = "SELECT u.nombre_completo as nombre, COUNT(v.id_venta) as num_ventas, SUM(v.total) as total_vendido " +
                     "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                     "WHERE v.fecha BETWEEN ? AND ? AND v.id_sucursal = ? GROUP BY u.id_usuario ORDER BY total_vendido DESC";
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, sucursal);
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

    public Map<String, Object> obtenerValorInventario(int sucursal) {
        Map<String, Object> resultado = new HashMap<>();
        String sql = "SELECT COALESCE(SUM(existencias_act * precio_compra), 0) as total_compra, " +
                     "COALESCE(SUM(existencias_act * precio_venta), 0) as total_venta " +
                     "FROM productos WHERE existencias_act > 0 AND id_sucursal = ?";
        try (Connection c = new Conexion().conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sucursal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resultado.put("totalCompra", rs.getDouble("total_compra"));
                    resultado.put("totalVenta",  rs.getDouble("total_venta"));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return resultado;
    }
}
