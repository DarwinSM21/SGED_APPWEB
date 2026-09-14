# Expediente de verificación (EV-1)

Guía del examen suspenso, UTEQ — Aplicaciones Web, PPA 2026-2027.
Por cada uno de los 14 pendientes: identificador, orden exacta, salida
pegada tal cual (corrida el 2026-09-14 sobre el commit indicado abajo) y
ruta del archivo que la respalda.

**Cómo reproducir todo de una vez:** `make verify` (equivalente a
`bash scripts/verify.sh`). Ese objetivo es EV-2: se puede correr entero
desde un clon limpio y su código de salida es 0 solo si todo pasa.

Commit sobre el que se corrió esta versión del expediente:
`b18ed4680f1c71792535ce02d672a8d292350d75`.

> **Nota de método.** Varios de los 14 pendientes que describe la guía ya
> tenían trabajo sustantivo hecho en el repositorio al momento de escribir
> este expediente (ver detalle en cada punto). Donde eso ocurre, lo digo
> explícitamente con la orden y la salida real que lo demuestra, en vez de
> asumir que hay que rehacerlo. Lo que de verdad falta queda marcado como
> **FALTA** con la orden que lo va a comprobar una vez cerrado.

---

## P1 — Respuestas del SUS (peso 1,3)

**Orden:**
```bash
n=$(($(wc -l < docs/mediciones/sus/respuestas.csv) - 1)); echo "participantes: $n"
grep -i "brooke" docs/mediciones/sus/REPORT.md
grep -i "t de Student" docs/mediciones/sus/REPORT.md
```

**Salida:**
```
participantes: 15
- Instrumento: System Usability Scale (Brooke, 1996), 10 items, escala 1-5
- Brooke, J. (1996). *SUS: A quick and dirty usability scale.*
- Metodo del IC | t de Student, gl=14, t=2.145
```

**Respalda:** [`docs/mediciones/sus/respuestas.csv`](docs/mediciones/sus/respuestas.csv), [`docs/mediciones/sus/REPORT.md`](docs/mediciones/sus/REPORT.md), [`docs/mediciones/sus/INTERPRETACION.md`](docs/mediciones/sus/INTERPRETACION.md)

**Estado:** los 15 registros reales, el recálculo Brooke y el IC 95% con t
de Student ya están hechos. Lo único que falta para que la medición sea
usable es el consentimiento de cada participante — **ver P13**, que
bloquea este punto.

---

## P2 — Lighthouse (peso 0,9)

**Orden:**
```bash
ls docs/mediciones/lighthouse/mobile-run*.report.json docs/mediciones/lighthouse/desktop-run*.report.json | wc -l
grep -n "onrender" docs/mediciones/lighthouse/REPORT.md
```

