package com.example.demo.repository;

import com.example.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // === Búsquedas directas ===
    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByNumber(String number);

    // === Validaciones de existencia ===
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByNumber(String number);

    // === Búsqueda combinada opcional (nombre o apellido o username) ===
    List<Usuario> findByNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
            String name, String lastName, String username
    );
}
