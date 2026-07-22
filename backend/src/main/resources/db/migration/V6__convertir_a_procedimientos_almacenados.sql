-- Reemplaza las funciones de V5 (fn_contar_estudiantes_activos,
-- fn_desactivar_estudiantes_categoria) por procedimientos almacenados
-- reales (sp_*), y elimina las funciones viejas.
--
-- Motivo: Spring Data JPA (@Procedure) invoca esto desde Java via
-- CallableStatement con sintaxis {call ...}; PostgreSQL solo acepta CALL
-- contra procedimientos reales (CREATE PROCEDURE), no contra funciones
-- (CREATE FUNCTION) - falla con "... is not a procedure. Hint: To call a
-- function, use SELECT." aunque la funcion tenga un parametro OUT.

DROP FUNCTION IF EXISTS seguridad.fn_contar_estudiantes_activos(VARCHAR);
DROP FUNCTION IF EXISTS seguridad.fn_desactivar_estudiantes_categoria(VARCHAR);

CREATE OR REPLACE PROCEDURE seguridad.sp_contar_estudiantes_activos(
    IN p_categoria VARCHAR,
    OUT total BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
      INTO total
      FROM seguridad.estudiantes e
     WHERE e.activo = TRUE
       AND e.categoria = p_categoria;
END;
$$;

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