**Salida:**
```
6
88:- **URL medida:** `https://sged-frontend-jofa.onrender.com` (despliegue
89:  público de Render) — las doce evidencias tienen `requestedUrl` y
90:  `finalUrl` en esa URL pública (nada terminó redirigida a `/login`)
```

**Respalda:** [`docs/mediciones/lighthouse/`](docs/mediciones/lighthouse/) (3 corridas móvil + 3 escritorio + evidencias adicionales de dashboard/inventario)

**Estado:** hecho. 3 corridas por perfil contra el despliegue público, JSON versionados.

---

## P3 — DOI retirado (peso 0,5)

**Orden:** `bash scripts/check-doi.sh`

**Salida:**
```
OK   10.5281/zenodo.21713239 -> 200
OK   10.5281/zenodo.22422305 -> 200
OK   10.5281/zenodo.22635766 -> 410 (retirado, documentado como tal; no se cita como vigente)
OK   10.5281/zenodo.22714477 -> 200
OK   10.5281/zenodo.22730565 -> 200
OK   10.5281/zenodo.22739944 -> 200
```

**Respalda:** [`scripts/check-doi.sh`](scripts/check-doi.sh), [`README.md`](README.md), [`CITATION.cff`](CITATION.cff)

**Estado:** hecho. Todos los DOI vigentes resuelven a 200. El DOI retirado
(`zenodo.22635766`) resuelve a 410 y el README ya explica que quedó
tombstone y que no debe citarse.

---

## P4 — Javadoc (peso 1,5)

**Orden:** `python3 scripts/javadoc-coverage.py 90`

**Salida:**
```
Metodos/constructores publicos encontrados: 612
Con Javadoc inmediatamente encima: 551
Cobertura: 90.0%  (umbral exigido: 90%)
Lista de 61 metodos sin Javadoc: docs/mediciones/javadoc-sin-documentar.txt
RESULTADO: PASA
```

**Respalda:** [`scripts/javadoc-coverage.py`](scripts/javadoc-coverage.py), [`docs/mediciones/javadoc-sin-documentar.txt`](docs/mediciones/javadoc-sin-documentar.txt)

**Estado:** cruza el umbral (90,03% real), pero por muy poco margen — el
contador de este script (612 métodos públicos) no coincide con el de la
guía (463). Antes de dar el punto por cerrado, el equipo debería:
(a) confirmar `mvn javadoc:javadoc` sin error también, y
(b) documentar los 61 métodos que quedan en la lista para tener margen real
y no depender de una heurística de conteo que puede diferir de la del
docente.

---

## P5 — Validador de trazabilidad (peso 0,6)

**Orden:**
```bash
bash scripts/validate-traceability.sh /ruta/inexistente.csv /ruta/inexistente.md; echo "exit=$?"
bash scripts/test-validate-traceability.sh; echo "exit=$?"
```

**Salida:**
```
exit=1
[... autotest de scripts/test-validate-traceability.sh ...] exit=0
```

**Respalda:** [`scripts/validate-traceability.sh`](scripts/validate-traceability.sh) (usa `exec`, por lo que ya propaga el código de salida de `validate-traceability.py`)

**Estado:** hecho. El script sí falla con código distinto de cero; el
defecto real era que nada lo invocaba automáticamente — `make verify`
(este mismo expediente) ya lo hace en cada corrida.

---

## P6 — Figuras en inglés (peso 0,7)

**Orden:** ver la sección `P6` de `scripts/verify.sh` (barrido de
`docs/diagramas/*.md` dentro de los bloques ```mermaid```, `docs/diagramas/*.svg`
y `docs/arquitectura/workspace.dsl` contra un diccionario de términos en
español).

**Salida:**
```
PASA: sin coincidencias del diccionario de terminos en español dentro de mermaid/svg/dsl
```

**Respalda:** [`docs/diagramas/diagrama-clases.md`](docs/diagramas/diagrama-clases.md), [`docs/diagramas/mer-profutbol.svg`](docs/diagramas/mer-profutbol.svg), [`docs/arquitectura/workspace.dsl`](docs/arquitectura/workspace.dsl)

**Estado:** las fuentes de las figuras (Mermaid, SVG del MER, DSL de C4) ya
están en inglés. **Pendiente de revisión manual:** los PNG del modelo C4 y
del MER son texto rasterizado — no se puede grepear — así que alguien del
equipo debe abrirlos y confirmarlos a simple vista antes de cerrar el
punto (están regenerados desde las mismas fuentes que ya pasan el barrido,
así que debería coincidir, pero no está comprobado automáticamente).

---

## P7 — SRS firmado, con MoSCoW (peso 0,8)

**Orden:**
```bash
grep -c "MoSCoW:" docs/requisitos/SRS.md
ls docs/requisitos/ACTA-APROBACION-SRS-v1.8.pdf
ls docs/requisitos/SRS-v1.1.0.pdf
```

**Salida:**
```
80
docs/requisitos/ACTA-APROBACION-SRS-v1.8.pdf
ls: cannot access 'docs/requisitos/SRS-v1.1.0.pdf': No such file or directory
```

**Respalda:** [`docs/requisitos/SRS.md`](docs/requisitos/SRS.md), [`docs/requisitos/ACTA-APROBACION-SRS-v1.8.pdf`](docs/requisitos/ACTA-APROBACION-SRS-v1.8.pdf)

**Estado — FALTA:** el MoSCoW ya está explícito en 80 requisitos y la firma
del docente-director ya existe (acta v1.8, 2026-09-12). Lo que falta es
puramente de nomenclatura: regenerar/copiar el PDF como
`docs/requisitos/SRS-v1.1.0.pdf` una vez exista el commit final y la
etiqueta `v1.1.0` (P8).

---

## P8 — Etiqueta única v1.1.0 (peso 0,6)

**Orden:**
```bash
git rev-parse -q --verify refs/tags/v1.1.0
grep v1.1.0 CITATION.cff
```

**Salida:**
```
(sin salida: la etiqueta no existe)
(sin coincidencias en CITATION.cff)
```

**Respalda:** `git tag -l` (hoy: `v1.0.0`, `v1.0.1`, `v1.0.2`, `v1.0.3`, más las de entregas previas)

**Estado — FALTA.** Esta es la última acción antes de entregar, no la
primera: hay que crear `v1.1.0` sobre el commit final que se va a
defender, actualizar `CITATION.cff` y la portada, y **entonces** regenerar
`SRS-v1.1.0.pdf` (P7) y el DOI de Zenodo sobre ese mismo commit (P3, que ya
tiene el mecanismo probado con `b18ed46`).

---

## P9 — Nombres de tipos en inglés (peso 0,7)

**Orden:** ver la sección `P9` de `scripts/verify.sh` (extrae todas las
declaraciones `class/interface/enum/record` de `backend/src/main/java` y
las cruza contra un diccionario de términos en español).

**Salida:**
```
0 de 274 tipos (0.0%) coinciden con el diccionario de terminos en español
PASA: 0.0% <= 5%
```

**Respalda:** `backend/src/main/java/**/*.java`

**Estado:** con el diccionario usado aquí, no se encontró ningún tipo con
nombre en español. La guía reporta 32/277 (11,6%) — la diferencia puede
ser un diccionario distinto (términos que mi lista no cubre) o que ya se
haya corregido. **Antes de dar el punto por cerrado, alguien del equipo
debería revisar la lista completa de 274 tipos a ojo** (no cuesta nada y
cierra la duda con certeza, a diferencia de fiarse de un grep).

---

## P10 — Roles CRediT con conteo real (peso 0,5)

**Orden:** `grep "Funding acquisition" CONTRIBUTORS.md`

**Salida:**
```
| Funding acquisition | — | No aplica (proyecto académico sin financiación externa). |
```

**Respalda:** [`CONTRIBUTORS.md`](CONTRIBUTORS.md)

**Estado — revisión manual pendiente.** Los 14 roles están cubiertos y
justificados, pero el número junto a cada persona en la tabla de roles es
su **total de commits**, repetido en cada fila donde aparece — no un
conteo específico de ese rol. Eso es probablemente lo que la guía objeta
con "sin conteos reales". Se soluciona reescribiendo esa columna con una
cifra propia de cada rol (p. ej. archivos o commits que sostienen
específicamente ese tipo de contribución), no repitiendo el total.

---

## P11 — Clave de ejemplo (peso 0,4)

**Orden:** `grep "^JWT_SECRET=" .env.example`

**Salida:**
```
JWT_SECRET=SGED_2026_SECRET_KEY_MUY_LARGA_Y_SEGURA_123456789
```

**Respalda:** [`.env.example`](.env.example)

**Estado — FALTA.** No tiene ningún marcador evidente (`CAMBIAR_EN_...`,
`<reemplazar>`, etc.). Cambio de una línea.

---

## P12 — Umbral de cobertura unificado (peso 0,5)

**Orden:**
```bash
grep "<minimum>" backend/pom.xml
grep -n "umbral" docs/informe/main.tex | grep -i cobertura
```

**Salida:**
```
<minimum>0.70</minimum>  (LINE)
<minimum>0.70</minimum>  (BRANCH)
[... todas las menciones de "umbral" + "cobertura" en el informe citan 70% / 0,70 ...]
```

**Respalda:** [`backend/pom.xml`](backend/pom.xml), [`docs/informe/main.tex`](docs/informe/main.tex)

**Estado:** consistente en todo lo revisado — pom.xml y el informe citan
70% en todas las menciones encontradas. Incluso hay un commit histórico
(`76e4e48`) que corrigió exactamente esta inconsistencia en el pasado.

---

## P13 — Consentimientos informados del SUS (peso 0,5)

**Orden:**
```bash
find docs/etica/consentimiento -type f
```

**Salida:**
```
docs/etica/consentimiento/plantilla.md
docs/etica/consentimiento/representante.md
```

**Respalda:** [`docs/etica/consentimiento/`](docs/etica/consentimiento/)

**Estado — FALTA.** Existe el modelo de consentimiento (plantilla), pero
no hay constancia de aceptación individual para ninguno de los 15
participantes de `respuestas.csv`. Esto bloquea también a P1: sin
consentimiento, la medición SUS no se puede dar por válida según la propia
guía.

---

## P14 — Estadística con trazabilidad (peso 0,5)

**Orden:**
```bash
find scripts -iname "*pvalue*" -o -iname "*p-valor*" -o -iname "*bonferroni*" -o -iname "*holm*"
```

**Salida:**
```
(sin resultados)
```

**Respalda:** —

**Estado — FALTA.** No existe ningún script o cuaderno versionado que
calcule p-valores corregidos por comparaciones múltiples a partir de los
datos crudos. Hay que construirlo desde cero.

---

## Regresión — sección 1 (lo que la guía ya da por resuelto)

`make verify` incluye una guarda mínima (PDF del informe existe, README
declara URL públicas, CORS sin comodines) para detectar si un cambio
futuro rompe algo de la sección 1. **No sustituye** correr `make test`
(JaCoCo), `make bench` (k6) y `make audit` (ZAP/SQL dinámico) antes de la
entrega final — esos objetivos ya existían y siguen siendo la fuente real
de esos números; `make verify` es deliberadamente rápido y no depende de
Docker para poder correrse todas las veces que haga falta mientras se
cierran los pendientes.

---

## Resumen de esta corrida

| # | Estado |
|---|---|
| P1 | Hecho — bloqueado por P13 |
| P2 | Hecho |
| P3 | Hecho |
| P4 | Pasa (90,03%, margen corto) |
| P5 | Hecho |
| P6 | Pasa en fuentes — falta revisión visual de los PNG |
| P7 | Falta el PDF renombrado v1.1.0 |
| P8 | Falta crear la etiqueta |
| P9 | Pasa con el diccionario usado — confirmar a mano |
| P10 | Falta corregir los conteos por rol |
| P11 | Falta el marcador en `.env.example` |
| P12 | Consistente |
| P13 | Falta — constancias de consentimiento |
| P14 | Falta — script de corrección estadística |

`bash scripts/verify.sh` / `make verify`: **18 comprobaciones pasan, 6
fallan, 1 requiere revisión manual** (corrida el 2026-09-14 sobre
`b18ed4680f1c71792535ce02d672a8d292350d75`). Código de salida: 1 (correcto:
todavía hay pendientes reales).
