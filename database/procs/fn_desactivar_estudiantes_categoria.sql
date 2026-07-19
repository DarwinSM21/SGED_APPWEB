-- fn_desactivar_estudiantes_categoria
-- Propósito: baja lógica masiva de todos los estudiantes activos de una
--            categoría (actualización masiva con criterio de negocio,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria VARCHAR
-- Salida:   INTEGER (número de filas afectadas)
-- Tablas:   seguridad.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE FUNCTION seguridad.fn_desactivar_estudiantes_categoria(
    p_categoria VARCHAR
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectados INTEGER;
BEGIN
    UPDATE seguridad.estudiantes e
       SET activo = FALSE
     WHERE e.activo = TRUE
       AND e.categoria = p_categoria;

    GET DIAGNOSTICS v_afectados = ROW_COUNT;
    RETURN v_afectados;
END;
$$;
