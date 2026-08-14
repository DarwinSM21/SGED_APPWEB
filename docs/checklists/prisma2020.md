# Checklist PRISMA 2020 (reducido a escala de PFC) — Bloque B.6 / E

Aplicado a la revisión de trabajos relacionados de SGED
(`docs/informe/main.tex`, capítulo "Trabajos relacionados"). Sigue la
lista de verificación de Page *et al.* (2021), *The PRISMA 2020
statement*, con la reducción de escala que la propia Guía de la Entrega
Final prevé para un PFC (Kitchenham y Charters, 2007, adaptado). Los
ítems que no aplican a una revisión de alcance reducido —no financiada,
sin protocolo preregistrado, sin evaluación formal de riesgo de sesgo
entre estudios— se marcan **N/A (reducción declarada)** en vez de
omitirse, siguiendo la misma disciplina de honestidad que el resto del
proyecto.

| # | Ítem PRISMA 2020 | Estado | Ubicación en el informe |
|---|---|---|---|
| 1 | Título identifica el informe como revisión | N/A (reducción declarada) — es un capítulo dentro del informe, no un artículo de revisión independiente | §Trabajos relacionados |
| 2 | Resumen estructurado de la revisión | 🟡 Parcial — el resumen general del informe no dedica una sección propia a la revisión; el capítulo sí resume alcance y hallazgo al final | §Trabajos relacionados, párrafo de brecha identificada |
| 3 | Racional: por qué se hace la revisión | ✅ | §Trabajos relacionados, primer párrafo |
| 4 | Objetivos explícitos (PICO o equivalente) | ✅ — seis ejes temáticos declarados en vez de PICO clínico, adaptación razonable al dominio de ingeniería de software | Lista de 6 ejes, §Estrategia de búsqueda |
| 5 | Registro del protocolo (PROSPERO o similar) | N/A (reducción declarada) — no aplica a un PFC, no es una revisión clínica ni se registra en un repositorio de protocolos | — |
| 6 | Criterios de elegibilidad | ✅ | Criterios de inclusión y exclusión explícitos, §Estrategia de búsqueda |
| 7 | Fuentes de información y fecha de última búsqueda | 🟡 Parcial — se declaran los ejes y la ventana temporal (2016–2026), pero no la fecha exacta de la última ejecución de búsqueda | §Estrategia de búsqueda |
| 8 | Estrategia de búsqueda completa (al menos una base de datos) | ✅ — 9 cadenas de búsqueda con operadores booleanos, una por eje temático más subdivisiones | §Estrategia de búsqueda, lista enumerada |
| 9 | Proceso de selección de estudios | ✅ | Diagrama de flujo (Figura `fig:prisma`): 85 → 28 → 12 → 8 |
| 10 | Proceso de extracción de datos | 🟡 Parcial — la tabla comparativa (`tab:trabajos-relacionados`) declara qué columnas se extrajeron (año, dominio, pila, patrones, evaluación empírica, limitaciones, diferencia), pero no si la extracción fue por una sola persona o verificada por dos | Tabla `tab:trabajos-relacionados` |
| 11 | Lista de ítems de datos extraídos | ✅ | Columnas de la tabla comparativa: referencia, año, dominio, pila, patrones, evaluación empírica, limitaciones, diferencia |
| 12 | Métodos para evaluar riesgo de sesgo de cada estudio | N/A (reducción declarada) — no se aplicó una herramienta formal de riesgo de sesgo (p. ej. ROBIS); se sustituyó por el criterio de exclusión "autoría o venue no verificable contra fuente primaria" | §Estrategia de búsqueda, criterios de exclusión |
| 13a | Métodos de síntesis (elegibilidad para cada síntesis) | ✅ — síntesis narrativa y tabular, no metaanálisis cuantitativo (apropiado para el tipo de estudios incluidos) | §Síntesis comparativa |
| 13b–13f | Preparación de datos, métodos estadísticos, exploración de heterogeneidad | N/A (reducción declarada) — no aplica: no hay metaanálisis cuantitativo de resultados numéricos entre estudios heterogéneos | — |
| 14 | Métodos para evaluar certeza de la evidencia (GRADE o similar) | N/A (reducción declarada) | — |
| **Resultados** | | | |
| 16a | Resultados del proceso de selección con diagrama de flujo | ✅ | Figura `fig:prisma`: 85 identificados, 57 excluidos por no ser contenido académico, 28 tras deduplicar, 16 excluidos por dominio, 12 elegibles, 4 excluidos por autoría/venue no verificable, 8 incluidos |
| 17 | Características de los estudios incluidos | ✅ | Tabla `tab:trabajos-relacionados`, 8 filas |
| 18 | Riesgo de sesgo en los estudios incluidos | N/A (reducción declarada), ver ítem 12 | — |
| 20a–20d | Resultados de síntesis individuales | ✅ | Tabla comparativa + párrafo de brecha identificada |
| 23a–23d | Discusión: interpretación, limitaciones, implicaciones | ✅ | Párrafo de brecha identificada, §Trabajos relacionados, y retomado en Discusión general del informe |
| **Otra información** | | | |
| 24a–24c | Registro y protocolo | N/A (reducción declarada), ver ítem 5 | — |
| 25 | Apoyo (financiamiento) | ✅ | §Declaraciones obligatorias — sin financiamiento externo |
| 26 | Conflictos de interés de los autores de la revisión | ✅ | §Declaraciones obligatorias — sin conflictos declarados |
| 27 | Disponibilidad de datos, código y otros materiales | 🟡 Parcial — el repositorio es público, pero no existe un archivo `.csv`/`.bib` versionado con el registro crudo de las 85 búsquedas iniciales (ver `docs/mediciones/DATA-PROVENANCE.md`, pendiente declarado #2) | `docs/informe/referencias.bib` |

## Resumen

| Estado | Cantidad |
|---|---|
| ✅ Cumple | 12 |
| 🟡 Parcial | 5 |
| N/A (reducción declarada, justificada) | 6 |

**Pendiente concreto antes del cierre:** archivar el registro crudo de
búsqueda (fecha exacta de ejecución, cadenas por eje, conteos por
fuente) como un archivo versionado bajo `docs/mediciones/`, en vez de
que los conteos vivan solo como texto narrativo en el informe — cierra
los ítems 7, 10 y 27 y el pendiente #2 de `DATA-PROVENANCE.md`.
