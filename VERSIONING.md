# Esquema de versionado

Este proyecto usa [Versionado Semántico](https://semver.org/lang/es/)
(`MAJOR.MINOR.PATCH`) con un sufijo que marca el hito del curso al que
corresponde cada entrega del Proyecto Fin de Curso:

| Tag | Hito | Estado |
|---|---|---|
| `v0.1.0-entrega-1b` | Entrega 1B | ✅ Publicado (2026-06-24) |
| `v0.9.0-rc` | Tercera Entrega (release candidate) | ✅ Publicado (2026-07-30) |
| `v1.0.0` | Entrega Final — corte entregado el 2026-08-24 | ✅ Publicado |
| `v1.0.1` | Entrega Final — matriz de trazabilidad tras el rename a inglés; corte con DOI de Zenodo | ✅ Publicado (2026-09-06) |
| `v1.0.2` | Entrega Final — recuperación de contraseña (RF-37) + revisión del SRS contra ISO/IEC/IEEE 29148 (M1–M9, A1–A4) | ✅ Publicado (2026-09-07) |
| `v1.0.3` | Entrega Final — **corte defendido**: revisión M1–M3 del SRS v1.6 (etiqueta sobre el cierre real, vocabulario de estados, RNF-26 para H-09) | ✅ Publicado (2026-09-10) |

Cada tag de la familia `v1.0.x` conserva el corte que se entregó en su
fecha; `v1.0.3` es el que se defiende y al que apunta la cabecera del SRS.
Los anteriores no se mueven, para que el estado declarado en cada uno siga
siendo verificable contra lo que ese tag realmente contiene.

## Criterios verificados antes de crear `v0.9.0-rc`

- `feature/entrega3` (reestructuración de paquetes `academico`/`deportivo`/
  `seguridad`, ADRs adicionales, C4 en Structurizr) mergeada a `main` (PRs
  #5–#9).
- `./mvnw test`: 101 pruebas, 0 fallos, 0 errores.
- Cobertura JaCoCo: 72,5 % al momento de taguear (por encima del umbral del
  60 %). **La cifra era correcta, pero la medición no era válida:** se hizo
  con `./mvnw test` sobre un `target/` que conservaba `.class` previos a la
  reestructuración, y el reporte archivado llegó a listar paquetes que ya no
  existen en el código fuente. Vuelta a medir con `./mvnw clean test` sobre
  el estado actual: 72,5 % (2507 de 3457 instrucciones), 102 pruebas. Que el
  porcentaje coincida es casualidad aritmética, no confirmación: por eso
  `make test` pasó a ejecutar `clean test`. Ver `docs/mediciones/jacoco/` y
  la sección de cobertura del informe.
- Documentación de arquitectura (ADR-002) corregida para coincidir con el
  código real (JWT en cookie, no `localStorage`).
- Datos de salud del estudiante (peso/altura) declarados explícitamente en
  `docs/etica/ETHICS.md` (hallazgo H-06), no ocultados.
- Único informe oficial con fuente versionada: `docs/informe/main.tex`.

## Advertencias que siguen vigentes al momento de tagear

Taguear no implica que todo esté resuelto — implica que el entregable es
honesto sobre lo que falta:

- ~~La encuesta SUS (Bloque C.3) no tiene participantes reales todavía.~~
  **Resuelto el 2026-07-30, después del tag:** 10 participantes reales,
  media 68,25 (grado C) — `docs/mediciones/sus/REPORT.md`.
- ~~`academico.representante` y `deportivo.equipo` son paquetes vacíos.~~
  **Resuelto el 2026-07-30, después del tag:** las 14 clases stub se
  eliminaron en vez de dejarse como archivos vacíos en el repositorio. Los
  dos módulos (RF-22 y el de equipos) siguen pendientes para la Entrega
  Final, pero ahora constan solo como texto en la documentación, no como
  código que aparenta existir.
- El dominio deportivo restante (horarios, sesiones, asistencias,
  evaluaciones) tiene esquema pero no API REST — objetivo de la Entrega
  Final.
- El hallazgo H-06 (peso/altura sin base legal documentada) sigue abierto;
  taguear no lo resuelve, solo lo deja registrado.

Las advertencias tachadas se resolvieron en commits posteriores a
`v0.9.0-rc`. Se dejan visibles, no borradas, para que el estado declarado
en el tag siga siendo verificable contra lo que el tag realmente contiene.
