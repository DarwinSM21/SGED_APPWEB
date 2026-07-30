# Diccionario de datos de mediciones — SGED ProFútbol

## 1. Rendimiento (`docs/mediciones/perf/`)

### Archivos: `k6-run1.json`, `k6-run2.json`, `k6-run3.json`

| Variable | Tipo | Unidad | Rango esperado | Significado |
|---|---|---|---|---|
| `http_req_duration` | float | ms | 5–200 | Tiempo total de respuesta HTTP (p95 < 200 ms objetivo) |
| `http_req_waiting` | float | ms | 5–150 | Tiempo de espera en servidor (TTFB) |
| `http_req_receiving` | float | ms | 0.03–16 | Tiempo de recepción de respuesta |
| `http_req_sending` | float | ms | 0.008–13 | Tiempo de envío de petición |
| `http_req_blocked` | float | ms | 0.003–7 | Tiempo bloqueado (socket reutilización/DNS) |
| `http_req_connecting` | float | ms | 0–6 | Tiempo de conexión TCP |
| `http_req_tls_handshaking` | float | ms | 0 | Tiempo de handshake TLS (0 por usar HTTP local) |
| `http_req_failed` | float | % | 0 | Tasa de fallos (objetivo: 0%) |
| `http_reqs` | int | req/s | 300–400 | Throughput (RPS) |
| `iterations` | int | iter/s | 300–400 | Iteraciones completadas por segundo |
| `iteration_duration` | float | ms | 107–160 | Duración total por iteración (incluye espera entre requests) |
| `data_sent` | int | bytes | ~6.8M | Datos enviados en la corrida |
| `data_received` | int | bytes | ~19M | Datos recibidos en la corrida |
| `vus` | int | VUs | 4–50 | Número de usuarios virtuales concurrentes |
| `vus_max` | int | VUs | 50 | Máximo de VUs configurado |
| `checks` | int | passes/fails | 0 fails | Verificaciones de estado 200 y contenido no vacío |

### Archivo: `REPORT.md`

| Variable | Tipo | Unidad | Rango esperado | Significado |
|---|---|---|---|---|
| media_tiempo_respuesta | float | ms | 15–25 | Media del tiempo de respuesta entre 3 corridas |
| dt_tiempo_respuesta | float | ms | 2–5 | Desviación típica entre corridas |
| p95_promedio | float | ms | 10–60 | Promedio del percentil 95 entre corridas |
| throughput | float | RPS | 340–370 | Promedio de peticiones por segundo |
| IC_95 | float | ms | ±3–33 | Intervalo de confianza al 95% |

---

## 2. Seguridad (`docs/mediciones/sec/`)

### Archivos: `a01-acceso-roto.txt`, `a02-tls.txt`, `a03-inyeccion.txt`, `a05-cabeceras.txt`, `a07-rate-limit.txt`, `a09-logging.txt`

| Variable | Tipo | Unidad | Rango esperado | Significado |
|---|---|---|---|---|
| HTTP status code | int | código HTTP | 200, 401, 403, 422, 429 | Código de respuesta del control OWASP |
| Content-Type | string | — | `application/problem+json` | Formato de respuesta de error estructurado |
| cabeceras_seguridad | string | — | CSP, HSTS, X-Frame-Options, X-Content-Type-Options | Cabeceras de seguridad presentes en la respuesta |
| detail | string | — | Mensaje descriptivo | Detalle del error (ej. "Credenciales inválidas") |
| status | int | código HTTP | 200–429 | Código HTTP dentro del body ProblemDetails |
| timestamp | datetime | ISO 8601 | — | Momento de la medición |

---

## 3. Usabilidad SUS (`docs/mediciones/sus/`)

### Archivo: `respuestas.csv`

| Variable | Tipo | Unidad | Rango | Significado |
|---|---|---|---|---|
| `participante` | int | — | 1–N | Identificador anonimizado del participante |
| `perfil` | string | — | administrativo, entrenador, externo | Rol del participante |
| `fecha` | date | ISO 8601 | — | Fecha de aplicación |
| `p1`–`p10` | int | Likert | 1–5 | Respuestas al cuestionario SUS (10 ítems) |
| `t1_completada`–`t7_completada` | string | — | si / no | Tareas completadas sin ayuda |
| `observaciones` | string | — | Libre | Notas del aplicador |

