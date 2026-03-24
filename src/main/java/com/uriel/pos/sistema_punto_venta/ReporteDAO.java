package com.uriel.pos.sistema_punto_venta;

import java.sql.*;
import java.util.*;

public class ReporteDAO {
    
    public List<Map<String, Object>> obtenerVentas(String inicio, String fin, int pagina, int tamanoPagina) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            // Primero obtener el total de ventas para la paginación
            String sqlCount = "SELECT COUNT(*) as total FROM ventas WHERE DATE(fecha) BETWEEN ? AND ?";
            PreparedStatement psCount = c.prepareStatement(sqlCount);
            psCount.setString(1, inicio);
            psCount.setString(2, fin);
            ResultSet rsCount = psCount.executeQuery();
            int totalRegistros = 0;
            if (rsCount.next()) {
                totalRegistros = rsCount.getInt("total");
            }
            
            // Obtener el total general de ventas en el período
            double totalGeneral = 0;
            String sqlSum = "SELECT COALESCE(SUM(total), 0) as total_general FROM ventas WHERE DATE(fecha) BETWEEN ? AND ?";
            PreparedStatement psSum = c.prepareStatement(sqlSum);
            psSum.setString(1, inicio);
            psSum.setString(2, fin);
            ResultSet rsSum = psSum.executeQuery();
            if (rsSum.next()) {
                totalGeneral = rsSum.getDouble("total_general");
            }
            
            // Calcular el offset
            int offset = (pagina - 1) * tamanoPagina;
            
            // Query con número de venta secuencial usando ROW_NUMBER
            String sql = "SELECT * FROM (" +
                         "SELECT v.id_venta, v.fecha, u.nombre_completo as nombre, v.total, " +
                         "ROW_NUMBER() OVER (ORDER BY v.id_venta DESC) as numero_venta " +
                         "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                         "WHERE DATE(v.fecha) BETWEEN ? AND ?" +
                         ") as subquery WHERE numero_venta BETWEEN ? AND ?";
            
            // Para MySQL 8+, usamos la versión anterior. Para versiones anteriores, usamos una alternativa
            // Vamos a usar una subconsulta para el número
            String sqlAlternativo = "SELECT v.id_venta, v.fecha, u.nombre_completo as nombre, v.total, " +
                         "(SELECT COUNT(*) + 1 FROM ventas v2 WHERE v2.id_venta > v.id_venta AND DATE(v2.fecha) BETWEEN ? AND ?) as numero_venta " +
                         "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                         "WHERE DATE(v.fecha) BETWEEN ? AND ? ORDER BY v.id_venta DESC LIMIT ? OFFSET ?";
            
            PreparedStatement ps = c.prepareStatement(sqlAlternativo);
            ps.setString(1, inicio);
            ps.setString(2, fin);
            ps.setString(3, inicio);
            ps.setString(4, fin);
            ps.setInt(5, tamanoPagina);
            ps.setInt(6, offset);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("idVenta", rs.getInt("id_venta"));
                map.put("numeroVenta", rs.getInt("numero_venta"));
                map.put("fecha", rs.getTimestamp("fecha").toString());
                map.put("nombre", rs.getString("nombre"));
                map.put("total", rs.getDouble("total"));
                lista.add(map);
            }
            
            // Agregar información de paginación al último elemento
            if (!lista.isEmpty()) {
                Map<String, Object> pagInfo = new HashMap<>();
                pagInfo.put("totalRegistros", totalRegistros);
                pagInfo.put("totalGeneral", totalGeneral);
                pagInfo.put("paginaActual", pagina);
                pagInfo.put("tamanoPagina", tamanoPagina);
                pagInfo.put("totalPaginas", (int) Math.ceil((double) totalRegistros / tamanoPagina));
                lista.add(pagInfo);
            }
            
            c.close();
        } catch(Exception e) { e.printStackTrace(); }
        return lista;
    }
    
    // Método sin paginación (para compatibilidad)
    public List<Map<String, Object>> obtenerVentas(String inicio, String fin) {
        return obtenerVentas(inicio, fin, 1, 1000);
    }

    public List<Map<String, Object>> obtenerDetalleVenta(int idVenta) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT p.nombre, dv.cantidad, dv.precio_unitario, dv.subtotal " +
                         "FROM detalle_venta dv JOIN productos p ON dv.id_producto = p.id_producto " +
                         "WHERE dv.id_venta = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("producto", rs.getString("nombre"));
                map.put("cantidad", rs.getInt("cantidad"));
                map.put("precio", rs.getDouble("precio_unitario"));
                map.put("subtotal", rs.getDouble("subtotal"));
                lista.add(map);
            }
            c.close();
        } catch(Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Map<String, Object>> obtenerTopProductos(String inicio, String fin) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT p.nombre, SUM(dv.cantidad) as cant, SUM(dv.cantidad * dv.precio_unitario) as total " +
                         "FROM detalle_venta dv JOIN productos p ON dv.id_producto = p.id_producto " +
                         "JOIN ventas v ON dv.id_venta = v.id_venta " +
                         "WHERE DATE(v.fecha) BETWEEN ? AND ? " +
                         "GROUP BY p.id_producto ORDER BY cant DESC LIMIT 10";
            
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, inicio);
            ps.setString(2, fin);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("nombre", rs.getString("nombre"));
                map.put("cantidad", rs.getDouble("cant"));
                map.put("total", rs.getDouble("total"));
                lista.add(map);
            }
            c.close();
        } catch(Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Map<String, Object>> obtenerVentasPorCajero(String inicio, String fin) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT u.nombre_completo as nombre, COUNT(v.id_venta) as num_ventas, SUM(v.total) as total_vendido " +
                         "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                         "WHERE DATE(v.fecha) BETWEEN ? AND ? " +
                         "GROUP BY u.id_usuario ORDER BY total_vendido DESC";
            
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, inicio);
            ps.setString(2, fin);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("nombre", rs.getString("nombre"));
                map.put("numVentas", rs.getInt("num_ventas"));
                map.put("total", rs.getDouble("total_vendido"));
                lista.add(map);
            }
            c.close();
        } catch(Exception e) { e.printStackTrace(); }
        return lista;
    }
}