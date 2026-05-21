-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS votacion_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE votacion_db;

-- Eliminar tablas en orden inverso a las dependencias
DROP TABLE IF EXISTS votos;
DROP TABLE IF EXISTS opciones;
DROP TABLE IF EXISTS encuesta;

-- Tabla de encuestas
CREATE TABLE encuesta (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    titulo      VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activa      BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Tabla de opciones (depende de encuesta)
CREATE TABLE opciones (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id INT NOT NULL,
    texto       VARCHAR(200) NOT NULL,
    orden       INT NOT NULL DEFAULT 0,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla de votos (depende de encuesta y opciones)
CREATE TABLE votos (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id     INT NOT NULL,
    opcion_id       INT NOT NULL,
    nombre_votante  VARCHAR(100) NULL,
    fecha           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta(id) ON DELETE CASCADE,
    FOREIGN KEY (opcion_id) REFERENCES opciones(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Encuesta inicial con datos de ejemplo
INSERT INTO encuesta (titulo)
VALUES ('¿Cuál es tu framework web favorito?');

SET @encuesta_id = LAST_INSERT_ID();

INSERT INTO opciones (encuesta_id, texto, orden) VALUES
    (@encuesta_id, 'Spring',  1),
    (@encuesta_id, 'Django',  2),
    (@encuesta_id, 'Laravel', 3),
    (@encuesta_id, 'Rails',   4);

INSERT INTO votos (encuesta_id, opcion_id, nombre_votante)
SELECT o.encuesta_id, o.id, v.nombre
FROM opciones o
JOIN (
    SELECT 'Spring' AS texto, 'Ana'   AS nombre UNION ALL
    SELECT 'Django' AS texto, 'Luis'  AS nombre UNION ALL
    SELECT 'Spring' AS texto, 'Marta' AS nombre
) v ON v.texto = o.texto
WHERE o.encuesta_id = @encuesta_id;
