package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario que paga
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pago_usuario")
    )
    private Usuario usuario;

    // Producto (plan) que está comprando
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "producto_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pago_producto")
    )
    private Producto producto;

    // Monto en tu sistema (en soles)
    @Column(nullable = false)
    private Double monto;

    // Moneda (PEN, USD, etc.)
    @Column(nullable = false, length = 10)
    private String moneda;

    // Método: YAPE, PLIN, CULQI_CARD, etc.
    @Column(nullable = false, length = 30)
    private String metodo;

    // Estado: PENDIENTE, APROBADO, RECHAZADO, CANCELADO
    @Column(nullable = false, length = 20)
    private String estado;

    // ID que devuelve la pasarela (ej: charge_id, order_id, token, etc.)
    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    // Info adicional (JSON, mensaje de error, etc.)
    @Column(name = "detalles", length = 1000)
    private String detalles;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Timestamp creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Timestamp actualizadoEn;
}
