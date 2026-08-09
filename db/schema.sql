-- ==============================================================
-- SGED - Esquema consolidado (Entrega 3, Bloque B.1)
-- Generado a partir de las migraciones Flyway V1..V5.
-- Se monta en /docker-entrypoint-initdb.d/ para reproducibilidad
-- desde clonación limpia. Prohibido ddl-auto=update.
-- ==============================================================
CREATE SCHEMA IF NOT EXISTS academico;

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
    cedula VARCHAR(10) NOT NULL,
    correo VARCHAR(200) NOT NULL,
    telefono VARCHAR(15),
    foto TEXT,
    fecha_nacimiento DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_personas_cedula ON seguridad.personas(cedula);
CREATE UNIQUE INDEX IF NOT EXISTS idx_personas_correo ON seguridad.personas(correo);

CREATE TABLE IF NOT EXISTS seguridad.roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS seguridad.usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL REFERENCES seguridad.personas(id_persona),
    id_estado_general BIGINT NOT NULL REFERENCES seguridad.estados_general(id_estado_general),
    username VARCHAR(50) NOT NULL,
    password_hash TEXT NOT NULL,
    ultimo_acceso TIMESTAMPTZ,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_username ON seguridad.usuarios(username);

CREATE TABLE IF NOT EXISTS seguridad.usuario_rol (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES seguridad.usuarios(id_usuario),
    id_rol BIGINT NOT NULL REFERENCES seguridad.roles(id_rol)
);

