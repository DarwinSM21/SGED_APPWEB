# ADR-003: Uso de Redis para Blacklist de Tokens JWT

## Estado
Aceptado

## Contexto
JWT es stateless: el servidor no almacena el token. Si un usuario cierra sesión 
o el token es comprometido, el servidor no tiene forma de invalidarlo sin una 
lista de revocación.

## Decisión
Se utiliza Redis para almacenar los JTI (JWT ID) de tokens revocados con un TTL 
igual al tiempo de expiración del token original. En cada solicitud, el filtro 
JWT consulta Redis antes de autorizar el acceso.

## Consecuencias

**Positivas:**
- Logout real e inmediato sin esperar la expiración del token
- TTL automático en Redis evita acumulación de entradas obsoletas
- Bajo latencia en la consulta de blacklist (operación O(1))
- Cumple con OWASP A07 - Fallas de autenticación

**Negativas:**
- Introduce dependencia de infraestructura adicional (Redis)
- Si Redis no está disponible, el sistema no puede validar tokens revocados

## Alternativas consideradas
- **Sin blacklist**: No permite logout real, inseguro
- **Blacklist en PostgreSQL**: Mayor latencia, requiere limpieza periódica
- **Sesiones stateful**: Contradice la arquitectura REST stateless

## Referencias
- OWASP Top 10 2021 - A07 Identification and Authentication Failures
- RFC 7519 - JSON Web Token (JWT)