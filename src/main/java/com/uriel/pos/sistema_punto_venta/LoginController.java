package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController 
@RequestMapping("/api") 
@CrossOrigin(origins = "*") // Permite peticiones desde el frontend si están en puertos distintos
public class LoginController {

    // Clase interna para mapear el JSON que viene del navegador (alias y pass)
    public static class LoginRequest {
        public String alias;
        public String pass;
    }

    @PostMapping("/login") 
    public Map<String, Object> handleLogin(@RequestBody LoginRequest request) {
        
        Login log = new Login();
        
        // Llamamos al método autenticar que ahora devuelve el Map con (id, nombre, rol)
        Map<String, Object> datosUsuario = log.autenticar(request.alias, request.pass);

        Map<String, Object> response = new HashMap<>();

        // Verificamos que el mapa no sea nulo y que contenga datos
        if (datosUsuario != null && !datosUsuario.isEmpty()) {
            response.put("success", true);
            response.put("message", "Acceso concedido");
            
            // Enviamos los datos que el frontend guardará en localStorage
            response.put("userId", datosUsuario.get("id"));
            response.put("nombre", datosUsuario.get("nombre"));
            response.put("rol", datosUsuario.get("rol"));
            
            System.out.println("Login exitoso para: " + datosUsuario.get("nombre"));
        } else {
            // Si el mapa está vacío, las credenciales fallaron o el usuario está inactivo
            response.put("success", false);
            response.put("message", "Usuario o contraseña incorrectos, o cuenta inactiva.");
        }
        
        return response; 
    }
}