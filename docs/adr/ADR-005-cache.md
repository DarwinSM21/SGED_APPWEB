# ADR-005: Estrategia de Caché y Gestión del Estado de Sesión

## Estado
Aprobado

## Contexto
Debido a la naturaleza *stateless* (sin estado) de la arquitectura JWT en el backend, el servidor no almacena sesiones de usuario en memoria. No obstante, surgen requerimientos técnicos críticos que rompen esta premisa, como la necesidad de invalidar inmediatamente un token JWT cuando un usuario hace clic en "Cerrar Sesión" (Logout) antes de la fecha de expiración del token, o mitigar la latencia en consultas repetitivas de datos altamente estáticos (como tablas de parametrización de roles, listados de configuraciones del sistema o categorías deportivas).

## Decisión
Implementar un mecanismo de almacenamiento en caché en memoria utilizando **Redis** como componente de soporte del backend de Spring Boot. Esta infraestructura se utilizará específicamente para cubrir dos escenarios:

1.  **Lista de Bloqueo de JWT (Token Blacklist):** Al invocar el endpoint `POST /api/auth/logout`, el token del usuario se registrará en Redis con un tiempo de vida (TTL) equivalente al tiempo restante para su expiración original. El filtro `JwtAuthenticationFilter` consultará a Redis en cada petición; si el token se encuentra en la lista de bloqueo, la solicitud será rechazada de inmediato con un código HTTP 401.
2.  **Caché de Datos Semiestáticos:** Se implementará la abstracción de caché de Spring (`@Cacheable`, `@CacheEvict`) respaldada por Redis para almacenar respuestas de endpoints cuyos datos cambian con muy poca frecuencia (ej. el catálogo de roles de `seguridad` o las categorías de los equipos en `deportivo`), reduciendo los accesos innecesarios a PostgreSQL.

## Consecuencias
*   **Positivas:**
    *   **Seguridad instantánea:** Permite la revocación inmediata de accesos (Logout efectivo), solucionando la mayor debilidad de los JWT puros sin comprometer significativamente el rendimiento.
    *   **Reducción de carga en la Base de Datos:** Libera de peticiones repetitivas a la instancia de PostgreSQL, mejorando el rendimiento global de la API.
*   **Negativas / Desafíos:**
    *   **Mayor infraestructura:** Añade un nuevo componente a la arquitectura (Servidor Redis), aumentando la complejidad de la configuración del entorno local y del despliegue en producción.
    *   **Invalidación de caché:** Requiere un diseño meticuloso de las políticas de expiración (TTL) y desalojo de datos para evitar que los usuarios visualicen información desactualizada.