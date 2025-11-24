package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticuloImportResult {
    private int totalRows;
    private int inserted;
    private int updated;
    private int skipped;
    private int errors;
}
