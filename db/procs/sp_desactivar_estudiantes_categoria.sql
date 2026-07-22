-- sp_desactivar_estudiantes_categoria
-- Propósito: baja lógica masiva de todos los estudiantes activos de una
--            categoría (actualización masiva con criterio de negocio,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria VARCHAR
-- Salida:   afectados INTEGER (parametro OUT, numero de filas afectadas)
-- Tablas:   seguridad.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): ver nota en
-- sp_contar_estudiantes_activos.sql sobre por que hace falta.
CREATE OR REPLACE PROCEDURE seguridad.sp_desactivar_estudiantes_categoria(
    IN p_categoria VARCHAR,
    OUT afectados INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE seguridad.estudiantes e
       SET activo = FALSE
     WHERE e.activo = TRUE
       AND e.categoria = p_categoria;

    GET DIAGNOSTICS afectados = ROW_COUNT;
END;
$$;
