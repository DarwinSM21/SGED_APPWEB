# Reporte de rendimiento — k6 (Bloque C.1)

- Fecha: 2026-07-22T02:42:01.886808+00:00
- Commit: c5aedc7
- Corridas independientes: 3 (50 VUs, 30 s, seed análisis = 42)

| Corrida | media (ms) | mediana | p90 | p95 | errores | RPS |
|---|---|---|---|---|---|---|
| k6-run1.json | 8.01 | 6.98 | 13.60 | 16.74 | 0.0000 | 398.80 |
| k6-run2.json | 7.68 | 7.59 | 12.60 | 14.87 | 0.0000 | 399.80 |
| k6-run3.json | 7.63 | 7.25 | 11.91 | 14.74 | 0.0000 | 400.04 |

## Agregado entre corridas

- Media del tiempo de respuesta: 7.78 ms (DT 0.21, IC 95 % ± 0.51)
- p95 promedio: 15.45 ms (IC 95 % ± 2.78)
- Throughput: 399.55 RPS (IC 95 % ± 1.63)

Umbral objetivo: p95 < 200 ms con cache caliente; < 500 ms con cache frío (ISO/IEC 25010).
