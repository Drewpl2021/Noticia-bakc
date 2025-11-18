package com.example.demo.service.impl;

import com.example.demo.entity.Scrapper;
import com.example.demo.repository.ScrapperRepository;
import com.example.demo.service.ScrapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScrapperServiceImpl implements ScrapperService {

    private final ScrapperRepository scrapperRepository;

    // === CREATE ===
    @Override
    public Scrapper crear(Scrapper scrapper) {
        return scrapperRepository.save(scrapper);
    }

    // === READ: listar todos ===
    @Override
    @Transactional(readOnly = true)
    public List<Scrapper> listarTodos() {
        return scrapperRepository.findAll();
    }

    // === READ: por id ===
    @Override
    @Transactional(readOnly = true)
    public Optional<Scrapper> obtenerPorId(Long id) {
        return scrapperRepository.findById(id);
    }

    // === UPDATE ===
    @Override
    public Scrapper actualizar(Long id, Scrapper scrapper) {
        return scrapperRepository.findById(id)
                .map(existing -> {
                    existing.setUrl(scrapper.getUrl());
                    existing.setNombrePagina(scrapper.getNombrePagina());
                    existing.setLogo(scrapper.getLogo());
                    return scrapperRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Scrapper no encontrado con id: " + id));
    }

    // === DELETE ===
    @Override
    public void eliminar(Long id) {
        if (!scrapperRepository.existsById(id)) {
            throw new RuntimeException("Scrapper no encontrado con id: " + id);
        }
        scrapperRepository.deleteById(id);
    }
}
