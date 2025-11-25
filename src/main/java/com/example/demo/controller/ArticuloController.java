package com.example.demo.controller;

import com.example.demo.dto.ArticuloImportResult;
import com.example.demo.dto.CsvAnalysisResult;
import com.example.demo.entity.Articulo;
import com.example.demo.service.ArticuloImportService;
import com.example.demo.service.ArticuloService;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


@CrossOrigin(origins = "https://noticia-angular-qgv4.vercel.app")
@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService service;
    private final ArticuloImportService importService;

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
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaPublicado,desc") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);

        LocalDateTime desdeDt = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = (hasta != null) ? hasta.atTime(23, 59, 59) : null;

        return service.porRangoFecha(desdeDt, hastaDt, pageable);
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

    @GetMapping("/export-csv")
    public ResponseEntity<Resource> exportarCsv() {
        byte[] data = service.exportarCsvArticulos();
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"articulos.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/export-csv-por-categoria")
    public ResponseEntity<Resource> exportarCsvPorCategoria(
            @RequestParam(name = "categoria", required = false) String categoria // ej: "Deportes" o "Deportes,Politica"
    ) {
        List<String> categorias = null;

        if (categoria != null && !categoria.isBlank()) {
            categorias = Arrays.stream(categoria.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        byte[] data = service.exportarCsvPorCategorias(categorias);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"articulos_por_categoria.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/export-csv-fecha")
    public ResponseEntity<Resource> exportarCsvPorFecha(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        LocalDateTime d1 = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime d2 = (hasta != null) ? hasta.atTime(23, 59, 59) : null;

        byte[] data = service.exportarCsvPorFecha(d1, d2);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"articulos_fecha.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(data.length)
                .body(resource);
    }
    @GetMapping("/export-csv-filtros")
    public ResponseEntity<Resource> exportarCsvPorFiltros(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String categoria
    ) {
        LocalDateTime d1 = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime d2 = (hasta != null) ? hasta.atTime(23, 59, 59) : null;

        List<String> categorias = null;
        if (categoria != null && !categoria.isBlank()) {
            categorias = Arrays.stream(categoria.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        byte[] data = service.exportarCsvPorFechaYCategorias(d1, d2, categorias);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"articulos_filtros.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(data.length)
                .body(resource);
    }

    @PostMapping("/etl/analisis-csv")
    public ResponseEntity<CsvAnalysisResult> analizarCsv(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        CsvAnalysisResult result = importService.analizarCsv(file);
        return ResponseEntity.ok(result);
    }

    // 🔹 2) Aplicar ETL a columnas seleccionadas + exportar CSV
    @PostMapping("/etl/aplicar-csv")
    public ResponseEntity<Resource> aplicarEtlYExportar(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "columnas", required = false) List<String> columnas
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        byte[] data = importService.aplicarEtlYExportar(
                file,
                (columnas == null || columnas.isEmpty())
                        ? null
                        : new HashSet<>(columnas)
        );

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"articulos_etl.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(data.length)
                .body(resource);
    }
}
