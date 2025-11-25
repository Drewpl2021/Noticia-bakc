package com.example.demo.controller;

import com.example.demo.entity.Membresia;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.MembresiaRepository;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.security.JwtTokenUtil;
import com.example.demo.security.dto.LoginRequest;
import com.example.demo.security.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;

    // ⭐ Nuevo: inyectamos el repo de membresías
    private final MembresiaRepository membresiaRepository;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
        );

        Authentication authenticated = authenticationManager.authenticate(authToken);

        // Aquí ya está autenticado
        CustomUserDetails principal = (CustomUserDetails) authenticated.getPrincipal();
        Usuario usuario = principal.getUsuario();

        // Rol
        String roleName = null;
        Rol rol = usuario.getRol();
        if (rol != null) {
            roleName = rol.getNombre();
        }

        // ⭐ Buscar membresía ACTIVA del usuario
        // En AuthController
        List<Membresia> membresias = membresiaRepository.findByUsuarioIdWithProducto(usuario.getId());

        Membresia membresiaActiva = membresias.isEmpty() ? null : membresias.get(0);
        String planName = obtenerNombrePlan(membresiaActiva);

        // ⭐ Generar token con role y plan como claims
        String token = jwtTokenUtil.generateToken(usuario.getUsername(), roleName, planName);

        // ⭐ Devolver también el plan en la respuesta
        return new LoginResponse(
                token,
                usuario.getName(),
                usuario.getLastName(),
                usuario.getId(),
                roleName,
                planName
        );
    }

    /**
     * Aquí SOLO cambia la línea de retorno según tu entidad Membresia.
     */
    private String obtenerNombrePlan(Membresia membresia) {
        if (membresia == null) return null;
        if (membresia.getProducto() == null) return null;

        return membresia.getProducto().getNombre(); // aquí está el nombre del plan
    }

}
