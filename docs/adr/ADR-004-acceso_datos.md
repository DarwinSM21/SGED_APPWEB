# ADR-004: Estrategia de Acceso a Datos (ORM vs Procedimientos Almacenados)

## Estado
Aprobado (revisado para la Entrega Final: la regla 2 original —"prohibida la
lógica en Procedimientos Almacenados"— quedó reemplazada por la decisión
real que el equipo terminó implementando; ver Contexto de la revisión).

## Contexto
Es necesario estandarizar cómo el backend desarrollado en Spring Boot interactúa con el motor de base de datos PostgreSQL. Se debatió entre un enfoque puramente relacional y procedural en la base de datos (utilizando funciones PL/pgSQL y procedimientos almacenados invocados mediante JDBC nativo) frente a un enfoque moderno basado en Mapeo Objeto-Relacional (ORM). El equipo busca maximizar la velocidad de desarrollo, garantizar la mantenibilidad del código y aprovechar las herramientas tipadas del framework, sin sacrificar el rendimiento en operaciones pesadas.

**Contexto de la revisión (Entrega Final):** la Guía de la Entrega Final (Bloque A.2) exige explícitamente un mínimo de seis procedimientos o funciones SQL versionados, uno por categoría funcional (consultas multi-tabla, cálculos agregados, reportes, actualizaciones masivas, validaciones cruzadas y generación de códigos secuenciales), invocados desde Java mediante los mecanismos formales de JPA 2.1 (`@Procedure` o `@NamedStoredProcedureQuery`) y con prohibición expresa de SQL dinámico. Esto es lo opuesto a lo que la versión anterior de esta decisión establecía (regla 2, abajo tachada): en la práctica el equipo ya había empezado a implementar `PROCEDURE`s reales de PostgreSQL antes de que esta ADR se corrigiera, y `docs/basedatos/CATALOGO-SP.md` documenta las seis que existen hoy. Se actualiza el texto para que la decisión declarada coincida con la decisión real, en vez de dejar dos fuentes contradictorias.

## Decisión
Adoptar una estrategia híbrida con fuerte prioridad en el uso de un **ORM**, implementado a través de **Spring Data JPA (Hibernate)** como el estándar principal para la mayoría de las operaciones del sistema (operaciones CRUD, consultas paginadas, filtrados dinámicos y relaciones de entidades).

Para las seis categorías que la Guía de la Entrega Final identifica como excepción, se usan **procedimientos almacenados reales de PostgreSQL** (`CREATE PROCEDURE`, no `FUNCTION`: solo un procedimiento real acepta `CALL` desde `@Procedure`/`CallableStatement`; ver la nota en cada archivo de `db/procs/`):
1.  Las operaciones básicas e intermedias se realizan mediante Repositorios de Spring Data (`JpaRepository`), aprovechando las facilidades de generación de consultas por convención de nombres o anotaciones `@Query` (JPQL/HQL).
2.  ~~Queda terminantemente prohibida la lógica de negocio compleja dentro de Procedimientos Almacenados o Triggers~~ — regla descartada (ver Contexto de la revisión). Cálculos agregados, actualizaciones masivas, validaciones cruzadas, generación de códigos secuenciales, reportes y consultas multi-tabla se implementan como procedimientos versionados en `db/procs/`, catalogados en `docs/basedatos/CATALOGO-SP.md`, sin lógica de negocio ajena a esa operación puntual (nada de reglas de autorización ni de flujo de aplicación dentro del procedimiento).
3.  Se prohíbe expresamente cualquier SQL dinámico (`EXECUTE IMMEDIATE`, `sp_executesql`, concatenación de cadenas para construir una consulta) tanto en Java como dentro de los procedimientos; se audita automáticamente con `scripts/audit-sql-dynamic.sh`.

## Consecuencias
*   **Positivas:**
    *   **Alta velocidad de desarrollo:** Reducción drástica del código repetitivo (Boilerplate) para operaciones CRUD estándar vía ORM.
    *   **Seguridad integrada:** Spring Data JPA reduce nativamente el riesgo de ataques por inyección SQL en el 90% de las operaciones; los procedimientos usan siempre parámetros nombrados (`IN`/`OUT`), nunca concatenación.
    *   **Tipado fuerte:** Las entidades Java mapean directamente los esquemas de PostgreSQL, facilitando el refactor.
    *   **Trazabilidad:** cada procedimiento vive en un archivo `.sql` versionado con su propósito documentado, catalogado en `docs/basedatos/CATALOGO-SP.md` y trazado en `docs/trazabilidad/matriz.csv` con `tipo_acceso = SP`.
*   **Negativas / Desafíos:**
    *   **Problema del N+1:** Riesgo latente de generar múltiples consultas a la base de datos si no se configuran correctamente las estrategias de carga (`FetchType.LAZY` vs `FetchType.EAGER`). Exige revisiones periódicas del log de consultas en desarrollo.
    *   **Portabilidad reducida en la porción SP:** la lógica en `plpgsql` ata esa porción del sistema a PostgreSQL; se acepta el costo porque el proyecto no contempla cambiar de motor.
    *   **Doble mecanismo de invocación:** un desarrollador nuevo debe saber cuándo usar `JpaRepository` y cuándo un `@Procedure`; se mitiga documentando la categoría funcional de cada excepción en el catálogo.