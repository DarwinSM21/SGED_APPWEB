-- Flyway V27 — RF-50 / hallazgo H-03 de docs/etica/ETHICS.md
--
-- Procedimiento de anonimizacion del titular. Ante una solicitud de
-- supresion del representante legal, un ADMINISTRADOR ejecuta esta
-- operacion (POST /api/estudiantes/{id}/anonimizar, auditada): los datos
-- identificativos de la persona del estudiante se sustituyen por valores
-- neutros y el texto libre escrito sobre el menor se borra, pero las
-- claves foraneas y las estadisticas agregadas (asistencia, evaluaciones,
-- pagos) se conservan intactas. Cierra el hallazgo H-03 e implementa el
-- mecanismo de supresion que RNF-22 exige.
--
-- Es un PROCEDURE con parametro OUT (no FUNCTION): ver la nota en
-- sp_contar_estudiantes_activos.sql sobre por que Spring Data @Procedure
-- necesita CREATE PROCEDURE. Sin SQL dinamico, parametros nombrados
-- (RD-02 / RD-03).

CREATE OR REPLACE PROCEDURE academico.sp_anonimizar_estudiante(
    IN  p_id_estudiante BIGINT,
    OUT campos_afectados INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_persona BIGINT;
    v_id_usuario BIGINT;
    v_n INTEGER;
BEGIN
    campos_afectados := 0;

    SELECT id_persona, id_usuario
      INTO v_id_persona, v_id_usuario
      FROM academico.estudiantes
     WHERE id_estudiante = p_id_estudiante;

    IF v_id_persona IS NULL THEN
        RAISE EXCEPTION 'No existe un estudiante con id %', p_id_estudiante
            USING ERRCODE = 'no_data_found';
    END IF;

    -- 1. Datos identificativos de la persona -> valores neutros. El correo
    --    lleva NOT NULL UNIQUE y la fecha de nacimiento NOT NULL, asi que se
    --    sustituyen por un valor neutro unico, no por NULL.
    UPDATE seguridad.personas
       SET nombre           = 'ANONIMIZADO',
           apellido         = 'ESTUDIANTE #' || p_id_estudiante,
           cedula           = NULL,
           correo           = 'anon+' || v_id_persona || '@anonimizado.local',
           telefono         = NULL,
           foto             = NULL,
           fecha_nacimiento = DATE '1900-01-01',
           updated_at       = NOW()
     WHERE id_persona = v_id_persona;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    campos_afectados := campos_afectados + v_n;

    -- 2. Cuenta de acceso, si el estudiante la tiene: se anonimiza el
    --    username (indice unico) y se desactiva. El hash de contrasena no es
    --    dato identificativo y la fila se conserva por la FK de auditoria.
    IF v_id_usuario IS NOT NULL THEN
        UPDATE seguridad.usuarios
           SET username = 'anon_' || v_id_usuario,
               activo   = FALSE
         WHERE id_usuario = v_id_usuario;
        GET DIAGNOSTICS v_n = ROW_COUNT;
        campos_afectados := campos_afectados + v_n;
    END IF;

    -- 3. Texto libre escrito sobre el menor. Ambas columnas son NOT NULL:
    --    se reemplazan por un marcador, no se ponen a NULL ni se borra la
    --    fila (se conservan las FKs y el conteo de lesiones/observaciones).
    UPDATE deportivo.observaciones_estudiante
       SET texto = '[texto suprimido por anonimizacion]'
     WHERE id_estudiante = p_id_estudiante;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    campos_afectados := campos_afectados + v_n;

    UPDATE deportivo.lesiones
       SET descripcion = '[texto suprimido por anonimizacion]'
     WHERE id_estudiante = p_id_estudiante;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    campos_afectados := campos_afectados + v_n;

    -- 4. Baja logica de la ficha: un registro anonimizado no debe seguir
    --    apareciendo como estudiante activo. La fila y sus FKs permanecen.
    UPDATE academico.estudiantes
       SET activo = FALSE
     WHERE id_estudiante = p_id_estudiante
       AND activo = TRUE;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    campos_afectados := campos_afectados + v_n;
END;
$$;
