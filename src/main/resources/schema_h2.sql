DROP TABLE IF EXISTS votos;
DROP TABLE IF EXISTS opciones;
DROP TABLE IF EXISTS encuesta;

CREATE TABLE encuesta (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    titulo      VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activa      BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin   TIMESTAMP NULL
);

CREATE TABLE opciones (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id INT NOT NULL,
    texto       VARCHAR(200) NOT NULL,
    orden       INT NOT NULL DEFAULT 0,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta(id) ON DELETE CASCADE
);

CREATE TABLE votos (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    encuesta_id     INT NOT NULL,
    opcion_id       INT NOT NULL,
    nombre_votante  VARCHAR(100) NULL,
    fecha           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta(id) ON DELETE CASCADE,
    FOREIGN KEY (opcion_id) REFERENCES opciones(id) ON DELETE CASCADE
);

INSERT INTO encuesta (titulo, descripcion, activa)
VALUES ('¿Cuál es tu framework web favorito?', 'Selecciona la opción que consideres ideal para desarrollo moderno.', true);

SET @encuesta_id = 1;

INSERT INTO opciones (encuesta_id, texto, orden) VALUES
    (@encuesta_id, 'Spring Boot',  1),
    (@encuesta_id, 'Jakarta EE / JSF',  2),
    (@encuesta_id, 'Django / Python', 3),
    (@encuesta_id, 'Laravel / PHP',   4);
