package com.example.demo.controller;

import com.example.demo.entity.Rol;
import com.example.demo.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    // LISTAR TODOS
    @GetMapping
    public List<Rol> listarTodos() {
        return rolService.listarTodos();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public Rol obtenerPorId(@PathVariable Long id) {
        return rolService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));
    }

    // CREAR
    @PostMapping
    public Rol crear(@RequestBody Rol rol) {
        return rolService.crear(rol);
    }

    // ACTUALIZAR
    @PutMapping("/{id}")
    public Rol actualizar(@PathVariable Long id, @RequestBody Rol rol) {
        return rolService.actualizar(id, rol);
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        rolService.eliminar(id);
    }
}
