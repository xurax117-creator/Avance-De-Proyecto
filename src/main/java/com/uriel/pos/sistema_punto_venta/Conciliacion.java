package com.uriel.pos.sistema_punto_venta;

import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Conciliacion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, Object> obtenerDatos(String fecha, int idSucursal) throws Exception {
        Map<String, Object> resultado = new HashMap<>();
        try (Connection c = new Conexion().conectar()) {

            Map<String, Object> cerrada = obtenerCerrada(c, fecha, idSucursal);
            resultado.put("existeCerrada", cerrada != null);
            resultado.put("conciliacion", cerrada);

            String fechaAnterior = LocalDate.parse(fecha).minusDays(1).toString();
            Map<String, Object> cerradaAnterior = obtenerCerrada(c, fechaAnterior, idSucursal);
            resultado.put("diaAnteriorCerrado", cerradaAnterior != null);
            resultado.put("montoInicialSugerido", cerradaAnterior != null ? cerradaAnterior.get("totalEfectivoReal") : 0.0);
            resultado.put("bancosTeoricoAnterior", cerradaAnterior != null ? cerradaAnterior.get("totalBancosTeorico") : 0.0);
            resultado.put("comisionBancariaAnterior", cerradaAnterior != null ? cerradaAnterior.get("comisionBancaria") : 0.0);

            resultado.put("automaticos", calcularAutomaticos(c, fecha, idSucursal));
        }
        return resultado;
    }

    private Map<String, Object> calcularAutomaticos(Connection c, String fecha, int idSucursal) throws Exception {
        String fechaHoraInicio = fecha + " 00:00:00";
        String fechaHoraFin    = fecha + " 23:59:59";

        double ventaEfectivoBruta = 0;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(monto_efectivo),0) AS v FROM ventas WHERE fecha BETWEEN ? AND ? AND id_sucursal = ?")) {
            ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, idSucursal);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) ventaEfectivoBruta = rs.getDouble("v"); }
        }

        // Una devolución afecta la caja del día en que se hace, sin importar cuándo fue la venta original.
        double efectivoDevuelto = 0;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(monto_efectivo_devuelto),0) AS v FROM devoluciones WHERE fecha BETWEEN ? AND ? AND id_sucursal = ?")) {
            ps.setString(1, fechaHoraInicio); ps.setString(2, fechaHoraFin); ps.setInt(3, idSucursal);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) efectivoDevuelto = rs.getDouble("v"); }
        }

        double ventaEfectivo = ventaEfectivoBruta - efectivoDevuelto;

        double terminal1 = 0, terminal2 = 0, monedas = 0, billetes = 0;
        String filasJson = null;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT terminal1, terminal2, monedas, billetes, filas FROM gastos_dia WHERE fecha = ? AND id_sucursal = ?")) {
            ps.setString(1, fecha); ps.setInt(2, idSucursal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    terminal1 = rs.getDouble("terminal1");
                    terminal2 = rs.getDouble("terminal2");
                    monedas   = rs.getDouble("monedas");
                    billetes  = rs.getDouble("billetes");
                    filasJson = rs.getString("filas");
                }
            }
        }

        double gastos = 0, personasQueDeben = 0;
        if (filasJson != null) {
            List<Map<String, Object>> filas = MAPPER.readValue(filasJson, List.class);
            for (Map<String, Object> fila : filas) {
                gastos += parsearMonto(fila.get("monto"));
                personasQueDeben += parsearMonto(fila.get("montoDeudor"));
            }
        }

        double ventaTarjetasConComision = terminal1 + terminal2;
        double ventaTarjetasSinComision = ventaTarjetasConComision * 0.99;
        double ventaTotal = ventaEfectivo + ventaTarjetasConComision;
        double totalEfectivoReal = monedas + billetes;

        Map<String, Object> automaticos = new HashMap<>();
        automaticos.put("ventaEfectivo", ventaEfectivo);
        automaticos.put("ventaTarjetasConComision", ventaTarjetasConComision);
        automaticos.put("ventaTarjetasSinComision", ventaTarjetasSinComision);
        automaticos.put("ventaTotal", ventaTotal);
        automaticos.put("gastos", gastos);
        automaticos.put("personasQueDeben", personasQueDeben);
        automaticos.put("totalEfectivoReal", totalEfectivoReal);
        return automaticos;
    }

    private double parsearMonto(Object valor) {
        if (valor == null) return 0;
        try { return Double.parseDouble(valor.toString().trim()); }
        catch (Exception e) { return 0; }
    }

    private Map<String, Object> obtenerCerrada(Connection c, String fecha, int idSucursal) throws Exception {
        String sql = "SELECT * FROM conciliaciones WHERE fecha = ? AND id_sucursal = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fecha);
            ps.setInt(2, idSucursal);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> m = new HashMap<>();
                m.put("idConciliacion", rs.getInt("id_conciliacion"));
                m.put("montoInicial", rs.getDouble("monto_inicial"));
                m.put("ventaEfectivo", rs.getDouble("venta_efectivo"));
                m.put("ventaTarjetasConComision", rs.getDouble("venta_tarjetas_con_comision"));
                m.put("ventaTarjetasSinComision", rs.getDouble("venta_tarjetas_sin_comision"));
                m.put("ventaTotal", rs.getDouble("venta_total"));
                m.put("gastos", rs.getDouble("gastos"));
                m.put("gastosCasa", rs.getDouble("gastos_casa"));
                m.put("personasQueDeben", rs.getDouble("personas_que_deben"));
                m.put("totalEfectivoTeorico", rs.getDouble("total_efectivo_teorico"));
                m.put("comisionBancaria", rs.getDouble("comision_bancaria"));
                m.put("disposicionesEfectivo", rs.getDouble("disposiciones_efectivo"));
                m.put("totalBancosTeorico", rs.getDouble("total_bancos_teorico"));
                m.put("totalEfectivoReal", rs.getDouble("total_efectivo_real"));
                m.put("totalBancosReal", rs.getDouble("total_bancos_real"));
                m.put("diferenciaEfectivo", rs.getDouble("diferencia_efectivo"));
                m.put("diferenciaBancos", rs.getDouble("diferencia_bancos"));
                m.put("fechaCierre", rs.getString("fecha_cierre"));
                return m;
            }
        }
    }

    public void cerrarConciliacion(String fecha, int idSucursal, double montoInicial, double gastosCasa,
                                    double comisionBancaria, double disposicionesEfectivo, double totalBancosReal,
                                    int idUsuario) throws Exception {
        try (Connection c = new Conexion().conectar()) {
            Map<String, Object> automaticos = calcularAutomaticos(c, fecha, idSucursal);

            double ventaEfectivo             = (double) automaticos.get("ventaEfectivo");
            double ventaTarjetasConComision  = (double) automaticos.get("ventaTarjetasConComision");
            double ventaTarjetasSinComision  = (double) automaticos.get("ventaTarjetasSinComision");
            double ventaTotal                = (double) automaticos.get("ventaTotal");
            double gastos                    = (double) automaticos.get("gastos");
            double personasQueDeben          = (double) automaticos.get("personasQueDeben");
            double totalEfectivoReal         = (double) automaticos.get("totalEfectivoReal");

            double totalEfectivoTeorico = montoInicial + ventaEfectivo - gastos - gastosCasa - personasQueDeben;

            String fechaAnterior = LocalDate.parse(fecha).minusDays(1).toString();
            Map<String, Object> cerradaAnterior = obtenerCerrada(c, fechaAnterior, idSucursal);
            double bancosTeoricoAnterior    = cerradaAnterior != null ? (double) cerradaAnterior.get("totalBancosTeorico") : 0.0;
            double comisionBancariaAnterior = cerradaAnterior != null ? (double) cerradaAnterior.get("comisionBancaria") : 0.0;
            double totalBancosTeorico = bancosTeoricoAnterior + ventaTarjetasSinComision - disposicionesEfectivo - comisionBancariaAnterior;

            double diferenciaEfectivo = totalEfectivoReal - totalEfectivoTeorico;
            double diferenciaBancos   = totalBancosReal - totalBancosTeorico;

            String sql = "INSERT INTO conciliaciones (fecha, id_sucursal, monto_inicial, venta_efectivo, " +
                         "venta_tarjetas_con_comision, venta_tarjetas_sin_comision, venta_total, gastos, gastos_casa, " +
                         "personas_que_deben, total_efectivo_teorico, comision_bancaria, disposiciones_efectivo, " +
                         "total_bancos_teorico, total_efectivo_real, total_bancos_real, diferencia_efectivo, " +
                         "diferencia_bancos, id_usuario_cierre) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                         "ON DUPLICATE KEY UPDATE monto_inicial=VALUES(monto_inicial), venta_efectivo=VALUES(venta_efectivo), " +
                         "venta_tarjetas_con_comision=VALUES(venta_tarjetas_con_comision), " +
                         "venta_tarjetas_sin_comision=VALUES(venta_tarjetas_sin_comision), venta_total=VALUES(venta_total), " +
                         "gastos=VALUES(gastos), gastos_casa=VALUES(gastos_casa), personas_que_deben=VALUES(personas_que_deben), " +
                         "total_efectivo_teorico=VALUES(total_efectivo_teorico), comision_bancaria=VALUES(comision_bancaria), " +
                         "disposiciones_efectivo=VALUES(disposiciones_efectivo), total_bancos_teorico=VALUES(total_bancos_teorico), " +
                         "total_efectivo_real=VALUES(total_efectivo_real), total_bancos_real=VALUES(total_bancos_real), " +
                         "diferencia_efectivo=VALUES(diferencia_efectivo), diferencia_bancos=VALUES(diferencia_bancos), " +
                         "id_usuario_cierre=VALUES(id_usuario_cierre), fecha_cierre=CURRENT_TIMESTAMP";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, fecha);
                ps.setInt(2, idSucursal);
                ps.setDouble(3, montoInicial);
                ps.setDouble(4, ventaEfectivo);
                ps.setDouble(5, ventaTarjetasConComision);
                ps.setDouble(6, ventaTarjetasSinComision);
                ps.setDouble(7, ventaTotal);
                ps.setDouble(8, gastos);
                ps.setDouble(9, gastosCasa);
                ps.setDouble(10, personasQueDeben);
                ps.setDouble(11, totalEfectivoTeorico);
                ps.setDouble(12, comisionBancaria);
                ps.setDouble(13, disposicionesEfectivo);
                ps.setDouble(14, totalBancosTeorico);
                ps.setDouble(15, totalEfectivoReal);
                ps.setDouble(16, totalBancosReal);
                ps.setDouble(17, diferenciaEfectivo);
                ps.setDouble(18, diferenciaBancos);
                ps.setInt(19, idUsuario);
                ps.executeUpdate();
            }
        }
    }
}
