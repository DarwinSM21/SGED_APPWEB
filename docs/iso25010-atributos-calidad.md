# Atributos de calidad — ISO/IEC 25010

Escenarios de calidad (estímulo → respuesta) para SGED, priorizados y
vinculados a la estrategia arquitectónica adoptada y a la evidencia que ya
producen los scripts del repositorio (`scripts/`, `docs/mediciones/`).

Prioridad: **A**lta (crítico para la entrega/negocio) · **M**edia · **B**aja.

| # | Característica (ISO 25010) | Subcaracterística | Prioridad | Escenario (estímulo → respuesta) | Estrategia / táctica arquitectónica | Evidencia / métrica |
|---|---|---|:---:|---|---|---|
| 1 | Seguridad | Control de acceso (Confidencialidad) | A | Un usuario con rol `USER` invoca `POST /api/estudiantes/operaciones/desactivar-categoria` → el sistema responde `403 Forbidden` sin ejecutar la baja masiva. | `@PreAuthorize` por rol en cada endpoint (RBAC declarativo), `SecurityFilterChain` con `anyRequest().authenticated()` por defecto (fail-closed). | `scripts/audit-owasp.sh` (control A01) → `docs/mediciones/sec/` |
| 2 | Seguridad | Autenticidad / Integridad de sesión | A | Un atacante intenta reutilizar un JWT después de `logout` → el sistema responde `401 Unauthorized`. | Cookie JWT `HttpOnly + Secure + SameSite=Strict` (no accesible por JS/XSS) + lista negra de tokens en `RedisBlacklistService`, verificada en `JwtAuthenticationFilter`. | `RedisBlacklistService`, `JwtServiceTest` |
| 3 | Seguridad | Confidencialidad de credenciales | A | Se realiza un volcado de la tabla `usuarios` → ninguna contraseña es recuperable en texto plano. | `BCryptPasswordEncoder(12)` en `SecurityConfig`. | `AuthServiceTest` |
| 4 | Seguridad | Resistencia a fuerza bruta | M | Un cliente realiza 10 intentos fallidos de login en 1 minuto sobre la misma cuenta → el sistema bloquea/retarda intentos adicionales. | `LoginAttemptService` (contador por usuario) + `TooManyRequestsException` → `429`. | `LoginAttemptService`, casos en `AuthServiceTest` |
| 5 | Eficiencia de desempeño | Comportamiento temporal | A | 50 usuarios concurrentes listan `/api/estudiantes` durante 30 s con caché caliente → el percentil 95 de latencia es < 200 ms y 0 % de error 5xx. | Paginación server-side (`PageRequest`/`Sort`) + índices en PostgreSQL (`idx_usuarios_username`, `idx_estudiantes_rfid`, `idx_asistencias_estudiante`) + caché Redis. | `k6/listado-estudiantes.js` (`thresholds: p95<200ms, rate==0`) → `make bench` → `scripts/perf-analysis.py` → `docs/mediciones/perf/REPORT.md` |
| 6 | Eficiencia de desempeño | Utilización de recursos | M | Una operación agregada (conteo/baja masiva por categoría) procesa N registros → se ejecuta en una sola sentencia, no en un bucle N+1 desde la aplicación. | Lógica agregada implementada como función PL/pgSQL en el motor (`fn_contar_estudiantes_activos`, `fn_desactivar_estudiantes_categoria`), sin SQL dinámico. | `scripts/audit-sql-dynamic.sh`, `db/schema.sql` |
| 7 | Fiabilidad | Disponibilidad | A | Se reinicia el contenedor `postgres` o `redis` → `docker compose` reintenta el arranque de `backend` hasta que las dependencias reportan `healthy`. | `healthcheck` + `depends_on: condition: service_healthy` en `docker-compose.yml`; endpoint `/actuator/health` expuesto y público. | `docker-compose.yml`, `SecurityConfig` (permitAll a `/actuator/health`) |
| 8 | Fiabilidad | Tolerancia a fallos | M | Una petición con datos inválidos o un recurso inexistente llega a la API → el cliente recibe un cuerpo `Problem Details` (RFC 7807) consistente, nunca un stack trace. | `GlobalExceptionHandler` + `ProblemDetailsAuthHandlers` centralizados. | `RecursoNoEncontradoException`, tests unitarios |
| 9 | Mantenibilidad | Modularidad | A | Se agrega el módulo `entrenador` (ya modelado en BD) → no requiere modificar `estudiante` ni `auth`. | Organización por dominio (`auth/`, `estudiante/`, `common/`, `config/`) con capas Controller→Service→Repository desacopladas. | Estructura de `backend/src/main/java/org/uteq/backend` |
| 10 | Mantenibilidad | Capacidad de prueba | A | Se ejecuta `make test` sobre una rama nueva → la cobertura de línea del módulo no baja de lo exigido. | JaCoCo con *quality gate* `COVEREDRATIO >= 0.60` configurado como *build check* en `pom.xml`; H2 en memoria para tests. | `pom.xml` (plugin `jacoco-maven-plugin`), CI (`.github/workflows/ci.yml`) |
| 11 | Compatibilidad | Interoperabilidad | M | Un tercero necesita integrar un sistema externo con la API → puede generar un cliente sin leer el código fuente. | Documentación OpenAPI 3.0 autogenerada (Springdoc) + Swagger UI + colección Postman versionada. | `docs/postman/coleccion.json`, `/api/docs`, `/api/swagger-ui.html` |
| 12 | Portabilidad | Capacidad de instalación / Adaptabilidad | A | Se clona el repositorio en una máquina limpia con Docker y `make` → el sistema completo queda operativo en menos de 2 minutos. | `docker-compose.yml` con imágenes pinadas por digest SHA-256 + `Makefile` (`make up`) + `db/schema.sql` montado en `initdb.d`. | `README.md` (Bloque B.1), `scripts/pin-digests.sh` |
| 13 | Usabilidad | Accesibilidad / Rendimiento percibido | B | Un evaluador ejecuta Lighthouse sobre el SPA → los puntajes de Performance y Accesibilidad superan el umbral definido. | SPA Angular standalone con *lazy loading* de rutas (según crezca `app.routes.ts`) y build de producción optimizado vía `nginx.conf`. | `lighthouserc.js` → `docs/mediciones/lighthouse/` |
| 14 | Funcionalidad | Corrección funcional | A | Se registra una asistencia `PRESENTE`/`TARDE` y luego se califica al estudiante → una calificación de un estudiante `AUSENTE` es rechazada. | Regla de negocio validada en el backend (documentada en `db/schema.sql`, V4) antes de persistir en `detalle_evaluacion`. | Pendiente de implementar en `EstudianteService`/nuevo `EvaluacionService` — **brecha a cerrar**, ver nota abajo |

## Notas de trazabilidad

- Las filas 1–12 corresponden a atributos **ya verificables** con el código y los
  scripts existentes en el repositorio (`scripts/`, `k6/`, `.github/workflows/ci.yml`).
- La fila 14 documenta una regla de negocio que **ya existe en el modelo de datos**
  (comentario en `db/schema.sql`, sección V4: *"se valida en backend"*) pero **aún
  no tiene componente de aplicación** (no existe `EvaluacionController`/`Service`
  en `backend/src/main/java`). Se deja registrada aquí para que quede trazada en
  la matriz de `docs/trazabilidad/matriz.csv` como pendiente, no como cumplida.
- Cada fila puede vincularse 1:1 a una fila de `docs/trazabilidad/matriz.csv`
  (columna sugerida: `atributo_calidad_id` = número de esta tabla).
