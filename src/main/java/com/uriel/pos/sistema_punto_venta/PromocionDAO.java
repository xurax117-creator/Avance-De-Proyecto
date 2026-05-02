package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PromocionDAO {

    public List<PromocionData> obtenerTodas() {
        List<PromocionData> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                SELECT p.id_promocion, p.nombre, p.tipo, p.id_producto, prod.nombre as nombre_producto,
                       p.cantidad_requerida, p.precio_especial, p.descuento_porcentaje, p.descuento_fijo,
                       p.fecha_inicio, p.fecha_fin, p.activo
                FROM promociones p
                JOIN productos prod ON p.id_producto = prod.id_producto
                ORDER BY p.id_promocion DESC
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new PromocionData(
                    rs.getInt("id_promocion"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    rs.getInt("id_producto"),
                    rs.getString("nombre_producto"),
                    rs.getDouble("cantidad_requerida"),
                    rs.getDouble("precio_especial"),
                    rs.getDouble("descuento_porcentaje"),
                    rs.getDouble("descuento_fijo"),
                    rs.getDate("fecha_inicio"),
                    rs.getDate("fecha_fin"),
                    rs.getBoolean("activo")
                ));
            }

            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public PromocionData obtenerPorId(int id) {
        PromocionData data = null;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                SELECT p.id_promocion, p.nombre, p.tipo, p.id_producto, prod.nombre as nombre_producto,
                       p.cantidad_requerida, p.precio_especial, p.descuento_porcentaje, p.descuento_fijo,
                       p.fecha_inicio, p.fecha_fin, p.activo
                FROM promociones p
                JOIN productos prod ON p.id_producto = prod.id_producto
                WHERE p.id_promocion = ?
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                data = new PromocionData(
                    rs.getInt("id_promocion"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    rs.getInt("id_producto"),
                    rs.getString("nombre_producto"),
                    rs.getDouble("cantidad_requerida"),
                    rs.getDouble("precio_especial"),
                    rs.getDouble("descuento_porcentaje"),
                    rs.getDouble("descuento_fijo"),
                    rs.getDate("fecha_inicio"),
                    rs.getDate("fecha_fin"),
                    rs.getBoolean("activo")
                );
            }

            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public boolean crear(PromocionRequest request) {
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                INSERT INTO promociones (nombre, tipo, id_producto, cantidad_requerida, precio_especial,
                                        descuento_porcentaje, descuento_fijo, fecha_inicio, fecha_fin, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, request.nombre);
            stmt.setString(2, request.tipo);
            stmt.setInt(3, request.idProducto);
            stmt.setDouble(4, request.cantidadRequerida);
            stmt.setDouble(5, request.precioEspecial);
            stmt.setDouble(6, request.descuentoPorcentaje);
            stmt.setDouble(7, request.descuentoFijo);
            stmt.setDate(8, request.fechaInicio != null ? java.sql.Date.valueOf(request.fechaInicio) : null);
            stmt.setDate(9, request.fechaFin != null ? java.sql.Date.valueOf(request.fechaFin) : null);
            stmt.setBoolean(10, request.activo);

            int rows = stmt.executeUpdate();
            stmt.close();
            c.close();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizar(int id, PromocionRequest request) {
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                UPDATE promociones SET nombre = ?, tipo = ?, id_producto = ?, cantidad_requerida = ?,
                                       precio_especial = ?, descuento_porcentaje = ?, descuento_fijo = ?,
                                       fecha_inicio = ?, fecha_fin = ?, activo = ?
                WHERE id_promocion = ?
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, request.nombre);
            stmt.setString(2, request.tipo);
            stmt.setInt(3, request.idProducto);
            stmt.setDouble(4, request.cantidadRequerida);
            stmt.setDouble(5, request.precioEspecial);
            stmt.setDouble(6, request.descuentoPorcentaje);
            stmt.setDouble(7, request.descuentoFijo);
            stmt.setDate(8, request.fechaInicio != null ? java.sql.Date.valueOf(request.fechaInicio) : null);
            stmt.setDate(9, request.fechaFin != null ? java.sql.Date.valueOf(request.fechaFin) : null);
            stmt.setBoolean(10, request.activo);
            stmt.setInt(11, id);

            int rows = stmt.executeUpdate();
            stmt.close();
            c.close();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminar(int id) {
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = "DELETE FROM promociones WHERE id_promocion = ?";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            stmt.close();
            c.close();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Método para obtener promociones activas y válidas por producto
    public List<PromocionData> obtenerPromocionesActivasPorProducto(int idProducto) {
        List<PromocionData> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                SELECT id_promocion, nombre, tipo, id_producto, cantidad_requerida, precio_especial,
                       descuento_porcentaje, descuento_fijo, fecha_inicio, fecha_fin, activo
                FROM promociones
                WHERE id_producto = ? AND activo = TRUE
                  AND (fecha_inicio IS NULL OR fecha_inicio <= CURDATE())
                  AND (fecha_fin IS NULL OR fecha_fin >= CURDATE())
                ORDER BY tipo, cantidad_requerida DESC
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new PromocionData(
                    rs.getInt("id_promocion"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    rs.getInt("id_producto"),
                    "", // nombre_producto no necesario aquí
                    rs.getDouble("cantidad_requerida"),
                    rs.getDouble("precio_especial"),
                    rs.getDouble("descuento_porcentaje"),
                    rs.getDouble("descuento_fijo"),
                    rs.getDate("fecha_inicio"),
                    rs.getDate("fecha_fin"),
                    rs.getBoolean("activo")
                ));
            }

            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}

class PromocionData {
    public int idPromocion;
    public String nombre;
    public String tipo;
    public int idProducto;
    public String nombreProducto;
    public double cantidadRequerida;
    public Double precioEspecial; // nullable
    public Double descuentoPorcentaje; // nullable
    public Double descuentoFijo; // nullable
    public java.sql.Date fechaInicio; // nullable
    public java.sql.Date fechaFin; // nullable
    public boolean activo;

    public PromocionData(int idPromocion, String nombre, String tipo, int idProducto, String nombreProducto,
                        double cantidadRequerida, Double precioEspecial, Double descuentoPorcentaje,
                        Double descuentoFijo, java.sql.Date fechaInicio, java.sql.Date fechaFin, boolean activo) {
        this.idPromocion = idPromocion;
        this.nombre = nombre;
        this.tipo = tipo;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidadRequerida = cantidadRequerida;
        this.precioEspecial = precioEspecial;
        this.descuentoPorcentaje = descuentoPorcentaje;
        this.descuentoFijo = descuentoFijo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
    }
}

class PromocionRequest {
    public String nombre;
    public String tipo;
    public int idProducto;
    public double cantidadRequerida;
    public Double precioEspecial;
    public Double descuentoPorcentaje;
    public Double descuentoFijo;
    public String fechaInicio; // formato YYYY-MM-DD
    public String fechaFin; // formato YYYY-MM-DD
    public boolean activo;
}