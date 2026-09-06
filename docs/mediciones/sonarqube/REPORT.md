# Análisis de calidad de código — SonarQube (complemento a SpotBugs/find-sec-bugs)

- Fecha: 2026-09-05
- Commit: d5bfaae
- Herramienta: SonarQube Community Edition 26.9.0 (contenedor
  `sonarqube:community`), analizado con `sonarsource/sonar-scanner-cli:latest`.
- Alcance: `backend/src/main/java` y `backend/src/test/java` (200 clases,
  9355 líneas de código sin comentarios), con el reporte de cobertura
  JaCoCo (`target/site/jacoco/jacoco.xml`) importado para correlacionar
  hallazgos con líneas realmente cubiertas por prueba.
- SpotBugs/find-sec-bugs (`docs/mediciones/sec/static-analysis/`) audita
  específicamente inyección SQL con un filtro restringido; SonarQube
  analiza el código completo sin ese filtro — cubre bugs, vulnerabilidades,
  *code smells*, duplicación y complejidad, no solo el patrón de SQL
  dinámico. Son complementarios, no redundantes.

## Resultado

| Métrica | Valor |
|---|---|
| Quality Gate | **OK** (Sonar way, sin condiciones incumplidas) |
| Bugs | **0** |
| Vulnerabilidades | **0** |
| *Security hotspots* | **0** |
| *Code smells* | 64 (0 *blocker*, 29 *critical*, 4 *major*, 9 *minor*, 22 *info*) |
| Cobertura (recalculada por SonarQube desde el XML de JaCoCo) | 81,6 % |
| Líneas duplicadas | 1,1 % |
| Calificación de fiabilidad | A |
| Calificación de seguridad | A |
| Calificación de mantenibilidad | A |
| Deuda técnica estimada | 393 minutos (≈ 6,5 h) |
| Complejidad ciclomática / cognitiva | 1121 / 532 |

**Cero *bugs* y cero vulnerabilidades** sobre las 9355 líneas analizadas.
Los 64 *code smells* son en su totalidad hallazgos de mantenibilidad, no
de corrección ni de seguridad. Desglose de los 29 *critical* por regla:

- **`java:S1192`** (23 casos) — "Define a constant instead of duplicating
  this literal". El patrón dominante: mensajes de error como
  `"Estudiante no encontrado con id: "` o `"Categoría no encontrada: "`
  repetidos 3 a 5 veces dentro del mismo servicio
  (`EstudianteService.java:94`, `CategoriaService.java:62`,
  `UsuarioService.java:83`, entre otros), y claves de mapas repetidas en
  `GlobalExceptionHandler.java:29` (`"timestamp"`, 10 veces) y
  `ReporteService.java` (nombres de columna de reporte, hasta 5 veces).
  Ninguno es un defecto funcional: es duplicación de literales que
  convendría extraer a constantes.
- **`java:S1186`** (4 casos) — métodos vacíos sin comentario que explique
  por qué, en `AuditoriaAspectTest.java` (3, *pointcuts* declarativos de
  AspectJ que no necesitan cuerpo) y `BackendApplicationTests.java` (1,
  `contextLoads()`, el patrón estándar de Spring Boot para verificar que
  el contexto levanta).
- **`java:S3776`** (2 casos) — complejidad cognitiva sobre el umbral de 15:
  `AlineacionService.java:88` (20) y
  `SesionEntrenamientoService.java:174` (19). Son, respectivamente, la
  regla de selección del once titular (sección~\ref{sec:iso25010}, fila
  ISO C10 del informe) y el conteo de asistencia por estado de
  `historial()` — ambos con lógica de negocio real, no *spaghetti code*
  accidental, pero con margen real de extracción de métodos.

## Advertencias del propio scanner (declaradas, no ocultas)

El scanner reportó 3 advertencias sobre la configuración de esta corrida,
que se declaran explícitamente en vez de omitirlas:

1. `sonar.scm.provider` no configurado — no se auto-detectó el sistema de
   control de versiones (el análisis corrió sobre el árbol de trabajo
   dentro de un contenedor, sin el directorio `.git`).
2 y 3. `sonar.java.libraries` / `sonar.java.test.libraries` no declarados
   — el análisis no tuvo acceso a los JAR de dependencias de Maven, así
   que algunas reglas que requieren resolución de tipos de librerías
   externas pueden ser menos precisas. No afecta a `java:S1192`,
   `java:S1186` ni `java:S3776` (las tres reglas de este reporte son
   sintácticas, no dependen de resolución de tipos externos).

## Reproducibilidad

```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
# tras healthcheck en http://localhost:9000/api/system/status:
docker run --rm --network host -v "$(pwd)/backend:/usr/src" -w /usr/src \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.projectKey=sged-appweb \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<token> \
  -Dsonar.sources=src/main/java -Dsonar.tests=src/test/java \
  -Dsonar.java.binaries=target/classes \
  -Dsonar.java.test.binaries=target/test-classes \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

Requiere `mvn verify` ejecutado antes (para que existan `target/classes`,
`target/test-classes` y el XML de JaCoCo). No se integró como paso de CI
en esta entrega: requeriría levantar un servidor SonarQube persistente
(no una instancia efímera como esta), lo cual excede el alcance de este
cierre y se declara como trabajo futuro en vez de improvisarlo a días
del examen.

Datos crudos completos: `measures.json`, `quality-gate.json`,
`issues.json` (los 64 *code smells* con archivo, línea y regla) en este
mismo directorio.
