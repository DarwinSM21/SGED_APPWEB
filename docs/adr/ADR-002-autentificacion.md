# ADR-002: Estrategia de Autenticación y Autorización

## Estado
Aprobado — **revisado el 2026-07-30 para que coincida con lo implementado**
(ver nota al final).

## Contexto
El sistema requiere un mecanismo robusto, seguro y escalable para gestionar la autenticación de usuarios (estudiantes, representantes, entrenadores, administradores) y el control de acceso a los diferentes endpoints de la API y módulos del frontend. Se evaluaron opciones como Supabase Auth de forma nativa frente a una solución desacoplada basada en tokens propios. Dado que el sistema debe ser portable y no depender de servicios externos propietarios para su lógica de negocio core, se decidió estructurar un esquema propio integrado directamente en el ecosistema del backend.

## Decisión
Implementar un esquema de autenticación **Stateless** basado en **JWT (JSON Web Tokens)** gestionado de forma nativa por el backend mediante **Spring Security 6.x** (incluido en Spring Boot 3.2.5) y la librería `io.jsonwebtoken`.

*   Las contraseñas de los usuarios serán encriptadas antes de su almacenamiento utilizando el algoritmo de hashing **BCrypt** con un factor de costo de 12.
*   El backend expondrá un endpoint público `POST /api/auth/login` que validará las credenciales y emitirá un token JWT de vida corta.
*   Los roles de usuario (ADMINISTRADOR, ENTRENADOR, etc.) se almacenarán directamente en la base de datos relacional dentro de la tabla `seguridad.usuario_rol` y se inyectarán como *claims* (autoridades) dentro del payload del token.
*   El JWT se entrega exclusivamente en una **cookie `HttpOnly` + `Secure` + `SameSite=Strict`**, nunca en el cuerpo de la respuesta ni en un header legible por JavaScript. El frontend (Angular) **no** almacena el token en `localStorage` ni lo adjunta manualmente: el navegador reenvía la cookie automáticamente (`withCredentials: true`), y el filtro `JwtAuthenticationFilter` la lee del lado del servidor. Detalle completo de esta decisión y de la revocación por lista negra en Redis: ver [ADR-007](ADR-007-jwt-cookie-redis.md).

## Consecuencias
*   **Positivas:**
    *   **Arquitectura Stateless:** El servidor no necesita almacenar sesiones en memoria, lo que facilita el escalado horizontal.
    *   **Desacoplamiento total:** Se elimina la dependencia directa con proveedores de identidad externos (Vendor Lock-in de Supabase Auth).
    *   **Control granular:** Mayor flexibilidad para personalizar claims en el token y aplicar anotaciones como `@PreAuthorize` a nivel de método en los controladores.
    *   **Sin superficie de ataque XSS sobre el token:** al no ser accesible desde JavaScript, un script inyectado no puede leer ni exfiltrar el JWT.
*   **Negativas / Desafíos:**
    *   **Invalidación de tokens:** al ser *stateless*, revocar un token antes de su expiración (logout) exige un mecanismo adicional — resuelto con una lista de revocación en Redis indexada por `jti` (ADR-008).
    *   **CSRF:** una cookie enviada automáticamente por el navegador es susceptible a *Cross-Site Request Forgery* si no se mitiga; se mitiga con `SameSite=Strict`, que impide que la cookie viaje en peticiones iniciadas desde otro origen.

---

> **Nota de corrección (2026-07-30).** La versión anterior de este ADR
> describía **JWT en `localStorage` con un `JwtInterceptor` agregando
> `Authorization: Bearer <token>`**, y citaba "Spring Security 7.x". Ninguna
> de las dos cosas es lo que el código hace: no hay `localStorage` ni
> cabecera `Bearer` en `frontend/src/app/auth/` (verificado — cero
> coincidencias), y la versión real es Spring Security 6.x vía Spring Boot
> 3.2.5 (`backend/pom.xml`). Es el mismo tipo de inconsistencia
> documento-vs-código que ya se había observado en la Entrega 1A (OBS-03:
> el ADR-001 original describía PHP/Laravel mientras el repositorio
> construía Spring Boot). Se corrige aquí por la misma razón: un ADR que no
> coincide con el código no sirve como evidencia, sirve como motivo de
> observación.