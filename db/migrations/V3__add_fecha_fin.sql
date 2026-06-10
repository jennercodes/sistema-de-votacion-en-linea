USE votacion_db;

-- Agregar columna fecha_fin a la tabla encuesta si no existe
ALTER TABLE encuesta
    ADD COLUMN IF NOT EXISTS fecha_fin TIMESTAMP NULL;
