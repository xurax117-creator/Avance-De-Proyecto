package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gastos")
public class GastoController {

    public static class GuardarHojaRequest {
        public String fecha;
        public int idSucursal;
        public List<Map<String, Object>> filas;
        public double monedas;
        public double billetes;
        public double terminal1;
        public double terminal2;
        public Integer userId;
    }

    @GetMapping("/hoja")
    public Map<String, Object> obtenerHoja(@RequestParam String fecha, @RequestParam(defaultValue = "1") int sucursal) {
        Gasto oper = new Gasto();
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> hoja = oper.obtenerHoja(fecha, sucursal);
            response.put("success", true);
            if (hoja != null) {
                response.put("existe", true);
                response.putAll(hoja);
            } else {
                response.put("existe", false);
                response.put("filas", new ArrayList<>());
                response.put("monedas", 0);
                response.put("billetes", 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @PostMapping("/hoja")
    public Map<String, Object> guardarHoja(@RequestBody GuardarHojaRequest request) {
        Gasto oper = new Gasto();
        Map<String, Object> response = new HashMap<>();
        try {
            if (request.fecha == null || request.fecha.isBlank()) {
                response.put("success", false);
                response.put("message", "La fecha es obligatoria.");
                return response;
            }
            int sucursal = request.idSucursal > 0 ? request.idSucursal : 1;
            List<Map<String, Object>> filas = request.filas != null ? request.filas : new ArrayList<>();
            boolean ok = oper.guardarHoja(request.fecha, sucursal, filas, request.monedas, request.billetes, request.terminal1, request.terminal2, request.userId);
            response.put("success", ok);
            if (!ok) response.put("message", "Error al guardar la hoja de gastos.");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }
}
