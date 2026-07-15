# ADR-001: Selección del stack tecnológico del servidor

## Estado

Aceptado

## Contexto

El equipo necesita elegir el stack tecnológico del lado del servidor para implementar el
sistema ProFútbol como aplicación web. Restricciones:

1. El tiempo disponible es de 10 semanas de desarrollo efectivo.
2. El sistema requiere una API REST que Angular consuma vía HTTP/JSON.
3. Se necesita autenticación segura por roles.
4. El motor de base de datos es PostgreSQL.
5. El docente indicó que PHP 8.2, ASP.NET Core 8 y Java/JSP son las opciones
   válidas para el backend.

## Opciones consideradas

### Opción A: PHP 8.2 con Laravel 11

Framework full-stack, hosting económico.

### Opción B: ASP.NET Core 8

Robusto, tipado estático, requiere hosting dedicado.

### Opción C: Java 21 con Spring Boot 4.0.6

Ecosistema enterprise, JPA para ORM, Angular se comunica nativamente con backend
Java por HTTP/JSON.

## Decisión

Se elige la **Opción C: Java 21 con Spring Boot 3.2.5**.

Razones:

1. Spring Boot simplifica la creación de APIs REST con Spring MVC y accede a
   PostgreSQL via Spring Data JPA.
2. Angular (frontend) se comunica nativamente con el backend Java a través de
   peticiones HTTP/JSON, sin capas intermedias.
3. Spring Security proporciona autenticación JWT robusta con soporte para roles.
4. IntelliJ IDEA Community Edition es gratuito.
5. PostgreSQL es el motor elegido (sin costos de licencia, soporte JSONB,
   rendimiento superior en consultas complejas).

## Consecuencias positivas

- Ecosistema enterprise con documentación extensa.
- Tipado estático (Java) reduce errores en runtime.
- Angular + Spring Boot forman un stack coherente (TypeScript + Java, ambos tipados).
- Spring Security maneja JWT, roles y protección de endpoints de forma integrada.
- JPA/Hibernate mapea entidades Java a tablas PostgreSQL sin SQL manual.

## Consecuencias negativas

- Curva de aprendizaje de Spring Boot (~2 semanas). Mitigación: Spring Initializr
  genera la estructura base, documentación oficial de Spring.
- Requiere JDK 21 instalado. Mitigación: SDKMAN gestiona versiones de JDK
  fácilmente.
- Hosting Java más costoso que hosting PHP compartido. Mitigación: Heroku/Railway/
  Render ofrecen tier gratuito para aplicaciones Java.
- Configuración inicial más compleja que Laravel. Mitigación: Spring Boot
  auto-configura la mayoría de componentes.