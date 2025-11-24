package com.example.demo.repository;

import com.example.demo.entity.Articulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticuloRepository extends JpaRepository<Articulo, Long>, JpaSpecificationExecutor<Articulo> {

    Optional<Articulo> findByUrl(String url);

    Page<Articulo> findByTituloContainingIgnoreCaseOrContenidoContainingIgnoreCaseOrTagsContainingIgnoreCaseOrCategoriasContainingIgnoreCase(
            String q1, String q2, String q3, String q4, Pageable pageable
    );

    Page<Articulo> findByFechaPublicadoBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Articulo> findByCategoriasContainingIgnoreCase(String categoria, Pageable pageable);

    Page<Articulo> findByTagsContainingIgnoreCase(String tag, Pageable pageable);

    @Query(value = "SELECT categorias FROM articulos WHERE categorias IS NOT NULL AND categorias <> ''", nativeQuery = true)
    List<String> findAllCategoriasRaw();

    List<Articulo> findByFechaPublicadoBetween(LocalDateTime desde, LocalDateTime hasta, Sort sort);

}
