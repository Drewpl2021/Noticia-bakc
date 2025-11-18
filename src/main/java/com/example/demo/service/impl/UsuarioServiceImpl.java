package com.example.demo.service.impl;

import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // === CREATE ===
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Usuario crear(Usuario usuario) {
        // Encriptar contraseña antes de guardar
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));

        return usuarioRepository.save(usuario);
    }

    // === READ: listar todos ===
    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // === READ: por id ===
    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    // === UPDATE ===
    @Override
    public Usuario actualizar(Long id, Usuario usuario) {
        return usuarioRepository.findById(id)
                .map(existing -> {

                    existing.setName(usuario.getName());
                    existing.setLastName(usuario.getLastName());
                    existing.setNumber(usuario.getNumber());
                    existing.setUsername(usuario.getUsername());
                    existing.setEmail(usuario.getEmail());
                    existing.setRol(usuario.getRol());

                    // Si envías una nueva contraseña, se encripta
                    if (usuario.getPasswordHash() != null && !usuario.getPasswordHash().isBlank()) {
                        existing.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
                    }

                    return usuarioRepository.save(existing);
                })
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado con id: " + id));
    }


    // === DELETE ===
    @Override
    public void eliminar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con id: " + id);
        }
        usuarioRepository.deleteById(id);
    }
}
