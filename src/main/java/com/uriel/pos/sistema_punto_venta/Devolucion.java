package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Devolucion {

    public Map<String, Object> obtenerVentaParaDevolucion(int idVenta) {
        Map<String, Object> resultado = new HashMap<>();
        try (Connection c = new Conexion().conectar()) {

            String sqlVenta = "SELECT v.id_venta, v.fecha, v.total, v.total_devuelto, u.nombre_completo AS nombre_cajero " +
                               "FROM ventas v JOIN usuarios u ON v.id_usuario = u.id_usuario WHERE v.id_venta = ?";
            try (PreparedStatement ps = c.prepareStatement(sqlVenta)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    resultado.put("idVenta", rs.getInt("id_venta"));
                    resultado.put("fecha", rs.getString("fecha"));
                    resultado.put("total", rs.getDouble("total"));
                    resultado.put("totalDevuelto", rs.getDouble("total_devuelto"));
                    resultado.put("nombreCajero", rs.getString("nombre_cajero"));
                }
            }

            List<Map<String, Object>> items = new ArrayList<>();
            String sqlItems = "SELECT dv.id_detalle, dv.id_producto, p.nombre, p.codigo_barras, p.foto_producto_blob, " +
                               "dv.cantidad AS cantidad_original, dv.precio_unitario, " +
                               "COALESCE(SUM(dd.cantidad), 0) AS cantidad_devuelta " +
                               "FROM detalle_venta dv " +
                               "JOIN productos p ON dv.id_producto = p.id_producto " +
                               "LEFT JOIN detalle_devolucion dd ON dd.id_detalle_venta = dv.id_detalle " +
                               "WHERE dv.id_venta = ? " +
                               "GROUP BY dv.id_detalle, dv.id_producto, p.nombre, p.codigo_barras, p.foto_producto_blob, dv.cantidad, dv.precio_unitario " +
                               "ORDER BY dv.id_detalle";
            try (PreparedStatement ps = c.prepareStatement(sqlItems)) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double cantidadOriginal = rs.getDouble("cantidad_original");
                        double cantidadDevuelta = rs.getDouble("cantidad_devuelta");
                        double disponible = cantidadOriginal - cantidadDevuelta;
                        if (disponible <= 0) continue;

                        byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                        Map<String, Object> item = new HashMap<>();
                        item.put("idDetalleVenta", rs.getInt("id_detalle"));
                        item.put("idProducto", rs.getInt("id_producto"));
                        item.put("nombre", rs.getString("nombre"));
                        item.put("codigo", rs.getString("codigo_barras"));
                        item.put("fotoProducto", fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null);
                        item.put("precioUnitario", rs.getDouble("precio_unitario"));
                        item.put("cantidadDisponible", disponible);
                        items.add(item);
                    }
                }
            }
            resultado.put("items", items);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return resultado;
    }

    public void procesarDevolucion(int idVenta, List<ItemDevolucionRequest> items, int idUsuario, int idSucursal) throws Exception {
        Connection c = null;
        try {
            c = new Conexion().conectar();
            c.setAutoCommit(false);

            int idDevolucion;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO devoluciones (id_venta, id_usuario, total_devuelto, id_sucursal) VALUES (?, ?, 0, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idVenta);
                ps.setInt(2, idUsuario);
                ps.setInt(3, idSucursal);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new Exception("No se pudo crear la devolución.");
                    idDevolucion = rs.getInt(1);
                }
            }

            double totalDevuelto = 0;
            try (PreparedStatement psCheck = c.prepareStatement(
                     "SELECT dv.cantidad AS cantidad_original, dv.id_producto, dv.precio_unitario, " +
                     "COALESCE((SELECT SUM(dd.cantidad) FROM detalle_devolucion dd WHERE dd.id_detalle_venta = dv.id_detalle), 0) AS cantidad_devuelta " +
                     "FROM detalle_venta dv WHERE dv.id_detalle = ? AND dv.id_venta = ?");
                 PreparedStatement psInsertDet = c.prepareStatement(
                     "INSERT INTO detalle_devolucion (id_devolucion, id_detalle_venta, id_producto, cantidad, precio_unitario) VALUES (?, ?, ?, ?, ?)");
                 PreparedStatement psStock = c.prepareStatement(
                     "UPDATE productos SET existencias_act = existencias_act + ? WHERE id_producto = ?")) {

                for (ItemDevolucionRequest item : items) {
                    if (item.cantidad <= 0) continue;

                    psCheck.setInt(1, item.idDetalleVenta);
                    psCheck.setInt(2, idVenta);
                    double cantidadOriginal, precioUnitario, cantidadDevueltaPrevia;
                    int idProducto;
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (!rs.next()) throw new Exception("Uno de los productos no pertenece a esta venta.");
                        cantidadOriginal = rs.getDouble("cantidad_original");
                        idProducto = rs.getInt("id_producto");
                        precioUnitario = rs.getDouble("precio_unitario");
                        cantidadDevueltaPrevia = rs.getDouble("cantidad_devuelta");
                    }

                    double disponible = cantidadOriginal - cantidadDevueltaPrevia;
                    if (item.cantidad > disponible + 0.0001) {
                        throw new Exception("La cantidad a devolver excede lo disponible para uno de los productos.");
                    }

                    psInsertDet.setInt(1, idDevolucion);
                    psInsertDet.setInt(2, item.idDetalleVenta);
                    psInsertDet.setInt(3, idProducto);
                    psInsertDet.setDouble(4, item.cantidad);
                    psInsertDet.setDouble(5, precioUnitario);
                    psInsertDet.executeUpdate();

                    psStock.setDouble(1, item.cantidad);
                    psStock.setInt(2, idProducto);
                    psStock.executeUpdate();

                    totalDevuelto += item.cantidad * precioUnitario;
                }
            }

            if (totalDevuelto <= 0) {
                throw new Exception("No se especificó ninguna cantidad válida a devolver.");
            }

            try (PreparedStatement ps = c.prepareStatement("UPDATE devoluciones SET total_devuelto = ? WHERE id_devolucion = ?")) {
                ps.setDouble(1, totalDevuelto);
                ps.setInt(2, idDevolucion);
                ps.executeUpdate();
            }

            double montoEfectivo, montoTarjeta, totalDevueltoPrevio;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT monto_efectivo, monto_tarjeta, total_devuelto FROM ventas WHERE id_venta = ?")) {
                ps.setInt(1, idVenta);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new Exception("Venta no encontrada.");
                    montoEfectivo = rs.getDouble("monto_efectivo");
                    montoTarjeta = rs.getDouble("monto_tarjeta");
                    totalDevueltoPrevio = rs.getDouble("total_devuelto");
                }
            }

            // El reembolso se reparte proporcionalmente entre efectivo y tarjeta según cómo se pagó
            // originalmente, para que el corte de caja (efectivo/tarjeta) del día siga cuadrando.
            double sumaPagos = montoEfectivo + montoTarjeta;
            double propEfectivo = sumaPagos > 0 ? montoEfectivo / sumaPagos : 1.0;
            double reduccionEfectivo = totalDevuelto * propEfectivo;
            double reduccionTarjeta = totalDevuelto - reduccionEfectivo;

            double nuevoEfectivo = Math.max(0, montoEfectivo - reduccionEfectivo);
            double nuevoTarjeta = Math.max(0, montoTarjeta - reduccionTarjeta);
            double nuevoTotalDevuelto = totalDevueltoPrevio + totalDevuelto;

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE ventas SET total_devuelto = ?, monto_efectivo = ?, monto_tarjeta = ? WHERE id_venta = ?")) {
                ps.setDouble(1, nuevoTotalDevuelto);
                ps.setDouble(2, nuevoEfectivo);
                ps.setDouble(3, nuevoTarjeta);
                ps.setInt(4, idVenta);
                ps.executeUpdate();
            }

            c.commit();
        } catch (Exception e) {
            if (c != null) { try { c.rollback(); } catch (Exception ex) {} }
            throw e;
        } finally {
            if (c != null) { try { c.setAutoCommit(true); c.close(); } catch (Exception ex) {} }
        }
    }
}

class ItemDevolucionRequest {
    public int idDetalleVenta;
    public double cantidad;
}
