# Escaneo OWASP ZAP baseline (Bloque A.1)

- Fecha: 2026-09-02
- Commit: 0fc8b69
- Herramienta: OWASP ZAP (imagen `ghcr.io/zaproxy/zaproxy:stable`), plan de
  automatización versionado en `zap.yaml` (pasivo, sin ataques activos —
  apropiado para un escaneo automatizado de rutina, no un pentest completo)
- Objetivo: `http://localhost:8080/api/docs` y `http://localhost:8080/`

## Resultado

```
FAIL-NEW: 0   FAIL-INPROG: 0   WARN-NEW: 0   WARN-INPROG: 0   INFO: 0   IGNORE: 0   PASS: 61
```

**Cero hallazgos, de cualquier severidad.** Antes de esta corrida, ZAP
marcaba una alerta de severidad **alta**: `Vulnerable JS Library` (regla
10003), sobre `swagger-ui-bundle.js`. La evidencia de esa corrida
—`DOMPurify.version="3.0.6"`— es una dependencia empaquetada por
Springdoc/Swagger UI 2.3.0, no código propio del equipo, y solo era
alcanzable porque la interfaz interactiva de Swagger quedaba expuesta sin
autenticación en `/api/docs`.

En vez de esperar a que Springdoc publique un *bundle* de Swagger UI con
una versión corregida de DOMPurify, se apagó la exposición pública de la
interfaz: `springdoc.api-docs.enabled` y `springdoc.swagger-ui.enabled`
ahora se leen de `SPRINGDOC_ENABLED` (`application.yml`), con valor
`true` por defecto para desarrollo local y `false` declarado en
`render.yaml` para el ambiente público. `/api/docs` y `/api/docs.json`
devuelven `404` con esa variable en `false`, verificado antes de repetir
el escaneo.

Con Swagger apagado, el rastreador de ZAP reporta dos advertencias de
plan, no hallazgos de seguridad: `GET /api/docs` devuelve `404` (la
interfaz ya no existe, es el resultado esperado del cambio) y `GET /`
devuelve `401` (endpoint protegido sin sesión autenticada durante un
escaneo automatizado, comportamiento correcto).

Reportes completos: `zap-report.html` (legible), `zap-report.json`,
`zap-report.xml` (máquina), en este mismo directorio.

## Corrida anterior

La corrida del 2026-08-14 (commit `73d5114`) sí encontró la alerta alta
de DOMPurify. Se documenta el hallazgo y la corrección aquí en vez de
descartar el historial, siguiendo la misma disciplina de reportar
instrumentos y hallazgos defectuosos que ya se aplicó en otras
mediciones de esta entrega.

## Nota de alcance

Este escaneo cubre el backend (`/api/docs` y lo que el rastreador
alcanza desde ahí). No incluye el *frontend* Angular servido por nginx
(`:4200`/`:8443`); una corrida complementaria contra esa URL queda como
trabajo pendiente antes del cierre de la Entrega Final, junto con la
puesta en producción (Bloque A.4).

---

# Escaneo OWASP ZAP autenticado con active scan (4.4, cierre)

- Fecha: 2026-09-11
- Commit: `1671174` (rama `main`)
- Herramienta: OWASP ZAP (`ghcr.io/zaproxy/zaproxy:stable`), plan de
  automatización versionado en `zap-authenticated.yaml`
- Objetivo: entorno **local** (`docker compose` / `make up`), nunca el
  despliegue público — un *active scan* manda payloads de ataque reales
  (inyección SQL, XSS) y no debe correr contra datos de producción

## Qué cambia respecto al baseline

El escaneo de arriba era pasivo y sin sesión: solo escuchaba tráfico, sin
atacar, y sin credenciales. Esta corrida cierra las dos brechas que
señalaba la observación 4.4:

1. **Sesión autenticada real.** La API es *stateless* por JWT en cookie
   `HttpOnly` (ADR-002/ADR-008), no por formulario de login que ZAP pueda
   automatizar solo — se hace `POST /api/auth/login` contra la cuenta
   semilla `admin` antes de escanear, se captura la cookie `sged_access`,
   y el *addon* `replacer` la inyecta en cada petición del plan
   (`zap-authenticated.yaml`, job `replacer`). Mismo principio que
   `scripts/lighthouse-ci.mjs` usa para Lighthouse.
