CREATE TABLE IF NOT EXISTS "items" (
    "id" SERIAL PRIMARY KEY,
    "titulo" VARCHAR(255) NOT NULL,
    "plataforma" VARCHAR(255) NOT NULL,
    "precio" DOUBLE PRECISION NOT NULL
);

-- Insertamos un par de registros para tests
INSERT INTO "items" ("titulo", "plataforma", "precio") VALUES ('Juego H2 1', 'PC', 19.99);
INSERT INTO "items" ("titulo", "plataforma", "precio") VALUES ('Juego H2 2', 'Consola', 29.99);
