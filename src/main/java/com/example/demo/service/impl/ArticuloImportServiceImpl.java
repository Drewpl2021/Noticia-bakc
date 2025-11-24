package com.example.demo.service.impl;

import com.example.demo.dto.ArticuloImportResult;
import com.example.demo.entity.Articulo;
import com.example.demo.repository.ArticuloRepository;
import com.example.demo.service.ArticuloImportService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticuloImportServiceImpl implements ArticuloImportService {

    private final ArticuloRepository repo;

    private static final DateTimeFormatter ISO_DTF = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public ArticuloImportResult importarDesdeCsv(MultipartFile file) {
        int total = 0;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int errors = 0;

        try (InputStreamReader reader =
                     new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {

            for (CSVRecord row : parser) {
                total++;

                try {
                    // ====== EXTRACT ======
                    String titulo      = safe(row, "titulo");
                    String autor       = safe(row, "autor");
                    String contenido   = safe(row, "contenido");
                    String url         = safe(row, "url");
                    String imagenUrl   = safe(row, "imagenUrl");
                    String categorias  = safe(row, "categorias");
                    String tags        = safe(row, "tags");
                    String fechaStr    = safe(row, "fechaPublicado");

                    // Si no hay URL, no podemos usarlo como clave -> omitir
                    if (url.isBlank()) {
                        skipped++;
                        continue;
                    }

                    // ====== TRANSFORM ======
                    // Limpieza básica
                    titulo = titulo.trim();
                    autor = autor.trim();
                    contenido = contenido.trim();
                    imagenUrl = imagenUrl.trim();
                    categorias = categorias.trim();
                    tags = tags.trim();

                    // Normalizar strings vacíos a null si quieres
                    if (titulo.isEmpty()) titulo = null;
                    if (autor.isEmpty()) autor = null;
                    if (contenido.isEmpty()) contenido = null;
                    if (imagenUrl.isEmpty()) imagenUrl = null;
                    if (categorias.isEmpty()) categorias = null;
                    if (tags.isEmpty()) tags = null;

                    LocalDateTime fechaPublicado = null;
                    if (!fechaStr.isBlank()) {
                        // intenta parsear en ISO-8601, ej: 2025-11-20T10:15:30
                        try {
                            fechaPublicado = LocalDateTime.parse(fechaStr, ISO_DTF);
                        } catch (Exception e) {
                            // si falla, podrías implementar otros formatos
                            // por ahora lo dejamos en null
                        }
                    }

                    // ====== LOAD ======
                    Optional<Articulo> opt = repo.findByUrl(url);
                    Articulo articulo;

                    if (opt.isPresent()) {
                        // UPDATE
                        articulo = opt.get();
                        articulo.setTitulo(titulo);
                        articulo.setAutor(autor);
                        articulo.setContenido(contenido);
                        articulo.setImagenUrl(imagenUrl);
                        articulo.setCategorias(categorias);
                        articulo.setTags(tags);
                        if (fechaPublicado != null) {
                            articulo.setFechaPublicado(fechaPublicado);
                        }
                        updated++;
                    } else {
                        // INSERT
                        articulo = new Articulo();
                        articulo.setTitulo(titulo);
                        articulo.setAutor(autor);
                        articulo.setContenido(contenido);
                        articulo.setUrl(url);
                        articulo.setImagenUrl(imagenUrl);
                        articulo.setCategorias(categorias);
                        articulo.setTags(tags);
                        articulo.setFechaPublicado(
                                fechaPublicado != null ? fechaPublicado : LocalDateTime.now()
                        );
                        inserted++;
                    }

                    repo.save(articulo);

                } catch (Exception ex) {
                    errors++;
                    // aquí podrías loguear la fila problemática
                }
            }

        } catch (Exception e) {
            // error general al leer el archivo
            throw new RuntimeException("Error al procesar el CSV de artículos", e);
        }

        return ArticuloImportResult.builder()
                .totalRows(total)
                .inserted(inserted)
                .updated(updated)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    private String safe(CSVRecord row, String column) {
        String val = row.isMapped(column) ? row.get(column) : "";
        return val == null ? "" : val;
    }
}