2. **Active scan, no solo pasivo.** Las rutas se enumeran desde el
   OpenAPI real (`/api/docs.json`, job `openapi`) en vez de depender de
   un *spider* HTML — la API no tiene enlaces que seguir —: **135 rutas**
   importadas. El job `activeScan` (`Default Policy`, acotado a 12 min de
   presupuesto) ataca esas rutas autenticadas. Se excluyen
   `/api/auth/logout` (mataría la sesión a mitad del escaneo) y
   `POST /api/estudiantes/{id}/anonimizar` (RF-50, destructivo por
   diseño — se prueba aparte, no bajo fuzzing).

## Resultado

**Primera corrida (antes del fix de abajo):**

```
FAIL-NEW: 0   WARN-NEW: 3   PASS: 58
```

El *active scan* encontró algo que el baseline pasivo nunca hubiera
visto: `GET` a rutas que solo aceptan `POST` (`/api/auth/login`,
`/api/auth/forgot`, `/api/auth/confirmar-correo`) devolvía **`500`** en
vez de **`405`**. `GlobalExceptionHandler` no tenía un
`@ExceptionHandler(HttpRequestMethodNotSupportedException.class)`
específico, así que esas peticiones caían en el `catch-all` genérico
(`Exception.class`) que sí está pensado para errores de verdad. El
cuerpo de la respuesta ya era el `ProblemDetail` genérico de siempre, sin
traza de pila ni ruta de archivo (`{"title":"Internal Server Error",...}`)
— **no había fuga real de información**, pero un método no soportado
tiene que dar `405`, no `500`. ZAP marcó esto con dos reglas que buscan
la cadena `"Internal Server Error"` en el cuerpo (`10023` Debug Error
Messages, `90022` Application Error Disclosure): ambos son falsos
positivos de fuga (el patrón está en un campo JSON intencional, no es
una traza), pero el `500` de fondo era un defecto real.

**Corregido** (`GlobalExceptionHandler.handleMetodoNoSoportado`, con
prueba `GlobalExceptionHandlerTest.metodoNoSoportado`): verificado en
vivo, `GET /api/auth/login` ahora da `405`.

**Segunda corrida (tras el fix):**

```
FAIL-NEW: 0   WARN-NEW: 1   PASS: 60
```

`Application Error Disclosure` pasa a `PASS`. El único `WARN` que queda,
`Authentication Request Identified` (10111, informativo — ZAP solo avisa
que detectó campos de contraseña en `/registro` y `/login`), no es un
hallazgo de seguridad.

**Cero hallazgos de severidad Alta o Media entre los que aplican al
código propio.** Dos advertencias Media quedan como decisiones de
alcance aceptadas, no defectos:

- **`HTTP Only Site` (10106):** el escaneo apunta directo al contenedor
  del backend en `:8080`, sin pasar por la capa TLS. El transporte
  cifrado del sistema es una capa aparte (nginx `:8443` en el
  laboratorio, certificado de CA reconocida en producción — RNF-21,
  `a02-tls.txt`), no responsabilidad del backend en sí; escanear `:8080`
  directo es el mismo método que ya usaba el baseline.
- **`Spring Actuator Information Leak` (40042):** `/actuator/health`
  expone el estado de `db` y `redis` sin autenticación. Es
  **intencional**, no un descuido: la observación **OBS-19 / tarea 3.7**
  del docente exige explícitamente "comprueben que el punto de salud
  responde" con evidencia pública de `db:UP, redis:UP` (`README.md`
  §Despliegue público), y Render usa ese mismo endpoint como
  *health check* del despliegue. Ocultar el detalle rompería un criterio
  de aceptación ya cerrado. Se documenta como *trade-off* aceptado, no
  como pendiente.

Reportes completos: `zap-authenticated-report.html` (legible),
`zap-authenticated-report.json`, `zap-authenticated-report.xml`
(máquina), en este mismo directorio. Plan de automatización versionado
en `zap-authenticated.yaml` (el token de sesión real **no** se
commitea — se inyecta en tiempo de ejecución sobre una copia temporal).
