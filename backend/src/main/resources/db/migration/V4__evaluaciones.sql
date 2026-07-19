-- V4__evaluaciones.sql
-- Flyway: evaluacion diaria del entrenador - criterios, evaluaciones, detalle, observaciones.

-- Criterios de evaluacion configurables
CREATE TABLE IF NOT EXISTS deportivo.criterios_evaluacion (
    id_criterio BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    puntaje_maximo SMALLINT NOT NULL DEFAULT 10 CHECK (puntaje_maximo > 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_criterios_actualizado_en
BEFORE UPDATE ON deportivo.criterios_evaluacion
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

INSERT INTO deportivo.criterios_evaluacion (nombre, descripcion, puntaje_maximo) VALUES
    ('Tecnica', 'Control, pase, conduccion y definicion', 10),
    ('Condicion fisica', 'Resistencia, velocidad y fuerza', 10),
    ('Tactica', 'Posicionamiento, lectura de juego y toma de decisiones', 10),
    ('Actitud', 'Disciplina, esfuerzo y trabajo en equipo', 10)
ON CONFLICT (nombre) DO NOTHING;

-- Evaluacion diaria (cabecera)
CREATE TABLE IF NOT EXISTS deportivo.evaluaciones_diarias (
    id_evaluacion BIGSERIAL PRIMARY KEY,
    id_sesion BIGINT NOT NULL REFERENCES deportivo.sesiones_entrenamiento(id_sesion),
    id_entrenador BIGINT NOT NULL REFERENCES deportivo.entrenadores(id_entrenador),
    fecha DATE NOT NULL,
    observacion_general TEXT,
    estado VARCHAR(15) NOT NULL DEFAULT 'BORRADOR'
        CHECK (estado IN ('BORRADOR', 'FINALIZADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evaluacion_sesion UNIQUE (id_sesion)
);

CREATE INDEX IF NOT EXISTS idx_evaluaciones_fecha
    ON deportivo.evaluaciones_diarias(fecha);

CREATE TRIGGER trg_evaluaciones_actualizado_en
BEFORE UPDATE ON deportivo.evaluaciones_diarias
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

-- Detalle de evaluacion: puntaje por estudiante y criterio
CREATE TABLE IF NOT EXISTS deportivo.detalle_evaluacion (
    id_detalle BIGSERIAL PRIMARY KEY,
    id_evaluacion BIGINT NOT NULL REFERENCES deportivo.evaluaciones_diarias(id_evaluacion) ON DELETE CASCADE,
    id_estudiante BIGINT NOT NULL REFERENCES seguridad.estudiantes(id_estudiante),
    id_criterio BIGINT NOT NULL REFERENCES deportivo.criterios_evaluacion(id_criterio),
    id_posicion_jugada BIGINT REFERENCES deportivo.posiciones(id_posicion),
    puntaje NUMERIC(4,1) NOT NULL CHECK (puntaje >= 0),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_detalle_eval_est_criterio UNIQUE (id_evaluacion, id_estudiante, id_criterio)
);

CREATE INDEX IF NOT EXISTS idx_detalle_estudiante
    ON deportivo.detalle_evaluacion(id_estudiante);

CREATE TRIGGER trg_detalle_actualizado_en
BEFORE UPDATE ON deportivo.detalle_evaluacion
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

-- Observaciones individuales
CREATE TABLE IF NOT EXISTS deportivo.observaciones_estudiante (
    id_observacion BIGSERIAL PRIMARY KEY,
    id_evaluacion BIGINT NOT NULL REFERENCES deportivo.evaluaciones_diarias(id_evaluacion) ON DELETE CASCADE,
    id_estudiante BIGINT NOT NULL REFERENCES seguridad.estudiantes(id_estudiante),
    id_entrenador BIGINT NOT NULL REFERENCES deportivo.entrenadores(id_entrenador),
    texto TEXT NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_observaciones_estudiante
    ON deportivo.observaciones_estudiante(id_estudiante);

CREATE TRIGGER trg_observaciones_actualizado_en
BEFORE UPDATE ON deportivo.observaciones_estudiante
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

-- Vista de promedio de evaluacion
CREATE OR REPLACE VIEW deportivo.v_promedio_evaluacion AS
SELECT
    d.id_evaluacion,
    d.id_estudiante,
    e.fecha,
    ROUND(AVG(d.puntaje), 1) AS promedio,
    COUNT(*) AS criterios_evaluados
FROM deportivo.detalle_evaluacion d
JOIN deportivo.evaluaciones_diarias e ON e.id_evaluacion = d.id_evaluacion
GROUP BY d.id_evaluacion, d.id_estudiante, e.fecha;
