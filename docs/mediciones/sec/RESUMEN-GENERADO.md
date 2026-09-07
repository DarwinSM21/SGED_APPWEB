# Resumen generado — evidencia OWASP cruda

Generado por `scripts/owasp-resumen.py` a partir de los `.txt` de este mismo
directorio. **No reemplaza** la columna "Evidencia observada" de
`tab:owasp`/`tab:acceso-por-recurso` en el informe (esa interpreta y resume;
esto solo extrae lo mecánicamente verificable: códigos HTTP, presencia de
cabeceras de seguridad, eventos de auditoría). Cierra el hueco #1 de
`docs/mediciones/DATA-PROVENANCE.md`: permite verificar por comando que la
transcripción manual no omite ni inventa nada.

| Control | Nombre OWASP | Archivo | Códigos HTTP | Cabeceras de seguridad presentes | Eventos de auditoría |
|---|---|---|---|---|---|
| A01 | Broken Access Control | `a01-acceso-roto.txt` (35 líneas) | `200`×2, `403`×7 | `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Cache-Control` | — |
| A02 | Cryptographic Failures | `a02-tls.txt` (13 líneas) | `200`×1 | (ninguna de las buscadas) | — |
| A03 | Injection | `a03-inyeccion.txt` (19 líneas) | `422`×1 | `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Cache-Control` | — |
| A05 | Security Misconfiguration | `a05-cabeceras.txt` (40 líneas) | `200`×2 | `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Strict-Transport-Security`, `Cache-Control` | — |
| A07 | Identification and Authentication Failures | `a07-rate-limit.txt` (26 líneas) | `401`×5, `429`×2 | `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Cache-Control` | — |
| A09 | Security Logging and Monitoring Failures | `a09-logging.txt` (24 líneas) | (sin código HTTP detectado) | (ninguna de las buscadas) | `AUTH_LOGIN_FAIL`×5, `AUTH_LOGIN_OK`×15 |

Total de archivos analizados: 6.
