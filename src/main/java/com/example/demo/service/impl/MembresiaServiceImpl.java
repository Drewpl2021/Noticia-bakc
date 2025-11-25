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

        if (membresia.getUsuario() == null || membresia.getUsuario().getId() == null) {
            throw new RuntimeException("La membresía debe tener un usuario con id.");
        }
        if (membresia.getProducto() == null || membresia.getProducto().getId() == null) {
            throw new RuntimeException("La membresía debe tener un producto (plan) con id.");
        }

        Long usuarioId = membresia.getUsuario().getId();

        // 🔹 Buscar si el usuario ya tiene una membresía ACTIVA
        List<Membresia> activas = membresiaRepository.findByUsuarioIdAndEstado(usuarioId, "ACTIVA");

        // Si ya tiene una membresía activa → ACTUALIZAMOS esa misma
        if (!activas.isEmpty()) {
            Membresia actual = activas.get(0); // asumimos una activa por usuario

            actual.setProducto(membresia.getProducto());
            actual.setFechaInicio(membresia.getFechaInicio());
            actual.setFechaFin(membresia.getFechaFin());
            actual.setPaymentReference(membresia.getPaymentReference());

            // aseguramos estado ACTIVA
            actual.setEstado("ACTIVA");

            return membresiaRepository.save(actual);
        }

        // Si NO tiene membresía activa → creamos una nueva
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
