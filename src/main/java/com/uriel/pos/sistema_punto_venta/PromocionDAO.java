package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PromocionDAO {

    public List<PromocionData> obtenerTodas() {
        List<PromocionData> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                SELECT p.id_promocion, p.nombre, p.tipo, 
                       p.cantidad_requerida, p.precio_especial, p.descuento_porcentaje, p.descuento_fijo,
                       p.fecha_inicio, p.fecha_fin, p.activo
                FROM promociones p
                ORDER BY p.id_promocion DESC
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PromocionData promo = new PromocionData(
                    rs.getInt("id_promocion"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    rs.getDouble("cantidad_requerida"),
                    rs.getDouble("precio_especial"),
                    rs.getDouble("descuento_porcentaje"),
                    rs.getDouble("descuento_fijo"),
                    rs.getDate("fecha_inicio"),
                    rs.getDate("fecha_fin"),
                    rs.getBoolean("activo")
                );
                promo.productos = obtenerProductosDePromocion(promo.idPromocion, c);
                promo.nombresProductos = obtenerNombresProductosDePromocion(promo.idPromocion, c);
                lista.add(promo);
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
                SELECT id_promocion, nombre, tipo, cantidad_requerida, precio_especial,
                       descuento_porcentaje, descuento_fijo, fecha_inicio, fecha_fin, activo
                FROM promociones
                WHERE id_promocion = ?
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                data = new PromocionData(
                    rs.getInt("id_promocion"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    rs.getDouble("cantidad_requerida"),
                    rs.getDouble("precio_especial"),
                    rs.getDouble("descuento_porcentaje"),
                    rs.getDouble("descuento_fijo"),
                    rs.getDate("fecha_inicio"),
                    rs.getDate("fecha_fin"),
                    rs.getBoolean("activo")
                );
                data.productos = obtenerProductosDePromocion(id, c);
                data.nombresProductos = obtenerNombresProductosDePromocion(id, c);
            }

            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    private Set<Integer> obtenerProductosDePromocion(int idPromocion, Connection c) throws SQLException {
        Set<Integer> productos = new HashSet<>();
        String sql = "SELECT id_producto FROM promocion_productos WHERE id_promocion = ?";
        PreparedStatement stmt = c.prepareStatement(sql);
        stmt.setInt(1, idPromocion);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            productos.add(rs.getInt("id_producto"));
        }
        rs.close();
        stmt.close();
        return productos;
    }

    private List<String> obtenerNombresProductosDePromocion(int idPromocion, Connection c) throws SQLException {
        List<String> nombres = new ArrayList<>();
        String sql = """
            SELECT prod.nombre FROM promocion_productos pp
            JOIN productos prod ON pp.id_producto = prod.id_producto
            WHERE pp.id_promocion = ?
            ORDER BY prod.nombre
        """;
        PreparedStatement stmt = c.prepareStatement(sql);
        stmt.setInt(1, idPromocion);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            nombres.add(rs.getString("nombre"));
        }
        rs.close();
        stmt.close();
        return nombres;
    }

    private void guardarProductosPromocion(int idPromocion, Set<Integer> productos, Connection c) throws SQLException {
        String deleteSql = "DELETE FROM promocion_productos WHERE id_promocion = ?";
        PreparedStatement deleteStmt = c.prepareStatement(deleteSql);
        deleteStmt.setInt(1, idPromocion);
        deleteStmt.executeUpdate();
        deleteStmt.close();

        String insertSql = "INSERT INTO promocion_productos (id_promocion, id_producto) VALUES (?, ?)";
        PreparedStatement insertStmt = c.prepareStatement(insertSql);
        for (Integer idProducto : productos) {
            insertStmt.setInt(1, idPromocion);
            insertStmt.setInt(2, idProducto);
            insertStmt.executeUpdate();
        }
        insertStmt.close();
    }

    public boolean crear(PromocionRequest request) {
        Connection c = null;
        try {
            if (request == null) {
                System.err.println("DAO ERROR: Request is null");
                return false;
            }
            Conexion con = new Conexion();
            c = con.conectar();
            if (c == null) {
                System.err.println("DAO ERROR: Connection is null");
                return false;
            }
            boolean autoCommitBefore = c.getAutoCommit();
            c.setAutoCommit(false);
            System.out.println("DAO: Transaction started, autoCommit before=" + autoCommitBefore + ", after=" + c.getAutoCommit());

            System.out.println("DAO: Intentando crear promoción: " + request.nombre);
            System.out.println("DAO: Tipo: " + request.tipo);
            System.out.println("DAO: Productos: " + request.productos);
            System.out.println("DAO: Cantidad requerida: " + request.cantidadRequerida);
            System.out.println("DAO: Precio especial: " + request.precioEspecial);
            System.out.println("DAO: Descuento porcentaje: " + request.descuentoPorcentaje);
            System.out.println("DAO: Descuento fijo: " + request.descuentoFijo);
            System.out.println("DAO: Fecha inicio: " + request.fechaInicio);
            System.out.println("DAO: Fecha fin: " + request.fechaFin);
            System.out.println("DAO: Activo: " + request.activo);

            String sql = """
                INSERT INTO promociones (nombre, tipo, cantidad_requerida, precio_especial,
                                         descuento_porcentaje, descuento_fijo, fecha_inicio, fecha_fin, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, request.nombre);
            stmt.setString(2, request.tipo);
            stmt.setDouble(3, request.cantidadRequerida);
            
            if (request.precioEspecial != null) {
                stmt.setDouble(4, request.precioEspecial);
            } else {
                stmt.setNull(4, java.sql.Types.DOUBLE);
            }
            
            if (request.descuentoPorcentaje != null) {
                stmt.setDouble(5, request.descuentoPorcentaje);
            } else {
                stmt.setNull(5, java.sql.Types.DOUBLE);
            }
            
            if (request.descuentoFijo != null) {
                stmt.setDouble(6, request.descuentoFijo);
            } else {
                stmt.setNull(6, java.sql.Types.DOUBLE);
            }
            
            stmt.setDate(7, request.fechaInicio != null ? java.sql.Date.valueOf(request.fechaInicio) : null);
            stmt.setDate(8, request.fechaFin != null ? java.sql.Date.valueOf(request.fechaFin) : null);
            stmt.setBoolean(9, request.activo);

            int rows = stmt.executeUpdate();
            
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            int idPromocion = -1;
            if (generatedKeys.next()) {
                idPromocion = generatedKeys.getInt(1);
            }
            generatedKeys.close();
            stmt.close();

            if (idPromocion > 0 && request.productos != null && !request.productos.isEmpty()) {
                guardarProductosPromocion(idPromocion, request.productos, c);
            }

            System.out.println("DAO: About to commit, idPromocion=" + idPromocion);
            c.commit();
            c.close();
            System.out.println("DAO: Promoción creada con ID: " + idPromocion);
            return true;
        } catch (Exception e) {
            System.err.println("DAO Error en crear: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            if (c != null) {
                try {
                    c.rollback();
                    c.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        }
    }

    public boolean actualizar(int id, PromocionRequest request) {
        Connection c = null;
        try {
            Conexion con = new Conexion();
            c = con.conectar();
            c.setAutoCommit(false);

            System.out.println("DAO: Intentando actualizar promoción ID: " + id);
            System.out.println("DAO: Nombre: " + request.nombre);
            System.out.println("DAO: Tipo: " + request.tipo);
            System.out.println("DAO: Productos: " + request.productos);

            String sql = """
                UPDATE promociones SET nombre = ?, tipo = ?, cantidad_requerida = ?,
                                        precio_especial = ?, descuento_porcentaje = ?, descuento_fijo = ?,
                                        fecha_inicio = ?, fecha_fin = ?, activo = ?
                WHERE id_promocion = ?
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setString(1, request.nombre);
            stmt.setString(2, request.tipo);
            stmt.setDouble(3, request.cantidadRequerida);
            
            if (request.precioEspecial != null) {
                stmt.setDouble(4, request.precioEspecial);
            } else {
                stmt.setNull(4, java.sql.Types.DOUBLE);
            }
            
            if (request.descuentoPorcentaje != null) {
                stmt.setDouble(5, request.descuentoPorcentaje);
            } else {
                stmt.setNull(5, java.sql.Types.DOUBLE);
            }
            
            if (request.descuentoFijo != null) {
                stmt.setDouble(6, request.descuentoFijo);
            } else {
                stmt.setNull(6, java.sql.Types.DOUBLE);
            }
            
            stmt.setDate(7, request.fechaInicio != null ? java.sql.Date.valueOf(request.fechaInicio) : null);
            stmt.setDate(8, request.fechaFin != null ? java.sql.Date.valueOf(request.fechaFin) : null);
            stmt.setBoolean(9, request.activo);
            stmt.setInt(10, id);

            int rows = stmt.executeUpdate();
            stmt.close();

            if (request.productos != null) {
                guardarProductosPromocion(id, request.productos, c);
            }

            c.commit();
            c.close();
            return rows > 0;
        } catch (Exception e) {
            System.err.println("DAO Error en actualizar: " + e.getMessage());
            e.printStackTrace();
            if (c != null) {
                try {
                    c.rollback();
                    c.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
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

    public List<PromocionData> obtenerPromocionesActivasPorProducto(int idProducto) {
        List<PromocionData> lista = new ArrayList<>();
        try {
            Conexion con = new Conexion();
            Connection c = con.conectar();

            String sql = """
                SELECT DISTINCT p.id_promocion, p.nombre, p.tipo, p.cantidad_requerida,
                       p.precio_especial, p.descuento_porcentaje, p.descuento_fijo,
                       p.fecha_inicio, p.fecha_fin, p.activo
                FROM promociones p
                JOIN promocion_productos pp ON p.id_promocion = pp.id_promocion
                WHERE pp.id_producto = ? AND p.activo = TRUE
                  AND (p.fecha_inicio IS NULL OR p.fecha_inicio <= CURDATE())
                  AND (p.fecha_fin IS NULL OR p.fecha_fin >= CURDATE())
                ORDER BY p.tipo, p.cantidad_requerida DESC
            """;

            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setInt(1, idProducto);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PromocionData promo = new PromocionData(
                    rs.getInt("id_promocion"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    rs.getDouble("cantidad_requerida"),
                    rs.getDouble("precio_especial"),
                    rs.getDouble("descuento_porcentaje"),
                    rs.getDouble("descuento_fijo"),
                    rs.getDate("fecha_inicio"),
                    rs.getDate("fecha_fin"),
                    rs.getBoolean("activo")
                );
                promo.productos = obtenerProductosDePromocion(promo.idPromocion, c);
                promo.nombresProductos = obtenerNombresProductosDePromocion(promo.idPromocion, c);
                lista.add(promo);
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
    public double cantidadRequerida;
    public Double precioEspecial;
    public Double descuentoPorcentaje;
    public Double descuentoFijo;
    public java.sql.Date fechaInicio;
    public java.sql.Date fechaFin;
    public boolean activo;
    public Set<Integer> productos;
    public List<String> nombresProductos;

    public PromocionData(int idPromocion, String nombre, String tipo, double cantidadRequerida,
                         Double precioEspecial, Double descuentoPorcentaje, Double descuentoFijo,
                         java.sql.Date fechaInicio, java.sql.Date fechaFin, boolean activo) {
        this.idPromocion = idPromocion;
        this.nombre = nombre;
        this.tipo = tipo;
        this.cantidadRequerida = cantidadRequerida;
        this.precioEspecial = precioEspecial;
        this.descuentoPorcentaje = descuentoPorcentaje;
        this.descuentoFijo = descuentoFijo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
        this.productos = new HashSet<>();
        this.nombresProductos = new ArrayList<>();
    }
}

class PromocionRequest {
    public String nombre;
    public String tipo;
    public Set<Integer> productos;
    public double cantidadRequerida;
    public Double precioEspecial;
    public Double descuentoPorcentaje;
    public Double descuentoFijo;
    public String fechaInicio;
    public String fechaFin;
    public boolean activo;
}