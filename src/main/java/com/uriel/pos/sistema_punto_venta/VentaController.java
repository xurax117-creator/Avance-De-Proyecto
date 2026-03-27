package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.util.ArrayList;
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
        public List<DetalleVentaRequest> listaCarrito;
    }
    
    // Request para guardar venta en espera
    public static class VentaEnEsperaRequest {
        public int userId;
        public double total;
        public List<DetalleVentaEnEsperaRequest> listaCarrito;
    }
    
    // Request para detalles de venta en espera
    public static class DetalleVentaEnEsperaRequest {
        public int idProducto;
        public String codigo;
        public String nombre;
        public double precio;
        public int cantidad;
        public String fotoProducto;
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
            oper.finalizarTransaccion(request.idVenta, request.listaCarrito, request.totalFinal);
            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al procesar la venta.");
        }
        return response;
    }

    // Endpoint para búsqueda parcial de productos (cuando no encuentra exacto)
    @PostMapping("/buscar")
    public Map<String, Object> buscarProductos(@RequestBody Map<String, String> request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            String busqueda = request.get("busqueda");
            if (busqueda == null || busqueda.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Búsqueda vacía");
                return response;
            }
            
            // Primero verificar si existe coincidencia exacta
            ProductoData productoExacto = oper.obtenerDatosProducto(busqueda.trim());
            
            if (productoExacto != null) {
                // Encontró producto exacto
                response.put("success", true);
                response.put("tipo", "exacto");
                response.put("producto", productoExacto);
            } else {
                // No encontró exacto, buscar coincidencias parciales
                List<ProductoData> sugerencias = oper.buscarProductosParcial(busqueda.trim());
                
                if (sugerencias.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "No se encontraron productos");
                } else {
                    response.put("success", true);
                    response.put("tipo", "sugerencias");
                    response.put("sugerencias", sugerencias);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }
    
    // Endpoint para guardar venta en espera
    @PostMapping("/guardar-espera")
    public Map<String, Object> guardarVentaEnEspera(@RequestBody VentaEnEsperaRequest request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            // Convertir los detalles del request a objetos Venta
            List<DetalleVentaEnEspera> detalles = new ArrayList<>();
            for (DetalleVentaEnEsperaRequest det : request.listaCarrito) {
                detalles.add(new DetalleVentaEnEspera(
                    det.idProducto,
                    det.codigo,
                    det.nombre,
                    det.precio,
                    det.cantidad,
                    det.fotoProducto
                ));
            }
            
            int idVentaEspera = oper.guardarVentaEnEspera(request.userId, request.total, detalles);
            if (idVentaEspera > 0) {
                response.put("success", true);
                response.put("idVentaEspera", idVentaEspera);
            } else {
                response.put("success", false);
                response.put("message", "Error al guardar la venta en espera");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor: " + e.getMessage());
        }
        return response;
    }
    
    // Endpoint para listar ventas en espera
    @GetMapping("/lista-espera")
    public Map<String, Object> listarVentasEnEspera() {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            List<VentaEnEspera> lista = oper.obtenerVentasEnEspera();
            response.put("success", true);
            response.put("ventas", lista);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor: " + e.getMessage());
        }
        return response;
    }
    
    // Endpoint para obtener una venta en espera por ID
    @GetMapping("/obtener-espera/{id}")
    public Map<String, Object> obtenerVentaEnEspera(@PathVariable int id) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            VentaEnEspera venta = oper.obtenerVentaEnEspera(id);
            if (venta != null) {
                response.put("success", true);
                response.put("venta", venta);
            } else {
                response.put("success", false);
                response.put("message", "Venta en espera no encontrada");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor: " + e.getMessage());
        }
        return response;
    }
    
    // Endpoint para eliminar una venta en espera
    @DeleteMapping("/eliminar-espera/{id}")
    public Map<String, Object> eliminarVentaEnEspera(@PathVariable int id) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            boolean eliminado = oper.eliminarVentaEnEspera(id);
            if (eliminado) {
                response.put("success", true);
            } else {
                response.put("success", false);
                response.put("message", "Error al eliminar la venta en espera");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor: " + e.getMessage());
        }
        return response;
    }
}