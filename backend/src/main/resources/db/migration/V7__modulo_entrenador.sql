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
