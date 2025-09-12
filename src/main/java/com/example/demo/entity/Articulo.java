package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "articulos",
        indexes = {@Index(name = "idx_articulos_fecha", columnList = "fecha_publicado")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_articulos_url", columnNames = {"url"})}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 600)
    private String url;

    @Lob
    private String titulo;

    @Column(length = 255)
    private String autor;

    @Column(name = "fecha_publicado")
    private LocalDateTime fechaPublicado;

    @Column(name = "imagen_url", length = 600)
    private String imagenUrl;

    @Lob
    private String contenido;

    @Lob
    private String tags;

    @Lob
    private String categorias;



    @UpdateTimestamp
    @Column(name = "actualizado_en", insertable = false)
    private java.sql.Timestamp actualizadoEn;

}
