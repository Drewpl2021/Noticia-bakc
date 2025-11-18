package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_roles_nombre", columnNames = {"nombre"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;   // ej: "ADMIN", "EDITOR", "USER"

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
