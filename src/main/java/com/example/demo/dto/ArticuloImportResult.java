package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ArticuloImportResult {

    private int totalRows;
    private int inserted;
    private int updated;
    private int skipped;
    private int errors;

    // ETL extra
    private int duplicatesInFile;   // mismas URL repetidas en el CSV
    private int duplicatesInDb;     // coincidieron con registros ya existentes (updates)

    // nulos por columna
    private Map<String, Integer> nullCounts;      // ej: {"titulo": 3, "autor": 10}
    private Map<String, Double> nullPercents;     // ej: {"titulo": 0.05, "autor": 0.20} (0-1)
}
