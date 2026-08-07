-- sp_reporte_asistencia_estudiante
-- Propósito: reporte -porcentaje de asistencia de un estudiante en un
--            rango de fechas. El denominador es el total de sesiones
--            programadas para SU categoría en ese rango (no solo las filas
--            de asistencia que existan), para que una inasistencia real
--            -que hoy no siempre deja fila propia- no quede invisible.
-- Entrada:  p_estudiante BIGINT, p_desde DATE, p_hasta DATE
-- Salida:   porcentaje_asistencia NUMERIC (parametro OUT, NULL si no hubo
--           sesiones programadas en el rango)
-- Tablas:   academico.estudiantes, deportivo.sesiones_entrenamiento,
--           deportivo.asistencias
-- Sin SQL dinámico. Parámetros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): ver nota en
-- sp_contar_estudiantes_activos.sql sobre por que hace falta.
CREATE OR REPLACE PROCEDURE deportivo.sp_reporte_asistencia_estudiante(
    IN p_estudiante BIGINT,
    IN p_desde DATE,
    IN p_hasta DATE,
    OUT porcentaje_asistencia NUMERIC
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_categoria BIGINT;
    v_total_sesiones INT;
    v_total_presentes INT;
BEGIN
    SELECT id_categoria INTO v_categoria
      FROM academico.estudiantes
     WHERE id_estudiante = p_estudiante;

    SELECT COUNT(*) INTO v_total_sesiones
      FROM deportivo.sesiones_entrenamiento se
     WHERE se.id_categoria = v_categoria
       AND se.fecha BETWEEN p_desde AND p_hasta;

    SELECT COUNT(*) INTO v_total_presentes
      FROM deportivo.asistencias a
      JOIN deportivo.sesiones_entrenamiento se ON se.id_sesion = a.id_sesion
     WHERE a.id_estudiante = p_estudiante
       AND a.estado IN ('PRESENTE', 'TARDE')
       AND se.fecha BETWEEN p_desde AND p_hasta;

    IF v_total_sesiones = 0 THEN
        porcentaje_asistencia := NULL;
    ELSE
        porcentaje_asistencia := ROUND((v_total_presentes::NUMERIC / v_total_sesiones) * 100, 2);
    END IF;
END;
$$;
