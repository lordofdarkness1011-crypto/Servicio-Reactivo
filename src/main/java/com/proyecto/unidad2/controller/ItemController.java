package com.proyecto.unidad2.controller;

import com.proyecto.unidad2.model.Item;
import com.proyecto.unidad2.repository.ItemRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository repository;
    private final MeterRegistry registry;

    // 1. GET /items -> Recuperación masiva (Simula un feed)
    // Se implementan las 4 estrategias de backpressure requeridas.
    @GetMapping
    public ResponseEntity<Flux<Item>> obtenerTodos(
            @RequestParam(required = false, defaultValue = "none") String strategy,
            @RequestParam(required = false, defaultValue = "100") int rate) {

        long startTime = System.nanoTime();

        Flux<Item> source = repository.findAll()
                .doFinally(signalType -> registry.timer("tienda.db.latency").record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));

        Flux<Item> responseBody = applyStrategy(source, strategy, rate);

        return ResponseEntity.ok()
                .header("X-Reactive-Strategy", strategy)
                .body(responseBody);
    }

    private Flux<Item> applyStrategy(Flux<Item> source, String strategy, int rate) {
        return switch (strategy) {
            case "limitRate" -> source.limitRate(rate);
            case "buffer" -> source.onBackpressureBuffer();
            case "drop" -> source.onBackpressureDrop();
            case "latest" -> source.onBackpressureLatest();
            default -> source; // Sin control de demanda
        };
    }

    // 2. GET /items/{id} -> Consulta puntual
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Item>> obtenerPorId(@PathVariable Long id) {
        long startTime = System.nanoTime();
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doFinally(signalType -> registry.timer("tienda.db.latency").record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
    }

    // 3. POST /items -> Escritura simple
    @PostMapping
    public Mono<ResponseEntity<Item>> crear(@RequestBody Item item) {
        long startTime = System.nanoTime();
        return repository.save(item)
                .map(savedItem -> ResponseEntity.status(201).body(savedItem))
                .doFinally(signalType -> registry.timer("tienda.db.latency").record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
    }
}