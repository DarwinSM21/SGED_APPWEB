-- fn_contar_estudiantes_activos
-- Propósito: contar estudiantes activos de una categoría (agregado COUNT,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria VARCHAR
-- Salida:   BIGINT (total de estudiantes activos de esa categoría)
-- Tablas:   seguridad.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE FUNCTION seguridad.fn_contar_estudiantes_activos(
    p_categoria VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_total BIGINT;
BEGIN
    SELECT COUNT(*)
      INTO v_total
      FROM seguridad.estudiantes e
     WHERE e.activo = TRUE
       AND e.categoria = p_categoria;
    RETURN v_total;
END;
$$;
