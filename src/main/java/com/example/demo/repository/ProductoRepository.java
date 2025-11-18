package com.example.demo.repository;

import com.example.demo.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByEstado(String estado);

    List<Producto> findByTipo(String tipo);

    boolean existsByNombre(String nombre);
    Optional<Producto> findByNombre(String nombre);
}
