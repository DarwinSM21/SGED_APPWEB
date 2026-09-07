# Reporte de rendimiento — k6 (Bloque C.1)

- Fecha: 2026-09-07T05:04:18.744300+00:00
- Commit: b1bdb76
- Herramienta: k6 v2.2.0 (go1.26.5, linux/amd64)
- Escenarios: caché cálida y caché fría; 5 corridas independientes cada uno (50 VUs, 30 s; seed análisis = 42)
- Endpoint: autenticado `GET /api/estudiantes`

## Escenario: Caché cálida

| Corrida | media (ms) | mediana | p90 | p95 | p99 | errores | RPS |
|---|---|---|---|---|---|---|---|
| k6-run1 | 56.66 | 45.56 | 104.87 | 137.08 | 204.28 | 0.0000% | 273.55 |
| k6-run2 | 15.63 | 12.78 | 26.34 | 32.50 | 49.76 | 0.0000% | 370.96 |
| k6-run3 | 10.33 | 9.22 | 14.56 | 17.40 | 25.44 | 0.0000% | 389.64 |
| k6-run4 | 9.33 | 8.75 | 12.07 | 13.99 | 20.05 | 0.0000% | 393.48 |
| k6-run5 | 9.83 | 9.01 | 13.20 | 15.88 | 22.15 | 0.0000% | 390.28 |

**Agregado Caché cálida** (corridas 2--5, tras descartar la primera como calentamiento JIT/de arranque): media 11.28 ms (DT 2.93, IC 95 % ± 4.66); p95 promedio 19.94 ms (± 13.51); p99 promedio 29.35 ms (± 21.93); throughput 386.09 RPS (± 16.27)

## Escenario: Caché fría

| Corrida | media (ms) | mediana | p90 | p95 | p99 | errores | RPS |
|---|---|---|---|---|---|---|---|
| k6-frio1 | 20.39 | 18.54 | 36.02 | 41.72 | 52.53 | 0.0000% | 356.15 |
| k6-frio2 | 18.81 | 15.92 | 34.14 | 39.77 | 50.89 | 0.0000% | 361.16 |
| k6-frio3 | 16.49 | 11.45 | 32.06 | 36.96 | 48.25 | 0.0000% | 367.80 |
| k6-frio4 | 17.22 | 13.06 | 32.58 | 37.43 | 47.51 | 0.0000% | 366.12 |
| k6-frio5 | 18.45 | 16.10 | 33.28 | 38.64 | 50.22 | 0.0000% | 362.33 |

**Agregado Caché fría** (corridas 2--5, tras descartar la primera como calentamiento JIT/de arranque): media 17.74 ms (DT 1.08, IC 95 % ± 1.72); p95 promedio 38.20 ms (± 2.01); p99 promedio 49.22 ms (± 2.54); throughput 364.35 RPS (± 4.97)

## Comparación estadística (caché cálida vs caché fría)

Test no paramétrico de Mann-Whitney (Wilcoxon) bilateral sobre los
tiempos de respuesta. La primera corrida de cada escenario se
descarta como calentamiento (JIT/de arranque, Georges et al.); el
contraste usa por tanto las corridas 2--5 de cada escenario
(n por corrida ≈ 15 000; pool global ≈ 62297 cálida vs
58762 fría). Tamaño de efecto: delta de Cliff (dominancia) y
estadístico A12 de Vargha y Delaney. Corrección por comparaciones
múltiples de Holm-Bonferroni sobre las cuatro corridas.

| Comparación | U | z | p | δ Cliff | A12 | Holm (α=0,05) |
|---|---|---|---|---|---|---|
| corrida-2 | 121675593 | 17.4 | 6.93e-68 | -0.117 | 0.441 | **rechaza** |
| corrida-3 | 155061192 | 49.9 | 2.25e-543 | -0.330 | 0.335 | **rechaza** |
| corrida-4 | 174960074 | 75.1 | 1.42e-1227 | -0.496 | 0.252 | **rechaza** |
| corrida-5 | 171094482 | 73.3 | 3.90e-1168 | -0.486 | 0.257 | **rechaza** |
| pool global | 2479872504 | 106.9 | 1.75e-2483 | -0.355 | 0.323 | — |

El pool combinado (corridas 2--5) da un valor p de 1.75e-2483 (z = 106.9) con A12 = 0.323 (delta de Cliff -0.355); A12 < 0,5 indica que la
caché cálida —primera muestra del contraste— tiende a tiempos
menores que la fría. Las cuatro comparaciones por corrida
sobreviven a la corrección de Holm.

Umbral objetivo: p95 < 200 ms con cache caliente; < 500 ms con cache frío (ISO/IEC 25010).
