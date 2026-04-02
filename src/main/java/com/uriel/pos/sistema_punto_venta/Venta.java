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
            
            String sql = "INSERT INTO ventas(id_usuario, total, fecha) VALUES(?, 0, NOW())";
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

            // Si idVenta es -1, significa que es una venta recuperada del espera y no existe en la tabla ventas
            // Debemo crear una nueva venta primero
            int ventaId = idVenta;
            if (idVenta == -1) {
                // Crear nueva venta
                String sqlInsertVenta = "INSERT INTO ventas (id_usuario, total, fecha) VALUES (?, ?, NOW())";
                PreparedStatement stmtVenta = c.prepareStatement(sqlInsertVenta, PreparedStatement.RETURN_GENERATED_KEYS);
                stmtVenta.setInt(1, 1); // usuario por defecto
                stmtVenta.setDouble(2, totalFinal);
                stmtVenta.executeUpdate();
                
                ResultSet rsVenta = stmtVenta.getGeneratedKeys();
                if (rsVenta.next()) {
                    ventaId = rsVenta.getInt(1);
                    System.out.println("Nueva venta creada con ID: " + ventaId);
                }
                rsVenta.close();
                stmtVenta.close();
            }

            String sqlTotal = "UPDATE ventas SET total = ? WHERE id_venta = ?";
            PreparedStatement stmtTotal = c.prepareStatement(sqlTotal);
            stmtTotal.setDouble(1, totalFinal);
            stmtTotal.setInt(2, ventaId);
            int rowsUpdated = stmtTotal.executeUpdate();
            System.out.println("Filas actualizadas en ventas: " + rowsUpdated);
            stmtTotal.close();

            for (DetalleVentaRequest detalle : detalles) {
                String sqlInsert = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
                PreparedStatement stmtInsert = c.prepareStatement(sqlInsert);
                stmtInsert.setInt(1, ventaId);
                stmtInsert.setInt(2, detalle.idProducto);
                stmtInsert.setDouble(3, detalle.cantidad);
                stmtInsert.setDouble(4, detalle.precioUnitario);
                stmtInsert.executeUpdate();
                stmtInsert.close();

                String sqlStock = "UPDATE productos SET existencias_act = existencias_act - ? WHERE id_producto = ?";
                PreparedStatement stmtStock = c.prepareStatement(sqlStock);
                stmtStock.setDouble(1, detalle.cantidad);
                stmtStock.setInt(2, detalle.idProducto);
                stmtStock.executeUpdate();
                stmtStock.close();
            }

            c.commit(); 
            System.out.println("Transacción completada exitosamente");
        } catch (SQLException e) {
            System.out.println("Error SQL: " + e.getMessage());
            if (c != null) c.rollback();
            throw e;
        } catch (Exception e) {
            System.out.println("Error general: " + e.getMessage());
            e.printStackTrace();
            if (c != null) {
                try { c.rollback(); } catch (Exception ex) {}
            }
            throw new SQLException(e.getMessage());
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
                stmtDetalle.setDouble(6, detalle.cantidad);
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
            System.out.println("Conectando a BD para obtener ventas en espera...");
            Conexion con = new Conexion();
            Connection c = con.conectar();
            System.out.println("Conexión exitosa");
            
            // Primero verificar si hay datos en la tabla
            String sqlCheck = "SELECT COUNT(*) as total FROM ventas_en_espera";
            PreparedStatement stmtCheck = c.prepareStatement(sqlCheck);
            ResultSet rsCheck = stmtCheck.executeQuery();
            if (rsCheck.next()) {
                System.out.println("Ventas en espera en BD: " + rsCheck.getInt("total"));
            }
            rsCheck.close();
            stmtCheck.close();
            
            // Consulta simplificada sin JOIN
            String sql = "SELECT id_venta_espera, id_usuario, total, fecha FROM ventas_en_espera ORDER BY fecha DESC";
            PreparedStatement stmt = c.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            System.out.println("Query ejecutado, buscando resultados...");
            
            while (rs.next()) {
                System.out.println("Encontrada venta: " + rs.getInt("id_venta_espera"));
                VentaEnEspera venta = new VentaEnEspera(
                    rs.getInt("id_venta_espera"),
                    rs.getInt("id_usuario"),
                    "Usuario #" + rs.getInt("id_usuario"),  // Username fijo por ahora
                    rs.getString("fecha"),
                    rs.getDouble("total")
                );
                lista.add(venta);
            }
            
            System.out.println("Total de ventas encontradas: " + lista.size());
            
            rs.close();
            stmt.close();
            
            // Obtener detalles para cada venta
            for (VentaEnEspera venta : lista) {
                String sqlDetalles = "SELECT id_producto, codigo, nombre, precio, cantidad, foto_producto FROM detalles_venta_en_espera WHERE id_venta_espera = ?";
                PreparedStatement stmtDetalles = c.prepareStatement(sqlDetalles);
                stmtDetalles.setInt(1, venta.id);
                ResultSet rsDetalles = stmtDetalles.executeQuery();
                
                System.out.println("Obteniendo detalles para venta " + venta.id + ", encontrados: " + rsDetalles.getFetchSize());
                
                while (rsDetalles.next()) {
                    DetalleVentaEnEspera detalle = new DetalleVentaEnEspera(
                        rsDetalles.getInt("id_producto"),
                        rsDetalles.getString("codigo"),
                        rsDetalles.getString("nombre"),
                        rsDetalles.getDouble("precio"),
                        rsDetalles.getDouble("cantidad"),
                        rsDetalles.getString("foto_producto")
                    );
                    venta.detalles.add(detalle);
                    System.out.println("  - Detalle: " + detalle.nombre + " x" + detalle.cantidad);
                }
                rsDetalles.close();
                stmtDetalles.close();
            }
            
            c.close();
            System.out.println("Conexión cerrada");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    // Obtener una venta en espera por ID
    public VentaEnEspera obtenerVentaEnEspera(int idVentaEspera) {
        System.out.println("Obteniendo venta en espera con ID: " + idVentaEspera);
        VentaEnEspera venta = null;
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();
            System.out.println("Conexión obtenida");
            
            // Consulta simplificada sin JOIN
            String sql = "SELECT id_venta_espera, id_usuario, total, fecha FROM ventas_en_espera WHERE id_venta_espera = ?";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, idVentaEspera);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("Venta encontrada en BD");
                venta = new VentaEnEspera(
                    rs.getInt("id_venta_espera"),
                    rs.getInt("id_usuario"),
                    "Usuario #" + rs.getInt("id_usuario"),
                    rs.getString("fecha"),
                    rs.getDouble("total")
                );
            } else {
                System.out.println("NO se encontró la venta en la BD");
            }
            rs.close();
            stmt.close();
            
            if (venta != null) {
                String sqlDetalles = "SELECT id_producto, codigo, nombre, precio, cantidad, foto_producto FROM detalles_venta_en_espera WHERE id_venta_espera = ?";
                PreparedStatement stmtDetalles = c.prepareStatement(sqlDetalles);
                stmtDetalles.setInt(1, idVentaEspera);
                ResultSet rsDetalles = stmtDetalles.executeQuery();
                
                while (rsDetalles.next()) {
                    DetalleVentaEnEspera detalle = new DetalleVentaEnEspera(
                        rsDetalles.getInt("id_producto"),
                        rsDetalles.getString("codigo"),
                        rsDetalles.getString("nombre"),
                        rsDetalles.getDouble("precio"),
                        rsDetalles.getDouble("cantidad"),
                        rsDetalles.getString("foto_producto")
                    );
                    venta.detalles.add(detalle);
                    System.out.println("Detalle agregado: " + detalle.nombre);
                }
                rsDetalles.close();
                stmtDetalles.close();
            }
            
            c.close();
            System.out.println("Venta retornada: " + (venta != null ? "SI" : "NO"));
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
    public double cantidad;
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
