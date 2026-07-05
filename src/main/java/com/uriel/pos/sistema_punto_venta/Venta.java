package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Venta {

    public ProductoData obtenerDatosProducto(String codigo, int idSucursal) {
        ProductoData data = null;
        String sql = """
            SELECT id_producto, nombre, precio_venta, existencias_act, foto_producto_blob, codigo_barras
            FROM productos
            WHERE (codigo_barras = ? OR codigo_barras_secundario = ?) AND activo = TRUE AND id_sucursal = ?
        """;
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            stmt.setString(2, codigo);
            stmt.setInt(3, idSucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                    data = new ProductoData(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getDouble("precio_venta"),
                        rs.getDouble("existencias_act"),
                        fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null,
                        rs.getString("codigo_barras")
                    );
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    public int crearVenta(int idUsuario, int idSucursal) {
        int idVenta = -1;
        String sql = "INSERT INTO ventas(id_usuario, total, fecha, id_sucursal) VALUES(?, 0, CONVERT_TZ(NOW(), '+02:00', '-04:00'), ?)";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idSucursal);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) idVenta = rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return idVenta;
    }

    public List<ProductoData> buscarProductosParcial(String busqueda, int idSucursal) {
        List<ProductoData> lista = new ArrayList<>();
        String sql = """
            SELECT id_producto, nombre, precio_venta, existencias_act, foto_producto_blob, codigo_barras
            FROM productos
            WHERE activo = TRUE AND (nombre LIKE ? OR codigo_barras LIKE ?) AND id_sucursal = ?
            ORDER BY nombre ASC LIMIT 20
        """;
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, "%" + busqueda + "%");
            stmt.setString(2, "%" + busqueda + "%");
            stmt.setInt(3, idSucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                    lista.add(new ProductoData(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getDouble("precio_venta"),
                        rs.getDouble("existencias_act"),
                        fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null,
                        rs.getString("codigo_barras")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public void finalizarTransaccion(int idVenta, List<DetalleVentaRequest> detalles, double totalFinal, int idSucursal, double montoEfectivo, double montoTarjeta) throws SQLException {
        Connection c = null;
        try {
            c = new Conexion().conectar();
            c.setAutoCommit(false);

            int ventaId = idVenta;
            if (idVenta == -1) {
                String sqlInsertVenta = "INSERT INTO ventas (id_usuario, total, fecha, id_sucursal) VALUES (?, ?, CONVERT_TZ(NOW(), '+02:00', '-04:00'), ?)";
                try (PreparedStatement stmtVenta = c.prepareStatement(sqlInsertVenta, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    stmtVenta.setInt(1, 1);
                    stmtVenta.setDouble(2, totalFinal);
                    stmtVenta.setInt(3, idSucursal);
                    stmtVenta.executeUpdate();
                    try (ResultSet rsVenta = stmtVenta.getGeneratedKeys()) {
                        if (rsVenta.next()) ventaId = rsVenta.getInt(1);
                    }
                }
            }

            try (PreparedStatement stmtTotal = c.prepareStatement("UPDATE ventas SET total = ?, monto_efectivo = ?, monto_tarjeta = ? WHERE id_venta = ?")) {
                stmtTotal.setDouble(1, totalFinal);
                stmtTotal.setDouble(2, montoEfectivo);
                stmtTotal.setDouble(3, montoTarjeta);
                stmtTotal.setInt(4, ventaId);
                stmtTotal.executeUpdate();
            }

            String sqlInsert = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
            String sqlStock  = "UPDATE productos SET existencias_act = existencias_act - ? WHERE id_producto = ?";
            try (PreparedStatement stmtInsert = c.prepareStatement(sqlInsert);
                 PreparedStatement stmtStock  = c.prepareStatement(sqlStock)) {
                for (DetalleVentaRequest detalle : detalles) {
                    stmtInsert.setInt(1, ventaId);
                    stmtInsert.setInt(2, detalle.idProducto);
                    stmtInsert.setDouble(3, detalle.cantidad);
                    stmtInsert.setDouble(4, detalle.precioUnitario);
                    stmtInsert.executeUpdate();

                    stmtStock.setDouble(1, detalle.cantidad);
                    stmtStock.setInt(2, detalle.idProducto);
                    stmtStock.executeUpdate();
                }
            }

            c.commit();
        } catch (SQLException e) {
            if (c != null) c.rollback();
            throw e;
        } catch (Exception e) {
            if (c != null) { try { c.rollback(); } catch (Exception ex) {} }
            throw new SQLException(e.getMessage());
        } finally {
            if (c != null) { c.setAutoCommit(true); c.close(); }
        }
    }

    public int guardarVentaEnEspera(int idUsuario, double total, List<DetalleVentaEnEspera> detalles, int idSucursal) {
        int idVentaEspera = -1;
        try (Connection c = new Conexion().conectar()) {
            String sql = "INSERT INTO ventas_en_espera (id_usuario, total, fecha, id_sucursal) VALUES (?, ?, CONVERT_TZ(NOW(), '+02:00', '-04:00'), ?)";
            try (PreparedStatement stmt = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, idUsuario);
                stmt.setDouble(2, total);
                stmt.setInt(3, idSucursal);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) idVentaEspera = rs.getInt(1);
                }
            }

            String sqlDetalle = "INSERT INTO detalles_venta_en_espera (id_venta_espera, id_producto, codigo, nombre, precio, cantidad, foto_producto) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmtDetalle = c.prepareStatement(sqlDetalle)) {
                for (DetalleVentaEnEspera detalle : detalles) {
                    stmtDetalle.setInt(1, idVentaEspera);
                    stmtDetalle.setInt(2, detalle.idProducto);
                    stmtDetalle.setString(3, detalle.codigo);
                    stmtDetalle.setString(4, detalle.nombre);
                    stmtDetalle.setDouble(5, detalle.precio);
                    stmtDetalle.setDouble(6, detalle.cantidad);
                    stmtDetalle.setString(7, detalle.fotoProducto);
                    stmtDetalle.executeUpdate();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return idVentaEspera;
    }

    // N+1 resuelto: 2 queries en total en lugar de 1 + N
    public List<VentaEnEspera> obtenerVentasEnEspera(int idSucursal) {
        List<VentaEnEspera> lista = new ArrayList<>();
        try (Connection c = new Conexion().conectar()) {
            Map<Integer, VentaEnEspera> ventaMap = new LinkedHashMap<>();
            try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT id_venta_espera, id_usuario, total, fecha FROM ventas_en_espera WHERE id_sucursal = ? ORDER BY fecha DESC")) {
                stmt.setInt(1, idSucursal);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        VentaEnEspera venta = new VentaEnEspera(
                            rs.getInt("id_venta_espera"),
                            rs.getInt("id_usuario"),
                            "Usuario #" + rs.getInt("id_usuario"),
                            rs.getString("fecha"),
                            rs.getDouble("total")
                        );
                        ventaMap.put(venta.id, venta);
                        lista.add(venta);
                    }
                }
            }

            if (!ventaMap.isEmpty()) {
                String ids = ventaMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
                String sqlDet = "SELECT id_venta_espera, id_producto, codigo, nombre, precio, cantidad, foto_producto " +
                                "FROM detalles_venta_en_espera WHERE id_venta_espera IN (" + ids + ")";
                try (PreparedStatement stmtDet = c.prepareStatement(sqlDet);
                     ResultSet rsDet = stmtDet.executeQuery()) {
                    while (rsDet.next()) {
                        VentaEnEspera venta = ventaMap.get(rsDet.getInt("id_venta_espera"));
                        if (venta != null) {
                            venta.detalles.add(new DetalleVentaEnEspera(
                                rsDet.getInt("id_producto"),
                                rsDet.getString("codigo"),
                                rsDet.getString("nombre"),
                                rsDet.getDouble("precio"),
                                rsDet.getDouble("cantidad"),
                                rsDet.getString("foto_producto")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public VentaEnEspera obtenerVentaEnEspera(int idVentaEspera) {
        VentaEnEspera venta = null;
        try (Connection c = new Conexion().conectar()) {
            try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT id_venta_espera, id_usuario, total, fecha FROM ventas_en_espera WHERE id_venta_espera = ?")) {
                stmt.setInt(1, idVentaEspera);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        venta = new VentaEnEspera(
                            rs.getInt("id_venta_espera"),
                            rs.getInt("id_usuario"),
                            "Usuario #" + rs.getInt("id_usuario"),
                            rs.getString("fecha"),
                            rs.getDouble("total")
                        );
                    }
                }
            }

            if (venta != null) {
                try (PreparedStatement stmtDet = c.prepareStatement(
                        "SELECT id_producto, codigo, nombre, precio, cantidad, foto_producto FROM detalles_venta_en_espera WHERE id_venta_espera = ?")) {
                    stmtDet.setInt(1, idVentaEspera);
                    try (ResultSet rsDet = stmtDet.executeQuery()) {
                        while (rsDet.next()) {
                            venta.detalles.add(new DetalleVentaEnEspera(
                                rsDet.getInt("id_producto"),
                                rsDet.getString("codigo"),
                                rsDet.getString("nombre"),
                                rsDet.getDouble("precio"),
                                rsDet.getDouble("cantidad"),
                                rsDet.getString("foto_producto")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return venta;
    }

    public boolean eliminarVentaEnEspera(int idVentaEspera) {
        try (Connection c = new Conexion().conectar()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM detalles_venta_en_espera WHERE id_venta_espera = ?")) {
                ps.setInt(1, idVentaEspera); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM ventas_en_espera WHERE id_venta_espera = ?")) {
                ps.setInt(1, idVentaEspera); ps.executeUpdate();
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}

class ProductoData {
    public int idProducto;
    public String nombre;
    public double precioVenta;
    public double stockActual;
    public String fotoProducto;
    public String codigoBarras;

    public ProductoData(int idProducto, String nombre, double precioVenta, double stockActual, String fotoProducto, String codigoBarras) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.fotoProducto = fotoProducto;
        this.codigoBarras = codigoBarras;
    }
}

class DetalleVentaRequest {
    public int idProducto;
    public double cantidad;
    public double precioUnitario;
}

class VentaEnEspera {
    public int id;
    public int idUsuario;
    public String nombreUsuario;
    public String fecha;
    public double total;
    public List<DetalleVentaEnEspera> detalles;

    public VentaEnEspera(int id, int idUsuario, String nombreUsuario, String fecha, double total) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.fecha = fecha;
        this.total = total;
        this.detalles = new ArrayList<>();
    }
}

class DetalleVentaEnEspera {
    public int idProducto;
    public String codigo;
    public String nombre;
    public double precio;
    public double cantidad;
    public String fotoProducto;

    public DetalleVentaEnEspera(int idProducto, String codigo, String nombre, double precio, double cantidad, String fotoProducto) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.fotoProducto = fotoProducto;
    }
}
