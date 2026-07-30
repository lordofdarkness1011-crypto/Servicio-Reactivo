# Walkthrough: Construcción del Servicio Reactivo (Moli/Miguel)

Hemos completado exitosamente la transformación del proyecto base para tener un entorno totalmente reactivo, asegurando al mismo tiempo que la estructura principal (endpoints, tabla, modelo) sea idéntica al servicio bloqueante de Juan.

## Pasos Realizados y Cambios Efectuados

1. **Aislamiento del Entorno**
   - Eliminamos la carpeta inútil `servicio-miguel` generada previamente.
   - Duplicamos `unidad2` (el código de Juan) hacia `servicio-reactivo` para trabajar en un entorno seguro y con una base 100% equivalente.

2. **Migración de Dependencias a Reactivo**
   En `pom.xml`, reemplazamos el stack tradicional por el equivalente no bloqueante:
   - `spring-boot-starter-webmvc` ➔ `spring-boot-starter-webflux` (con Netty como servidor embebido).
   - `spring-boot-starter-data-jpa` ➔ `spring-boot-starter-data-r2dbc` (y agregamos `r2dbc-postgresql` como driver nativo de Postgres).

3. **Configuración de R2DBC y Eliminación del Pool Tomcat**
   - Modificamos `application.properties` para usar una conexión `r2dbc:postgresql://...`.
   - Removimos el límite de hilos (`server.tomcat.threads.max=200`), ya que Netty maneja por defecto el **Event Loop** asíncrono utilizando los hilos nativos de CPU.

4. **Refactorización del Modelo y Repositorio**
   - Modificamos la entidad `Item.java` para utilizar las anotaciones puras de Spring Data Relational (`@Table`, `@Id`) y retiramos todas las referencias a JPA e Hibernate.
   - Modificamos el repositorio `ItemRepository.java` de forma que ahora herede de `ReactiveCrudRepository<Item, Long>`, entregando así objetos `Mono` y `Flux`.

5. **Implementación de Controladores y Backpressure (Objetivo Central)**
   El controlador (`ItemController.java`) fue completamente reprogramado bajo el modelo reactivo. El aspecto más importante (y diferencial) fue incorporar las **4 estrategias de backpressure** que solicita la guía técnica:
   ```java
   public Flux<Item> obtenerTodos(@RequestParam String strategy, @RequestParam int rate) {
       Flux<Item> source = repository.findAll();
       return switch (strategy) {
           case "limitRate" -> source.limitRate(rate); // Consumo limitado (Demanda)
           case "buffer" -> source.onBackpressureBuffer(); // Encolar exceso
           case "drop" -> source.onBackpressureDrop(); // Descartar exceso masivo
           case "latest" -> source.onBackpressureLatest(); // Descartar y mantener último
           default -> source; // Sin control (Equivale a request(MAX_VALUE))
       };
   }
   ```
   También actualizamos componentes secundarios, como el `GlobalExceptionHandler` (sustituyendo `HttpServletRequest` por `ServerHttpRequest` de WebFlux) y `VideojuegoService`.

## Verificación

- **Compilación Exitosa:** El proyecto pasó correctamente el proceso de empaquetado y compilación (`mvnw clean compile`) demostrando que no existen errores de sintaxis y que las bibliotecas reactivas son completamente compatibles.
- Ahora este servicio está **listo para la fase de pruebas de carga**, en la cual se usarán herramientas de inyección (JMeter, por ejemplo, que es el que se observó en la carpeta de Juan) atacando los endpoints `GET /items?strategy=...`.
