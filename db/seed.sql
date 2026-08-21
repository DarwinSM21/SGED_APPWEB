-- ==============================================================
-- SGED - Datos semilla reproducibles (Entrega 3, Bloque B.1)
-- Usuario administrador documentado en el README:
--   username: admin  |  password: sged2026
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
       ('RECEPCIONISTA', 'Encargado de emitir el codigo QR de asistencia'),
       ('REPRESENTANTE', 'Padre, madre o tutor legal de uno o mas estudiantes'),
       ('ESTUDIANTE', 'Deportista inscrito, con acceso propio para marcar su asistencia por QR')
ON CONFLICT DO NOTHING;

INSERT INTO seguridad.personas (nombre, apellido, cedula, correo, fecha_nacimiento, activo)
VALUES ('Admin', 'SGED', '0000000000', 'admin@sged.com', '2000-01-01', TRUE);

INSERT INTO seguridad.usuarios (id_persona, id_estado_general, username, password_hash, activo)
SELECT p.id_persona, 1, 'admin',
       '$2a$12$.4TLX5R.HfQup7R0oOFeKuCCo.jUwIvyx9.DsyI95dM6RQsHFUdXm',
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

-- ==============================================================
-- Cuentas de prueba por rol (una por integrante del equipo, mismas
-- para todos al clonar el repo). Ver README "Credenciales de prueba".
--
-- 5 cuentas "simples", una por rol, username = nombre del rol,
-- contraseña sged2026 (admin ya definido arriba usa la misma).
--
-- 13 cuentas "realistas": username = nombre+apellido+inicialnombre+
-- inicialapellido (ej. Juan Perez -> juanperezjp), correo
-- <username>@sged.com, contraseña <username>2026. Distribucion:
-- 5 entrenador, 5 representante, 5 estudiante, 2 recepcionista (cada
-- grupo incluye la cuenta simple correspondiente).
--
-- De las realistas, una por rol (entrenador/representante/estudiante)
-- tiene ficha completa en su dominio ademas del login; las demas solo
-- inician sesion. RECEPCIONISTA y ADMINISTRADOR no tienen tabla de
-- dominio propia en este esquema, por eso solo llevan persona+usuario.
--
-- Hashes BCrypt costo 12 extraidos de una corrida real de
-- /api/auth/registro contra este mismo seed (no inventados a mano).
-- ==============================================================

INSERT INTO seguridad.personas (nombre, apellido, cedula, correo, fecha_nacimiento, activo)
VALUES
    ('Recepcionista', 'Principal', '4000000001', 'recepcionista@sged.com', '1990-01-01', TRUE),
    ('Entrenador', 'Principal', '4000000002', 'entrenador@sged.com', '1990-01-01', TRUE),
    ('Representante', 'Principal', '4000000003', 'representante@sged.com', '1990-01-01', TRUE),
    ('Estudiante', 'Principal', '4000000004', 'estudiante@sged.com', '2012-01-01', TRUE),
    ('Ana', 'Torres', '4000000005', 'anatorresat@sged.com', '1988-03-12', TRUE),
    ('Luis', 'Vera', '4000000006', 'luisveralv@sged.com', '1985-06-20', TRUE),
    ('Pedro', 'Salazar', '4000000007', 'pedrosalazarps@sged.com', '1987-09-05', TRUE),
    ('Diego', 'Castillo', '4000000008', 'diegocastillodc@sged.com', '1990-11-15', TRUE),
    ('Marco', 'Jimenez', '4000000009', 'marcojimenezmj@sged.com', '1983-02-28', TRUE),
    ('Rosa', 'Chuquimarca', '4000000010', 'rosachuquimarcarc@sged.com', '1982-04-18', TRUE),
    ('Elena', 'Vargas', '4000000011', 'elenavargasev@sged.com', '1979-07-22', TRUE),
    ('Fernando', 'Rios', '4000000012', 'fernandoriosfr@sged.com', '1980-10-09', TRUE),
    ('Patricia', 'Gomez', '4000000013', 'patriciagomezpg@sged.com', '1984-12-30', TRUE),
    ('Kevin', 'Andrade', '4000000014', 'kevinandradeka@sged.com', '2011-05-14', TRUE),
    ('Sofia', 'Ramirez', '4000000015', 'sofiaramirezsr@sged.com', '2012-08-19', TRUE),
    ('Mateo', 'Villacres', '4000000016', 'mateovillacresmv@sged.com', '2010-03-27', TRUE),
    ('Valentina', 'Ortiz', '4000000017', 'valentinaortizvo@sged.com', '2013-01-08', TRUE)
