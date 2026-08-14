# Checklist FAIR — SGED / ProFútbol (Bloque E, Guía de la Entrega Final)

Autoevaluación honesta contra los cuatro principios FAIR (Wilkinson et al.,
2016) para el paquete completo (software + datos + metadatos). Cada ítem se
marca `[x]` solo si hay evidencia verificable en el repositorio o en Zenodo
a la fecha de esta revisión; `[ ]` si sigue pendiente. No se marca nada por
intención, solo por evidencia — mismo criterio que ya aplica
`docs/observaciones/OBSERVACIONES.md`.

## F — Findable (localizable)

- [x] El software tiene un identificador persistente: DOI de Zenodo
      `10.5281/zenodo.21713240` (`CITATION.cff`, badge en `README.md`).
- [ ] Ese DOI corresponde a la versión `v1.0.0` — hoy resuelve a `v0.9.0-rc`
      (`CITATION.cff` todavía declara `version: 0.9.0-rc`). Pendiente
      publicar una nueva versión en Zenodo cuando se cierre el tag `v1.0.0`.
- [x] Metadatos ricos y buscables: `CITATION.cff` con título, autores,
      afiliación, licencia, palabras clave (`spring-boot`, `angular`,
      `postgresql`, `jwt`, `owasp`, `proyecto-fin-de-curso`).
- [ ] ORCID de cada autor en `CITATION.cff` — hoy ningún autor tiene el
      campo `orcid` (bloquea también el criterio R3 de la rúbrica).
- [x] El repositorio es públicamente indexable (GitHub, público, con
      `README.md` descriptivo).
- [ ] El *dataset* de mediciones (`docs/mediciones/`) todavía no tiene DOI
      propio — hoy solo existe el DOI del software. FAIR pide que los datos
      sean localizables de forma independiente del código que los generó.

## A — Accessible (accesible)

- [x] Se accede por protocolo estándar, abierto y gratuito: HTTPS vía
      `git`/GitHub y vía Zenodo.
- [x] Los metadatos son accesibles incluso si el contenido cambia o se
      retira (Zenodo conserva metadatos de versiones anteriores).
- [x] No hay barreras de autenticación para leer el repositorio, el
      `CITATION.cff` ni el DOI.
- [ ] El *dataset* separado (una vez tenga su propio DOI) debe declarar su
      propia licencia de acceso — pendiente hasta que exista ese depósito.

## I — Interoperable (interoperable)

- [x] Formatos de datos no propietarios y ampliamente soportados: JSON
      (`k6`, Lighthouse), CSV (`respuestas.csv`, `matriz.csv`), SQL plano,
      Markdown, LaTeX/BibTeX.
- [x] La API usa un lenguaje formal y estándar de descripción: OpenAPI 3.0
      (`/api/docs`, Swagger UI), no un formato ad-hoc.
- [x] Vocabulario de metadatos reconocido: CRediT (`CONTRIBUTORS.md`), SemVer
      + Keep a Changelog (`VERSIONING.md`, `CHANGELOG.md`).
- [ ] Los reportes de medición (JaCoCo, Lighthouse, k6) se archivan como
      salida cruda de cada herramienta; no hay todavía un esquema propio
      documentado que unifique sus campos entre sí (relevante para
      `DATA-DICTIONARY.md`, criterio R2 — no es requisito estricto de FAIR
      pero facilita la reutilización real).

## R — Reusable (reutilizable)

- [x] Licencia clara y aprobada por OSI: MIT (`LICENSE`), declarada en
      `CITATION.cff`, `README.md` y el pie de cada documento clave.
- [x] Procedencia detallada: `CONTRIBUTORS.md` documenta autoría real
      (verificada contra `git log`, no autodeclarada) y el uso de
      asistencia de IA generativa, con alcance y revisión humana explícitos.
- [x] Documentación de dominio, contexto y limitaciones que va más allá del
      código: `docs/etica/ETHICS.md` (8 hallazgos abiertos o resueltos,
      marco legal LOPDP/Código de la Niñez), 8 ADRs con alternativas
      descartadas.
- [x] Cumple estándares de la comunidad para el tipo de artefacto: Flyway
      para migraciones versionadas, JPA 2.1 para el acceso a
      procedimientos, Docker Compose con imágenes pinadas por digest.
- [ ] El *dataset* de mediciones todavía no tiene licencia propia declarada
      (se recomienda CC BY 4.0, separada de la licencia MIT del software,
      siguiendo el principio de citación independiente de software y datos
      — Smith et al., 2016).

## Resumen

| Principio | Cumplidos | Pendientes | Nota |
|---|---|---|---|
| Findable | 3/6 | DOI en v1.0.0, ORCID, DOI del dataset | |
| Accessible | 3/4 | Licencia del dataset separado | |
| Interoperable | 3/4 | Esquema unificado de reportes (no bloqueante) | |
| Reusable | 4/5 | Licencia CC BY 4.0 del dataset | |

**Lectura honesta:** el software en sí ya es razonablemente FAIR. Lo que
falta en los cuatro principios converge en el mismo punto — **el dataset de
mediciones necesita su propio DOI, su propia licencia (CC BY 4.0) y el
`CITATION.cff` necesita ORCID de cada autor** — no son cuatro problemas
distintos, es uno solo con cuatro síntomas. Resolver el depósito Zenodo del
*dataset* y agregar ORCID resuelve la mayoría de las casillas vacías de
una vez.
