package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    @GetMapping("/todas")
    public List<Map<String, Object>> listarTodas() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            PromocionDAO dao = new PromocionDAO();
            List<PromocionData> promociones = dao.obtenerTodas();
            for (PromocionData p : promociones) {
                Map<String, Object> m = new HashMap<>();
                m.put("id_promocion", p.idPromocion);
                m.put("nombre", p.nombre);
                m.put("tipo", p.tipo);
                m.put("productos", p.productos);
                m.put("productos_texto", String.join(", ", p.nombresProductos));
                m.put("cantidad_requerida", p.cantidadRequerida);
                m.put("precio_especial", p.precioEspecial);
                m.put("descuento_porcentaje", p.descuentoPorcentaje);
                m.put("descuento_fijo", p.descuentoFijo);
                m.put("fecha_inicio", p.fechaInicio != null ? p.fechaInicio.toString() : null);
                m.put("fecha_fin", p.fechaFin != null ? p.fechaFin.toString() : null);
                m.put("activo", p.activo);
                lista.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtenerPorId(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try {
            PromocionDAO dao = new PromocionDAO();
            PromocionData p = dao.obtenerPorId(id);
            if (p != null) {
                res.put("id_promocion", p.idPromocion);
                res.put("nombre", p.nombre);
                res.put("tipo", p.tipo);
                res.put("productos", p.productos);
                res.put("productos_texto", String.join(", ", p.nombresProductos));
                res.put("cantidad_requerida", p.cantidadRequerida);
                res.put("precio_especial", p.precioEspecial);
                res.put("descuento_porcentaje", p.descuentoPorcentaje);
                res.put("descuento_fijo", p.descuentoFijo);
                res.put("fecha_inicio", p.fechaInicio != null ? p.fechaInicio.toString() : null);
                res.put("fecha_fin", p.fechaFin != null ? p.fechaFin.toString() : null);
                res.put("activo", p.activo);
                res.put("success", true);
            } else {
                res.put("success", false);
                res.put("message", "Promoción no encontrada");
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/guardar")
    public Map<String, Object> guardar(@RequestBody Map<String, Object> datos) {
        Map<String, Object> res = new HashMap<>();
        try {
            PromocionRequest request = new PromocionRequest();
            request.nombre = datos.get("nombre").toString();
            request.tipo = datos.get("tipo").toString();
            request.cantidadRequerida = Double.parseDouble(datos.get("cantidad_requerida").toString());
            
            if (datos.get("productos") instanceof List) {
                List<?> prods = (List<?>) datos.get("productos");
                request.productos = new HashSet<>();
                for (Object prod : prods) {
                    if (prod instanceof Number) {
                        request.productos.add(((Number) prod).intValue());
                    } else if (prod instanceof String) {
                        request.productos.add(Integer.parseInt(prod.toString()));
                    }
                }
            }
            
            request.precioEspecial = datos.get("precio_especial") != null && !datos.get("precio_especial").toString().isEmpty()
                ? Double.parseDouble(datos.get("precio_especial").toString()) : null;
            request.descuentoPorcentaje = datos.get("descuento_porcentaje") != null && !datos.get("descuento_porcentaje").toString().isEmpty()
                ? Double.parseDouble(datos.get("descuento_porcentaje").toString()) : null;
            request.descuentoFijo = datos.get("descuento_fijo") != null && !datos.get("descuento_fijo").toString().isEmpty()
                ? Double.parseDouble(datos.get("descuento_fijo").toString()) : null;
            request.fechaInicio = datos.get("fecha_inicio") != null && !datos.get("fecha_inicio").toString().isEmpty()
                ? datos.get("fecha_inicio").toString() : null;
            request.fechaFin = datos.get("fecha_fin") != null && !datos.get("fecha_fin").toString().isEmpty()
                ? datos.get("fecha_fin").toString() : null;
            request.activo = datos.get("activo") != null ? Boolean.parseBoolean(datos.get("activo").toString()) : true;

            System.out.println("Datos recibidos para promocion: " + request.nombre + ", tipo: " + request.tipo + ", productos: " + request.productos);

            PromocionDAO dao = new PromocionDAO();
            boolean success = false;
            String id = datos.get("id_promocion") != null ? datos.get("id_promocion").toString() : "";
            if (!id.isEmpty()) {
                success = dao.actualizar(Integer.parseInt(id), request);
                res.put("message", success ? "Promoción actualizada correctamente" : "Error al actualizar promoción");
                System.out.println("Actualizando promoción ID: " + id + ", success: " + success);
            } else {
                success = dao.crear(request);
                res.put("message", success ? "Promoción creada correctamente" : "Error al crear promoción");
                System.out.println("Creando nueva promoción, success: " + success);
            }

            res.put("success", success);
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    @DeleteMapping("/eliminar/{id}")
    public Map<String, Object> eliminar(@PathVariable int id) {
        Map<String, Object> res = new HashMap<>();
        try {
            PromocionDAO dao = new PromocionDAO();
            boolean success = dao.eliminar(id);
            res.put("success", success);
            res.put("message", success ? "Promoción eliminada correctamente" : "Error al eliminar promoción");
        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    @GetMapping("/activas/producto/{idProducto}")
    public List<Map<String, Object>> obtenerActivasPorProducto(@PathVariable int idProducto) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            PromocionDAO dao = new PromocionDAO();
            List<PromocionData> promociones = dao.obtenerPromocionesActivasPorProducto(idProducto);
            for (PromocionData p : promociones) {
                Map<String, Object> m = new HashMap<>();
                m.put("id_promocion", p.idPromocion);
                m.put("nombre", p.nombre);
                m.put("tipo", p.tipo);
                m.put("productos", p.productos);
                m.put("productos_texto", String.join(", ", p.nombresProductos));
                m.put("cantidad_requerida", p.cantidadRequerida);
                m.put("precio_especial", p.precioEspecial);
                m.put("descuento_porcentaje", p.descuentoPorcentaje);
                m.put("descuento_fijo", p.descuentoFijo);
                lista.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    @GetMapping("/activas/todas")
    public List<Map<String, Object>> obtenerTodasActivas() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            PromocionDAO dao = new PromocionDAO();
            List<PromocionData> promociones = dao.obtenerTodas();
            for (PromocionData p : promociones) {
                if (p.activo) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id_promocion", p.idPromocion);
                    m.put("nombre", p.nombre);
                    m.put("tipo", p.tipo);
                    m.put("productos", p.productos);
                    m.put("cantidad_requerida", p.cantidadRequerida);
                    m.put("precio_especial", p.precioEspecial);
                    m.put("descuento_porcentaje", p.descuentoPorcentaje);
                    m.put("descuento_fijo", p.descuentoFijo);
                    lista.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}