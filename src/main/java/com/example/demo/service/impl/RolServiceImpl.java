package com.example.demo.service.impl;

import com.example.demo.entity.Rol;
import com.example.demo.repository.RolRepository;
import com.example.demo.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    // === CREATE ===
    @Override
    public Rol crear(Rol rol) {
        return rolRepository.save(rol);
    }

    // === READ: listar todos ===
    @Override
    @Transactional(readOnly = true)
    public List<Rol> listarTodos() {
        return rolRepository.findAll();
    }

    // === READ: por id ===
    @Override
    @Transactional(readOnly = true)
    public Optional<Rol> obtenerPorId(Long id) {
        return rolRepository.findById(id);
    }

    // === UPDATE ===
    @Override
    public Rol actualizar(Long id, Rol rol) {
        return rolRepository.findById(id)
                .map(existing -> {
                    existing.setNombre(rol.getNombre());
                    existing.setDescripcion(rol.getDescripcion());
                    return rolRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));
    }

    // === DELETE ===
    @Override
    public void eliminar(Long id) {
        if (!rolRepository.existsById(id)) {
            throw new RuntimeException("Rol no encontrado con id: " + id);
        }
        rolRepository.deleteById(id);
    }
}
