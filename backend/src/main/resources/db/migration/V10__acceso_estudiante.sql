-- ============================================================
-- V10: Acceso de estudiante (lado que faltaba del QR de asistencia)
--
-- El rol ESTUDIANTE (sembrado en db/seed.sql) y esta columna cierran el
-- ciclo: la pantalla de recepcion (RECEPCIONISTA) ya emitia un QR real,
-- pero nadie podia canjearlo porque un Estudiante no tenia forma de
-- autenticarse. id_usuario es NULLABLE y UNIQUE a proposito: la inmensa
-- mayoria de estudiantes existentes no necesita login propio, solo los
-- que un administrador habilita explicitamente (POST
-- /api/estudiantes/{id}/acceso, que reutiliza la Persona YA existente del
-- estudiante en vez de crear una duplicada -a diferencia del patron de
-- Representante, que si crea una Persona nueva porque el tutor no
-- existia antes en el sistema-).
-- ============================================================

ALTER TABLE academico.estudiantes
    ADD COLUMN IF NOT EXISTS id_usuario BIGINT UNIQUE REFERENCES seguridad.usuarios(id_usuario);
