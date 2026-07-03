-- ============================================================
-- V3: Dominio deportivo
-- Esquema nuevo "deportivo": entrenadores, posiciones,
-- horarios, sesiones de entrenamiento y asistencias.
-- Aditivo: no modifica ni elimina nada existente en "seguridad".
-- ============================================================

CREATE SCHEMA IF NOT EXISTS deportivo;

-- Función genérica reutilizable para mantener actualizado_en
CREATE OR REPLACE FUNCTION deportivo.set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ------------------------------------------------------------
-- Posiciones de juego (portero, defensa, mediocampista, etc.)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.posiciones (
    id_posicion BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    abreviatura VARCHAR(5),
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO deportivo.posiciones (nombre, abreviatura, descripcion) VALUES
    ('Portero', 'POR', 'Guardameta'),
    ('Defensa central', 'DFC', 'Defensa por el centro'),
    ('Lateral derecho', 'LD', 'Defensa por banda derecha'),
    ('Lateral izquierdo', 'LI', 'Defensa por banda izquierda'),
    ('Mediocentro defensivo', 'MCD', 'Contención en el mediocampo'),
    ('Mediocentro', 'MC', 'Organización y distribución'),
    ('Mediapunta', 'MP', 'Creación entre líneas'),
    ('Extremo derecho', 'ED', 'Ataque por banda derecha'),
    ('Extremo izquierdo', 'EI', 'Ataque por banda izquierda'),
    ('Delantero centro', 'DC', 'Referencia ofensiva')
ON CONFLICT (nombre) DO NOTHING;

-- ------------------------------------------------------------
-- Entrenadores (vinculados a personas del esquema seguridad)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.entrenadores (
    id_entrenador BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL REFERENCES seguridad.personas(id_persona),
    especialidad VARCHAR(100),
    fecha_contratacion DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_entrenadores_persona
    ON deportivo.entrenadores(id_persona);

CREATE TRIGGER trg_entrenadores_actualizado_en
BEFORE UPDATE ON deportivo.entrenadores
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

-- ------------------------------------------------------------
-- Ampliación aditiva de estudiantes: posición y código RFID
-- ------------------------------------------------------------
ALTER TABLE seguridad.estudiantes
    ADD COLUMN IF NOT EXISTS id_posicion BIGINT REFERENCES deportivo.posiciones(id_posicion),
    ADD COLUMN IF NOT EXISTS rfid_codigo VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS idx_estudiantes_rfid
    ON seguridad.estudiantes(rfid_codigo)
    WHERE rfid_codigo IS NOT NULL;

-- ------------------------------------------------------------
-- Horarios recurrentes de entrenamiento por categoría
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.horarios_entrenamiento (
    id_horario BIGSERIAL PRIMARY KEY,
    id_entrenador BIGINT NOT NULL REFERENCES deportivo.entrenadores(id_entrenador),
    categoria VARCHAR(25) NOT NULL,
    dia_semana SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7), -- 1=Lunes ... 7=Domingo
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL CHECK (hora_fin > hora_inicio),
    campo VARCHAR(100),
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_horarios_actualizado_en
BEFORE UPDATE ON deportivo.horarios_entrenamiento
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

-- ------------------------------------------------------------
-- Sesiones de entrenamiento (instancias concretas en una fecha)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.sesiones_entrenamiento (
    id_sesion BIGSERIAL PRIMARY KEY,
    id_horario BIGINT REFERENCES deportivo.horarios_entrenamiento(id_horario),
    id_entrenador BIGINT NOT NULL REFERENCES deportivo.entrenadores(id_entrenador),
    categoria VARCHAR(25) NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME,
    hora_fin TIME,
    campo VARCHAR(100),
    estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADA'
        CHECK (estado IN ('PROGRAMADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sesiones_fecha
    ON deportivo.sesiones_entrenamiento(fecha);
CREATE INDEX IF NOT EXISTS idx_sesiones_entrenador_fecha
    ON deportivo.sesiones_entrenamiento(id_entrenador, fecha);

CREATE TRIGGER trg_sesiones_actualizado_en
BEFORE UPDATE ON deportivo.sesiones_entrenamiento
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

-- ------------------------------------------------------------
-- Asistencias (una por estudiante por sesión)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.asistencias (
    id_asistencia BIGSERIAL PRIMARY KEY,
    id_sesion BIGINT NOT NULL REFERENCES deportivo.sesiones_entrenamiento(id_sesion),
    id_estudiante BIGINT NOT NULL REFERENCES seguridad.estudiantes(id_estudiante),
    hora_entrada TIME,
    metodo VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
        CHECK (metodo IN ('RFID', 'MANUAL')),
    estado VARCHAR(15) NOT NULL
        CHECK (estado IN ('PRESENTE', 'TARDE', 'AUSENTE', 'JUSTIFICADO')),
    observacion VARCHAR(255),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_asistencia_sesion_estudiante UNIQUE (id_sesion, id_estudiante)
);

CREATE INDEX IF NOT EXISTS idx_asistencias_estudiante
    ON deportivo.asistencias(id_estudiante);

CREATE TRIGGER trg_asistencias_actualizado_en
BEFORE UPDATE ON deportivo.asistencias
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();
