package com.example.demo.service;

import com.example.demo.dto.CsvAnalysisResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface CsvEtlService {

    /**
     * Analiza el CSV sin tocar BD:
     * - total de filas
     * - por columna: nulos y duplicados (conteo y porcentaje)
     * Funciona con cualquier CSV que tenga cabecera.
     */
    CsvAnalysisResult analyzeCsv(MultipartFile file);

    /**
     * ETL genérico sobre la DATA IMPORTADA (CSV), sin tocar BD:
     * - limpia textos (trim, vacíos -> null)
     * - elimina filas donde TODAS las columnas seleccionadas están vacías
     * - si se especifica keyColumn y existe en el CSV:
     *      - deduplica por esa columna (última aparición gana)
     *   si NO se especifica o no existe:
     *      - NO deduplica (mantiene todas las filas)
     * - exporta CSV con SOLO las columnas seleccionadas (o todas si no se envían).
     *
     * @param file            archivo CSV original
     * @param selectedColumns columnas a conservar (si null o vacío -> todas las columnas del CSV)
     * @param keyColumn       columna a usar como clave para deduplicar (opcional)
     */
    byte[] etlAndExport(MultipartFile file,
                        Set<String> selectedColumns,
                        String keyColumn);
}
