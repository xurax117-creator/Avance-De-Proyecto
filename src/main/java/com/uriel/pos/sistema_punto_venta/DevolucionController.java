package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devolucion")
public class DevolucionController {

    public static class ItemDevolucionDTO {
        public int idDetalleVenta;
        public double cantidad;
    }

    public static class ProcesarDevolucionRequest {
        public int idVenta;
        public int userId;
        public int idSucursal;
        public List<ItemDevolucionDTO> items;
    }

    @GetMapping("/venta/{id}")
    public Map<String, Object> obtenerVentaParaDevolucion(@PathVariable int id) {
        Devolucion oper = new Devolucion();
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> venta = oper.obtenerVentaParaDevolucion(id);
            if (venta == null) {
                response.put("success", false);
                response.put("message", "Venta no encontrada.");
                return response;
            }
            response.put("success", true);
            response.put("venta", venta);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @PostMapping("/procesar")
    public Map<String, Object> procesarDevolucion(@RequestBody ProcesarDevolucionRequest request) {
        Devolucion oper = new Devolucion();
        Map<String, Object> response = new HashMap<>();
        try {
            if (request.items == null || request.items.isEmpty()) {
                response.put("success", false);
                response.put("message", "No se especificaron productos a devolver.");
                return response;
            }

            List<ItemDevolucionRequest> items = new ArrayList<>();
            for (ItemDevolucionDTO dto : request.items) {
                ItemDevolucionRequest item = new ItemDevolucionRequest();
                item.idDetalleVenta = dto.idDetalleVenta;
                item.cantidad = dto.cantidad;
                items.add(item);
            }

            int sucursal = request.idSucursal > 0 ? request.idSucursal : 1;
            oper.procesarDevolucion(request.idVenta, items, request.userId, sucursal);
            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage() != null ? e.getMessage() : "Error al procesar la devolución.");
        }
        return response;
    }
}
