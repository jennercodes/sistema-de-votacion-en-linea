-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS votacion_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE votacion_db;

-- Eliminar tablas en orden inverso a las dependencias
DROP TABLE IF EXISTS auditoria_admin;
DROP TABLE IF EXISTS votos;
DROP TABLE IF EXISTS registro_participacion;
DROP TABLE IF EXISTS opciones;
DROP TABLE IF EXISTS encuesta;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS usuario;

-- 1. Tabla de Usuarios (Soporta Autenticación con BCrypt)
CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    rol ENUM('ADMIN', 'VOTANTE') NOT NULL DEFAULT 'VOTANTE',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Tabla de Categorías
CREATE TABLE categoria (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NULL
) ENGINE=InnoDB;

-- 3. Tabla de Encuestas (Relacionada con Categoria)
CREATE TABLE encuesta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    categoria_id INT NULL,
    titulo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activa BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP NULL,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 4. Tabla de Opciones
CREATE TABLE opciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id INT NOT NULL,
    texto VARCHAR(200) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Tabla de Registro de Participación (Garantiza voto único por usuario en cada encuesta)
CREATE TABLE registro_participacion (
    usuario_id INT NOT NULL,
    encuesta_id INT NOT NULL,
    fecha_voto TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, encuesta_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Tabla de Votos (Normalizada, sin encuesta_id redundante)
CREATE TABLE votos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    encuesta_id INT NOT NULL, -- Requerido para verificar participación mediante FK compuesta
    opcion_id INT NOT NULL,
    FOREIGN KEY (usuario_id, encuesta_id) REFERENCES registro_participacion(usuario_id, encuesta_id) ON DELETE CASCADE,
    FOREIGN KEY (opcion_id) REFERENCES opciones(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Tabla de Auditoría para Administradores
CREATE TABLE auditoria_admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    accion VARCHAR(100) NOT NULL,
    detalles TEXT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================================
-- DATOS SEMILLA (Inicialización)
-- Se usan IDs explícitos para que las referencias de votos sean deterministas
-- y el conjunto de datos de ejemplo sea abundante y reproducible.
-- =========================================================================

-- Categorías (5)
INSERT INTO categoria (id, nombre, descripcion) VALUES
    (1, 'Programación',   'Lenguajes, frameworks y desarrollo de software'),
    (2, 'Deportes',       'Fútbol, básquetbol, tenis y actividades deportivas'),
    (3, 'Cultura General','Ciencia, historia, geografía y temas de interés general'),
    (4, 'Tecnología',     'Dispositivos, gadgets y tendencias tecnológicas'),
    (5, 'Entretenimiento','Cine, series, música y ocio');

-- Usuarios (15)
-- Contraseñas:
--   admin / admin123  (ADMIN)
--   juan  / juan123        maria / maria123
--   ana, luis, marta / juan123   (votantes de ejemplo previos)
--   resto (pedro..mateo) / demo123
INSERT INTO usuario (id, username, password_hash, email, rol) VALUES
    (1,  'admin', '$2a$10$N.6PY9AFggIZvwxXfbpmRO4e8KnWIn3PZMCzcJTzvLLyy6HP0F9Cu', 'admin@votacion.com', 'ADMIN'),
    (2,  'juan',  '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'juan@gmail.com',   'VOTANTE'),
    (3,  'maria', '$2a$10$Ex14DXjA86yIXuIhGHtIEO7zR.rT4fM.M6fNukT/qXbJu98WOnmU2', 'maria@gmail.com',  'VOTANTE'),
    (4,  'ana',   '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'ana@gmail.com',    'VOTANTE'),
    (5,  'luis',  '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'luis@gmail.com',   'VOTANTE'),
    (6,  'marta', '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'marta@gmail.com',  'VOTANTE'),
    (7,  'pedro',     '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'pedro@gmail.com',     'VOTANTE'),
    (8,  'sofia',     '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'sofia@gmail.com',     'VOTANTE'),
    (9,  'diego',     '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'diego@gmail.com',     'VOTANTE'),
    (10, 'valentina', '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'valentina@gmail.com', 'VOTANTE'),
    (11, 'carlos',    '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'carlos@gmail.com',    'VOTANTE'),
    (12, 'lucia',     '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'lucia@gmail.com',     'VOTANTE'),
    (13, 'andres',    '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'andres@gmail.com',    'VOTANTE'),
    (14, 'camila',    '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'camila@gmail.com',    'VOTANTE'),
    (15, 'mateo',     '$2a$10$Umjkky9C0ciXhNWryKJFO.OCqx9v3R5kvQJMd7gJpK/.GW0FMaAtS', 'mateo@gmail.com',     'VOTANTE');

-- Encuestas (8). Algunas con fecha_fin para mostrar cuenta regresiva.
INSERT INTO encuesta (id, categoria_id, titulo, descripcion, fecha_fin) VALUES
    (1, 1, '¿Cuál es tu framework web favorito?',        'Vota por el framework que más usas en tus proyectos web.',       DATE_ADD(NOW(), INTERVAL 20 DAY)),
    (2, 1, '¿Qué lenguaje prefieres para el backend?',   'El lenguaje con el que más cómodo te sientes del lado servidor.', DATE_ADD(NOW(), INTERVAL 12 DAY)),
    (3, 1, '¿Cuál es tu editor de código favorito?',     'El editor o IDE en el que pasas la mayor parte del día.',        NULL),
    (4, 2, '¿Cuál es el mejor deporte para ver en vivo?','Nada como el ambiente del estadio o la cancha.',                 DATE_ADD(NOW(), INTERVAL 8 DAY)),
    (5, 2, '¿Qué equipo ganará la próxima Champions?',   'Tu favorito para levantar la orejona esta temporada.',           DATE_ADD(NOW(), INTERVAL 45 DAY)),
    (6, 3, 'Si pudieras explorar un planeta, ¿cuál?',    'Un viaje sin retorno de presupuesto ilimitado.',                 NULL),
    (7, 4, '¿Qué dispositivo usas más para conectarte?', 'Ese que tienes siempre a mano para navegar.',                    NULL),
    (8, 5, '¿Qué género de películas prefieres?',        'El género que eliges primero un viernes por la noche.',          DATE_ADD(NOW(), INTERVAL 30 DAY));

-- Opciones (ids explícitos, agrupadas por encuesta)
INSERT INTO opciones (id, encuesta_id, texto, orden) VALUES
    -- E1 framework
    (1,  1, 'Spring',  1), (2,  1, 'Django', 2), (3,  1, 'Laravel', 3), (4,  1, 'Rails', 4),
    -- E2 backend
    (5,  2, 'Java', 1), (6,  2, 'Python', 2), (7,  2, 'JavaScript (Node.js)', 3), (8,  2, 'Go', 4), (9,  2, 'PHP', 5),
    -- E3 editor
    (10, 3, 'VS Code', 1), (11, 3, 'IntelliJ IDEA', 2), (12, 3, 'Neovim', 3), (13, 3, 'Sublime Text', 4),
    -- E4 deporte
    (14, 4, 'Fútbol', 1), (15, 4, 'Básquetbol', 2), (16, 4, 'Tenis', 3), (17, 4, 'Vóley', 4),
    -- E5 champions
    (18, 5, 'Real Madrid', 1), (19, 5, 'Manchester City', 2), (20, 5, 'Bayern Múnich', 3), (21, 5, 'PSG', 4),
    -- E6 planeta
    (22, 6, 'Marte', 1), (23, 6, 'Júpiter', 2), (24, 6, 'Saturno', 3), (25, 6, 'Venus', 4),
    -- E7 dispositivo
    (26, 7, 'Smartphone', 1), (27, 7, 'Laptop', 2), (28, 7, 'Tablet', 3), (29, 7, 'PC de escritorio', 4),
    -- E8 películas
    (30, 8, 'Acción', 1), (31, 8, 'Comedia', 2), (32, 8, 'Terror', 3), (33, 8, 'Ciencia ficción', 4), (34, 8, 'Drama', 5);

-- Registro de participación (voto único por usuario/encuesta)
INSERT INTO registro_participacion (usuario_id, encuesta_id) VALUES
    -- E1 (12 votos)
    (2,1),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1),(9,1),(10,1),(11,1),(13,1),(14,1),
    -- E2 (14 votos)
    (2,2),(3,2),(4,2),(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(11,2),(12,2),(13,2),(14,2),(15,2),
    -- E3 (5 votos)
    (2,3),(4,3),(5,3),(9,3),(13,3),
    -- E4 (9 votos)
    (2,4),(3,4),(4,4),(5,4),(7,4),(8,4),(9,4),(11,4),(12,4),
    -- E5 (4 votos)
    (2,5),(3,5),(7,5),(11,5),
    -- E7 (7 votos)
    (2,7),(3,7),(6,7),(9,7),(11,7),(14,7),(15,7),
    -- E8 (13 votos)
    (2,8),(3,8),(4,8),(5,8),(6,8),(7,8),(8,8),(9,8),(10,8),(11,8),(12,8),(13,8),(14,8);

-- Votos (cada fila referencia la participación anterior)
INSERT INTO votos (usuario_id, encuesta_id, opcion_id) VALUES
    -- E1: Spring 5, Django 3, Laravel 3, Rails 1
    (2,1,1),(5,1,1),(8,1,1),(10,1,1),(13,1,1),
    (3,1,2),(6,1,2),(11,1,2),
    (4,1,3),(9,1,3),(14,1,3),
    (7,1,4),
    -- E2: Java 4, Python 5, JS 3, Go 1, PHP 1
    (2,2,5),(4,2,5),(7,2,5),(11,2,5),
    (3,2,6),(5,2,6),(8,2,6),(12,2,6),(15,2,6),
    (6,2,7),(9,2,7),(14,2,7),
    (10,2,8),
    (13,2,9),
    -- E3: VS Code 3, IntelliJ 1, Neovim 1
    (2,3,10),(5,3,10),(9,3,10),
    (4,3,11),
    (13,3,12),
    -- E4: Fútbol 5, Básquetbol 2, Tenis 1, Vóley 1
    (2,4,14),(3,4,14),(5,4,14),(7,4,14),(12,4,14),
    (4,4,15),(9,4,15),
    (8,4,16),
    (11,4,17),
    -- E5: Real Madrid 2, Man City 1, Bayern 1
    (2,5,18),(7,5,18),
    (3,5,19),
    (11,5,20),
    -- E7: Smartphone 4, Laptop 2, Tablet 1
    (3,7,26),(6,7,26),(9,7,26),(14,7,26),
    (2,7,27),(11,7,27),
    (15,7,28),
    -- E8: Acción 4, Comedia 3, Terror 2, Ciencia ficción 3, Drama 1
    (2,8,30),(5,8,30),(8,8,30),(11,8,30),
    (3,8,31),(7,8,31),(12,8,31),
    (4,8,32),(9,8,32),
    (6,8,33),(10,8,33),(14,8,33),
    (13,8,34);
