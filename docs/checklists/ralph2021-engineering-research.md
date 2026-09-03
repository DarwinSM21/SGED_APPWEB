# Checklist del estándar empírico "Engineering Research" — Ralph et al. 2021 (Bloque E)

SGED se autoclasifica como **Engineering Research** dentro de los
estándares empíricos de Ralph *et al.*, *Empirical Standards for
Software Engineering Research* (2021): el trabajo diseña, implementa y
evalúa empíricamente un artefacto de software nuevo (un sistema de
gestión escolar deportiva), no un experimento controlado con sujetos
humanos asignados a condiciones, ni un estudio de caso puramente
observacional sobre un sistema preexistente. Es la misma clasificación
que ya sustenta el capítulo de Materiales y métodos del informe (DSR de
Peffers). Este checklist adapta las dimensiones generales de ese
estándar al alcance de un PFC, con evidencia concreta del repositorio
en cada fila — no una autoevaluación genérica.

| Dimensión | Estado | Evidencia |
|---|---|---|
| El problema y su motivación están claramente descritos | ✅ | §Contexto y motivación del informe, con 3 referencias que sustentan la carencia de digitalización en clubes deportivos de formación |
| Preguntas de investigación explícitas y verificables | ✅ | RQ1–RQ4, §Preguntas de investigación, cada una cerrada y trazada a evidencia empírica concreta |
| El artefacto (tecnología) está descrito con suficiente detalle para juzgar su novedad y su contribución | ✅ | Capítulos de Arquitectura e Implementación; estrategia híbrida de acceso a datos como aporte técnico explícito, con ADR-006 |
| El proceso de diseño/construcción del artefacto está documentado | ✅ | 7 ADR (Nygard), C4 niveles 1–3 generados desde `workspace.dsl`, `docs/observaciones/OBSERVACIONES.md` como bitácora del proceso iterativo |
| La evaluación del artefacto usa métodos apropiados a las preguntas planteadas | ✅ | k6 para RQ1 (rendimiento), OWASP+ZAP+análisis estático para RQ2 (seguridad del acceso a datos), SUS para RQ3 (usabilidad), JaCoCo+matriz de trazabilidad para RQ4 (cobertura y trazabilidad) |
| El procedimiento de evaluación es reproducible por un tercero | 🟡 | `make bench`/`make audit`/`make test` reproducen el procedimiento, pero la evidencia archivada hoy está desactualizada frente al código actual (declarado explícitamente en §Estado de la entrega del informe) — la reproducibilidad del *procedimiento* es real, la vigencia de los *números* archivados no lo es todavía |
| Se declara el contexto/entorno de la evaluación (para juzgar generalización) | ✅ | §Entorno de ejecución: SO, versión de Docker, JDK, estado inicial de la base de datos |
| Se declaran las limitaciones de la muestra/participantes cuando aplica | ✅ | §Población, muestreo y participantes: N=15 para SUS (recolectado en dos tandas, 2026-07-30 y 2026-08-18), muestreo por conveniencia declarado explícitamente según Baltes y Ralph (2022) |
| Amenazas a la validez tratadas por categoría (constructo, interna, externa, conclusión) | ✅ | Capítulo Amenazas a la validez, cuatro categorías con mitigación declarada por cada una |
| El artefacto y los datos de evaluación están disponibles para un tercero | 🟡 | Repositorio público con DOI de software; falta el DOI separado del *dataset* de mediciones (Bloque D.3, pendiente declarado en §Declaraciones obligatorias) |
| Conflictos de interés y financiamiento declarados | ✅ | §Declaraciones obligatorias |
| Los resultados se interpretan sin sobregeneralizar más allá de la evidencia | ✅ | §Discusión responde cada RQ contra evidencia concreta; la Conclusión distingue explícitamente "lo cumplido" de "lo pendiente" en vez de presentar el sistema como terminado |
| Defectos del propio proceso de medición se reportan si se encuentran | ✅ | Es un rasgo distintivo del proyecto: tres instrumentos de medición defectuosos detectados y corregidos en la Tercera Entrega (cobertura, auditoría A01, registro de usuarios), documentados como hallazgo y no ocultados |

## Resumen

| Estado | Cantidad |
|---|---|
| ✅ Cumple | 11 |
| 🟡 Cumple parcialmente | 2 |
| ❌ No cumple | 0 |

Las dos filas parciales comparten la misma causa raíz que ya señala
`docs/mediciones/DATA-PROVENANCE.md`: la evidencia empírica archivada
quedó desactualizada frente al crecimiento reciente del sistema, y el
*dataset* de mediciones todavía no tiene su propio DOI separado del
software. Ninguna de las dos es un defecto de diseño metodológico —
ambas se resuelven regenerando y publicando datos, no rediseñando el
estudio.
