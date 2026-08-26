-- ============================================================
-- V22: la formacion pasa de los entrenamientos a los partidos.
--
-- V21 colgo la alineacion de deportivo.sesiones_entrenamiento, que es donde se
-- construyo primero, pero una alineacion no es un hecho del entrenamiento: es
-- la decision de con quien se sale a jugar. Atarla a la sesion obligaba ademas
-- a que solo pudieran alinearse los que asistieron a ESE entrenamiento, cuando
-- lo que corresponde para un partido es mirar el rendimiento acumulado de las
-- semanas anteriores.
--
-- Un entrenamiento registra lo suyo -asistencia y evaluacion- y el partido
-- registra lo suyo -quien jugo y como fue-. Son dos hechos distintos.
--
-- deportivo.partidos NO se crea: ya existia. V16 la documento tal como estaba
-- fuera de control de versiones, vacia, sin entidad JPA y sin API. Aqui se le
-- da forma y uso en lugar de crear una segunda tabla que signifique lo mismo.
-- ============================================================

-- El partido pertenece a una categoria: la SUB-14 no juega el partido de la
-- SUB-17, y sin esto no habria de donde sacar los convocables.
ALTER TABLE deportivo.partidos
    ADD COLUMN IF NOT EXISTS id_categoria BIGINT REFERENCES deportivo.categorias(id_categoria);
ALTER TABLE deportivo.partidos ALTER COLUMN id_categoria SET NOT NULL;

ALTER TABLE deportivo.partidos
    ADD COLUMN IF NOT EXISTS hora TIME,
    ADD COLUMN IF NOT EXISTS observacion VARCHAR(500),
    ADD COLUMN IF NOT EXISTS creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Nombres alineados con el resto de deportivo (sesiones usa "fecha" a secas).
ALTER TABLE deportivo.partidos RENAME COLUMN fecha_partido TO fecha;

-- Solo se lleva el marcador propio. El sistema es de UNA academia: "local" y
-- "visitante" pedirian una tabla de rivales que nadie va a mantener, y lo que
-- se necesita saber es si se gano.
ALTER TABLE deportivo.partidos RENAME COLUMN goles_local TO goles_favor;
ALTER TABLE deportivo.partidos RENAME COLUMN goles_visitante TO goles_contra;

-- Se quita el DEFAULT 0: un partido recien agendado no va 0-0, todavia no se
-- juega. NULL es "sin resultado" y 0 es "no metio ninguno"; con DEFAULT 0 las
-- dos cosas se veian igual.
ALTER TABLE deportivo.partidos
    ALTER COLUMN goles_favor  DROP DEFAULT,
    ALTER COLUMN goles_contra DROP DEFAULT;

-- Columnas del diseno viejo que nunca tuvieron datos, ni FK, ni codigo.
ALTER TABLE deportivo.partidos
    DROP COLUMN IF EXISTS id_equipo_local,
    DROP COLUMN IF EXISTS id_equipo_visitante,
    DROP COLUMN IF EXISTS ubicacion;

ALTER TABLE deportivo.partidos
    DROP CONSTRAINT IF EXISTS chk_partido_goles_no_negativos,
    DROP CONSTRAINT IF EXISTS chk_partido_marcador_completo;

ALTER TABLE deportivo.partidos
    ADD CONSTRAINT chk_partido_goles_no_negativos
        CHECK (goles_favor >= 0 AND goles_contra >= 0),
    -- O estan los dos goles o no esta ninguno: un marcador a medias -"metimos
    -- 3" sin saber cuantos recibimos- no dice si se gano o se perdio, que es
    -- justamente para lo que se guarda.
    ADD CONSTRAINT chk_partido_marcador_completo
        CHECK ((goles_favor IS NULL) = (goles_contra IS NULL));

CREATE INDEX IF NOT EXISTS idx_partido_categoria_fecha
    ON deportivo.partidos(id_categoria, fecha DESC);

-- ------------------------------------------------------------
-- La alineacion pasa a colgar del partido.
--
-- Se recrea en vez de alterarse: las alineaciones que existian estaban atadas
-- a sesiones de entrenamiento y no corresponden a ningun partido, asi que
-- conservarlas seria inventar partidos que nunca se jugaron.

DROP TABLE IF EXISTS deportivo.alineacion_jugador;
DROP TABLE IF EXISTS deportivo.alineaciones;

CREATE TABLE deportivo.alineaciones (
    id_alineacion   BIGSERIAL PRIMARY KEY,
    id_partido      BIGINT NOT NULL UNIQUE
                    REFERENCES deportivo.partidos(id_partido) ON DELETE CASCADE,

    -- Como le fue al equipo con este once, de 1 a 5. Admite nulo porque la
    -- alineacion se arma antes de jugar y se califica despues.
    valoracion      SMALLINT CHECK (valoracion BETWEEN 1 AND 5),
    observacion     VARCHAR(500),

    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE deportivo.alineacion_jugador (
    id_alineacion_jugador BIGSERIAL PRIMARY KEY,
    id_alineacion   BIGINT NOT NULL
                    REFERENCES deportivo.alineaciones(id_alineacion) ON DELETE CASCADE,
    id_estudiante   BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),

    -- El puesto que ocupo ESE dia, que no tiene por que ser su posicion
    -- nominal: de eso se trata poder mover gente.
    id_posicion     BIGINT REFERENCES deportivo.posiciones(id_posicion),
    titular         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_alineacion_jugador UNIQUE (id_alineacion, id_estudiante)
);

-- Dos titulares no pueden ocupar el mismo puesto. En el banco si se repite:
-- puede haber tres suplentes delanteros.
CREATE UNIQUE INDEX idx_alineacion_puesto_unico
    ON deportivo.alineacion_jugador(id_alineacion, id_posicion)
    WHERE titular AND id_posicion IS NOT NULL;

CREATE INDEX idx_alineacion_jugador_estudiante
    ON deportivo.alineacion_jugador(id_estudiante);
