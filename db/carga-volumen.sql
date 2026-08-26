-- ============================================================
-- Carga de volumen: un millon de asistencias para medir como responde el
-- sistema con varios anios de operacion encima.
--
-- NO es la base de la demostracion. Son dos cosas distintas y no deben
-- mezclarse:
--
--   * db/seed.sql        el plantel real de la escuela, con el que se muestra
--                        el sistema. Tamano de una academia de verdad.
--   * db/carga-volumen   este archivo. Sirve para sostener "aguanta volumen",
--                        no para ensenar como se ve el sistema: 1.000 chicos
--                        en SUB-12 no existen en ninguna academia, y quien
--                        vea esa pantalla va a pensar que el dato es falso
--                        -porque lo es-.
--
-- Se quita con db/limpiar-carga.sql, que no toca nada del plantel real.
--
-- ------------------------------------------------------------
-- Los datos son INVENTADOS a proposito.
--
-- Usar datos de una academia real significaria meter nombres, cedulas y
-- fechas de nacimiento de menores sin su consentimiento, que es exactamente
-- el hallazgo H-04 de docs/etica/ETHICS.md -y H-07 aclara que la plantilla
-- de consentimiento que existe cubre a los adultos del SUS, no a los
-- representantes de los menores-. Los nombres salen de listas frecuentes en
-- Ecuador para que las pantallas se vean como se verian de verdad -nombres
-- largos, apellidos compuestos- sin exponer a nadie.
-- ============================================================

\timing on
\set ON_ERROR_STOP on

-- ---------------------------------------------------------------- 1) personas
--
-- El nombre se toma indexando el arreglo por el id, NO con
-- "(SELECT ... ORDER BY md5(i) LIMIT 1)" como hacia la primera version: esa
-- subconsulta correlacionada Postgres la evalua UNA vez, asi que los 3.000
-- salian casi todos con el mismo nombre. Con 574 jugadores llamados igual no
-- se puede probar un buscador, que es justamente uno de los defectos que esta
-- carga tenia que sacar a la luz.

CREATE TEMP TABLE catalogo_nombres AS
SELECT ARRAY['Mateo','Sebastian','Nicolas','Emilio','Thiago','Dylan','Alexis',
             'Kevin','Bryan','Josue','Ariel','Damian','Ismael','Joaquin',
             'Camila','Valentina','Emilia','Antonella','Domenica','Micaela',
             'Sofia','Isabella','Renata','Julieta','Martina','Amelia'] AS pila,
       ARRAY['Zambrano','Cedeno','Moreira','Bermudez','Andrade','Piguave',
             'Villacres','Quinonez','Loor','Mendoza','Solorzano','Vera',
             'Macias','Intriago','Chavez','Alcivar','Delgado','Ponce',
             'Rivas','Tumbaco','Ubilla','Yepez','Aguirre','Chuquimarca',
             'Bajana','Carrera','Zurita','Salazar','Jimenez','Rios'] AS apellidos;

INSERT INTO seguridad.personas
    (nombre, apellido, cedula, correo, telefono, fecha_nacimiento, activo, created_at, updated_at)
SELECT
    c.pila[1 + (i % array_length(c.pila, 1))],
    c.apellidos[1 + ((i * 7) % array_length(c.apellidos, 1))],
    -- cedulas sinteticas fuera del rango real de provincias (empiezan en 90)
    '90' || lpad(i::text, 8, '0'),
    'sintetico' || i || '@carga.test',
    '09' || lpad((i % 100000000)::text, 8, '0'),
    -- edades entre 10 y 18 repartidas
    (DATE '2008-01-01' + ((i * 37) % 3200) * INTERVAL '1 day')::date,
    true, NOW(), NOW()
FROM generate_series(1, 3000) i, catalogo_nombres c;

-- --------------------------------------------------------------- 2) estudiantes

INSERT INTO academico.estudiantes
    (id_persona, id_categoria, id_estado_general, codigo_estudiante,
     fecha_ingreso, peso, altura, activo, created_at, updated_at)
SELECT
    p.id_persona,
    (SELECT c.id_categoria FROM deportivo.categorias c
      WHERE EXTRACT(YEAR FROM AGE(p.fecha_nacimiento)) BETWEEN c.edad_min AND c.edad_max
      ORDER BY c.id_categoria LIMIT 1),
    1,
    'CARGA-' || p.id_persona,
    DATE '2023-02-01' + ((p.id_persona * 13) % 700) * INTERVAL '1 day',
    35 + (p.id_persona % 40),
    1.30 + ((p.id_persona % 45) / 100.0),
    true, NOW(), NOW()
FROM seguridad.personas p
WHERE p.correo LIKE '%@carga.test'
  AND NOT EXISTS (SELECT 1 FROM academico.estudiantes e WHERE e.id_persona = p.id_persona)
  AND EXISTS (SELECT 1 FROM deportivo.categorias c
               WHERE EXTRACT(YEAR FROM AGE(p.fecha_nacimiento)) BETWEEN c.edad_min AND c.edad_max);

-- ------------------------------------------------------------------ 3) sesiones
-- Tres anios de entrenamientos: cada categoria, de lunes a viernes.

INSERT INTO deportivo.sesiones_entrenamiento
    (id_categoria, id_entrenador, fecha, hora_inicio, hora_fin, campo, creado_en, actualizado_en)
