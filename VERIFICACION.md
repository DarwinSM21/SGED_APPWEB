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

**Orden:** `python3 scripts/credit-counts.py`

**Salida:**
```
Rol                            Pallo Pinto Alejandro          Velez Lopez Ricardo             Arcalle Grefa Darwin
Conceptualization              26                              6                               40
Data curation                  49                              6                               27
Formal analysis                11                              3                               4
Investigation                  3                                0                               2
Methodology                    4                                2                               2
Resources                      12                              7                               14
Software                       128                             11                              50
Validation                     72                              9                               41
Visualization                  4                                3                               6
Writing – original draft       60                              14                              55
Writing – review & editing     87                              24                              87

No cuantificables por ruta de archivo (declarar aparte, criterio cualitativo):
  - Project administration
  - Supervision
  - Funding acquisition
```

**Respalda:** [`CONTRIBUTORS.md`](CONTRIBUTORS.md), [`scripts/credit-counts.py`](scripts/credit-counts.py)

**Estado:** hecho. El defecto real (el número junto a cada persona era su
total de commits, repetido en cada fila) queda corregido: ahora es el
conteo de commits de esa persona que tocaron al menos un archivo de las
rutas declaradas para ese rol, con el mapeo rol→rutas explícito y
editable en el script. Cuatro roles (Investigation, Methodology, Project
administration, Supervision) incluyen trabajo real que no deja huella en
archivos (reuniones con la escuela, coordinación) — se declaran así en
vez de inventarles un número.

---

## P11 — Clave de ejemplo (peso 0,4)

**Orden:** `grep "^JWT_SECRET=" .env.example`

**Salida:**
```
JWT_SECRET=CAMBIAR_EN_PRODUCCION_min_32_caracteres_aleatorios
```

**Respalda:** [`.env.example`](.env.example)

