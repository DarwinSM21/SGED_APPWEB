# Diseño — Recuperación de contraseña (A22)

**Fecha:** 2026-09-07
**Origen:** Revisión de requisitos SRS SGED v1.2/1.3 contra ISO/IEC/IEEE 29148:2018, punto **A22**.
**Autor:** Arcalle Grefa Darwin Orlando
**Estado:** diseño aprobado, pendiente de plan de implementación.

---

## 1. Problema

`AuthController` expone hoy solo `registro`, `login`, `logout`, `refresh`, `me` y
`ping`. El alta de cuentas la hace exclusivamente el `ADMINISTRADOR`. Un usuario
que olvida su contraseña **no tiene ninguna vía de recuperarla**: depende de que
un administrador se la cambie a mano, y ni siquiera está especificado que pueda
hacerlo.

La revisión del docente exige **una de dos**: especificar el flujo completo, o
declararlo fuera de alcance con justificación. El equipo decidió implementarlo
(Opción 2 de la conversación de diseño): flujo self-service con enlace enviado
por correo.

## 2. Alcance

**Dentro:**

- Endpoint público para solicitar el restablecimiento (`POST /api/auth/forgot`).
- Endpoint público para consumir el enlace y fijar la nueva contraseña
  (`POST /api/auth/reset`).
- Token de un solo uso, de vigencia limitada, almacenado en Redis.
- Envío del enlace por correo (SMTP Gmail en producción; modo consola en
  desarrollo/CI).
- Política de contraseñas unificada — **cubre también el punto A21** de la misma
  revisión.
- Pantallas Angular: solicitud y restablecimiento.
- Invalidación de las sesiones activas del usuario al restablecer.
- Rate limiting de la solicitud.
- Requisitos nuevos en `SRS.md` y filas en `matriz.csv` (A22, A21 y el RNF de
  correo saliente).
- `.env.example`, `render.yaml` y `RUNBOOK.md` actualizados.

**Fuera (YAGNI):**

- Verificación de titularidad del correo (los `Person.correo` no están
  verificados; se documenta como limitación).
- Cola de reintentos de envío en background.
- Autoservicio de registro (sigue siendo solo del ADMINISTRADOR).
- Notificación al usuario de que su contraseña cambió (el correo de reset ya
  implica interacción; un segundo correo es ruido).
- Historial persistente de restablecimientos (la auditoría existente deja
  rastro suficiente).

## 3. Restricciones del entorno

| Restricción | Consecuencia en el diseño |
|---|---|
| No hay servidor de correo propio ni credenciales compartidas | Producción usa Gmail vía App Password (lo administra Darwin); dev/CI no envían correo |
| `make up` desde un clon limpio debe funcionar sin configuración extra (RNF-12) | El modo por defecto es `mail.enabled=false` → no se envía nada, se registra la URL en el log |
| `/api/auth/**` ya es `permitAll()` en `SecurityConfig` | No se toca la configuración de seguridad |
| Redis ya está disponible (blacklist JWT, caché) | El token, la época de invalidación y el rate-limit viven en Redis; **sin migración de esquema** |
| `username` es un correo (`@Email` en `RegisterRequest`), `Person.correo` es `NOT NULL UNIQUE` | La búsqueda admite username o correo; siempre hay un correo destino |
| El docente revisa contra OWASP | El endpoint no revela si la cuenta existe; hay rate limiting; el token es opaco y de alta entropía |

## 4. Arquitectura

### 4.1 Componentes nuevos (backend)

Paquete `org.uteq.backend.seguridad.auth` (junto a lo existente):

