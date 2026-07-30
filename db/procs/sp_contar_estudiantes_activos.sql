-- sp_contar_estudiantes_activos
-- Propósito: contar estudiantes activos de una categoría (agregado COUNT,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria VARCHAR
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
    IN p_categoria VARCHAR,
    OUT total BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
      INTO total
      FROM academico.estudiantes e
      JOIN deportivo.categorias c ON c.id_categoria = e.id_categoria
     WHERE e.activo = TRUE
       AND c.nombre = p_categoria;
END;
$$;
