-- sp_anonimizar_estudiante
-- Proposito: supresion / anonimizacion de los datos del titular (RF-50,
--            hallazgo H-03 de docs/etica/ETHICS.md). Ante una solicitud del
--            representante legal, sustituye los datos identificativos de la
--            persona del estudiante por valores neutros y borra el texto
--            libre escrito sobre el menor, conservando las claves foraneas y
--            las estadisticas agregadas (asistencia, evaluaciones, pagos).
--            Implementa el mecanismo de supresion que RNF-22 exige.
-- Entrada:  p_id_estudiante BIGINT
-- Salida:   campos_afectados INTEGER (parametro OUT, numero de filas tocadas:
--           persona + cuenta de acceso + observaciones + lesiones + ficha)
-- Tablas:   academico.estudiantes, seguridad.personas, seguridad.usuarios,
--           deportivo.observaciones_estudiante, deportivo.lesiones
-- Efecto:   nombre='ANONIMIZADO', apellido='ESTUDIANTE #<id>', cedula=NULL,
--           correo='anon+<id_persona>@anonimizado.local', telefono=NULL,
--           foto=NULL, fecha_nacimiento=1900-01-01; username='anon_<id>' y
--           usuario desactivado; texto libre -> marcador; ficha a baja logica.
--           Lanza EXCEPTION (ERRCODE no_data_found) si el estudiante no existe.
-- Sin SQL dinamico. Parametros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): ver la nota en
-- sp_contar_estudiantes_activos.sql sobre por que hace falta.
--
-- Se invoca desde StudentService.anonymize (endpoint
-- POST /api/estudiantes/{id}/anonimizar, restringido a ADMINISTRADOR y
-- anotado con @Audited). La fuente versionada vive en la migracion
-- backend/src/main/resources/db/migration/V27__sp_anonimizar_estudiante.sql.
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

    IF v_id_usuario IS NOT NULL THEN
        UPDATE seguridad.usuarios
           SET username = 'anon_' || v_id_usuario,
               activo   = FALSE
         WHERE id_usuario = v_id_usuario;
        GET DIAGNOSTICS v_n = ROW_COUNT;
        campos_afectados := campos_afectados + v_n;
    END IF;

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

    UPDATE academico.estudiantes
       SET activo = FALSE
     WHERE id_estudiante = p_id_estudiante
       AND activo = TRUE;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    campos_afectados := campos_afectados + v_n;
END;
$$;
