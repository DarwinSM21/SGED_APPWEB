# Despliegue en producción — SGED (Bloque A.4, Entrega Final)

**Estado: en definición.** Este documento describe la topología ya
decidida y deja explícito qué falta cerrar. No se declara nada como
"hecho" que todavía no esté verificado en un ambiente público real — esa
es la misma regla que ya sigue `VERSIONING.md` para las etiquetas.

## 1. Topología

El sistema local (`docker-compose.yml`: postgres + redis + backend +
frontend, los cuatro autocontenidos) se separa en producción en tres
piezas independientes, bajo el punto (v) "combinación equivalente" del
Bloque A.4.1:

| Componente | Local (`make up`) | Producción |
|---|---|---|
| Base de datos | Contenedor `postgres` propio | **Supabase** (Postgres gestionado — ya configurado, ver `.env.example`, sección "Opción 2") |
| Caché / blacklist JWT | Contenedor `redis` propio | **Upstash Redis** (capa gratuita, protocolo Redis estándar — no depende del proveedor de cómputo elegido) |
| Backend (Spring Boot) | Contenedor `backend` propio | **Fly.io**, app separada (`backend/fly.toml`), despliega directo desde `backend/Dockerfile` — sin exponer necesariamente al tribunal, se llega a través del proxy del frontend |
| Frontend (Angular + nginx) | Contenedor `frontend` propio | **Fly.io**, app separada (`frontend/fly.toml` + `frontend/Dockerfile.fly`) — es la única URL que el navegador del tribunal toca directo |

**Por qué esta separación no rompe la reproducibilidad (Bloque D.1):** el
camino de `make all` / `make up` sigue siendo 100% autocontenido con la
`Opción 1` de `.env.example` (Postgres y Redis en contenedores propios).
Un evaluador que clone el repo y siga el README no necesita ninguna
cuenta externa para reproducir los resultados — Supabase/Upstash son
solo la infraestructura del despliegue público, no un requisito para
reproducir localmente.

## 2. Pendiente de ejecutar (decidido, falta hacerlo)

- [x] **Proveedor de cómputo:** Fly.io, confirmado. `backend/fly.toml` y
  `frontend/fly.toml` ya están en el repo.
- [ ] Crear cuenta en Fly.io (`fly auth login`, requiere `flyctl`
  instalado) y en Upstash (Redis).
- [ ] `fly launch --no-deploy --copy-config` en `backend/` y en
  `frontend/` — si los nombres de app (`sged-profutbol-api`,
  `sged-profutbol-web`) ya están tomados, Fly pide uno distinto; si eso
  pasa, actualizar también `frontend/nginx.fly.conf` (el `proxy_pass`
  apunta al nombre del backend por dominio completo).
- [ ] **Dominio / subdominio** para la URL pública declarada en el README
  y en la portada del PDF — el subdominio `*.fly.dev` que asigna Fly por
  defecto ya cumple el requisito (HTTPS válido, sin advertencias); un
  dominio propio es opcional, no bloqueante.

## 3. Variables de entorno que cambian respecto al `.env` local

Copiar `.env.example` como base, pero estas variables **deben** cambiar
para producción — dejarlas en su valor de desarrollo es exactamente el
tipo de defecto que este proyecto ya encontró y corrigió una vez
(hallazgo H-08, `docs/etica/ETHICS.md`):

| Variable | Valor en `.env.example` (dev) | Valor requerido en producción |
|---|---|---|
| `COOKIE_SECURE` | `false` | `true` — sin esto, la cookie del JWT viaja sin el flag `Secure` y el ADR-008 (JWT en cookie `HttpOnly`) queda incompleto en la práctica aunque el código esté bien. |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:4200,...` | El dominio público real, por `https://` — los `localhost` no sirven ni deben quedar habilitados en producción. |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres local o Supabase de desarrollo | Credenciales del proyecto de Supabase de **producción** (recomendado: un proyecto Supabase separado del de desarrollo, no el mismo con datos de prueba mezclados con datos reales del tribunal). |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Host y puerto que entregue Upstash (normalmente requiere también TLS — revisar si el cliente Redis de Spring necesita `rediss://` en vez de `redis://`). |
| `JWT_SECRET` | Valor de ejemplo, público en el repo | Uno nuevo generado solo para producción, **nunca** el mismo que aparece en `.env.example`. |
| `USUARIO_ADMIN` / `CONTRASENA_ADMIN` | `admin` / `Admin2026!` (documentado a propósito en el README para el tribunal) | Se mantiene igual **a propósito** — el Bloque A.4.1 exige un usuario demo con credenciales publicadas, así que este es el único secreto de la lista que no se rota para producción. |

