# ADR-003: Motor de Base de Datos y Aislamiento de Datos

## Estado
Aprobado — con dos precisiones (2026-07-30):

1. **Hospedaje real.** El entorno reproducible de esta entrega (`make up`,
   `docker-compose.yml`) usa **PostgreSQL 16 auto-hospedado** en contenedor,
   con la imagen fijada por digest SHA-256. Supabase es una opción de
   conexión alternativa ya soportada en `.env.example` (`DB_URL` apuntando a
   `*.pooler.supabase.com`), pero no es el entorno que se evalúa ni el que
   verifican los scripts de `make bench`/`make audit`.
2. **Esquemas realmente migrados.** De los 5 esquemas descritos abajo, hoy
   existen 4 en `db/schema.sql`: `seguridad`, `academico`, `deportivo`,
   `inventario` (migrado en `V15__inventario.sql`, 2026-08-12 — RF-27 a
   RF-30 en `docs/requisitos/SRS.md`). `auditoria` sigue siendo diseño a
   futuro, sin migración Flyway todavía.

## Contexto
El Sistema de Gestión para Escuela Deportiva (SGED) maneja información con una alta integridad relacional (relaciones complejas entre estudiantes, representantes, asistencias, cobros, inventario y entrenamientos). Se requiere un motor de base de datos robusto, con soporte maduro para transacciones ACID, y que permita organizar de manera limpia los diferentes dominios del sistema para evitar que el crecimiento de la aplicación genere un esquema masivo y difícil de mantener.

## Decisión
Utilizar **PostgreSQL** como motor de base de datos relacional principal, alojado en la plataforma de nube **Supabase**. Para garantizar el orden, modularidad y seguridad a nivel de datos, el almacenamiento se estructurará de forma lógica utilizando **Schemas organizacionales** bien definidos, segmentando el sistema en los siguientes 5 bloques:

1.  `seguridad`: Tablas de usuarios, roles, permisos y auditoría de accesos.
2.  `academico`: Información de estudiantes, representantes e inscripciones.
3.  `deportivo`: Gestión de entrenadores, equipos, categorías, entrenamientos, asistencias y partidos.
4.  `inventario`: Control de uniformes, balones, implementos deportivos y asignaciones.
5.  `auditoria`: Historial de modificaciones y logs de transacciones críticas del sistema.

Se prohíbe el uso de claves foráneas huérfanas o la ausencia de restricciones referenciales integras. Cada tabla deberá contar con restricciones consistentes a nivel de base de datos.

## Consecuencias
*   **Positivas:**
    *   **Estructura modular clara:** El uso de múltiples esquemas evita la colisión de nombres de tablas y permite delegar la lógica de base de datos de manera organizada según el dominio de negocio.
    *   **Garantía ACID:** Integridad de datos absoluta para módulos sensibles como cobros o registros de asistencia.
    *   **Seguridad por esquema:** Permite configurar permisos de usuario de base de datos específicos por esquema si fuera necesario en el futuro.
*   **Negativas / Desafíos:**
    *   **Complejidad en las consultas:** Al realizar cruces de información (*JOINs*) entre diferentes esquemas (ej. unir un usuario de `seguridad` con un estudiante de `academico`), se debe especificar explícitamente el prefijo del esquema (`seguridad.usuario JOIN academico.estudiante`), incrementando la complejidad de los mapeos y queries SQL manuales.