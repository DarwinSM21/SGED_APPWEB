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

| Nombre | Tipo | Categoría funcional | Propósito | Entrada | Salida | Tablas | Invocación |
|---|---|---|---|---|---|---|---|
| `academico.sp_contar_estudiantes_activos` | Procedimiento | Cálculo agregado | Conteo de estudiantes activos por categoría (agregado COUNT) | `p_categoria INT` | `total BIGINT` (OUT) | `academico.estudiantes` | `EstudianteRepository.contarEstudiantesActivosPorCategoria` (`@Procedure`) |
| `academico.sp_desactivar_estudiantes_categoria` | Procedimiento | Actualización masiva | Baja lógica masiva de una categoría completa (UPDATE multi-fila) | `p_categoria INT` | `afectados INTEGER` (OUT, filas afectadas) | `academico.estudiantes` | `EstudianteRepository.desactivarEstudiantesPorCategoria` (`@Procedure`) |
| `deportivo.sp_validar_categoria_estudiante_sesion` | Procedimiento | Validación cruzada | Un estudiante solo puede marcar asistencia en una sesión de su propia categoría | `p_estudiante BIGINT`, `p_sesion BIGINT` | `coincide BOOLEAN` (OUT) | `academico.estudiantes`, `deportivo.sesiones_entrenamiento` | `AsistenciaRepository.validarCategoriaCoincide` (`@Procedure`), llamado desde `AsistenciaService.marcarPorQr` antes de registrar |
| `academico.sp_generar_codigo_estudiante` | Procedimiento | Generación de código secuencial | Propone el siguiente `codigo_estudiante` consecutivo de un año (`EST-<año>-0000`) | `p_anio INT` | `codigo VARCHAR` (OUT) | `academico.estudiantes` | `EstudianteRepository.generarSiguienteCodigo` (`@Procedure`), expuesto en `GET /api/estudiantes/operaciones/siguiente-codigo` |
| `deportivo.sp_reporte_asistencia_estudiante` | Procedimiento | Reporte | Porcentaje de asistencia de un estudiante en un rango de fechas, sobre el total de sesiones programadas de su categoría | `p_estudiante BIGINT`, `p_desde DATE`, `p_hasta DATE` | `porcentaje_asistencia NUMERIC` (OUT) | `academico.estudiantes`, `deportivo.sesiones_entrenamiento`, `deportivo.asistencias` | `AsistenciaRepository.calcularPorcentajeAsistencia` (`@Procedure`), incluido en el informe del representante (últimos 30 días) |
| `academico.sp_contacto_representante_estudiante` | Procedimiento | Consulta multi-tabla | Nombre y teléfono del representante activo de un estudiante, para contacto de emergencia | `p_estudiante BIGINT` | `contacto_info VARCHAR` (OUT) | `academico.representante_estudiante`, `academico.representantes`, `seguridad.personas` | `RepresentanteEstudianteRepository.contactoDe` (`@Procedure`), expuesto en `GET /api/estudiantes/{id}/contacto-emergencia` |
| `inventario.sp_reporte_stock_bajo` | Procedimiento | Reporte / cálculo agregado | Conteo de artículos activos con `stock_actual <= stock_minimo` (el listado detallado se resuelve en `ArticuloRepository`, vía JPA) | ninguna | `total_bajo_stock BIGINT` (OUT) | `inventario.articulos` | `ArticuloRepository.contarStockBajo` (`@Procedure`), expuesto en `GET /api/inventario/articulos/stock-bajo` |

Los archivos fuente viven en `db/procs/` y se instalan en dos vías
equivalentes: migraciones Flyway (`V5`, `V6`, `V11` y `V15` — desarrollo
local) y `db/schema.sql` montado en `/docker-entrypoint-initdb.d/`
(contenedores, Bloque B.1). Las seis categorías funcionales que exige la
Guía de la Entrega Final (Bloque A.2.1) están cubiertas: cálculo
agregado, actualización masiva, validación cruzada, generación de código
secuencial, reporte y consulta multi-tabla.
