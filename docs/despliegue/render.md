# Despliegue en Render

Despliegue público con HTTPS real y sin tarjeta de crédito. La configuración
está en `render.yaml` (raíz del repositorio), así que Render crea los recursos
de una vez en lugar de depender de ajustes hechos a mano. La base de datos
**no** vive en Render: es PostgreSQL gestionado en Supabase (ver más abajo).

## Qué resuelve

Elimina el hallazgo **H-05** de `docs/etica/ETHICS.md`: Render sirve HTTPS con
certificado real, así que el sistema deja de depender del autofirmado. A
diferencia del túnel de Cloudflare (`cloudflare-tunnel.md`), la URL no depende
de que tu computadora esté encendida.

## Base de datos: Supabase, no Render

El plan gratuito de Render Postgres se elimina 30 días después de crearse, y
con él los datos. Por eso la base va en **Supabase**, cuyo plan gratuito no
expira así. La cadena por defecto del repo (`.env.example`) ya apunta a
Supabase; `render.yaml` no crea ninguna base y deja `DB_URL` / `DB_USER` /
`DB_PASSWORD` como variables a completar en el panel (paso 2).

Supabase da dos puertos sobre el mismo host: **6543** (Transaction Pooler, el
que usa la app en marcha) y **5432** (Session Pooler / conexión directa, el
que hace falta para cargar el esquema con `psql`, porque el 6543 no sostiene
los advisory locks).

## Arquitectura

```
navegador ──HTTPS──> sged-frontend (sitio estático, CDN)
                          │
                          │  regla de reescritura /api/*
                          ▼
                     sged-backend (contenedor Docker) ──> Supabase (PostgreSQL)
                          │
                          ▼
                     sged-redis (Key Value de Render)
```

**El frontend es un sitio estático, no un contenedor.** Render da 750 horas de
instancia gratis al mes para todo el workspace; un nginx en contenedor las
consumiría solo para servir ficheros y reenviar `/api`. Como sitio estático no
consume horas, y las reglas de reescritura hacen ese trabajo.

**La reescritura conserva el mismo origen.** No es redirección: el navegador
sigue viendo el dominio del frontend, así que las cookies `SameSite=Strict`
siguen siendo válidas. Con una redirección se rompería la sesión.

## Paso 1 — Crear el proyecto de Supabase

En `supabase.com`: **New project**. Anota la contraseña de la base que te pide
(es `DB_PASSWORD`). Cuando termine de aprovisionar, en **Project Settings →
Database → Connection string** verás el host
(`aws-1-<región>.pooler.supabase.com`) y el usuario
(`postgres.<id-del-proyecto>`).

## Paso 2 — Crear el Blueprint en Render

En `render.com`: **New → Blueprint**, conecta el repositorio de GitHub. Render
detecta `render.yaml` y muestra los recursos que va a crear (backend, Key
Value y frontend). Pedirá los valores marcados `sync: false`:

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>.pooler.supabase.com:6543/postgres?prepareThreshold=0&preparedStatementCacheQueries=0` |
| `DB_USER` | `postgres.<id-del-proyecto>` |
| `DB_PASSWORD` | La contraseña del proyecto de Supabase |

El puerto **6543** es el Transaction Pooler. El `?prepareThreshold=0&preparedStatementCacheQueries=0`
es obligatorio: sin eso Hibernate 6 contra el pooler falla con *"could not
determine data type of parameter"*. El prefijo `jdbc:` también: Supabase da la
cadena como `postgresql://…` y Spring no la acepta así.

`JWT_SECRET` lo genera Render solo. `GEMINI_API_KEY` solo si vas a mostrar la
retroalimentación por IA.

## Paso 3 — Cargar el esquema en Supabase

Las migraciones de Flyway **no** reconstruyen la base desde cero: ninguna crea
`deportivo.categorias` aunque V7, V16 y V17 la referencian. La fuente de
verdad es `db/schema.sql`, y se aplica una sola vez — por el puerto **5432**
(el 6543 no sostiene los advisory locks que necesita un script grande).

