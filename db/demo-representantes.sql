-- ============================================================
-- Representantes de demostracion.
--
-- El sistema tenia dos, y con dos no se ve nada de lo que el modulo hace:
-- ni un tutor con varios hijos, ni un hijo con padre y madre, ni un vinculo
-- desactivado. Esto crea ocho cuentas que cubren esos casos.
--
-- Los datos son inventados, igual que el resto del dato sintetico: son
-- adultos, no menores, pero el criterio es el mismo -no se usan personas
-- reales- y ademas estos son representantes DE menores, asi que ligarlos a
-- una familia real seria identificarla.
--
-- Todas entran con la contrasena de demostracion del proyecto. El hash se
-- copia del usuario 'representante' que ya existe: es la misma contrasena,
-- asi que el mismo hash sirve.
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- ------------------------------------------------------------ 1) personas
CREATE TEMP TABLE nuevos_representantes (
    orden        int,
    nombre       text,
    apellido     text,
    parentesco   text,
    usuario      text,
    -- Estudiantes a cargo. Un tutor puede tener mas de uno.
    hijos        bigint[]
);

INSERT INTO nuevos_representantes VALUES
    (1, 'Marisol',  'Piguave',    'Madre',  'marisol.piguave',  ARRAY[4609]),
    -- Padre y madre del mismo chico: dos cuentas, un solo estudiante.
    (2, 'Ramon',    'Cedeno',     'Padre',  'ramon.cedeno',     ARRAY[4612]),
    (3, 'Yolanda',  'Cedeno',     'Madre',  'yolanda.cedeno',   ARRAY[4612]),
    -- Un tutor con tres representados: el caso que la pantalla tiene que
    -- resolver con un selector y no con una lista suelta.
    (4, 'Gladys',   'Moreira',    'Abuela', 'gladys.moreira',   ARRAY[4613, 4611, 4614]),
    (5, 'Wilfrido', 'Quinonez',   'Padre',  'wilfrido.quinonez', ARRAY[4617]),
    (6, 'Narcisa',  'Loor',       'Madre',  'narcisa.loor',     ARRAY[4618, 4619]),
    (7, 'Segundo',  'Solorzano',  'Tio',    'segundo.solorzano', ARRAY[4620]),
    (8, 'Mercedes', 'Vera',       'Madre',  'mercedes.vera',    ARRAY[4621, 4622, 4623]);

INSERT INTO seguridad.personas
    (nombre, apellido, cedula, correo, telefono, fecha_nacimiento, activo, created_at, updated_at)
SELECT n.nombre, n.apellido,
       '12' || lpad((900 + n.orden)::text, 8, '0'),
       n.usuario || '@sged.test',
       '09' || lpad((87000000 + n.orden)::text, 8, '0'),
       -- Adultos, entre 35 y 60 anios
       (DATE '1975-01-01' + (n.orden * 137) * INTERVAL '1 day')::date,
       true, NOW(), NOW()
FROM nuevos_representantes n
WHERE NOT EXISTS (SELECT 1 FROM seguridad.personas p WHERE p.correo = n.usuario || '@sged.test');

-- ------------------------------------------------------------ 2) usuarios
INSERT INTO seguridad.usuarios
    (id_persona, id_estado_general, username, password_hash, created_at, updated_at)
SELECT p.id_persona, 1, n.usuario,
       (SELECT password_hash FROM seguridad.usuarios WHERE username = 'representante'),
       NOW(), NOW()
FROM nuevos_representantes n
JOIN seguridad.personas p ON p.correo = n.usuario || '@sged.test'
WHERE NOT EXISTS (SELECT 1 FROM seguridad.usuarios u WHERE u.username = n.usuario);

INSERT INTO seguridad.usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, (SELECT id_rol FROM seguridad.roles WHERE nombre = 'REPRESENTANTE')
FROM nuevos_representantes n
JOIN seguridad.usuarios u ON u.username = n.usuario
WHERE NOT EXISTS (SELECT 1 FROM seguridad.usuario_rol ur WHERE ur.id_usuario = u.id_usuario);

-- ------------------------------------------------------- 3) representantes
INSERT INTO academico.representantes
    (id_persona, id_usuario, parentesco, telefono_contacto, activo, created_at, updated_at)
SELECT p.id_persona, u.id_usuario, n.parentesco, p.telefono, true, NOW(), NOW()
FROM nuevos_representantes n
JOIN seguridad.usuarios u ON u.username = n.usuario
JOIN seguridad.personas p ON p.id_persona = u.id_persona
WHERE NOT EXISTS (SELECT 1 FROM academico.representantes r WHERE r.id_usuario = u.id_usuario);

-- --------------------------------------------------------------- 4) vinculos
INSERT INTO academico.representante_estudiante
    (id_representante, id_estudiante, activo, created_at, updated_at)
SELECT r.id_representante, hijo, true, NOW(), NOW()
FROM nuevos_representantes n
JOIN seguridad.usuarios u ON u.username = n.usuario
JOIN academico.representantes r ON r.id_usuario = u.id_usuario
CROSS JOIN LATERAL unnest(n.hijos) AS hijo
WHERE EXISTS (SELECT 1 FROM academico.estudiantes e WHERE e.id_estudiante = hijo AND e.activo)
  AND NOT EXISTS (SELECT 1 FROM academico.representante_estudiante re
                   WHERE re.id_representante = r.id_representante AND re.id_estudiante = hijo);

-- Un vinculo DESACTIVADO, para poder comprobar que un tutor al que se le
-- corto el acceso a un representado deja de verlo -y sigue viendo a los
-- otros-. Es el caso de una custodia revocada, y sin un dato asi no hay
-- forma de verificar que la regla funciona.
UPDATE academico.representante_estudiante re
   SET activo = false, updated_at = NOW()
  FROM academico.representantes r
  JOIN seguridad.usuarios u ON u.id_usuario = r.id_usuario
 WHERE re.id_representante = r.id_representante
   AND u.username = 'mercedes.vera'
   AND re.id_estudiante = 4623;

COMMIT;

SELECT u.username || ' (' || r.parentesco || ') -> ' ||
       string_agg(p.nombre || ' ' || p.apellido || CASE WHEN re.activo THEN '' ELSE ' [SIN ACCESO]' END, ', ')
  FROM academico.representantes r
  JOIN seguridad.usuarios u ON u.id_usuario = r.id_usuario
  JOIN academico.representante_estudiante re ON re.id_representante = r.id_representante
  JOIN academico.estudiantes e ON e.id_estudiante = re.id_estudiante
  JOIN seguridad.personas p ON p.id_persona = e.id_persona
 GROUP BY u.username, r.parentesco
 ORDER BY u.username;
