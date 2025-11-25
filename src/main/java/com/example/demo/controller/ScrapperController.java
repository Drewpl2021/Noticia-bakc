package com.example.demo.controller;

import com.example.demo.dto.RunScraperRequest;
import com.example.demo.ejecutable.ScraperNoticias;
import com.example.demo.entity.Scrapper;
import com.example.demo.service.ScrapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "https://noticia-angular-qgv4.vercel.app")
@RestController
@RequestMapping("/api/scrappers")
@RequiredArgsConstructor
public class ScrapperController {

    private final ScrapperService scrapperService;

    // LISTAR TODOS
    @GetMapping
    public List<Scrapper> listarTodos() {
        return scrapperService.listarTodos();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public Scrapper obtenerPorId(@PathVariable Long id) {
        return scrapperService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Scrapper no encontrado con id: " + id));
    }

    // CREAR
    @PostMapping
    public Scrapper crear(@RequestBody Scrapper scrapper) {
        return scrapperService.crear(scrapper);
    }

    // ACTUALIZAR
    @PutMapping("/{id}")
    public Scrapper actualizar(@PathVariable Long id, @RequestBody Scrapper scrapper) {
        return scrapperService.actualizar(id, scrapper);
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        scrapperService.eliminar(id);
    }

    @PostMapping("/run")
    public Map<String, Object> ejecutarScraper(@RequestBody RunScraperRequest request) {
        String homeUrl = request.getUrl();

        int total = ScraperNoticias.runOnce(homeUrl);

        Map<String, Object> resp = new HashMap<>();
        resp.put("url", homeUrl);
        resp.put("articulosGuardados", total);

        return resp;
    }
}
