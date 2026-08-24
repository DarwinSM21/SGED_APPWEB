-- V21: guardar la alineacion que el entrenador puso realmente en la cancha.
--
-- Hasta ahora la formacion era solo una sugerencia recalculada: el entrenador
-- podia arrastrar jugadores en la pantalla, pero ese cambio vivia solo en el
-- navegador y se perdia al refrescar. Eso deja fuera lo que de verdad ocurre
-- en un entrenamiento: el entrenador mira la sugerencia, decide otra cosa
-- -mete al suplente que viene entrenando mejor, cambia a alguien de banda- y
-- juega con ESA. Sin guardarla no hay forma de saber despues que once se uso
-- ni de evaluar si funciono.
--
-- La sugerencia NO desaparece. Sigue calculandose igual y es lo que se ofrece
-- mientras nadie haya guardado nada. La tabla solo registra la decision del
-- entrenador cuando existe: sugerencia y decision son cosas distintas y se
-- guardan por separado.

CREATE TABLE IF NOT EXISTS deportivo.alineaciones (
    id_alineacion   BIGSERIAL PRIMARY KEY,
    id_sesion       BIGINT NOT NULL UNIQUE
                    REFERENCES deportivo.sesiones_entrenamiento(id_sesion) ON DELETE CASCADE,
    -- Como le fue al equipo con esta alineacion. Nullable porque el entrenador
    -- la guarda antes de jugar y la califica despues, si es que la califica.
    valoracion      SMALLINT CHECK (valoracion BETWEEN 1 AND 5),
    observacion     VARCHAR(500),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- UNIQUE sobre id_sesion: una sesion tiene una sola alineacion puesta en
-- cancha. Guardar de nuevo la reemplaza, no acumula versiones; lo que importa
-- historicamente es con que once se jugo, no cuantas veces la retoco antes.

CREATE TABLE IF NOT EXISTS deportivo.alineacion_jugador (
    id_alineacion_jugador BIGSERIAL PRIMARY KEY,
    id_alineacion   BIGINT NOT NULL
                    REFERENCES deportivo.alineaciones(id_alineacion) ON DELETE CASCADE,
    id_estudiante   BIGINT NOT NULL REFERENCES academico.estudiantes(id_estudiante),
    -- El puesto en el que el entrenador lo puso ESE dia, que no tiene por que
    -- ser su posicion nominal: de eso se trata poder hacer cambios.
    id_posicion     BIGINT REFERENCES deportivo.posiciones(id_posicion),
    titular         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_alineacion_jugador UNIQUE (id_alineacion, id_estudiante)
);

-- Un mismo estudiante no puede estar dos veces en la misma alineacion, y dos
-- titulares no pueden ocupar el mismo puesto.
CREATE UNIQUE INDEX IF NOT EXISTS idx_alineacion_puesto_unico
    ON deportivo.alineacion_jugador(id_alineacion, id_posicion)
    WHERE titular AND id_posicion IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_alineacion_jugador_estudiante
    ON deportivo.alineacion_jugador(id_estudiante);
