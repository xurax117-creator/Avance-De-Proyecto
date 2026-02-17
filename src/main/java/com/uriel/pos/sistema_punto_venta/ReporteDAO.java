package com.uriel.pos.sistema_punto_venta;

import java.sql.*;
import java.util.*;

public class ReporteDAO {
    
    public List<Map<String, Object>> obtenerVentas(String inicio, String fin) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            String sql = "SELECT v.id_venta, v.fecha, u.nombre_completo as nombre, v.total " +
                         "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                         "WHERE DATE(v.fecha) BETWEEN ? AND ? ORDER BY v.id_venta DESC";
            
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, inicio);
            ps.setString(2, fin);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("idVenta", rs.getInt("id_venta"));
                map.put("fecha", rs.getTimestamp("fecha").toString());
                map.put("nombre", rs.getString("nombre"));
                map.put("total", rs.getDouble("total"));
                lista.add(map);
            }
            c.close();
        } catch(Exception e) { e.printStackTrace(); }
        return lista;
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
                map.put("cantidad", rs.getInt("cant"));
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