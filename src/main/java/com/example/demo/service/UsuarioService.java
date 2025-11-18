package com.example.demo.service;

import com.example.demo.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    // CREATE
    Usuario crear(Usuario usuario);

    // READ
    List<Usuario> listarTodos();
    Optional<Usuario> obtenerPorId(Long id);

    // UPDATE
    Usuario actualizar(Long id, Usuario usuario);

    // DELETE
    void eliminar(Long id);
}
