package com.example.demo.dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {

    // quién paga
    private Long usuarioId;

    // qué plan/producto está comprando
    private Long productoId;

    // token de Culqi generado en el front (source_id)
    private String sourceId;

    // email del pagador (Culqi lo exige)
    private String email;

    // opcional: descripción que verás en Culqi
    private String descripcion;
}
