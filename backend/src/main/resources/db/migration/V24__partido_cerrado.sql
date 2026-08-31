ALTER TABLE deportivo.partidos
    ADD COLUMN IF NOT EXISTS cerrado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cerrado_en TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cerrado_por_id_usuario BIGINT;

ALTER TABLE deportivo.partidos
    DROP CONSTRAINT IF EXISTS partidos_cerrado_por_id_usuario_fkey;

ALTER TABLE deportivo.partidos
    ADD CONSTRAINT partidos_cerrado_por_id_usuario_fkey
        FOREIGN KEY (cerrado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE deportivo.partidos
    DROP CONSTRAINT IF EXISTS chk_partido_cerrado_con_marcador;

ALTER TABLE deportivo.partidos
    ADD CONSTRAINT chk_partido_cerrado_con_marcador
        CHECK (NOT cerrado OR (goles_favor IS NOT NULL AND goles_contra IS NOT NULL));

ALTER TABLE deportivo.partidos
    DROP CONSTRAINT IF EXISTS chk_partido_cerrado_con_fecha;

ALTER TABLE deportivo.partidos
    ADD CONSTRAINT chk_partido_cerrado_con_fecha
        CHECK (cerrado = (cerrado_en IS NOT NULL));

UPDATE deportivo.partidos
   SET cerrado = TRUE,
       cerrado_en = COALESCE(actualizado_en, now())
 WHERE goles_favor IS NOT NULL
   AND goles_contra IS NOT NULL
   AND NOT cerrado;
