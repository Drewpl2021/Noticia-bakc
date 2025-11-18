package com.example.demo.service.impl;

import com.example.demo.entity.Membresia;
import com.example.demo.repository.MembresiaRepository;
import com.example.demo.service.MembresiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MembresiaServiceImpl implements MembresiaService {

    private final MembresiaRepository membresiaRepository;

    @Override
    public List<Membresia> listarTodas() {
        return membresiaRepository.findAll();
    }

    @Override
    public Optional<Membresia> obtenerPorId(Long id) {
        return membresiaRepository.findById(id);
    }

    @Override
    public List<Membresia> listarPorUsuario(Long usuarioId) {
        return membresiaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public Membresia crear(Membresia membresia) {
        // Aquí podrías validar:
        // - que venga usuario
        // - que venga producto
        // - que fechaFin > fechaInicio
        if (membresia.getEstado() == null) {
            membresia.setEstado("ACTIVA");
        }
        return membresiaRepository.save(membresia);
    }

    @Override
    public Membresia actualizar(Long id, Membresia membresia) {
        return membresiaRepository.findById(id)
                .map(existing -> {
                    existing.setUsuario(membresia.getUsuario());
                    existing.setProducto(membresia.getProducto());          // <-- antes era planId
                    existing.setFechaInicio(membresia.getFechaInicio());
                    existing.setFechaFin(membresia.getFechaFin());
                    existing.setEstado(membresia.getEstado());
                    existing.setPaymentReference(membresia.getPaymentReference());
                    return membresiaRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada con id: " + id));
    }

    @Override
    public void eliminar(Long id) {
        if (!membresiaRepository.existsById(id)) {
            throw new RuntimeException("Membresía no encontrada con id: " + id);
        }
        membresiaRepository.deleteById(id);
    }
}