**Estado:** hecho. Se reemplazó el valor con aspecto real por un
marcador evidente (`CAMBIAR_EN_PRODUCCION_...`). No hay ninguna otra
referencia al valor anterior en el repositorio (comprobado con
`grep -rn "SGED_2026_SECRET_KEY_MUY_LARGA"`, sin resultados).

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
grep -cE '^\| ENC-' docs/etica/consentimiento/registro.md
grep -cE '^\| ENC-[0-9]+ \|[^|]*\|[^|]*\| PENDIENTE \|' docs/etica/consentimiento/registro.md
```

**Salida:**
```
15
15
```

**Respalda:** [`docs/etica/consentimiento/registro.md`](docs/etica/consentimiento/registro.md), [`docs/etica/consentimiento/plantilla.md`](docs/etica/consentimiento/plantilla.md)

**Estado — FALTA, y es trabajo humano, no automatizable.** El modelo de
consentimiento (`plantilla.md`) ya existía. Se agregó
`docs/etica/consentimiento/registro.md`: una fila por cada uno de los 15
participantes reales (mismos identificadores anónimos que
`respuestas.csv`), con columnas para marcar cuándo se obtuvo la
constancia y dónde queda archivado el original — sin exponer nombres ni
firmas en el repositorio público, igual que exige el propio diseño de
`plantilla.md`.

Las 15 filas están en `PENDIENTE` a propósito: **no se pueden marcar
`OBTENIDO` sin que cada uno de los 15 participantes reales acepte y
firme.** Inventar esa aceptación sería fabricar una respuesta de
encuesta — Piso 3 = cero directo, sin segunda oportunidad. Las encuestas
ya se administraron (2026-07-30 y 2026-08-18); si en su momento no se
archivó la constancia firmada, `registro.md` explica las dos opciones
reales: volver a contactar a cada participante para formalizar el
consentimiento ya otorgado verbalmente, o declararle al docente-director
que algún participante no está disponible — nunca forzar o inventar la
fila. Esto también bloquea a P1: sin consentimiento, la guía no da por
válida la medición SUS aunque el resto (15 respuestas, Brooke, IC t de
Student) ya esté hecho.

---

## P14 — Estadística con trazabilidad (peso 0,5)

**Orden:** `grep -n "holm_bonferroni" scripts/perf-analysis.py`

**Salida:**
```
193:def holm_bonferroni(log10p_vals, alfa=0.05):
289:    rechasos, p_aj = holm_bonferroni(pvals)
```

Tabla ya generada en [`docs/mediciones/perf/REPORT.md`](docs/mediciones/perf/REPORT.md):

```
| Comparación | U | z | p | δ Cliff | A12 | Holm (α=0,05) |
|---|---|---|---|---|---|---|
| corrida-2 | 121675593 | 17.4 | 6.93e-68 | -0.117 | 0.441 | **rechaza** |
| corrida-3 | 155061192 | 49.9 | 2.25e-543 | -0.330 | 0.335 | **rechaza** |
| corrida-4 | 174960074 | 75.1 | 1.42e-1227 | -0.496 | 0.252 | **rechaza** |
| corrida-5 | 171094482 | 73.3 | 3.90e-1168 | -0.486 | 0.257 | **rechaza** |
```

**Respalda:** [`scripts/perf-analysis.py`](scripts/perf-analysis.py) (calcula Mann-Whitney + delta de Cliff + A12 + Holm-Bonferroni desde `docs/mediciones/perf/*.samples.json`, datos crudos de k6), [`docs/mediciones/perf/REPORT.md`](docs/mediciones/perf/REPORT.md)

**Estado:** hecho — esto también estaba resuelto y mi primer barrido no
lo encontró porque busqué por *nombre de archivo* (`*bonferroni*`,
`*holm*`) y el script se llama `perf-analysis.py`. Reproducible corriendo
`make bench` (que termina llamando a este script) desde un clon limpio
con el sistema en marcha.

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
| P4 | Pasa (90,03%, margen corto — revisión manual recomendada) |
| P5 | Hecho |
| P6 | Pasa en fuentes — falta revisión visual de los PNG (manual) |
| P7 | **Falta** el PDF renombrado v1.1.0 (depende de P8) |
| P8 | **Falta** crear la etiqueta |
| P9 | Pasa con el diccionario usado — confirmar a mano (manual) |
| P10 | Hecho |
| P11 | Hecho |
| P12 | Consistente |
| P13 | **Falta** — constancias de consentimiento (no fabricable por IA, ver nota) |
| P14 | Hecho |

`bash scripts/verify.sh` / `make verify`: **21 comprobaciones pasan, 4
fallan (P7, P8×2, P13), 3 requieren revisión manual (P4, P6, P9)** (corrida
el 2026-09-14 sobre el commit vigente tras el ajuste de P10/P11/P14).
Código de salida: 1 (correcto: P7/P8/P13 son pendientes reales).

**Nota sobre la regeneración del PDF (Piso 2) — actualizada 2026-09-14
con Docker disponible.** `docs/informe/main.tex` tenía su propia copia de
la tabla CRediT, con el mismo defecto de P10 y un párrafo que lo
defendía explícitamente; ya está sincronizada con `CONTRIBUTORS.md`
(commit `cf00727`).

Al correr `make docs` por primera vez con Docker activo se encontró un
**defecto real y preexistente del propio `Makefile`** (no introducido en
esta sesión): el objetivo montaba solo `docs/informe:/work`, pero
`main.tex` referencia los tres PNG del modelo C4 con
`../arquitectura/*.png` — con ese mount, el `../` se sale del
contenedor y `pdflatex` fallaba con `File not found` (error fatal, no
advertencia). Esto llevaba el examen suspenso a **CERO por Piso 2** de
haberse detectado en la entrega y no antes.

Corregido montando todo `docs/:/work` con `-w /work/informe` (mismo
layout relativo que en el host). Verificado end-to-end en Docker:

```bash
docker run --rm -v "$(pwd)/docs:/work" -w /work/informe texlive/texlive \
  sh -c "pdflatex -interaction=nonstopmode main.tex && bibtex main && \
         pdflatex -interaction=nonstopmode main.tex && \
         pdflatex -interaction=nonstopmode main.tex && \
         pdflatex -interaction=nonstopmode main.tex"
```

Salida relevante (4ª y última pasada, cero advertencias):
```
Output written on main.pdf (72 pages, 1365470 bytes).
```
Sin errores fatales (`grep -c "^!" ` → 0), sin citas ni referencias sin
resolver en la pasada final (`grep -c "undefined"` sobre la 4ª pasada →
0). Con 3 pasadas (la cantidad que tenía el `Makefile` antes de este
commit) el documento ya resolvía todas las citas pero quedaba una
advertencia de `Label(s) may have changed. Rerun` — se agregó una 4ª
pasada de `pdflatex` al objetivo `docs` para eliminarla del todo.

`docs/informe-final.pdf`, `docs/informe/main.pdf`,
`docs/informe/caratula-standalone.pdf` e `informe-final.pdf` (copia raíz,
antes idéntica byte a byte a la de `docs/`) quedan regenerados y
comprometidos junto con este expediente.

**Nota sobre P13.** Las constancias de consentimiento firmado no se
pueden generar de forma automática ni por IA: exigen que cada uno de los
15 participantes reales de la encuesta SUS acepte y firme. Inventar esa
aceptación sería fabricar evidencia (Piso 3 = cero directo). Lo único que
se puede automatizar es el mecanismo (la plantilla ya existe en
`docs/etica/consentimiento/plantilla.md`); recolectar las 15 constancias
es trabajo humano del equipo, no de esta herramienta.
