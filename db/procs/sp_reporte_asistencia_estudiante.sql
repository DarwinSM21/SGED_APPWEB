-- sp_reporte_asistencia_estudiante
-- Propósito: reporte -porcentaje de asistencia de un estudiante en un
--            rango de fechas. El denominador es el total de sesiones
--            programadas para SU categoría en ese rango (no solo las filas
--            de asistencia que existan), para que una inasistencia real
--            -que hoy no siempre deja fila propia- no quede invisible.
--            El rango se recorta a ayer: una sesión que todavía no ocurrió
--            no puede contar como falta.
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
    v_corte DATE;
    v_total_sesiones INT;
    v_total_presentes INT;
BEGIN
    -- Una sesion de hoy o posterior todavia no ocurrio: nadie pudo asistir.
    -- Si entrara al denominador, el porcentaje de todos caeria cada manana
    -- al generarse la sesion del dia y se recuperaria recien cuando el
    -- entrenador pasa lista, castigando al estudiante por una ausencia que
    -- aun no existe. Se mide hasta ayer. El recorte va aqui y no en cada
    -- servicio porque los tres llamadores pasan hasta = hoy.
    v_corte := LEAST(p_hasta, CURRENT_DATE - 1);

    SELECT id_categoria INTO v_categoria
      FROM academico.estudiantes
     WHERE id_estudiante = p_estudiante;

    SELECT COUNT(*) INTO v_total_sesiones
      FROM deportivo.sesiones_entrenamiento se
     WHERE se.id_categoria = v_categoria
       AND se.fecha BETWEEN p_desde AND v_corte;

    SELECT COUNT(*) INTO v_total_presentes
      FROM deportivo.asistencias a
      JOIN deportivo.sesiones_entrenamiento se ON se.id_sesion = a.id_sesion
     WHERE a.id_estudiante = p_estudiante
       AND a.estado IN ('PRESENT', 'LATE')
       AND se.fecha BETWEEN p_desde AND v_corte;

    IF v_total_sesiones = 0 THEN
        porcentaje_asistencia := NULL;
    ELSE
        porcentaje_asistencia := ROUND((v_total_presentes::NUMERIC / v_total_sesiones) * 100, 2);
    END IF;
END;
$$;
