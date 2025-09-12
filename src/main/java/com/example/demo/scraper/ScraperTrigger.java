// com/example/demo/scraper/ScraperTrigger.java
package com.example.demo.scraper;

import com.example.demo.ejecutable.ScraperNoticias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class ScraperTrigger {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Instant lastRun = Instant.EPOCH; // nunca ejecutado

    /**
     * Lanza el scraper si han pasado al menos 'minInterval' desde la última ejecución.
     * No bloquea el hilo que llama. Evita ejecuciones en paralelo.
     */
    public void maybeRun(Duration minInterval) {
        Instant now = Instant.now();
        if (running.get()) {
            log.debug("Scraper ya está corriendo; se omite.");
            return;
        }
        if (now.isBefore(lastRun.plus(minInterval))) {
            log.debug("Aún dentro del intervalo mínimo; se omite. Ultima: {}, minInterval: {}", lastRun, minInterval);
            return;
        }

        if (!running.compareAndSet(false, true)) {
            // otro hilo se adelanta
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Scraper iniciado...");
                ScraperNoticias.runOnce(); // tu método estático
                lastRun = Instant.now();
                log.info("Scraper finalizado. lastRun={}", lastRun);
            } catch (Exception e) {
                log.error("Error ejecutando scraper", e);
            } finally {
                running.set(false);
            }
        });
    }


}
