# Contribuidores y roles (CRediT)

Proyecto Fin de Curso — Aplicaciones Web, UTEQ. Roles asignados según la
[taxonomía CRediT](https://credit.niso.org/), a partir de la evidencia real
del historial de `git log` (no auto-declarados).

| Integrante | Correo institucional | Roles (CRediT) |
|---|---|---|
| Pallo Pinto Alejandro Daniel | dpallop@uteq.edu.ec | Software, Formal analysis, Validation, Data curation, Writing – original draft, Visualization |
| Velez Lopez Ricardo Elias | rvelezl3@uteq.edu.ec | Conceptualization, Software, Validation, Methodology, Resources, Writing – review & editing |
| Arcalle Grefa Darwin Orlando | darcalleg@uteq.edu.ec | Conceptualization, Software, Investigation, Project administration, Supervision |

La taxonomía CRediT completa define catorce roles; en este proyecto todos
quedan cubiertos por el equipo de la siguiente manera. El conteo junto a
cada integrante es su **número real de commits en `main`** (el mismo de
la tabla cuantitativa de abajo, reproducible con
`git log --pretty="AUTOR:%ae" main | sort | uniq -c`): CRediT clasifica
*tipos* de contribución intelectual, no un archivo por rol, así que un
mismo commit puede sostener varios roles a la vez (una prueba de carga
es a la vez `Software` y `Validation`) — por eso el conteo es el total
verificable de la persona, no una partición exclusiva por rol.

| Rol CRediT | Integrante(s) | Cobertura |
|---|---|---|
| Conceptualization | Ricardo (55), Darwin (136) | Diseño de los cuatro dominios (académico, deportivo, inventario, seguridad) y de la estrategia híbrida de acceso a datos. |
| Data curation | Alejandro (233) | Diseño del esquema, procedimientos almacenados y limpieza de los datos crudos de medición. |
| Formal analysis | Alejandro (233) | Análisis estadístico de los datos de rendimiento y usabilidad (intervalos, distribución t). |
| Funding acquisition | — | No aplica (proyecto académico sin financiación externa). |
| Investigation | Darwin (136) | Relevamiento de requisitos con la escuela ProFútbol y recolección de evidencia empírica. |
| Methodology | Ricardo (55) | Proceso de investigación (DSR) y protocolo de medición. |
| Project administration | Darwin (136) | Administración del proyecto, calendario y gestión de entregas. |
| Resources | Ricardo (55) | Configuración del entorno de despliegue (Render), contenedores Docker y base de datos. |
| Software | Alejandro (233), Ricardo (55), Darwin (136) | Implementación de backend (Spring Boot), frontend (Angular) y procedimientos almacenados. |
| Supervision | Darwin (136) | Coordinación del equipo y seguimiento del repositorio. |
| Validation | Alejandro (233), Ricardo (55) | Pruebas de cobertura (JaCoCo), pruebas de carga (k6), estudio de usabilidad (SUS) y auditoría de seguridad. |
| Visualization | Alejandro (233) | Diagramas C4 y de arquitectura del sistema. |
| Writing – original draft | Alejandro (233) | Redacción del informe, del documento de requisitos (SRS) y de la documentación técnica. |
| Writing – review & editing | Ricardo (55) | Revisión y corrección de la documentación y su consistencia con el código. |

## Evidencia cuantitativa (derivada de `git log`, no autodeclarada)

Medido sobre la rama `main`. Se separa lo **escrito** de lo **generado**
(reportes JaCoCo/Lighthouse, salidas crudas de k6, `package-lock.json`,
PDF), porque contar un reporte HTML como autoría inflaría la cifra sin
reflejar trabajo real.

| Integrante | Commits | Líneas escritas | Archivos escritos |
|---|---:|---:|---:|
| Pallo Pinto Alejandro Daniel | 233 | 82 050 | 792 |
| Arcalle Grefa Darwin Orlando | 136 | 60 993 | 776 |
| Velez Lopez Ricardo Elias | 55 | 11 912 | 153 |
| **Total** | **424** | **154 955** | **1 721** |

_Medido 2026-09-12 sobre `main` (commit `6fd0581`). "Archivos escritos" cuenta
rutas distintas tocadas por cada integrante (no eventos de cambio repetidos).
El conteo de Arcalle Grefa
Darwin Orlando suma los commits de sus dos correos vinculados a la misma
cuenta de GitHub (`darcalleg@uteq.edu.ec` y `darwinarcalle@gmail.com`; ver
nota más abajo). Estas cifras cambian con cada commit nuevo por diseño —
la cifra que cuenta es la que resulte de correr el comando de abajo sobre
el commit que finalmente se defienda, no la congelada aquí._

Reproducible con:

```bash
git log --pretty="AUTOR:%an" --numstat main
```

> **El volumen no es la contribución.** Estas cifras miden actividad, no
> valor: un cambio de dos líneas que corrige un fallo de control de acceso
> pesa más que dos mil líneas de documentación. La tabla existe porque la
> evaluación exige autoría verificable, no para jerarquizar al equipo.

## Nota de trazabilidad: historia del correo en el repositorio

La identidad Git del equipo quedó unificada en correos institucionales al
reorganizar el historial para este repositorio (el estado anterior mezclaba
correos personales de `outlook.es`/`gmail.com` y un tipeo `uteq.edue.ec`).
Desde entonces, los commits de Pallo Pinto y Vélez López usan exclusivamente
`dpallop@uteq.edu.ec` y `rvelezl3@uteq.edu.ec`. Los commits de Arcalle Grefa
usan **dos** correos — `darcalleg@uteq.edu.ec` (institucional) y
`darwinarcalle@gmail.com` (personal, usado en los commits más recientes) —
porque ambos están vinculados a la misma cuenta de GitHub (`DarwinSM21`), la
propietaria del repositorio canónico; GitHub atribuye los commits de
cualquiera de los dos a esa única cuenta, y por eso esta tabla los suma
juntos. El contenido de los archivos no cambió; solo la atribución de
autoría. La verificación se puede repetir con
`git log main --format='%ae' | sort -u` (debe devolver exactamente los
cuatro correos de arriba: los dos de Arcalle Grefa más los de Pallo Pinto y
Vélez López).

## Declaración de asistencia de Inteligencia Artificial

Siguiendo la guía de transparencia del SWEBOK v4.0, este equipo declara el
uso de IA generativa (Claude, de Anthropic) como herramienta de apoyo en
partes de este proyecto: revisión y corrección de código de seguridad
(autenticación JWT, hardening OWASP), generación de evidencia técnica
(scripts de auditoría, análisis de resultados de k6/JaCoCo) y redacción de
esta documentación (LICENSE, CITATION.cff, este archivo, CHANGELOG.md,
VERSIONING.md). El diseño de la arquitectura, las decisiones de seguridad y
la verificación de que el sistema funciona correctamente fueron hechos y
revisados por el equipo, no de forma autónoma por la IA. Los commits de este
repositorio no incluyen atribución de coautoría a IA — la autoría de cada
commit corresponde únicamente a la persona que lo realizó.

Declaración ampliada por fase del proyecto:
[`docs/etica/ai-disclosure.md`](docs/etica/ai-disclosure.md).
