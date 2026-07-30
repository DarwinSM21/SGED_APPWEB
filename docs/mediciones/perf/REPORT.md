# Reporte de rendimiento — k6 (Bloque C.1)

- Fecha: 2026-07-29T23:29:38.425373+00:00
- Commit: dd911e2
- Herramienta: k6 v2.1.0 (commit/83a87a41e2, go1.26.4, linux/amd64)
- Corridas independientes: 3 (50 VUs, 30 s, seed análisis = 42)

| Corrida | media (ms) | mediana | p90 | p95 | errores | RPS |
|---|---|---|---|---|---|---|
| k6-run1.json | 18.58 | 17.41 | 26.07 | 30.03 | 0.0000 | 359.11 |
| k6-run2.json | 23.65 | 19.14 | 42.14 | 54.15 | 0.0000 | 341.96 |
| k6-run3.json | 19.72 | 18.05 | 27.96 | 32.95 | 0.0000 | 355.79 |

## Agregado entre corridas

- Media del tiempo de respuesta: 20.65 ms (DT 2.66, IC 95 % ± 6.60)
- p95 promedio: 39.04 ms (IC 95 % ± 32.71)
- Throughput: 352.29 RPS (IC 95 % ± 22.59)

Umbral objetivo: p95 < 200 ms con cache caliente; < 500 ms con cache frío (ISO/IEC 25010).
