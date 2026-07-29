# Contribuidores y roles (CRediT)

Proyecto Fin de Curso — Aplicaciones Web, UTEQ. Roles asignados según la
[taxonomía CRediT](https://credit.niso.org/), a partir de la evidencia real
del historial de `git log` (no auto-declarados).

| Integrante | Correo institucional | Roles (CRediT) |
|---|---|---|
| Pallo Pinto Alejandro Daniel | dpallop@uteq.edu.ec | Software, Seguridad (JWT/cookies/OWASP), Validación (pruebas, cobertura JaCoCo, k6), Curación de datos (migraciones Flyway, procedimientos almacenados), Redacción (documentación de evidencia) |
| Velez Lopez Ricardo Elias | _(commits con correo no institucional — ver nota)_ | Conceptualización, Software (arranque inicial de Angular, módulo de autenticación JWT, CRUD de Estudiante), Metodología |
| Arcalle Grefa Darwin Orlando | _(commits con correo no institucional — ver nota)_ | Conceptualización, Software (estructura inicial del repositorio), Documentación de arquitectura (ADRs, diagramas C4) |

## Nota de trazabilidad: commits de "DannaN24"

Los commits `feat: exponer registro, logout y refresh...` y
`fix: completar archivos vacios V1 sql, docker-compose...` (2026-06-29)
aparecen en `git log` bajo `DannaN24 <dninasuntar@uteq.edu.ec>`, pero son
trabajo real de **Pallo Pinto Alejandro Daniel**: esa cuenta era la que
había quedado configurada en el PC de la universidad que usó ese día. Esto
ya había quedado insinuado en el propio historial (commit `a456255`, del
mismo 2026-06-29: *"docs: commits anteriores realizados por Alejandro Pallo
- cuenta DannaN24 era la configurada en PC universitaria"*), pero sin una
nota escrita hasta ahora. No se reescribió el `git log` (evitar reescribir
historia ya compartida con el equipo); esta nota es la aclaración oficial de
autoría para efectos de evaluación.

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
