# ADR-004: Estilo Arquitectónico de la API (REST/HTTP)

## Estado
Aprobado

## Contexto
El sistema expone una API de backend consumida por un frontend Angular en el
navegador. Antes de fijar contratos y controladores hubo que elegir el estilo
arquitectónico de la interfaz de servicio: determina cómo se modelan los
recursos, cómo se implementa la autenticación, cómo se versionan los
contratos y qué herramientas de integración soporta el ecosistema. Las
opciones reales consideradas fueron tres.

**Opción A — API REST (Fielding).** Modelar el dominio como recursos
identificables por URL, operados con los verbos HTTP estándar
(`GET`/`POST`/`PUT`/`PATCH`/`DELETE`), con estado de la aplicación trasladado
al cliente y respuestas *stateless* lado servidor.

**Opción B — RPC/endpoints orientados a acciones.** Exponer operaciones
arbitrarias tipo `POST /api/estudiante/matricular` o `POST /api/entrenador/generar-pdf`,
sin imponer una correspondencia 1:1 entre verbo HTTP y semántica del recurso.

**Opción C — GraphQL.** Un único endpoint que recibe consultas tipadas y
resuelve los campos que el cliente pide, con esquema autodescrito y
resolución de sobre/sub-captura de datos.

## Decisión
Se adopta **REST/HTTP** (Opción A). La API del backend de SGED sigue las
restricciones que Fielding identificó para sistemas distribuidos orientados
a recursos: interfaz uniforme vía verbos estándar, *statelessness* del lado
del servidor por petición, capacidad de *caching* y sistema en capas. Cada
dominio de negocio (seguridad, académico, deportivo, inventario) se expone
como un conjunto de controladores `@RestController` bajo `/api/<dominio>/...`
con correspondencia directa entre el nombre del recurso y su verbo HTTP.

La elección es coherente con la autenticación *stateless* por JWT en cookie
`HttpOnly` (ADR-002, ADR-008): al no mantener sesión en el servidor, cada
petición restaure su propio contexto de seguridad a partir del token;
`@PreAuthorize` a nivel de método autoriza o deniega cada operación sobre el
recurso. La documentación del contrato se genera con OpenAPI/springdoc, y el
consumo desde Angular se hace con `HttpClient` y URLs por recurso.

## Consecuencias
*   **Positivas:**
    *   **Semántica explícita y cacheable:** los verbos HTTP estandarizan el
        contrato; las respuestas `GET` admiten *caching* (p. ej. en Redis,
        ADR-005) sin inventar reglas.
    *   **Interoperabilidad y herramientas:** REST es el estilo con mejor
        soporte en el ecosistema (Spring, Angular, k6, ZAP, Postman) y el
        más fácil de verificar con mediciones reproducibles.
    *   **Menor superficie de contratos ad-hoc:** al modelar el dominio como
        recursos, el diseño se soporta en el propio problema de negocio y no
        en catálogos de acciones.
*   **Negativas / Desafíos:**
    *   **Sobre- o sub-captura de datos:** un recurso agregado puede traer
        más campos de los que una vista concreta necesita; se mitiga con DTO
        por operación en los paquetes `dto` de cada dominio.
    *   **Debilidad inherente para operaciones de servicio largas o por
        comando:** operaciones que mezclan varios recursos (registro con
        matrícula, cobro) se resuelven como transacciones de aplicación en
        el *service* de capa correspondiente, no exponiendo transiciones de
        estado arbitrarias como verbos propietarios.

---

Este ADR cubre el estilo arquitectónico de la API. La separación entre
CRUD vía ORM y operaciones complejas vía procedimientos almacenados se
documenta en ADR-006; la organización de esquemas en PostgreSQL, en
ADR-003; la estrategia de despliegue, en ADR-007.