-- sp_contacto_representante_estudiante
-- Propósito: consulta multi-tabla -contacto de emergencia de un estudiante
--            (nombre y teléfono de su representante vinculado activo), para
--            que un entrenador pueda ubicar rápido a un tutor ante una
--            lesión sin navegar tres pantallas distintas.
-- Entrada:  p_estudiante BIGINT
-- Salida:   contacto_info VARCHAR (parametro OUT, NULL si no tiene
--           representante activo vinculado)
-- Tablas:   academico.representante_estudiante, academico.representantes,
--           seguridad.personas
-- Sin SQL dinámico. Parámetros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): ver nota en
-- sp_contar_estudiantes_activos.sql sobre por que hace falta.
--
-- Si un estudiante tiene mas de un representante activo (p.ej. padre y
-- madre), se toma el vinculo mas antiguo como principal: es una eleccion
-- arbitraria pero estable, documentada aqui para no sorprender a quien lea
-- el resultado.
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
