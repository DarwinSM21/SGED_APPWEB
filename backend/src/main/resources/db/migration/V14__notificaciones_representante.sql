-- ============================================================
-- V14: Notificaciones al representante (RF-22)
--
-- Notificacion en-app (no correo/SMS: no hay infraestructura de envio
-- externo en este proyecto, y agregar una requeriria credenciales que
-- nadie tiene todavia). Se crea una fila por cada representante con
-- vinculo ACTIVO al estudiante, cuando el estudiante marca asistencia
-- o se le registra una lesion.
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
