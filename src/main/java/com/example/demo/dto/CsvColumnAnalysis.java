package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CsvColumnAnalysis {

    private String name;

    private int nullCount;
    private double nullPercent;

    private int duplicateCount;
    private double duplicatePercent;
}
