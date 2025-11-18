package com.example.demo.controller;

import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.security.JwtTokenUtil;
import com.example.demo.security.dto.LoginRequest;
import com.example.demo.security.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;

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

        String token = jwtTokenUtil.generateToken(usuario.getUsername());

        String roleName = null;
        Rol rol = usuario.getRol();
        if (rol != null) {
            roleName = rol.getNombre();
        }

        return new LoginResponse(
                token,
                usuario.getName(),
                usuario.getLastName(),
                roleName
        );
    }
}
