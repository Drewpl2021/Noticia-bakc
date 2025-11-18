package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "scrappers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_scrappers_url", columnNames = {"url"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scrapper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, length = 600)
    private String url;  // URL base de la página a scrapear

    @Column(name = "nombre_pagina", nullable = false, length = 200)
    private String nombrePagina; // Ej: "El Comercio", "RPP", etc.

    @Column(name = "logo_url", length = 600)
    private String logo; // URL del logo de la página
}
