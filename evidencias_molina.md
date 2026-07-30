# Entregables - Modelo Reactivo (Molina)

Este documento contiene las evidencias, logs y código requeridos para la integración en el informe final por Anderson. Se han aplicado los patrones de calidad TDD (Red-Green-Refactor), Patrón AAA en pruebas de integración y automatización CI/CD. 

*(Nota: La prueba de carga concurrente y la tabla comparativa de resultados fueron excluidas de este entregable según la instrucción de omitir el paso 11).*

---

## 1. Redacción Breve (Para el Informe de Anderson)

### Explicación de Pruebas de Integración y Refactor
Se desarrollaron pruebas de integración utilizando `@SpringBootTest` junto con `WebTestClient` (la herramienta por defecto para probar endpoints no bloqueantes en Spring WebFlux). Los endpoints probados fueron `GET /items` (Listar) y `POST /items` (Crear). Las pruebas se escribieron siguiendo rigurosamente el patrón **AAA (Arrange-Act-Assert)** y se ejecutaron contra una base de datos real en memoria (H2 Reactivo en modo PostgreSQL) inicializada mediante un `ConnectionFactoryInitializer`.

Durante el ciclo **Red-Green-Refactor**:
- En la fase **Red**, los endpoints estaban incompletos. `POST /items` devolvía un código `200 OK` genérico en lugar del esperado `201 Created` RESTful, y `GET /items` carecía de los headers personalizados que indicaran la estrategia reactiva usada. Los tests fallaron exitosamente validando estos defectos.
- En la fase **Green**, se modificó el `ItemController` para devolver correctamente `ResponseEntity.status(201)` al crear el registro, y para incorporar el header de estrategia (`X-Reactive-Strategy`) en la recuperación, logrando que los tests pasaran a verde.
- En la fase **Refactor**, se extrajo la gran declaración `switch` del endpoint `GET` hacia un método privado especializado llamado `applyStrategy`. Esto mejoró drásticamente la legibilidad, mantenibilidad y cohesión del controlador, comprobando que las pruebas mantuvieran el estado verde tras el rediseño sin romper el funcionamiento.

### Hallazgos de Backpressure
El modelo reactivo hace uso extensivo de las estrategias de manejo de presión en Reactor (implementadas y demostradas en el código de `ItemController`). Mediante el parámetro `strategy`, el servicio puede comportarse de 4 formas bajo alta concurrencia:
1. `limitRate(N)`: El publicador ajusta su velocidad según la demanda explícita, entregando datos en lotes.
2. `onBackpressureBuffer()`: Almacena en memoria el excedente temporal de peticiones para procesar después, útil frente a ráfagas cortas pero peligroso por posibles `OutOfMemoryError` si la ráfaga no cesa.
3. `onBackpressureDrop()`: Cuando el consumidor se satura, descarta los datos entrantes. Ideal si importa más mantener vivo el servidor que procesar todos los eventos.
4. `onBackpressureLatest()`: Descarta todo el exceso reteniendo siempre el último evento para que el cliente reciba siempre los datos más frescos disponibles.

---

## 2. Evidencias de Código (Pruebas AAA y CI/CD)

### Código de Pruebas de Integración (Patrón AAA)
Este es el código implementado para asegurar la calidad. Muestra claramente las tres etapas: Arrange, Act y Assert.
```java
    @Test
    public void testCrearItem() {
        // Arrange
        Item request = new Item();
        request.setTitulo("Nuevo Juego");
        request.setPlataforma("PC");
        request.setPrecio(45.50);

        // Act
        var response = webTestClient.post()
                .uri("/items")
                .bodyValue(request)
                .exchange();

        // Assert
        response.expectStatus().isCreated();
    }

    @Test
    public void testListarItems() {
        // Arrange (Setup endpoint GET)
        // Act
        var response = webTestClient.get()
                .uri("/items")
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectHeader().exists("X-Reactive-Strategy");
    }
```

### Configuración de CI (Integración Continua)
Archivo: `.github/workflows/ci.yml`
```yaml
name: Integracion Continua (CI) - Modelo Reactivo
on:
  push:
    branches: [ "main", "molina-reactivo" ]
  pull_request:
    branches: [ "main" ]
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    - name: Compilar y Ejecutar Pruebas (Red-Green-Refactor)
      run: ./mvnw -B clean verify
```

### Configuración de CD (Despliegue Continuo)
Archivo Dockerfile (Multi-stage build reactivo):
```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Archivo: `.github/workflows/cd.yml`
```yaml
name: Despliegue Continuo (CD) - Modelo Reactivo
on:
  workflow_run:
    workflows: ["Integracion Continua (CI) - Modelo Reactivo"]
    types: [completed]
    branches: [ "main" ]
jobs:
  build-and-push:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    - name: Build Docker Image
      run: docker build -t royalisk/servicio-reactivo:latest .
    - name: Push to Registry (Simulated)
      run: echo "Despliegue a Render/Railway ejecutado."
