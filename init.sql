CREATE TABLE IF NOT EXISTS items (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    plataforma VARCHAR(255) NOT NULL,
    precio DOUBLE PRECISION NOT NULL
);

-- Insertamos 5000 registros de prueba para que los reportes tengan "peso"
INSERT INTO items (titulo, plataforma, precio)
SELECT 
    'Juego ' || i,
    CASE WHEN i % 2 = 0 THEN 'PC' ELSE 'Consola' END,
    (RANDOM() * 50) + 10
FROM generate_series(1, 5000) AS i;