ON CONFLICT (cedula) DO NOTHING;

INSERT INTO seguridad.usuarios (id_persona, id_estado_general, username, password_hash, activo)
SELECT p.id_persona, 1, v.username, v.password_hash, TRUE
FROM (VALUES
    ('4000000001', 'recepcionista', '$2a$12$91HwwpcAHFNg6/trpGfReOWQ7n4PlKgdIthQuB.lAsDKJs/P9OmV.'),
    ('4000000002', 'entrenador', '$2a$12$YsZxX9CZy21y.vBNtE0soeJ.QFtdIscEhnXvV/3eErcPJkKRmHih2'),
    ('4000000003', 'representante', '$2a$12$jW57zU.jncRB3ZiSIeihHulK51xjR3hTGr4x0IJWP53e8Dor4EKgS'),
    ('4000000004', 'estudiante', '$2a$12$roPl4xlfk0eSpQTBStm6feVeznexGwctcE.EQQC7JYzk/Kot8JKd.'),
    ('4000000005', 'anatorresat', '$2a$12$lbJcp33fJ4UmObrQB31fq.FR5o05QfqMVpmtVkXyR7CY0RB1lp9sG'),
    ('4000000006', 'luisveralv', '$2a$12$jiGA8uQBP25Gn9WorV5G.ufH8FZzpWIZncthjgYpckyGwDNAAFXPS'),
    ('4000000007', 'pedrosalazarps', '$2a$12$dzVgddSA/RXax6k6C6Zpo.niQAS2KYeBeZM8xErfBmJpcC7TyVp.G'),
    ('4000000008', 'diegocastillodc', '$2a$12$.54.d2BEI/7dPIwsaqdRCeOcUXh3NYmND6gQGH2M.E0PRSBPXZlkm'),
    ('4000000009', 'marcojimenezmj', '$2a$12$Jius3BfjPmUJUsZ81.iji.odPn6HV89AUrvNf809GzbBwIKtj3WAe'),
    ('4000000010', 'rosachuquimarcarc', '$2a$12$PFu27/jFl/BfPBFCa/pIreLZJYjfawtsC0vfw7G9nXNrlHn/7Rnk6'),
    ('4000000011', 'elenavargasev', '$2a$12$eC8HK3p2U5YhErjikbekO.8RYzPPOFyWjTeTrGPQSGaQHcxXKJs4S'),
    ('4000000012', 'fernandoriosfr', '$2a$12$j.65.7HYysXC/j8vzYv37ecbA.PPkcES7DtItvGYqRFBnVUcx4.xe'),
    ('4000000013', 'patriciagomezpg', '$2a$12$WwatK6rrZNw6OZla87uAWeuaa2kj9C6fS0NnIOnlnL.qQHK1ZMSDm'),
    ('4000000014', 'kevinandradeka', '$2a$12$.3pH89EDr0oewPlWXt091esYNim8oqhlnmwuLuuu1wFrE4pr7Uahy'),
    ('4000000015', 'sofiaramirezsr', '$2a$12$R8Li/PJAplJW3i0R7xf.Pey2gS5wIPp.TZx7HdAR2q1rqjnBZk5F2'),
    ('4000000016', 'mateovillacresmv', '$2a$12$BwOEI5A0OZQ7PYn0.dkgW.THKRJpm2ClxcLd6dkxqfA7WLXgNnQ2q'),
    ('4000000017', 'valentinaortizvo', '$2a$12$gynmPNF5FyMbWCKL8ND3U.vbALY8kioBJJkNwA8pPJ2dCN2AWzLoC')
) AS v(cedula, username, password_hash)
JOIN seguridad.personas p ON p.cedula = v.cedula
ON CONFLICT (username) DO NOTHING;

