package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @GetMapping("/ventas")
    public List<Map<String, Object>> getVentas(
            @RequestParam String inicio, 
            @RequestParam String fin,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerVentas(inicio, fin, pagina, tamano);
    }

    @GetMapping("/detalle/{id}")
    public List<Map<String, Object>> getDetalleVenta(@PathVariable int id) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerDetalleVenta(id);
    }

    @GetMapping("/productos")
    public List<Map<String, Object>> getTopProductos(
            @RequestParam String inicio, 
            @RequestParam String fin,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerTopProductos(inicio, fin, pagina, tamano);
    }

    @GetMapping("/cajeros")
    public List<Map<String, Object>> getVentasPorCajero(@RequestParam String inicio, @RequestParam String fin) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerVentasPorCajero(inicio, fin);
    }
}