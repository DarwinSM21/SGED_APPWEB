# Catálogo de procedimientos almacenados y funciones (Bloque A.2)

Estrategia híbrida de acceso a datos: CRUD elementales en ORM (Spring Data
JPA); agregados, reportes y operaciones masivas en funciones SQL versionadas
en `db/procs/`, invocadas con `@Procedure` (JPA 2.1). Parámetros siempre
nombrados; prohibido el SQL dinámico (auditado por `scripts/audit-sql-dynamic.sh`).

| Nombre | Tipo | Propósito | Entrada | Salida | Tablas | Invocación |
|---|---|---|---|---|---|---|
| `seguridad.fn_contar_estudiantes_activos` | Función | Conteo de estudiantes activos por categoría (agregado COUNT) | `p_categoria VARCHAR` | `BIGINT` | `seguridad.estudiantes` | `EstudianteRepository.contarActivosPorCategoria` (`@Procedure`) |
| `seguridad.fn_desactivar_estudiantes_categoria` | Función | Baja lógica masiva de una categoría completa (UPDATE multi-fila) | `p_categoria VARCHAR` | `INTEGER` (filas afectadas) | `seguridad.estudiantes` | `EstudianteRepository.desactivarPorCategoria` (`@Procedure`) |

Los archivos fuente viven en `db/procs/` y se instalan en dos vías
equivalentes: migración Flyway `V5__procedimientos_almacenados.sql`
(desarrollo local) y `db/schema.sql` montado en
`/docker-entrypoint-initdb.d/` (contenedores, Bloque B.1).
