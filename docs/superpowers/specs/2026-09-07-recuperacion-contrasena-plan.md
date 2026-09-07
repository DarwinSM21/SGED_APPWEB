# Plan de implementación — Recuperación de contraseña (A22 + A21)

**Spec:** [`2026-09-07-recuperacion-contrasena-design.md`](./2026-09-07-recuperacion-contrasena-design.md)
**Rama:** `main` (Ricardo trabaja después).
**Numeración acordada:** RF nuevos desde RF-37, RNF nuevos desde RNF-14.
**Identidad de commits:** `DarwinSM21 <darwinarcalle@gmail.com>`, sin coautoría.

Cada fase termina con `./mvnw -q test` (o el subconjunto indicado) en verde y es
un commit. Orden pensado para que cada pieza sea testeable aislada antes de
cablearla.

---

## Fase 0 — Dependencia y configuración (sin comportamiento nuevo)

| Archivo | Cambio |
|---|---|
| `backend/pom.xml` | Agregar `spring-boot-starter-mail` |
| `backend/src/main/resources/application.yml` | Bloque `mail:` + `spring.mail.*` de la sección 4.8 del spec |
| `.env.example` | Sección "Correo saliente (recuperación de contraseña)" con las 5 `MAIL_*` vacías y comentadas |
| `render.yaml` | Declarar las 5 `MAIL_*` (sin valor; se cargan como secretos en el panel) |

**Verificación:** `./mvnw -q compile`; `docker compose up backend` arranca con
`MAIL_ENABLED` ausente (usa el default `false`).
**Commit:** `chore(mail): dependencia spring-mail y configuracion (deshabilitada por defecto)`

---

## Fase 1 — Política de contraseñas (A21)

| Archivo | Cambio |
|---|---|
| `backend/.../seguridad/auth/PasswordPolicy.java` (nuevo) | `validar(String password, String username)` → lanza `IllegalArgumentException` con mensaje concreto. Reglas: min 8, ≤72 bytes, ≥1 letra, ≥1 dígito, ≠ username (ignore case) |
| `backend/.../PasswordPolicyTest.java` (nuevo) | 7 chars · 73 bytes · sin dígito · sin letra · = username · caso válido |
| `backend/.../seguridad/auth/dto/RegisterRequest.java` | Quitar `@Size(min = 6)` de `password` (deja `@NotBlank`); la regla real la aplica el servicio |
| `backend/.../seguridad/auth/service/AuthService.java` | En `register`, llamar `passwordPolicy.validar(...)` antes de `encode`; traducir a `422` vía `GlobalExceptionHandler` (ya mapea `IllegalArgumentException`? — si no, agregar mapeo puntual) |
| `backend/.../seguridad/user/service/UserAccountService.java` | En el cambio de contraseña del admin, misma llamada |
| Tests de seed/registro existentes | Ajustar contraseñas de prueba que ya no cumplan (p. ej. `"123456"` → `"clave1234"`) |

**Verificación:** `./mvnw -q test -Dtest='PasswordPolicyTest,AuthServiceTest,UserAccountServiceTest,AuthControllerTest'`
**Commit:** `feat(seguridad): politica de contrasenas unificada (A21)`

---

## Fase 2 — Token en Redis + época de invalidación de sesiones

| Archivo | Cambio |
|---|---|
| `backend/.../seguridad/auth/PasswordResetTokenStore.java` (nuevo) | `StringRedisTemplate`. `guardar(username, tokenCrudo, ttl)` (hashea SHA-256, escribe `pwreset:{hash}`→username y `pwreset:user:{username}`→hash, borra el hash anterior si existía). `resolver(tokenCrudo)→Optional<String>`. `consumir(tokenCrudo)`. |
| `backend/.../PasswordResetTokenStoreTest.java` (nuevo) | guardar/resolver/consumir · segundo `guardar` invalida el primero · token inexistente → empty |
| `backend/.../seguridad/auth/security/SessionEpochService.java` (nuevo) | `marcar(username)` escribe `sec:pwepoch:{username}`=epoch-seg con TTL = vida refresh. `epocaDe(username)→Optional<Long>` |
| `backend/.../SessionEpochServiceTest.java` (nuevo) | marcar y leer · sin marca → empty |
| `backend/.../seguridad/auth/security/JwtAuthenticationFilter.java` | Tras validar firma/exp: si hay época y `iat` del token < época → 401 (no autentica) |
| `backend/.../JwtAuthenticationFilterTest.java` | token con `iat` anterior a la época → rechazado; posterior → aceptado |

