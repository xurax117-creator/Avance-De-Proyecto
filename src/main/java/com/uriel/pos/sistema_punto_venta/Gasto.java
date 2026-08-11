package com.uriel.pos.sistema_punto_venta;

import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Gasto {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, Object> obtenerHoja(String fecha, int idSucursal) {
        String sql = "SELECT filas, monedas, billetes, terminal1, terminal2 FROM gastos_dia WHERE fecha = ? AND id_sucursal = ?";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, fecha);
            stmt.setInt(2, idSucursal);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("filas", MAPPER.readValue(rs.getString("filas"), List.class));
                    resultado.put("monedas", rs.getDouble("monedas"));
                    resultado.put("billetes", rs.getDouble("billetes"));
                    resultado.put("terminal1", rs.getDouble("terminal1"));
                    resultado.put("terminal2", rs.getDouble("terminal2"));
                    return resultado;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean guardarHoja(String fecha, int idSucursal, List<Map<String, Object>> filas, double monedas, double billetes, double terminal1, double terminal2, Integer idUsuario) {
        String filasJson = MAPPER.writeValueAsString(filas);
        String sql = "INSERT INTO gastos_dia (fecha, id_sucursal, filas, monedas, billetes, terminal1, terminal2, id_usuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE filas = VALUES(filas), monedas = VALUES(monedas), billetes = VALUES(billetes), " +
                     "terminal1 = VALUES(terminal1), terminal2 = VALUES(terminal2), id_usuario = VALUES(id_usuario)";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, fecha);
            stmt.setInt(2, idSucursal);
            stmt.setString(3, filasJson);
            stmt.setDouble(4, monedas);
            stmt.setDouble(5, billetes);
            stmt.setDouble(6, terminal1);
            stmt.setDouble(7, terminal2);
            if (idUsuario != null && idUsuario > 0) stmt.setInt(8, idUsuario);
            else stmt.setNull(8, Types.INTEGER);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
