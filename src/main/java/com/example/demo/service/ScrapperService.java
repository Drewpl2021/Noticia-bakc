package com.example.demo.service;

import com.example.demo.entity.Scrapper;

import java.util.List;
import java.util.Optional;

public interface ScrapperService {

    // CREATE
    Scrapper crear(Scrapper scrapper);

    // READ
    List<Scrapper> listarTodos();
    Optional<Scrapper> obtenerPorId(Long id);

    // UPDATE
    Scrapper actualizar(Long id, Scrapper scrapper);

    // DELETE
    void eliminar(Long id);
}
