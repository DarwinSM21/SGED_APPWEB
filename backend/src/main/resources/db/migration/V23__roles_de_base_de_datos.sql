-- ============================================================
-- V23: la aplicacion deja de conectarse como superusuario.
--
-- Hasta aqui el backend usaba 'postgres': superusuario, dueño de todo y con
-- permiso para borrar la base entera. Funcionaba, y por eso el problema pasa
-- desapercibido: no rompe nada hasta el dia que rompe todo. Una inyeccion SQL
-- en cualquier endpoint no daria acceso a leer datos -daria acceso a DROP
-- DATABASE-, y una equivocacion en una consola abierta tampoco tiene red.
--
-- El principio es el de menor privilegio: cada quien puede hacer exactamente
-- lo que su trabajo necesita y nada mas.
--
--   sged_app      lo que usa el backend. Lee y escribe datos, ejecuta los
--                 procedimientos. NO puede crear ni borrar tablas, ni tocar
--                 la estructura.
--   sged_lectura  solo SELECT. Para reportes, consultas de auditoria o
--                 conectar una herramienta externa sin riesgo de escribir.
--   postgres      queda para migrar el esquema y administrar. La aplicacion
--                 ya no lo usa.
--
-- Las contraseñas de este archivo son de DESARROLLO. En un despliegue real se
-- cambian con ALTER ROLE ... PASSWORD y no viven en el repositorio.
-- ============================================================

-- ------------------------------------------------------------------ roles
-- DO ... IF NOT EXISTS porque CREATE ROLE no admite IF NOT EXISTS y la
-- migracion tiene que poder correrse sobre una base que ya los tenga.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sged_app') THEN
        CREATE ROLE sged_app LOGIN PASSWORD 'sged_app_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sged_lectura') THEN
        CREATE ROLE sged_lectura LOGIN PASSWORD 'sged_lectura_dev';
    END IF;
END
$$;

-- Ninguno de los dos puede crear bases ni roles: eso es administracion, no
-- trabajo de la aplicacion.
ALTER ROLE sged_app     NOCREATEDB NOCREATEROLE NOSUPERUSER;
ALTER ROLE sged_lectura NOCREATEDB NOCREATEROLE NOSUPERUSER;

-- ------------------------------------------------------- entrar a la base
GRANT CONNECT ON DATABASE sged_db TO sged_app, sged_lectura;

-- USAGE deja ver el esquema y usar lo que hay dentro; NO deja crear objetos.
GRANT USAGE ON SCHEMA academico, deportivo, inventario, seguridad
    TO sged_app, sged_lectura;

-- El esquema public queda cerrado. Desde PostgreSQL 15 ya no es escribible
-- por defecto, pero se revoca explicitamente para que quede dicho.
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- ------------------------------------------------------------- sged_app
-- Los cuatro verbos de datos sobre las tablas que ya existen.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA academico, deportivo, inventario, seguridad
    TO sged_app;

-- Las secuencias van aparte: sin esto, un INSERT en una tabla con BIGSERIAL
-- falla con "permission denied for sequence", que es un error confuso de
-- diagnosticar porque la tabla si tenia permiso.
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA academico, deportivo, inventario, seguridad
    TO sged_app;

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA academico, deportivo, inventario, seguridad
    TO sged_app;
GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA academico, deportivo, inventario, seguridad
    TO sged_app;

-- --------------------------------------------------------- sged_lectura
GRANT SELECT ON ALL TABLES IN SCHEMA academico, deportivo, inventario, seguridad
    TO sged_lectura;

-- ------------------------------------------------- lo que se cree despues
-- Sin esto, la proxima migracion que agregue una tabla la dejaria invisible
-- para sged_app y la aplicacion empezaria a fallar despues de un despliegue
-- que "no toco nada". Los DEFAULT PRIVILEGES se aplican a lo que cree
-- postgres de aqui en adelante.

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
    IN SCHEMA academico, deportivo, inventario, seguridad
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sged_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
    IN SCHEMA academico, deportivo, inventario, seguridad
    GRANT USAGE, SELECT ON SEQUENCES TO sged_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
    IN SCHEMA academico, deportivo, inventario, seguridad
    GRANT EXECUTE ON FUNCTIONS TO sged_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
    IN SCHEMA academico, deportivo, inventario, seguridad
    GRANT SELECT ON TABLES TO sged_lectura;

-- ---------------------------------------------------------------- medicion
-- pg_stat_statements acumula el costo real de cada consulta. Es la
-- herramienta con la que se responde "que esta lento" con datos y no con
-- intuicion. Requiere estar en shared_preload_libraries (docker-compose).
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- pg_stat_statements guarda las consultas de TODA la instancia, incluidas las
-- de otros usuarios: leerla es un privilegio de administracion, no de la
-- aplicacion. Se concede solo al rol de lectura, que es el que se usa para
-- analizar.
GRANT SELECT ON pg_stat_statements TO sged_lectura;