| Componente | Tipo | Responsabilidad |
|---|---|---|
| `PasswordResetService` | `@Service` | Orquesta: generar token, guardarlo, disparar correo, validar token, cambiar contraseña, invalidar sesiones, auditar |
| `PasswordResetTokenStore` | `@Component` | Encapsula el acceso a Redis para el token (guardar con TTL, resolver, consumir, invalidar el anterior del usuario) |
| `ResetRequestLimitService` | `@Service` | Rate limiting por identificador y por IP (mismo patrón que `LoginAttemptService`) |
| `SessionEpochService` | `@Service` | Marca y consulta la época de invalidación de sesiones por usuario en Redis (sección 4.5) |
| `PasswordPolicy` | clase de validación | Reglas de contraseña; reutilizada por registro, reset y cambio del admin |
| `PasswordResetMailer` | interfaz | `enviarEnlace(correo, urlRestablecimiento)` |
| `SmtpPasswordResetMailer` | `@Component` (`mail.enabled=true`) | Envía HTML vía `JavaMailSender` |
| `LoggingPasswordResetMailer` | `@Component` (`@ConditionalOnProperty` — por defecto) | Escribe la URL en el log a nivel `WARN` |
| `ForgotPasswordRequest` | `record` DTO | `{ identificador }` |
| `ResetPasswordRequest` | `record` DTO | `{ token, nuevaPassword }` |

`AuthController` gana dos métodos (`forgot`, `reset`) que solo traducen HTTP →
`PasswordResetService`, igual que el resto del controlador.

### 4.2 Flujo — solicitud (`POST /api/auth/forgot`)

```
Cliente → POST /api/auth/forgot { identificador }
  AuthController.forgot
    ResetRequestLimitService.check(identificador, ip)   // 429 si excede
    PasswordResetService.solicitar(identificador)
      buscar UserAccount por username O por Person.correo (activo)
      si no existe o inactivo:  return  (silencioso)
      si existe:
        token = 32 bytes aleatorios → Base64URL
        PasswordResetTokenStore.guardar(username, token, ttl=30min)
          - invalida el token anterior del usuario si había
        url = mail.reset-url-base + "?token=" + token
        PasswordResetMailer.enviarEnlace(person.correo, url)   // try/catch: log, no propaga
        AuditService.recordEvent("PWRESET_SOLICITADO", "Usuario", idUsuario, ...)
  ← 202 Accepted { mensaje: "Si existe una cuenta asociada, se enviaron instrucciones..." }
```

El `202` es **idéntico** exista o no la cuenta, y tanto si el correo se envió
como si falló. La única señal observable distinta es el `429` del rate limit,
que no depende de la existencia de la cuenta.

### 4.3 Flujo — restablecimiento (`POST /api/auth/reset`)

```
Cliente → POST /api/auth/reset { token, nuevaPassword }
  AuthController.reset
    PasswordResetService.restablecer(token, nuevaPassword)
      username = PasswordResetTokenStore.resolver(token)     // null → 400
      PasswordPolicy.validar(nuevaPassword, username)        // inválida → 422
      user = UserAccountRepository.findByUsernameAndActivoTrue(username)  // null → 400
      user.password_Hash = passwordEncoder.encode(nuevaPassword)
      UserAccountRepository.save(user)
      PasswordResetTokenStore.consumir(token)                // borra token + índice de usuario
      SessionEpochService.marcar(username)                   // sec:pwepoch:{username} = now
      AuditService.recordEvent("PWRESET_COMPLETADO", "Usuario", idUsuario, ...)
  ← 204 No Content
```

### 4.4 Token en Redis

- Generación: `SecureRandom`, 32 bytes → `Base64.getUrlEncoder().withoutPadding()`.
- **No se guarda el token crudo.** Clave: `pwreset:{sha256(token)}` → valor
  `username`. TTL 30 min (`mail.reset-token-ttl-minutes`).
- Índice inverso para "un solo token activo por usuario":
  `pwreset:user:{username}` → `sha256(token)`, mismo TTL. Al generar uno nuevo se
  borra la clave `pwreset:{sha256_anterior}`.
- `consumir(token)`: borra `pwreset:{sha256}` y `pwreset:user:{username}`.
- Motivo de guardar el hash: un volcado de Redis no entrega tokens usables.

