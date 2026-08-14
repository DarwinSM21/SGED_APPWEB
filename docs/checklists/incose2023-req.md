# Checklist INCOSE v4 — calidad de requisitos (Bloque A.3.1 / D0R)

Evaluación de los 31 requisitos funcionales (RF-01 a RF-30 + RF-11b), 13
no funcionales (RNF-01 a RNF-13) y 3 restricciones de diseño (RD-01 a
RD-03) de SGED contra la *INCOSE Guide to Writing Requirements* v4
(INCOSE-TP-2010-006-04). Fuente primaria: `docs/requisitos/SRS.md`.
Cada característica se marca ✅ Cumple, 🟡 Cumple parcialmente o
❌ No cumple, con la evidencia concreta — no una autoevaluación genérica.

## Características de requisitos individuales (C1–C9)

| # | Característica | Estado | Evidencia |
|---|---|---|---|
| C1 | **Necessary** (necesario) | ✅ | Cada RF traza a al menos una historia de usuario o caso de uso en `docs/trazabilidad/matriz.csv`; no hay requisitos "por si acaso" sin origen declarado. |
| C2 | **Appropriate** (nivel de abstracción apropiado) | ✅ | Los RF describen comportamiento observable (`"El sistema deberá..."`), no detalles de implementación; las decisiones de implementación viven en los ADR, no en el SRS. |
| C3 | **Unambiguous** (inequívoco) | ✅ | Patrón sintáctico ISO/IEC/IEEE 29148:2018 aplicado de forma uniforme desde la resolución de OBS-01 (Entrega 1A). |
| C4 | **Complete** (completo en sí mismo, sin TBD) | 🟡 | RF-17 a RF-22 (los más recientes, implementados 2026-08-09 a 2026-08-13) no tienen campo `Prioridad:` explícito — 6 de 31 RF, hallazgo ya declarado en el informe (§Ingeniería de requisitos). |
| C5 | **Singular** (un solo requisito por enunciado) | ✅ | Ningún RF del SRS usa conjunciones que combinen dos comportamientos verificables por separado; revisión manual línea por línea. |
| C6 | **Feasible** (factible dentro de restricciones conocidas) | ✅ | Los 28 RF en estado ✅ Implementado son la prueba directa de factibilidad; los 3 en estado 🟡 Modelado (RF-19 a RF-21) tienen el esquema de base de datos ya migrado, reduciendo el riesgo restante a la capa de API. |
| C7 | **Verifiable** (verificable con criterio finito) | ✅ | 31 de 31 RF referencian una prueba automatizada nombrada explícitamente en su propia entrada del SRS (columna `prueba_automatizada` de la matriz). |
| C8 | **Correct** (representa fielmente la necesidad) | 🟡 | RF-11b (peso/altura) está `Implementado sin resolución ética` — el requisito es correcto técnicamente pero su base legal no está resuelta (hallazgo H-06 de `ETHICS.md`), lo que cuestiona si debería estar habilitado como está. |
| C9 | **Conforming** (sigue la plantilla y el estilo de la organización) | ✅ | Los 31 RF siguen el mismo patrón `[condición] [sujeto] deberá [acción] [objeto] [restricción]`, sin excepciones. |

## Características del conjunto de requisitos (C10–C15)

| # | Característica | Estado | Evidencia |
|---|---|---|---|
| C10 | **Complete** (el conjunto cubre el alcance declarado) | 🟡 | El módulo `Equipo`/`Partido` (esquema `V16`) no tiene requisito funcional formal en el SRS pese a estar migrado — está declarado como `Planificado — solo esquema` en la matriz, pero no como un RF con estado explícito. |
| C11 | **Consistent** (sin contradicciones internas) | 🟡 | Desincronización declarada: RF-17, RF-18 y RF-22 figuran `✅ Implementado` en el SRS pero `docs/requisitos/historias-usuario.md` y `docs/requisitos/casos-uso.md` aún los marcan como pendientes — mismo dato, dos estados distintos según el archivo. |
| C12 | **Feasible** (el conjunto es alcanzable con los recursos del equipo) | ✅ | 90,3 % de los RF ya están implementados por un equipo de tres integrantes en tres entregas previas — evidencia empírica de factibilidad, no una proyección. |
| C13 | **Comprehensible** (legible por un tercero sin explicación adicional) | ✅ | El SRS incluye glosario, prioridad y estado por requisito; este mismo documento de checklist pudo evaluarse contra él sin necesitar contexto oral del equipo. |
| C14 | **Able to be validated** (el conjunto admite un procedimiento de validación) | ✅ | Validación continua contra pruebas automatizadas nombradas (C7) más el ciclo de observaciones docente-equipo documentado en `docs/observaciones/OBSERVACIONES.md`. |
| C15 | **Correct** (el conjunto refleja el problema real, sin desviaciones) | 🟡 | Ver C11: mientras el SRS y los archivos de historias/casos de uso no estén sincronizados, "el conjunto" no tiene una única fuente de verdad consistente — el SRS se declara como la autoritativa, pero el documento académico depende también de los otros dos. |

## Resumen

| Nivel | Cumple | Parcial | No cumple |
|---|---|---|---|
| Individual (C1–C9) | 7 | 2 | 0 |
| Conjunto (C10–C15) | 3 | 3 | 0 |
| **Total (15)** | **10** | **5** | **0** |

Ninguna característica está en estado "No cumple". Las cinco parciales
comparten una causa común: el crecimiento del sistema (RF-16 a RF-30,
agregados entre el 3 y el 13 de agosto) superó la velocidad a la que se
actualizaron `historias-usuario.md`, `casos-uso.md` y los campos de
prioridad del SRS. **Pendiente concreto antes del cierre de la Entrega
Final:** una pasada de sincronización de esos tres archivos contra el
SRS vigente, y la resolución de la base legal de RF-11b (H-06).