INSERT INTO seguridad.usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM (VALUES
    ('recepcionista', 'RECEPCIONISTA'),
    ('entrenador', 'ENTRENADOR'),
    ('representante', 'REPRESENTANTE'),
    ('estudiante', 'ESTUDIANTE'),
    ('anatorresat', 'RECEPCIONISTA'),
    ('luisveralv', 'ENTRENADOR'),
    ('pedrosalazarps', 'ENTRENADOR'),
    ('diegocastillodc', 'ENTRENADOR'),
    ('marcojimenezmj', 'ENTRENADOR'),
    ('rosachuquimarcarc', 'REPRESENTANTE'),
    ('elenavargasev', 'REPRESENTANTE'),
    ('fernandoriosfr', 'REPRESENTANTE'),
    ('patriciagomezpg', 'REPRESENTANTE'),
    ('kevinandradeka', 'ESTUDIANTE'),
    ('sofiaramirezsr', 'ESTUDIANTE'),
    ('mateovillacresmv', 'ESTUDIANTE'),
    ('valentinaortizvo', 'ESTUDIANTE')
) AS v(username, rol)
JOIN seguridad.usuarios u ON u.username = v.username
JOIN seguridad.roles r ON r.nombre = v.rol;

-- Fichas de dominio: la cuenta simple de cada rol con ficha, mas una
-- cuenta realista elegida por rol para entrenador y representante.
-- ESTUDIANTE lleva ficha en las 5 cuentas (simple + 4 realistas): sin
-- fila en academico.estudiantes, /api/asistencias/qr/marcar rechaza al
-- estudiante con "No hay un estudiante asociado a esta cuenta" y no
-- puede marcar asistencia por QR, que es el uso principal del rol.
INSERT INTO deportivo.entrenadores (id_persona, id_usuario, experiencia_anios, certificacion)
SELECT p.id_persona, u.id_usuario, v.experiencia, v.certificacion
FROM (VALUES
    ('4000000002', 5, 'Licencia UEFA B (demo)'),
    ('4000000006', 8, 'Licencia UEFA A (demo)')
) AS v(cedula, experiencia, certificacion)
JOIN seguridad.personas p ON p.cedula = v.cedula
JOIN seguridad.usuarios u ON u.id_persona = p.id_persona
ON CONFLICT (id_persona) DO NOTHING;

-- id_usuario va aparte de id_persona a proposito (ver ALTER TABLE mas abajo
-- en este mismo archivo, columna aditiva de la reestructuracion): sin este
-- JOIN la ficha queda creada pero sin cuenta vinculada, y
-- /api/estudiante/mi-equipo, mi-informe, mi-asistencia y el QR de
-- asistencia rechazan al estudiante con "No hay un estudiante asociado a
-- esta cuenta" aunque el login funcione perfecto.
INSERT INTO academico.estudiantes (id_persona, id_usuario, id_categoria, id_estado_general, codigo_estudiante, fecha_ingreso)
SELECT p.id_persona, u.id_usuario, c.id_categoria, 1, v.codigo, '2026-01-15'
FROM (VALUES
    ('4000000004', 'SUB-14', 'EST-008'),
    ('4000000014', 'SUB-16', 'EST-018'),
    ('4000000015', 'SUB-14', 'EST-019'),
    ('4000000016', 'SUB-16', 'EST-020'),
    ('4000000017', 'SUB-14', 'EST-021')
) AS v(cedula, categoria, codigo)
JOIN seguridad.personas p ON p.cedula = v.cedula
JOIN seguridad.usuarios u ON u.id_persona = p.id_persona
JOIN deportivo.categorias c ON c.nombre = v.categoria
ON CONFLICT (codigo_estudiante) DO NOTHING;

INSERT INTO academico.representantes (id_persona, id_usuario, parentesco, telefono_contacto)
SELECT p.id_persona, u.id_usuario, v.parentesco, v.telefono
FROM (VALUES
    ('4000000003', 'Padre/Madre (demo)', '0990000000'),
    ('4000000010', 'Madre', '0991234567')
) AS v(cedula, parentesco, telefono)
JOIN seguridad.personas p ON p.cedula = v.cedula
JOIN seguridad.usuarios u ON u.id_persona = p.id_persona
ON CONFLICT (id_persona) DO NOTHING;

INSERT INTO academico.representante_estudiante (id_representante, id_estudiante)
SELECT rep.id_representante, est.id_estudiante
FROM (VALUES
    ('4000000003', 'EST-008'),
    ('4000000010', 'EST-018')
) AS v(cedula_representante, codigo_estudiante)
JOIN seguridad.personas p ON p.cedula = v.cedula_representante
JOIN academico.representantes rep ON rep.id_persona = p.id_persona
JOIN academico.estudiantes est ON est.codigo_estudiante = v.codigo_estudiante
ON CONFLICT (id_representante, id_estudiante) DO NOTHING;