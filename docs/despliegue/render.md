# Despliegue en Render

Despliegue público con HTTPS real, en **una sola cuenta** y sin tarjeta de
crédito. La configuración está en `render.yaml` (raíz del repositorio), así
que Render crea los cuatro recursos de una vez en lugar de depender de
ajustes hechos a mano.

## Qué resuelve

Elimina el hallazgo **H-05** de `docs/etica/ETHICS.md`: Render sirve HTTPS con
certificado real, así que el sistema deja de depender del autofirmado. A
diferencia del túnel de Cloudflare (`cloudflare-tunnel.md`), la URL no depende
de que tu computadora esté encendida.

## ⚠️ La base de datos caduca en 30 días

El plan gratuito de Render Postgres **se elimina 30 días después de crearse**,
y con él los datos. Está asumido a propósito para esta entrega.

**Anota la fecha de creación.** Si el sistema debe seguir en pie después,
las salidas son: pasar la base a un plan de pago (desde $6/mes) o moverla a un
proveedor cuyo plan gratuito no expire, como Supabase — en ese caso solo
cambian `DB_URL`, `DB_USER` y `DB_PASSWORD`, nada más.

## Arquitectura

```
navegador ──HTTPS──> sged-frontend (sitio estático, CDN)
                          │
                          │  regla de reescritura /api/*
                          ▼
                     sged-backend (contenedor Docker)
                          │
                ┌─────────┴─────────┐
                ▼                   ▼
          sged-postgres        sged-redis
```

**El frontend es un sitio estático, no un contenedor.** Render da 750 horas de
instancia gratis al mes para todo el workspace; un nginx en contenedor las
consumiría solo para servir ficheros y reenviar `/api`. Como sitio estático no
consume horas, y las reglas de reescritura hacen ese trabajo.

**La reescritura conserva el mismo origen.** No es redirección: el navegador
sigue viendo el dominio del frontend, así que las cookies `SameSite=Strict`
siguen siendo válidas. Con una redirección se rompería la sesión.

## Paso 1 — Crear el Blueprint

En `render.com`: **New → Blueprint**, conecta el repositorio de GitHub. Render
detecta `render.yaml` y muestra los cuatro recursos que va a crear.

## Paso 2 — Completar las tres variables de la base

Render pedirá los valores marcados como `sync: false`. Están en el panel de
**sged-postgres**, pestaña *Info*:

| Variable | De dónde sale |
|---|---|
| `DB_URL` | `jdbc:postgresql://<Internal-Host>/sged_db` |
| `DB_USER` | El campo *Username* |
| `DB_PASSWORD` | El campo *Password* |

Usa el host **interno** (`dpg-…-a`), no el externo: el backend corre dentro de
Render y así la conexión no sale a internet.

Render entrega su cadena en formato `postgres://…`, que Spring **no** acepta
como `spring.datasource.url` porque le falta el prefijo `jdbc:`. Por eso
`DB_URL` se arma a mano en lugar de tomarse tal cual.

> Si más adelante activas *connection pooling* (pgbouncer) en la base, añade
> `?prepareThreshold=0&preparedStatementCacheQueries=0` al final de `DB_URL`.
> Sin eso, Hibernate 6 contra un pooler falla con *"could not determine data
> type of parameter"*. Sin pooling no hace falta.

`JWT_SECRET` lo genera Render solo. `GEMINI_API_KEY` solo si vas a mostrar la
retroalimentación por IA.

## Paso 3 — Cargar el esquema

Las migraciones de Flyway **no** reconstruyen la base desde cero: ninguna crea
`deportivo.categorias` aunque V7, V16 y V17 la referencian. La fuente de
verdad es `db/schema.sql`, y se aplica una sola vez.

Copia la **External Database URL** del panel y expórtala como variable, para
que la contraseña no quede en el historial de la terminal:

```bash
export RENDER_DB='postgresql://USUARIO:PASSWORD@HOST-EXTERNO.oregon-postgres.render.com/sged_db'
```

```bash
psql "$RENDER_DB" -f db/schema.sql
```

```bash
psql "$RENDER_DB" -f db/seed.sql
```

Comprueba que quedó:

```bash
psql "$RENDER_DB" -c "SELECT count(*) FROM information_schema.tables WHERE table_schema IN ('seguridad','academico','deportivo','inventario');"
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
- **La base caduca a los 30 días** (ver arriba).

Para una demo evaluada: entra a la URL unos minutos antes para que el backend
ya esté despierto. Que responda lento en el momento de la revisión cuenta como
riesgo real, no como detalle.

## Antes de compartir el enlace

`db/seed.sql` trae una contraseña conocida para `admin`. En una URL pública
hay que cambiarla.
