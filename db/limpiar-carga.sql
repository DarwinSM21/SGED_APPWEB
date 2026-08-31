-- ============================================================
-- Deshace db/carga-volumen.sql y deja solo el plantel real.
--
-- Todo lo sintetico es identificable sin ambiguedad -codigo 'CARGA-%',
-- correo '@carga.test'-, asi que el borrado no depende de recordar cuando se
-- cargo ni de comparar fechas.
--
-- Se corre antes de mostrar el sistema: mil chicos en SUB-12 no existen en
-- ninguna academia, y esa pantalla hace dudar del dato entero.
-- ============================================================

\timing on
\set ON_ERROR_STOP on

BEGIN;

-- Una alineacion guardada puede tener jugadores sinteticos: si se agendo un
-- partido mientras la carga estaba puesta, el once salio de ese plantel. Hay
-- que soltarlos ANTES de borrar a los estudiantes, o la llave foranea lo
-- impide.
--
-- Se borra la alineacion ENTERA, no solo las filas sinteticas: un once al que
-- le faltan ocho jugadores no es una alineacion, es basura que despues nadie
-- entiende. El partido se conserva; volver a generar la plantilla es un clic.
DELETE FROM deportivo.alineaciones al
 WHERE EXISTS (
        SELECT 1 FROM deportivo.alineacion_jugador aj
          JOIN academico.estudiantes e ON e.id_estudiante = aj.id_estudiante
         WHERE aj.id_alineacion = al.id_alineacion
           AND e.codigo_estudiante LIKE 'CARGA-%');

DELETE FROM deportivo.asistencias a
 USING academico.estudiantes e
 WHERE e.id_estudiante = a.id_estudiante
   AND e.codigo_estudiante LIKE 'CARGA-%';

DELETE FROM academico.estudiantes WHERE codigo_estudiante LIKE 'CARGA-%';
DELETE FROM seguridad.personas    WHERE correo LIKE '%@carga.test';

-- Las sesiones que dejo la carga: sin horario que las originara, sin
-- evaluacion y ya sin ninguna asistencia. Las tres condiciones juntas, porque
-- una sola no alcanza -hay sesiones reales creadas a mano, sin horario, a las
-- que el entrenador nunca les paso lista, y esas deben quedarse: forman parte
-- del historial aunque esten vacias-.
--
-- Importan mas de lo que parece: son el DENOMINADOR del porcentaje de
-- asistencia. Con 3.780 sesiones fantasma, todo alumno real aparecia con ~1 %
-- de asistencia en alertas y reportes.
DELETE FROM deportivo.sesiones_entrenamiento s
 WHERE s.id_horario IS NULL
   AND NOT EXISTS (SELECT 1 FROM deportivo.evaluaciones_diarias e WHERE e.id_sesion = s.id_sesion)
   AND NOT EXISTS (SELECT 1 FROM deportivo.asistencias a WHERE a.id_sesion = s.id_sesion);

COMMIT;

ANALYZE seguridad.personas;
ANALYZE academico.estudiantes;
ANALYZE deportivo.sesiones_entrenamiento;
ANALYZE deportivo.asistencias;

SELECT 'personas    ' || to_char(COUNT(*), 'FM999G999G999') FROM seguridad.personas
UNION ALL SELECT 'estudiantes ' || to_char(COUNT(*), 'FM999G999G999') FROM academico.estudiantes
UNION ALL SELECT 'sesiones    ' || to_char(COUNT(*), 'FM999G999G999') FROM deportivo.sesiones_entrenamiento
UNION ALL SELECT 'asistencias ' || to_char(COUNT(*), 'FM999G999G999') FROM deportivo.asistencias;

SELECT 'rastro sintetico restante: ' ||
       (SELECT COUNT(*) FROM academico.estudiantes WHERE codigo_estudiante LIKE 'CARGA-%') ||
       ' estudiantes, ' ||
       (SELECT COUNT(*) FROM seguridad.personas WHERE correo LIKE '%@carga.test') || ' personas';
