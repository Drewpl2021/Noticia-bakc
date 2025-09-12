package com.example.demo.service.impl;

import com.example.demo.entity.Articulo;
import com.example.demo.repository.ArticuloRepository;
import com.example.demo.service.ArticuloService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ArticuloServiceImpl implements ArticuloService {

    private final ArticuloRepository repo;

    @Override
    public Page<Articulo> listar(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public Optional<Articulo> porId(Long id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Articulo> porUrl(String url) {
        return repo.findByUrl(url);
    }

    @Override
    public Page<Articulo> buscarTexto(String q, Pageable pageable) {
        String s = q == null ? "" : q.trim();
        return repo.findByTituloContainingIgnoreCaseOrContenidoContainingIgnoreCaseOrTagsContainingIgnoreCaseOrCategoriasContainingIgnoreCase(
                s, s, s, s, pageable
        );
    }

    @Override
    public Page<Articulo> porRangoFecha(LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        if (desde == null) desde = LocalDateTime.of(1970,1,1,0,0);
        if (hasta == null)  hasta  = LocalDateTime.now();
        return repo.findByFechaPublicadoBetween(desde, hasta, pageable);
    }

    @Override
    public Page<Articulo> porCategoria(String categoria, Pageable pageable) {
        return repo.findByCategoriasContainingIgnoreCase(categoria == null ? "" : categoria, pageable);
    }

    @Override
    public Page<Articulo> porTag(String tag, Pageable pageable) {
        return repo.findByTagsContainingIgnoreCase(tag == null ? "" : tag, pageable);
    }



    @Override
    public List<String> listarCategoriasUnicas() {
        List<String> filas = repo.findAllCategoriasRaw();
        if (filas == null || filas.isEmpty()) return List.of();

        // canonical -> display (primer formato visto)
        Map<String, String> canonToDisplay = new LinkedHashMap<>();

        for (String fila : filas) {
            if (fila == null || fila.isBlank()) continue;

            // Split por comas (puedes ajustar el separador si usas otro)
            String[] parts = fila.split("\\s*,\\s*");
            for (String p : parts) {
                String display = p.trim();
                if (display.isEmpty()) continue;

                String key = canonical(display);
                // guarda el primer formato encontrado para ese canonical
                canonToDisplay.putIfAbsent(key, display);
            }
        }

        return new ArrayList<>(canonToDisplay.values());
    }

    /** Normaliza tildes y mayúsculas para deduplicar: "Policía" ~ "policia" */
    private static String canonical(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", ""); // quita diacríticos
        n = n.toLowerCase(Locale.ROOT).trim();
        n = n.replaceAll("\\s+", " ");     // colapsa espacios
        return n;
    }

    // com/example/demo/service/ArticuloServiceImpl.java
    @Override
    public Page<Articulo> filtrarPorCategorias(List<String> categorias, Pageable pageable) {
        if (categorias == null || categorias.isEmpty()) {
            return repo.findAll(pageable);
        }

        // normalizamos valores
        List<String> terms = categorias.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();

        if (terms.isEmpty()) {
            return repo.findAll(pageable);
        }

        // AQUÍ va el bloque del Specification
        Specification<Articulo> spec = (root, query, cb) -> {
            List<Predicate> orPreds = new ArrayList<>();
            for (String t : terms) {
                orPreds.add(cb.like(root.get("categorias"), "%" + t + "%")); // sin lower()
            }
            return cb.and(
                    cb.isNotNull(root.get("categorias")),
                    cb.or(orPreds.toArray(new Predicate[0]))
            );
        };

        return repo.findAll(spec, pageable);
    }

}
