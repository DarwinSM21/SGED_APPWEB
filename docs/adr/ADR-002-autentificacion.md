# ADR-002: Estrategia de Autenticación y Autorización

## Estado
Aprobado

## Contexto
El sistema requiere un mecanismo robusto, seguro y escalable para gestionar la autenticación de usuarios (estudiantes, representantes, entrenadores, administradores) y el control de acceso a los diferentes endpoints de la API y módulos del frontend. Se evaluaron opciones como Supabase Auth de forma nativa frente a una solución desacoplada basada en tokens propios. Dado que el sistema debe ser portable y no depender de servicios externos propietarios para su lógica de negocio core, se decidió estructurar un esquema propio integrado directamente en el ecosistema del backend.

## Decisión
Implementar un esquema de autenticación **Stateless** basado en **JWT (JSON Web Tokens)** gestionado de forma nativa por el backend mediante **Spring Security 7.x** y la librería `io.jsonwebtoken`. 

*   Las contraseñas de los usuarios serán encriptadas antes de su almacenamiento utilizando el algoritmo de hashing **BCrypt** con un factor de costo de 12.
*   El backend expondrá un endpoint público `POST /api/auth/login` que validará las credenciales y emitirá un token JWT de vida corta.
*   Los roles de usuario (ADMINISTRADOR, ENTRENADOR, etc.) se almacenarán directamente en la base de datos relacional dentro de la tabla `seguridad.usuario_rol` y se inyectarán como *claims* (autoridades) dentro del payload del token.
*   El frontend (Angular) almacenará el token en el `localStorage` e implementará un `JwtInterceptor` para adjuntarlo automáticamente en la cabecera `Authorization: Bearer <token>` de cada petición HTTP saliente.

## Consecuencias
*   **Positivas:** 
    *   **Arquitectura Stateless:** El servidor no necesita almacenar sesiones en memoria, lo que facilita el escalado horizontal.
    *   **Desacoplamiento total:** Se elimina la dependencia directa con proveedores de identidad externos (Vendor Lock-in de Supabase Auth).
    *   **Control granular:** Mayor flexibilidad para personalizar claims en el token y aplicar anotaciones como `@PreAuthorize` a nivel de método en los controladores.
*   **Negativas / Desafíos:**
    *   **Invalidadación de Tokens:** Al ser stateless, revocar un token antes de su fecha de expiración (por ejemplo, en un Logout) requiere de mecanismos adicionales como una lista de bloqueo en memoria caché.
    *   **Seguridad del lado del cliente:** Almacenar tokens en `localStorage` introduce una superficie de ataque ante vulnerabilidades del tipo XSS (Cross-Site Scripting), lo que obliga a extremar las medidas de sanitización de datos en el frontend Angular.