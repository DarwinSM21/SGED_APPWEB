# Declaración de aportes — examen suspenso (EV-4)

Guía del examen suspenso, UTEQ — Aplicaciones Web, PPA 2026-2027. Por cada
uno de los 14 pendientes de la guía: quién lo cerró, con qué archivos y con
qué commits, contrastado contra `git log` (no autodeclarado). Es la base de
la calificación individual (sección 4 de la guía).

Metodología: para cada punto identifico (a) el trabajo de fondo ya
existente en el repositorio y quién lo escribió, y (b) el cierre específico
que resuelve lo que la guía marcó como pendiente, con sus commits. Cuando
ambos coinciden en la misma persona lo digo una sola vez. Los correos son
los institucionales de cada integrante, tomados de
[`CONTRIBUTORS.md`](CONTRIBUTORS.md).

| Integrante | Correo institucional |
|---|---|
| Arcalle Grefa Darwin Orlando | darcalleg@uteq.edu.ec |
| Pallo Pinto Alejandro Daniel | dpallop@uteq.edu.ec |
| Velez Lopez Ricardo Elias | rvelezl3@uteq.edu.ec |

---

## P1 — Respuestas del SUS (peso 1,3)

**Titular del cierre:** Arcalle Grefa Darwin Orlando, con la contribución
específica de Velez Lopez Ricardo Elias descrita abajo.

- Los 15 registros reales, el recálculo Brooke y el intervalo de confianza
  con t de Student ya eran trabajo de fondo de **Pallo Pinto Alejandro
  Daniel** (`docs/mediciones/sus/respuestas.csv`, `REPORT.md`,
  `INTERPRETACION.md`; commits `40f4d39`, `de93a8f`, `0a19cb6`, entre
  otros) y de **Velez Lopez Ricardo Elias** (amenazas a la validez,
  commit `d973292`).
- Lo que bloqueaba el punto (consentimiento de cada participante) se
  cerró junto con P13 — ver esa sección. El cierre en sí (destrabar P1
  una vez obtenido el consentimiento) es de **Arcalle Grefa Darwin
  Orlando**, `VERIFICACION.md` (commit `7f6f424`).

## P2 — Lighthouse (peso 0,9)

**Titular:** Pallo Pinto Alejandro Daniel.

Las 3 corridas por perfil (móvil/escritorio) contra el despliegue público
y su versionado en `docs/mediciones/lighthouse/` son trabajo de fondo de
Pallo Pinto (commit `9f75e71` y siguientes). No requirió cierre adicional
en esta ronda: `VERIFICACION.md` documenta la orden que lo comprueba.

## P3 — DOI retirado (peso 0,5)

**Titular del cierre:** Arcalle Grefa Darwin Orlando.

`scripts/check-doi.sh` (comprueba que los DOI vigentes resuelvan a 200 y
que el retirado resuelva a 410, documentado como tal) se agregó en el
commit `c2c36fa`. La gestión previa de los DOI de Zenodo across releases
es compartida entre los tres integrantes a lo largo del proyecto (ver
`CITATION.cff` y su historial); el cierre específico de este punto —la
verificación reproducible— es de Arcalle Grefa.

## P4 — Javadoc (peso 1,5)

**Titular:** Arcalle Grefa Darwin Orlando.

- Grueso de la documentación Javadoc añadida en los commits `3441c02`
  (98 DTO record), `fa8573e` (22 entidades JPA), `335ac57` (resto de
  entidades), `e666d41` (repositorios).
- En esta ronda final: corregido un defecto real de
  `scripts/javadoc-coverage.py` (no seguía anotaciones partidas en varias
  líneas ni saltaba comentarios `//` sueltos, lo que hacía ver 90,03%
  donde en realidad había 100%), eliminada una duplicación accidental de
  Javadoc en `ConsentRepository.java`, y documentado el constructor
  compacto de `AnonymousPlayerProfile` (el único método genuinamente sin
  Javadoc de los 61 que reportaba la lista). Commit `39ca9fa`.
