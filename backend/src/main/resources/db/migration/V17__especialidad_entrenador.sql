-- V17__especialidad_entrenador.sql
-- Flyway: catalogo de especialidades para Entrenador, reemplaza el texto
-- libre por una FK -- mismo criterio que se aplico antes con
-- deportivo.categorias para el campo categoria de Estudiante.

CREATE TABLE IF NOT EXISTS deportivo.especialidades (
    id_especialidad BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO deportivo.especialidades (nombre) VALUES
    ('Preparador físico'),
    ('Técnico'),
    ('Táctico'),
    ('Porteros'),
    ('Fuerza y acondicionamiento'),
    ('Físico')
ON CONFLICT (nombre) DO NOTHING;

ALTER TABLE deportivo.entrenadores
    ADD COLUMN IF NOT EXISTS id_especialidad BIGINT
        REFERENCES deportivo.especialidades(id_especialidad);

ALTER TABLE deportivo.entrenadores
    DROP COLUMN IF EXISTS especialidad;
