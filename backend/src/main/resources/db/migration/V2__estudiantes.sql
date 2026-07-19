-- V2__estudiantes.sql
-- Flyway: tabla de estudiantes del modulo seguridad.

CREATE TABLE IF NOT EXISTS seguridad.estudiantes (
    id_estudiante BIGSERIAL PRIMARY KEY,
    id_persona BIGINT REFERENCES seguridad.personas(id_persona),
    categoria VARCHAR(25),
    fecha_ingreso TIMESTAMPTZ,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION seguridad.set_estudiante_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_estudiantes_actualizado_en
BEFORE UPDATE ON seguridad.estudiantes
FOR EACH ROW EXECUTE FUNCTION seguridad.set_estudiante_actualizado_en();
