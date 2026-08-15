-- V18__auditoria.sql
-- Auditoria: registro de toda accion relevante del sistema (CRUD de
-- negocio, autenticacion, administracion de cuentas), consultable solo
-- por ADMINISTRADOR desde /api/admin/auditorias.

CREATE TABLE IF NOT EXISTS seguridad.auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_usuario BIGINT REFERENCES seguridad.usuarios(id_usuario) ON DELETE SET NULL,
    usuario_nombre VARCHAR(150) NOT NULL,
    rol VARCHAR(50),
    accion VARCHAR(30) NOT NULL,
    entidad VARCHAR(100),
    entidad_id BIGINT,
    descripcion TEXT NOT NULL,
    ip VARCHAR(45)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON seguridad.auditoria (fecha DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON seguridad.auditoria (id_usuario);
CREATE INDEX IF NOT EXISTS idx_auditoria_entidad ON seguridad.auditoria (entidad);
CREATE INDEX IF NOT EXISTS idx_auditoria_accion ON seguridad.auditoria (accion);
