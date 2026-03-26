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
    public int guardarVentaEnEspera(String nombreVenta, int idUsuario, double total, String detallesJson) {
        int id = -1;
        Connection c = null;
        try {
            Conexion con = new Conexion();
            c = con.conectar();
            
            System.out.println("Intentando guardar venta en espera:");
            System.out.println("nombreVenta: " + nombreVenta);
            System.out.println("idUsuario: " + idUsuario);
            System.out.println("total: " + total);
            System.out.println("detallesJson length: " + (detallesJson != null ? detallesJson.length() : 0));
            
            String sql = "INSERT INTO ventas_en_espera (nombre_venta, id_usuario, total, detalles) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, nombreVenta);
            stmt.setInt(2, idUsuario);
            stmt.setDouble(3, total);
            stmt.setString(4, detallesJson);
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
                System.out.println("Generated ID: " + id);
            }

            rs.close();
            stmt.close();
        } catch (Exception e) {
            System.out.println("Error en guardarVentaEnEspera: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (c != null) c.close(); } catch (Exception e) {}
        }
        return id;
    }
    
    // Obtener todas las ventas en espera
    public List<VentaEnEspera> obtenerVentasEnEspera() {
        List<VentaEnEspera> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = "SELECT id_venta_espera, nombre_venta, fecha, id_usuario, total, detalles FROM ventas_en_espera ORDER BY fecha DESC";
            PreparedStatement stmt = c.prepareStatement(sql);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String detallesJson = rs.getString("detalles");
                List<DetalleVenta> detalles = parsearDetalles(detallesJson);
                
                lista.add(new VentaEnEspera(
                    rs.getInt("id_venta_espera"),
                    rs.getString("nombre_venta"),
                    rs.getString("fecha"),
                    rs.getInt("id_usuario"),
                    rs.getDouble("total"),
                    detalles
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
    
    // Parsear detalles desde JSON
    private List<DetalleVenta> parsearDetalles(String json) {
        List<DetalleVenta> lista = new ArrayList<>();
        try {
            // Simple JSON parsing without library
            json = json.trim();
            if (json.startsWith("[")) {
                json = json.substring(1, json.length() - 1);
            }
            
            // Split by },{" to get individual objects
            String[] objetos = json.split("\\},\\s*\\{");
            for (String obj : objetos) {
                obj = obj.replace("{", "").replace("}", "");
                
                int idProducto = 0;
                String codigo = "";
                String nombre = "";
                double precio = 0;
                int cantidad = 0;
                
                String[] campos = obj.split(",");
                for (String campo : campos) {
                    String[] kv = campo.split(":");
                    if (kv.length >= 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        
                        switch (key) {
                            case "idProducto":
                            case "id_producto":
                                idProducto = Integer.parseInt(value);
                                break;
                            case "codigo":
                            case "codigo_barras":
                                codigo = value;
                                break;
                            case "nombre":
                                nombre = value;
                                break;
                            case "precio":
                            case "precio_venta":
                            case "precioUnitario":
                                precio = Double.parseDouble(value);
                                break;
                            case "cantidad":
                                cantidad = Integer.parseInt(value);
                                break;
                        }
                    }
                }
                
                if (idProducto > 0) {
                    lista.add(new DetalleVenta(idProducto, codigo, nombre, precio, cantidad));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    // Eliminar venta en espera
    public void eliminarVentaEnEspera(int id) {
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            
            String sql = "DELETE FROM ventas_en_espera WHERE id_venta_espera = ?";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
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

// Nueva clase para lista de productos
class ProductoDataList {
    public List<ProductoData> productos;
    
    public ProductoDataList(List<ProductoData> productos) {
        this.productos = productos;
    }
}

// Clase para ventas en espera
class VentaEnEspera {
    public int idVentaEspera;
    public String nombreVenta;
    public String fecha;
    public int idUsuario;
    public double total;
    public List<DetalleVenta> detalles;
    
    public VentaEnEspera(int idVentaEspera, String nombreVenta, String fecha, int idUsuario, double total, List<DetalleVenta> detalles) {
        this.idVentaEspera = idVentaEspera;
        this.nombreVenta = nombreVenta;
        this.fecha = fecha;
        this.idUsuario = idUsuario;
        this.total = total;
        this.detalles = detalles;
    }
}

class DetalleVenta {
    public int idProducto;
    public String codigo;
    public String nombre;
    public double precio;
    public int cantidad;
    
    public DetalleVenta(int idProducto, String codigo, String nombre, double precio, int cantidad) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
    }
}
