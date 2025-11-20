package com.example.demo.service;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.PaymentResponseDTO;

public interface PaymentService {

    PaymentResponseDTO pagarMembresia(PaymentRequestDTO request);
}
