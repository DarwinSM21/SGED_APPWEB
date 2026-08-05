-- ============================================================
-- V9: Representante y Recepcionista
--
-- Dos piezas nuevas para el modulo deportivo:
--
-- 1. Representante: padre/madre/tutor legal, con su propia cuenta
--    de acceso (mismo patron que Entrenador: persona + usuario 1-a-1).
--    Puede tener uno o mas representados. representante_estudiante es
--    una entidad de vinculo de primera clase, no una tabla puente
--    plana, porque necesita su propio "activo" para poder cortar el
--    acceso de un tutor puntual (p.ej. disputa de custodia) sin tocar
--    su cuenta ni sus otros hijos.
--
-- 2. Consentimientos: registra que un representante autorizo el
--    tratamiento de los datos de un representado, con fecha, alcance
--    y quien lo registro. Resuelve el hallazgo H-04 de
--    docs/etica/ETHICS.md, que bloqueaba RF-22 (notificaciones al
--    representante) hasta que existiera este registro.
--
--    Deliberado: esta tabla NO gatea la lectura de informes (eso lo
--    autoriza unicamente el vinculo activo representante_estudiante,
--    que crea un administrador). Queda reservada para cuando se
--    construya el envio real de notificaciones (RF-22 propiamente
--    dicho), que todavia no existe. Ver la nota de resolucion bajo
--    H-04 en ETHICS.md. alcance es VARCHAR libre a proposito: si
--    algun dia se decide que la lectura tambien debe requerir
--    consentimiento, agregar ese caso es un dato nuevo, no una
--    migracion.
--
-- El rol RECEPCIONISTA (sembrado en db/seed.sql, junto a los demas
-- roles del proyecto) no necesita tabla propia: su unico trabajo
-- -emitir el QR de asistencia- ya lo cubre AsistenciaQrController,
-- solo le falta el permiso.
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

-- Solo un consentimiento vigente por (representante, estudiante, alcance)
-- a la vez; revocar y volver a otorgar deja historial en vez de pisarlo,
-- mismo patron que idx_lesion_activa_por_estudiante.
CREATE UNIQUE INDEX IF NOT EXISTS idx_consentimiento_vigente
    ON academico.consentimientos(id_representante, id_estudiante, alcance)
    WHERE revocado_en IS NULL;

CREATE INDEX IF NOT EXISTS idx_consentimientos_estudiante
    ON academico.consentimientos(id_estudiante);