### Archivo: `REPORT.md` (generado por `sus-analysis.py`)

| Variable | Tipo | Unidad | Rango | Significado |
|---|---|---|---|---|
| puntuacion_SUS_media | float | puntos | 0–100 | Media del SUS score (objetivo ≥ 68) |
| desviacion_tipica | float | puntos | 0–50 | Dispersión entre participantes |
| IC_95 | float | puntos | ± N | Intervalo de confianza al 95% |
| mediana | float | puntos | 0–100 | Mediana del SUS score |
| minimo | float | puntos | 0–100 | Puntuación mínima observada |
| maximo | float | puntos | 0–100 | Puntuación máxima observada |
| grado | string | — | A–F | Grado según Bangor, Kortum y Miller (2009) |

---

## 4. Calidad web Lighthouse (`docs/mediciones/lighthouse/`)

### Archivos: `run1.report.json`, `run2.report.json`, `run3.report.json`

| Variable | Tipo | Unidad | Rango | Significado |
|---|---|---|---|---|
| `performance` | float | puntuación | 0–1 | Rendimiento general (carga, interactividad) |
| `accessibility` | float | puntuación | 0–1 | Accesibilidad (contraste, ARIA, semántica) |
| `best-practices` | float | puntuación | 0–1 | Buenas prácticas (HTTP/2, sin vulnerabilidades conocidas) |
| `seo` | float | puntuación | 0–1 | Posicionamiento en buscadores (meta tags, crawlability) |
| `fetchTime` | datetime | ISO 8601 | — | Momento de la medición |
| `lighthouseVersion` | string | — | 13.x | Versión de Lighthouse usada |

### Archivo: `REPORT.md`

| Variable | Tipo | Unidad | Rango | Significado |
|---|---|---|---|---|
| performance_score | int | % | 0–100 | Puntuación agregada de rendimiento |
| accessibility_score | int | % | 0–100 | Puntuación agregada de accesibilidad |
| best_practices_score | int | % | 0–100 | Puntuación agregada de buenas prácticas |
| seo_score | int | % | 0–100 | Puntuación agregada de SEO |
| LCP | float | s | 0–4 | Largest Contentful Paint (Web Vital) |
| TBT | float | ms | 0–300 | Total Blocking Time (Web Vital) |
| CLS | float | — | 0–0.25 | Cumulative Layout Shift (Web Vital) |

---

## 5. Cobertura JaCoCo (`docs/mediciones/jacoco/`)

### Archivo: `jacoco.csv`

| Variable | Tipo | Unidad | Rango | Significado |
|---|---|---|---|---|
| `GROUP` | string | — | backend | Módulo del proyecto |
| `PACKAGE` | string | — | — | Paquete Java |
| `CLASS` | string | — | — | Clase evaluada |
| `INSTRUCTION_MISSED` | int | instrucciones | ≥ 0 | Instrucciones bytecode no cubiertas |
| `INSTRUCTION_COVERED` | int | instrucciones | ≥ 0 | Instrucciones bytecode cubiertas |
| `BRANCH_MISSED` | int | ramas | ≥ 0 | Ramas condicionales no cubiertas |
| `BRANCH_COVERED` | int | ramas | ≥ 0 | Ramas condicionales cubiertas |
| `LINE_MISSED` | int | líneas | ≥ 0 | Líneas de código no cubiertas |
| `LINE_COVERED` | int | líneas | ≥ 0 | Líneas de código cubiertas |
| `COMPLEXITY_MISSED` | int | complejidad | ≥ 0 | Complejidad ciclomática no cubierta |
| `COMPLEXITY_COVERED` | int | complejidad | ≥ 0 | Complejidad ciclomática cubierta |
| `METHOD_MISSED` | int | métodos | ≥ 0 | Métodos no cubiertos |
| `METHOD_COVERED` | int | métodos | ≥ 0 | Métodos cubiertos |

### Cobertura global (derivada de jacoco.csv)

| Variable | Tipo | Unidad | Rango | Significado |
|---|---|---|---|---|
| cobertura_instrucciones | float | % | 0–100 | Porcentaje de instrucciones cubiertas |
| cobertura_ramas | float | % | 0–100 | Porcentaje de ramas cubiertas |
| cobertura_lineas | float | % | 0–100 | Porcentaje de líneas cubiertas |
| cobertura_metodos | float | % | 0–100 | Porcentaje de métodos cubiertos |