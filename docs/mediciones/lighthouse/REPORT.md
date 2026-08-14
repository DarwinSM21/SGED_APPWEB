# Reporte de calidad web — Lighthouse (Bloque C.5 / A.1)

- **Fecha:** 2026-08-14
- **Commit base:** `35188d4`
- **Herramienta:** Lighthouse v13.4.1 (CLI), conectado a un Chrome
  124 sin cabeza corriendo en contenedor Docker (`zenika/alpine-chrome`),
  ya que este entorno de ejecución no tiene un navegador local instalable.
- **URL medida:** `https://localhost:8443` (frontend `sged_frontend`,
  certificado TLS autofirmado — se acepta explícitamente para la
  medición, igual que hace un navegador real tras la advertencia)
- **Perfiles:** móvil (412×823, DPR 1.75, `throttlingMethod: simulate`,
  igual configuración que `lighthouserc.js`) y escritorio (preset
  oficial `--preset=desktop` de Lighthouse: 1350×940, sin *emulation*
  de red/CPU adicional)
- **Corridas:** 3 independientes por perfil (6 en total) — cumple el
  mínimo de tres por perfil que exige el Bloque A.1 de la Entrega Final;
  la medición anterior (Tercera Entrega) solo cubría el perfil móvil.

## Resultados por categoría

### Perfil móvil

| Categoría | Run 1 | Run 2 | Run 3 | Media | Umbral | Estado |
|---|---|---|---|---|---|---|
| Rendimiento | 82 | 82 | 82 | **82,0** | ≥ 80 | ✅ Cumple |
| Accesibilidad | 100 | 100 | 100 | **100** | ≥ 90 | ✅ Cumple |
| Buenas prácticas | 96 | 96 | 96 | **96** | ≥ 90 | ✅ Cumple |
| SEO | 63 | 63 | 63 | **63** | ≥ 90 (*warn*) | ⚠️ Ver nota |

### Perfil escritorio

| Categoría | Run 1 | Run 2 | Run 3 | Media | Umbral | Estado |
|---|---|---|---|---|---|---|
| Rendimiento | 99 | 99 | 99 | **99,0** | ≥ 80 | ✅ Cumple |
| Accesibilidad | 100 | 100 | 100 | **100** | ≥ 90 | ✅ Cumple |
| Buenas prácticas | 96 | 96 | 96 | **96** | ≥ 90 | ✅ Cumple |
| SEO | 63 | 63 | 63 | **63** | ≥ 90 (*warn*) | ⚠️ Ver nota |

Desviación típica de 0 en las tres corridas de cada perfil: es un
resultado esperado, no un defecto de medición — con
`throttlingMethod: simulate`, Lighthouse deriva los tiempos a partir de
un único *trace* real más un modelo de red/CPU determinista (Lantern),
en vez de aplicar limitación real variable en cada corrida, así que la
variabilidad entre corridas es mínima por diseño del método.

## Nota sobre el umbral de rendimiento en escritorio (corrección metodológica)

La primera corrida de escritorio de esta sesión, con una configuración
manual de *throttling* calcada de la del perfil móvil, midió
**62/100** de rendimiento — por debajo del umbral. Antes de reportarlo
como un hallazgo real se verificó la causa: forzar
`throttlingMethod: simulate` con los multiplicadores de red/CPU del
perfil móvil sobre un `form-factor: desktop` no es una configuración
válida de Lighthouse (aplica un modelo de limitación pensado para móvil
a una medición de escritorio). Repetida la corrida con el preset
oficial `--preset=desktop` de la propia herramienta, el resultado sube
a 99/100 de forma consistente en las tres corridas. Se documenta el
descarte en vez de omitirlo, siguiendo la misma disciplina que ya
aplicó la Tercera Entrega con las tres mediciones defectuosas
corregidas en su momento (JaCoCo, auditoría A01, registro de usuarios).

## Nota sobre SEO (umbral relajado deliberadamente)

Igual que documenta `lighthouserc.js`: SGED es una aplicación de
gestión interna que trata datos personales de menores de edad, por lo
que `public/robots.txt` declara `Disallow: /`. La auditoría
`is-crawlable` de Lighthouse penaliza eso (−27 puntos) porque su
categoría SEO asume que el sitio *quiere* ser indexado por buscadores —
aquí es correcto justamente lo contrario (`docs/etica/ETHICS.md`). El
umbral de SEO se mantiene como advertencia (*warn*), no como error, y
las auditorías SEO que sí aplican al caso (`meta-description`,
`document-title`, `html-has-lang`, `viewport`) se verifican en modo
estricto y pasan en las seis corridas.

## Métricas Web Vitals (perfil móvil, run 1)

Ver `mobile-run1.report.html` para el desglose completo de First
Contentful Paint, Largest Contentful Paint, Total Blocking Time y
Cumulative Layout Shift.
