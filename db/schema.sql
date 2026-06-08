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
-- =========================================================================

-- Categorías
INSERT INTO categoria (nombre, descripcion) VALUES
    ('Programación', 'Encuestas relacionadas con lenguajes, frameworks y desarrollo de software'),
    ('Deportes', 'Fútbol, básquetbol, tenis y actividades deportivas locales o mundiales'),
    ('Cultura General', 'Preguntas sobre ciencia, historia, geografía y temas de interés general');

-- Usuarios semilla
-- admin / admin123  (BCrypt)
-- juan / juan123    (BCrypt)
-- maria / maria123  (BCrypt)
-- ana / juan123, luis / juan123, marta / juan123 (Votantes para datos de ejemplo)
INSERT INTO usuario (username, password_hash, email, rol) VALUES
    ('admin', '$2a$10$N.6PY9AFggIZvwxXfbpmRO4e8KnWIn3PZMCzcJTzvLLyy6HP0F9Cu', 'admin@votacion.com', 'ADMIN'),
    ('juan', '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'juan@gmail.com', 'VOTANTE'),
    ('maria', '$2a$10$Ex14DXjA86yIXuIhGHtIEO7zR.rT4fM.M6fNukT/qXbJu98WOnmU2', 'maria@gmail.com', 'VOTANTE'),
    ('ana', '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'ana@gmail.com', 'VOTANTE'),
    ('luis', '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'luis@gmail.com', 'VOTANTE'),
    ('marta', '$2a$10$8WdnbQQZq5HkG2f4O0vLG.JzqXRteCd4J6gSsZTusx.nDB3Sy.Rdi', 'marta@gmail.com', 'VOTANTE');

-- Encuesta inicial
INSERT INTO encuesta (categoria_id, titulo, descripcion)
VALUES (
    (SELECT id FROM categoria WHERE nombre = 'Programación'),
    '¿Cuál es tu framework web favorito?',
    'Vota por el framework que más utilizas en tus proyectos de desarrollo web.'
);

SET @encuesta_id = LAST_INSERT_ID();

-- Opciones de la encuesta
INSERT INTO opciones (encuesta_id, texto, orden) VALUES
    (@encuesta_id, 'Spring',  1),
    (@encuesta_id, 'Django',  2),
    (@encuesta_id, 'Laravel', 3),
    (@encuesta_id, 'Rails',   4);

-- Registrar votos iniciales para que el gráfico/conteo muestre datos
-- Primero registramos participación:
INSERT INTO registro_participacion (usuario_id, encuesta_id) VALUES
    ((SELECT id FROM usuario WHERE username = 'ana'), @encuesta_id),
    ((SELECT id FROM usuario WHERE username = 'luis'), @encuesta_id),
    ((SELECT id FROM usuario WHERE username = 'marta'), @encuesta_id);

-- Luego registramos los votos correspondientes:
INSERT INTO votos (usuario_id, encuesta_id, opcion_id) VALUES
    ((SELECT id FROM usuario WHERE username = 'ana'), @encuesta_id, (SELECT id FROM opciones WHERE texto = 'Spring' AND encuesta_id = @encuesta_id)),
    ((SELECT id FROM usuario WHERE username = 'luis'), @encuesta_id, (SELECT id FROM opciones WHERE texto = 'Django' AND encuesta_id = @encuesta_id)),
    ((SELECT id FROM usuario WHERE username = 'marta'), @encuesta_id, (SELECT id FROM opciones WHERE texto = 'Spring' AND encuesta_id = @encuesta_id));
