# Evidencia de restauración de respaldo — SGED (RNF-24 / Bloque A.4.2)

**Fecha:** 2026-09-09
**Ejecutó:** equipo SGED (`darcalleg`)
**Objetivo:** cumplir la condición (d) de RNF-24 — *"un procedimiento de
restauración documentado y verificado con evidencia archivada y fechada de al
menos una ejecución real contra una base separada"*— y medir el **RTO real**,
no estimado.

---

## 1. Respaldo de origen

Respaldo completo de la base de datos de **producción** (Supabase), tomado con
`pg_dump` en formato custom comprimido, tal como especifica RNF-24 (a).

| | |
|---|---|
| Comando | `pg_dump -F c --no-owner --no-privileges -n seguridad -n academico -n deportivo -n inventario` |
| Archivo | `sged_prod_2026-09-09.dump` (167 KB) |
| Creado | 2026-09-09 06:13:59 UTC |
| `pg_dump` | 17.11 |
| Motor de origen | PostgreSQL **17.6** (Supabase) |
| Esquemas incluidos | `seguridad`, `academico`, `deportivo`, `inventario` (se excluyen los internos de Supabase: `auth`, `storage`, `graphql`, …) |
| Entradas TOC | 397 |
| Destino del archivo | almacenamiento privado del equipo, **fuera del repositorio** (el dump contiene datos personales de menores) |

## 2. Entorno de restauración (base separada)

Contenedor Docker efímero, **aislado de producción**:

```
docker run -d --name sged_rto -e POSTGRES_PASSWORD=*** -e POSTGRES_DB=sged_restore postgres:17
```

- Motor: PostgreSQL 17.11 (imagen oficial `postgres:17`).
- Base destino: `sged_restore`, vacía antes de la prueba.

## 3. Procedimiento ejecutado

```
docker cp sged_prod_2026-09-09.dump sged_rto:/tmp/prod.dump
docker exec sged_rto pg_restore --clean --if-exists --no-owner --no-privileges \
    -U postgres -d sged_restore /tmp/prod.dump
```

## 4. Cronometraje — RTO del paso de restauración

| Corrida | Base destino | Duración `pg_restore` | Código de salida | Errores |
|---|---|---|---|---|
| 1 | vacía | **0,90 s** | 0 | 0 |
| 2 | con datos previos (`--clean`) | 1,27 s | 0 | 0 |
| 3 | con datos previos (`--clean`) | 1,30 s | 0 | 0 |
| 4 | con datos previos (`--clean`) | 1,32 s | 0 | 0 |

- **RTO del `pg_restore` (dataset actual): ≈ 1 s** sobre una base recién
  aprovisionada; ≈ 1,3 s cuando además hay que descartar los objetos previos.
- **RTO de la recuperación completa del servicio** (aprovisionar una base
  nueva + restaurar + reapuntar el backend + verificar): dominado por el
  aprovisionamiento de la base, del orden de **minutos** (≈ 3–5 min con un
  proyecto Supabase nuevo o un contenedor local). El `pg_restore` en sí no es
  el cuello de botella con este volumen de datos.
- El RTO crece con el tamaño de los datos; con datos reales (no de demo) debe
  volver a medirse.

## 5. Verificación post-restauración

Un `pg_restore` sin errores no garantiza coherencia con lo que la aplicación
espera; se verifica explícitamente:

### 5.1 Estructura

| Esquema | Tablas restauradas |
|---|---|
| `seguridad` | 6 |
| `academico` | 6 |
| `deportivo` | 20 |
| `inventario` | 3 |

Objetos de código presentes (12): `academico.sp_anonimizar_estudiante`,
`sp_contar_estudiantes_activos`, `sp_desactivar_estudiantes_categoria`,
`sp_generar_codigo_estudiante`, `sp_contacto_representante_estudiante`,
`deportivo.sp_reporte_asistencia_estudiante`,
`sp_validar_categoria_estudiante_sesion`, `inventario.sp_reporte_stock_bajo`,
y los 4 triggers `set_updated_at` / `set_actualizado_en`.

Migraciones incrementales presentes (aplicadas a producción el 2026-09-09):
`ck_lesion_descripcion_longitud` (V25), `ux_personas_cedula_no_nula` (V26),
`sp_anonimizar_estudiante` (V27) → los tres verificados.

### 5.2 Datos

| Tabla | Filas |
|---|---|
| `seguridad.personas` | 29 |
| `seguridad.usuarios` | 26 |
| `seguridad.roles` | 5 |
| `seguridad.auditoria` | 85 |
| `academico.estudiantes` | 16 |
| `academico.representantes` | 2 |
| `academico.consentimientos` | 0 |
| `academico.pagos` | 0 |
| `deportivo.categorias` | 3 (SUB-12, SUB-14, SUB-16) |
| `deportivo.asistencias` | 11 |
| `deportivo.evaluaciones_diarias` | 2 |
| `deportivo.lesiones` | 0 |

### 5.3 Flujo de lectura real

- **Autenticación:** el usuario `admin` con rol `ADMINISTRADOR` existe y resuelve
  por el join `usuarios → usuario_rol → roles`.
- **Listado por categoría (procedimiento almacenado):**
  `CALL academico.sp_contar_estudiantes_activos(1, NULL)` → **11** estudiantes
  activos en SUB-12.
- Consultas de catálogo (`deportivo.categorias`) y de historial
  (`deportivo.asistencias`) devuelven filas coherentes.

## 6. Resultado

✅ **Restauración exitosa y verificada.** El respaldo de producción se
restaura sin errores en una base separada, el esquema y los procedimientos
quedan completos, y un flujo de lectura de la aplicación (login + listado por
SP) funciona sobre la base restaurada.

## 7. RPO

- **RPO garantizado: ≤ 24 h** — el `pg_dump` propio se ejecuta a diario
  (`docs/despliegue/BACKUP.md`), de modo que la pérdida máxima ante un
  incidente que inutilice la base es de un día de operación.
- **PITR de Supabase:** el proyecto está en el **plan Free**, que **no ofrece
  PITR ni respaldos gestionados**. El `pg_dump` diario propio es la única
  copia de seguridad y el punto de recuperación es siempre el de ese dump.

## 8. Limpieza

El contenedor `sged_rto` y su base se eliminan al terminar la prueba
(`docker rm -f sged_rto`). El archivo de respaldo **no** se versiona en el
repositorio.
