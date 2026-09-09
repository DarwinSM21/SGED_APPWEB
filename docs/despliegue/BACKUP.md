# Estrategia de respaldo — SGED (Bloque A.4.2, Entrega Final)

## Por qué esto no es opcional

El sistema trata datos personales de menores de edad (`docs/etica/ETHICS.md`):
nombre, cédula, fecha de nacimiento, asistencia con hora, evaluaciones de
desempeño. Perder esos datos sin respaldo no es solo un incidente técnico.

## Frecuencia y destino

- **Frecuencia:** respaldo diario de la base de datos completa (`pg_dump -F c`),
  ejecutado por el equipo.
- **Base de datos:** PostgreSQL 17 gestionado en **Supabase** (no Render — ver
  `docs/despliegue/render.md`).
- **Respaldo gestionado por el proveedor:** el proyecto está en el **plan
  Free de Supabase**, que **no incluye respaldos diarios gestionados ni PITR**
  (son funciones de los planes de pago). Por eso el `pg_dump` propio de abajo
  no es un complemento sino la **única** copia de seguridad; su cadencia
  diaria y su retención son las que determinan el RPO.
- **`pg_dump` propio, independiente del proveedor:** un dump diario en formato
  custom (`pg_dump -F c`), para no depender solo de una política de terceros
  que puede cambiar. Se toma por el **Session Pooler de Supabase (puerto
  5432)**, restringido a los esquemas de la aplicación:
  ```bash
  pg_dump "$SUPA_DB" -F c --no-owner --no-privileges \
    -n seguridad -n academico -n deportivo -n inventario \
    -f "sged_prod_$(date +%F).dump"
  ```
- **Destino del `pg_dump` propio:** almacenamiento privado del equipo
  **fuera del repositorio** — carpeta compartida de acceso restringido
  (Drive institucional del equipo), el mismo patrón que ya usa el proyecto
  para los consentimientos SUS firmados. **Nunca** el repositorio público:
  los dumps contienen datos personales de menores aunque hoy sean de prueba.
  El archivo local de trabajo del equipo (`~/sged-backups/`) queda cubierto
  por `.gitignore` (`sged_prod_*.dump`).

## Retención

Mínimo exigido por la guía: **un respaldo diario durante los 30 días
posteriores a la fecha de la defensa oral** (semana 17). Fuera de esa
ventana obligatoria, se recomienda una retención más liviana (por ejemplo
7 diarios + 4 semanales) para no acumular indefinidamente datos de
menores en múltiples copias sin necesidad.

## Objetivos de recuperación (RPO / RTO)

- **RPO (Recovery Point Objective) ≤ 24 h.** El `pg_dump` propio se ejecuta a
  diario, así que la pérdida máxima ante un incidente que inutilice la base es
  de un día de operación. Si el PITR del plan de Supabase está disponible (ver
  abajo), el RPO efectivo baja al orden de minutos dentro de su ventana.
- **RTO (Recovery Time Objective) — medido, no estimado.** El `pg_restore` del
  respaldo actual tarda **≈ 1 s** sobre una base recién aprovisionada
  (medición del 2026-09-09, `docs/mediciones/backup/restauracion-2026-09-09.md`).
  La recuperación completa del servicio —aprovisionar una base nueva +
  restaurar + reapuntar el backend + verificar— está dominada por el
  aprovisionamiento y es del orden de **3–5 min**. Ambos valores deben
  volver a medirse cuando el volumen de datos crezca.

## Recuperación punto-en-el-tiempo (PITR) de Supabase

**El plan Free contratado no ofrece PITR** (Project Settings → Database →
Backups lo confirma: PITR es un complemento de pago sobre los planes Pro y
superiores, con ventanas de 7 días o más). Por tanto:

- No hay recuperación a un instante arbitrario: el punto de recuperación es
  siempre el del último `pg_dump` diario.
- El **RPO efectivo es ≤ 24 h**, dado por la cadencia del `pg_dump`.
- Si el proyecto migrara a un plan con PITR, esta sección debe actualizarse
  con la ventana de retención real y el RPO bajaría al orden de minutos.

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

**Evidencia archivada:**
[`docs/mediciones/backup/restauracion-2026-09-09.md`](../mediciones/backup/restauracion-2026-09-09.md)
— restauración completa del respaldo de producción del 2026-09-09 contra una
base separada (contenedor `postgres:17` efímero), sin errores, con el `RTO`
cronometrado (≈ 1 s el `pg_restore`) y verificación del esquema, los 12
procedimientos almacenados, los conteos de filas y un flujo de lectura de la
aplicación (login del `ADMINISTRADOR` + listado por `sp_contar_estudiantes_activos`).

La prueba se repite:

1. Contra una base de datos **separada** (nunca la de producción).
2. **Cronometrando** cada ejecución para mantener el RTO al día.
3. Archivando la evidencia con fecha en `docs/mediciones/backup/`.

Cadencia recomendada: en cada cambio de esquema (migración nueva) y, como
mínimo, una vez antes de cada entrega o defensa.
