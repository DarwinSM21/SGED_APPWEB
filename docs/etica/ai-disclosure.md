# Declaración de uso de Inteligencia Artificial generativa

**Sistema:** SGED — Sistema de Gestión para la Escuela Deportiva ProFútbol
**Equipo:** Darwin Arcalle Grefa, Alejandro Pallo Pinto, Ricardo Velez Lopez —
Universidad Técnica Estatal de Quevedo (UTEQ)

---

## 1. Por qué este documento existe por separado

`CONTRIBUTORS.md` ya incluye una declaración breve de uso de IA (sección
"Declaración de asistencia de Inteligencia Artificial"). Este documento no la
reemplaza ni la contradice: la **amplía** con el detalle que exige la guía de
evaluación — herramienta, fase del proyecto, propósito específico y revisión
posterior del equipo — y queda archivado junto al resto de la documentación
ética (`docs/etica/`), no solo en la lista de contribuidores.

## 2. Herramienta

**Claude Code** (Anthropic), un asistente de IA generativa operado desde la
línea de comandos, usado por **Alejandro Pallo Pinto** en sesiones de trabajo
individuales a lo largo del proyecto. No se usó ningún otro asistente de IA
generativa (ChatGPT, Copilot, Gemini u otro) para escribir código o
documentación de este repositorio por parte de Alejandro.

> **Nota sobre Darwin Arcalle Grefa y Ricardo Velez Lopez:** este documento
> solo puede dar fe, de primera mano, del uso hecho por Alejandro Pallo Pinto.
> Si Darwin o Ricardo usaron algún asistente de IA generativa en sus propias
> sesiones de trabajo, deben añadir su propia declaración en esta misma
> sección antes de la entrega — no se asume ni se descarta su uso a falta de
> esa confirmación, siguiendo el mismo criterio de no rellenar con valores no
> confirmados ya aplicado al ORCID pendiente en `CITATION.cff`.

## 3. Fases del proyecto en que se usó, y con qué propósito

| Fase | Propósito del uso de IA |
|---|---|
| Hardening de seguridad (JWT/cookies, CSP, CORS, auditoría OWASP) | Revisión y corrección de código de autenticación y de las cabeceras de seguridad; redacción de `scripts/audit-owasp.sh`. |
| Reestructuración en paquetes `academico`/`deportivo`/`seguridad` | Identificación de clases stub vacías y de duplicación entre capas; propuesta de la reorganización, revisada y aprobada por el equipo antes de aplicarse. |
| Documentación técnica (SRS, ADRs, matriz de trazabilidad, `DATA-DICTIONARY.md`, `ETHICS.md`) | Redacción de borradores a partir del esquema y del código real del repositorio, no de descripciones genéricas. |
| Generación de evidencia técnica | Automatización y lectura de resultados de JaCoCo, k6 y Lighthouse; la ejecución de las herramientas y los números resultantes son reales, no generados por la IA. |
| Funcionalidades del módulo deportivo (horario semanal, historial de asistencia del estudiante, KPI de ingresos) | Implementación de código, con pruebas automatizadas y verificación manual contra la aplicación corriendo antes de cada commit. |
| Depuración de errores en producción (CORS entre el celular y el backend, desfase de zona horaria UTC vs. Ecuador) | Diagnóstico a partir de logs reales del contenedor, no de suposiciones. |
| Redacción de documentación de cierre (`LICENSE`, `CITATION.cff`, `CONTRIBUTORS.md`, `CHANGELOG.md`, `VERSIONING.md`, este archivo) | Redacción de texto; los datos que contienen (autores, fechas, cifras de `git log`) provienen del historial real del repositorio. |

## 4. Qué no hizo la IA

Las decisiones de arquitectura, las decisiones de seguridad y la verificación
de que el sistema funciona correctamente fueron tomadas y revisadas por
Alejandro Pallo Pinto en cada sesión — nunca aplicadas de forma autónoma. Todo
cambio de código pasó por compilación, la suite de pruebas automatizadas, y en
la mayoría de los casos verificación manual contra la aplicación corriendo
antes de integrarse. Los commits de este repositorio no incluyen atribución de
coautoría a IA (`Co-Authored-By`): la autoría de cada commit corresponde
únicamente a la persona que lo realizó y lo empujó al repositorio con su
propia cuenta.

## 5. Referencias

- Declaración breve equivalente: [`CONTRIBUTORS.md`](../../CONTRIBUTORS.md),
  sección "Declaración de asistencia de Inteligencia Artificial".
- Guía de transparencia seguida: SWEBOK v4.0.
