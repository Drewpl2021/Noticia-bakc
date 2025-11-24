package com.example.demo.service.impl;

import com.example.demo.entity.*;
import com.example.demo.repository.ArticuloRepository;
import com.example.demo.repository.SolicitudNoticiaRepository;
import com.example.demo.service.SolicitudNoticiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SolicitudNoticiaServiceImpl implements SolicitudNoticiaService {

    private final SolicitudNoticiaRepository solicitudRepo;
    private final ArticuloRepository articuloRepo;

    @Override
    public SolicitudNoticia crearSolicitud(SolicitudNoticia solicitud) {
        // siempre entra como pendiente
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        // si no mandan fecha, puedes dejarla null o poner ahora
        if (solicitud.getFechaPublicado() == null) {
            solicitud.setFechaPublicado(LocalDateTime.now());
        }
        return solicitudRepo.save(solicitud);
    }

    @Override
    public List<SolicitudNoticia> listarPorEstado(EstadoSolicitud estado) {
        return solicitudRepo.findByEstado(estado);
    }

    @Override
    public Optional<SolicitudNoticia> porId(Long id) {
        return solicitudRepo.findById(id);
    }

    @Override
    public SolicitudNoticia aprobar(Long id) {
        SolicitudNoticia sol = solicitudRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (sol.getEstado() == EstadoSolicitud.APROBADO) {
            return sol; // ya está aprobada
        }

        // 1. Crear Articulo a partir de la solicitud
        Articulo articulo = Articulo.builder()
                .url(sol.getUrl())
                .titulo(sol.getTitulo())
                .autor(sol.getAutor())
                .fechaPublicado(
                        sol.getFechaPublicado() != null
                                ? sol.getFechaPublicado()
                                : LocalDateTime.now()
                )
                .imagenUrl(sol.getImagenUrl())
                .contenido(sol.getContenido())
                .tags(sol.getTags())
                .categorias(sol.getCategorias())
                .build();

        articuloRepo.save(articulo);

        // 2. Actualizar estado de la solicitud
        sol.setEstado(EstadoSolicitud.APROBADO);
        return solicitudRepo.save(sol);
    }

    @Override
    public SolicitudNoticia rechazar(Long id) {
        SolicitudNoticia sol = solicitudRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        sol.setEstado(EstadoSolicitud.RECHAZADO);
        return solicitudRepo.save(sol);
    }


    @Override
    public List<SolicitudNoticia> listarPorUsuario(Long usuarioId) {
        return solicitudRepo.findByUsuarioId(usuarioId);
    }

    @Override
    public List<SolicitudNoticia> listarPorEstadoYUsuario(Long usuarioId, EstadoSolicitud estado) {
        return solicitudRepo.findByEstadoAndUsuarioId(estado, usuarioId);
    }
    @Override
    public List<SolicitudNoticia> listarTodas() {
        return solicitudRepo.findAll();
    }


        // ... resto de métodos (crearSolicitud, aprobar, rechazar)

}
