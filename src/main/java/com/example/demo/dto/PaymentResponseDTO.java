package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponseDTO {

    private boolean success;
    private String message;

    // id del cargo en Culqi (charge_id)
    private String chargeId;

    // id de la membresía creada en tu BD
    private Long membresiaId;

    // opcional, para debug
    private String rawResponse;
}