CREATE OR REPLACE FUNCTION seguridad.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_updated_at
BEFORE UPDATE ON seguridad.usuarios
FOR EACH ROW EXECUTE FUNCTION seguridad.set_updated_at();
-- Función genérica reutilizable para mantener actualizado_en
CREATE OR REPLACE FUNCTION deportivo.set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION academico.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ------------------------------------------------------------
-- Categorías de jugadores (sub-12, sub-14, etc.)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.categorias (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    edad_min SMALLINT NOT NULL,
    edad_max SMALLINT NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS academico.estudiantes (
    id_estudiante BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL REFERENCES seguridad.personas(id_persona),
    id_categoria BIGINT NOT NULL REFERENCES deportivo.categorias(id_categoria),
    id_estado_general BIGINT NOT NULL REFERENCES seguridad.estados_general(id_estado_general),
    codigo_estudiante VARCHAR(30) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    peso NUMERIC(5,2),
    altura NUMERIC(5,2),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_estudiantes_codigo
    ON academico.estudiantes(codigo_estudiante);
-- ============================================================
-- V3: Dominio deportivo
-- Esquema nuevo "deportivo": entrenadores, posiciones,
-- horarios, sesiones de entrenamiento y asistencias.
-- Aditivo: no modifica ni elimina nada existente en "seguridad".
-- ============================================================


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
    id_persona BIGINT NOT NULL UNIQUE REFERENCES seguridad.personas(id_persona),
    id_usuario BIGINT NOT NULL UNIQUE REFERENCES seguridad.usuarios(id_usuario),
    especialidad VARCHAR(150),
    experiencia_anios SMALLINT,
    certificacion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Ampliación aditiva de estudiantes: posición y código RFID
-- ------------------------------------------------------------
ALTER TABLE academico.estudiantes
    ADD COLUMN IF NOT EXISTS id_posicion BIGINT REFERENCES deportivo.posiciones(id_posicion),
    ADD COLUMN IF NOT EXISTS rfid_codigo VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS idx_estudiantes_rfid
    ON academico.estudiantes(rfid_codigo)
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
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
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
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
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
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
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

-- sp_contar_estudiantes_activos
-- Propósito: contar estudiantes activos de una categoría (agregado COUNT,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria INT (id_categoria)
-- Salida:   total BIGINT (parametro OUT)
-- Tablas:   academico.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE PROCEDURE academico.sp_contar_estudiantes_activos(
    IN p_categoria INT,
    OUT total BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
      INTO total
      FROM academico.estudiantes e
     WHERE e.activo = TRUE
       AND e.id_categoria = p_categoria;
END;
$$;

-- sp_desactivar_estudiantes_categoria
-- Propósito: baja lógica masiva de todos los estudiantes activos de una
--            categoría (actualización masiva con criterio de negocio,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria INT (id_categoria)
-- Salida:   afectados INTEGER (parametro OUT, numero de filas afectadas)
-- Tablas:   academico.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE PROCEDURE academico.sp_desactivar_estudiantes_categoria(
    IN p_categoria INT,
    OUT afectados INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE academico.estudiantes e
       SET activo = FALSE
     WHERE e.activo = TRUE
       AND e.id_categoria = p_categoria;

    GET DIAGNOSTICS afectados = ROW_COUNT;
END;
$$;

-- ============================================================
-- V7: Modulo Entrenador
--
-- Cubre lo que el documento de diseno del modulo (2026-07-26)
-- exige y el esquema todavia no soportaba:
--   1. Registro de lesiones por parte del entrenador.
--   2. Categoria y posicion del estudiante EN EL DIA de cada
--      evaluacion, no solo su categoria actual.
--   3. Marcado de asistencia por codigo QR.
--   4. Normalizacion de la categoria en horarios y sesiones.
--
-- Aditivo salvo por la reestructuracion de detalle_evaluacion y
-- la normalizacion de 'categoria', que son seguras porque
-- ninguna de esas tablas tiene datos: el dominio deportivo nunca
-- se expuso por REST, asi que nada las ha escrito todavia.
-- ============================================================


-- ------------------------------------------------------------
-- 1. Lesiones
--
-- Las registra el entrenador, no la recepcionista. Una lesion
-- esta activa mientras fecha_alta sea NULL; eso es lo que
-- excluye al jugador de las sugerencias de plantilla y lo que
-- distingue una ausencia por lesion de una falta sin motivo.
--
-- Nota etica: 'descripcion' es texto libre sobre la condicion
-- fisica de un menor de edad. Le aplica el mismo tratamiento que
-- al hallazgo H-02 de docs/etica/ETHICS.md (observaciones libres
-- sin control de contenido) y ademas es un dato de salud, como
-- peso y altura en H-06.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.lesiones (
    id_lesion BIGSERIAL PRIMARY KEY,
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    id_entrenador BIGINT NOT NULL REFERENCES deportivo.entrenadores(id_entrenador),
    descripcion TEXT NOT NULL,
    fecha_lesion DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_estimada_retorno DATE,
    fecha_alta DATE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_lesion_retorno_posterior
        CHECK (fecha_estimada_retorno IS NULL OR fecha_estimada_retorno >= fecha_lesion),
    CONSTRAINT ck_lesion_alta_posterior
        CHECK (fecha_alta IS NULL OR fecha_alta >= fecha_lesion)
);

-- Un estudiante no puede tener dos lesiones activas a la vez.
CREATE UNIQUE INDEX IF NOT EXISTS idx_lesion_activa_por_estudiante
    ON deportivo.lesiones(id_estudiante)
    WHERE fecha_alta IS NULL;

CREATE INDEX IF NOT EXISTS idx_lesiones_estudiante
    ON deportivo.lesiones(id_estudiante);

CREATE TRIGGER trg_lesiones_actualizado_en
BEFORE UPDATE ON deportivo.lesiones
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();


-- ------------------------------------------------------------
-- 2. Evaluacion por estudiante (fila intermedia)
--
-- Antes, detalle_evaluacion guardaba una fila por criterio (4 por
-- jugador) y repetia en cada una la posicion jugada. Eso permitia
-- que las 4 filas de un mismo jugador se contradijeran entre si, y
-- no habia donde guardar la categoria del dia.
--
-- Esta tabla es una fila por jugador evaluado. El documento pide
-- explicitamente guardar "la categoria en la que estaba el
-- estudiante ESE dia", lo que resuelve tres casos:
--   - cambio permanente de categoria: el historial viejo queda
--     etiquetado con la categoria de entonces;
--   - entrenamiento puntual con otro grupo, sin cambiar la
--     categoria oficial;
--   - estudiante eventual sin categoria fija, al que se le asigna
--     una categoria comodin solo para ese registro.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deportivo.evaluacion_estudiante (
    id_evaluacion_estudiante BIGSERIAL PRIMARY KEY,
    id_evaluacion BIGINT NOT NULL
        REFERENCES deportivo.evaluaciones_diarias(id_evaluacion) ON DELETE CASCADE,
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    id_categoria_dia BIGINT NOT NULL REFERENCES deportivo.categorias(id_categoria),
    id_posicion_jugada BIGINT REFERENCES deportivo.posiciones(id_posicion),
    id_lesion BIGINT REFERENCES deportivo.lesiones(id_lesion),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evaluacion_estudiante UNIQUE (id_evaluacion, id_estudiante)
);

CREATE INDEX IF NOT EXISTS idx_eval_estudiante_estudiante
    ON deportivo.evaluacion_estudiante(id_estudiante);
CREATE INDEX IF NOT EXISTS idx_eval_estudiante_categoria_dia
    ON deportivo.evaluacion_estudiante(id_categoria_dia);

CREATE TRIGGER trg_eval_estudiante_actualizado_en
BEFORE UPDATE ON deportivo.evaluacion_estudiante
FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();


-- ------------------------------------------------------------
-- 3. detalle_evaluacion cuelga ahora de evaluacion_estudiante
--
-- Queda reducido a lo que realmente es: el puntaje de un criterio.
-- El estudiante, su categoria del dia y la posicion jugada suben
-- un nivel y dejan de repetirse por criterio.
-- ------------------------------------------------------------
-- La vista v_promedio_evaluacion lee id_evaluacion e id_estudiante
-- directamente de detalle_evaluacion, que es justo lo que esta migracion
-- mueve un nivel arriba. PostgreSQL impide alterar una columna de la que
-- depende una vista, asi que se retira aqui y se vuelve a crear al final
-- leyendo desde evaluacion_estudiante.
DROP VIEW IF EXISTS deportivo.v_promedio_evaluacion;

ALTER TABLE deportivo.detalle_evaluacion
    ADD COLUMN IF NOT EXISTS id_evaluacion_estudiante BIGINT
        REFERENCES deportivo.evaluacion_estudiante(id_evaluacion_estudiante) ON DELETE CASCADE;

-- Sin datos previos: se puede exigir NOT NULL de inmediato.
ALTER TABLE deportivo.detalle_evaluacion
    ALTER COLUMN id_evaluacion_estudiante SET NOT NULL;

ALTER TABLE deportivo.detalle_evaluacion
    DROP CONSTRAINT IF EXISTS uq_detalle_eval_est_criterio;

ALTER TABLE deportivo.detalle_evaluacion
    DROP COLUMN IF EXISTS id_evaluacion,
    DROP COLUMN IF EXISTS id_estudiante,
    DROP COLUMN IF EXISTS id_posicion_jugada;

ALTER TABLE deportivo.detalle_evaluacion
    ADD CONSTRAINT uq_detalle_evaluacion_criterio
        UNIQUE (id_evaluacion_estudiante, id_criterio);

-- Vista reconstruida sobre la nueva estructura. Gana una columna:
-- id_categoria_dia permite promediar el historial por la categoria en la
-- que el estudiante estaba en cada fecha, no por la que tiene hoy. Ese es
-- justamente el caso que el documento del modulo pide resolver cuando un
-- jugador cambia de categoria a mitad de temporada.
CREATE OR REPLACE VIEW deportivo.v_promedio_evaluacion AS
SELECT
    ee.id_evaluacion,
    ee.id_estudiante,
    ee.id_categoria_dia,
    e.fecha,
    ROUND(AVG(d.puntaje), 1) AS promedio,
    COUNT(*) AS criterios_evaluados
FROM deportivo.detalle_evaluacion d
JOIN deportivo.evaluacion_estudiante ee
    ON ee.id_evaluacion_estudiante = d.id_evaluacion_estudiante
JOIN deportivo.evaluaciones_diarias e
    ON e.id_evaluacion = ee.id_evaluacion
GROUP BY ee.id_evaluacion, ee.id_estudiante, ee.id_categoria_dia, e.fecha;


-- ------------------------------------------------------------
-- 4. Asistencia por codigo QR
--
-- El QR lo muestra la aplicacion en la pantalla de recepcion y lo
-- escanea el propio estudiante: el codigo no contiene ningun dato
-- personal, la identidad sale de la sesion autenticada de quien
-- escanea. El token que viaja dentro del QR rota cada pocos
-- segundos y vive en Redis con TTL, no en esta base: es efimero y
-- de un solo uso, y no tiene por que dejar rastro persistente.
-- Lo que si queda auditado aqui es COMO se marco cada asistencia.
-- ------------------------------------------------------------
ALTER TABLE deportivo.asistencias
    DROP CONSTRAINT IF EXISTS asistencias_metodo_check;

ALTER TABLE deportivo.asistencias
    ADD CONSTRAINT asistencias_metodo_check
        CHECK (metodo IN ('QR', 'RFID', 'MANUAL'));


-- ------------------------------------------------------------
-- 5. Normalizacion de la categoria en horarios y sesiones
--
-- Ambas guardaban la categoria como VARCHAR(25) de texto libre,
-- que es exactamente la denormalizacion que ya se corrigio en
-- academico.estudiantes al crear el catalogo deportivo.categorias.
-- Sin esto, la categoria de una sesion no puede compararse de
-- forma fiable con la de un estudiante.
-- ------------------------------------------------------------
ALTER TABLE deportivo.horarios_entrenamiento
    ADD COLUMN IF NOT EXISTS id_categoria BIGINT
        REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE deportivo.horarios_entrenamiento
    ALTER COLUMN id_categoria SET NOT NULL;

ALTER TABLE deportivo.horarios_entrenamiento
    DROP COLUMN IF EXISTS categoria;

ALTER TABLE deportivo.sesiones_entrenamiento
    ADD COLUMN IF NOT EXISTS id_categoria BIGINT
        REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE deportivo.sesiones_entrenamiento
    ALTER COLUMN id_categoria SET NOT NULL;

ALTER TABLE deportivo.sesiones_entrenamiento
    DROP COLUMN IF EXISTS categoria;

CREATE INDEX IF NOT EXISTS idx_sesiones_categoria_fecha
    ON deportivo.sesiones_entrenamiento(id_categoria, fecha);

-- ============================================================
-- V8: Quinto criterio de evaluacion - Velocidad
-- ============================================================
INSERT INTO deportivo.criterios_evaluacion (nombre, descripcion, puntaje_maximo) VALUES
    ('Velocidad', 'Explosividad y rapidez en el terreno de juego', 10)
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- V9: Representante y Recepcionista
--
-- representante_estudiante es una entidad de vinculo de primera
-- clase (con su propio "activo"), no una tabla puente plana: un
-- administrador debe poder cortar el acceso de un tutor puntual
-- sin tocar su cuenta ni sus otros hijos.
--
-- consentimientos resuelve el hallazgo H-04 de docs/etica/ETHICS.md.
-- Deliberado: NO gatea la lectura de informes (eso lo autoriza solo
-- el vinculo activo de arriba); queda reservada para cuando exista
-- el envio real de notificaciones (RF-22). Ver la nota de resolucion
-- bajo H-04 en ETHICS.md.
--
-- RECEPCIONISTA (rol sembrado en db/seed.sql) no necesita tabla
-- propia: solo le falta permiso sobre AsistenciaQrController.
-- ============================================================

CREATE TABLE IF NOT EXISTS academico.representantes (
    id_representante BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL UNIQUE REFERENCES seguridad.personas(id_persona),
    id_usuario BIGINT NOT NULL UNIQUE REFERENCES seguridad.usuarios(id_usuario),
    parentesco VARCHAR(30),
    telefono_contacto VARCHAR(20),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_representantes_updated_at
BEFORE UPDATE ON academico.representantes
FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();

CREATE TABLE IF NOT EXISTS academico.representante_estudiante (
    id_representante_estudiante BIGSERIAL PRIMARY KEY,
    id_representante BIGINT NOT NULL REFERENCES academico.representantes(id_representante),
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id_representante, id_estudiante)
);

CREATE TRIGGER trg_representante_estudiante_updated_at
BEFORE UPDATE ON academico.representante_estudiante
FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();

CREATE INDEX IF NOT EXISTS idx_representante_estudiante_estudiante
    ON academico.representante_estudiante(id_estudiante);

CREATE TABLE IF NOT EXISTS academico.consentimientos (
    id_consentimiento BIGSERIAL PRIMARY KEY,
    id_representante BIGINT NOT NULL REFERENCES academico.representantes(id_representante),
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    alcance VARCHAR(50) NOT NULL,
    otorgado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    registrado_por_id_usuario BIGINT REFERENCES seguridad.usuarios(id_usuario),
    revocado_en TIMESTAMPTZ,
    revocado_por_id_usuario BIGINT REFERENCES seguridad.usuarios(id_usuario)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_consentimiento_vigente
    ON academico.consentimientos(id_representante, id_estudiante, alcance)
    WHERE revocado_en IS NULL;

CREATE INDEX IF NOT EXISTS idx_consentimientos_estudiante
    ON academico.consentimientos(id_estudiante);

-- ============================================================
-- V10: Acceso de estudiante
--
-- Cierra el ciclo del QR de asistencia: RECEPCIONISTA ya podia emitir un
-- token real, pero nadie podia canjearlo porque un Estudiante no tenia
-- forma de autenticarse. id_usuario es NULLABLE y UNIQUE: la mayoria de
-- estudiantes no necesita login, solo los que un administrador habilita
-- explicitamente (POST /api/estudiantes/{id}/acceso, sobre la Persona ya
-- existente del estudiante, sin duplicarla).
-- ============================================================

ALTER TABLE academico.estudiantes
    ADD COLUMN IF NOT EXISTS id_usuario BIGINT UNIQUE REFERENCES seguridad.usuarios(id_usuario);

-- ============================================================
-- V11: cuatro procedimientos almacenados nuevos para cerrar el minimo
-- de 6 exigido por la Guia de la Entrega Final (Bloque A.2.1), uno por
-- cada categoria funcional que aun faltaba: consulta multi-tabla,
-- reporte, validacion cruzada y generacion de codigo secuencial. Fuente
-- y proposito de cada uno en db/procs/<nombre>.sql.
-- ============================================================

CREATE OR REPLACE PROCEDURE deportivo.sp_validar_categoria_estudiante_sesion(
    IN p_estudiante BIGINT,
    IN p_sesion BIGINT,
    OUT coincide BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_categoria_estudiante BIGINT;
    v_categoria_sesion BIGINT;
BEGIN
    SELECT id_categoria INTO v_categoria_estudiante
      FROM academico.estudiantes
     WHERE id_estudiante = p_estudiante;

    SELECT id_categoria INTO v_categoria_sesion
      FROM deportivo.sesiones_entrenamiento
     WHERE id_sesion = p_sesion;

    coincide := (v_categoria_estudiante IS NOT NULL)
        AND (v_categoria_sesion IS NOT NULL)
        AND (v_categoria_estudiante = v_categoria_sesion);
END;
$$;

CREATE OR REPLACE PROCEDURE academico.sp_generar_codigo_estudiante(
    IN p_anio INT,
    OUT codigo VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_prefijo VARCHAR(10) := 'EST-' || p_anio::TEXT || '-';
    v_siguiente INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) AS INT)), 0) + 1
      INTO v_siguiente
      FROM academico.estudiantes
     WHERE codigo_estudiante LIKE v_prefijo || '%'
       AND SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) ~ '^[0-9]+$';

    codigo := v_prefijo || LPAD(v_siguiente::TEXT, 4, '0');
END;
$$;

CREATE OR REPLACE PROCEDURE deportivo.sp_reporte_asistencia_estudiante(
    IN p_estudiante BIGINT,
    IN p_desde DATE,
    IN p_hasta DATE,
    OUT porcentaje_asistencia NUMERIC
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_categoria BIGINT;
    v_total_sesiones INT;
    v_total_presentes INT;
BEGIN
    SELECT id_categoria INTO v_categoria
      FROM academico.estudiantes
     WHERE id_estudiante = p_estudiante;

    SELECT COUNT(*) INTO v_total_sesiones
      FROM deportivo.sesiones_entrenamiento se
     WHERE se.id_categoria = v_categoria
       AND se.fecha BETWEEN p_desde AND p_hasta;

    SELECT COUNT(*) INTO v_total_presentes
      FROM deportivo.asistencias a
      JOIN deportivo.sesiones_entrenamiento se ON se.id_sesion = a.id_sesion
     WHERE a.id_estudiante = p_estudiante
       AND a.estado IN ('PRESENTE', 'TARDE')
       AND se.fecha BETWEEN p_desde AND p_hasta;

    IF v_total_sesiones = 0 THEN
        porcentaje_asistencia := NULL;
    ELSE
        porcentaje_asistencia := ROUND((v_total_presentes::NUMERIC / v_total_sesiones) * 100, 2);
    END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE academico.sp_contacto_representante_estudiante(
    IN p_estudiante BIGINT,
    OUT contacto_info VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre VARCHAR;
    v_apellido VARCHAR;
    v_telefono VARCHAR;
BEGIN
    SELECT p.nombre, p.apellido, COALESCE(r.telefono_contacto, p.telefono)
      INTO v_nombre, v_apellido, v_telefono
      FROM academico.representante_estudiante re
      JOIN academico.representantes r ON r.id_representante = re.id_representante
      JOIN seguridad.personas p ON p.id_persona = r.id_persona
     WHERE re.id_estudiante = p_estudiante
       AND re.activo = TRUE
       AND r.activo = TRUE
     ORDER BY re.created_at ASC
     LIMIT 1;

    IF v_nombre IS NULL THEN
        contacto_info := NULL;
    ELSE
        contacto_info := v_nombre || ' ' || v_apellido || ' - ' || COALESCE(v_telefono, 'sin telefono');
    END IF;
END;
$$;

-- ============================================================
-- V12: fusion con feature/moduloacademico (Darwin, PR #10 a main).
--
-- Ese trabajo construyo, en paralelo y sin saberlo (su propia
-- migracion lo declara: "esas tablas nunca tuvieron codigo Java"),
-- un segundo sistema de representante/asistencia/sesion sobre
-- tablas nuevas (academico.representantes con "ocupacion" en vez
-- de login, academico.estudiante_representante, academico.asistencia,
-- deportivo.sesion_entrenamiento, deportivo.entrenador_categoria,
-- deportivo.estado_asistencia). Se resuelve asi:
--   - representante/asistencia/sesion de ESTE archivo (V7/V9/V10)
--     se mantienen como el sistema unico: tienen login+consentimiento
--     para representante y marcado QR con tolerancia para asistencia,
--     capacidades que la version paralela no tenia.
--   - Las tablas nuevas de la version paralela NO se crean aqui: el
--     codigo Java correspondiente se retira en el mismo commit.
--   - Se rescatan las dos mejoras aditivas que si valen la pena:
--     las validaciones de datos sobre estudiantes, y los campos
--     relacion/contacto_principal del vinculo representante-estudiante
--     (utiles para cuando un estudiante tiene mas de un tutor).
-- ============================================================

ALTER TABLE academico.estudiantes
    ADD CONSTRAINT estudiantes_id_persona_key UNIQUE (id_persona);

ALTER TABLE academico.estudiantes
    ADD CONSTRAINT estudiantes_peso_check CHECK (peso > 0),
    ADD CONSTRAINT estudiantes_altura_check CHECK (altura > 0);

ALTER TABLE academico.representante_estudiante
    ADD COLUMN IF NOT EXISTS relacion VARCHAR(50),
    ADD COLUMN IF NOT EXISTS contacto_principal BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================================
-- V13: Pagos (membresia mensual y diario/eventual)
--
-- Dos tipos con reglas distintas: MEMBRESIA cubre un mes calendario
-- exacto (anio + mes obligatorios; el indice unico parcial impide
-- cobrar el mismo mes dos veces para el mismo estudiante). DIARIO es
-- un pago puntual de un solo dia (anio/mes NULL).
-- ============================================================

CREATE TABLE IF NOT EXISTS academico.pagos (
    id_pago BIGSERIAL PRIMARY KEY,
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('MEMBRESIA', 'DIARIO')),
    anio SMALLINT,
    mes SMALLINT CHECK (mes BETWEEN 1 AND 12),
    monto NUMERIC(8,2) NOT NULL CHECK (monto > 0),
    fecha_pago DATE NOT NULL,
    registrado_por_id_usuario BIGINT NOT NULL REFERENCES seguridad.usuarios(id_usuario),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_pago_periodo_segun_tipo CHECK (
        (tipo = 'MEMBRESIA' AND anio IS NOT NULL AND mes IS NOT NULL)
        OR (tipo = 'DIARIO' AND anio IS NULL AND mes IS NULL)
    )
);

CREATE TRIGGER trg_pagos_updated_at
BEFORE UPDATE ON academico.pagos
FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS idx_pago_membresia_unico
    ON academico.pagos(id_estudiante, anio, mes)
    WHERE tipo = 'MEMBRESIA';

CREATE INDEX IF NOT EXISTS idx_pagos_estudiante
    ON academico.pagos(id_estudiante, fecha_pago DESC);

-- ============================================================
-- V14: Notificaciones al representante (RF-22)
--
-- Notificacion en-app (no correo/SMS: no hay infraestructura de envio
-- externo en este proyecto). Una fila por cada representante con vinculo
-- ACTIVO al estudiante, cuando marca asistencia o se le registra una
-- lesion.
-- ============================================================

CREATE TABLE IF NOT EXISTS academico.notificaciones (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_representante BIGINT NOT NULL REFERENCES academico.representantes(id_representante),
    id_estudiante BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ASISTENCIA', 'LESION')),
    mensaje TEXT NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notificaciones_representante
    ON academico.notificaciones(id_representante, created_at DESC);
