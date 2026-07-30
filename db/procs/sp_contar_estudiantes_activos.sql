-- sp_contar_estudiantes_activos
-- Propósito: contar estudiantes activos de una categoría (agregado COUNT,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria INT (id_categoria)
-- Salida:   total BIGINT (parametro OUT)
-- Tablas:   academico.estudiantes
-- Sin SQL dinámico. Parámetros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): Spring Data JPA (@Procedure) invoca esto
-- desde Java via CallableStatement con sintaxis {call ...}, y PostgreSQL
-- solo acepta CALL contra procedimientos reales (CREATE PROCEDURE), no
-- contra funciones (CREATE FUNCTION), incluso si la funcion declara un
-- parametro OUT.
CREATE OR REPLACE PROCEDURE academico.sp_contar_estudiantes_activos(
    IN p_categoria INT,
    OUT total BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
      INTO total
      FROM academico.estudiantes e
     WHERE e.activo = TRUE
       AND e.id_categoria = p_categoria;
END;
$$;
