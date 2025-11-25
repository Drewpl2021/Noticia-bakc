package com.example.demo.repository;

import com.example.demo.entity.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    // Todas las membresías de un usuario
    List<Membresia> findByUsuarioId(Long usuarioId);

    // Membresías de un usuario con cierto estado, por ejemplo ACTIVA
    List<Membresia> findByUsuarioIdAndEstado(Long usuarioId, String estado);

    boolean existsByUsuarioUsernameAndEstado(String username, String estado);

    @Query("SELECT m FROM Membresia m JOIN FETCH m.producto WHERE m.usuario.id = :usuarioId")
    List<Membresia> findByUsuarioIdWithProducto(@Param("usuarioId") Long usuarioId);
}
