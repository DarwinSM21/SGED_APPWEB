# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Ver [VERSIONING.md](VERSIONING.md) para el esquema de versiones/tags.

## [Unreleased] — hacia v0.9.0-rc (Tercera Entrega)

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
