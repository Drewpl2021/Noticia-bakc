package com.example.demo.service;

import com.example.demo.entity.Membresia;

import java.util.List;
import java.util.Optional;

public interface MembresiaService {

    List<Membresia> listarTodas();

    Optional<Membresia> obtenerPorId(Long id);

    List<Membresia> listarPorUsuario(Long usuarioId);

    Membresia crear(Membresia membresia);

    Membresia actualizar(Long id, Membresia membresia);

    void eliminar(Long id);
}