Exporta la cadena como variable para que la contraseña no quede en el
historial de la terminal (fíjate en el `:5432`):

```bash
export SUPA_DB='postgresql://postgres.<id-del-proyecto>:PASSWORD@aws-1-<región>.pooler.supabase.com:5432/postgres'
```

```bash
psql "$SUPA_DB" -f db/schema.sql
```

```bash
psql "$SUPA_DB" -f db/seed.sql
```

Comprueba que quedó:

```bash
psql "$SUPA_DB" -c "SELECT count(*) FROM information_schema.tables WHERE table_schema IN ('seguridad','academico','deportivo','inventario');"
```

## Paso 3b — Migraciones incrementales posteriores a `schema.sql`

`db/schema.sql` es una **foto** del esquema hasta cierto punto de la historia. Las
migraciones de Flyway que vinieron después **no están en esa foto** y, como
`render.yaml` deja `FLYWAY_ENABLED=false`, **nadie las aplica en el deploy**: hay
que correrlas a mano, una vez, en orden, por el puerto **5432** (mismo `$SUPA_DB`
del paso 3).

> Si no se aplican, el backend arranca igual —`ddl-auto: validate` solo valida
> tablas de entidades, no procedimientos ni constraints— pero las funciones que
> dependen de ellas fallan en caliente. Ejemplo: sin `V27`,
> `POST /api/estudiantes/{id}/anonimizar` (RF-50) devuelve `500` con
> *"procedure academico.sp_anonimizar_estudiante does not exist"*.

**Regla:** toda migración `V2x`/`V3x` cuyo efecto no esté ya en `schema.sql` se
aplica con `psql "$SUPA_DB" -f <ruta>` en orden ascendente. `V23`
(`roles_de_base_de_datos`) se **omite** en Supabase (gestiona sus propios roles).

Pendientes a fecha 2026-09-10 (esquema base + estas cuatro = lo que espera el
código en `main`):

```bash
# 1. Pre-check ANTES de V26: el índice único parcial de cédula falla si ya hay
#    duplicados. Si esto devuelve filas, hay que limpiarlas primero.
psql "$SUPA_DB" -c "SELECT cedula, COUNT(*) FROM seguridad.personas WHERE cedula IS NOT NULL GROUP BY cedula HAVING COUNT(*) > 1;"

# 2. Aplicar en orden
psql "$SUPA_DB" -f backend/src/main/resources/db/migration/V25__limite_texto_libre_menores.sql
psql "$SUPA_DB" -f backend/src/main/resources/db/migration/V26__cedula_opcional_y_unica.sql
psql "$SUPA_DB" -f backend/src/main/resources/db/migration/V27__sp_anonimizar_estudiante.sql
psql "$SUPA_DB" -f backend/src/main/resources/db/migration/V28__correo_verificado.sql
```

Idempotencia: `V26`, `V27` y `V28` se pueden re-ejecutar sin daño (`IF [NOT]
EXISTS`, `CREATE OR REPLACE`; `V28` marca como verificadas las filas ya
verificadas, que es un no-op en la segunda pasada). **`V25` no**: su
`ALTER TABLE ... ADD CONSTRAINT` aborta si la constraint ya existe — si hay
que reintentar, quita antes las tres `ck_*_longitud` o salta `V25`.

`V28` (RNF-26 / H-09) **es requisito de arranque**: la entidad `Person` mapea
`correo_verificado` como `nullable = false`, así que con `ddl-auto: validate`
el backend **no levanta** si la columna no existe. Aplicar `V28` **antes** de
sincronizar el Blueprint con el commit que la introduce.

Verifica que quedaron:

```bash
psql "$SUPA_DB" -c "
  SELECT 'V25 ck_lesion_descripcion_longitud' AS objeto, count(*) FROM pg_constraint  WHERE conname='ck_lesion_descripcion_longitud'
  UNION ALL SELECT 'V26 ux_personas_cedula_no_nula',        count(*) FROM pg_indexes  WHERE indexname='ux_personas_cedula_no_nula'
  UNION ALL SELECT 'V27 sp_anonimizar_estudiante',          count(*) FROM pg_proc     WHERE proname='sp_anonimizar_estudiante';"
```

