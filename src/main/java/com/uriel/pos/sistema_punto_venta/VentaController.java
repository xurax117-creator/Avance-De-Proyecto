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
        public int idSucursal;
    }

    public static class ProductoRapidoRequest {
        public String nombre;
        public double precio;
        public int idVentaActual;
        public int userId;
        public int idSucursal;
    }

    public static class FinalizarRequest {
        public int userId;
        public int idVenta;
        public double totalFinal;
        public int idSucursal;
        public List<DetalleVentaRequest> listaCarrito;
        public double montoEfectivo;
        public double montoTarjeta;
    }

    public static class VentaEnEsperaRequest {
        public int userId;
        public double total;
        public int idSucursal;
        public List<DetalleVentaEnEsperaRequest> listaCarrito;
    }

    public static class DetalleVentaEnEsperaRequest {
        public int idProducto;
        public String codigo;
        public String nombre;
        public double precio;
        public double cantidad;
        public String fotoProducto;
    }

    @PostMapping("/producto")
    public Map<String, Object> buscarProducto(@RequestBody CodigoRequest request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            int sucursal = request.idSucursal > 0 ? request.idSucursal : 1;
            ProductoData data = oper.obtenerDatosProducto(request.codigo, sucursal);
            if (data == null) {
                response.put("success", false);
                response.put("message", "Producto no encontrado.");
                return response;
            }

            int ventaId = request.idVentaActual;
            if (ventaId == -1) {
                ventaId = oper.crearVenta(request.userId, sucursal);
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

    @PostMapping("/producto-rapido")
    public Map<String, Object> crearProductoRapido(@RequestBody ProductoRapidoRequest request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            if (request.nombre == null || request.nombre.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El nombre es obligatorio.");
                return response;
            }
            if (request.precio <= 0) {
                response.put("success", false);
                response.put("message", "El precio debe ser mayor a 0.");
                return response;
            }

            int sucursal = request.idSucursal > 0 ? request.idSucursal : 1;
            int idProducto = oper.crearProductoTemporal(request.nombre.trim(), request.precio, sucursal);
            if (idProducto == -1) {
                response.put("success", false);
                response.put("message", "Error al crear el producto temporal.");
                return response;
            }

            int ventaId = request.idVentaActual;
            if (ventaId == -1) {
                ventaId = oper.crearVenta(request.userId, sucursal);
            }

            Map<String, Object> productoMap = new HashMap<>();
            productoMap.put("idProducto", idProducto);
            productoMap.put("nombre", request.nombre.trim());
            productoMap.put("precio", request.precio);
            productoMap.put("idVenta", ventaId);

            response.put("success", true);
            response.put("data", productoMap);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @DeleteMapping("/producto-rapido/{id}")
    public Map<String, Object> eliminarProductoRapido(@PathVariable int id) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            oper.eliminarProductoTemporal(id);
            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
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
            System.out.println("=== FINALIZAR VENTA ===");
            System.out.println("idVenta: " + request.idVenta);
            System.out.println("totalFinal: " + request.totalFinal);
            System.out.println("Items en carrito: " + request.listaCarrito.size());

            int sucursal = request.idSucursal > 0 ? request.idSucursal : 1;
            double tarjeta = Math.min(Math.max(request.montoTarjeta, 0), request.totalFinal);
            double efectivo = request.totalFinal - tarjeta;
            oper.finalizarTransaccion(request.idVenta, request.listaCarrito, request.totalFinal, sucursal, efectivo, tarjeta);
            response.put("success", true);
            System.out.println("Venta finalizada exitosamente");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al procesar la venta: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/buscar")
    public Map<String, Object> buscarProductos(@RequestBody Map<String, Object> request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            String busqueda = request.get("busqueda") != null ? request.get("busqueda").toString() : "";
            int sucursal = request.get("idSucursal") != null ? Integer.parseInt(request.get("idSucursal").toString()) : 1;

            if (busqueda.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Búsqueda vacía");
                return response;
            }

            ProductoData productoExacto = oper.obtenerDatosProducto(busqueda.trim(), sucursal);

            if (productoExacto != null) {
                response.put("success", true);
                response.put("tipo", "exacto");
                response.put("producto", productoExacto);
            } else {
                List<ProductoData> sugerencias = oper.buscarProductosParcial(busqueda.trim(), sucursal);

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

    @PostMapping("/guardar-espera")
    public Map<String, Object> guardarVentaEnEspera(@RequestBody VentaEnEsperaRequest request) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            List<DetalleVentaEnEspera> detalles = new ArrayList<>();
            for (DetalleVentaEnEsperaRequest det : request.listaCarrito) {
                detalles.add(new DetalleVentaEnEspera(
                    det.idProducto, det.codigo, det.nombre, det.precio, det.cantidad, det.fotoProducto
                ));
            }

            int sucursal = request.idSucursal > 0 ? request.idSucursal : 1;
            int idVentaEspera = oper.guardarVentaEnEspera(request.userId, request.total, detalles, sucursal);
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

    @GetMapping("/lista-espera")
    public Map<String, Object> listarVentasEnEspera(@RequestParam(defaultValue = "1") int sucursal) {
        Venta oper = new Venta();
        Map<String, Object> response = new HashMap<>();
        try {
            List<VentaEnEspera> lista = oper.obtenerVentasEnEspera(sucursal);
            response.put("success", true);
            response.put("ventas", lista);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor: " + e.getMessage());
        }
        return response;
    }

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
