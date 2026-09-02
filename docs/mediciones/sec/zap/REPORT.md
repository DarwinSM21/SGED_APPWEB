# Escaneo OWASP ZAP baseline (Bloque A.1)

- Fecha: 2026-09-02
- Commit: PENDIENTE-DE-COMMIT
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
