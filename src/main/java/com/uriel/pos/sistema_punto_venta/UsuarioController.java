package com.uriel.pos.sistema_punto_venta;

import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    public static class UsuarioRequest {
        public int id;
        public String nombreCompleto;
        public String alias;
        public String contraseña;
        public String rol;
        public Boolean activo;
        public String fotoPerfil;
        public int idSucursal;
    }

    @GetMapping("/listar")
    public Map<String, Object> listarUsuarios(@RequestParam(defaultValue = "1") int sucursal) {
        Usuario oper = new Usuario();
        Map<String, Object> response = new HashMap<>();
        try {
            List<UsuarioData> usuarios = oper.obtenerTodos(sucursal);
            response.put("success", true);
            response.put("data", usuarios);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener usuarios.");
        }
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtenerUsuario(@PathVariable int id) {
        Usuario oper = new Usuario();
        Map<String, Object> response = new HashMap<>();
        try {
            UsuarioData usuario = oper.obtenerPorId(id);
            if (usuario != null) {
                response.put("success", true);
                response.put("data", usuario);
            } else {
                response.put("success", false);
                response.put("message", "Usuario no encontrado.");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @PostMapping("/crear")
    public Map<String, Object> crearUsuario(@RequestBody UsuarioRequest request) {
        Usuario oper = new Usuario();
        Map<String, Object> response = new HashMap<>();
        try {
            if (request.nombreCompleto == null || request.nombreCompleto.trim().isEmpty() ||
                request.alias == null || request.alias.trim().isEmpty() ||
                request.contraseña == null || request.contraseña.trim().isEmpty() ||
                request.rol == null || request.rol.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Todos los campos son requeridos.");
                return response;
            }

            String rol = request.rol.trim();
            if (!rol.equals("Administrador") && !rol.equals("Gerente") && !rol.equals("Cajero")) {
                response.put("success", false);
                response.put("message", "Rol inválido. Debe ser: Administrador, Gerente o Cajero.");
                return response;
            }

            byte[] fotoBytes = null;
            if (request.fotoPerfil != null && !request.fotoPerfil.isEmpty()) {
                try {
                    fotoBytes = Base64.getDecoder().decode(request.fotoPerfil);
                } catch (IllegalArgumentException e) {
                    response.put("success", false);
                    response.put("message", "Formato de imagen inválido.");
                    return response;
                }
            }

            int idSucursal = request.idSucursal > 0 ? request.idSucursal : 1;

            boolean resultado = oper.crearUsuario(
                request.nombreCompleto.trim(),
                request.alias.trim(),
                request.contraseña.trim(),
                rol,
                fotoBytes,
                idSucursal
            );

            if (resultado) {
                response.put("success", true);
                response.put("message", "Usuario creado correctamente.");
            } else {
                response.put("success", false);
                response.put("message", "Error al crear usuario. El alias ya podría existir.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @PutMapping("/actualizar")
    public Map<String, Object> actualizarUsuario(@RequestBody UsuarioRequest request) {
        Usuario oper = new Usuario();
        Map<String, Object> response = new HashMap<>();
        try {
            // La contraseña es opcional al actualizar: si se deja en blanco, se conserva la actual.
            if (request.id <= 0 ||
                request.nombreCompleto == null || request.nombreCompleto.trim().isEmpty() ||
                request.alias == null || request.alias.trim().isEmpty() ||
                request.rol == null || request.rol.trim().isEmpty() ||
                request.activo == null) {
                response.put("success", false);
                response.put("message", "Todos los campos son requeridos.");
                return response;
            }

            String rol = request.rol.trim();
            if (!rol.equals("Administrador") && !rol.equals("Gerente") && !rol.equals("Cajero")) {
                response.put("success", false);
                response.put("message", "Rol inválido. Debe ser: Administrador, Gerente o Cajero.");
                return response;
            }

            byte[] fotoBytes = null;
            if (request.fotoPerfil != null && !request.fotoPerfil.isEmpty()) {
                try {
                    fotoBytes = Base64.getDecoder().decode(request.fotoPerfil);
                } catch (IllegalArgumentException e) {
                    response.put("success", false);
                    response.put("message", "Formato de imagen inválido.");
                    return response;
                }
            }

            boolean resultado = oper.actualizarUsuario(
                request.id,
                request.nombreCompleto.trim(),
                request.alias.trim(),
                request.contraseña != null ? request.contraseña.trim() : null,
                rol,
                request.activo,
                fotoBytes
            );

            if (resultado) {
                response.put("success", true);
                response.put("message", "Usuario actualizado correctamente.");
            } else {
                response.put("success", false);
                response.put("message", "Error al actualizar usuario.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @DeleteMapping("/eliminar/{id}")
    public Map<String, Object> eliminarUsuario(@PathVariable int id) {
        Usuario oper = new Usuario();
        Map<String, Object> response = new HashMap<>();
        try {
            boolean resultado = oper.eliminarUsuario(id);
            if (resultado) {
                response.put("success", true);
                response.put("message", "Usuario eliminado correctamente.");
            } else {
                response.put("success", false);
                response.put("message", "Error al eliminar usuario.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }

    @PutMapping("/estado/{id}")
    public Map<String, Object> cambiarEstado(@PathVariable int id, @RequestBody UsuarioRequest request) {
        Usuario oper = new Usuario();
        Map<String, Object> response = new HashMap<>();
        try {
            if (request.activo == null) {
                response.put("success", false);
                response.put("message", "El estado activo es requerido.");
                return response;
            }

            boolean resultado = oper.cambiarActivo(id, request.activo);
            if (resultado) {
                response.put("success", true);
                response.put("message", request.activo ? "Usuario activado." : "Usuario desactivado.");
            } else {
                response.put("success", false);
                response.put("message", "Error al cambiar estado.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error en servidor.");
        }
        return response;
    }
}
