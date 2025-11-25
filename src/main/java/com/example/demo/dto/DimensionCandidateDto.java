package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DimensionCandidateDto {
    private String name;
    private long distinctCount;
    private double distinctRatio; // distinctCount / totalRows
    private boolean suggested;    // true si es buena candidata
    private List<String> sampleValues; // primeros N valores distintos
}
