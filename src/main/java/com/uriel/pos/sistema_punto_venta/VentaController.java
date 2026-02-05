package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/venta")
public class VentaController {

    public static class CodigoRequest {
        public String codigo;
        public int idVentaActual;
        public int userId;
    }

    public static class FinalizarRequest {
        public int userId;
        public int idVenta;
        public double totalFinal;
        public List<DetalleVentaRequest> carrito;
    }

    @PostMapping("/producto")
    public Map<String, Object> buscarProducto(@RequestBody CodigoRequest request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            ProductoData data = oper.obtenerDatosProducto(request.codigo);
            if (data == null) {
                response.put("success", false);
                response.put("message", "Producto no encontrado.");
                return response;
            }

            int ventaId = request.idVentaActual;
            if (ventaId == -1) {
                ventaId = oper.crearVenta(request.userId);
            }

            Map<String, Object> productoMap = new HashMap<>();
            productoMap.put("idProducto", data.idProducto);
            productoMap.put("nombre", data.nombre);
            productoMap.put("precio", data.precioVenta);
            productoMap.put("stock", data.stockActual);
            productoMap.put("idVenta", ventaId);
            productoMap.put("fotoProducto", data.fotoProducto);

            response.put("success", true);
            response.put("data", productoMap);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @PostMapping("/finalizar")
    public Map<String, Object> finalizarVenta(@RequestBody FinalizarRequest request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            oper.finalizarTransaccion(request.idVenta, request.carrito, request.totalFinal);
            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al procesar la venta.");
        }
        return response;
    }
}