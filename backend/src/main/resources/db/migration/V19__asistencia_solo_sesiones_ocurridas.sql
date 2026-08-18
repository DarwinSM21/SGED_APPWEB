-- V19: el porcentaje de asistencia deja de contar sesiones que no ocurrieron.
--
-- Con la generacion automatica de sesiones (7 dias hacia adelante) la sesion
-- del dia aparece en la base a primera hora, sin asistencias todavia. Como el
-- denominador del reporte tomaba el rango completo hasta hoy, cada manana el
-- porcentaje de TODOS los estudiantes caia -medido en la demo: de 100% a 50%
-- con solo dos sesiones en ventana- y se recuperaba recien cuando el
-- entrenador pasaba lista. El panel de alertas terminaba marcando a los 25
-- estudiantes por asistencia baja, que es lo mismo que no marcar a ninguno.
--
-- El recorte va dentro del procedimiento y no en los servicios porque los
-- tres llamadores (alertas, informe al representante y ficha de asistencia)
-- pasan hasta = hoy y todos arrastraban el mismo error.
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
       AND a.estado IN ('PRESENTE', 'TARDE')
       AND se.fecha BETWEEN p_desde AND v_corte;

    IF v_total_sesiones = 0 THEN
        porcentaje_asistencia := NULL;
    ELSE
        porcentaje_asistencia := ROUND((v_total_presentes::NUMERIC / v_total_sesiones) * 100, 2);
    END IF;
END;
$$;