### 4.5 Invalidación de sesiones (época por usuario)

- `SessionEpochService.marcar(username)` escribe `sec:pwepoch:{username}` = epoch
  segundos actual, TTL = vida del refresh token (para no acumular claves muertas).
- `JwtAuthenticationFilter` (o `JwtService.isTokenValid`): tras validar firma y
  expiración, si existe `sec:pwepoch:{username}` y el `iat` del token es anterior,
  el token se rechaza (401).
- Efecto: al restablecer, todas las sesiones emitidas antes quedan muertas —
  incluidas las de refresh token, que hoy no se pueden revocar de otro modo.
- Coste: ~15 líneas + una lectura de Redis por request autenticado (Redis ya
  está en la ruta caliente por la blacklist).

### 4.6 Política de contraseñas (`PasswordPolicy`) — cubre A21

Reglas:

- longitud mínima **8**, máximo **72 bytes** (límite de BCrypt; una contraseña
  más larga se trunca silenciosamente y es un riesgo real);
- al menos **una letra** y **un dígito**;
- distinta del `username` (comparación exacta, sin distinguir mayúsculas).

Aplicación:

- `PasswordResetService.restablecer` → `422` con `ProblemDetail` si falla.
- `AuthService.register` → sustituye el `@Size(min = 6)` actual de
  `RegisterRequest`.
- `UserAccountService` (cambio de contraseña por el admin) → misma validación.

La regla se expresa una vez, en `PasswordPolicy.validar(password, username)`, que
lanza `IllegalArgumentException` con el mensaje concreto; cada llamador lo traduce
a su código HTTP.

### 4.7 Rate limiting (`ResetRequestLimitService`)

Mismo patrón que `LoginAttemptService` (Redis `increment` + `expire`):

| Clave | Límite | Ventana |
|---|---|---|
| `pwreset_lim:id:{identificador_normalizado}` | 3 | 15 min |
| `pwreset_lim:ip:{ip}` | 10 | 60 min |

Exceder cualquiera → `TooManyRequestsException` → `429` con `ProblemDetail`
(mismo manejador que RF-06). El identificador se normaliza (trim + minúsculas)
antes de la clave para que no se eluda con mayúsculas.

### 4.8 Correo

`spring-boot-starter-mail` nuevo en `backend/pom.xml`.

Config en `application.yml` (bloque nuevo, con defaults, deshabilitado por
defecto — misma filosofía que `ia:`):

```yaml
mail:
  enabled: ${MAIL_ENABLED:false}
  from: ${MAIL_FROM:no-reply@sged.local}
  reset-url-base: ${MAIL_RESET_URL_BASE:https://localhost:8443/#/restablecer}
  reset-token-ttl-minutes: ${MAIL_RESET_TTL:30}

spring:
  mail:
    host: ${MAIL_HOST:}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.connectiontimeout: 10000
      mail.smtp.timeout: 10000
      mail.smtp.writetimeout: 10000
```

Selección de implementación:

- `mail.enabled=false` (por defecto) → `LoggingPasswordResetMailer`. Escribe
  `WARN "PWRESET url para {correo}: {url}"`. `make up`, la CI y una demo offline
  funcionan sin tocar nada.
- `mail.enabled=true` → `SmtpPasswordResetMailer`. Construye un
  `MimeMessage` HTML (asunto "Restablece tu contraseña de SGED", cuerpo con el
  enlace y la nota de vigencia de 30 min) y lo envía por `JavaMailSender`.

Fallo del proveedor SMTP: `SmtpPasswordResetMailer` captura la excepción, la
registra (`log.error` + evento de auditoría `PWRESET_ENVIO_FALLIDO`) y **no la
propaga**. `/forgot` responde `202` igual. El usuario puede reintentar.

### 4.9 Frontend (Angular, `frontend/src/app/auth/`)

