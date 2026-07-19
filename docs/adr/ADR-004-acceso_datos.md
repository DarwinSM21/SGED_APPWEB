# ADR-004: Estrategia de Acceso a Datos (ORM vs Procedimientos Almacenados)

## Estado
Aprobado

## Contexto
Es necesario estandarizar cómo el backend desarrollado en Spring Boot interactúa con el motor de base de datos PostgreSQL. Se debatió entre un enfoque puramente relacional y procedural en la base de datos (utilizando funciones PL/pgSQL y procedimientos almacenados invocados mediante JDBC nativo) frente a un enfoque moderno basado en Mapeo Objeto-Relacional (ORM). El equipo busca maximizar la velocidad de desarrollo, garantizar la mantenibilidad del código y aprovechar las herramientas tipadas del framework, sin sacrificar el rendimiento en operaciones pesadas.

## Decisión
Adoptar una estrategia híbrida con fuerte prioridad en el uso de un **ORM**, implementado a través de **Spring Data JPA (Hibernate)** como el estándar principal para el 90% de las operaciones del sistema (operaciones CRUD, consultas paginadas, filtrados dinámicos y relaciones de entidades).

Sin embargo, para garantizar el rendimiento óptimo del sistema, se establecen las siguientes reglas de excepción coordinadas con el equipo:
1.  Las operaciones básicas e intermedias se realizarán mediante Repositorios de Spring Data (`JpaRepository`), aprovechando las facilidades de generación de consultas por convención de nombres o anotaciones `@Query` (JPQL/HQL).
2.  Queda terminantemente prohibida la lógica de negocio compleja dentro de Procedimientos Almacenados o Triggers para mantener la portabilidad del software.
3.  Para consultas masivas, reportes complejos con agregaciones multifuente o procesos batch pesados (como el procesamiento masivo de pensiones mensuales o reportes anuales de asistencia), se permitirá el uso de **Queries Nativos de SQL** optimizados o proyecciones de Spring Data para evadir el sobrecosto de memoria del ciclo de vida de Hibernate.

## Consecuencias
*   **Positivas:**
    *   **Alta velocidad de desarrollo:** Reducción drástica del código repetitivo (Boilerplate) para operaciones CRUD estándar.
    *   **Seguridad integrada:** Spring Data JPA reduce nativamente el riesgo de ataques por inyección SQL si se utilizan sus métodos estándar y JPQL parametrizado.
    *   **Tipado fuerte:** Las entidades Java mapean directamente los esquemas de PostgreSQL, facilitando el refactor.
*   **Negativas / Desafíos:**
    *   **Problema del N+1:** Riesgo latente de generar múltiples consultas a la base de datos si no se configuran correctamente las estrategias de carga (`FetchType.LAZY` vs `FetchType.EAGER`). Exige revisiones periódicas del log de consultas en desarrollo.