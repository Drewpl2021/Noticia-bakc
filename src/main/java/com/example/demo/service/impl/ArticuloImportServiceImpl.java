// com/example/demo/service/impl/ArticuloImportServiceImpl.java
package com.example.demo.service.impl;

import com.example.demo.dto.ArticuloImportResult;
import com.example.demo.dto.CsvAnalysisResult;
import com.example.demo.dto.CsvColumnAnalysis;
import com.example.demo.entity.Articulo;
import com.example.demo.repository.ArticuloRepository;
import com.example.demo.service.ArticuloImportService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ArticuloImportServiceImpl implements ArticuloImportService {

    private final ArticuloRepository repo;

    private static final DateTimeFormatter ISO_DTF = DateTimeFormatter.ISO_DATE_TIME;

    // ==================== 1) IMPORT + ETL + MÉTRICAS (lo que ya tenías) ====================

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

                    if (url.isBlank()) {
                        skipped++;
                        continue;
                    }

                    // ====== TRANSFORM ======
                    titulo = normalize(titulo);
                    autor = normalize(autor);
                    contenido = normalize(contenido);
                    imagenUrl = normalize(imagenUrl);
                    categorias = normalize(categorias);
                    tags = normalize(tags);

                    LocalDateTime fechaPublicado = null;
                    if (fechaStr != null && !fechaStr.isBlank()) {
                        try {
                            fechaPublicado = LocalDateTime.parse(fechaStr, ISO_DTF);
                        } catch (Exception e) {
                            // dejalo null
                        }
                    }

                    // ====== LOAD ======
                    Optional<Articulo> opt = repo.findByUrl(url);
                    Articulo articulo;
                    if (opt.isPresent()) {
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
                        articulo = new Articulo();
                        articulo.setUrl(url);
                        articulo.setTitulo(titulo);
                        articulo.setAutor(autor);
                        articulo.setContenido(contenido);
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
                }
            }

        } catch (Exception e) {
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

    // ==================== 2) SOLO ANÁLISIS (sin tocar BD) ====================

    @Override
    public CsvAnalysisResult analizarCsv(MultipartFile file) {
        int total = 0;

        // nulls por columna
        Map<String, Integer> nullCounts = new HashMap<>();
        // conteo de valores por columna (para duplicados)
        Map<String, Map<String, Integer>> valueCounts = new HashMap<>();

        try (InputStreamReader reader =
                     new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {

            List<String> headers = parser.getHeaderNames();

            for (CSVRecord row : parser) {
                total++;

                for (String col : headers) {
                    String raw = row.isMapped(col) ? row.get(col) : null;
                    String val = (raw == null ? "" : raw.trim());

                    if (val.isEmpty()) {
                        nullCounts.put(col, nullCounts.getOrDefault(col, 0) + 1);
                    } else {
                        Map<String, Integer> vc =
                                valueCounts.computeIfAbsent(col, k -> new HashMap<>());
                        vc.put(val, vc.getOrDefault(val, 0) + 1);
                    }
                }
            }

            // construir resultado columna por columna
            List<CsvColumnAnalysis> cols = new ArrayList<>();
            for (String col : valueCounts.keySet()) {
                int nullCount = nullCounts.getOrDefault(col, 0);
                double nullPct = (total > 0) ? (nullCount * 1.0 / total) : 0.0;

                Map<String, Integer> vc = valueCounts.getOrDefault(col, Collections.emptyMap());
                int dupCount = 0;
                for (int c : vc.values()) {
                    if (c > 1) {
                        dupCount += (c - 1); // número de filas “extra” repetidas
                    }
                }
                double dupPct = (total > 0) ? (dupCount * 1.0 / total) : 0.0;

                cols.add(CsvColumnAnalysis.builder()
                        .name(col)
                        .nullCount(nullCount)
                        .nullPercent(nullPct)
                        .duplicateCount(dupCount)
                        .duplicatePercent(dupPct)
                        .build());
            }

            return CsvAnalysisResult.builder()
                    .totalRows(total)
                    .columns(cols)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error analizando CSV", e);
        }
    }

    // ==================== 3) APLICAR ETL SOLO A COLUMNAS SELECCIONADAS + EXPORTAR CSV ====================

    @Override
    public byte[] aplicarEtlYExportar(MultipartFile file, Set<String> columnas) {
        // si columnas es null o vacio => usar todas las lógicas conocidas del Articulo
        boolean allColumns = (columnas == null || columnas.isEmpty());

        // columnas lógicas esperadas
        List<String> knownCols = List.of(
                "titulo", "autor", "contenido",
                "url", "imagenUrl", "categorias",
                "tags", "fechaPublicado"
        );

        // columnas efectivamente a exportar (en el orden conocido)
        List<String> exportCols = new ArrayList<>();
        for (String c : knownCols) {
            if (allColumns || columnas.stream().anyMatch(cc -> cc.equalsIgnoreCase(c))) {
                exportCols.add(c);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (InputStreamReader reader =
                     new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader);
             PrintWriter writer =
                     new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            // ====== HEADER CSV EXPORTADO ======
            writer.write('\uFEFF'); // BOM UTF-8
            writer.println(String.join(",", exportCols));

            for (CSVRecord row : parser) {
                try {
                    String titulo      = safe(row, "titulo");
                    String autor       = safe(row, "autor");
                    String contenido   = safe(row, "contenido");
                    String url         = safe(row, "url");
                    String imagenUrl   = safe(row, "imagenUrl");
                    String categorias  = safe(row, "categorias");
                    String tags        = safe(row, "tags");
                    String fechaStr    = safe(row, "fechaPublicado");

                    if (url.isBlank()) {
                        // sin URL no podemos usarlo como clave de Articulo
                        continue;
                    }

                    // TRANSFORM
                    titulo = normalize(titulo);
                    autor = normalize(autor);
                    contenido = normalize(contenido);
                    imagenUrl = normalize(imagenUrl);
                    categorias = normalize(categorias);
                    tags = normalize(tags);

                    LocalDateTime fechaPublicado = null;
                    if (fechaStr != null && !fechaStr.isBlank()) {
                        try {
                            fechaPublicado = LocalDateTime.parse(fechaStr, ISO_DTF);
                        } catch (Exception e) {
                            // ignora formato raro
                        }
                    }

                    // ====== LOAD -> BD (solo columnas seleccionadas) ======
                    Optional<Articulo> opt = repo.findByUrl(url);
                    Articulo articulo;

                    boolean importTitulo     = shouldImport("titulo", columnas);
                    boolean importAutor      = shouldImport("autor", columnas);
                    boolean importContenido  = shouldImport("contenido", columnas);
                    boolean importImagenUrl  = shouldImport("imagenUrl", columnas);
                    boolean importCategorias = shouldImport("categorias", columnas);
                    boolean importTags       = shouldImport("tags", columnas);
                    boolean importFecha      = shouldImport("fechaPublicado", columnas);

                    if (opt.isPresent()) {
                        articulo = opt.get();
                        if (importTitulo)     articulo.setTitulo(titulo);
                        if (importAutor)      articulo.setAutor(autor);
                        if (importContenido)  articulo.setContenido(contenido);
                        if (importImagenUrl)  articulo.setImagenUrl(imagenUrl);
                        if (importCategorias) articulo.setCategorias(categorias);
                        if (importTags)       articulo.setTags(tags);
                        if (importFecha && fechaPublicado != null) {
                            articulo.setFechaPublicado(fechaPublicado);
                        }
                    } else {
                        articulo = new Articulo();
                        articulo.setUrl(url);

                        if (importTitulo)     articulo.setTitulo(titulo);
                        if (importAutor)      articulo.setAutor(autor);
                        if (importContenido)  articulo.setContenido(contenido);
                        if (importImagenUrl)  articulo.setImagenUrl(imagenUrl);
                        if (importCategorias) articulo.setCategorias(categorias);
                        if (importTags)       articulo.setTags(tags);

                        articulo.setFechaPublicado(
                                (importFecha && fechaPublicado != null)
                                        ? fechaPublicado
                                        : LocalDateTime.now()
                        );
                    }
                    repo.save(articulo);

                    // ====== ESCRIBIR FILA EN CSV EXPORTADO ======
                    List<String> rowOut = new ArrayList<>();
                    for (String col : exportCols) {
                        String val;
                        switch (col) {
                            case "titulo"        -> val = titulo;
                            case "autor"         -> val = autor;
                            case "contenido"     -> val = contenido;
                            case "url"           -> val = url;
                            case "imagenUrl"     -> val = imagenUrl;
                            case "categorias"    -> val = categorias;
                            case "tags"          -> val = tags;
                            case "fechaPublicado"-> val = (fechaPublicado != null
                                    ? fechaPublicado.toString() : "");
                            default              -> val = "";
                        }
                        rowOut.add(csv(val));
                    }
                    writer.println(String.join(",", rowOut));

                } catch (Exception ex) {
                    // puedes loguear errores por fila si quieres
                }
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error aplicando ETL y exportando CSV", e);
        }
    }

    // ==================== HELPERS ====================

    private String safe(CSVRecord row, String column) {
        String val = row.isMapped(column) ? row.get(column) : "";
        return val == null ? "" : val;
    }

    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        return s;
    }

    private boolean shouldImport(String col, Set<String> columnas) {
        if (columnas == null || columnas.isEmpty()) return true;
        return columnas.stream().anyMatch(c -> c.equalsIgnoreCase(col));
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        boolean hasSpecial = text.contains(",") || text.contains("\"")
                || text.contains("\n") || text.contains("\r");
        if (hasSpecial) {
            text = text.replace("\"", "\"\"");
            return "\"" + text + "\"";
        }
        return text;
    }
}
