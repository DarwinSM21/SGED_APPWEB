# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Ver [VERSIONING.md](VERSIONING.md) para el esquema de versiones/tags.
Para el historial cronológico específico de cada requisito (qué RF cambió,
cuándo y en qué commit), ver
[`docs/requisitos/CHANGELOG-REQ.md`](docs/requisitos/CHANGELOG-REQ.md) —
complementario a este, no un duplicado.

## [v0.9.0-rc] - 2026-07-30

### Reestructuración (mergeada desde `feature/entrega3`)
- Backend reorganizado en tres dominios: `academico`, `deportivo`,
  `seguridad`. `Estudiante` se mueve de `seguridad` a `academico`.
- Categoría normalizada: de texto libre (`VARCHAR`, patrón `SUB-NN`) a
  entidad propia `deportivo.categorias` con catálogo y rango de edad.
- Cinco recursos CRUD nuevos con API REST propia: `Categoria`,
  `Entrenador`, `Usuario`, `Persona`, `EstadoGeneral`.
- Procedimientos almacenados movidos a `academico` con parámetro `INT`
  (`id_categoria`) en vez de `VARCHAR`.

### Correcciones tras la reestructuración
- `ADR-002` corregido: describía JWT en `localStorage` con header
  `Authorization: Bearer`; el código real usa cookie `HttpOnly`.
- `docs/etica/ETHICS.md` corregido: afirmaba que el equipo no recolectaba
  peso/altura del estudiante; esos campos ya son reales y opcionales en la
  API (hallazgo H-06, sin base legal documentada todavía).
- Cobertura de pruebas: regresión detectada a 39,8 % (los 5 recursos nuevos
  no tenían pruebas propias); corregida a 72,7 % con 57 pruebas nuevas
  (10 clases).
- Eliminadas 14 clases stub sin implementación de `academico.representante`
  y `deportivo.equipo` — 13 con cuerpo vacío y una de 0 bytes
  (`RepresentanteController.java`). Nada las referenciaba. Los archivos de
  0 bytes son el defecto que el docente ya observó tres veces en la Entrega
  1B (OBS-08, OBS-10, OBS-11); ambos módulos siguen documentados como
  pendientes para la Entrega Final.
- Reporte JaCoCo archivado regenerado con `clean test`: el anterior se midió
  sobre un `target/` con `.class` de antes de la reestructuración e incluía
  paquetes inexistentes (`org.uteq.backend.auth.*`,
  `org.uteq.backend.estudiante.*`). El objetivo `make test` pasa a usar
  `clean test` para que el defecto no pueda repetirse.
- Modelo C4 actualizado al estado real: el nivel 3 pasa de 2 a 20
  componentes (los tres dominios, no solo `auth` y `estudiante`), y la
  descripción de PostgreSQL corrige el esquema `academico` y los
  procedimientos `sp_*` reales. Nuevo `L3-componentes.png`.
- Eliminados los `.puml` de `docs/diagramas/` que duplicaban el modelo C4
  con contenido previo a la reestructuración; `workspace.dsl` queda como
  fuente única y `docs/diagramas/` conserva solo el MER.
- Nuevo objetivo `make diagrams`: regenera los PNG del C4 desde el DSL con
  `structurizr/structurizr` y `plantuml/plantuml` en contenedores. Se
  documenta que la imagen `structurizr/cli` quedó deprecada y su entrypoint
  ya no exporta nada.
- Retirado `docs/informe-entrega-3.pdf` (sin fuente `.tex`/`.docx`
  versionada); `docs/informe/main.tex` queda como único informe oficial.
- Colisión de numeración `ADR-003` resuelta (el propio pasa a `ADR-007`).

### Seguridad
- **Control de acceso restablecido en los 5 recursos de la reestructuración.**
  `Categoria`, `Entrenador`, `Persona` y `EstadoGeneral` no tenían ninguna
  anotación `@PreAuthorize`: con `anyRequest().authenticated()`, cualquier
  cuenta con rol `USER` podía listar personas, buscarlas por cédula y
  crear/editar/eliminar registros. Es una regresión de OBS-09. Registrado
  como hallazgo H-08 en `docs/etica/ETHICS.md` y verificado con evidencia
  por recurso en `docs/mediciones/sec/a01-acceso-roto.txt`.
- JWT migrado de header `Authorization` a cookies `HttpOnly` + `Secure` +
  `SameSite=Strict`; `/api/auth/registro` protegido.
- Terminación TLS en `:8443` vía nginx con certificado autofirmado (OWASP A02).
- Content-Security-Policy explícito en `SecurityConfig`.
- Log de auditoría estructurado A09 (login OK/FAIL con IP y `sub`).
- `@Valid` responde `422` en vez de `400` (alineado a la auditoría OWASP A03).
- Auditoría OWASP de 6 controles corregida y regenerada contra el stack real
  (A01, A02, A03, A05, A07, A09) — `docs/mediciones/sec/`.

### Correcciones de funcionamiento
- **`POST /api/auth/registro` estaba roto (RF-01).** Devolvía `500` en toda
  petición: la reestructuración volvió `cedula`, `correo` y
  `fecha_nacimiento` columnas `NOT NULL`, pero `RegisterRequest` no las
  pedía y el alta violaba la restricción. Tampoco se asignaba
  `id_estado_general`, también `NOT NULL`. Las pruebas no lo detectaron
  porque mockean el repositorio y la restricción la aplica PostgreSQL.
