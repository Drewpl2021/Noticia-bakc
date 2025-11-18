package com.example.demo.service;

import com.example.demo.entity.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoService {

    List<Producto> listarTodos();

    Optional<Producto> obtenerPorId(Long id);

    Producto crear(Producto producto);

    Producto actualizar(Long id, Producto producto);

    void eliminar(Long id);
}
