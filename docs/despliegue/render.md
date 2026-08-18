# Despliegue en Render

Despliegue público y permanente, independiente de cualquier equipo local. La
configuración está en `render.yaml` (raíz del repositorio), así que Render
crea los tres servicios de una sola vez en lugar de depender de ajustes
hechos a mano en el panel.

## Qué resuelve

Elimina el hallazgo **H-05** de `docs/etica/ETHICS.md`: Render sirve HTTPS con
certificado real, así que el sistema deja de depender del certificado
autofirmado. A diferencia del túnel de Cloudflare (ver `cloudflare-tunnel.md`),
la URL no depende de que tu computadora esté encendida.

## Arquitectura y por qué

```
navegador ──HTTPS──> sged-frontend (sitio estático, CDN)
                          │
                          │  regla de reescritura /api/*
                          ▼
                     sged-backend (contenedor Docker)
                          │
                ┌─────────┴─────────┐
                ▼                   ▼
           Supabase            sged-redis
          (PostgreSQL)      (Render Key Value)
```

**El frontend es un sitio estático, no un contenedor.** Render da 750 horas de
instancia gratuitas al mes para todo el workspace; un contenedor de nginx las
consumiría solo para servir ficheros y reenviar `/api`. Como sitio estático no
consume horas y las reglas de reescritura hacen ese trabajo, dejando la bolsa
completa para el backend.

**La reescritura conserva el mismo origen.** No es una redirección: el
navegador sigue viendo el dominio del frontend, así que las cookies
`SameSite=Strict` siguen siendo válidas. Si fuera redirección, la sesión se
rompería.

**La base va en Supabase, no en Render.** El Postgres gratuito de Render
**caduca a los 30 días de creado**. El plan gratuito de Supabase no expira.

## Antes de empezar

Necesitas dos cuentas, que debes crear tú:

- **Render** (`render.com`) — plan *Hobby*, $0/mes.
- **Supabase** (`supabase.com`) — para PostgreSQL.

## Paso 1 — Base de datos en Supabase

Crea un proyecto y guarda la contraseña. Copia la cadena de conexión del
**Transaction Pooler** (puerto 6543), no la conexión directa.

Aplica el esquema una sola vez. Conviene exportar la cadena como variable en
lugar de escribirla en el comando, para que no quede en el historial:

```bash
export SUPABASE_URL='postgresql://postgres.TU_REF:TU_PASSWORD@aws-0-us-east-1.pooler.supabase.com:6543/postgres'
```

```bash
psql "$SUPABASE_URL" -f db/schema.sql
```

```bash
psql "$SUPABASE_URL" -f db/seed.sql
```

> Las migraciones de Flyway **no** reconstruyen la base desde cero: ninguna
> crea `deportivo.categorias` aunque V7, V16 y V17 la referencian. Por eso
> `FLYWAY_ENABLED=false` y la fuente de verdad del esquema es
> `db/schema.sql`.

Comprueba que quedó:

```bash
psql "$SUPABASE_URL" -c "SELECT count(*) FROM information_schema.tables WHERE table_schema IN ('seguridad','academico','deportivo','inventario');"
```

## Paso 2 — Crear el Blueprint en Render

En el panel: **New → Blueprint**, conecta el repositorio de GitHub y Render
detectará `render.yaml`.

Te pedirá los valores marcados como `sync: false`, que nunca se escriben en el
repositorio:

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?prepareThreshold=0&preparedStatementCacheQueries=0` |
| `DB_USER` | `postgres.TU_REF` |
| `DB_PASSWORD` | La contraseña de Supabase |
| `GEMINI_API_KEY` | Opcional, solo si vas a mostrar la IA |

`prepareThreshold=0&preparedStatementCacheQueries=0` **no es opcional**: sin
eso, Hibernate 6 contra el pooler falla con *"could not determine data type of
parameter"*.

`JWT_SECRET` lo genera Render solo.

## Paso 3 — Ajustar la URL del backend

`render.yaml` asume que el backend queda en
`https://sged-backend.onrender.com`. Si Render le asigna otro nombre —porque
ya exista uno igual—, corrige el `destination` de la regla `/api/*` y vuelve a
sincronizar el Blueprint. **Si no coincide, el frontend carga pero ninguna
llamada a la API funciona.**

## Paso 4 — Verificar, en este orden

Primero que el backend viva:

```bash
curl -s https://sged-backend.onrender.com/actuator/health
```

Debe responder `{"status":"UP",...}`. La primera petición puede tardar
bastante: el plan gratuito tiene 0.1 CPU y Spring Boot arranca lento.

Después **el flujo de cookies, que es lo más frágil de este diseño**:

```bash
curl -s -D - -o /dev/null -X POST https://sged-frontend.onrender.com/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"TU_PASSWORD"}' | grep -i "set-cookie"
```

Tienen que aparecer `sged_access` y `sged_refresh` con `Secure; HttpOnly;
SameSite=Strict`.

> **Si las cookies no llegan**, la reescritura del sitio estático no está
> propagando el encabezado `Set-Cookie`. No es un problema de la aplicación.
> La solución es servir el frontend como servicio Docker en vez de sitio
> estático, reutilizando `frontend/Dockerfile.fly` y
> `frontend/nginx.fly.conf.template` (que ya hacen exactamente eso y están
> probados), a costa de consumir horas de instancia. Esta ruta es la primera
> que hay que comprobar tras desplegar.

## Limitaciones del plan gratuito

- **Se apaga tras 15 minutos sin tráfico** y despierta con la siguiente
  petición. Con 0.1 CPU, ese arranque en frío es lento.
- **750 horas de instancia al mes** para todo el workspace. El sitio estático
  no consume; solo el backend.
- **512 MB de RAM y 0.1 CPU** para el backend.

Para una demo evaluada: entra a la URL unos minutos antes para que el backend
ya esté despierto. Que la URL responda lento en el momento de la revisión
cuenta como riesgo real, no como detalle.

## Antes de compartir el enlace

`db/seed.sql` trae una contraseña conocida para `admin`. En una URL pública
hay que cambiarla.
