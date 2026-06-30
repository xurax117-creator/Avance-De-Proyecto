package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {

    public static class LoginRequest {
        public String alias;
        public String pass;
        public int idSucursal;
    }

    @PostMapping("/login")
    public Map<String, Object> handleLogin(@RequestBody LoginRequest request) {

        Login log = new Login();

        Map<String, Object> datosUsuario = log.autenticar(request.alias, request.pass, request.idSucursal);

        Map<String, Object> response = new HashMap<>();

        if (datosUsuario != null && !datosUsuario.isEmpty()) {
            response.put("success", true);
            response.put("message", "Acceso concedido");

            response.put("userId", datosUsuario.get("id"));
            response.put("nombre", datosUsuario.get("nombre"));
            response.put("rol", datosUsuario.get("rol"));

            System.out.println("Login exitoso para: " + datosUsuario.get("nombre"));
        } else {
            response.put("success", false);
            response.put("message", "Usuario o contraseña incorrectos, o no tienes acceso a esta sucursal.");
        }

        return response;
    }
}
