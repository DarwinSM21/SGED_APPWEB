-- Reemplaza las funciones de V5 por procedimientos almacenados y unifica
-- en schema academico con parametro INT (id_categoria).
--
-- Motivo: Spring Data JPA (@Procedure) invoca esto desde Java via
-- CallableStatement con sintaxis {call ...}; PostgreSQL solo acepta CALL
-- contra procedimientos reales (CREATE PROCEDURE), no contra funciones
-- (CREATE FUNCTION) - falla con "... is not a procedure. Hint: To call a
-- function, use SELECT." aunque la funcion tenga un parametro OUT.

DROP FUNCTION IF EXISTS seguridad.fn_contar_estudiantes_activos(VARCHAR);
DROP FUNCTION IF EXISTS seguridad.fn_desactivar_estudiantes_categoria(VARCHAR);
DROP FUNCTION IF EXISTS academico.fn_contar_estudiantes_activos(VARCHAR);
DROP FUNCTION IF EXISTS academico.fn_desactivar_estudiantes_categoria(VARCHAR);

DROP PROCEDURE IF EXISTS seguridad.sp_contar_estudiantes_activos(VARCHAR);
DROP PROCEDURE IF EXISTS seguridad.sp_desactivar_estudiantes_categoria(VARCHAR);

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

CREATE OR REPLACE PROCEDURE academico.sp_desactivar_estudiantes_categoria(
    IN p_categoria INT,
    OUT afectados INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE academico.estudiantes e
       SET activo = FALSE
     WHERE e.activo = TRUE
       AND e.id_categoria = p_categoria;

    GET DIAGNOSTICS afectados = ROW_COUNT;
END;
$$;
