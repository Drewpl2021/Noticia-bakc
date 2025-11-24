package com.example.demo.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_noticias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudNoticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Podrías generar la url al aprobar, pero lo dejo igual que Articulo
    @Column(nullable = false, length = 600)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String titulo;

    @Column(length = 255)
    private String autor;

    @Column(name = "fecha_publicado")
    private LocalDateTime fechaPublicado;

    @Column(name = "imagen_url", length = 600)
    private String imagenUrl;

    @Column(columnDefinition = "LONGTEXT")
    private String contenido;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String categorias;

    // nuevo campo: estado de la solicitud
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", insertable = false)
    private LocalDateTime actualizadoEn;
}