SELECT
    c.id_categoria,
    (SELECT id_entrenador FROM deportivo.entrenadores WHERE activo ORDER BY id_entrenador LIMIT 1),
    d::date,
    TIME '16:00', TIME '18:00', 'Cancha principal', NOW(), NOW()
FROM deportivo.categorias c
CROSS JOIN generate_series(DATE '2023-08-01', CURRENT_DATE - 1, INTERVAL '1 day') d
WHERE c.activo
  AND EXTRACT(ISODOW FROM d) <= 5
  AND NOT EXISTS (SELECT 1 FROM deportivo.sesiones_entrenamiento s
                   WHERE s.id_categoria = c.id_categoria AND s.fecha = d::date);

-- --------------------------------------------------------------- 4) asistencias
--
-- Aqui esta el volumen, y aqui esta lo que la primera version hacia mal.
--
-- Antes el estado salia de "(id_estudiante * 7 + id_sesion) % 10", que reparte
-- exactamente 70/10/10/10 para TODOS por igual: todos los chicos asistian lo
-- mismo. Eso deja sin nada que mostrar al panel de alertas -si nadie destaca
-- por faltar, no hay a quien senalar- y hace que la convocatoria del partido
-- desempate siempre por id, porque las presencias empatan.
--
-- Ahora cada estudiante tiene su PROPIA tendencia, que es como se comporta un
-- plantel real: la mayoria viene casi siempre, unos pocos vienen a medias, y
-- un grupo chico es el que preocupa -y es el que tiene que aparecer en
-- "Requieren atencion"-.

INSERT INTO deportivo.asistencias
    (id_sesion, id_estudiante, hora_entrada, metodo, estado, creado_en, actualizado_en)
SELECT
    s.id_sesion,
    e.id_estudiante,
    -- La hora solo existe si la midio el QR. Vacia significa "lo marco el
    -- entrenador a mano": el afirma que el chico estuvo, no a que hora entro.
    CASE WHEN sorteo.v < umbral.v AND (e.id_estudiante + s.id_sesion) % 4 = 0
         THEN TIME '16:00' + ((sorteo.v % 14) * INTERVAL '1 minute')
         ELSE NULL END,
    CASE WHEN (e.id_estudiante + s.id_sesion) % 4 = 0 THEN 'QR' ELSE 'MANUAL' END,
    CASE
      WHEN sorteo.v < umbral.v - 8 THEN 'PRESENTE'
      WHEN sorteo.v < umbral.v     THEN 'TARDE'
      WHEN sorteo.v < umbral.v + 5 THEN 'JUSTIFICADO'
      ELSE 'AUSENTE'
    END,
    NOW(), NOW()
FROM deportivo.sesiones_entrenamiento s
JOIN academico.estudiantes e ON e.id_categoria = s.id_categoria AND e.activo
-- Tendencia personal, estable en el tiempo: define que tan seguido viene.
CROSS JOIN LATERAL (SELECT CASE
        WHEN e.id_estudiante % 100 < 70 THEN 93   -- regulares
        WHEN e.id_estudiante % 100 < 90 THEN 75   -- irregulares
        ELSE 48                                    -- los que preocupan
    END AS v) AS umbral
-- Sorteo del dia: mismo estudiante, distinta sesion, distinto resultado.
CROSS JOIN LATERAL (SELECT (('x' || substr(md5(e.id_estudiante::text || '-' || s.id_sesion::text), 1, 8))
        ::bit(32)::bigint & 2147483647) % 100 AS v) AS sorteo
WHERE s.fecha < CURRENT_DATE
  AND e.codigo_estudiante LIKE 'CARGA-%'
  AND NOT EXISTS (SELECT 1 FROM deportivo.asistencias a
                   WHERE a.id_sesion = s.id_sesion AND a.id_estudiante = e.id_estudiante)
-- El ORDER BY no es cosmetico: sin el, el LIMIT cortaba en un punto
-- arbitrario y ~1.700 chicos quedaban SIN NINGUNA fila de asistencia. El
-- sistema los leia como 0 % y los mandaba a "Requieren atencion", asi que el
-- panel se llenaba de un artefacto de la carga en vez de mostrar a quien de
-- verdad falta. Llenando de la sesion mas nueva hacia atras, la ventana
-- reciente -que es la que miran las alertas y la convocatoria- queda completa
-- para todos, y lo que se pierde es historia vieja, que no le importa a nadie.
ORDER BY s.fecha DESC, e.id_estudiante
LIMIT 1000000;

ANALYZE seguridad.personas;
ANALYZE academico.estudiantes;
ANALYZE deportivo.sesiones_entrenamiento;
ANALYZE deportivo.asistencias;

SELECT 'personas    ' || to_char(COUNT(*), 'FM999G999G999') FROM seguridad.personas
UNION ALL SELECT 'estudiantes ' || to_char(COUNT(*), 'FM999G999G999') FROM academico.estudiantes
UNION ALL SELECT 'sesiones    ' || to_char(COUNT(*), 'FM999G999G999') FROM deportivo.sesiones_entrenamiento
UNION ALL SELECT 'asistencias ' || to_char(COUNT(*), 'FM999G999G999') FROM deportivo.asistencias;

-- Reparto real de la asistencia sintetica: tiene que variar entre chicos, no
-- ser el mismo 70/10/10/10 para todos.
SELECT estado || ': ' || COUNT(*) || ' (' ||
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 1) || '%)'
  FROM deportivo.asistencias GROUP BY estado ORDER BY 1;
