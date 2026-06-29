CREATE SCHEMA IF NOT EXISTS seguridad;

CREATE TABLE IF NOT EXISTS seguridad.estados_general (
    id_estado_general BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS seguridad.personas (
    id_persona BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    cedula VARCHAR(10),
    correo VARCHAR(255),
    telefono VARCHAR(10),
    fecha_nacimiento DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS seguridad.roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS seguridad.usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    id_persona BIGINT REFERENCES seguridad.personas(id_persona),
    id_estado_general BIGINT REFERENCES seguridad.estados_general(id_estado_general),
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_usuarios_username ON seguridad.usuarios(username);

ALTER TABLE seguridad.usuarios
ADD CONSTRAINT chk_activo CHECK (activo IN (TRUE, FALSE));

CREATE TABLE IF NOT EXISTS seguridad.usuario_rol (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES seguridad.usuarios(id_usuario),
    id_rol BIGINT NOT NULL REFERENCES seguridad.roles(id_rol)
);

CREATE OR REPLACE FUNCTION seguridad.set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_actualizado_en
BEFORE UPDATE ON seguridad.usuarios
FOR EACH ROW EXECUTE FUNCTION seguridad.set_actualizado_en();