package com.example.demo.service;

import com.example.demo.dto.DatamartAnalysisResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatamartService {

    /**
     * Analiza un CSV para ver qué columnas pueden ser dimensiones y medidas.
     * Sirve para cualquier CSV con cabecera.
     */
    DatamartAnalysisResult analyzeForDatamart(MultipartFile file);

    /**
     * Construye un datamart físico (tablas) y carga datos desde el CSV.
     *
     * @param file          CSV origen
     * @param datamartName  nombre base del datamart (ej: "noticias", "clientes")
     * @param dimensionCols columnas elegidas como dimensiones (1 a 5)
     * @param measureCols   columnas elegidas como medidas
     */
    void buildDatamart(MultipartFile file,
                       String datamartName,
                       List<String> dimensionCols,
                       List<String> measureCols);

    // com/example/demo/service/DatamartService.java

    byte[] buildDatamartAndExportCsv(MultipartFile file,
                                     String datamartName,
                                     List<String> dimensionCols,
                                     List<String> measureCols);
    byte[] buildDatamartAndExportZip(
            MultipartFile file,
            String datamartName,
            List<String> dimensionCols,
            List<String> measureCols
    );
}

