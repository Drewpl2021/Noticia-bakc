package com.example.demo.repository;

import com.example.demo.entity.EstadoSolicitud;
import com.example.demo.entity.SolicitudNoticia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudNoticiaRepository extends JpaRepository<SolicitudNoticia, Long> {

// com.example.demo.repository.SolicitudNoticiaRepository

    List<SolicitudNoticia> findByEstado(EstadoSolicitud estado);

    List<SolicitudNoticia> findByUsuarioId(Long usuarioId);

    List<SolicitudNoticia> findByEstadoAndUsuarioId(EstadoSolicitud estado, Long usuarioId);


}
