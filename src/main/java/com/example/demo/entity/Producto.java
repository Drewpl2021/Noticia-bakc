package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del producto
    @Column(nullable = false, length = 150)
    private String nombre;

    // Descripción opcional
    @Column(length = 500)
    private String descripcion;

    // Precio actual
    @Column(nullable = false)
    private Double precio;

    // Tipo de producto (ej: FISICO, DIGITAL, SUSCRIPCION)
    @Column(nullable = false, length = 30)
    private String tipo;

    // Estado: ACTIVO / INACTIVO
    @Column(nullable = false, length = 20)
    private String estado;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Timestamp creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Timestamp actualizadoEn;
}
