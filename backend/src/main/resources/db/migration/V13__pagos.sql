-- ============================================================
-- V13: Pagos (membresia mensual y diario/eventual)
--
-- Dos tipos con reglas distintas:
--   MEMBRESIA cubre un mes calendario exacto (anio + mes obligatorios).
--   El indice unico parcial de abajo impide cobrar el mismo mes dos
--   veces para el mismo estudiante.
--   DIARIO es un pago puntual de un solo dia (anio/mes NULL): no
--   registra periodo de cobertura, solo que se pago en fecha_pago.
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
