package com.example.demo.service.impl;

import com.example.demo.entity.Articulo;
import com.example.demo.repository.ArticuloRepository;
import com.example.demo.service.ArticuloService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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

    @Override
    public byte[] exportarCsvArticulos() {
        // todos los artículos, ordenados por fechaPublicado desc
        List<Articulo> articulos = repo.findAll(Sort.by(Sort.Direction.DESC, "fechaPublicado"));
        return toCsv(articulos);
    }

    @Override
    public byte[] exportarCsvPorCategorias(List<String> categorias) {
        if (categorias == null || categorias.isEmpty()) {
            // si no mandan categorías, exporta todo
            List<Articulo> articulos = repo.findAll(Sort.by(Sort.Direction.DESC, "fechaPublicado"));
            return toCsv(articulos);
        }

        // Reutilizamos tu Specification, pero sin paginación (unpaged)
        Page<Articulo> page = filtrarPorCategorias(categorias, Pageable.unpaged());
        List<Articulo> articulos = page.getContent();

        return toCsv(articulos);
    }

    // 🔹 Lógica central para generar el CSV (reutilizable)
    private byte[] toCsv(List<Articulo> articulos) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PrintWriter writer =
                     new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            // BOM para que Excel lea bien UTF-8
            writer.write('\uFEFF');

            // Cabeceras CSV: ajusta a los campos reales de tu entidad
            writer.println("id,titulo,autor,fechaPublicado,url,imagenUrl,categorias,tags");

            for (Articulo a : articulos) {
                writer.println(
                        csv(a.getId()) + "," +
                                csv(a.getTitulo()) + "," +
                                csv(a.getAutor()) + "," +
                                csv(a.getFechaPublicado() != null ? a.getFechaPublicado().toString() : "") + "," +
                                csv(a.getUrl()) + "," +
                                csv(a.getImagenUrl()) + "," +
                                csv(a.getCategorias()) + "," +
                                csv(a.getTags())
                );
            }

            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar CSV de artículos", e);
        }

        return baos.toByteArray();
    }

    /**
     * Escapa comas, comillas y saltos de línea para formato CSV.
     */
    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        boolean hasSpecial = text.contains(",") || text.contains("\"")
                || text.contains("\n") || text.contains("\r");
        if (hasSpecial) {
            text = text.replace("\"", "\"\""); // escapa comillas dobles
            return "\"" + text + "\"";
        }
        return text;
    }
    @Override
    public byte[] exportarCsvPorFecha(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null) desde = LocalDateTime.of(1970, 1, 1, 0, 0);
        if (hasta == null) hasta = LocalDateTime.now();

        List<Articulo> articulos = repo.findByFechaPublicadoBetween(desde, hasta, Sort.by(Sort.Direction.DESC, "fechaPublicado"));
        return toCsv(articulos);
    }
    @Override
    public byte[] exportarCsvPorFechaYCategorias(LocalDateTime desde,
                                                 LocalDateTime hasta,
                                                 List<String> categorias) {

        // Normalizas primero
        LocalDateTime desdeFinal = (desde != null)
                ? desde
                : LocalDateTime.of(1970, 1, 1, 0, 0);

        LocalDateTime hastaFinal = (hasta != null)
                ? hasta
                : LocalDateTime.now();

        Specification<Articulo> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            // ✅ Usas las variables "finales"
            preds.add(cb.between(root.get("fechaPublicado"), desdeFinal, hastaFinal));

            if (categorias != null && !categorias.isEmpty()) {
                List<Predicate> orCats = new ArrayList<>();
                for (String c : categorias) {
                    orCats.add(cb.like(root.get("categorias"), "%" + c + "%"));
                }
                preds.add(cb.or(orCats.toArray(new Predicate[0])));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };

        List<Articulo> articulos =
                repo.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaPublicado"));

        return toCsv(articulos);
    }


}