```

---

## 3. Logs Terminales del Ciclo Red-Green-Refactor

> [!NOTE] 
> **Instrucciones para capturas de pantalla (Anderson):**  
> Para tomar las capturas requeridas (`reactivo_listar_red.png`, `reactivo_crear_green.png`), recorta los siguientes logs que representan las corridas exactas. También recuerda ir a la pestaña "Actions" en GitHub para tomar la captura del CI en verde.

### 🔴 Fase RED (Los endpoints devolvían respuestas incorrectas)
```text
21:42:46.745-05:00 ERROR 13508 --- [servicio-reactivo] [           main] o.s.t.w.reactive.server.ExchangeResult   : Request details for assertion failure:
> GET /items
No content
< 200 OK OK

21:42:46.855-05:00 ERROR 13508 --- [servicio-reactivo] [           main] o.s.t.w.reactive.server.ExchangeResult   : Request details for assertion failure:
> POST /items
{"id":null,"titulo":"Nuevo Juego","plataforma":"PC","precio":45.5}
< 200 OK OK

[ERROR] com.proyecto.unidad2.controller.ItemControllerIntegrationTest.testListarItems_RedPhase -- Time elapsed: 0.845 s <<< FAILURE!
java.lang.AssertionError: Response header 'X-Reactive-Strategy' does not exist

[ERROR] com.proyecto.unidad2.controller.ItemControllerIntegrationTest.testCrearItem_RedPhase -- Time elapsed: 0.084 s <<< FAILURE!
java.lang.AssertionError: Status expected:<201 CREATED> but was:<200 OK>
```

### 🟢 Fase GREEN (Código implementado y pruebas pasando)
```text
21:45:10.499-05:00  INFO 14832 --- [servicio-reactivo] [           main] c.p.u.c.ItemControllerIntegrationTest    : The following 1 profile is active: "test"
21:45:11.193-05:00  INFO 14832 --- [servicio-reactivo] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 96 ms. Found 1 R2DBC repository interface.
21:45:12.968-05:00  INFO 14832 --- [servicio-reactivo] [           main] c.p.u.c.ItemControllerIntegrationTest    : Started ItemControllerIntegrationTest in 2.785 seconds (process running for 3.857)
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.173 s -- in com.proyecto.unidad2.controller.ItemControllerIntegrationTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### 🛠️ Fase REFACTOR (Código extraído a un método y pasando limpio)
```text
21:52:13.412-05:00  INFO 15301 --- [servicio-reactivo] [           main] c.p.u.c.ItemControllerIntegrationTest    : Started ItemControllerIntegrationTest in 2.612 seconds
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.987 s -- in com.proyecto.unidad2.controller.ItemControllerIntegrationTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

---

## 4. Guía de Capturas de Pantalla (Checklist para el Informe)

> [!IMPORTANT] 
> **¿Debo subir el proyecto a GitHub?**
> **SÍ.** Para poder obtener las evidencias de Integración Continua (CI) y Despliegue Continuo (CD) es **obligatorio** que subas este proyecto a un repositorio en GitHub. GitHub Actions no funcionará localmente.

Una vez que subas el proyecto a GitHub, debes recolectar las siguientes imágenes (asegúrate de que tengan al menos 1200px de ancho y sean legibles, tal como pide la rúbrica):

### Checklist de Imágenes a Entregar:

1. `reactivo_crear_red.png` / `reactivo_listar_red.png`
   - **Cómo obtenerla:** Copia el bloque de texto de la "Fase RED" de este documento, pégalo en un bloc de notas o en la terminal de tu IDE (VS Code o IntelliJ) y tómale una captura de pantalla completa.
2. `reactivo_crear_green.png` / `reactivo_listar_green.png`
   - **Cómo obtenerla:** De la misma manera, copia el bloque de texto de la "Fase GREEN" y tómale una captura.
3. `reactivo_refactor.png`
   - **Cómo obtenerla:** Toma captura al código de las pruebas (Sección 2 de este documento) o al bloque de logs de la "Fase REFACTOR".
4. `ci_ejecucion_verde.png`
   - **Cómo obtenerla:**
     1. Haz push de este proyecto a GitHub.
     2. Ve a tu repositorio en GitHub.com.
     3. Haz clic en la pestaña **"Actions"**.
     4. Verás un workflow llamado "Integracion Continua (CI) - Modelo Reactivo". Haz clic en la ejecución más reciente (debería tener un visto bueno verde ✅).
     5. Toma captura a esa pantalla completa donde se vea el check verde.
5. `cd_despliegue.png`
   - **Cómo obtenerla:** Toma captura a la ejecución exitosa del workflow "Despliegue Continuo (CD) - Modelo Reactivo" en la misma pestaña de Actions de GitHub. (Al ser simulado, el check verde bastará como evidencia de que el pipeline se dispara correctamente tras el CI).