## 4. Procedimiento de despliegue

Todos los comandos requieren estar autenticados (`fly auth login`, abre
el navegador) y se corren desde la carpeta de cada app (`backend/` o
`frontend/`), donde vive su respectivo `fly.toml`.

### 4.1 Una sola vez (creación de las apps)

```bash
# instalar flyctl si no está: https://fly.io/docs/flyctl/install/
fly auth login

cd backend
fly launch --no-deploy --copy-config     # crea la app sged-profutbol-api en Fly
fly redis create                          # si el complemento gestionado de Fly (Upstash) está disponible; si no, crear el Redis directo en upstash.com y usar esa URL como secreto

cd ../frontend
fly launch --no-deploy --copy-config     # crea la app sged-profutbol-web
```

Si Fly asigna un nombre de app distinto al de `fly.toml` (nombre
ocupado), actualizar `app = "..."` en ambos `fly.toml` y el
`proxy_pass`/`Host` en `frontend/nginx.fly.conf` para que apunten al
nombre real del backend.

### 4.2 Secretos (una vez, o cuando se rotan — ver `RUNBOOK.md`)

```bash
cd backend
fly secrets set \
  DB_URL="jdbc:postgresql://<host-supabase>:6543/postgres?prepareThreshold=0&preparedStatementCacheQueries=0" \
  DB_USER="<usuario-supabase-produccion>" \
  DB_PASSWORD="<password-supabase-produccion>" \
  REDIS_HOST="<host-upstash>" \
  REDIS_PORT="6379" \
  JWT_SECRET="<nuevo-secreto-solo-de-produccion>" \
  USUARIO_ADMIN="admin" \
  CONTRASENA_ADMIN="Admin2026!" \
  CORS_ALLOWED_ORIGIN_PATTERNS="https://sged-profutbol-web.fly.dev" \
  IA_HABILITADO="false"
```

(`GEMINI_API_KEY` solo si `IA_HABILITADO=true`.)

### 4.3 Despliegue

```bash
cd backend  && fly deploy
cd ../frontend && fly deploy
```

### 4.4 Verificación (repetir después de cada despliegue, no solo el primero)

```bash
curl -I https://sged-profutbol-web.fly.dev/                 # 200, certificado válido
curl https://sged-profutbol-web.fly.dev/actuator/health     # {"status":"UP",...}
```

Ver la checklist completa en la sección 5.

## 5. Verificación posterior al despliegue

Checklist a correr después de cada despliegue a producción, no solo el
primero:

- [ ] `https://<dominio>/` sirve el frontend sin advertencia de
  certificado (regla transversal 8: si esto falla el día de la
  evaluación, P5 cae automáticamente a Insuficiente).
- [ ] `https://<dominio-api>/actuator/health` devuelve `UP` en todos los
  componentes (BD, Redis).
- [ ] Login con el usuario demo funciona end-to-end (cookie `HttpOnly`
  recibida, `SameSite=Strict`, con el flag `Secure` presente).
- [ ] Un procedimiento almacenado responde correctamente desde el
  ambiente público (por ejemplo `sp_contar_estudiantes_activos` vía el
  endpoint que lo invoca) — confirma que Supabase quedó con el esquema y
  los procedimientos de `db/procs/` aplicados, no solo las tablas base.

## 6. Recursos consumidos

Pendiente de completar con cifras reales una vez desplegado (CPU, RAM,
disco por servicio) — no se estima a ciegas.
