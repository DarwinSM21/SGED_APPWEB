-- ============================================================
-- V16: Documenta en Flyway el esquema de equipos/partidos/ejercicios
-- que ya existia en la base de Supabase del proyecto, creado a mano
-- fuera de control de versiones (descubierto 2026-08-12 al reconciliar
-- el modulo Inventario). No tiene entidades JPA ni API REST propia
-- todavia -- son tablas vacias, sin datos -- asi que se documenta como
-- planificado (ver docs/requisitos/SRS.md), igual que el resto del
-- diseno original de ADR-003 que aun no se implemento.
--
-- Completa ademas la limpieza de 2026-07-30 que elimino el paquete Java
-- vacio EquipoController (ver docs/trazabilidad/matriz.csv, fila
-- "Equipo"): esa limpieza fue solo de codigo, esta migracion es el lado
-- de base de datos, sin modificarla -- se documenta tal cual esta, sin
-- agregar FKs que hoy no existen (ver notas abajo).
-- ============================================================

CREATE TABLE IF NOT EXISTS deportivo.ejercicios (
    id_ejercicio BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    duracion_min SMALLINT NOT NULL,
    nivel VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS deportivo.entrenamiento_ejercicios (
    id_entrenamiento_ejercicio BIGSERIAL PRIMARY KEY,
    id_sesion_entrenamiento BIGINT REFERENCES deportivo.sesiones_entrenamiento(id_sesion),
    id_ejercicio BIGINT REFERENCES deportivo.ejercicios(id_ejercicio),
    observaciones TEXT
);

CREATE TABLE IF NOT EXISTS deportivo.equipos (
    id_equipo BIGSERIAL PRIMARY KEY,
    id_categoria BIGINT REFERENCES deportivo.categorias(id_categoria),
    id_estado_general BIGINT REFERENCES seguridad.estados_general(id_estado_general),
    nombre_equipo VARCHAR(100) NOT NULL,
    siglas VARCHAR(10),
    logo_url TEXT,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- id_equipo_local / id_equipo_visitante SIN foreign key hacia
-- deportivo.equipos: asi esta la tabla real en Supabase, se documenta
-- tal cual -- no es un olvido de esta migracion.
CREATE TABLE IF NOT EXISTS deportivo.partidos (
    id_partido BIGSERIAL PRIMARY KEY,
    id_equipo_local BIGINT,
    id_equipo_visitante BIGINT,
    fecha_partido DATE NOT NULL,
    ubicacion VARCHAR(255),
    goles_local SMALLINT DEFAULT 0,
    goles_visitante SMALLINT DEFAULT 0
);

-- id_estudiante SIN foreign key hacia academico.estudiantes: mismo
-- caso, se documenta tal cual esta en Supabase.
CREATE TABLE IF NOT EXISTS deportivo.estadistica_partidos (
    id_estadistica_partido BIGSERIAL PRIMARY KEY,
    id_partido BIGINT REFERENCES deportivo.partidos(id_partido),
    id_estudiante BIGINT,
    goles SMALLINT DEFAULT 0,
    asistencias SMALLINT DEFAULT 0,
    tarjetas_amarillas SMALLINT DEFAULT 0,
    tarjetas_rojas SMALLINT DEFAULT 0,
    minutos_jugados SMALLINT DEFAULT 0
);
