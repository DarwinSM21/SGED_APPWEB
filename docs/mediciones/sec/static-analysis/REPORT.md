# Análisis estático de inyección SQL — SpotBugs + find-sec-bugs (Bloque A.2.3)

- Fecha: 2026-08-14
- Commit: 35188d4
- Herramienta: `spotbugs-maven-plugin` 4.8.6.4 + `findsecbugs-plugin` 1.13.0
- Entorno de ejecución: contenedor `maven:3.9-eclipse-temurin-21` (el SpotBugs
  4.8.6.4 usado no reconoce todavía el bytecode de Java 25 — ver nota más
  abajo — así que la corrida se hizo con JDK 21, la misma versión objetivo
  del proyecto).
- Filtro de inclusión: `backend/spotbugs-security-include.xml`, restringido
  a los detectores de inyección SQL: `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE`
  (la regla que exige explícitamente el Bloque A.2.3) y sus variantes
  `SQL_INJECTION_JDBC`, `SQL_INJECTION_JPA`, `SQL_INJECTION_HIBERNATE`,
  `SQL_INJECTION_SPRING_JDBC`.

## Resultado

```
BugInstance size is 0
Error size is 0
No errors/warnings found
```

**Cero hallazgos** de los patrones de inyección SQL sobre los 148 archivos
fuente del backend. Consistente con la auditoría manual de
`scripts/audit-sql-dynamic.sh` (sección "Ausencia de SQL dinámico" del
informe) y con el hecho de que todo acceso a datos usa Spring Data JPA
parametrizado o procedimientos almacenados invocados con `@Procedure`
(JPA 2.1), nunca `createNativeQuery` con concatenación de cadenas.

Reporte completo (XML, formato SpotBugs estándar): `spotbugsXml.xml` en
este mismo directorio.

## Nota sobre el entorno de ejecución

Al ejecutar `spotbugs-maven-plugin:4.8.6.4:check` con el JDK 25 instalado
en la máquina de desarrollo, el análisis falla con
`IllegalArgumentException: Unsupported class file major version 69`: la
versión de la librería ASM que empaqueta SpotBugs 4.8.6.4 no reconoce
todavía el bytecode que emite el propio *runtime* de Java 25 al cargar
sus clases base (`java.util.Set`, `java.time.ZoneId`, etc., necesarias
para el análisis, no el bytecode compilado del proyecto — el backend
compila con `--release 21` sin cambios). La corrida documentada aquí se
hizo dentro de un contenedor con JDK 21 para evitar el problema; el
mismo comando debería funcionar directo en cualquier máquina cuyo
`JAVA_HOME` apunte a JDK 21, o con una versión más reciente de
`spotbugs-maven-plugin` que ya reconozca Java 25.
