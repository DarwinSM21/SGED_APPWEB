-- sp_validar_categoria_estudiante_sesion
-- Propósito: validación cruzada -un estudiante solo puede marcar
--            asistencia en una sesión de su propia categoría; sin esto,
--            AsistenciaService.marcarPorQr aceptaba el QR de cualquier
--            sesión sin comprobar la categoría del estudiante.
-- Entrada:  p_estudiante BIGINT, p_sesion BIGINT
-- Salida:   coincide BOOLEAN (parametro OUT)
-- Tablas:   academico.estudiantes, deportivo.sesiones_entrenamiento
-- Sin SQL dinámico. Parámetros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): ver nota en
-- sp_contar_estudiantes_activos.sql sobre por que hace falta.
CREATE OR REPLACE PROCEDURE deportivo.sp_validar_categoria_estudiante_sesion(
    IN p_estudiante BIGINT,
    IN p_sesion BIGINT,
    OUT coincide BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_categoria_estudiante BIGINT;
    v_categoria_sesion BIGINT;
BEGIN
    SELECT id_categoria INTO v_categoria_estudiante
      FROM academico.estudiantes
     WHERE id_estudiante = p_estudiante;

    SELECT id_categoria INTO v_categoria_sesion
      FROM deportivo.sesiones_entrenamiento
     WHERE id_sesion = p_sesion;

    coincide := (v_categoria_estudiante IS NOT NULL)
        AND (v_categoria_sesion IS NOT NULL)
        AND (v_categoria_estudiante = v_categoria_sesion);
END;
$$;
