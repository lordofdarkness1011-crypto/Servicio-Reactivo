package com.proyecto.unidad2.service;

import com.proyecto.unidad2.model.Item;
import com.proyecto.unidad2.repository.ItemRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
public class VideojuegoService {

    private final ItemRepository repository;
    private final Timer dbLatencyTimer;

    public VideojuegoService(ItemRepository repository, MeterRegistry registry) {
        this.repository = repository;
        // Registro del medidor de latencia síncrono para PostgreSQL
        this.dbLatencyTimer = Timer.builder("tienda.db.latency")
                .description("Latencia de consultas a PostgreSQL")
                .register(registry);
    }

    public Flux<Item> obtenerCatalogo() {
        long startTime = System.nanoTime();
        return repository.findAll()
                .doFinally(signalType -> dbLatencyTimer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
    }

    public Mono<Item> guardarJuego(Item juego) {
        return repository.save(juego);
    }

    // Flujo masivo reactivo para simular la carga
    public Flux<Item> obtenerFlujoMasivoReactivo() {
        return Flux.range(1, 500000)
                .map(i -> Item.builder()
                        .id((long) i)
                        .titulo("Juego Pesado " + i)
                        .plataforma("Multiplataforma")
                        .precio(29.99)
                        .build());
    }
}