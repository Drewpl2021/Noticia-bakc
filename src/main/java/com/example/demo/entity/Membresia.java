package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "membresias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario dueño de la membresía
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membresia_usuario")
    )
    private Usuario usuario;

    // Producto/plan asociado (Premium Mensual, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "producto_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membresia_producto")
    )
    private Producto producto;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado; // ACTIVA, VENCIDA, CANCELADA

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private java.sql.Timestamp creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", insertable = false)
    private java.sql.Timestamp actualizadoEn;
}
