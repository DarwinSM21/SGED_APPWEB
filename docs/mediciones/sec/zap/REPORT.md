# Escaneo OWASP ZAP baseline (Bloque A.1)

- Fecha: 2026-08-14
- Commit: 35188d4
- Herramienta: OWASP ZAP (imagen `ghcr.io/zaproxy/zaproxy:stable`), modo
  `zap-baseline.py` (pasivo, sin ataques activos — apropiado para un
  escaneo automatizado de rutina, no un pentest completo)
- Objetivo: `http://localhost:8080/api/docs` (backend, 15 URLs
  descubiertas por el rastreador a partir de la especificación OpenAPI)

## Resultado

```
FAIL-NEW: 0   FAIL-INPROG: 0   WARN-NEW: 7   WARN-INPROG: 0   INFO: 0   IGNORE: 0   PASS: 60
```

**Cero hallazgos de severidad alta.** Los 7 hallazgos de advertencia
(`WARN-NEW`), todos de severidad baja/media, son:

| Regla | Hallazgo | Alcance |
|---|---|---|
| 10003 | Librería JS potencialmente vulnerable | `swagger-ui-bundle.js` (dependencia de terceros de Springdoc/Swagger UI, no código propio) |
| 10049 | Contenido no cacheable marcado como tal | Respuestas `401`/`302` propias del flujo de autenticación — comportamiento esperado, no un defecto |
| 10055 | CSP sin directiva de *fallback* (`default-src`) | Páginas de Swagger UI servidas por el propio Springdoc, fuera del `SecurityConfig` de la aplicación |
| 10063 | Cabecera `Permissions-Policy` ausente | Endpoints de Swagger UI |
| 10096 | Marca de tiempo Unix visible | Dentro de `swagger-ui-standalone-preset.js` (librería de terceros) |
| 10109 | "Aplicación web moderna" (informativo) | Detección heurística de SPA, no es una vulnerabilidad |
| 90004 | Cabecera `Cross-Origin-Embedder-Policy` ausente | Endpoints de Swagger UI |

**Lectura:** los siete hallazgos se concentran en la interfaz de
documentación Swagger UI (una dependencia de terceros, expuesta
intencionalmente sin autenticación para facilitar la revisión del
tribunal) y no en los endpoints de negocio de la API, que quedan fuera
del alcance de estas advertencias. Ninguno compromete confidencialidad,
integridad o disponibilidad de datos de estudiantes. El rastreador
reportó además un error esperado: `GET /` devuelve `401` en vez de
`200` porque no hay sesión autenticada durante el escaneo automatizado
(comportamiento correcto de un endpoint protegido, no un fallo del
escaneo).

Reportes completos: `zap-report.html` (legible), `zap-report.json`,
`zap-report.xml` (máquina), en este mismo directorio.

## Nota de alcance

Este escaneo cubre el backend (`/api/docs` y lo que el rastreador
alcanza desde ahí). No incluye el *frontend* Angular servido por nginx
(`:4200`/`:8443`); una corrida complementaria contra esa URL queda como
trabajo pendiente antes del cierre de la Entrega Final, junto con la
puesta en producción (Bloque A.4).
