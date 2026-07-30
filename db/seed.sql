-- ==============================================================
-- SGED - Datos semilla reproducibles (Entrega 3, Bloque B.1)
-- Usuario administrador documentado en el README:
--   username: admin  |  password: Admin2026!
-- Hash BCrypt costo 12 generado de forma determinista para el seed.
-- ==============================================================

-- Estado general para seed
INSERT INTO seguridad.estados_general (id_estado_general, nombre)
VALUES (1, 'Activo'),
       (2, 'Inactivo')
ON CONFLICT (id_estado_general) DO NOTHING;

INSERT INTO seguridad.roles (nombre, descripcion)
VALUES ('ADMINISTRADOR', 'Administrador del sistema'),
       ('ENTRENADOR', 'Entrenador de la escuela'),
       ('USER', 'Usuario estandar')
ON CONFLICT DO NOTHING;

INSERT INTO seguridad.personas (nombre, apellido, cedula, correo, fecha_nacimiento, activo)
VALUES ('Admin', 'SGED', '0000000000', 'admin@sged.com', '2000-01-01', TRUE);

INSERT INTO seguridad.usuarios (id_persona, id_estado_general, username, password_hash, activo)
SELECT p.id_persona, 1, 'admin',
       '$2b$12$Wlcf50ZskxGsDz2Ujrp7mObJ6pxLAClSZFSnOgYde5EJux8bvmDc2',
       TRUE
FROM seguridad.personas p
WHERE p.nombre = 'Admin' AND p.apellido = 'SGED'
LIMIT 1;

INSERT INTO seguridad.usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM seguridad.usuarios u, seguridad.roles r
WHERE u.username = 'admin' AND r.nombre = 'ADMINISTRADOR';

-- Categoría de ejemplo
INSERT INTO deportivo.categorias (nombre, edad_min, edad_max, descripcion)
VALUES ('SUB-12', 10, 12, 'Categoría sub-12'),
       ('SUB-14', 12, 14, 'Categoría sub-14'),
       ('SUB-16', 14, 16, 'Categoría sub-16')
ON CONFLICT DO NOTHING;

-- Estudiantes de ejemplo para que el listado y el cache tengan datos
INSERT INTO seguridad.personas (nombre, apellido, cedula, correo, fecha_nacimiento, activo)
VALUES ('Juan', 'Perez', '1111111111', 'juan@sged.com', '2012-05-10', TRUE),
       ('Maria', 'Lopez', '2222222222', 'maria@sged.com', '2013-08-15', TRUE),
       ('Carlos', 'Mora', '3333333333', 'carlos@sged.com', '2011-12-20', TRUE)
ON CONFLICT (cedula) DO NOTHING;

INSERT INTO academico.estudiantes (id_persona, id_categoria, id_estado_general, codigo_estudiante, fecha_ingreso, activo)
SELECT p.id_persona, c.id_categoria, 1,
       'EST-' || p.id_persona, '2024-01-15', TRUE
FROM seguridad.personas p
CROSS JOIN deportivo.categorias c
WHERE p.nombre IN ('Juan', 'Maria', 'Carlos')
  AND c.nombre = 'SUB-12';