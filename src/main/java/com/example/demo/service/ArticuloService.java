package com.example.demo.service;

import com.example.demo.entity.Articulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticuloService {
    Page<Articulo> listar(Pageable pageable);
    Optional<Articulo> porId(Long id);
    Optional<Articulo> porUrl(String url);
    Page<Articulo> buscarTexto(String q, Pageable pageable);
    Page<Articulo> porRangoFecha(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    Page<Articulo> porCategoria(String categoria, Pageable pageable);
    Page<Articulo> porTag(String tag, Pageable pageable);
    List<String> listarCategoriasUnicas();
    Page<Articulo> filtrarPorCategorias(List<String> categorias, Pageable pageable);

}
