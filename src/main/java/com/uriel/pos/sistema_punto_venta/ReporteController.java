package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @GetMapping("/ventas")
    public List<Map<String, Object>> getVentas(
            @RequestParam String inicio,
            @RequestParam(defaultValue = "00:00") String horaInicio,
            @RequestParam String fin,
            @RequestParam(defaultValue = "23:59") String horaFin,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerVentas(inicio, horaInicio, fin, horaFin, pagina, tamano);
    }

    @GetMapping("/detalle/{id}")
    public List<Map<String, Object>> getDetalleVenta(@PathVariable int id) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerDetalleVenta(id);
    }

    @GetMapping("/productos")
    public List<Map<String, Object>> getTopProductos(
            @RequestParam String inicio,
            @RequestParam(defaultValue = "00:00") String horaInicio,
            @RequestParam String fin,
            @RequestParam(defaultValue = "23:59") String horaFin,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int tamano,
            @RequestParam(defaultValue = "") String busqueda) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerTopProductos(inicio, horaInicio, fin, horaFin, pagina, tamano, busqueda);
    }

    @GetMapping("/cajeros")
    public List<Map<String, Object>> getVentasPorCajero(
            @RequestParam String inicio,
            @RequestParam(defaultValue = "00:00") String horaInicio,
            @RequestParam String fin,
            @RequestParam(defaultValue = "23:59") String horaFin) {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerVentasPorCajero(inicio, horaInicio, fin, horaFin);
    }

    @GetMapping("/inventario")
    public Map<String, Object> getValorInventario() {
        ReporteDAO dao = new ReporteDAO();
        return dao.obtenerValorInventario();
    }
}