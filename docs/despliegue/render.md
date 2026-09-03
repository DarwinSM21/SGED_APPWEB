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

## Paso 4 — Ajustar la URL del backend

`render.yaml` asume que el backend queda en
`https://sged-backend.onrender.com`. Si Render le asigna otro nombre —porque
ya exista uno igual—, corrige el `destination` de la regla `/api/*` y vuelve a
sincronizar. **Si no coincide, el frontend carga pero ninguna llamada a la API
funciona.**

## Paso 5 — Verificar, en este orden

Primero que el backend viva:

```bash
curl -s https://sged-backend.onrender.com/actuator/health
```

Debe responder `{"status":"UP",...}`. La primera petición puede tardar
bastante: 0.1 CPU y Spring Boot arranca lento.

Después **el flujo de cookies, que es lo más frágil de este diseño**:

```bash
curl -s -D - -o /dev/null -X POST https://sged-frontend.onrender.com/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"TU_PASSWORD"}' | grep -i "set-cookie"
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
