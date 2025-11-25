package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DatamartAnalysisResult {
    private int totalRows;
    private int maxDimensionsSuggested; // por ejemplo 5
    private List<DimensionCandidateDto> dimensionCandidates;
    private List<MeasureCandidateDto> measureCandidates;
}