**Verificación:** `./mvnw -q test -Dtest='PasswordResetTokenStoreTest,SessionEpochServiceTest,JwtAuthenticationFilterTest,JwtServiceTest'`
**Commit:** `feat(seguridad): token de reseteo en Redis y epoca de invalidacion de sesiones`

---

## Fase 3 — Rate limiting

| Archivo | Cambio |
|---|---|
| `backend/.../seguridad/auth/security/ResetRequestLimitService.java` (nuevo) | Espejo de `LoginAttemptService`. `check(identificador, ip)` → lanza `TooManyRequestsException` si `pwreset_lim:id:{id}` ≥ 3 (15 min) o `pwreset_lim:ip:{ip}` ≥ 10 (60 min). `registrar(identificador, ip)` incrementa ambos |
| `backend/.../ResetRequestLimitServiceTest.java` (nuevo) | 4.ª por id → bloquea · 11.ª por IP → bloquea · expira la ventana |

**Verificación:** `./mvnw -q test -Dtest='ResetRequestLimitServiceTest'`
**Commit:** `feat(seguridad): rate limiting de solicitudes de reseteo`

---

## Fase 4 — Mailers

| Archivo | Cambio |
|---|---|
| `backend/.../seguridad/auth/mail/PasswordResetMailer.java` (nuevo) | Interfaz: `enviarEnlace(String correo, String url)` |
| `backend/.../seguridad/auth/mail/LoggingPasswordResetMailer.java` (nuevo) | `@Component` `@ConditionalOnProperty(name="mail.enabled", havingValue="false", matchIfMissing=true)`. `log.warn("PWRESET url para {}: {}", correo, url)` |
| `backend/.../seguridad/auth/mail/SmtpPasswordResetMailer.java` (nuevo) | `@Component` `@ConditionalOnProperty(name="mail.enabled", havingValue="true")`. `JavaMailSender` + `MimeMessageHelper` HTML. Asunto "Restablece tu contraseña de SGED". Captura excepción → `log.error` + evento auditoría `PWRESET_ENVIO_FALLIDO`, no propaga |
| `backend/.../LoggingPasswordResetMailerTest.java` (nuevo) | captura el log, verifica que aparece la URL |
| `backend/.../SmtpPasswordResetMailerTest.java` (nuevo) | mock `JavaMailSender`, verifica destinatario/asunto; simula excepción → no propaga |

**Verificación:** `./mvnw -q test -Dtest='*MailerTest'`
**Commit:** `feat(mail): mailers de reseteo (consola por defecto, SMTP si mail.enabled)`

---

## Fase 5 — Servicio de orquestación

| Archivo | Cambio |
|---|---|
| `backend/.../seguridad/auth/dto/ForgotPasswordRequest.java` (nuevo) | `record ForgotPasswordRequest(@NotBlank String identificador)` |
| `backend/.../seguridad/auth/dto/ResetPasswordRequest.java` (nuevo) | `record ResetPasswordRequest(@NotBlank String token, @NotBlank String nuevaPassword)` |
| `backend/.../seguridad/auth/service/PasswordResetService.java` (nuevo) | `solicitar(identificador)` y `restablecer(token, nuevaPassword)` según secciones 4.2–4.3 del spec. Inyecta `UserAccountRepository`, `PersonRepository` (o query por correo), `PasswordResetTokenStore`, `PasswordResetMailer`, `SessionEpochService`, `PasswordPolicy`, `PasswordEncoder`, `AuditService`, `@Value` de `mail.reset-url-base` |
| `backend/.../PasswordResetServiceTest.java` (nuevo) | Los 9 casos de la sección 6 del spec |

**Verificación:** `./mvnw -q test -Dtest='PasswordResetServiceTest'`
**Commit:** `feat(seguridad): servicio de recuperacion de contrasena`

---

## Fase 6 — Endpoints

| Archivo | Cambio |
|---|---|
| `backend/.../seguridad/auth/controller/AuthController.java` | `@PostMapping("/forgot")` → `check` + `solicitar` + `registrar`, siempre `202` con cuerpo genérico. `@PostMapping("/reset")` → `restablecer`, `204` / `400` / `422`. Javadoc como el resto |
| `backend/.../AuthControllerTest.java` | `/forgot` siempre `202` (cuenta existe / no existe) · `/reset` `204` · token malo `400` · contraseña débil `422` · `429` al exceder |
| `docs/postman/coleccion.json` | Agregar las dos peticiones |

