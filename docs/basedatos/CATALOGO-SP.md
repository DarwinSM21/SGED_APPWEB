# Catálogo de procedimientos almacenados (Bloque A.2)

Estrategia híbrida de acceso a datos: CRUD elementales en ORM (Spring Data
JPA); agregados, reportes y operaciones masivas en procedimientos almacenados
SQL versionados en `db/procs/`, invocados con `@Procedure` (JPA 2.1).
Parámetros siempre nombrados; prohibido el SQL dinámico (auditado por
`scripts/audit-sql-dynamic.sh`).

Nota: están implementados como `PROCEDURE` (no `FUNCTION`). Spring Data JPA
invoca `@Procedure` desde Java vía `CallableStatement` con sintaxis
`{call ...}`, y PostgreSQL solo acepta `CALL` contra procedimientos reales;
contra una función (aunque declare un parámetro `OUT`) responde
`... is not a procedure. Hint: To call a function, use SELECT.`

| Nombre | Tipo | Propósito | Entrada | Salida | Tablas | Invocación |
|---|---|---|---|---|---|---|
| `academico.sp_contar_estudiantes_activos` | Procedimiento | Conteo de estudiantes activos por categoría (agregado COUNT) | `p_categoria INT` | `total BIGINT` (OUT) | `academico.estudiantes` | `EstudianteRepository.contarEstudiantesActivosPorCategoria` (`@Procedure`) |
| `academico.sp_desactivar_estudiantes_categoria` | Procedimiento | Baja lógica masiva de una categoría completa (UPDATE multi-fila) | `p_categoria INT` | `afectados INTEGER` (OUT, filas afectadas) | `academico.estudiantes` | `EstudianteRepository.desactivarEstudiantesPorCategoria` (`@Procedure`) |

Los archivos fuente viven en `db/procs/` y se instalan en dos vías
equivalentes: migraciones Flyway `V5__procedimientos_almacenados.sql` +
`V6__convertir_a_procedimientos_almacenados.sql` (desarrollo local) y
`db/schema.sql` montado en `/docker-entrypoint-initdb.d/` (contenedores,
Bloque B.1).
