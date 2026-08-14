# ADR-008: JWT en cookie HttpOnly y blacklist de revocacion en Redis

## Estado
Aceptado

## Contexto
[ADR-002](ADR-002-autentificacion.md) ya establece que la autenticación es
JWT stateless emitido en `POST /api/auth/login`. Este ADR cubre la parte que
ADR-002 deja abierta: **cómo** viaja ese token y **cómo se revoca**. JWT es
stateless: el servidor no almacena el token, y si viaja accesible desde
JavaScript (localStorage, header manejado a mano) queda expuesto a robo por
XSS. Ademas, sin una lista de revocacion, el servidor no tiene forma de
invalidar un token antes de su expiracion natural cuando el usuario cierra
sesion o el token se compromete. La guia de la Tercera Entrega (Bloque A.1)
exige ademas que toda la autenticacion opere bajo cookie HttpOnly + Secure +
SameSite=Strict.

## Decision
El access token y el refresh token se emiten como cookies `HttpOnly + Secure +
SameSite=Strict`, nunca en el body ni en un header de respuesta legible por
JavaScript. El filtro JWT (`JwtAuthenticationFilter`) lee el JWT directamente
de la cookie `access_token` en cada solicitud. Ademas, se usa Redis para
almacenar los JTI de tokens revocados con un TTL igual al tiempo de expiracion
del token original; el filtro consulta Redis antes de autorizar el acceso, y
`/api/auth/logout` marca el JTI vigente como revocado y limpia ambas cookies.

## Consecuencias

**Positivas:**
- El JWT nunca es legible ni manipulable desde JavaScript (mitiga robo por XSS).
- `SameSite=Strict` mitiga CSRF sin necesitar un token anti-CSRF adicional.
- Logout real e inmediato sin esperar la expiracion del token.
- TTL automático en Redis evita acumulación de entradas obsoletas.
- Baja latencia en la consulta de blacklist (operación O(1)).
- Cumple con OWASP A07 - Fallas de autenticación.

**Negativas:**
- Introduce dependencia de infraestructura adicional (Redis).
- Si Redis no está disponible, el sistema no puede validar tokens revocados.
- La cookie `Secure` exige que el trafico real sea HTTPS; en un entorno local
  sin TLS terminado (ver auditoria OWASP A02, todavia pendiente) el navegador
  puede rechazar guardar o enviar la cookie.

## Alternativas consideradas
- **JWT en localStorage o header manejado por JS**: descartado, expuesto a XSS.
- **Sin blacklist**: no permite logout real, inseguro.
- **Blacklist en PostgreSQL**: mayor latencia, requiere limpieza periódica.
- **Sesiones stateful**: contradice la arquitectura REST stateless.

## Referencias
- OWASP Top 10 2021 - A07 Identification and Authentication Failures
- RFC 7519 - JSON Web Token (JWT)
- RFC 6265 - HTTP State Management Mechanism (atributos de cookies)
