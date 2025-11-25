package com.example.demo.controller;

import com.example.demo.dto.DatamartAnalysisResult;
import com.example.demo.service.DatamartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@CrossOrigin(origins = "https://noticia-angular-qgv4.vercel.app")
@RestController
@RequestMapping("/api/datamart")
@RequiredArgsConstructor
public class DatamartController {

    private final DatamartService datamartService;

    // 1) Analizar CSV para DataMart
    @PostMapping("/analisis")
    public ResponseEntity<DatamartAnalysisResult> analisis(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        DatamartAnalysisResult result = datamartService.analyzeForDatamart(file);
        return ResponseEntity.ok(result);
    }

    // 2) Construir DataMart (crear tablas + cargar datos)
    @PostMapping("/construir")
    public ResponseEntity<Void> construir(
            @RequestParam("file") MultipartFile file,
            @RequestParam("nombre") String datamartName,
            @RequestParam("dimensiones") List<String> dimensiones,
            @RequestParam(name = "medidas", required = false) List<String> medidas
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        datamartService.buildDatamart(file, datamartName, dimensiones, medidas);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/construir-y-exportar")
    public ResponseEntity<Resource> construirYExportar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("nombre") String datamartName,
            @RequestParam("dimensiones") List<String> dimensiones,
            @RequestParam(name = "medidas", required = false) List<String> medidas
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] data = datamartService.buildDatamartAndExportCsv(
                file,
                datamartName,
                dimensiones,
                medidas
        );

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dm_" + datamartName + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(data.length)
                .body(resource);
    }
    @PostMapping("/construir-y-exportar-zip")
    public ResponseEntity<Resource> construirYExportarZip(
            @RequestParam("file") MultipartFile file,
            @RequestParam("nombre") String datamartName,
            @RequestParam("dimensiones") List<String> dimensiones,
            @RequestParam(name = "medidas", required = false) List<String> medidas
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] data = datamartService.buildDatamartAndExportZip(
                file,
                datamartName,
                dimensiones,
                medidas
        );

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dm_" + datamartName + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(data.length)
                .body(resource);
    }
}
