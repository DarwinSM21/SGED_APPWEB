# Esquema de versionado

Este proyecto usa [Versionado Semántico](https://semver.org/lang/es/)
(`MAJOR.MINOR.PATCH`) con un sufijo que marca el hito del curso al que
corresponde cada entrega del Proyecto Fin de Curso:

| Tag | Hito | Estado |
|---|---|---|
| `v0.1.0-entrega-1b` | Entrega 1B | ✅ Publicado (2026-06-24) |
| `v0.9.0-rc` | Tercera Entrega (release candidate) | ⏳ Pendiente |
| `v1.0.0` | Entrega Final | ⏳ Pendiente |

## Por qué todavía no existe `v0.9.0-rc`

El trabajo técnico de la Tercera Entrega (hardening JWT/cookies, auditoría
OWASP de 6 controles, evidencia k6, cobertura JaCoCo, procedimientos
almacenados, pinning por digest) ya está en `main` — ver
[CHANGELOG.md](CHANGELOG.md#unreleased--hacia-v090-rc-tercera-entrega`). El
tag no se creó todavía porque la rama `feature/entrega3` (reestructuración
de paquetes, ADRs adicionales, C4 en Structurizr) está en proceso de
reconciliarse con `main` antes de cerrar esta entrega. Crear el tag antes de
esa reconciliación arriesgaría dejar fuera contenido que sí cuenta para la
evaluación, o taguear una arquitectura que va a cambiar apenas se resuelva
la reconciliación.

**Antes de tagear `v0.9.0-rc`:** confirmar que `feature/entrega3` esté
mergeado (o descartado a propósito), que el árbol de archivos coincida con
la estructura exigida por la guía de la Tercera Entrega, y que
`make up && make test && make audit && make bench` corran limpios desde una
clonación nueva.
