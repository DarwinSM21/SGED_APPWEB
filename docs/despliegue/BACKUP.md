# Estrategia de respaldo — SGED (Bloque A.4.2, Entrega Final)

## Por qué esto no es opcional

El sistema trata datos personales de menores de edad (`docs/etica/ETHICS.md`):
nombre, cédula, fecha de nacimiento, asistencia con hora, evaluaciones de
desempeño. Perder esos datos sin respaldo no es solo un incidente técnico.

## Frecuencia y destino

- **Frecuencia:** respaldo diario de la base de datos completa (`pg_dump`),
  automatizado.
- **Destino:** todavía no está fijado un destino de almacenamiento externo
  al proveedor de base de datos — depende de la decisión de proveedor que
  se registra en `DEPLOYMENT.md` (pendiente). En cuanto se elija:
  - Si la base de datos es Supabase (ver `.env.example`, opción activa
    hoy): Supabase incluye respaldos administrados según el plan
    contratado — **verificar explícitamente qué retención da el plan
    vigente antes de asumir que basta**, porque el nivel gratuito
    históricamente no incluye retención de 30 días. Se recomienda además
    un `pg_dump` propio independiente del proveedor, para no depender
    únicamente de una política de terceros que puede cambiar.
  - El destino del `pg_dump` propio no debe ser el repositorio público
    (los dumps contienen datos personales, aunque sean de prueba) — un
    almacenamiento privado (bucket privado, Drive institucional con acceso
    restringido) es el patrón que ya usa el proyecto para los
    consentimientos SUS firmados (`docs/etica/ETHICS.md`, sección de
    consentimientos).
- **Comando de referencia** (ejecutar contra `DB_URL` del entorno,
  ajustando si el proveedor final no es Supabase):
  ```bash
  pg_dump "$DB_URL" -F c -f "respaldo_$(date +%F).dump"
  ```

## Retención

Mínimo exigido por la guía: **un respaldo diario durante los 30 días
posteriores a la fecha de la defensa oral** (semana 17). Fuera de esa
ventana obligatoria, se recomienda una retención más liviana (por ejemplo
7 diarios + 4 semanales) para no acumular indefinidamente datos de
menores en múltiples copias sin necesidad.

## Procedimiento de restauración

1. `pg_restore --clean --if-exists -d "$DB_URL" respaldo_YYYY-MM-DD.dump`
   (o `psql "$DB_URL" < archivo.sql` si el dump es texto plano).
2. Verificar `/actuator/health` en estado `UP`.
3. Verificar un flujo de lectura real (login con el usuario demo, listado
   de estudiantes, un procedimiento almacenado como
   `sp_contar_estudiantes_activos`) — un `pg_restore` sin errores no
   garantiza por sí solo que el esquema y los datos quedaron coherentes
   con lo que la aplicación espera.

## Prueba periódica de restauración

**Todavía no hay evidencia archivada de una restauración de prueba
ejecutada.** Esto es un pendiente real, no un procedimiento ya cumplido —
se declara así en vez de darlo por hecho. Antes de la Entrega Final:

1. Ejecutar una restauración completa contra una base de datos separada
   (nunca sobre la de producción).
2. Cronometrar cuánto tarda — determina el objetivo de tiempo de
   recuperación (RTO) real, no uno estimado de memoria.
3. Archivar la evidencia (log de la restauración, capturas, tiempo
   medido) en `docs/mediciones/` o en este mismo archivo, con fecha.
