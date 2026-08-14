# Runbook de operación — SGED (Bloque A.4.2, Entrega Final)

Procedimientos de operación básica. Válidos tanto para el stack local
(`docker-compose.yml`) como para producción, salvo donde se indique lo
contrario — la topología exacta de producción (proveedor, DNS) se
documenta en `DEPLOYMENT.md`, todavía pendiente de definir el proveedor.

## 1. Arranque ordenado

```bash
git clone https://github.com/DarwinSM21/SGED_APPWEB.git
cd SGED_APPWEB
cp .env.example .env        # completar secretos reales, nunca committear .env
make up
```

`make up` espera activamente a que `/actuator/health` del backend
responda `healthy` antes de imprimir las URLs — no hay que adivinar si ya
está listo. En producción, sustituir por el mecanismo de arranque propio
del proveedor elegido (todavía sin definir), pero el orden de dependencia
no cambia: **base de datos y Redis alcanzables antes que el backend**, y
backend saludable antes que el frontend empiece a servir tráfico real.

Nota sobre la base de datos: `db/seed.sql` solo se aplica automáticamente
la primera vez que el volumen de Postgres se crea (vía
`docker-entrypoint-initdb.d`). Si el proyecto ya está usando Supabase como
Postgres gestionado (ver `Opción 2` en `.env.example`), ese seed inicial
se aplica una sola vez manualmente contra la base de Supabase, no en cada
arranque.

## 2. Apagado ordenado

```bash
make down          # apaga contenedores, conserva volúmenes/datos
# o, para liberar también volúmenes (dev/test, NUNCA en producción):
make clean
```

**`make clean` borra el volumen de Postgres.** En producción no se corre
nunca; el equivalente de apagado en producción es detener el/los
servicio(s) del proveedor sin tocar el almacenamiento persistente ni la
base de datos gestionada (Supabase, si aplica).

## 3. Rotación de secretos

Inventario real de secretos, sacado de `.env.example` — no hay ninguno
fuera de esta lista:

| Secreto | Dónde vive | Procedimiento de rotación |
|---|---|---|
| `JWT_SECRET` | Variable de entorno del backend | Generar uno nuevo (≥32 caracteres aleatorios), actualizar la variable y reiniciar el backend. **Efecto secundario esperado, no un bug:** al ser JWT *stateless* firmado con ese secreto, cambiarlo invalida instantáneamente **todas** las sesiones activas — de hecho es el mecanismo de revocación masiva de emergencia si se sospecha un secreto comprometido. |
| `DB_PASSWORD` (o credencial de Supabase) | Panel de Supabase / variable de entorno | Rotar primero en el proveedor (Supabase → Database → Reset password, o el motor que corresponda), y solo después actualizar la variable de entorno del backend y reiniciar — en ese orden, para no dejar una ventana donde el backend tenga una contraseña que la base ya no reconoce. |
| `GEMINI_API_KEY` | Google AI Studio / variable de entorno | Revocar la key en [ai.google.dev](https://ai.google.dev), generar una nueva, actualizar la variable. Si `IA_HABILITADO=false`, no hay urgencia — la funcionalidad ya degrada de forma segura sin ella (la evaluación se guarda igual, el comentario generado simplemente no aparece). |
| `CONTRASENA_ADMIN` (usuario semilla `admin`) | `db/seed.sql`, hasheada con BCrypt | En producción, cambiar la contraseña del usuario `admin` real inmediatamente después del primer arranque (vía la propia aplicación, no editando el seed), o regenerar el hash BCrypt e inyectarlo por variable de entorno antes del primer seed si se automatiza. La contraseña publicada en el README (`Admin2026!`) es intencionalmente pública y **nunca** debe ser la vigente en un ambiente con datos reales. |

## 4. Rotación de contenedores por actualizaciones de seguridad

```bash
docker compose pull         # trae las imágenes con digest actualizado (postgres, redis)
docker compose up -d --build   # reconstruye backend/frontend con la última capa base
```

Revisar periódicamente si hay una versión más nueva de las imágenes base
del `Dockerfile` (`eclipse-temurin:21-jdk/jre`, `node:20-alpine`,
`nginx:1.25-alpine`) con parches de seguridad, y de las imágenes pinadas
por digest en `docker-compose.yml` (`postgres`, `redis`) —
`scripts/pin-digests.sh` regenera esos digests. No hay automatización de
esto todavía (Dependabot o equivalente); es manual hasta que se agregue.

## 5. Restauración desde respaldo

Ver `BACKUP.md` para la estrategia completa. Resumen del procedimiento:

1. Confirmar el respaldo a restaurar (fecha, destino) en `BACKUP.md`.
2. Apagar el backend (evita escrituras concurrentes durante la restauración).
3. Restaurar el dump sobre una base nueva o la existente, según el caso:
   `psql "$DB_URL" < respaldo_YYYY-MM-DD.sql` (o `pg_restore` si el dump
   es en formato custom).
4. Verificar `/actuator/health` y un flujo de lectura básico (login +
   listado de estudiantes) antes de reabrir tráfico.
5. Registrar la restauración (fecha, motivo, quién la ejecutó) — no existe
   todavía un lugar formal para ese registro; usar por ahora una entrada
   en `CHANGELOG.md` bajo una sección `### Operación`.
