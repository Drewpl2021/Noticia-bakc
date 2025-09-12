package com.example.demo.scraper;

import com.example.demo.ejecutable.ScraperNoticias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScraperJob {

    @Scheduled(cron = "*/10 * * * * *") // cada 10 segundos (para probar)
    public void runJob() {
        try {
            ScraperNoticias.runOnce(); // llamas a tu lógica
            log.info("Scraper ejecutado correctamente");
        } catch (Exception e) {
            log.error("Error en scraper", e);
        }
    }
}