# Reporte de calidad web — Lighthouse (Bloque C.5)

- **Fecha:** 2026-07-29
- **Commit base:** `98bab0d`
- **Herramienta:** Lighthouse v13.4.1 (CLI)
- **URL medida:** `http://localhost:4200` (contenedor `sged_frontend` recién reconstruido)
- **Perfil:** móvil (412 × 823, DPR 1.75), throttling simulado
- **Corridas:** 3 independientes
- **Configuración:** [`lighthouserc.js`](../../../lighthouserc.js)

## Resultados por categoría

| Categoría | Run 1 | Run 2 | Run 3 | Media | Umbral | Estado |
|---|---|---|---|---|---|---|
| Rendimiento | 93 | 92 | 92 | **92,3** | ≥ 80 | ✅ Cumple |
| Accesibilidad | 100 | 100 | 100 | **100** | ≥ 90 | ✅ Cumple |
| Buenas prácticas | 96 | 96 | 96 | **96** | ≥ 90 | ✅ Cumple |
| SEO | 63 | 63 | 63 | **63** | ≥ 90 | ⚠️ Ver §3 |

## Métricas Web Vitals

| Métrica | Run 1 | Run 2 | Run 3 | Media |
|---|---|---|---|---|
| First Contentful Paint | 2,38 s | 2,43 s | 2,46 s | **2,43 s** |
| Largest Contentful Paint | 2,81 s | 2,85 s | 2,90 s | **2,86 s** |
| Speed Index | 2,38 s | 2,43 s | 2,46 s | **2,43 s** |
| Total Blocking Time | 23 ms | 14 ms | 40 ms | **25,7 ms** |
| Cumulative Layout Shift | 0,000 | 0,000 | 0,000 | **0,000** |

CLS de 0,000 en las tres corridas: la interfaz no produce ningún
desplazamiento de contenido durante la carga.

---

## 1. Defectos detectados y corregidos en esta medición

La primera corrida expuso tres defectos reales del frontend, que fueron
corregidos y vueltos a medir:

| # | Defecto | Corrección | Efecto medido |
|---|---|---|---|
| 1 | `<html lang="en">` en una aplicación íntegramente en español | `lang="es"` en `frontend/src/index.html` | Auditoría `html-has-lang` correcta |
| 2 | `<title>Frontend</title>` (placeholder de Angular CLI) y sin `meta description` | Título real y descripción en `index.html` | `document-title` y `meta-description` pasan a 1,0 |
| 3 | Sin elemento `<main>`: la auditoría `landmark-one-main` fallaba | `<main>` envolviendo el `<router-outlet />` en `app.html` | **Accesibilidad 97 → 100** |

También se corrigió un defecto en la propia configuración de medición:
`lighthouserc.js` declaraba `preset: 'mobile'`, valor que no existe en
Lighthouse (los válidos son `perf`, `experimental`, `desktop`). Eso abortaba
toda corrida con código de salida 1, por lo que **esta medición nunca se
había podido ejecutar antes**. Se sustituyó por `formFactor: 'mobile'` más
`screenEmulation` explícita.

---

## 2. Evolución de las puntuaciones

| Categoría | Antes de las correcciones | Después |
|---|---|---|
| Rendimiento | 92 | 92,3 |
| Accesibilidad | 97 | **100** |
| Buenas prácticas | 96 | 96 |
| SEO | 82 | 63 (ver §3) |

---

## 3. Sobre el puntaje SEO: por qué 63 es el resultado correcto

**El descenso de 82 a 63 es intencional y deseable.**

Al corregir el defecto «`robots.txt` no es válido» se añadió un
`frontend/public/robots.txt` real con:

```
User-agent: *
Disallow: /
```

Lighthouse penaliza eso con la auditoría `is-crawlable` («Page is blocked
from indexing»), que por sí sola vale −27 puntos de la categoría SEO. Esa
categoría **asume que el sitio quiere ser indexado por buscadores**.

SGED es una aplicación de **gestión interna** que trata datos personales de
**menores de edad** (nombres, fechas de nacimiento, asistencia con hora y
lugar, evaluaciones individuales de desempeño). Que sea indexable por
Google sería un defecto de privacidad, no una virtud. Ver
[`docs/etica/ETHICS.md`](../../etica/ETHICS.md) §1 y §3.2.

Por eso, en `lighthouserc.js`:

- el umbral agregado de la categoría SEO pasó de `error` a `warn`, con la
  justificación escrita en el propio archivo;
- se mantienen como **error bloqueante** las auditorías SEO que sí aplican a
  una aplicación interna, y todas ellas pasan:

| Auditoría SEO aplicable | Puntaje |
|---|---|
| `meta-description` | 1,0 ✅ |
| `document-title` | 1,0 ✅ |
| `html-has-lang` | 1,0 ✅ |
| `viewport` | 1,0 ✅ |
| `is-crawlable` | 0,0 — **intencional** |

Optimizar hacia un SEO de 90 exigiría eliminar el `robots.txt`, es decir,
degradar deliberadamente la privacidad del sistema para mejorar una métrica.
Se optó por lo contrario y por documentarlo.

---

## 4. Hallazgo abierto

**`errors-in-console` (buenas prácticas, 96/100).** Al cargar la aplicación
sin sesión iniciada, el guard de rutas consulta `GET /api/auth/me`, que
responde `401` — comportamiento correcto y esperado (RF-05) — y el navegador
registra ese `401` como error de consola.

No se modificó, porque suprimirlo implicaría alterar el flujo de
autenticación por una métrica cosmética. Queda documentado como deuda menor:
podría resolverse haciendo que el guard consulte el estado de sesión sin
provocar una respuesta de error, por ejemplo con un endpoint que devuelva
`200` con `{autenticado: false}`.

---

## 5. Reproducción

Con el stack levantado (`make up`):

```bash
npx lighthouse http://localhost:4200 \
  --output=json --output=html \
  --output-path=./docs/mediciones/lighthouse/run1 \
  --form-factor=mobile --throttling-method=simulate \
  --screenEmulation.mobile --screenEmulation.width=412 \
  --screenEmulation.height=823 --screenEmulation.deviceScaleFactor=1.75 \
  --chrome-flags="--headless=new --no-sandbox --disable-gpu"
```

> **Nota de entorno (Windows).** Tras generar y guardar los informes,
> `chrome-launcher` falla al borrar su perfil temporal con
> `EPERM: \\?\C:\Users\...\Temp\lighthouse.NNNNN`. Es un fallo de limpieza
> **posterior** a la escritura de los resultados: los archivos
> `runN.report.json` y `runN.report.html` quedan completos y son válidos.
> Por ese mismo motivo `lhci autorun` no llega a archivar nada en Windows, y
> se usa el CLI directo con `--output-path`.

## 6. Artefactos

| Archivo | Contenido |
|---|---|
| `run1.report.json` | Corrida 1 — evidencia cruda completa |
| `run2.report.json` | Corrida 2 — evidencia cruda completa |
| `run3.report.json` | Corrida 3 — evidencia cruda completa |
| `run1.report.html` | Informe navegable de la corrida 1 |

Se versionan los tres JSON (evidencia cruda, de la que se puede recomputar
toda la tabla de este reporte) y un único HTML como muestra legible. Los HTML
de las corridas 2 y 3 se omiten por redundancia: pesan ~470 KB cada uno y su
contenido es reproducible desde el JSON correspondiente.
