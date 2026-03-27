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
                SELECT id_producto, nombre, precio_venta, existencias_act, foto_producto_blob, codigo_barras
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
                    fotoBase64,
                    rs.getString("codigo_barras")
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
                SELECT id_producto, nombre, precio_venta, existencias_act, foto_producto_blob, codigo_barras
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
                    fotoBase64,
                    rs.getString("codigo_barras")
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
    
    // Guardar venta en espera
    public int guardarVentaEnEspera(int idUsuario, double total, List<DetalleVentaEnEspera> detalles) {
        int idVentaEspera = -1;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "INSERT INTO ventas_en_espera (id_usuario, total, fecha) VALUES (?, ?, NOW())";
            PreparedStatement stmt = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, idUsuario);
            stmt.setDouble(2, total);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                idVentaEspera = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            
            // Guardar detalles
            for (DetalleVentaEnEspera detalle : detalles) {
                String sqlDetalle = "INSERT INTO detalles_venta_en_espera (id_venta_espera, id_producto, codigo, nombre, precio, cantidad, foto_producto) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement stmtDetalle = c.prepareStatement(sqlDetalle);
                stmtDetalle.setInt(1, idVentaEspera);
                stmtDetalle.setInt(2, detalle.idProducto);
                stmtDetalle.setString(3, detalle.codigo);
                stmtDetalle.setString(4, detalle.nombre);
                stmtDetalle.setDouble(5, detalle.precio);
                stmtDetalle.setInt(6, detalle.cantidad);
                stmtDetalle.setString(7, detalle.fotoProducto);
                stmtDetalle.executeUpdate();
                stmtDetalle.close();
            }
            
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idVentaEspera;
    }
    
    // Obtener todas las ventas en espera
    public List<VentaEnEspera> obtenerVentasEnEspera() {
        List<VentaEnEspera> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = """
                SELECT v.id_venta_espera, v.id_usuario, v.total, v.fecha, u.nombre as nombre_usuario
                FROM ventas_en_espera v
                LEFT JOIN usuarios u ON v.id_usuario = u.id_usuario
                ORDER BY v.fecha DESC
            """;
            PreparedStatement stmt = c.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                VentaEnEspera venta = new VentaEnEspera(
                    rs.getInt("id_venta_espera"),
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_usuario") != null ? rs.getString("nombre_usuario") : "Usuario #" + rs.getInt("id_usuario"),
                    rs.getString("fecha"),
                    rs.getDouble("total")
                );
                lista.add(venta);
            }
            
            rs.close();
            stmt.close();
            
            // Obtener detalles para cada venta
            for (VentaEnEspera venta : lista) {
                String sqlDetalles = "SELECT * FROM detalles_venta_en_espera WHERE id_venta_espera = ?";
                PreparedStatement stmtDetalles = c.prepareStatement(sqlDetalles);
                stmtDetalles.setInt(1, venta.id);
                ResultSet rsDetalles = stmtDetalles.executeQuery();
                
                while (rsDetalles.next()) {
                    DetalleVentaEnEspera detalle = new DetalleVentaEnEspera(
                        rsDetalles.getInt("id_producto"),
                        rsDetalles.getString("codigo"),
                        rsDetalles.getString("nombre"),
                        rsDetalles.getDouble("precio"),
                        rsDetalles.getInt("cantidad"),
                        rsDetalles.getString("foto_producto")
                    );
                    venta.detalles.add(detalle);
                }
                rsDetalles.close();
                stmtDetalles.close();
            }
            
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    // Obtener una venta en espera por ID
    public VentaEnEspera obtenerVentaEnEspera(int idVentaEspera) {
        VentaEnEspera venta = null;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = """
                SELECT v.id_venta_espera, v.id_usuario, v.total, v.fecha, u.nombre as nombre_usuario
                FROM ventas_en_espera v
                LEFT JOIN usuarios u ON v.id_usuario = u.id_usuario
                WHERE v.id_venta_espera = ?
            """;
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, idVentaEspera);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                venta = new VentaEnEspera(
                    rs.getInt("id_venta_espera"),
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_usuario") != null ? rs.getString("nombre_usuario") : "Usuario #" + rs.getInt("id_usuario"),
                    rs.getString("fecha"),
                    rs.getDouble("total")
                );
            }
            rs.close();
            stmt.close();
            
            if (venta != null) {
                String sqlDetalles = "SELECT * FROM detalles_venta_en_espera WHERE id_venta_espera = ?";
                PreparedStatement stmtDetalles = c.prepareStatement(sqlDetalles);
                stmtDetalles.setInt(1, idVentaEspera);
                ResultSet rsDetalles = stmtDetalles.executeQuery();
                
                while (rsDetalles.next()) {
                    DetalleVentaEnEspera detalle = new DetalleVentaEnEspera(
                        rsDetalles.getInt("id_producto"),
                        rsDetalles.getString("codigo"),
                        rsDetalles.getString("nombre"),
                        rsDetalles.getDouble("precio"),
                        rsDetalles.getInt("cantidad"),
                        rsDetalles.getString("foto_producto")
                    );
                    venta.detalles.add(detalle);
                }
                rsDetalles.close();
                stmtDetalles.close();
            }
            
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return venta;
    }
    
    // Eliminar venta en espera
    public boolean eliminarVentaEnEspera(int idVentaEspera) {
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            // Eliminar detalles primero
            String sqlDetalles = "DELETE FROM detalles_venta_en_espera WHERE id_venta_espera = ?";
            PreparedStatement stmtDetalles = c.prepareStatement(sqlDetalles);
            stmtDetalles.setInt(1, idVentaEspera);
            stmtDetalles.executeUpdate();
            stmtDetalles.close();
            
            // Eliminar venta
            String sql = "DELETE FROM ventas_en_espera WHERE id_venta_espera = ?";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, idVentaEspera);
            stmt.executeUpdate();
            stmt.close();
            
            c.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

class ProductoData {
    public int idProducto;
    public String nombre;
    public double precioVenta;
    public int stockActual;
    public String fotoProducto;
    public String codigoBarras;

    public ProductoData(int idProducto, String nombre, double precioVenta, int stockActual, String fotoProducto, String codigoBarras) {
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
    public int cantidad;
    public double precioUnitario;
}

// Clase para venta en espera
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
    public int cantidad;
    public String fotoProducto;
    
    public DetalleVentaEnEspera(int idProducto, String codigo, String nombre, double precio, int cantidad, String fotoProducto) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.fotoProducto = fotoProducto;
    }
}