- Un cuerpo de petición ausente o mal formado devolvía `500`
  ("Error interno del servidor") en vez de `400`: faltaba el manejador de
  `HttpMessageNotReadableException` en `GlobalExceptionHandler`.
- `scripts/audit-owasp.sh` invocaba `desactivar-categoria` con
  `?categoria=SUB-12`, forma anterior a la normalización de la categoría, y
  no limpiaba el contador de intentos de login —que se lleva por IP, no por
  usuario—, de modo que su propio control A07 dejaba las corridas siguientes
  bloqueadas y A01 devolvía `401` en todo, un falso correcto.

### Datos
- Conversión de consultas JPQL a procedimientos almacenados reales invocados
  vía `@Procedure` (antes quedaban huérfanos o usaban `FUNCTION` en vez de
  `PROCEDURE`) — `V5`/`V6` en `db/migration/`.
- `database/` renombrado a `db/` en la raíz para cumplir la estructura exigida.
- Postgres y Redis pinados por digest sha256 real en `docker-compose.yml`
  (Bloque B.1).

### Rendimiento y pruebas
- Corrección de JaCoCo (el `argLine` de Surefire pisaba el javaagent) —
  cobertura real ahora medible.
- Pruebas unitarias e de integración agregadas: `EstudianteController`,
  `LoginAttemptService`, `RedisBlacklistService`, y 10 clases nuevas para
  los recursos de la reestructuración.
- Evidencia empírica real generada contra el stack en vivo: 3 corridas de
  k6 con análisis de intervalo de confianza 95%, `docs/mediciones/perf/REPORT.md`.
- Lighthouse: accesibilidad 100/100, rendimiento 92,3; SEO limitado a 63
  a propósito (`robots.txt` real por tratar datos de menores).

### Correcciones
- Cache de `Estudiante` rompía desde el segundo request (Jackson no leía el
  `@class` raíz al usar `GenericJackson2JsonRedisSerializer`).
- `GenericJackson2JsonRedisSerializer` no soportaba `java.time.Instant`.
- Bugs de autorización y sesión encontrados corriendo el sistema en vivo.

### Pendiente para la Entrega Final
- ~~Encuesta SUS con participantes externos reales~~ — completada el
  2026-07-30 (commit posterior a este tag): 10 participantes, media 68,25
  (grado C), patrón bimodal por perfil. Ver
  `docs/mediciones/sus/REPORT.md`.
- API REST del dominio deportivo restante (horarios, sesiones, asistencias,
  evaluaciones) — esquema ya migrado.
- `academico.representante` y `deportivo.equipo`: paquetes vacíos, sin
  esquema.

## [Unreleased]

### Seguridad
- JWT migrado de header `Authorization` a cookies `HttpOnly` + `Secure` +
  `SameSite=Strict`; `/api/auth/registro` protegido.
- Terminación TLS en `:8443` vía nginx con certificado autofirmado (OWASP A02).
- Content-Security-Policy explícito en `SecurityConfig`.
- Log de auditoría estructurado A09 (login OK/FAIL con IP y `sub`).
- `@Valid` responde `422` en vez de `400` (alineado a la auditoría OWASP A03).
- Auditoría OWASP de 6 controles corregida y regenerada contra el stack real
  (A01, A02, A03, A05, A07, A09) — `docs/mediciones/sec/`.

### Datos
- Conversión de consultas JPQL a procedimientos almacenados reales invocados
  vía `@Procedure` (antes quedaban huérfanos o usaban `FUNCTION` en vez de
  `PROCEDURE`) — `V5`/`V6` en `db/migration/`.
- `database/` renombrado a `db/` en la raíz para cumplir la estructura exigida.
- Postgres y Redis pinados por digest sha256 real en `docker-compose.yml`
  (Bloque B.1).

### Rendimiento y pruebas
- Corrección de JaCoCo (el `argLine` de Surefire pisaba el javaagent) —
  cobertura real ahora medible.
- Pruebas unitarias e de integración agregadas: `EstudianteController`,
  `LoginAttemptService`, `RedisBlacklistService`.
- Evidencia empírica real generada contra el stack en vivo: 3 corridas de
  k6 con análisis de intervalo de confianza 95%, `docs/mediciones/perf/REPORT.md`.

### Correcciones
- Cache de `Estudiante` rompía desde el segundo request (Jackson no leía el
  `@class` raíz al usar `GenericJackson2JsonRedisSerializer`).
- `GenericJackson2JsonRedisSerializer` no soportaba `java.time.Instant`.
- Bugs de autorización y sesión encontrados corriendo el sistema en vivo.

## [v0.1.0-entrega-1b] - 2026-06-24

### Agregado
- Arranque inicial del backend (Spring Boot 3.2.5, Java 21) y frontend (Angular):
  autenticación JWT, `AuthController`, `JwtService`, blacklist de tokens en Redis.
- CRUD completo de `Estudiante` con paginación, soft delete y caché.
- Migraciones Flyway iniciales (`V1`–`V4`).
- `docker-compose.yml`, `Dockerfile`, documentación inicial (README, ADR-001, ADR-003).
- Colección Postman con 11 endpoints versionada.
- Diagramas C4 (nivel 1 y 2) y modelo entidad-relación de ProFútbol.
- Estructura y evidencia de Entrega 1A (planificación y diseño).

---

Commits individuales: ver `git log`. Roles y trazabilidad de autoría en
[CONTRIBUTORS.md](CONTRIBUTORS.md).