- Resultado: 612/612 métodos públicos documentados (100%),
  `mvn javadoc:javadoc` sin error.

## P5 — Validador de trazabilidad (peso 0,6)

**Titular:** Arcalle Grefa Darwin Orlando, sobre la base de Pallo Pinto
Alejandro Daniel.

El defecto original ("el validador nunca podía fallar") lo corrigió
Pallo Pinto (commit `3297ec0`); Arcalle Grefa forzó UTF-8 en su
salida (`6fd0581`) y, en el expediente EV-1/EV-2 (`c2c36fa`), hizo que
`make verify` invoque el script en cada corrida — que era el defecto real
que seguía abierto (nada lo llamaba automáticamente).

## P6 — Figuras en inglés (peso 0,7)

**Titular:** Arcalle Grefa Darwin Orlando.

Traducción de las fuentes (Mermaid, SVG, DSL de C4) en los commits
`215a4ec` y `f8aeb48`. Revisión visual final de los 4 PNG rasterizados
(no cubiertos por grep), confirmando que están 100% en inglés, en esta
ronda — `VERIFICACION.md` (commit `7f6f424`).

## P7 — SRS firmado, con MoSCoW (peso 0,8)

**Titular:** Arcalle Grefa Darwin Orlando.

El campo MoSCoW en `docs/requisitos/SRS.md` es trabajo de fondo
compartido del equipo a lo largo de las revisiones del SRS. El registro
de la firma del docente-director en el acta de aprobación es de Arcalle
Grefa (commit `cead25d`). El pipeline reproducible que genera
`docs/requisitos/SRS-v1.1.0.pdf` (`scripts/build-srs-pdf.sh`, `make srs`)
es de Arcalle Grefa, commit `fca011a`.

## P8 — Etiqueta única v1.1.0 (peso 0,6)

**Titular:** Arcalle Grefa Darwin Orlando.

Etiqueta movida y referencias actualizadas (portada, CITATION.cff,
README, SRS) en el commit `ebd4b69`, con la corrección posterior del
chequeo de `CITATION.cff` (`cc6b905`) y de la dereferenciación del commit
real de la etiqueta anotada (`74af4dd`).

## P9 — Nombres de tipos en inglés (peso 0,7)

**Titular:** Arcalle Grefa Darwin Orlando.

Renombrado de identificadores a inglés en los commits `f6ff161`
(backend), `4536466` (frontend, alineado a los DTO renombrados) y
`011fc15` (valores de 4 enums de dominio). Revisión manual final de los
277 nombres de tipo del backend uno por uno en esta ronda, confirmando
0% en español — `VERIFICACION.md` (commit `7f6f424`).

## P10 — Roles CRediT con conteo real (peso 0,5)

**Titular:** Arcalle Grefa Darwin Orlando, sobre la base de Pallo Pinto
Alejandro Daniel.

La declaración original de roles CRediT es de Pallo Pinto (commit
`ebb5906`). El defecto que señalaba la guía (el número junto a cada
persona era su total de commits, repetido en cada fila) lo corrigió
Arcalle Grefa con `scripts/credit-counts.py` (conteo real por rol y
rutas de archivo), commit `634537b`.

## P11 — Clave de ejemplo (peso 0,4)

**Titular:** Arcalle Grefa Darwin Orlando, sobre la base de Pallo Pinto
Alejandro Daniel.

`.env.example` es trabajo de fondo de Pallo Pinto. El reemplazo del valor
con aspecto real por el marcador `CAMBIAR_EN_PRODUCCION_...` es de
Arcalle Grefa, commit `634537b`.

## P12 — Umbral de cobertura unificado (peso 0,5)

**Titular:** Pallo Pinto Alejandro Daniel, con verificación de Arcalle
Grefa Darwin Orlando.

