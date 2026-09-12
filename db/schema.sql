-- ==============================================================
-- SGED - Esquema consolidado
-- Regenerado con pg_dump --schema-only a partir de una base con
-- las migraciones Flyway V1..V28 aplicadas (incluye cedula opcional
-- de V26 y correo_verificado de V28). Se monta en
-- /docker-entrypoint-initdb.d/ para reproducibilidad desde
-- clonacion limpia. Prohibido ddl-auto=update.
--
-- Regenerar cuando se agregue una migracion nueva:
--   1. Aplicar V1..Vn sobre un Postgres limpio (o partir de un dump
--      de produccion).
--   2. pg_dump --schema-only --no-owner --no-privileges
--      --schema=academico --schema=deportivo --schema=seguridad
--      --schema=inventario > db/schema.sql
--   3. Verificar contra las entidades JPA: arrancar el backend con
--      ddl-auto=validate contra esa base y confirmar arranque limpio.
-- ==============================================================


CREATE SCHEMA IF NOT EXISTS academico;

CREATE SCHEMA IF NOT EXISTS deportivo;

CREATE SCHEMA IF NOT EXISTS inventario;

CREATE SCHEMA IF NOT EXISTS seguridad;

CREATE FUNCTION academico.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE PROCEDURE academico.sp_anonimizar_estudiante(IN p_id_estudiante bigint, OUT campos_afectados integer)
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

CREATE PROCEDURE academico.sp_contacto_representante_estudiante(IN p_estudiante bigint, OUT contacto_info character varying)
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

CREATE PROCEDURE academico.sp_contar_estudiantes_activos(IN p_categoria integer, OUT total bigint)
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

CREATE PROCEDURE academico.sp_desactivar_estudiantes_categoria(IN p_categoria integer, OUT afectados integer)
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

CREATE PROCEDURE academico.sp_generar_codigo_estudiante(IN p_anio integer, OUT codigo character varying)
    LANGUAGE plpgsql
    AS $_$
DECLARE
    v_prefijo VARCHAR(10) := 'EST-' || p_anio::TEXT || '-';
    v_siguiente INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) AS INT)), 0) + 1
      INTO v_siguiente
      FROM academico.estudiantes
     WHERE codigo_estudiante LIKE v_prefijo || '%'
       AND SUBSTRING(codigo_estudiante FROM LENGTH(v_prefijo) + 1) ~ '^[0-9]+$';

    codigo := v_prefijo || LPAD(v_siguiente::TEXT, 4, '0');
END;
$_$;

CREATE FUNCTION deportivo.set_actualizado_en() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$;

CREATE PROCEDURE deportivo.sp_reporte_asistencia_estudiante(IN p_estudiante bigint, IN p_desde date, IN p_hasta date, OUT porcentaje_asistencia numeric)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_categoria BIGINT;
    v_corte DATE;
    v_total_sesiones INT;
    v_total_presentes INT;
BEGIN
    -- Una sesion de hoy o posterior todavia no ocurrio: nadie pudo asistir.
    -- Si entrara al denominador, el porcentaje de todos caeria cada manana
    -- al generarse la sesion del dia y se recuperaria recien cuando el
    -- entrenador pasa lista, castigando al estudiante por una ausencia que
    -- aun no existe. Se mide hasta ayer. El recorte va aqui y no en cada
    -- servicio porque los tres llamadores pasan hasta = hoy.
    v_corte := LEAST(p_hasta, CURRENT_DATE - 1);

    SELECT id_categoria INTO v_categoria
      FROM academico.estudiantes
     WHERE id_estudiante = p_estudiante;

    SELECT COUNT(*) INTO v_total_sesiones
      FROM deportivo.sesiones_entrenamiento se
     WHERE se.id_categoria = v_categoria
       AND se.fecha BETWEEN p_desde AND v_corte;

    SELECT COUNT(*) INTO v_total_presentes
      FROM deportivo.asistencias a
      JOIN deportivo.sesiones_entrenamiento se ON se.id_sesion = a.id_sesion
     WHERE a.id_estudiante = p_estudiante
       AND a.estado IN ('PRESENTE', 'TARDE')
       AND se.fecha BETWEEN p_desde AND v_corte;

    IF v_total_sesiones = 0 THEN
        porcentaje_asistencia := NULL;
    ELSE
        porcentaje_asistencia := ROUND((v_total_presentes::NUMERIC / v_total_sesiones) * 100, 2);
    END IF;
END;
$$;

CREATE PROCEDURE deportivo.sp_validar_categoria_estudiante_sesion(IN p_estudiante bigint, IN p_sesion bigint, OUT coincide boolean)
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

CREATE FUNCTION inventario.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE PROCEDURE inventario.sp_reporte_stock_bajo(OUT total_bajo_stock bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
    SELECT COUNT(*)
      INTO total_bajo_stock
      FROM inventario.articulos
     WHERE activo = TRUE
       AND stock_actual <= stock_minimo;
END;
$$;

CREATE FUNCTION seguridad.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS academico.consentimientos (
    id_consentimiento bigint NOT NULL,
    id_representante bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    alcance character varying(50) NOT NULL,
    otorgado_en timestamp with time zone DEFAULT now() NOT NULL,
    registrado_por_id_usuario bigint,
    revocado_en timestamp with time zone,
    revocado_por_id_usuario bigint
);

CREATE SEQUENCE IF NOT EXISTS academico.consentimientos_id_consentimiento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE academico.consentimientos_id_consentimiento_seq OWNED BY academico.consentimientos.id_consentimiento;

CREATE TABLE IF NOT EXISTS academico.estudiantes (
    id_estudiante bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_categoria bigint NOT NULL,
    id_estado_general bigint NOT NULL,
    codigo_estudiante character varying(30) NOT NULL,
    fecha_ingreso date NOT NULL,
    peso numeric(5,2),
    altura numeric(5,2),
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    id_posicion bigint,
    rfid_codigo character varying(100),
    id_usuario bigint,
    CONSTRAINT estudiantes_altura_check CHECK ((altura > (0)::numeric)),
    CONSTRAINT estudiantes_peso_check CHECK ((peso > (0)::numeric))
);

CREATE SEQUENCE IF NOT EXISTS academico.estudiantes_id_estudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE academico.estudiantes_id_estudiante_seq OWNED BY academico.estudiantes.id_estudiante;

CREATE TABLE IF NOT EXISTS academico.notificaciones (
    id_notificacion bigint NOT NULL,
    id_representante bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    mensaje text NOT NULL,
    leida boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notificaciones_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['ASISTENCIA'::character varying, 'LESION'::character varying])::text[])))
);

CREATE SEQUENCE IF NOT EXISTS academico.notificaciones_id_notificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE academico.notificaciones_id_notificacion_seq OWNED BY academico.notificaciones.id_notificacion;

CREATE TABLE IF NOT EXISTS academico.pagos (
    id_pago bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    anio smallint,
    mes smallint,
    monto numeric(8,2) NOT NULL,
    fecha_pago date NOT NULL,
    registrado_por_id_usuario bigint NOT NULL,
    anulado_en timestamp with time zone,
    anulado_por_id_usuario bigint,
    motivo_anulacion character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_pago_anulacion_completa CHECK ((((anulado_en IS NULL) AND (anulado_por_id_usuario IS NULL) AND (motivo_anulacion IS NULL)) OR ((anulado_en IS NOT NULL) AND (anulado_por_id_usuario IS NOT NULL) AND (motivo_anulacion IS NOT NULL)))),
    CONSTRAINT chk_pago_periodo_segun_tipo CHECK (((((tipo)::text = 'MEMBRESIA'::text) AND (anio IS NOT NULL) AND (mes IS NOT NULL)) OR (((tipo)::text = 'DIARIO'::text) AND (anio IS NULL) AND (mes IS NULL)))),
    CONSTRAINT pagos_mes_check CHECK (((mes >= 1) AND (mes <= 12))),
    CONSTRAINT pagos_monto_check CHECK ((monto > (0)::numeric)),
    CONSTRAINT pagos_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['MEMBRESIA'::character varying, 'DIARIO'::character varying])::text[])))
);

