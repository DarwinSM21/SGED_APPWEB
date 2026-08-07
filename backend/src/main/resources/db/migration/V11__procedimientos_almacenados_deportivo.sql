-- ============================================================
-- V11: cuatro procedimientos almacenados nuevos para cerrar el minimo
-- de 6 exigido por la Guia de la Entrega Final (Bloque A.2.1), uno por
-- cada categoria funcional que aun faltaba: consulta multi-tabla,
-- reporte, validacion cruzada y generacion de codigo secuencial.
-- (Los dos ya existentes -sp_contar_estudiantes_activos, agregado; y
-- sp_desactivar_estudiantes_categoria, actualizacion masiva- vienen de
-- V5/V6.)
--
-- Fuente de cada uno en db/procs/<nombre>.sql, con el proposito de cada
-- cual documentado ahi.
-- ============================================================

CREATE OR REPLACE PROCEDURE deportivo.sp_validar_categoria_estudiante_sesion(
    IN p_estudiante BIGINT,
    IN p_sesion BIGINT,
    OUT coincide BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_categoria_estudiante BIGINT;
    v_categoria_sesion BIGINT;
BEGIN
    SELECT id_categoria INTO v_categoria_estudiante
      FROM academico.estudiantes
     WHERE id_estudiante = p_estudiante;

    SELECT id_categoria INTO v_categoria_sesion
      FROM deportivo.sesiones_entrenamiento
     WHERE id_sesion = p_sesion;

    coincide := (v_categoria_estudiante IS NOT NULL)
        AND (v_categoria_sesion IS NOT NULL)
        AND (v_categoria_estudiante = v_categoria_sesion);
END;
$$;

CREATE OR REPLACE PROCEDURE academico.sp_generar_codigo_estudiante(
    IN p_anio INT,
    OUT codigo VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_prefijo VARCHAR(10) := 'EST-' || p_anio::TEXT || '-';
    v_siguiente INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) AS INT)), 0) + 1
      INTO v_siguiente
      FROM academico.estudiantes
     WHERE codigo_estudiante LIKE v_prefijo || '%'
       AND SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) ~ '^[0-9]+$';

    codigo := v_prefijo || LPAD(v_siguiente::TEXT, 4, '0');
END;
$$;

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

CREATE OR REPLACE PROCEDURE academico.sp_contacto_representante_estudiante(
    IN p_estudiante BIGINT,
    OUT contacto_info VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre VARCHAR;
    v_apellido VARCHAR;
    v_telefono VARCHAR;
BEGIN
    SELECT p.nombre, p.apellido, COALESCE(r.telefono_contacto, p.telefono)
      INTO v_nombre, v_apellido, v_telefono
      FROM academico.representante_estudiante re
      JOIN academico.representantes r ON r.id_representante = re.id_representante
      JOIN seguridad.personas p ON p.id_persona = r.id_persona
     WHERE re.id_estudiante = p_estudiante
       AND re.activo = TRUE
       AND r.activo = TRUE
     ORDER BY re.created_at ASC
     LIMIT 1;

    IF v_nombre IS NULL THEN
        contacto_info := NULL;
    ELSE
        contacto_info := v_nombre || ' ' || v_apellido || ' - ' || COALESCE(v_telefono, 'sin telefono');
    END IF;
END;
$$;
