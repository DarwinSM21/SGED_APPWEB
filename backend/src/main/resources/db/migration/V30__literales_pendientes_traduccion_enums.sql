-- V30: dos objetos de base de datos que V29 no alcanzo a corregir al
-- traducir los vocabularios de negocio a ingles (Punto E3 de la rubrica).
--
-- V29 migro los DATOS y los CHECK constraints de asistencias/pagos/
-- asignaciones, pero dejo intactos dos objetos que tenian los valores
-- viejos en espanol grabados en su propia definicion (no en la tabla):
--
-- 1) sp_reporte_asistencia_estudiante (V19): el cuerpo compilado del
--    procedimiento seguia comparando "a.estado IN ('PRESENTE', 'TARDE')".
--    Como los datos ya se migraron a 'PRESENT'/'LATE', esa condicion no
--    volvia a encontrar coincidencias nunca -- el porcentaje de asistencia
--    quedaba en 0 para todos silenciosamente, sin que ningun CHECK ni test
--    de columna lo detectara.
--
-- 2) idx_pago_membresia_unico (V20): el indice unico parcial seguia
--    filtrando "WHERE tipo = 'MEMBRESIA'". Los pagos ya se migraron a
--    'MEMBERSHIP', asi que ese indice dejo de aplicar a cualquier fila
--    nueva -- la restriccion de un solo pago de membresia por estudiante/
--    anio/mes quedo sin efecto, sin error visible.

-- 1) Redefinir el procedimiento con los literales en ingles (mismo cuerpo,
--    solo cambian los dos literales de la condicion).
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

-- 2) Recrear el indice con el literal en ingles (misma definicion que V20,
--    solo cambia el valor filtrado).
DROP INDEX IF EXISTS academico.idx_pago_membresia_unico;
CREATE UNIQUE INDEX idx_pago_membresia_unico
    ON academico.pagos (id_estudiante, anio, mes)
    WHERE tipo = 'MEMBERSHIP' AND anulado_en IS NULL;
