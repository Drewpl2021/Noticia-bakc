package com.example.demo.controller;

import com.example.demo.entity.EstadoSolicitud;
import com.example.demo.entity.SolicitudNoticia;
import com.example.demo.service.SolicitudNoticiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "https://noticia-angular-qgv4.vercel.app")
@RestController
@RequestMapping("/api/solicitudes-noticias")
@RequiredArgsConstructor
public class SolicitudNoticiaController {

    private final SolicitudNoticiaService service;

    // Usuario envía una nueva solicitud
    @PostMapping
    public ResponseEntity<SolicitudNoticia> crear(@RequestBody SolicitudNoticia solicitud) {
        SolicitudNoticia creada = service.crearSolicitud(solicitud);
        return ResponseEntity.ok(creada);
    }


    @GetMapping("/{id}")
    public SolicitudNoticia porId(@PathVariable Long id) {
        return service.porId(id).orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
    }

    // Aprobar -> crea Articulo y cambia estado
    @PostMapping("/{id}/aprobar")
    public SolicitudNoticia aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    // Rechazar
    @PostMapping("/{id}/rechazar")
    public SolicitudNoticia rechazar(@PathVariable Long id) {
        return service.rechazar(id);
    }

    // com.example.demo.controller.SolicitudNoticiaController
    @GetMapping("/mias")
    public List<SolicitudNoticia> misSolicitudes(
            @RequestParam(name = "usuarioId") Long usuarioId,
            @RequestParam(required = false) EstadoSolicitud estado
    ) {
        if (estado != null) {
            return service.listarPorEstadoYUsuario(usuarioId, estado);
        }
        return service.listarPorUsuario(usuarioId);
    }

    // Listar todas o filtrar por estado (solo para admin)
    @GetMapping
    public List<SolicitudNoticia> listar(
            @RequestParam(name = "estado", required = false) EstadoSolicitud estado
    ) {
        if (estado != null) {
            // si viene estado, filtra
            return service.listarPorEstado(estado);
        }
        // si no viene estado, devuelve TODO
        return service.listarTodas();
    }

}