Las tres filas deben dar `1`. Después, reinicia el backend en Render (**Manual
Deploy → Clear build cache & deploy** no hace falta; con **Restart service**
basta) para que tome el esquema nuevo, y prueba el endpoint afectado.

> **Mejora pendiente (no bloqueante):** fijar `spring.flyway.baselineVersion` en
> la última versión ya contenida en `schema.sql` y volver a poner
> `FLYWAY_ENABLED=true`. Como las migraciones `V25+` sí son consistentes contra
> una base creada por `schema.sql`, Flyway podría aplicarlas y llevar el
> historial. Requiere probar el arranque una vez antes de confiarlo al deploy.

## Paso 4 — Ajustar las URLs si Render puso sufijo

Los nombres `sged-backend` / `sged-frontend` son globales en `.onrender.com`.
Si ya están tomados (otro despliegue del equipo), Render agrega un sufijo
aleatorio distinto por servicio. En el despliegue actual quedaron:

- backend  → `https://sged-backend-2p05.onrender.com`
- frontend → `https://sged-frontend-r2rs.onrender.com`

`render.yaml` ya apunta a esas dos (regla `/api/*` del frontend y
`CORS_ALLOWED_ORIGIN_PATTERNS` del backend). **Si se re-crea el blueprint
desde cero, Render asigna otros sufijos y hay que actualizar esas dos líneas
y volver a sincronizar** — si no coinciden, el frontend carga en blanco
porque las llamadas a la API se cuelgan contra el nombre viejo.

## Paso 5 — Verificar, en este orden

Primero que el backend viva:

```bash
curl -s https://sged-backend-2p05.onrender.com/actuator/health
```

Debe responder `{"status":"UP",...}`. La primera petición puede tardar
bastante: 0.1 CPU y Spring Boot arranca lento.

Después **el flujo de cookies, que es lo más frágil de este diseño**:

```bash
curl -s -D - -o /dev/null -X POST https://sged-frontend-r2rs.onrender.com/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"TU_PASSWORD"}' | grep -i "set-cookie"
```

Deben aparecer `sged_access` y `sged_refresh` con `Secure; HttpOnly;
SameSite=Strict`.

> **Si las cookies no llegan**, la reescritura del sitio estático no está
> propagando el encabezado `Set-Cookie`. No es un fallo de la aplicación. La
> salida es servir el frontend como servicio Docker en vez de sitio estático,
> reutilizando `frontend/Dockerfile.fly` y `frontend/nginx.fly.conf.template`
> —que ya hacen exactamente eso y están probados—, a costa de consumir horas
> de instancia. Es lo PRIMERO que hay que comprobar tras desplegar.

## Limitaciones del plan gratuito

- **Se apaga tras 15 minutos sin tráfico** y despierta con la siguiente
  petición. Con 0.1 CPU ese arranque en frío es lento.
- **750 horas de instancia al mes** para todo el workspace. El sitio estático
  no consume; solo el backend.
- **512 MB de RAM y 0.1 CPU** para el backend.
- **El Key Value (Redis) gratuito no persiste a disco**: al reiniciarse pierde
  la caché de lecturas y la lista de revocación de tokens. No afecta la
  corrección, solo obliga a volver a iniciar sesión.

La base en Supabase no entra en estos límites (su plan gratuito no expira a
los 30 días como el de Render Postgres), pero pausa el proyecto tras una
semana sin actividad — se reactiva desde el panel.

Para una demo evaluada: entra a la URL unos minutos antes para que el backend
ya esté despierto. Que responda lento en el momento de la revisión cuenta como
riesgo real, no como detalle.

## Antes de compartir el enlace

`db/seed.sql` trae una contraseña conocida para `admin`. En una URL pública
hay que cambiarla.
