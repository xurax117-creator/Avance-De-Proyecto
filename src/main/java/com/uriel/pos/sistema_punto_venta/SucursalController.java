package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/sucursales")
public class SucursalController {

    @GetMapping("/activas")
    public List<Map<String, Object>> listarActivas() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_sucursal, nombre, color FROM sucursales WHERE activo = TRUE ORDER BY id_sucursal ASC";
        try (Connection c = new Conexion().conectar();
             PreparedStatement stmt = c.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id",     rs.getInt("id_sucursal"));
                m.put("nombre", rs.getString("nombre"));
                m.put("color",  rs.getString("color"));
                lista.add(m);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }
}