| Ruta | Componente | Contenido |
|---|---|---|
| `/recuperar` | `RecuperarComponent` | Un campo (usuario o correo) + botón. Al enviar: `POST /forgot`, luego pantalla de confirmación genérica (siempre el mismo texto). Enlace de vuelta a login. |
| `/restablecer` | `RestablecerComponent` | Lee `?token=` de la URL. Dos campos (contraseña nueva + confirmación) con las reglas de `PasswordPolicy` visibles y validación en vivo. Al enviar: `POST /reset` → éxito redirige a `/login` con aviso; `400` muestra "el enlace expiró o ya se usó" + enlace a `/recuperar`; `422` muestra el detalle de la regla incumplida. |

- Enlace "¿Olvidaste tu contraseña?" en `login.component.html`, debajo del botón
  de entrar.
- `auth.service.ts`: `solicitarRecuperacion(identificador): Observable<void>` y
  `restablecerPassword(token, password): Observable<void>`.
- Ambas rutas son públicas (fuera del `authGuard`).

## 5. Errores y códigos

| Situación | HTTP | Cuerpo |
|---|---|---|
| Solicitud aceptada (exista o no la cuenta, se envíe o no el correo) | `202` | `{ mensaje }` genérico |
| Rate limit excedido | `429` | `ProblemDetail` |
| Reset con token válido | `204` | — |
| Reset con token ausente / desconocido / expirado / ya usado | `400` | `ProblemDetail` "enlace inválido o expirado" |
| Reset con contraseña que incumple la política | `422` | `ProblemDetail` con la regla concreta |
| Cuerpo mal formado (falta campo) | `400` | `ProblemDetail` de `@Valid` |

Ningún mensaje de error distingue "cuenta no existe" de "cuenta existe".

## 6. Pruebas

**Backend (cuentan hacia RNF-09 ≥ 70 %):**

- `PasswordResetServiceTest`
  - `solicitar` con username existente → guarda token con TTL y llama al mailer
  - `solicitar` con correo existente (no username) → igual
  - `solicitar` con identificador inexistente → no toca Redis, no llama al mailer, no lanza
  - `solicitar` con cuenta inactiva → igual que inexistente
  - segunda `solicitar` del mismo usuario → el token anterior deja de resolver
  - `restablecer` con token válido → cambia el hash, consume el token, marca la época
  - `restablecer` con token ya consumido → 400 (no cambia nada)
  - `restablecer` con token expirado (simulado) → 400
  - `restablecer` con contraseña débil → 422, no cambia el hash
- `PasswordResetTokenStoreTest` — guardar/resolver/consumir/invalidar-anterior contra Redis embebido o mock
- `ResetRequestLimitServiceTest` — 4.ª solicitud por identificador → bloquea; por IP a la 11.ª; expiración de ventana
- `PasswordPolicyTest` — 7 caracteres, sin dígito, sin letra, igual al username, 73 bytes, caso válido
- `SessionEpochServiceTest` — token con `iat` anterior a la época → inválido; posterior → válido
- `AuthControllerTest` — `/forgot` siempre `202`; `/reset` `204` / `400` / `422`; `429` al exceder
- `SmtpPasswordResetMailerTest` — invoca `JavaMailSender.send` con destinatario y asunto correctos (mock)
- `LoggingPasswordResetMailerTest` — registra la URL

**Frontend:**

- `recuperar.component.spec.ts` — envía la solicitud; muestra confirmación genérica; no filtra si la cuenta existe
- `restablecer.component.spec.ts` — toma el token de la query; validación en vivo de la política; maneja `204` / `400` / `422`

## 7. Impacto en documentación (mismo Track 1)

### 7.1 `SRS.md`

