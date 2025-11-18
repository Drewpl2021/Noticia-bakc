package com.example.demo.controller;

import com.example.demo.entity.Membresia;
import com.example.demo.service.MembresiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/membresias")
@RequiredArgsConstructor
public class MembresiaController {

    private final MembresiaService membresiaService;

    // LISTAR TODAS
    @GetMapping
    public List<Membresia> listarTodas() {
        return membresiaService.listarTodas();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public Membresia obtenerPorId(@PathVariable Long id) {
        return membresiaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada con id: " + id));
    }

    // LISTAR POR USUARIO
    @GetMapping("/usuario/{usuarioId}")
    public List<Membresia> listarPorUsuario(@PathVariable Long usuarioId) {
        return membresiaService.listarPorUsuario(usuarioId);
    }

    // CREAR
    @PostMapping
    public Membresia crear(@RequestBody Membresia membresia) {
        return membresiaService.crear(membresia);
    }

    // ACTUALIZAR
    @PutMapping("/{id}")
    public Membresia actualizar(@PathVariable Long id, @RequestBody Membresia membresia) {
        return membresiaService.actualizar(id, membresia);
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        membresiaService.eliminar(id);
    }
}