CREATE SEQUENCE IF NOT EXISTS academico.pagos_id_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE academico.pagos_id_pago_seq OWNED BY academico.pagos.id_pago;

CREATE TABLE IF NOT EXISTS academico.representante_estudiante (
    id_representante_estudiante bigint NOT NULL,
    id_representante bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    relacion character varying(50),
    contacto_principal boolean DEFAULT false NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS academico.representante_estudiante_id_representante_estudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE academico.representante_estudiante_id_representante_estudiante_seq OWNED BY academico.representante_estudiante.id_representante_estudiante;

CREATE TABLE IF NOT EXISTS academico.representantes (
    id_representante bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_usuario bigint NOT NULL,
    parentesco character varying(30),
    telefono_contacto character varying(20),
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS academico.representantes_id_representante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE academico.representantes_id_representante_seq OWNED BY academico.representantes.id_representante;

CREATE TABLE IF NOT EXISTS deportivo.alineacion_jugador (
    id_alineacion_jugador bigint NOT NULL,
    id_alineacion bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_posicion bigint,
    titular boolean DEFAULT true NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS deportivo.alineacion_jugador_id_alineacion_jugador_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.alineacion_jugador_id_alineacion_jugador_seq OWNED BY deportivo.alineacion_jugador.id_alineacion_jugador;

CREATE TABLE IF NOT EXISTS deportivo.alineaciones (
    id_alineacion bigint NOT NULL,
    id_partido bigint NOT NULL,
    valoracion smallint,
    observacion character varying(500),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT alineaciones_valoracion_check CHECK (((valoracion >= 1) AND (valoracion <= 5)))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.alineaciones_id_alineacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.alineaciones_id_alineacion_seq OWNED BY deportivo.alineaciones.id_alineacion;

CREATE TABLE IF NOT EXISTS deportivo.asistencias (
    id_asistencia bigint NOT NULL,
    id_sesion bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    hora_entrada time without time zone,
    metodo character varying(10) DEFAULT 'MANUAL'::character varying NOT NULL,
    estado character varying(15) NOT NULL,
    observacion character varying(255),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT asistencias_estado_check CHECK (((estado)::text = ANY ((ARRAY['PRESENTE'::character varying, 'TARDE'::character varying, 'AUSENTE'::character varying, 'JUSTIFICADO'::character varying])::text[]))),
    CONSTRAINT asistencias_metodo_check CHECK (((metodo)::text = ANY ((ARRAY['QR'::character varying, 'RFID'::character varying, 'MANUAL'::character varying])::text[])))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.asistencias_id_asistencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.asistencias_id_asistencia_seq OWNED BY deportivo.asistencias.id_asistencia;

CREATE TABLE IF NOT EXISTS deportivo.categorias (
    id_categoria bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    edad_min smallint NOT NULL,
    edad_max smallint NOT NULL,
    descripcion character varying(255),
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS deportivo.categorias_id_categoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.categorias_id_categoria_seq OWNED BY deportivo.categorias.id_categoria;

CREATE TABLE IF NOT EXISTS deportivo.criterios_evaluacion (
    id_criterio bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(255),
    puntaje_maximo smallint DEFAULT 10 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT criterios_evaluacion_puntaje_maximo_check CHECK ((puntaje_maximo > 0))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.criterios_evaluacion_id_criterio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.criterios_evaluacion_id_criterio_seq OWNED BY deportivo.criterios_evaluacion.id_criterio;

CREATE TABLE IF NOT EXISTS deportivo.detalle_evaluacion (
    id_detalle bigint NOT NULL,
    id_criterio bigint NOT NULL,
    puntaje numeric(4,1) NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    id_evaluacion_estudiante bigint NOT NULL,
    CONSTRAINT detalle_evaluacion_puntaje_check CHECK ((puntaje >= (0)::numeric))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.detalle_evaluacion_id_detalle_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.detalle_evaluacion_id_detalle_seq OWNED BY deportivo.detalle_evaluacion.id_detalle;

CREATE TABLE IF NOT EXISTS deportivo.ejercicios (
    id_ejercicio bigint NOT NULL,
    nombre character varying(255) NOT NULL,
    descripcion text,
    duracion_min smallint NOT NULL,
    nivel character varying(50)
);

CREATE SEQUENCE IF NOT EXISTS deportivo.ejercicios_id_ejercicio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.ejercicios_id_ejercicio_seq OWNED BY deportivo.ejercicios.id_ejercicio;

CREATE TABLE IF NOT EXISTS deportivo.entrenadores (
    id_entrenador bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_usuario bigint NOT NULL,
    id_especialidad bigint,
    experiencia_anios smallint,
    certificacion character varying(255),
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS deportivo.entrenadores_id_entrenador_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.entrenadores_id_entrenador_seq OWNED BY deportivo.entrenadores.id_entrenador;

CREATE TABLE IF NOT EXISTS deportivo.entrenamiento_ejercicios (
    id_entrenamiento_ejercicio bigint NOT NULL,
    id_sesion_entrenamiento bigint,
    id_ejercicio bigint,
    observaciones text
);

CREATE SEQUENCE IF NOT EXISTS deportivo.entrenamiento_ejercicios_id_entrenamiento_ejercicio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.entrenamiento_ejercicios_id_entrenamiento_ejercicio_seq OWNED BY deportivo.entrenamiento_ejercicios.id_entrenamiento_ejercicio;

CREATE TABLE IF NOT EXISTS deportivo.equipos (
    id_equipo bigint NOT NULL,
    id_categoria bigint,
    id_estado_general bigint,
    nombre_equipo character varying(100) NOT NULL,
    siglas character varying(10),
    logo_url text,
    activo boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS deportivo.equipos_id_equipo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.equipos_id_equipo_seq OWNED BY deportivo.equipos.id_equipo;

CREATE TABLE IF NOT EXISTS deportivo.especialidades (
    id_especialidad bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS deportivo.especialidades_id_especialidad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.especialidades_id_especialidad_seq OWNED BY deportivo.especialidades.id_especialidad;

CREATE TABLE IF NOT EXISTS deportivo.estadistica_partidos (
    id_estadistica_partido bigint NOT NULL,
    id_partido bigint,
    id_estudiante bigint,
    goles smallint DEFAULT 0,
    asistencias smallint DEFAULT 0,
    tarjetas_amarillas smallint DEFAULT 0,
    tarjetas_rojas smallint DEFAULT 0,
    minutos_jugados smallint DEFAULT 0
);

CREATE SEQUENCE IF NOT EXISTS deportivo.estadistica_partidos_id_estadistica_partido_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.estadistica_partidos_id_estadistica_partido_seq OWNED BY deportivo.estadistica_partidos.id_estadistica_partido;

CREATE TABLE IF NOT EXISTS deportivo.evaluacion_estudiante (
    id_evaluacion_estudiante bigint NOT NULL,
    id_evaluacion bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_categoria_dia bigint NOT NULL,
    id_posicion_jugada bigint,
    id_lesion bigint,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS deportivo.evaluacion_estudiante_id_evaluacion_estudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.evaluacion_estudiante_id_evaluacion_estudiante_seq OWNED BY deportivo.evaluacion_estudiante.id_evaluacion_estudiante;

CREATE TABLE IF NOT EXISTS deportivo.evaluaciones_diarias (
    id_evaluacion bigint NOT NULL,
    id_sesion bigint NOT NULL,
    id_entrenador bigint NOT NULL,
    fecha date NOT NULL,
    observacion_general text,
    estado character varying(15) DEFAULT 'BORRADOR'::character varying NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_evaluacion_observacion_longitud CHECK (((observacion_general IS NULL) OR (char_length(observacion_general) <= 2000))),
    CONSTRAINT evaluaciones_diarias_estado_check CHECK (((estado)::text = ANY ((ARRAY['BORRADOR'::character varying, 'FINALIZADA'::character varying])::text[])))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.evaluaciones_diarias_id_evaluacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.evaluaciones_diarias_id_evaluacion_seq OWNED BY deportivo.evaluaciones_diarias.id_evaluacion;

CREATE TABLE IF NOT EXISTS deportivo.horarios_entrenamiento (
    id_horario bigint NOT NULL,
    id_entrenador bigint NOT NULL,
    dia_semana smallint NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    campo character varying(100),
    descripcion character varying(255),
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    id_categoria bigint NOT NULL,
    CONSTRAINT horarios_entrenamiento_check CHECK ((hora_fin > hora_inicio)),
    CONSTRAINT horarios_entrenamiento_dia_semana_check CHECK (((dia_semana >= 1) AND (dia_semana <= 7)))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.horarios_entrenamiento_id_horario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.horarios_entrenamiento_id_horario_seq OWNED BY deportivo.horarios_entrenamiento.id_horario;

CREATE TABLE IF NOT EXISTS deportivo.lesiones (
    id_lesion bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_entrenador bigint NOT NULL,
    descripcion text NOT NULL,
    fecha_lesion date DEFAULT CURRENT_DATE NOT NULL,
    fecha_estimada_retorno date,
    fecha_alta date,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_lesion_alta_posterior CHECK (((fecha_alta IS NULL) OR (fecha_alta >= fecha_lesion))),
    CONSTRAINT ck_lesion_descripcion_longitud CHECK ((char_length(descripcion) <= 1000)),
    CONSTRAINT ck_lesion_retorno_posterior CHECK (((fecha_estimada_retorno IS NULL) OR (fecha_estimada_retorno >= fecha_lesion)))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.lesiones_id_lesion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.lesiones_id_lesion_seq OWNED BY deportivo.lesiones.id_lesion;

CREATE TABLE IF NOT EXISTS deportivo.observaciones_estudiante (
    id_observacion bigint NOT NULL,
    id_evaluacion bigint NOT NULL,
    id_estudiante bigint NOT NULL,
    id_entrenador bigint NOT NULL,
    texto text NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_observacion_estudiante_longitud CHECK ((char_length(texto) <= 2000))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.observaciones_estudiante_id_observacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.observaciones_estudiante_id_observacion_seq OWNED BY deportivo.observaciones_estudiante.id_observacion;

CREATE TABLE IF NOT EXISTS deportivo.partidos (
    id_partido bigint NOT NULL,
    id_categoria bigint NOT NULL,
    fecha date NOT NULL,
    hora time without time zone,
    goles_favor smallint,
    goles_contra smallint,
    observacion character varying(500),
    cerrado boolean DEFAULT false NOT NULL,
    cerrado_en timestamp with time zone,
    cerrado_por_id_usuario bigint,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_partido_cerrado_con_fecha CHECK ((cerrado = (cerrado_en IS NOT NULL))),
    CONSTRAINT chk_partido_cerrado_con_marcador CHECK (((NOT cerrado) OR ((goles_favor IS NOT NULL) AND (goles_contra IS NOT NULL)))),
    CONSTRAINT chk_partido_goles_no_negativos CHECK (((goles_favor >= 0) AND (goles_contra >= 0))),
    CONSTRAINT chk_partido_marcador_completo CHECK (((goles_favor IS NULL) = (goles_contra IS NULL)))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.partidos_id_partido_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.partidos_id_partido_seq OWNED BY deportivo.partidos.id_partido;

CREATE TABLE IF NOT EXISTS deportivo.posiciones (
    id_posicion bigint NOT NULL,
    nombre character varying(50) NOT NULL,
    abreviatura character varying(5),
    descripcion character varying(255),
    activo boolean DEFAULT true NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS deportivo.posiciones_id_posicion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.posiciones_id_posicion_seq OWNED BY deportivo.posiciones.id_posicion;

CREATE TABLE IF NOT EXISTS deportivo.sesiones_entrenamiento (
    id_sesion bigint NOT NULL,
    id_horario bigint,
    id_entrenador bigint NOT NULL,
    fecha date NOT NULL,
    hora_inicio time without time zone,
    hora_fin time without time zone,
    campo character varying(100),
    estado character varying(20) DEFAULT 'PROGRAMADA'::character varying NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    id_categoria bigint NOT NULL,
    CONSTRAINT sesiones_entrenamiento_estado_check CHECK (((estado)::text = ANY ((ARRAY['PROGRAMADA'::character varying, 'EN_CURSO'::character varying, 'FINALIZADA'::character varying, 'CANCELADA'::character varying])::text[])))
);

CREATE SEQUENCE IF NOT EXISTS deportivo.sesiones_entrenamiento_id_sesion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE deportivo.sesiones_entrenamiento_id_sesion_seq OWNED BY deportivo.sesiones_entrenamiento.id_sesion;

CREATE OR REPLACE VIEW deportivo.v_promedio_evaluacion AS
 SELECT ee.id_evaluacion,
    ee.id_estudiante,
    ee.id_categoria_dia,
    e.fecha,
    round(avg(d.puntaje), 1) AS promedio,
    count(*) AS criterios_evaluados
   FROM ((deportivo.detalle_evaluacion d
     JOIN deportivo.evaluacion_estudiante ee ON ((ee.id_evaluacion_estudiante = d.id_evaluacion_estudiante)))
     JOIN deportivo.evaluaciones_diarias e ON ((e.id_evaluacion = ee.id_evaluacion)))
  GROUP BY ee.id_evaluacion, ee.id_estudiante, ee.id_categoria_dia, e.fecha;

CREATE TABLE IF NOT EXISTS inventario.articulos (
    id_articulo bigint NOT NULL,
    nombre character varying(150) NOT NULL,
    tipo character varying(20) NOT NULL,
    talla character varying(20),
    descripcion character varying(255),
    stock_actual integer DEFAULT 0 NOT NULL,
    stock_minimo integer DEFAULT 0 NOT NULL,
    unidad_medida character varying(20) DEFAULT 'unidad'::character varying NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT articulos_stock_actual_check CHECK ((stock_actual >= 0)),
    CONSTRAINT articulos_stock_minimo_check CHECK ((stock_minimo >= 0)),
    CONSTRAINT articulos_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['UNIFORME'::character varying, 'BALON'::character varying, 'IMPLEMENTO'::character varying, 'OTRO'::character varying])::text[])))
);

CREATE SEQUENCE IF NOT EXISTS inventario.articulos_id_articulo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE inventario.articulos_id_articulo_seq OWNED BY inventario.articulos.id_articulo;

CREATE TABLE IF NOT EXISTS inventario.asignaciones (
    id_asignacion bigint NOT NULL,
    id_articulo bigint NOT NULL,
    cantidad integer NOT NULL,
    tipo_destinatario character varying(15) NOT NULL,
    id_estudiante bigint,
    id_entrenador bigint,
    fecha_asignacion date DEFAULT CURRENT_DATE NOT NULL,
    fecha_devolucion_esperada date,
    fecha_devolucion_real date,
    estado character varying(15) DEFAULT 'ASIGNADO'::character varying NOT NULL,
    registrado_por_id_usuario bigint NOT NULL,
    observaciones character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT asignaciones_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT asignaciones_estado_check CHECK (((estado)::text = ANY ((ARRAY['ASIGNADO'::character varying, 'DEVUELTO'::character varying, 'PERDIDO'::character varying])::text[]))),
    CONSTRAINT asignaciones_tipo_destinatario_check CHECK (((tipo_destinatario)::text = ANY ((ARRAY['ESTUDIANTE'::character varying, 'ENTRENADOR'::character varying])::text[]))),
    CONSTRAINT chk_asignacion_destinatario CHECK (((((tipo_destinatario)::text = 'ESTUDIANTE'::text) AND (id_estudiante IS NOT NULL) AND (id_entrenador IS NULL)) OR (((tipo_destinatario)::text = 'ENTRENADOR'::text) AND (id_entrenador IS NOT NULL) AND (id_estudiante IS NULL))))
);

CREATE SEQUENCE IF NOT EXISTS inventario.asignaciones_id_asignacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE inventario.asignaciones_id_asignacion_seq OWNED BY inventario.asignaciones.id_asignacion;

CREATE TABLE IF NOT EXISTS inventario.movimientos_stock (
    id_movimiento bigint NOT NULL,
    id_articulo bigint NOT NULL,
    tipo_movimiento character varying(10) NOT NULL,
    cantidad integer NOT NULL,
    motivo character varying(255),
    registrado_por_id_usuario bigint NOT NULL,
    fecha_movimiento timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT movimientos_stock_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT movimientos_stock_tipo_movimiento_check CHECK (((tipo_movimiento)::text = ANY ((ARRAY['ENTRADA'::character varying, 'SALIDA'::character varying, 'AJUSTE'::character varying])::text[])))
);

CREATE SEQUENCE IF NOT EXISTS inventario.movimientos_stock_id_movimiento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE inventario.movimientos_stock_id_movimiento_seq OWNED BY inventario.movimientos_stock.id_movimiento;

CREATE TABLE IF NOT EXISTS seguridad.auditoria (
    id_auditoria bigint NOT NULL,
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    id_usuario bigint,
    usuario_nombre character varying(150) NOT NULL,
    rol character varying(50),
    accion character varying(30) NOT NULL,
    entidad character varying(100),
    entidad_id bigint,
    descripcion text NOT NULL,
    ip character varying(45)
);

CREATE SEQUENCE IF NOT EXISTS seguridad.auditoria_id_auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE seguridad.auditoria_id_auditoria_seq OWNED BY seguridad.auditoria.id_auditoria;

CREATE TABLE IF NOT EXISTS seguridad.estados_general (
    id_estado_general bigint NOT NULL,
    nombre character varying(100) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS seguridad.estados_general_id_estado_general_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE seguridad.estados_general_id_estado_general_seq OWNED BY seguridad.estados_general.id_estado_general;

CREATE TABLE IF NOT EXISTS seguridad.personas (
    id_persona bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    apellido character varying(100) NOT NULL,
    cedula character varying(10),
    correo character varying(200) NOT NULL,
    telefono character varying(15),
    foto text,
    fecha_nacimiento date NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    correo_verificado boolean DEFAULT false NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS seguridad.personas_id_persona_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE seguridad.personas_id_persona_seq OWNED BY seguridad.personas.id_persona;

CREATE TABLE IF NOT EXISTS seguridad.roles (
    id_rol bigint NOT NULL,
    nombre character varying(50) NOT NULL,
    descripcion character varying(255)
);

CREATE SEQUENCE IF NOT EXISTS seguridad.roles_id_rol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE seguridad.roles_id_rol_seq OWNED BY seguridad.roles.id_rol;

CREATE TABLE IF NOT EXISTS seguridad.usuario_rol (
    id_usuario_rol bigint NOT NULL,
    id_usuario bigint NOT NULL,
    id_rol bigint NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS seguridad.usuario_rol_id_usuario_rol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE seguridad.usuario_rol_id_usuario_rol_seq OWNED BY seguridad.usuario_rol.id_usuario_rol;

CREATE TABLE IF NOT EXISTS seguridad.usuarios (
    id_usuario bigint NOT NULL,
    id_persona bigint NOT NULL,
    id_estado_general bigint NOT NULL,
    username character varying(50) NOT NULL,
    password_hash text NOT NULL,
    ultimo_acceso timestamp with time zone,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS seguridad.usuarios_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE seguridad.usuarios_id_usuario_seq OWNED BY seguridad.usuarios.id_usuario;

ALTER TABLE ONLY academico.consentimientos ALTER COLUMN id_consentimiento SET DEFAULT nextval('academico.consentimientos_id_consentimiento_seq'::regclass);

ALTER TABLE ONLY academico.estudiantes ALTER COLUMN id_estudiante SET DEFAULT nextval('academico.estudiantes_id_estudiante_seq'::regclass);

ALTER TABLE ONLY academico.notificaciones ALTER COLUMN id_notificacion SET DEFAULT nextval('academico.notificaciones_id_notificacion_seq'::regclass);

ALTER TABLE ONLY academico.pagos ALTER COLUMN id_pago SET DEFAULT nextval('academico.pagos_id_pago_seq'::regclass);

ALTER TABLE ONLY academico.representante_estudiante ALTER COLUMN id_representante_estudiante SET DEFAULT nextval('academico.representante_estudiante_id_representante_estudiante_seq'::regclass);

ALTER TABLE ONLY academico.representantes ALTER COLUMN id_representante SET DEFAULT nextval('academico.representantes_id_representante_seq'::regclass);

ALTER TABLE ONLY deportivo.alineacion_jugador ALTER COLUMN id_alineacion_jugador SET DEFAULT nextval('deportivo.alineacion_jugador_id_alineacion_jugador_seq'::regclass);

ALTER TABLE ONLY deportivo.alineaciones ALTER COLUMN id_alineacion SET DEFAULT nextval('deportivo.alineaciones_id_alineacion_seq'::regclass);

ALTER TABLE ONLY deportivo.asistencias ALTER COLUMN id_asistencia SET DEFAULT nextval('deportivo.asistencias_id_asistencia_seq'::regclass);

ALTER TABLE ONLY deportivo.categorias ALTER COLUMN id_categoria SET DEFAULT nextval('deportivo.categorias_id_categoria_seq'::regclass);

ALTER TABLE ONLY deportivo.criterios_evaluacion ALTER COLUMN id_criterio SET DEFAULT nextval('deportivo.criterios_evaluacion_id_criterio_seq'::regclass);

ALTER TABLE ONLY deportivo.detalle_evaluacion ALTER COLUMN id_detalle SET DEFAULT nextval('deportivo.detalle_evaluacion_id_detalle_seq'::regclass);

ALTER TABLE ONLY deportivo.ejercicios ALTER COLUMN id_ejercicio SET DEFAULT nextval('deportivo.ejercicios_id_ejercicio_seq'::regclass);

ALTER TABLE ONLY deportivo.entrenadores ALTER COLUMN id_entrenador SET DEFAULT nextval('deportivo.entrenadores_id_entrenador_seq'::regclass);

ALTER TABLE ONLY deportivo.entrenamiento_ejercicios ALTER COLUMN id_entrenamiento_ejercicio SET DEFAULT nextval('deportivo.entrenamiento_ejercicios_id_entrenamiento_ejercicio_seq'::regclass);

ALTER TABLE ONLY deportivo.equipos ALTER COLUMN id_equipo SET DEFAULT nextval('deportivo.equipos_id_equipo_seq'::regclass);

ALTER TABLE ONLY deportivo.especialidades ALTER COLUMN id_especialidad SET DEFAULT nextval('deportivo.especialidades_id_especialidad_seq'::regclass);

ALTER TABLE ONLY deportivo.estadistica_partidos ALTER COLUMN id_estadistica_partido SET DEFAULT nextval('deportivo.estadistica_partidos_id_estadistica_partido_seq'::regclass);

ALTER TABLE ONLY deportivo.evaluacion_estudiante ALTER COLUMN id_evaluacion_estudiante SET DEFAULT nextval('deportivo.evaluacion_estudiante_id_evaluacion_estudiante_seq'::regclass);

ALTER TABLE ONLY deportivo.evaluaciones_diarias ALTER COLUMN id_evaluacion SET DEFAULT nextval('deportivo.evaluaciones_diarias_id_evaluacion_seq'::regclass);

ALTER TABLE ONLY deportivo.horarios_entrenamiento ALTER COLUMN id_horario SET DEFAULT nextval('deportivo.horarios_entrenamiento_id_horario_seq'::regclass);

ALTER TABLE ONLY deportivo.lesiones ALTER COLUMN id_lesion SET DEFAULT nextval('deportivo.lesiones_id_lesion_seq'::regclass);

ALTER TABLE ONLY deportivo.observaciones_estudiante ALTER COLUMN id_observacion SET DEFAULT nextval('deportivo.observaciones_estudiante_id_observacion_seq'::regclass);

ALTER TABLE ONLY deportivo.partidos ALTER COLUMN id_partido SET DEFAULT nextval('deportivo.partidos_id_partido_seq'::regclass);

ALTER TABLE ONLY deportivo.posiciones ALTER COLUMN id_posicion SET DEFAULT nextval('deportivo.posiciones_id_posicion_seq'::regclass);

ALTER TABLE ONLY deportivo.sesiones_entrenamiento ALTER COLUMN id_sesion SET DEFAULT nextval('deportivo.sesiones_entrenamiento_id_sesion_seq'::regclass);

ALTER TABLE ONLY inventario.articulos ALTER COLUMN id_articulo SET DEFAULT nextval('inventario.articulos_id_articulo_seq'::regclass);

ALTER TABLE ONLY inventario.asignaciones ALTER COLUMN id_asignacion SET DEFAULT nextval('inventario.asignaciones_id_asignacion_seq'::regclass);

ALTER TABLE ONLY inventario.movimientos_stock ALTER COLUMN id_movimiento SET DEFAULT nextval('inventario.movimientos_stock_id_movimiento_seq'::regclass);

ALTER TABLE ONLY seguridad.auditoria ALTER COLUMN id_auditoria SET DEFAULT nextval('seguridad.auditoria_id_auditoria_seq'::regclass);

ALTER TABLE ONLY seguridad.estados_general ALTER COLUMN id_estado_general SET DEFAULT nextval('seguridad.estados_general_id_estado_general_seq'::regclass);

ALTER TABLE ONLY seguridad.personas ALTER COLUMN id_persona SET DEFAULT nextval('seguridad.personas_id_persona_seq'::regclass);

ALTER TABLE ONLY seguridad.roles ALTER COLUMN id_rol SET DEFAULT nextval('seguridad.roles_id_rol_seq'::regclass);

ALTER TABLE ONLY seguridad.usuario_rol ALTER COLUMN id_usuario_rol SET DEFAULT nextval('seguridad.usuario_rol_id_usuario_rol_seq'::regclass);

ALTER TABLE ONLY seguridad.usuarios ALTER COLUMN id_usuario SET DEFAULT nextval('seguridad.usuarios_id_usuario_seq'::regclass);

ALTER TABLE ONLY academico.consentimientos
    ADD CONSTRAINT consentimientos_pkey PRIMARY KEY (id_consentimiento);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_id_persona_key UNIQUE (id_persona);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_id_usuario_key UNIQUE (id_usuario);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_pkey PRIMARY KEY (id_estudiante);

ALTER TABLE ONLY academico.notificaciones
    ADD CONSTRAINT notificaciones_pkey PRIMARY KEY (id_notificacion);

ALTER TABLE ONLY academico.pagos
    ADD CONSTRAINT pagos_pkey PRIMARY KEY (id_pago);

ALTER TABLE ONLY academico.representante_estudiante
    ADD CONSTRAINT representante_estudiante_id_representante_id_estudiante_key UNIQUE (id_representante, id_estudiante);

ALTER TABLE ONLY academico.representante_estudiante
    ADD CONSTRAINT representante_estudiante_pkey PRIMARY KEY (id_representante_estudiante);

ALTER TABLE ONLY academico.representantes
    ADD CONSTRAINT representantes_id_persona_key UNIQUE (id_persona);

ALTER TABLE ONLY academico.representantes
    ADD CONSTRAINT representantes_id_usuario_key UNIQUE (id_usuario);

ALTER TABLE ONLY academico.representantes
    ADD CONSTRAINT representantes_pkey PRIMARY KEY (id_representante);

ALTER TABLE ONLY deportivo.alineacion_jugador
    ADD CONSTRAINT alineacion_jugador_pkey PRIMARY KEY (id_alineacion_jugador);

ALTER TABLE ONLY deportivo.alineaciones
    ADD CONSTRAINT alineaciones_id_partido_key UNIQUE (id_partido);

ALTER TABLE ONLY deportivo.alineaciones
    ADD CONSTRAINT alineaciones_pkey PRIMARY KEY (id_alineacion);

ALTER TABLE ONLY deportivo.asistencias
    ADD CONSTRAINT asistencias_pkey PRIMARY KEY (id_asistencia);

ALTER TABLE ONLY deportivo.categorias
    ADD CONSTRAINT categorias_pkey PRIMARY KEY (id_categoria);

ALTER TABLE ONLY deportivo.criterios_evaluacion
    ADD CONSTRAINT criterios_evaluacion_nombre_key UNIQUE (nombre);

ALTER TABLE ONLY deportivo.criterios_evaluacion
    ADD CONSTRAINT criterios_evaluacion_pkey PRIMARY KEY (id_criterio);

ALTER TABLE ONLY deportivo.detalle_evaluacion
    ADD CONSTRAINT detalle_evaluacion_pkey PRIMARY KEY (id_detalle);

ALTER TABLE ONLY deportivo.ejercicios
    ADD CONSTRAINT ejercicios_pkey PRIMARY KEY (id_ejercicio);

ALTER TABLE ONLY deportivo.entrenadores
    ADD CONSTRAINT entrenadores_id_persona_key UNIQUE (id_persona);

ALTER TABLE ONLY deportivo.entrenadores
    ADD CONSTRAINT entrenadores_id_usuario_key UNIQUE (id_usuario);

ALTER TABLE ONLY deportivo.entrenadores
    ADD CONSTRAINT entrenadores_pkey PRIMARY KEY (id_entrenador);

ALTER TABLE ONLY deportivo.entrenamiento_ejercicios
    ADD CONSTRAINT entrenamiento_ejercicios_pkey PRIMARY KEY (id_entrenamiento_ejercicio);

ALTER TABLE ONLY deportivo.equipos
    ADD CONSTRAINT equipos_pkey PRIMARY KEY (id_equipo);

ALTER TABLE ONLY deportivo.especialidades
    ADD CONSTRAINT especialidades_nombre_key UNIQUE (nombre);

ALTER TABLE ONLY deportivo.especialidades
    ADD CONSTRAINT especialidades_pkey PRIMARY KEY (id_especialidad);

ALTER TABLE ONLY deportivo.estadistica_partidos
    ADD CONSTRAINT estadistica_partidos_pkey PRIMARY KEY (id_estadistica_partido);

ALTER TABLE ONLY deportivo.evaluacion_estudiante
    ADD CONSTRAINT evaluacion_estudiante_pkey PRIMARY KEY (id_evaluacion_estudiante);

ALTER TABLE ONLY deportivo.evaluaciones_diarias
    ADD CONSTRAINT evaluaciones_diarias_pkey PRIMARY KEY (id_evaluacion);

ALTER TABLE ONLY deportivo.horarios_entrenamiento
    ADD CONSTRAINT horarios_entrenamiento_pkey PRIMARY KEY (id_horario);

ALTER TABLE ONLY deportivo.lesiones
    ADD CONSTRAINT lesiones_pkey PRIMARY KEY (id_lesion);

ALTER TABLE ONLY deportivo.observaciones_estudiante
    ADD CONSTRAINT observaciones_estudiante_pkey PRIMARY KEY (id_observacion);

ALTER TABLE ONLY deportivo.partidos
    ADD CONSTRAINT partidos_pkey PRIMARY KEY (id_partido);

ALTER TABLE ONLY deportivo.posiciones
    ADD CONSTRAINT posiciones_nombre_key UNIQUE (nombre);

ALTER TABLE ONLY deportivo.posiciones
    ADD CONSTRAINT posiciones_pkey PRIMARY KEY (id_posicion);

ALTER TABLE ONLY deportivo.sesiones_entrenamiento
    ADD CONSTRAINT sesiones_entrenamiento_pkey PRIMARY KEY (id_sesion);

ALTER TABLE ONLY deportivo.alineacion_jugador
    ADD CONSTRAINT uq_alineacion_jugador UNIQUE (id_alineacion, id_estudiante);

ALTER TABLE ONLY deportivo.asistencias
    ADD CONSTRAINT uq_asistencia_sesion_estudiante UNIQUE (id_sesion, id_estudiante);

ALTER TABLE ONLY deportivo.detalle_evaluacion
    ADD CONSTRAINT uq_detalle_evaluacion_criterio UNIQUE (id_evaluacion_estudiante, id_criterio);

ALTER TABLE ONLY deportivo.evaluacion_estudiante
    ADD CONSTRAINT uq_evaluacion_estudiante UNIQUE (id_evaluacion, id_estudiante);

ALTER TABLE ONLY deportivo.evaluaciones_diarias
    ADD CONSTRAINT uq_evaluacion_sesion UNIQUE (id_sesion);

ALTER TABLE ONLY inventario.articulos
    ADD CONSTRAINT articulos_pkey PRIMARY KEY (id_articulo);

ALTER TABLE ONLY inventario.asignaciones
    ADD CONSTRAINT asignaciones_pkey PRIMARY KEY (id_asignacion);

ALTER TABLE ONLY inventario.movimientos_stock
    ADD CONSTRAINT movimientos_stock_pkey PRIMARY KEY (id_movimiento);

ALTER TABLE ONLY seguridad.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id_auditoria);

ALTER TABLE ONLY seguridad.estados_general
    ADD CONSTRAINT estados_general_pkey PRIMARY KEY (id_estado_general);

ALTER TABLE ONLY seguridad.personas
    ADD CONSTRAINT personas_pkey PRIMARY KEY (id_persona);

ALTER TABLE ONLY seguridad.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id_rol);

ALTER TABLE ONLY seguridad.usuario_rol
    ADD CONSTRAINT usuario_rol_pkey PRIMARY KEY (id_usuario_rol);

ALTER TABLE ONLY seguridad.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id_usuario);

CREATE UNIQUE INDEX IF NOT EXISTS idx_consentimiento_vigente ON academico.consentimientos USING btree (id_representante, id_estudiante, alcance) WHERE (revocado_en IS NULL);

CREATE INDEX IF NOT EXISTS idx_consentimientos_estudiante ON academico.consentimientos USING btree (id_estudiante);

CREATE UNIQUE INDEX IF NOT EXISTS idx_estudiantes_codigo ON academico.estudiantes USING btree (codigo_estudiante);

CREATE UNIQUE INDEX IF NOT EXISTS idx_estudiantes_rfid ON academico.estudiantes USING btree (rfid_codigo) WHERE (rfid_codigo IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_notificaciones_representante ON academico.notificaciones USING btree (id_representante, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pago_membresia_unico ON academico.pagos USING btree (id_estudiante, anio, mes) WHERE (((tipo)::text = 'MEMBRESIA'::text) AND (anulado_en IS NULL));

CREATE INDEX IF NOT EXISTS idx_pagos_estudiante ON academico.pagos USING btree (id_estudiante, fecha_pago DESC);

CREATE INDEX IF NOT EXISTS idx_pagos_vigentes_por_fecha ON academico.pagos USING btree (fecha_pago) WHERE (anulado_en IS NULL);

CREATE INDEX IF NOT EXISTS idx_representante_estudiante_estudiante ON academico.representante_estudiante USING btree (id_estudiante);

CREATE INDEX IF NOT EXISTS idx_alineacion_jugador_estudiante ON deportivo.alineacion_jugador USING btree (id_estudiante);

CREATE UNIQUE INDEX IF NOT EXISTS idx_alineacion_puesto_unico ON deportivo.alineacion_jugador USING btree (id_alineacion, id_posicion) WHERE (titular AND (id_posicion IS NOT NULL));

CREATE INDEX IF NOT EXISTS idx_asistencias_estudiante ON deportivo.asistencias USING btree (id_estudiante);

CREATE INDEX IF NOT EXISTS idx_eval_estudiante_categoria_dia ON deportivo.evaluacion_estudiante USING btree (id_categoria_dia);

CREATE INDEX IF NOT EXISTS idx_eval_estudiante_estudiante ON deportivo.evaluacion_estudiante USING btree (id_estudiante);

CREATE INDEX IF NOT EXISTS idx_evaluaciones_fecha ON deportivo.evaluaciones_diarias USING btree (fecha);

CREATE UNIQUE INDEX IF NOT EXISTS idx_lesion_activa_por_estudiante ON deportivo.lesiones USING btree (id_estudiante) WHERE (fecha_alta IS NULL);

CREATE INDEX IF NOT EXISTS idx_lesiones_estudiante ON deportivo.lesiones USING btree (id_estudiante);

CREATE INDEX IF NOT EXISTS idx_observaciones_estudiante ON deportivo.observaciones_estudiante USING btree (id_estudiante);

CREATE INDEX IF NOT EXISTS idx_partido_categoria_fecha ON deportivo.partidos USING btree (id_categoria, fecha DESC);

CREATE INDEX IF NOT EXISTS idx_sesiones_categoria_fecha ON deportivo.sesiones_entrenamiento USING btree (id_categoria, fecha);

CREATE INDEX IF NOT EXISTS idx_sesiones_entrenador_fecha ON deportivo.sesiones_entrenamiento USING btree (id_entrenador, fecha);

CREATE INDEX IF NOT EXISTS idx_sesiones_fecha ON deportivo.sesiones_entrenamiento USING btree (fecha);

CREATE INDEX IF NOT EXISTS idx_articulos_tipo ON inventario.articulos USING btree (tipo);

CREATE INDEX IF NOT EXISTS idx_asignaciones_entrenador ON inventario.asignaciones USING btree (id_entrenador) WHERE (id_entrenador IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_asignaciones_estado ON inventario.asignaciones USING btree (estado);

CREATE INDEX IF NOT EXISTS idx_asignaciones_estudiante ON inventario.asignaciones USING btree (id_estudiante) WHERE (id_estudiante IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_movimientos_articulo ON inventario.movimientos_stock USING btree (id_articulo, fecha_movimiento DESC);

CREATE INDEX IF NOT EXISTS idx_auditoria_accion ON seguridad.auditoria USING btree (accion);

CREATE INDEX IF NOT EXISTS idx_auditoria_entidad ON seguridad.auditoria USING btree (entidad);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON seguridad.auditoria USING btree (fecha DESC);

CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON seguridad.auditoria USING btree (id_usuario);

CREATE UNIQUE INDEX IF NOT EXISTS idx_personas_correo ON seguridad.personas USING btree (correo);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_username ON seguridad.usuarios USING btree (username);

CREATE UNIQUE INDEX IF NOT EXISTS ux_personas_cedula_no_nula ON seguridad.personas USING btree (cedula) WHERE (cedula IS NOT NULL);

CREATE TRIGGER trg_pagos_updated_at BEFORE UPDATE ON academico.pagos FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();

CREATE TRIGGER trg_representante_estudiante_updated_at BEFORE UPDATE ON academico.representante_estudiante FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();

CREATE TRIGGER trg_representantes_updated_at BEFORE UPDATE ON academico.representantes FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();

CREATE TRIGGER trg_asistencias_actualizado_en BEFORE UPDATE ON deportivo.asistencias FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_criterios_actualizado_en BEFORE UPDATE ON deportivo.criterios_evaluacion FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_detalle_actualizado_en BEFORE UPDATE ON deportivo.detalle_evaluacion FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_eval_estudiante_actualizado_en BEFORE UPDATE ON deportivo.evaluacion_estudiante FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_evaluaciones_actualizado_en BEFORE UPDATE ON deportivo.evaluaciones_diarias FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_horarios_actualizado_en BEFORE UPDATE ON deportivo.horarios_entrenamiento FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_lesiones_actualizado_en BEFORE UPDATE ON deportivo.lesiones FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_observaciones_actualizado_en BEFORE UPDATE ON deportivo.observaciones_estudiante FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_sesiones_actualizado_en BEFORE UPDATE ON deportivo.sesiones_entrenamiento FOR EACH ROW EXECUTE FUNCTION deportivo.set_actualizado_en();

CREATE TRIGGER trg_articulos_updated_at BEFORE UPDATE ON inventario.articulos FOR EACH ROW EXECUTE FUNCTION inventario.set_updated_at();

CREATE TRIGGER trg_asignaciones_updated_at BEFORE UPDATE ON inventario.asignaciones FOR EACH ROW EXECUTE FUNCTION inventario.set_updated_at();

CREATE TRIGGER trg_usuarios_updated_at BEFORE UPDATE ON seguridad.usuarios FOR EACH ROW EXECUTE FUNCTION seguridad.set_updated_at();

ALTER TABLE ONLY academico.consentimientos
    ADD CONSTRAINT consentimientos_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY academico.consentimientos
    ADD CONSTRAINT consentimientos_id_representante_fkey FOREIGN KEY (id_representante) REFERENCES academico.representantes(id_representante);

ALTER TABLE ONLY academico.consentimientos
    ADD CONSTRAINT consentimientos_registrado_por_id_usuario_fkey FOREIGN KEY (registrado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY academico.consentimientos
    ADD CONSTRAINT consentimientos_revocado_por_id_usuario_fkey FOREIGN KEY (revocado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_id_estado_general_fkey FOREIGN KEY (id_estado_general) REFERENCES seguridad.estados_general(id_estado_general);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_id_persona_fkey FOREIGN KEY (id_persona) REFERENCES seguridad.personas(id_persona);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_id_posicion_fkey FOREIGN KEY (id_posicion) REFERENCES deportivo.posiciones(id_posicion);

ALTER TABLE ONLY academico.estudiantes
    ADD CONSTRAINT estudiantes_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY academico.notificaciones
    ADD CONSTRAINT notificaciones_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY academico.notificaciones
    ADD CONSTRAINT notificaciones_id_representante_fkey FOREIGN KEY (id_representante) REFERENCES academico.representantes(id_representante);

ALTER TABLE ONLY academico.pagos
    ADD CONSTRAINT pagos_anulado_por_id_usuario_fkey FOREIGN KEY (anulado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY academico.pagos
    ADD CONSTRAINT pagos_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY academico.pagos
    ADD CONSTRAINT pagos_registrado_por_id_usuario_fkey FOREIGN KEY (registrado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY academico.representante_estudiante
    ADD CONSTRAINT representante_estudiante_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY academico.representante_estudiante
    ADD CONSTRAINT representante_estudiante_id_representante_fkey FOREIGN KEY (id_representante) REFERENCES academico.representantes(id_representante);

ALTER TABLE ONLY academico.representantes
    ADD CONSTRAINT representantes_id_persona_fkey FOREIGN KEY (id_persona) REFERENCES seguridad.personas(id_persona);

ALTER TABLE ONLY academico.representantes
    ADD CONSTRAINT representantes_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY deportivo.alineacion_jugador
    ADD CONSTRAINT alineacion_jugador_id_alineacion_fkey FOREIGN KEY (id_alineacion) REFERENCES deportivo.alineaciones(id_alineacion) ON DELETE CASCADE;

ALTER TABLE ONLY deportivo.alineacion_jugador
    ADD CONSTRAINT alineacion_jugador_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY deportivo.alineacion_jugador
    ADD CONSTRAINT alineacion_jugador_id_posicion_fkey FOREIGN KEY (id_posicion) REFERENCES deportivo.posiciones(id_posicion);

ALTER TABLE ONLY deportivo.alineaciones
    ADD CONSTRAINT alineaciones_id_partido_fkey FOREIGN KEY (id_partido) REFERENCES deportivo.partidos(id_partido) ON DELETE CASCADE;

ALTER TABLE ONLY deportivo.asistencias
    ADD CONSTRAINT asistencias_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY deportivo.asistencias
    ADD CONSTRAINT asistencias_id_sesion_fkey FOREIGN KEY (id_sesion) REFERENCES deportivo.sesiones_entrenamiento(id_sesion);

ALTER TABLE ONLY deportivo.detalle_evaluacion
    ADD CONSTRAINT detalle_evaluacion_id_criterio_fkey FOREIGN KEY (id_criterio) REFERENCES deportivo.criterios_evaluacion(id_criterio);

ALTER TABLE ONLY deportivo.detalle_evaluacion
    ADD CONSTRAINT detalle_evaluacion_id_evaluacion_estudiante_fkey FOREIGN KEY (id_evaluacion_estudiante) REFERENCES deportivo.evaluacion_estudiante(id_evaluacion_estudiante) ON DELETE CASCADE;

ALTER TABLE ONLY deportivo.entrenadores
    ADD CONSTRAINT entrenadores_id_especialidad_fkey FOREIGN KEY (id_especialidad) REFERENCES deportivo.especialidades(id_especialidad);

ALTER TABLE ONLY deportivo.entrenadores
    ADD CONSTRAINT entrenadores_id_persona_fkey FOREIGN KEY (id_persona) REFERENCES seguridad.personas(id_persona);

ALTER TABLE ONLY deportivo.entrenadores
    ADD CONSTRAINT entrenadores_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY deportivo.entrenamiento_ejercicios
    ADD CONSTRAINT entrenamiento_ejercicios_id_ejercicio_fkey FOREIGN KEY (id_ejercicio) REFERENCES deportivo.ejercicios(id_ejercicio);

ALTER TABLE ONLY deportivo.entrenamiento_ejercicios
    ADD CONSTRAINT entrenamiento_ejercicios_id_sesion_entrenamiento_fkey FOREIGN KEY (id_sesion_entrenamiento) REFERENCES deportivo.sesiones_entrenamiento(id_sesion);

ALTER TABLE ONLY deportivo.equipos
    ADD CONSTRAINT equipos_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE ONLY deportivo.equipos
    ADD CONSTRAINT equipos_id_estado_general_fkey FOREIGN KEY (id_estado_general) REFERENCES seguridad.estados_general(id_estado_general);

ALTER TABLE ONLY deportivo.estadistica_partidos
    ADD CONSTRAINT estadistica_partidos_id_partido_fkey FOREIGN KEY (id_partido) REFERENCES deportivo.partidos(id_partido);

ALTER TABLE ONLY deportivo.evaluacion_estudiante
    ADD CONSTRAINT evaluacion_estudiante_id_categoria_dia_fkey FOREIGN KEY (id_categoria_dia) REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE ONLY deportivo.evaluacion_estudiante
    ADD CONSTRAINT evaluacion_estudiante_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY deportivo.evaluacion_estudiante
    ADD CONSTRAINT evaluacion_estudiante_id_evaluacion_fkey FOREIGN KEY (id_evaluacion) REFERENCES deportivo.evaluaciones_diarias(id_evaluacion) ON DELETE CASCADE;

ALTER TABLE ONLY deportivo.evaluacion_estudiante
    ADD CONSTRAINT evaluacion_estudiante_id_lesion_fkey FOREIGN KEY (id_lesion) REFERENCES deportivo.lesiones(id_lesion);

ALTER TABLE ONLY deportivo.evaluacion_estudiante
    ADD CONSTRAINT evaluacion_estudiante_id_posicion_jugada_fkey FOREIGN KEY (id_posicion_jugada) REFERENCES deportivo.posiciones(id_posicion);

ALTER TABLE ONLY deportivo.evaluaciones_diarias
    ADD CONSTRAINT evaluaciones_diarias_id_entrenador_fkey FOREIGN KEY (id_entrenador) REFERENCES deportivo.entrenadores(id_entrenador);

ALTER TABLE ONLY deportivo.evaluaciones_diarias
    ADD CONSTRAINT evaluaciones_diarias_id_sesion_fkey FOREIGN KEY (id_sesion) REFERENCES deportivo.sesiones_entrenamiento(id_sesion);

ALTER TABLE ONLY deportivo.horarios_entrenamiento
    ADD CONSTRAINT horarios_entrenamiento_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE ONLY deportivo.horarios_entrenamiento
    ADD CONSTRAINT horarios_entrenamiento_id_entrenador_fkey FOREIGN KEY (id_entrenador) REFERENCES deportivo.entrenadores(id_entrenador);

ALTER TABLE ONLY deportivo.lesiones
    ADD CONSTRAINT lesiones_id_entrenador_fkey FOREIGN KEY (id_entrenador) REFERENCES deportivo.entrenadores(id_entrenador);

ALTER TABLE ONLY deportivo.lesiones
    ADD CONSTRAINT lesiones_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY deportivo.observaciones_estudiante
    ADD CONSTRAINT observaciones_estudiante_id_entrenador_fkey FOREIGN KEY (id_entrenador) REFERENCES deportivo.entrenadores(id_entrenador);

ALTER TABLE ONLY deportivo.observaciones_estudiante
    ADD CONSTRAINT observaciones_estudiante_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY deportivo.observaciones_estudiante
    ADD CONSTRAINT observaciones_estudiante_id_evaluacion_fkey FOREIGN KEY (id_evaluacion) REFERENCES deportivo.evaluaciones_diarias(id_evaluacion) ON DELETE CASCADE;

ALTER TABLE ONLY deportivo.partidos
    ADD CONSTRAINT partidos_cerrado_por_id_usuario_fkey FOREIGN KEY (cerrado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY deportivo.partidos
    ADD CONSTRAINT partidos_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE ONLY deportivo.sesiones_entrenamiento
    ADD CONSTRAINT sesiones_entrenamiento_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES deportivo.categorias(id_categoria);

ALTER TABLE ONLY deportivo.sesiones_entrenamiento
    ADD CONSTRAINT sesiones_entrenamiento_id_entrenador_fkey FOREIGN KEY (id_entrenador) REFERENCES deportivo.entrenadores(id_entrenador);

ALTER TABLE ONLY deportivo.sesiones_entrenamiento
    ADD CONSTRAINT sesiones_entrenamiento_id_horario_fkey FOREIGN KEY (id_horario) REFERENCES deportivo.horarios_entrenamiento(id_horario);

ALTER TABLE ONLY inventario.asignaciones
    ADD CONSTRAINT asignaciones_id_articulo_fkey FOREIGN KEY (id_articulo) REFERENCES inventario.articulos(id_articulo);

ALTER TABLE ONLY inventario.asignaciones
    ADD CONSTRAINT asignaciones_id_entrenador_fkey FOREIGN KEY (id_entrenador) REFERENCES deportivo.entrenadores(id_entrenador);

ALTER TABLE ONLY inventario.asignaciones
    ADD CONSTRAINT asignaciones_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES academico.estudiantes(id_estudiante);

ALTER TABLE ONLY inventario.asignaciones
    ADD CONSTRAINT asignaciones_registrado_por_id_usuario_fkey FOREIGN KEY (registrado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY inventario.movimientos_stock
    ADD CONSTRAINT movimientos_stock_id_articulo_fkey FOREIGN KEY (id_articulo) REFERENCES inventario.articulos(id_articulo);

ALTER TABLE ONLY inventario.movimientos_stock
    ADD CONSTRAINT movimientos_stock_registrado_por_id_usuario_fkey FOREIGN KEY (registrado_por_id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY seguridad.auditoria
    ADD CONSTRAINT auditoria_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES seguridad.usuarios(id_usuario) ON DELETE SET NULL;

ALTER TABLE ONLY seguridad.usuario_rol
    ADD CONSTRAINT usuario_rol_id_rol_fkey FOREIGN KEY (id_rol) REFERENCES seguridad.roles(id_rol);

ALTER TABLE ONLY seguridad.usuario_rol
    ADD CONSTRAINT usuario_rol_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES seguridad.usuarios(id_usuario);

ALTER TABLE ONLY seguridad.usuarios
    ADD CONSTRAINT usuarios_id_estado_general_fkey FOREIGN KEY (id_estado_general) REFERENCES seguridad.estados_general(id_estado_general);

ALTER TABLE ONLY seguridad.usuarios
    ADD CONSTRAINT usuarios_id_persona_fkey FOREIGN KEY (id_persona) REFERENCES seguridad.personas(id_persona);

