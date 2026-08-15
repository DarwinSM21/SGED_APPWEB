# Contribuidores y roles (CRediT)

Proyecto Fin de Curso — Aplicaciones Web, UTEQ. Roles asignados según la
[taxonomía CRediT](https://credit.niso.org/), a partir de la evidencia real
del historial de `git log` (no auto-declarados).

| Integrante | Correo institucional | Roles (CRediT) |
|---|---|---|
| Pallo Pinto Alejandro Daniel | dpallop@uteq.edu.ec | Software, Seguridad (JWT/cookies/OWASP), Validación (pruebas, cobertura JaCoCo, k6, SUS), Curación de datos (procedimientos almacenados, esquema), Redacción (borrador original), Visualización |
| Velez Lopez Ricardo Elias | _(commits con correo no institucional — ver nota)_ | Conceptualización, Software (arranque de Angular, módulo de autenticación JWT, CRUD de Estudiante, reestructuración en dominios), Metodología |
| Arcalle Grefa Darwin Orlando | _(commits con correo no institucional — ver nota)_ | Conceptualización, Software (estructura inicial del repositorio, modelo de datos, frontend), Administración del proyecto |

## Evidencia cuantitativa (derivada de `git log`, no autodeclarada)

Medido sobre la rama `main`. Se separa lo **escrito** de lo **generado**
(reportes JaCoCo/Lighthouse, salidas crudas de k6, `package-lock.json`,
PDF), porque contar un reporte HTML como autoría inflaría la cifra sin
reflejar trabajo real.

| Integrante | Commits | Líneas escritas | Archivos escritos |
|---|---:|---:|---:|
| Pallo Pinto Alejandro Daniel | 72 | 17 760 | 374 |
| Arcalle Grefa Darwin Orlando | 17 | 5 429 | 303 |
| Velez Lopez Ricardo Elias | 32 | 4 077 | 108 |
| **Total** | **121** | **27 266** | — |

Reproducible con:

```bash
git log --pretty="AUTOR:%an" --numstat main
```

> **El volumen no es la contribución.** Estas cifras miden actividad, no
> valor: un cambio de dos líneas que corrige un fallo de control de acceso
> pesa más que dos mil líneas de documentación. La tabla existe porque la
> evaluación exige autoría verificable, no para jerarquizar al equipo.

## Nota de trazabilidad: corrección de autoría en el historial (2026-08-15)

Hasta el 2026-08-14, dos commits del 2026-06-29 (`feat: exponer registro,
logout y refresh...` y `fix: completar archivos vacios V1 sql,
docker-compose...`) aparecían en `git log` bajo `DannaN24
<dninasuntar@uteq.edu.ec>`, siendo en realidad trabajo de **Pallo Pinto
Alejandro Daniel**: esa cuenta había quedado configurada por accidente en
el PC de la universidad que usó ese día. Esto ya estaba insinuado en el
propio historial del momento (commit `docs: commits anteriores realizados
por Alejandro Pallo - cuenta DannaN24 era la configurada en PC
universitaria`), pero sin corregirse.

Adicionalmente, 2 commits de Alejandro tenían un error de tipeo en el
dominio del correo (`uteq.edue.ec` en vez de `uteq.edu.ec`) y 5 commits de
Ricardo usaban un correo (`ricardo@email.com`) no vinculado a su cuenta de
GitHub — ambos casos hacían que esos commits aparecieran como
"colaboradores fantasma" (cuentas anónimas, sin nombre) en la vista de
Contribuidores de GitHub en vez de atribuirse a sus autores reales.

El 2026-08-15, con acuerdo explícito de los tres integrantes, se corrigió
la autoría (`git commit --amend`/`filter-branch` sobre esos 9 commits
puntuales, sin tocar el contenido de ningún archivo) y se forzó el push.
Los tags `v0.1.0-entrega-1b`, `v0.7.1` y `v0.9.0-rc` se recrearon apuntando
a los commits equivalentes ya corregidos, preservando mensaje, fecha y
autor original de cada tag. El resto del historial —158 de los 167
commits de `main`, ya correctamente atribuidos desde el inicio— no se
tocó.

## Nota sobre correos no institucionales

Varios commits de Ricardo y Darwin usan correos personales
(`outlook.es`, `gmail.com`) en vez de `@uteq.edu.ec`. Recomendado corregir
`git config user.email` para commits futuros si la rúbrica de esta entrega
evalúa trazabilidad de autoría por correo institucional.

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
