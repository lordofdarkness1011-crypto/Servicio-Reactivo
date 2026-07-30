package com.proyecto.unidad2.controller;

import com.proyecto.unidad2.model.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import io.r2dbc.spi.ConnectionFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ItemControllerIntegrationTest {

    @TestConfiguration
    static class DatabaseInitializationConfiguration {
        @Bean
        ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
            ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
            initializer.setConnectionFactory(connectionFactory);
            initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema-h2.sql")));
            return initializer;
        }
    }

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        this.webTestClient = WebTestClient.bindToApplicationContext(this.context).build();
    }

    @Test
    public void testCrearItem_RedPhase() {
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
        // The test expects 201 CREATED, but the current controller returns 200 OK.
        // This will fail (Red phase).
        response.expectStatus().isCreated();
    }

    @Test
    public void testListarItems_RedPhase() {
        // Arrange (No body needed for GET, setup endpoint config if needed)
        // We assume we want 2 items returned initially.

        // Act
        var response = webTestClient.get()
                .uri("/items")
                .exchange();

        // Assert
        // We will assert that a custom header "X-Reactive-Strategy" exists.
        // It currently doesn't, so it will fail (Red phase).
        response.expectStatus().isOk()
                .expectHeader().exists("X-Reactive-Strategy");
    }
}
