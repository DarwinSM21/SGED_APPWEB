-- Flyway V25 — RNF-25 / hallazgo H-02 de docs/etica/ETHICS.md
--
-- Tope de longitud, a nivel de motor, para el texto libre que se escribe
-- sobre un estudiante menor de edad. Es defensa en profundidad: la capa de
-- aplicacion ya valida (@Size en la descripcion de lesion, guarda de longitud
-- en EvaluacionDiariaService.finalizar), pero deportivo.observaciones_estudiante
-- no tiene aun codigo JPA y esta restriccion la protege igual.
--
-- Limites: 2000 caracteres para observaciones/evaluacion, 1000 para la
-- descripcion de lesion (coincide con el @Size del DTO RegistrarLesionRequest).

ALTER TABLE deportivo.evaluaciones_diarias
    ADD CONSTRAINT ck_evaluacion_observacion_longitud
    CHECK (observacion_general IS NULL OR char_length(observacion_general) <= 2000);

ALTER TABLE deportivo.observaciones_estudiante
    ADD CONSTRAINT ck_observacion_estudiante_longitud
    CHECK (char_length(texto) <= 2000);

ALTER TABLE deportivo.lesiones
    ADD CONSTRAINT ck_lesion_descripcion_longitud
    CHECK (char_length(descripcion) <= 1000);
