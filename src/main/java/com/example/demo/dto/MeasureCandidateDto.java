package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeasureCandidateDto {
    private String name;
    private boolean numeric;
}