**Verificación:** `./mvnw -q test -Dtest='AuthControllerTest'` + `./mvnw -q verify` (cobertura ≥70%)
**Commit:** `feat(auth): endpoints POST /api/auth/forgot y /api/auth/reset (A22)`

---

## Fase 7 — Frontend (Angular)

| Archivo | Cambio |
|---|---|
| `frontend/src/app/auth/auth.service.ts` | `solicitarRecuperacion(identificador)` → POST `/api/auth/forgot`; `restablecerPassword(token, password)` → POST `/api/auth/reset` |
| `frontend/src/app/auth/recuperar/recuperar.component.{ts,html,spec.ts}` (nuevo) | Form 1 campo → envía → pantalla de confirmación genérica |
| `frontend/src/app/auth/restablecer/restablecer.component.{ts,html,spec.ts}` (nuevo) | Lee `?token=`; 2 campos con validación en vivo de la política; maneja `204`/`400`/`422` |
| `frontend/src/app/app.routes.ts` | Rutas `/recuperar` y `/restablecer` (públicas, fuera de `authGuard`) |
| `frontend/src/app/auth/login/login.component.html` | Enlace "¿Olvidaste tu contraseña?" → `/recuperar` |

**Verificación:** `cd frontend && npm test -- --watch=false` y `npm run build`
**Commit:** `feat(frontend): pantallas de recuperacion y restablecimiento de contrasena`

---

## Fase 8 — Documentación (porción del Track 1 que acompaña este código)

| Archivo | Cambio |
|---|---|
| `docs/requisitos/SRS.md` | **RF-37** (reseteo, §3.1), **RNF-14** (política de contraseñas, §4.2), **RNF-15** (correo saliente, §4.2). Textos en la sección 7.1 del spec |
| `docs/trazabilidad/matriz.csv` | 3 filas nuevas (RF-37, RNF-14, RNF-15) con endpoint, archivo, prueba, estado |
| `docs/requisitos/CHANGELOG-REQ.md` | Entradas de las 3 adiciones |
| `docs/despliegue/RUNBOOK.md` | Sección "Correo de recuperación": generar App Password de Gmail (2FA → myaccount.google.com/apppasswords), qué va en cada `MAIL_*`, cómo cargarlo en Render |
| `docs/etica/ETHICS.md` | Nota breve: los correos destino no están verificados (limitación conocida de A22) |

**Verificación:** `bash scripts/validate-traceability.sh` (o `make audit`) sin violaciones
**Commit:** `docs(requisitos): RF-37 reseteo de contrasena, RNF-14 politica, RNF-15 correo`

---

## Fase 9 — Verificación de envío real (requiere tus credenciales)

1. Tú: `MAIL_ENABLED=true` + las 5 `MAIL_*` en `backend/.env` local.
2. `docker compose up -d` → `POST /api/auth/forgot` con un usuario real de `seed.sql`.
3. Confirmar que el correo llega a la bandeja de `darwinarcalle@gmail.com`.
4. Abrir el enlace, restablecer, verificar login con la nueva y que la sesión vieja quedó muerta.
5. Tú: rotar el App Password (el que quede en git-history del chat ya no sirve).

**Sin commit** (es verificación manual); si algo falla, se corrige en la fase que corresponda.

---

## Fase 10 — Cierre

- `./mvnw -q clean verify` completo — 0 fallos, cobertura ≥ 70 % líneas y branches.
- `cd frontend && npm test -- --watch=false && npm run build` limpio.
- Esperar CI en verde tras el push.
- Te presento el diff completo para revisión antes de pushear a `origin/main`.

---

## Resumen de archivos nuevos (backend)

```
seguridad/auth/PasswordPolicy.java
seguridad/auth/PasswordResetTokenStore.java
seguridad/auth/security/SessionEpochService.java
seguridad/auth/security/ResetRequestLimitService.java
seguridad/auth/mail/PasswordResetMailer.java
seguridad/auth/mail/LoggingPasswordResetMailer.java
seguridad/auth/mail/SmtpPasswordResetMailer.java
seguridad/auth/service/PasswordResetService.java
seguridad/auth/dto/ForgotPasswordRequest.java
seguridad/auth/dto/ResetPasswordRequest.java
+ 8 clases de prueba
```

## Riesgo de choque con Ricardo (Track 2, los M)

`SRS.md` y `matriz.csv` los toca él para M5–M20. Este plan añade **al final**
(RF-37, RNF-14, RNF-15) y filas nuevas en la matriz — no reescribe entradas
existentes, así que el merge es aditivo. Si él ya empezó, hago la Fase 8 de
último y rebaso.
