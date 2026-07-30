# Changelog de Requisitos — SGED ProFútbol

**Propósito:** Registro cronológico de todo cambio (adición, modificación, eliminación) a los requisitos funcionales (RF-01 a RF-22) y no funcionales (RNF-01 a RNF-07) desde la Entrega 1A hasta la Tercera Entrega.

| Fecha | Autor | Requisito | Tipo de Cambio | Motivo | Commit |
|---|---|---|---|---|---|
| 2026-06-10 | Equipo | RF-01 a RF-07 | Adición | Módulo de autenticación JWT con registro, login, logout, refresh, rate limiting y ping (Entrega 1B) | `af03d91`, `aa846fe` |
| 2026-06-10 | Equipo | RF-08 a RF-13 | Adición | CRUD de estudiantes con paginación, validación de categoría y baja lógica (Entrega 1B) | `a6877f4`, `ad2fec0` |
| 2026-06-12 | Equipo | RF-14, RF-15 | Adición | Conteo de activos por categoría y desactivación masiva mediante procedimientos almacenados | `ad2fec0`, `c5aedc7`, `2d3c007` |
| 2026-06-14 | Equipo | RF-01 | Modificación | Registro requiere rol ADMIN; se protege endpoint con @PreAuthorize | `b141d6c` |
| 2026-06-14 | Equipo | RF-02 | Modificación | Cambio de JWT en header Authorization a cookie HttpOnly + Secure + SameSite=Strict | `bcb3642`, `666c543`, `4382598` |
| 2026-06-14 | Equipo | RF-03 | Modificación | Se cablea RedisBlacklistService al endpoint /logout para revocación efectiva por JTI | `8002131`, `84de825` (merge) |
| 2026-06-15 | Equipo | RF-06 | Modificación | LoginAttemptService con 5 intentos/15 min; A07 muestra ProblemDetails completo | `3fae486`, `43f10e1` |
| 2026-06-15 | Equipo | RF-08 | Modificación | Se agrega RedisCacheConfig con TTL 60s al listado paginado | `52feb3a` |
| 2026-06-16 | Equipo | RF-11 | Modificación | Validación de formato SUB-NN en EstudianteRequest; @Valid responde 422 en vez de 400 | `3082063`, `9e97153` |
| 2026-06-18 | Equipo | RF-01 a RF-15 | Modificación | Se agregan pruebas JUnit 5 (42 tests, 7 clases) — se cumple mínimo de 5 | `50f8b13`, `1e798bc`, `a2c3d53` |
| 2026-06-20 | Equipo | RF-14, RF-15 | Modificación | Migración de FUNCTION a PROCEDURE real para compatibilidad con @Procedure de Spring Data | `c5aedc7` |
| 2026-06-22 | Equipo | RF-16 a RF-21 | Adición | Modelado del dominio deportivo en V3/V4 de Flyway: entrenadores, horarios, sesiones, asistencias, evaluaciones (solo esquema BD, sin API REST) | `ad2fec0` |
| 2026-06-22 | Equipo | RF-22 | Adición | Planificación: notificación a representantes (sin implementar, solo consideraciones éticas en ETHICS.md) | `98bab0d` |
| 2026-07-01 | Equipo | RNF-01 | Adición | Criterio de rendimiento: p95 < 200 ms con 50 VUs; verificado con k6 (p95 real: 14.18 ms) | `fa02f64`, `4c16cce` |
| 2026-07-01 | Equipo | RNF-02 | Adición | Cache de listados con TTL 60s mediante Redis | `52feb3a` |
| 2026-07-01 | Equipo | RNF-03 | Adición | Contraseñas con BCrypt coste 12 | `aa846fe` |
| 2026-07-05 | Equipo | RNF-04 | Adición | TLS 1.3 en :8443 vía nginx con certificado autofirmado | `767ad92`, `9d9a353` |
| 2026-07-05 | Equipo | RNF-05 | Adición | Cabeceras de seguridad: CSP, HSTS, X-Content-Type-Options: nosniff, X-Frame-Options: DENY | `220ff72`, `43f10e1` |
| 2026-07-05 | Equipo | RNF-06 | Adición | Control de acceso por rol con @PreAuthorize en todos los endpoints protegidos | `b141d6c` |
| 2026-07-05 | Equipo | RNF-07 | Adición | Auditoría de SQL dinámico: script verifica que no exista EXECUTE dinámico ni concatenación en queries Java | `43f10e1` |
| 2026-07-10 | Equipo | RF-01 a RF-07 | Modificación | Se expone /registro, /logout, /refresh completamente; se agrega soporte híbrido JWT (cookie + header como fallback) | `f8dbd63` |
| 2026-07-15 | Equipo | RF-08 a RF-15 | Modificación | Corrección de schema: columnas NOT NULL, tipos SMALLINT, FKs reales, seed data actualizada | `f8d604e` |
| 2026-07-24 | Equipo | RF-14, RF-15 | Modificación | Procedimientos movidos a schema `academico` con JOIN correcto a `deportivo.categorias` | `f8d604e` |
| 2026-07-29 | Equipo | RF-16 a RF-22 | Sin cambios | Pendientes de implementación en próximas entregas (modelados en BD, sin API REST ni UI) | — |