# Despliegue en Fly.io

Guía para publicar SGED en internet. Los ficheros de configuración ya están en
el repositorio y **probados localmente**; lo que queda es ejecutar los comandos
con tu propia cuenta.

## Por qué vale la pena

El despliegue elimina el hallazgo **H-05** de `docs/etica/ETHICS.md`: el
certificado autofirmado. Fly termina TLS con un certificado real de Let's
Encrypt, así que la aplicación pasa de "no apta para producción con datos de
menores" a servir sobre HTTPS válido.

Lo que **no** resuelve: el envío real de notificaciones (RF-22) sigue sin
construirse, y las migraciones de Flyway siguen sin poder reconstruir la base
desde cero (ver la advertencia del paso 1).

## Arquitectura del despliegue

```
navegador ──HTTPS──> Fly proxy ──HTTP──> sged-frontend (nginx :8080)
                                              │
                                    red privada .internal
                                              ▼
                                         sged-backend (Spring Boot :8080)
                                              │
                                    ┌─────────┴─────────┐
                                    ▼                   ▼
                              Supabase            Upstash
                              (PostgreSQL)        (Redis)
```

El backend **no** se publica: su única puerta es el proxy `/api/` del
frontend. Eso no es solo por seguridad — las cookies de sesión son
`SameSite=Strict`, y solo funcionan si el navegador ve un único origen.

## Lo que necesitas antes de empezar

Tres cuentas, que debes crear tú (no las puede crear nadie por ti):

- **Fly.io** — pide tarjeta incluso para uso gratuito. **Verifica el precio
  actual antes de desplegar**, porque el modelo de Fly cambió y esta guía no
  puede garantizarte que salga en cero.
- **Supabase** — PostgreSQL con plan gratuito real.
- **Upstash** — Redis con plan gratuito real.

Y el CLI:

```bash
curl -L https://fly.io/install.sh | sh
```

```bash
fly auth login
```

## Paso 1 — Base de datos en Supabase

Crea un proyecto y guarda la contraseña. Del panel toma la cadena de conexión
del **Transaction Pooler** (puerto 6543), no la conexión directa.

> **Importante.** El esquema se aplica a mano, una sola vez. Las migraciones
> de Flyway **no** pueden construir la base desde cero: ninguna crea
> `deportivo.categorias`, aunque V7, V16 y V17 la referencian, y
> `seguridad.estudiantes` (V2) nunca se mueve a `academico.estudiantes`. Por
> eso `FLYWAY_ENABLED=false`. La fuente de verdad del esquema es
> `db/schema.sql`.

```bash
psql "postgresql://postgres.TU_REF:TU_PASSWORD@aws-0-us-east-1.pooler.supabase.com:6543/postgres" -f db/schema.sql
```

```bash
psql "postgresql://postgres.TU_REF:TU_PASSWORD@aws-0-us-east-1.pooler.supabase.com:6543/postgres" -f db/seed.sql
```

Comprueba que quedaron las 33 tablas:

```bash
psql "TU_CADENA" -c "SELECT count(*) FROM information_schema.tables WHERE table_schema IN ('seguridad','academico','deportivo','inventario');"
```

## Paso 2 — Redis en Upstash

Crea una base Redis y anota el endpoint y la contraseña. Upstash **solo**
acepta TLS; por eso el backend ahora lee `REDIS_PASSWORD` y `REDIS_SSL`, que
antes no existían.

## Paso 3 — Backend

```bash
cd backend && fly launch --no-deploy --name sged-backend --region mia
```

Responde **no** cuando ofrezca crear Postgres o Redis: ya los tienes. El
`fly.toml` del repositorio tiene la configuración correcta; si el CLI ofrece
sobrescribirlo, di que no.

Los secretos nunca van al repositorio:

```bash
fly secrets set -a sged-backend DB_URL="jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?prepareThreshold=0&preparedStatementCacheQueries=0" DB_USER="postgres.TU_REF" DB_PASSWORD="TU_PASSWORD" REDIS_HOST="TU_ENDPOINT.upstash.io" REDIS_PASSWORD="TU_TOKEN" JWT_SECRET="$(openssl rand -base64 48)"
```

`prepareThreshold=0&preparedStatementCacheQueries=0` no es opcional: sin eso,
Hibernate 6 y el pooler fallan con "could not determine data type of
parameter".

Si vas a mostrar la IA del entrenador, agrega también su clave:

```bash
fly secrets set -a sged-backend GEMINI_API_KEY="TU_CLAVE" IA_HABILITADO="true"
```

```bash
fly deploy -a sged-backend
```

```bash
fly logs -a sged-backend
```

Si el arranque falla por `SchemaManagementException`, el esquema del paso 1 no
se aplicó completo: `ddl-auto: validate` compara contra las entidades y aborta
ante cualquier diferencia.

## Paso 4 — Frontend

```bash
cd ../frontend && fly launch --no-deploy --name sged-frontend --region mia
```

```bash
fly deploy -a sged-frontend
```

Confirma que el DNS privado que declara `fly.toml` es el correcto:

```bash
fly ssh console -a sged-frontend -C "cat /etc/resolv.conf"
```

Si el `nameserver` que aparece no es `fdaa::3`, corrige `DNS_RESOLVER` en
`frontend/fly.toml` y vuelve a desplegar.

## Paso 5 — Comprobar

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://sged-frontend.fly.dev/
```

```bash
curl -s -i -X POST https://sged-frontend.fly.dev/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"TU_PASSWORD_ADMIN"}' | grep -iE "^HTTP|set-cookie"
```

Debe responder `200` y emitir `sged_access` y `sged_refresh` con los atributos
`Secure` y `SameSite=Strict`. Si las cookies llegan **sin** `Secure`, el
backend no está viendo el esquema HTTPS: revisa que nginx reenvíe
`X-Forwarded-Proto`.

Y que el actuator no quede expuesto — debe devolver el HTML de la SPA, no
JSON:

```bash
curl -s https://sged-frontend.fly.dev/actuator/health | head -c 40
```

## Qué se probó localmente y qué no

**Probado** (con la imagen `Dockerfile.fly` real):

- nginx arranca aunque el backend no exista todavía. Con el nombre literal en
  `proxy_pass` no arrancaba —`host not found in upstream`—, lo que habría
  impedido reiniciar el frontend mientras el backend duerme por
  `auto_stop_machines`. Por eso el destino pasa por variable.
- La SPA se sirve y el fallback de rutas funciona (`/categorias` → 200).
- Con el backend caído, `/api/` devuelve 502 en lugar de tumbar el servidor.
- Login completo a través del proxy de producción: `200` con ambas cookies.
- `/actuator/health` devuelve la SPA, no el JSON del actuator.
- La imagen no contiene certificados autofirmados.

**No probado** (requiere las cuentas reales): la resolución `.internal` de
Fly, el valor de `DNS_RESOLVER`, y la conexión a Supabase y Upstash.

## Después del despliegue

Actualiza `docs/etica/ETHICS.md` para reflejar que H-05 dejó de aplicar en el
entorno publicado, indicando la URL. Y cambia la contraseña del usuario
`admin` sembrado: `db/seed.sql` trae una conocida, aceptable en local e
inaceptable en internet.
