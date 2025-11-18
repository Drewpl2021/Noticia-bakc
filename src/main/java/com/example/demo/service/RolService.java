package com.example.demo.service;

import com.example.demo.entity.Rol;

import java.util.List;
import java.util.Optional;

public interface RolService {

    // CREATE
    Rol crear(Rol rol);

    // READ
    List<Rol> listarTodos();
    Optional<Rol> obtenerPorId(Long id);

    // UPDATE
    Rol actualizar(Long id, Rol rol);

    // DELETE
    void eliminar(Long id);
}
