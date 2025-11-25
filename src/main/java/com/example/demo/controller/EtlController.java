package com.example.demo.controller;

import com.example.demo.dto.CsvAnalysisResult;
import com.example.demo.service.CsvEtlService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "https://noticia-angular-qgv4.vercel.app")
@RestController
@RequestMapping("/api/etl")
@RequiredArgsConstructor
public class EtlController {

    private final CsvEtlService csvEtlService;

    // 1) ANÁLISIS (para cualquier CSV)
    @PostMapping("/analisis")
    public ResponseEntity<CsvAnalysisResult> analizar(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        CsvAnalysisResult result = csvEtlService.analyzeCsv(file);
        return ResponseEntity.ok(result);
    }

    // 2) APLICAR ETL + EXPORTAR CSV LIMPIO (para cualquier CSV)
    @PostMapping("/aplicar")
    public ResponseEntity<Resource> aplicarEtl(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "columnas", required = false) List<String> columnas,
            @RequestParam(name = "keyColumn", required = false) String keyColumn
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Set<String> cols = null;
        if (columnas != null && !columnas.isEmpty()) {
            cols = new HashSet<>(columnas);
        }

        byte[] data = csvEtlService.etlAndExport(file, cols, keyColumn);

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"etl_result.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(data.length)
                .body(resource);
    }
}