La corrección original de esta misma inconsistencia (RNF-09 citaba el
umbral equivocado) es de Pallo Pinto, commit `76e4e48`. La verificación
de que todo el entregable sigue citando 70% de forma consistente es de
Arcalle Grefa, commit `e70f02a`.

## P13 — Consentimientos informados del SUS (peso 0,5)

**Titulares:** Velez Lopez Ricardo Elias (obtención real del
consentimiento) y Arcalle Grefa Darwin Orlando (mecanismo de registro).

El mecanismo para comprobar el consentimiento sin exponer datos
personales en el repositorio público (`docs/etica/consentimiento/
registro.md`, endurecimiento de `scripts/verify.sh`) es de Arcalle Grefa,
commit `ee0e699`. La plantilla de consentimiento sobre la que se firmó
(`plantilla.md`) es trabajo de fondo compartido (commits `fb08831` de
Velez Lopez y `9fc1801`/`eca162c` de Pallo Pinto y Arcalle Grefa).

**El cierre real de este punto es trabajo de campo de Velez Lopez Ricardo
Elias**: contactó a los 15 participantes reales de la encuesta SUS y les
hizo firmar el consentimiento el 2026-09-14, como formalización
retroactiva del consentimiento ya otorgado verbalmente en las sesiones
del 2026-07-30 y 2026-08-18, firmando él mismo como investigador
responsable en las 15 constancias. Arcalle Grefa verificó cada constancia
individualmente y actualizó el registro (commit `7f6f424`); los 15
originales firmados (con nombre y firma reales, dato personal) no se
suben al repositorio, según el propio diseño de `plantilla.md`.

## P14 — Estadística con trazabilidad (peso 0,5)

**Titular:** Arcalle Grefa Darwin Orlando, sobre la base de Pallo Pinto
Alejandro Daniel.

`scripts/perf-analysis.py`, incluida la función `holm_bonferroni`, es
trabajo de fondo de Pallo Pinto (commits `734a79f`, `7ce1474` y
siguientes). El punto que señalaba la guía (encontrar esa trazabilidad
sin depender de buscar por nombre de archivo) se cerró documentándolo
explícitamente en `VERIFICACION.md`, commit `634537b`, de Arcalle Grefa.

---

## Entregables EV-1/EV-2/EV-3 (sin peso propio, pero condicionan los 14 puntos)

`VERIFICACION.md`, `scripts/verify.sh`/`make verify` y la etiqueta
`v1.1.0` son de **Arcalle Grefa Darwin Orlando** (commits `c2c36fa` y
siguientes hasta el cierre de este expediente).

## Declaración de asistencia de Inteligencia Artificial

Seguimos el mismo criterio ya declarado en
[`CONTRIBUTORS.md`](CONTRIBUTORS.md#declaración-de-asistencia-de-inteligencia-artificial):
el cierre de estos pendientes usó Claude (Anthropic) como herramienta de
apoyo — para leer y contrastar la guía contra el repositorio, redactar
`VERIFICACION.md`, corregir el script de cobertura de Javadoc y verificar
individualmente las 15 constancias de consentimiento. Cada commit se
revisó y se hizo bajo la identidad y responsabilidad de la persona que
firma más abajo; ningún commit incluye atribución de coautoría a IA.

## Firmas

Cada integrante confirma que la atribución de arriba es correcta y que
puede defender ante el docente el código y la evidencia de los puntos que
declara suyos.

| Integrante | Correo institucional | Conforme |
|---|---|---|
| Arcalle Grefa Darwin Orlando | darcalleg@uteq.edu.ec | |
| Pallo Pinto Alejandro Daniel | dpallop@uteq.edu.ec | |
| Velez Lopez Ricardo Elias | rvelezl3@uteq.edu.ec | |

_Pendiente: Pallo Pinto y Velez Lopez deben revisar esta declaración y
confirmar su conformidad (columna "Conforme") antes del cierre, en
particular la atribución de P12/P14/P2 (Pallo Pinto) y P13 (Velez Lopez)._
