package com.example.demo.service;

import com.example.demo.dto.ArticuloImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface ArticuloImportService {

    /**
     * Realiza ETL de un CSV de artículos:
     * - Lee filas del CSV
     * - Limpia/transforma campos
     * - Inserta/actualiza Articulo por URL (clave natural)
     */
    ArticuloImportResult importarDesdeCsv(MultipartFile file);
}
