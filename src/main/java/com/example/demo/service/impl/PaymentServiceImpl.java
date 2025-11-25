package com.example.demo.service.impl;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.PaymentResponseDTO;
import com.example.demo.entity.Membresia;
import com.example.demo.entity.Producto;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.MembresiaRepository;
import com.example.demo.repository.ProductoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WebClient paymentWebClient;   // viene de PaymentClientConfig
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final MembresiaRepository membresiaRepository;

    @Override
    public PaymentResponseDTO pagarMembresia(PaymentRequestDTO request) {

        // 1) Validar usuario y producto
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!"ACTIVO".equalsIgnoreCase(producto.getEstado())) {
            throw new RuntimeException("El producto no está activo");
        }

        // 2) Monto en CENTIMOS (Culqi usa integer)
        int amountInCents = (int) Math.round(producto.getPrecio() * 100);

        // 3) Armar request para Culqi
        Map<String, Object> culqiRequest = new HashMap<>();
        culqiRequest.put("amount", amountInCents);
        culqiRequest.put("currency_code", "PEN");
        culqiRequest.put("email", request.getEmail());
        culqiRequest.put("source_id", request.getSourceId());
        culqiRequest.put("description",
                request.getDescripcion() != null ? request.getDescripcion()
                        : "Membresía " + producto.getNombre());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("usuarioId", usuario.getId());
        metadata.put("productoId", producto.getId());
        culqiRequest.put("metadata", metadata);

        // 4) Llamar a Culqi
        Map<String, Object> culqiResponse;
        try {
            culqiResponse = paymentWebClient.post()
                    .uri("/v2/charges")
                    .bodyValue(culqiRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            return PaymentResponseDTO.builder()
                    .success(false)
                    .message("Error al comunicarse con Culqi: " + e.getMessage())
                    .build();
        }

        if (culqiResponse == null) {
            return PaymentResponseDTO.builder()
                    .success(false)
                    .message("Respuesta vacía de Culqi")
                    .build();
        }

        // 5) Verificar resultado simple (puedes hacer esto más robusto)
        String chargeId = (String) culqiResponse.get("id");

        // outcome -> type = "venta_exitosa" normalmente
        Map<String, Object> outcome =
                (Map<String, Object>) culqiResponse.get("outcome");

        String type = outcome != null ? (String) outcome.get("type") : null;

        if (chargeId == null || !"venta_exitosa".equalsIgnoreCase(type)) {
            String msg = outcome != null ? (String) outcome.get("user_message")
                    : "Pago no aprobado";
            return PaymentResponseDTO.builder()
                    .success(false)
                    .chargeId(chargeId)
                    .message(msg)
                    .rawResponse(culqiResponse.toString())
                    .build();
        }

        // 6) Crear **o actualizar** la membresía en tu BD
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fin = ahora.plusDays(30);

// Buscar si el usuario ya tiene una membresía ACTIVA
        var activas = membresiaRepository.findByUsuarioIdAndEstado(usuario.getId(), "ACTIVA");

        Membresia membresia;

        if (!activas.isEmpty()) {
            // ✅ Ya tiene una membresía activa → la ACTUALIZAMOS
            membresia = activas.get(0);
            membresia.setProducto(producto);
            membresia.setFechaInicio(ahora);
            membresia.setFechaFin(fin);
            membresia.setEstado("ACTIVA");
            membresia.setPaymentReference(chargeId);
        } else {
            // ✅ No tiene membresía activa → CREAMOS una nueva
            membresia = Membresia.builder()
                    .usuario(usuario)
                    .producto(producto)
                    .fechaInicio(ahora)
                    .fechaFin(fin)
                    .estado("ACTIVA")
                    .paymentReference(chargeId) // id de Culqi
                    .build();
        }

        membresia = membresiaRepository.save(membresia);

        return PaymentResponseDTO.builder()
                .success(true)
                .message("Pago exitoso y membresía creada/actualizada")
                .chargeId(chargeId)
                .membresiaId(membresia.getId())
                .rawResponse(culqiResponse.toString())
                .build();

    }
}
