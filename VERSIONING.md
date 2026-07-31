# Esquema de versionado

Este proyecto usa [Versionado Semántico](https://semver.org/lang/es/)
(`MAJOR.MINOR.PATCH`) con un sufijo que marca el hito del curso al que
corresponde cada entrega del Proyecto Fin de Curso:

| Tag | Hito | Estado |
|---|---|---|
| `v0.1.0-entrega-1b` | Entrega 1B | ✅ Publicado (2026-06-24) |
| `v0.9.0-rc` | Tercera Entrega (release candidate) | ✅ Publicado (2026-07-30) |
| `v1.0.0` | Entrega Final | ⏳ Pendiente (2026-08-17) |

## Criterios verificados antes de crear `v0.9.0-rc`

- `feature/entrega3` (reestructuración de paquetes `academico`/`deportivo`/
  `seguridad`, ADRs adicionales, C4 en Structurizr) mergeada a `main` (PRs
  #5–#9).
- `./mvnw test`: 101 pruebas, 0 fallos, 0 errores.
- Cobertura JaCoCo: 72,5 % (por encima del umbral del 60 %) — ver
  `docs/mediciones/jacoco/` y la sección de cobertura del informe.
- Documentación de arquitectura (ADR-002) corregida para coincidir con el
  código real (JWT en cookie, no `localStorage`).
- Datos de salud del estudiante (peso/altura) declarados explícitamente en
  `docs/etica/ETHICS.md` (hallazgo H-06), no ocultados.
- Único informe oficial con fuente versionada: `docs/informe/main.tex`.

## Advertencias que siguen vigentes al momento de tagear

Taguear no implica que todo esté resuelto — implica que el entregable es
honesto sobre lo que falta:

- La encuesta SUS (Bloque C.3) no tiene participantes reales todavía.
- `academico.representante` y `deportivo.equipo` son paquetes vacíos (RF-22
  y el módulo de equipos), sin esquema de base de datos.
- El dominio deportivo restante (horarios, sesiones, asistencias,
  evaluaciones) tiene esquema pero no API REST — objetivo de la Entrega
  Final.
- El hallazgo H-06 (peso/altura sin base legal documentada) sigue abierto;
  taguear no lo resuelve, solo lo deja registrado.
