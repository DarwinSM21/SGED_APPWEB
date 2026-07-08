-- ==============================================================
-- SGED - Esquema consolidado (Entrega 3, Bloque B.1)
-- Generado a partir de las migraciones Flyway V1..V5.
-- Se monta en /docker-entrypoint-initdb.d/ para reproducibilidad
-- desde clonación limpia. Prohibido ddl-auto=update.
-- ==============================================================
CREATE SCHEMA IF NOT EXISTS deportivo;

CREATE SCHEMA IF NOT EXISTS seguridad;

CREATE TABLE IF NOT EXISTS seguridad.estados_general (
    id_estado_general BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS seguridad.personas (
    id_persona BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    cedula VARCHAR(10),
    correo VARCHAR(255),
    telefono VARCHAR(10),
    fecha_nacimiento DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS seguridad.roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS seguridad.usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    id_persona BIGINT REFERENCES seguridad.personas(id_persona),
    id_estado_general BIGINT REFERENCES seguridad.estados_general(id_estado_general),
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_usuarios_username ON seguridad.usuarios(username);

ALTER TABLE seguridad.usuarios
ADD CONSTRAINT chk_activo CHECK (activo IN (TRUE, FALSE));

CREATE TABLE IF NOT EXISTS seguridad.usuario_rol (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES seguridad.usuarios(id_usuario),
    id_rol BIGINT NOT NULL REFERENCES seguridad.roles(id_rol)
);

CREATE OR REPLACE FUNCTION seguridad.set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_actualizado_en
BEFORE UPDATE ON seguridad.usuarios
FOR EACH ROW EXECUTE FUNCTION seguridad.set_actualizado_en();
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

-- ============================================================
-- V4: Evaluación diaria del entrenador
-- Criterios de evaluación, evaluación por sesión, detalle por
-- estudiante/criterio y observaciones individuales.
-- Regla de negocio (se valida en backend): solo se califica a
-- estudiantes con asistencia PRESENTE o TARDE en la sesión.
-- ============================================================

-- ------------------------------------------------------------
-- Criterios de evaluación configurables
-- ------------------------------------------------------------
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
    ('Técnica', 'Control, pase, conducción y definición', 10),
    ('Condición física', 'Resistencia, velocidad y fuerza', 10),
    ('Táctica', 'Posicionamiento, lectura de juego y toma de decisiones', 10),
    ('Actitud', 'Disciplina, esfuerzo y trabajo en equipo', 10)
ON CONFLICT (nombre) DO NOTHING;

-- ------------------------------------------------------------
-- Evaluación diaria (cabecera: una por sesión de entrenamiento)
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- Detalle de evaluación: puntaje por estudiante y criterio
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- Observaciones individuales por estudiante en una evaluación
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- Vista de apoyo: promedio de un estudiante por evaluación
-- (útil para reportes y para alimentar la plantilla con IA)
-- ------------------------------------------------------------
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

-- fn_contar_estudiantes_activos
-- Propósito: contar estudiantes activos de una categoría (agregado COUNT,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria VARCHAR
-- Salida:   BIGINT (total de estudiantes activos de esa categoría)
-- Tablas:   seguridad.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE FUNCTION seguridad.fn_contar_estudiantes_activos(
    p_categoria VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_total BIGINT;
BEGIN
    SELECT COUNT(*)
      INTO v_total
      FROM seguridad.estudiantes e
     WHERE e.activo = TRUE
       AND e.categoria = p_categoria;
    RETURN v_total;
END;
$$;

-- fn_desactivar_estudiantes_categoria
-- Propósito: baja lógica masiva de todos los estudiantes activos de una
--            categoría (actualización masiva con criterio de negocio,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria VARCHAR
-- Salida:   INTEGER (número de filas afectadas)
-- Tablas:   seguridad.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE FUNCTION seguridad.fn_desactivar_estudiantes_categoria(
    p_categoria VARCHAR
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectados INTEGER;
BEGIN
    UPDATE seguridad.estudiantes e
       SET activo = FALSE
     WHERE e.activo = TRUE
       AND e.categoria = p_categoria;

    GET DIAGNOSTICS v_afectados = ROW_COUNT;
    RETURN v_afectados;
END;
$$;