- **RF nuevo (A22)** — módulo 3.1:
  > *El sistema deberá permitir a un usuario restablecer su contraseña mediante
  > un enlace de un solo uso, de vigencia limitada (30 minutos), enviado a su
  > correo registrado, sin intervención del administrador, e invalidando las
  > sesiones activas del usuario al completarse el restablecimiento.*
  - Prioridad: Alta · Estado: ✅ Implementado · MoSCoW: Must
  - Origen: `POST /api/auth/forgot`, `POST /api/auth/reset`
  - Verificación: `PasswordResetServiceTest`, `AuthControllerTest`; método:
    prueba automatizada + demostración en vivo.

- **RNF nuevo (A21)** — 4.2 Seguridad:
  > *El sistema deberá exigir contraseñas de al menos 8 caracteres, con al menos
  > una letra y un dígito, distintas del nombre de usuario, y de un máximo de 72
  > bytes; esta política deberá aplicarse en el registro, en el restablecimiento
  > y en el cambio de contraseña administrativo.*
  - Verificación: `PasswordPolicyTest`; método: prueba automatizada + inspección.

- **RNF nuevo (correo saliente)** — 4.2 Seguridad / interfaces externas:
  > *El sistema deberá enviar el correo de restablecimiento mediante SMTP sobre
  > STARTTLS a través de un proveedor autorizado (Gmail). Ante la
  > indisponibilidad del proveedor, la solicitud de restablecimiento deberá
  > seguir respondiendo de forma genérica y el fallo deberá quedar registrado;
  > la configuración por defecto no envía correo y registra el enlace en la
  > bitácora.*
  - Verificación: `SmtpPasswordResetMailerTest`, `LoggingPasswordResetMailerTest`;
    método: prueba automatizada + demostración.

### 7.2 `matriz.csv`

Una fila por cada uno de los tres, con endpoint, archivo de implementación,
prueba y estado.

### 7.3 Configuración y despliegue

- `.env.example` — sección "Correo saliente (recuperación de contraseña)" con
  las 5 variables `MAIL_*` vacías y comentadas.
- `render.yaml` — las 5 variables declaradas (los valores se cargan como
  secretos en el panel de Render).
- `docs/despliegue/RUNBOOK.md` — cómo generar el App Password de Gmail
  (Verificación en 2 pasos → https://myaccount.google.com/apppasswords) y qué
  valor va en cada variable.

## 8. Riesgos y decisiones abiertas

| Tema | Decisión |
|---|---|
| Correos no verificados | Aceptado como limitación; se documenta en `ETHICS.md` o en la nota del RF. Un atacante que ya controla el correo de la víctima puede tomar la cuenta — pero ese mismo atacante ya tendría acceso a cualquier servicio de la víctima. |
| App Password de una cuenta Gmail personal | Aceptado para la defensa; el secreto vive solo en `.env` local y en los secretos de Render, nunca en git. Rotable al instante. |
| Época de invalidación añade una lectura de Redis por request | Aceptado; Redis ya está en la ruta caliente por la blacklist de JTI. |
| `SmtpPasswordResetMailer` síncrono dentro del request de `/forgot` | Aceptado; `JavaMailSender` con timeout corto (10 s, propiedad `mail.smtp.timeout`). El `202` no espera confirmación de entrega real del proveedor, solo el envío al SMTP. |

## 9. Definición de terminado

- `/api/auth/forgot` y `/api/auth/reset` implementados con los códigos de la
  sección 5.
- Token de un solo uso en Redis con TTL, hash y un token activo por usuario.
- `PasswordPolicy` aplicada en los tres puntos.
- Rate limiting activo.
- Invalidación de sesiones por época.
- Los dos mailers, con `LoggingPasswordResetMailer` por defecto.
- Pantallas `/recuperar` y `/restablecer` con el enlace desde login.
- Suite de pruebas de la sección 6 en verde; cobertura global sigue ≥ 70 %.
- `SRS.md` con los tres requisitos nuevos; `matriz.csv` con sus filas.
- `.env.example`, `render.yaml`, `RUNBOOK.md` actualizados.
- Envío real verificado una vez contra Gmail con `MAIL_ENABLED=true`.
