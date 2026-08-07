-- sp_generar_codigo_estudiante
-- Propósito: generación de código secuencial -antes codigo_estudiante se
--            escribía a mano en el alta (EstudianteRequest.codigoEstudiante,
--            sin más regla que la unicidad); esto propone el siguiente
--            consecutivo del año para que el admin no tenga que inventarlo.
-- Entrada:  p_anio INT
-- Salida:   codigo VARCHAR (parametro OUT), formato 'EST-<anio>-<0000>'
-- Tablas:   academico.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): ver nota en
-- sp_contar_estudiantes_activos.sql sobre por que hace falta.
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
    -- El filtro ~ '^[0-9]+$' evita que un codigo_estudiante histórico que no
    -- siga este formato (cargado a mano antes de existir esta convención)
    -- rompa el CAST a INT.
    SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) AS INT)), 0) + 1
      INTO v_siguiente
      FROM academico.estudiantes
     WHERE codigo_estudiante LIKE v_prefijo || '%'
       AND SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) ~ '^[0-9]+$';

    codigo := v_prefijo || LPAD(v_siguiente::TEXT, 4, '0');
END;
$$;
