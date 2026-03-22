package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Venta {

    public ProductoData obtenerDatosProducto(String codigo) {
        ProductoData data = null;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                SELECT id_producto, nombre, precio_venta, existencias_act, foto_producto_blob
                FROM productos 
                WHERE (codigo_barras = ? OR codigo_barras_secundario = ?) AND activo = TRUE
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, codigo);
            stmt.setString(2, codigo);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                String fotoBase64 = fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null;
                
                data = new ProductoData(
                    rs.getInt("id_producto"),
                    rs.getString("nombre"),
                    rs.getDouble("precio_venta"),
                    rs.getInt("existencias_act"),
                    fotoBase64
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

    public int crearVenta(int idUsuario) {
        int idVenta = -1;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "INSERT INTO ventas(id_usuario, total) VALUES(?, 0)";
            PreparedStatement stmt = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                idVenta = rs.getInt(1);
            }

            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idVenta;
    }

    // Método para búsqueda parcial de productos
    public List<ProductoData> buscarProductosParcial(String busqueda) {
        List<ProductoData> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                SELECT id_producto, nombre, precio_venta, existencias_act, foto_producto_blob
                FROM productos 
                WHERE activo = TRUE AND (nombre LIKE ? OR codigo_barras LIKE ?)
                ORDER BY nombre ASC
                LIMIT 20
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, "%" + busqueda + "%");
            stmt.setString(2, "%" + busqueda + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                byte[] fotoBytes = rs.getBytes("foto_producto_blob");
                String fotoBase64 = fotoBytes != null ? Base64.getEncoder().encodeToString(fotoBytes) : null;
                
                lista.add(new ProductoData(
                    rs.getInt("id_producto"),
                    rs.getString("nombre"),
                    rs.getDouble("precio_venta"),
                    rs.getInt("existencias_act"),
                    fotoBase64
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

    public void finalizarTransaccion(int idVenta, List<DetalleVentaRequest> detalles, double totalFinal) throws SQLException {
        Connection c = null;
        try {
            Conexion con = new Conexion();
            c = con.conectar();
            c.setAutoCommit(false); 

            String sqlTotal = "UPDATE ventas SET total = ? WHERE id_venta = ?";
            PreparedStatement stmtTotal = c.prepareStatement(sqlTotal);
            stmtTotal.setDouble(1, totalFinal);
            stmtTotal.setInt(2, idVenta);
            stmtTotal.executeUpdate();
            stmtTotal.close();

            for (DetalleVentaRequest detalle : detalles) {
                String sqlInsert = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
                PreparedStatement stmtInsert = c.prepareStatement(sqlInsert);
                stmtInsert.setInt(1, idVenta);
                stmtInsert.setInt(2, detalle.idProducto);
                stmtInsert.setInt(3, detalle.cantidad);
                stmtInsert.setDouble(4, detalle.precioUnitario);
                stmtInsert.executeUpdate();
                stmtInsert.close();

                String sqlStock = "UPDATE productos SET existencias_act = existencias_act - ? WHERE id_producto = ?";
                PreparedStatement stmtStock = c.prepareStatement(sqlStock);
                stmtStock.setInt(1, detalle.cantidad);
                stmtStock.setInt(2, detalle.idProducto);
                stmtStock.executeUpdate();
                stmtStock.close();
            }

            c.commit(); 
        } catch (SQLException e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) {
                c.setAutoCommit(true);
                c.close();
            }
        }
    }
}

class ProductoData {
    public int idProducto;
    public String nombre;
    public double precioVenta;
    public int stockActual;
    public String fotoProducto;

    public ProductoData(int idProducto, String nombre, double precioVenta, int stockActual, String fotoProducto) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.fotoProducto = fotoProducto;
    }
}

class DetalleVentaRequest {
    public int idProducto;
    public int cantidad;
    public double precioUnitario;
}

// Nueva clase para lista de productos
class ProductoDataList {
    public List<ProductoData> productos;
    
    public ProductoDataList(List<ProductoData> productos) {
        this.productos = productos;
    }
}
