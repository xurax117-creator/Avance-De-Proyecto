package com.uriel.pos.sistema_punto_venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PromocionDAO {

    // N+1 resuelto: 2 queries en total en lugar de 3N
    public List<PromocionData> obtenerTodas(int idSucursal) {
        List<PromocionData> lista = new ArrayList<>();
        try (Connection c = new Conexion().conectar()) {
            Map<Integer, PromocionData> promoMap = new LinkedHashMap<>();
            String sqlPromos = """
                SELECT id_promocion, nombre, tipo, cantidad_requerida, precio_especial,
                       descuento_porcentaje, descuento_fijo, fecha_inicio, fecha_fin, activo
                FROM promociones WHERE id_sucursal = ? ORDER BY id_promocion DESC
            """;
            try (PreparedStatement stmt = c.prepareStatement(sqlPromos)) {
                stmt.setInt(1, idSucursal);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        PromocionData promo = new PromocionData(
                            rs.getInt("id_promocion"), rs.getString("nombre"), rs.getString("tipo"),
                            rs.getDouble("cantidad_requerida"),
                            rs.getObject("precio_especial")       != null ? rs.getDouble("precio_especial")       : null,
                            rs.getObject("descuento_porcentaje")  != null ? rs.getDouble("descuento_porcentaje")  : null,
                            rs.getObject("descuento_fijo")        != null ? rs.getDouble("descuento_fijo")        : null,
                            rs.getDate("fecha_inicio"), rs.getDate("fecha_fin"), rs.getBoolean("activo")
                        );
                        promoMap.put(promo.idPromocion, promo);
                        lista.add(promo);
                    }
                }
            }

            if (!promoMap.isEmpty()) {
                cargarProductosEnLote(promoMap, c);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public PromocionData obtenerPorId(int id) {
        PromocionData data = null;
        String sql = """
            SELECT id_promocion, nombre, tipo, cantidad_requerida, precio_especial,
                   descuento_porcentaje, descuento_fijo, fecha_inicio, fecha_fin, activo
            FROM promociones WHERE id_promocion = ?
        """;
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data = new PromocionData(
                        rs.getInt("id_promocion"), rs.getString("nombre"), rs.getString("tipo"),
                        rs.getDouble("cantidad_requerida"),
                        rs.getObject("precio_especial")       != null ? rs.getDouble("precio_especial")       : null,
                        rs.getObject("descuento_porcentaje")  != null ? rs.getDouble("descuento_porcentaje")  : null,
                        rs.getObject("descuento_fijo")        != null ? rs.getDouble("descuento_fijo")        : null,
                        rs.getDate("fecha_inicio"), rs.getDate("fecha_fin"), rs.getBoolean("activo")
                    );
                }
            }
            if (data != null) {
                cargarProductosDePromocion(data, c);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    private void cargarProductosDePromocion(PromocionData promo, Connection c) throws SQLException {
        String sql = """
            SELECT prod.id_producto, prod.nombre
            FROM promocion_productos pp
            JOIN productos prod ON pp.id_producto = prod.id_producto
            WHERE pp.id_promocion = ? ORDER BY prod.nombre
        """;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, promo.idPromocion);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int idProd = rs.getInt("id_producto");
                    String nombre = rs.getString("nombre");
                    promo.productos.add(idProd);
                    promo.nombresProductos.add(nombre);
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", idProd);
                    m.put("nombre", nombre);
                    promo.productosConNombre.add(m);
                }
            }
        }
    }

    private void cargarProductosEnLote(Map<Integer, PromocionData> promoMap, Connection c) throws SQLException {
        String ids = promoMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
        String sql = "SELECT pp.id_promocion, prod.id_producto, prod.nombre " +
                     "FROM promocion_productos pp " +
                     "JOIN productos prod ON pp.id_producto = prod.id_producto " +
                     "WHERE pp.id_promocion IN (" + ids + ") ORDER BY prod.nombre";
        try (PreparedStatement stmt = c.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                PromocionData promo = promoMap.get(rs.getInt("id_promocion"));
                if (promo == null) continue;
                int idProd = rs.getInt("id_producto");
                String nombre = rs.getString("nombre");
                promo.productos.add(idProd);
                promo.nombresProductos.add(nombre);
                Map<String, Object> m = new HashMap<>();
                m.put("id", idProd);
                m.put("nombre", nombre);
                promo.productosConNombre.add(m);
            }
        }
    }

    private void guardarProductosPromocion(int idPromocion, Set<Integer> productos, Connection c) throws SQLException {
        try (PreparedStatement del = c.prepareStatement("DELETE FROM promocion_productos WHERE id_promocion = ?")) {
            del.setInt(1, idPromocion); del.executeUpdate();
        }
        if (productos == null || productos.isEmpty()) return;
        try (PreparedStatement ins = c.prepareStatement("INSERT INTO promocion_productos (id_promocion, id_producto) VALUES (?, ?)")) {
            for (Integer idProducto : productos) {
                ins.setInt(1, idPromocion); ins.setInt(2, idProducto); ins.executeUpdate();
            }
        }
    }

    public boolean crear(PromocionRequest request) {
        Connection c = null;
        try {
            c = new Conexion().conectar();
            c.setAutoCommit(false);

            String sql = """
                INSERT INTO promociones (nombre, tipo, cantidad_requerida, precio_especial,
                                         descuento_porcentaje, descuento_fijo, fecha_inicio, fecha_fin, activo, id_sucursal)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            int idPromocion = -1;
            try (PreparedStatement stmt = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, request.nombre);
                stmt.setString(2, request.tipo);
                stmt.setDouble(3, request.cantidadRequerida);
                if (request.precioEspecial      != null) stmt.setDouble(4, request.precioEspecial);      else stmt.setNull(4, java.sql.Types.DOUBLE);
                if (request.descuentoPorcentaje != null) stmt.setDouble(5, request.descuentoPorcentaje); else stmt.setNull(5, java.sql.Types.DOUBLE);
                if (request.descuentoFijo       != null) stmt.setDouble(6, request.descuentoFijo);       else stmt.setNull(6, java.sql.Types.DOUBLE);
                stmt.setDate(7, request.fechaInicio != null ? java.sql.Date.valueOf(request.fechaInicio) : null);
                stmt.setDate(8, request.fechaFin    != null ? java.sql.Date.valueOf(request.fechaFin)    : null);
                stmt.setBoolean(9, request.activo);
                stmt.setInt(10, request.sucursal > 0 ? request.sucursal : 1);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) idPromocion = rs.getInt(1);
                }
            }

            if (idPromocion <= 0) { c.rollback(); return false; }
            guardarProductosPromocion(idPromocion, request.productos, c);
            c.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (c != null) { try { c.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            return false;
        } finally {
            if (c != null) { try { c.setAutoCommit(true); c.close(); } catch (SQLException ex) { ex.printStackTrace(); } }
        }
    }

    public boolean actualizar(int id, PromocionRequest request) {
        Connection c = null;
        try {
            c = new Conexion().conectar();
            c.setAutoCommit(false);

            String sql = """
                UPDATE promociones SET nombre=?, tipo=?, cantidad_requerida=?,
                                        precio_especial=?, descuento_porcentaje=?, descuento_fijo=?,
                                        fecha_inicio=?, fecha_fin=?, activo=?
                WHERE id_promocion=?
            """;
            int rows;
            try (PreparedStatement stmt = c.prepareStatement(sql)) {
                stmt.setString(1, request.nombre);
                stmt.setString(2, request.tipo);
                stmt.setDouble(3, request.cantidadRequerida);
                if (request.precioEspecial      != null) stmt.setDouble(4, request.precioEspecial);      else stmt.setNull(4, java.sql.Types.DOUBLE);
                if (request.descuentoPorcentaje != null) stmt.setDouble(5, request.descuentoPorcentaje); else stmt.setNull(5, java.sql.Types.DOUBLE);
                if (request.descuentoFijo       != null) stmt.setDouble(6, request.descuentoFijo);       else stmt.setNull(6, java.sql.Types.DOUBLE);
                stmt.setDate(7, request.fechaInicio != null ? java.sql.Date.valueOf(request.fechaInicio) : null);
                stmt.setDate(8, request.fechaFin    != null ? java.sql.Date.valueOf(request.fechaFin)    : null);
                stmt.setBoolean(9, request.activo);
                stmt.setInt(10, id);
                rows = stmt.executeUpdate();
            }

            if (request.productos != null) guardarProductosPromocion(id, request.productos, c);
            c.commit();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            if (c != null) { try { c.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            return false;
        } finally {
            if (c != null) { try { c.setAutoCommit(true); c.close(); } catch (SQLException ex) { ex.printStackTrace(); } }
        }
    }

    public boolean eliminar(int id) {
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement("DELETE FROM promociones WHERE id_promocion = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public List<PromocionData> obtenerPromocionesActivasPorProducto(int idProducto, int idSucursal) {
        List<PromocionData> lista = new ArrayList<>();
        String sql = """
            SELECT DISTINCT p.id_promocion, p.nombre, p.tipo, p.cantidad_requerida,
                   p.precio_especial, p.descuento_porcentaje, p.descuento_fijo,
                   p.fecha_inicio, p.fecha_fin, p.activo
            FROM promociones p
            JOIN promocion_productos pp ON p.id_promocion = pp.id_promocion
            WHERE pp.id_producto = ? AND p.activo = TRUE AND p.id_sucursal = ?
              AND (p.fecha_inicio IS NULL OR p.fecha_inicio <= CURDATE())
              AND (p.fecha_fin   IS NULL OR p.fecha_fin   >= CURDATE())
            ORDER BY p.tipo, p.cantidad_requerida DESC
        """;
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            stmt.setInt(2, idSucursal);
            Map<Integer, PromocionData> promoMap = new LinkedHashMap<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PromocionData promo = new PromocionData(
                        rs.getInt("id_promocion"), rs.getString("nombre"), rs.getString("tipo"),
                        rs.getDouble("cantidad_requerida"),
                        rs.getDouble("precio_especial"), rs.getDouble("descuento_porcentaje"), rs.getDouble("descuento_fijo"),
                        rs.getDate("fecha_inicio"), rs.getDate("fecha_fin"), rs.getBoolean("activo")
                    );
                    promoMap.put(promo.idPromocion, promo);
                    lista.add(promo);
                }
            }
            if (!promoMap.isEmpty()) cargarProductosEnLote(promoMap, c);
        } catch (Exception e) { e.printStackTrace(); }
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
    public List<Map<String, Object>> productosConNombre;

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
        this.productosConNombre = new ArrayList<>();
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
    public int sucursal;
}
