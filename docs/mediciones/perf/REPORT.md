# Reporte de rendimiento — k6 (Bloque C.1)

- Fecha: 2026-07-25T01:50:56.247011+00:00
- Commit: 3fae486
- Herramienta: k6 v2.1.0 (commit/83a87a41e2, go1.26.4, linux/amd64)
- Corridas independientes: 3 (50 VUs, 30 s, seed análisis = 42)

| Corrida | media (ms) | mediana | p90 | p95 | errores | RPS |
|---|---|---|---|---|---|---|
| k6-run1.json | 6.53 | 5.26 | 11.38 | 14.45 | 0.0000 | 404.21 |
| k6-run2.json | 6.67 | 5.94 | 11.63 | 15.32 | 0.0000 | 403.52 |
| k6-run3.json | 6.25 | 5.66 | 10.37 | 12.78 | 0.0000 | 405.99 |

## Agregado entre corridas

- Media del tiempo de respuesta: 6.48 ms (DT 0.21, IC 95 % ± 0.53)
- p95 promedio: 14.18 ms (IC 95 % ± 3.20)
- Throughput: 404.57 RPS (IC 95 % ± 3.16)

Umbral objetivo: p95 < 200 ms con cache caliente; < 500 ms con cache frío (ISO/IEC 25010).
