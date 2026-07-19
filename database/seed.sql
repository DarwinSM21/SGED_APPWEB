-- ==============================================================
-- SGED - Datos semilla reproducibles (Entrega 3, Bloque B.1)
-- Usuario administrador documentado en el README:
--   username: admin  |  password: Admin2026!
-- Hash BCrypt costo 12 generado de forma determinista para el seed.
-- ==============================================================

INSERT INTO seguridad.roles (nombre, descripcion)
VALUES ('ADMINISTRADOR', 'Administrador del sistema'),
       ('ENTRENADOR', 'Entrenador de la escuela'),
       ('USER', 'Usuario estandar')
ON CONFLICT DO NOTHING;

INSERT INTO seguridad.personas (nombre, apellido, activo)
VALUES ('Admin', 'SGED', TRUE);

INSERT INTO seguridad.usuarios (id_persona, username, password_hash, activo)
SELECT p.id_persona, 'admin',
       '$2b$12$Wlcf50ZskxGsDz2Ujrp7mObJ6pxLAClSZFSnOgYde5EJux8bvmDc2',
       TRUE
FROM seguridad.personas p
WHERE p.nombre = 'Admin' AND p.apellido = 'SGED'
LIMIT 1;

INSERT INTO seguridad.usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM seguridad.usuarios u, seguridad.roles r
WHERE u.username = 'admin' AND r.nombre = 'ADMINISTRADOR';

-- Estudiantes de ejemplo para que el listado y el cache tengan datos
INSERT INTO seguridad.personas (nombre, apellido, activo)
VALUES ('Juan', 'Perez', TRUE), ('Maria', 'Lopez', TRUE), ('Carlos', 'Mora', TRUE);

INSERT INTO seguridad.estudiantes (id_persona, categoria, activo)
SELECT p.id_persona, 'SUB-12', TRUE
FROM seguridad.personas p
WHERE p.nombre IN ('Juan', 'Maria', 'Carlos');
