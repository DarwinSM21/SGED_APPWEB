-- Traduce a ingles los valores almacenados de cuatro vocabularios de
-- negocio que quedaron en espanol cuando el resto de identificadores del
-- codigo ya se habia renombrado (Punto E3 de la rubrica de evaluacion):
--   pagos.tipo             MEMBRESIA/DIARIO       -> MEMBERSHIP/DAILY
--   asignaciones.tipo_destinatario  ESTUDIANTE/ENTRENADOR -> STUDENT/COACH
--   asignaciones.estado     ASIGNADO/DEVUELTO/PERDIDO -> ASSIGNED/RETURNED/LOST
--   asistencias.estado      PRESENTE/TARDE/AUSENTE/JUSTIFICADO
--                                    -> PRESENT/LATE/ABSENT/EXCUSED
-- No toca la tabla de roles ni ningun rol de Spring Security: ESTUDIANTE/
-- ENTRENADOR como nombre de ROL (usados en @PreAuthorize/hasRole) son un
-- concepto distinto que comparte la misma palabra por coincidencia del
-- espanol, y no se renombra aqui.

-- 1) pagos.tipo
ALTER TABLE academico.pagos DROP CONSTRAINT pagos_tipo_check;
ALTER TABLE academico.pagos DROP CONSTRAINT chk_pago_periodo_segun_tipo;

UPDATE academico.pagos SET tipo = 'MEMBERSHIP' WHERE tipo = 'MEMBRESIA';
UPDATE academico.pagos SET tipo = 'DAILY' WHERE tipo = 'DIARIO';

ALTER TABLE academico.pagos ADD CONSTRAINT pagos_tipo_check
    CHECK (tipo IN ('MEMBERSHIP', 'DAILY'));
ALTER TABLE academico.pagos ADD CONSTRAINT chk_pago_periodo_segun_tipo
    CHECK (
        (tipo = 'MEMBERSHIP' AND anio IS NOT NULL AND mes IS NOT NULL)
        OR (tipo = 'DAILY' AND anio IS NULL AND mes IS NULL)
    );

-- 2) asignaciones.tipo_destinatario y asignaciones.estado
ALTER TABLE inventario.asignaciones DROP CONSTRAINT asignaciones_tipo_destinatario_check;
ALTER TABLE inventario.asignaciones DROP CONSTRAINT chk_asignacion_destinatario;
ALTER TABLE inventario.asignaciones DROP CONSTRAINT asignaciones_estado_check;
ALTER TABLE inventario.asignaciones ALTER COLUMN estado DROP DEFAULT;

UPDATE inventario.asignaciones SET tipo_destinatario = 'STUDENT' WHERE tipo_destinatario = 'ESTUDIANTE';
UPDATE inventario.asignaciones SET tipo_destinatario = 'COACH' WHERE tipo_destinatario = 'ENTRENADOR';
UPDATE inventario.asignaciones SET estado = 'ASSIGNED' WHERE estado = 'ASIGNADO';
UPDATE inventario.asignaciones SET estado = 'RETURNED' WHERE estado = 'DEVUELTO';
UPDATE inventario.asignaciones SET estado = 'LOST' WHERE estado = 'PERDIDO';

ALTER TABLE inventario.asignaciones ADD CONSTRAINT asignaciones_tipo_destinatario_check
    CHECK (tipo_destinatario IN ('STUDENT', 'COACH'));
ALTER TABLE inventario.asignaciones ADD CONSTRAINT chk_asignacion_destinatario
    CHECK (
        (tipo_destinatario = 'STUDENT' AND id_estudiante IS NOT NULL AND id_entrenador IS NULL)
        OR (tipo_destinatario = 'COACH' AND id_entrenador IS NOT NULL AND id_estudiante IS NULL)
    );
ALTER TABLE inventario.asignaciones ADD CONSTRAINT asignaciones_estado_check
    CHECK (estado IN ('ASSIGNED', 'RETURNED', 'LOST'));
ALTER TABLE inventario.asignaciones ALTER COLUMN estado SET DEFAULT 'ASSIGNED';

-- 3) asistencias.estado
ALTER TABLE deportivo.asistencias DROP CONSTRAINT asistencias_estado_check;

UPDATE deportivo.asistencias SET estado = 'PRESENT' WHERE estado = 'PRESENTE';
UPDATE deportivo.asistencias SET estado = 'LATE' WHERE estado = 'TARDE';
UPDATE deportivo.asistencias SET estado = 'ABSENT' WHERE estado = 'AUSENTE';
UPDATE deportivo.asistencias SET estado = 'EXCUSED' WHERE estado = 'JUSTIFICADO';

ALTER TABLE deportivo.asistencias ADD CONSTRAINT asistencias_estado_check
    CHECK (estado IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED'));
