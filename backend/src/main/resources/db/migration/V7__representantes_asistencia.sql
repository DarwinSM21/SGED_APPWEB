-- ============================================================
-- V7: Representantes, relación estudiante-representante,
-- entrenador-categoría, sesión de entrenamiento y asistencia
-- normalizada (catálogo estado_asistencia).
--
-- Estructura alineada 1:1 con la base de datos real (Supabase).
-- Aditivo respecto a deportivo.sesiones_entrenamiento / asistencias
-- / evaluaciones_* (V3/V4): esas tablas nunca tuvieron código Java
-- y no se tocan aquí; quedan pendientes de una limpieza aparte.
-- ============================================================

-- ------------------------------------------------------------
-- Alineación de academico.estudiantes con la BD real:
-- una persona solo puede tener un registro de estudiante, y
-- peso/altura deben ser positivos cuando se informan.
-- ------------------------------------------------------------
ALTER TABLE academico.estudiantes
    ADD CONSTRAINT estudiantes_id_persona_key UNIQUE (id_persona);

ALTER TABLE academico.estudiantes
    ADD CONSTRAINT estudiantes_peso_check CHECK (peso > 0),
    ADD CONSTRAINT estudiantes_altura_check CHECK (altura > 0);

-- ------------------------------------------------------------
-- Catálogo de estados de asistencia
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.estado_asistencia (
    id_estado_asistencia BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT estado_asistencia_nombre_key UNIQUE (nombre)
);

INSERT INTO deportivo.estado_asistencia (nombre) VALUES
    ('PRESENTE'), ('TARDE'), ('AUSENTE'), ('JUSTIFICADO')
ON CONFLICT (nombre) DO NOTHING;

-- ------------------------------------------------------------
-- Asignación de entrenadores a categorías
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.entrenador_categoria (
    id_entrenador_categoria BIGSERIAL PRIMARY KEY,
    id_entrenador BIGINT NOT NULL REFERENCES deportivo.entrenadores(id_entrenador),
    id_categoria BIGINT NOT NULL REFERENCES deportivo.categorias(id_categoria),
    entrenador_principal BOOLEAN DEFAULT TRUE,
    fecha_asignacion DATE DEFAULT CURRENT_DATE,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_entrenador_categoria UNIQUE (id_entrenador, id_categoria)
);

CREATE TRIGGER trg_entrenador_categoria_updated_at
BEFORE UPDATE ON deportivo.entrenador_categoria
FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();

-- ------------------------------------------------------------
-- Sesiones de entrenamiento (instancia concreta de una
-- categoría+entrenador en una fecha/hora)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.sesion_entrenamiento (
    id_sesion_entrenamiento BIGSERIAL PRIMARY KEY,
    id_entrenador_categoria BIGINT NOT NULL REFERENCES deportivo.entrenador_categoria(id_entrenador_categoria),
    id_estado_general BIGINT REFERENCES seguridad.estados_general(id_estado_general),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT sesion_entrenamiento_check CHECK (hora_fin > hora_inicio)
);

-- ------------------------------------------------------------
-- Representantes (tutores/padres) vinculados a personas
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS academico.representantes (
    id_representante BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL REFERENCES seguridad.personas(id_persona),
    ocupacion VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE
);

-- ------------------------------------------------------------
-- Relación N:M estudiante-representante con datos propios.
-- Nota: el FK de id_estudiante no venía en el DDL compartido;
-- se añade aquí por consistencia referencial con el resto del
-- esquema (toda columna id_x NOT NULL referencia su tabla).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS academico.estudiante_representante (
    id_estudiante_representante BIGSERIAL PRIMARY KEY,
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    id_representante BIGINT NOT NULL REFERENCES academico.representantes(id_representante),
    relacion VARCHAR(50) NOT NULL,
    contacto_principal BOOLEAN DEFAULT FALSE
);

-- ------------------------------------------------------------
-- Asistencia de un estudiante a una sesión de entrenamiento
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS academico.asistencia (
    id_asistencia BIGSERIAL PRIMARY KEY,
    id_sesion_entrenamiento BIGINT NOT NULL REFERENCES deportivo.sesion_entrenamiento(id_sesion_entrenamiento),
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    id_estado_asistencia BIGINT NOT NULL REFERENCES deportivo.estado_asistencia(id_estado_asistencia),
    fecha_registro DATE DEFAULT CURRENT_DATE
);
