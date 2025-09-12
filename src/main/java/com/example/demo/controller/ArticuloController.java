package com.example.demo.controller;

import com.example.demo.entity.Articulo;
import com.example.demo.service.ArticuloService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService service;

    @GetMapping
    public Page<Articulo> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaPublicado,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        return service.listar(pageable);
    }
    // com/example/demo/controller/ArticuloController.java
    @GetMapping("/noticias")
    public Page<Articulo> todasLasNoticias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaPublicado,desc") String sort,
            @RequestParam(name = "categoria", required = false) String categoria // ej: "Jherson" o "Jherson,Deportes"
    ) {
        Pageable pageable = toPageable(page, size, sort);

        if (categoria == null || categoria.isBlank()) {
            // sin categoría -> lista normal
            return service.listar(pageable);
        }

        // soporta 1 o varias categorías separadas por coma
        List<String> cats = Arrays.stream(categoria.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return service.filtrarPorCategorias(cats, pageable);
    }




    @GetMapping("/{id}")
    public Articulo porId(@PathVariable Long id) {
        return service.porId(id).orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
    }

    @GetMapping("/by-url")
    public Articulo porUrl(@RequestParam String url) {
        return service.porUrl(url).orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
    }

    @GetMapping("/search")
    public Page<Articulo> buscar(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaPublicado,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        return service.buscarTexto(q, pageable);
    }

    @GetMapping("/fecha")
    public Page<Articulo> porFecha(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaPublicado,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        return service.porRangoFecha(desde, hasta, pageable);
    }

    @GetMapping("/categoria")
    public Page<Articulo> porCategoria(
            @RequestParam("c") String categoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaPublicado,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        return service.porCategoria(categoria, pageable);
    }

    @GetMapping("/tag")
    public Page<Articulo> porTag(
            @RequestParam("t") String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaPublicado,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        return service.porTag(tag, pageable);
    }

    private Pageable toPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(dir, field));
    }
    @GetMapping("/categorias")
    public List<String> categoriasUnicas() {
        return service.listarCategoriasUnicas();
    }
}
