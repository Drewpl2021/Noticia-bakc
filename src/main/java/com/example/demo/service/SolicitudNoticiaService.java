package com.example.demo.service;

import com.example.demo.entity.SolicitudNoticia;
import com.example.demo.entity.EstadoSolicitud;

import java.util.List;
import java.util.Optional;

public interface SolicitudNoticiaService {

    SolicitudNoticia crearSolicitud(SolicitudNoticia solicitud);

    List<SolicitudNoticia> listarPorEstado(EstadoSolicitud estado);

    Optional<SolicitudNoticia> porId(Long id);

    // Admin
    SolicitudNoticia aprobar(Long id);

    SolicitudNoticia rechazar(Long id);

    List<SolicitudNoticia> listarPorUsuario(Long usuarioId);

    List<SolicitudNoticia> listarPorEstadoYUsuario(Long usuarioId, EstadoSolicitud estado);
    List<SolicitudNoticia> listarTodas();

}

