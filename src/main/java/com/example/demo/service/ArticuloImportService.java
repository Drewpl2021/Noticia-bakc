package com.example.demo.service;

import com.example.demo.dto.ArticuloImportResult;
import com.example.demo.dto.CsvAnalysisResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface ArticuloImportService {


    ArticuloImportResult importarDesdeCsv(MultipartFile file);

    // 🔹 NUEVO: solo análisis, sin tocar BD
    CsvAnalysisResult analizarCsv(MultipartFile file);

    // 🔹 NUEVO: aplicar ETL solo a columnas seleccionadas y exportar CSV
    byte[] aplicarEtlYExportar(MultipartFile file, Set<String> columnas);
}
