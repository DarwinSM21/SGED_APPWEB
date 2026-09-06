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
quedan cubiertos por el equipo de la siguiente manera:

| Rol CRediT | Integrante(s) | Cobertura |
|---|---|---|
| Conceptualization | Ricardo, Darwin | Diseño de los cuatro dominios (académico, deportivo, inventario, seguridad) y de la estrategia híbrida de acceso a datos. |
| Data curation | Alejandro | Diseño del esquema, procedimientos almacenados y limpieza de los datos crudos de medición. |
| Formal analysis | Alejandro | Análisis estadístico de los datos de rendimiento y usabilidad (intervalos, distribución t). |
| Funding acquisition | — | No aplica (proyecto académico sin financiación externa). |
| Investigation | Darwin | Relevamiento de requisitos con la escuela ProFútbol y recolección de evidencia empírica. |
| Methodology | Ricardo | Proceso de investigación (DSR) y protocolo de medición. |
| Project administration | Darwin | Administración del proyecto, calendario y gestión de entregas. |
| Resources | Ricardo | Configuración del entorno de despliegue (Render), contenedores Docker y base de datos. |
| Software | Alejandro, Ricardo, Darwin | Implementación de backend (Spring Boot), frontend (Angular) y procedimientos almacenados. |
| Supervision | Darwin | Coordinación del equipo y seguimiento del repositorio. |
| Validation | Alejandro, Ricardo | Pruebas de cobertura (JaCoCo), pruebas de carga (k6), estudio de usabilidad (SUS) y auditoría de seguridad. |
| Visualization | Alejandro | Diagramas C4 y de arquitectura del sistema. |
| Writing – original draft | Alejandro | Redacción del informe, del documento de requisitos (SRS) y de la documentación técnica. |
| Writing – review & editing | Ricardo | Revisión y corrección de la documentación y su consistencia con el código. |

## Evidencia cuantitativa (derivada de `git log`, no autodeclarada)

Medido sobre la rama `main`. Se separa lo **escrito** de lo **generado**
(reportes JaCoCo/Lighthouse, salidas crudas de k6, `package-lock.json`,
PDF), porque contar un reporte HTML como autoría inflaría la cifra sin
reflejar trabajo real.

| Integrante | Commits | Líneas escritas | Archivos escritos |
|---|---:|---:|---:|
| Pallo Pinto Alejandro Daniel | 211 | 69 850 | 1 662 |
| Arcalle Grefa Darwin Orlando | 58 | 45 743 | 730 |
| Velez Lopez Ricardo Elias | 41 | 6 093 | 126 |
| **Total** | **310** | **121 686** | **2 518** |

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
reorganizar el historial para este repositorio. Todo el historial vigente
usa exclusivamente:
`dpallop@uteq.edu.ec`, `rvelezl3@uteq.edu.ec` y `darcalleg@uteq.edu.ec` (el
estado anterior mezclaba correos personales de `outlook.es`/`gmail.com` y
un tipeo `uteq.edue.ec`; ninguno figura ya en `git log`). El contenido de
los archivos no cambió; solo la atribución de autoría. La verificación se
puede repetir con `git log main --format='%ae' | sort -u` (debe devolver
exactamente los tres correos de arriba).

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
