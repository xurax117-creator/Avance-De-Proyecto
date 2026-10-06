package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/conciliacion")
public class ConciliacionController {

    public static class CerrarConciliacionRequest {
        public String fecha;
        public int idSucursal;
        public double montoInicial;
        public double gastosCasa;
        public double comisionBancaria;
        public double disposicionesEfectivo;
        public double totalBancosReal;
        public int userId;
    }

    @GetMapping("/datos")
    public Map<String, Object> obtenerDatos(@RequestParam String fecha, @RequestParam(defaultValue = "1") int sucursal) {
        Conciliacion oper = new Conciliacion();
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> datos = oper.obtenerDatos(fecha, sucursal);
            response.put("success", true);
            response.putAll(datos);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @PostMapping("/cerrar")
    public Map<String, Object> cerrar(@RequestBody CerrarConciliacionRequest request) {
        Conciliacion oper = new Conciliacion();
        Map<String, Object> response = new HashMap<>();
        try {
            if (request.fecha == null || request.fecha.isBlank()) {
                response.put("success", false);
                response.put("message", "La fecha es obligatoria.");
                return response;
            }
            int sucursal = request.idSucursal > 0 ? request.idSucursal : 1;
            oper.cerrarConciliacion(request.fecha, sucursal, request.montoInicial, request.gastosCasa,
                    request.comisionBancaria, request.disposicionesEfectivo, request.totalBancosReal, request.userId);
            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage() != null ? e.getMessage() : "Error al cerrar la conciliación.");
        }
        return response;
    }
}
