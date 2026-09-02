# Reporte de rendimiento — k6 (Bloque C.1)

- Fecha: 2026-08-14T18:44:22.770796+00:00
- Commit: 73d5114
- Herramienta: k6 v2.2.0 (commit/00a9a1b7f5, go1.26.5, linux/amd64)
- Corridas independientes: 5 (50 VUs, 30 s, seed análisis = 42)

| Corrida | media (ms) | mediana | p90 | p95 | errores | RPS |
|---|---|---|---|---|---|---|
| k6-run1.json | 7.15 | 6.88 | 9.04 | 9.87 | 0.0000 | 403.64 |
| k6-run2.json | 7.04 | 6.74 | 9.16 | 10.04 | 0.0000 | 404.73 |
| k6-run3.json | 6.96 | 6.58 | 9.06 | 10.13 | 0.0000 | 404.46 |
| k6-run4.json | 6.97 | 6.68 | 8.81 | 9.62 | 0.0001 | 287.88 |
| k6-run5.json | 6.96 | 6.62 | 8.98 | 9.87 | 0.0000 | 404.88 |

## Agregado entre corridas

- Media del tiempo de respuesta: 7.02 ms (DT 0.08, IC 95 % ± 0.10)
- p95 promedio: 9.91 ms (IC 95 % ± 0.25)
- Throughput: 381.12 RPS (IC 95 % ± 64.71)

Umbral objetivo: p95 < 200 ms con cache caliente; < 500 ms con cache frío (ISO/IEC 25010).
