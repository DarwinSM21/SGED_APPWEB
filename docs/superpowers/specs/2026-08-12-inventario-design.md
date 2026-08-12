# Diseño: Módulo Inventario

**Fecha:** 2026-08-12
**Estado:** Aprobado para implementación

## Contexto

El `ADR-003` reservó desde el inicio un quinto schema, `inventario`
("Control de uniformes, balones, implementos deportivos y asignaciones"),
pero lo dejó como diseño a futuro: "sin migración Flyway todavía — no se
presenta como implementado en el SRS". El `SRS.md` (§1.2 Alcance) declara
hoy solo 3 dominios: seguridad, académico, deportivo.

Este documento especifica la primera versión completa del módulo
Inventario: catálogo de artículos, control de stock por movimientos, y
asignación/devolución de artículos a estudiantes o entrenadores.

## Alcance

Incluido:
- Catálogo de artículos (uniformes, balones, implementos, otro) con stock
  agregado por cantidad (no serializado por unidad individual).
- Registro de movimientos de stock (entradas, salidas, ajustes).
- Asignación de artículos a estudiantes o entrenadores, con devolución.
- Reporte de artículos con stock por debajo del mínimo (vía procedimiento
  almacenado, para mantener la estrategia híbrida ORM+SP del proyecto).
- Actualización de SRS, ADR-003, matriz de trazabilidad, catálogo de SP y
  diccionario de datos.

Explícitamente fuera de alcance de esta primera versión:
- Tracking serializado por unidad individual (ej. "Balón #14").
- Asignación a categorías/equipos completos (solo a una persona:
  estudiante o entrenador).
- Compras/proveedores, costos o valorización monetaria del inventario.
- Actualización de `historias-usuario.md` / `casos-uso.md` (se deja para
  una iteración posterior si se pide explícitamente).

## Modelo de datos (schema `inventario`)

Se sigue el estilo más reciente del proyecto (igual que `academico.pagos`,
migración V13): `BIGSERIAL` PK, `CHECK` para enums, `created_at`/
`updated_at` con trigger `set_updated_at`, FK explícitas, columna
`registrado_por_id_usuario` para auditoría de quién ejecuta la acción.

### `inventario.articulos`

| Columna | Tipo | Notas |
|---|---|---|
| id_articulo | BIGSERIAL PK | |
| nombre | VARCHAR(150) NOT NULL | |
| tipo | VARCHAR(20) NOT NULL | CHECK IN ('UNIFORME','BALON','IMPLEMENTO','OTRO') |
| talla | VARCHAR(20) NULL | aplica a UNIFORME |
| descripcion | VARCHAR(255) NULL | |
| stock_actual | INTEGER NOT NULL DEFAULT 0 | CHECK stock_actual >= 0 |
| stock_minimo | INTEGER NOT NULL DEFAULT 0 | CHECK stock_minimo >= 0 |
| unidad_medida | VARCHAR(20) NOT NULL DEFAULT 'unidad' | |
| activo | BOOLEAN NOT NULL DEFAULT TRUE | baja lógica |
| created_at / updated_at | TIMESTAMPTZ | trigger `trg_articulos_updated_at` |

### `inventario.movimientos_stock`

Ledger de entradas/salidas/ajustes, independiente de a quién se entrega
algo (eso lo cubre `asignaciones`).

| Columna | Tipo | Notas |
|---|---|---|
| id_movimiento | BIGSERIAL PK | |
| id_articulo | BIGINT NOT NULL REFERENCES articulos | |
| tipo_movimiento | VARCHAR(10) NOT NULL | CHECK IN ('ENTRADA','SALIDA','AJUSTE') |
| cantidad | INTEGER NOT NULL | CHECK cantidad > 0 |
| motivo | VARCHAR(255) NULL | |
| registrado_por_id_usuario | BIGINT NOT NULL REFERENCES seguridad.usuarios | |
| fecha_movimiento | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |
| created_at | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |

Un `ENTRADA`/`AJUSTE` positivo suma a `stock_actual`; un `SALIDA` resta.
La actualización de `stock_actual` ocurre en el service (`@Transactional`),
no vía trigger, siguiendo el patrón del proyecto de mantener las reglas de
negocio en Java. El service rechaza (`422 ProblemDetail`) cualquier
`SALIDA` que deje `stock_actual` negativo.

### `inventario.asignaciones`

| Columna | Tipo | Notas |
|---|---|---|
| id_asignacion | BIGSERIAL PK | |
| id_articulo | BIGINT NOT NULL REFERENCES articulos | |
| cantidad | INTEGER NOT NULL | CHECK cantidad > 0 |
| tipo_destinatario | VARCHAR(15) NOT NULL | CHECK IN ('ESTUDIANTE','ENTRENADOR') |
| id_estudiante | BIGINT NULL REFERENCES academico.estudiantes | |
| id_entrenador | BIGINT NULL REFERENCES deportivo.entrenadores | |
| fecha_asignacion | DATE NOT NULL DEFAULT CURRENT_DATE | |
| fecha_devolucion_esperada | DATE NULL | |
| fecha_devolucion_real | DATE NULL | |
| estado | VARCHAR(15) NOT NULL DEFAULT 'ASIGNADO' | CHECK IN ('ASIGNADO','DEVUELTO','PERDIDO') |
| registrado_por_id_usuario | BIGINT NOT NULL REFERENCES seguridad.usuarios | |
| observaciones | VARCHAR(255) NULL | |
| created_at / updated_at | TIMESTAMPTZ | trigger `trg_asignaciones_updated_at` |

Constraint cruzado: exactamente una de `id_estudiante`/`id_entrenador` no
nula, según `tipo_destinatario`:

```sql
CONSTRAINT chk_asignacion_destinatario CHECK (
    (tipo_destinatario = 'ESTUDIANTE' AND id_estudiante IS NOT NULL AND id_entrenador IS NULL)
    OR (tipo_destinatario = 'ENTRENADOR' AND id_entrenador IS NOT NULL AND id_estudiante IS NULL)
)
```

Crear una asignación resta `cantidad` de `stock_actual` (mismo chequeo de
no negativo que una `SALIDA`); marcar `estado = 'DEVUELTO'` con
`fecha_devolucion_real` repone esa cantidad. `PERDIDO` no repone stock.

`movimientos_stock` y `asignaciones` quedan como dos historiales
independientes — uno responde "¿cuánto entró/salió del depósito?", el
otro "¿quién tiene qué?" — sin acoplarlos entre sí, porque cada pantalla
del frontend consulta una sola tabla y no hace falta la indirección de
un movimiento compartido.

### Migración e instalación

- Flyway: `backend/src/main/resources/db/migration/V15__inventario.sql`.
- Reflejado también en `db/schema.sql` (montado en
  `/docker-entrypoint-initdb.d/` para `make up`), igual que las tablas
  anteriores.
- Nuevo `db/procs/sp_reporte_stock_bajo.sql`, instalado por ambas vías
  (Flyway V15 y `db/schema.sql`), igual que los procedimientos existentes.

## Backend (Spring Boot)

Paquete `org.uteq.backend.inventario`, subpaquetes `articulo`,
`movimiento`, `asignacion` — mismo patrón entity/dto/repository/service/
controller que `deportivo.categoria` / `academico.pago`.

Endpoints y permisos (`@PreAuthorize`, mismo estilo que
`CategoriaController`):

| Endpoint | Método | Roles |
|---|---|---|
| `/api/inventario/articulos` | GET | ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR |
| `/api/inventario/articulos/{id}` | GET | ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR |
| `/api/inventario/articulos/stock-bajo` | GET | ADMINISTRADOR, RECEPCIONISTA |
| `/api/inventario/articulos` | POST | ADMINISTRADOR, RECEPCIONISTA |
| `/api/inventario/articulos/{id}` | PUT | ADMINISTRADOR, RECEPCIONISTA |
| `/api/inventario/articulos/{id}` | DELETE (baja lógica) | ADMINISTRADOR, RECEPCIONISTA |
| `/api/inventario/movimientos` | GET | ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR |
| `/api/inventario/movimientos` | POST | ADMINISTRADOR, RECEPCIONISTA |
| `/api/inventario/asignaciones` | GET | ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR |
| `/api/inventario/asignaciones` | POST | ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR |
| `/api/inventario/asignaciones/{id}/devolver` | PATCH | ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR |

ENTRENADOR solo puede leer el catálogo y crear/devolver asignaciones; no
gestiona artículos ni movimientos de stock.

Reglas de negocio (en el service, no en la DB):
- Rechazar `SALIDA`/asignación que deje `stock_actual < 0` → `422`
  vía el `GlobalExceptionHandler` existente (mismo patrón que
  `EstudianteControllerTest.crear_con_categoria_invalida_da_422`).
- Rechazar devolver una asignación que ya está `DEVUELTO`/`PERDIDO`.

Procedimiento almacenado nuevo (mantiene la estrategia híbrida ORM+SP y
cubre la categoría "reporte" también en este módulo):

`inventario.sp_reporte_stock_bajo` — sin parámetros de entrada, devuelve
el conjunto de artículos activos con `stock_actual <= stock_minimo`.
Invocado vía `@Procedure` desde `ArticuloRepository`, expuesto en
`GET /api/inventario/articulos/stock-bajo`.

## Frontend (Angular)

`frontend/src/app/features/inventario/`, mismo patrón que `features/
pagos/` (component + service + models por pantalla):

- **Artículos**: listado con filtro por tipo/activo; alta/edición
  visibles solo para ADMINISTRADOR/RECEPCIONISTA; badge de stock bajo
  usando el endpoint `stock-bajo`.
- **Movimientos**: formulario para registrar ENTRADA/SALIDA/AJUSTE +
  tabla histórica; visible para ADMINISTRADOR/RECEPCIONISTA.
- **Asignaciones**: formulario para asignar a estudiante o entrenador +
  tabla con estado y acción "Devolver"; visible para los 3 roles
  (ENTRENADOR ve esta pantalla únicamente).

Entradas de navegación condicionadas por rol, igual que el resto del
shell existente.

## Documentación y trazabilidad

Por convención del proyecto (todo RF se documenta con historia, caso de
uso, endpoint, archivo, prueba y evidencia en la matriz):

- `docs/requisitos/SRS.md`:
  - §1.2 Alcance: agregar "Inventario" como cuarto dominio.
  - Nuevos requisitos, forma "El sistema deberá...", con estado real:
    - **RF-27** — Gestión del catálogo de artículos de inventario.
    - **RF-28** — Registro de movimientos de stock (entradas, salidas,
      ajustes).
    - **RF-29** — Asignación y devolución de artículos a estudiantes o
      entrenadores.
    - **RF-30** — Reporte de artículos con stock por debajo del mínimo.
- `docs/adr/ADR-003-base_datos.md`: actualizar la nota de estado — deja
  de decir "inventario... sin migración Flyway todavía".
- `docs/trazabilidad/matriz.csv`: una fila por cada RF-27..RF-30.
- `docs/basedatos/CATALOGO-SP.md`: fila para `sp_reporte_stock_bajo`,
  mismo formato que las 6 entradas existentes.
- `docs/basedatos/DATA-DICTIONARY.md`: entradas para `articulos`,
  `movimientos_stock`, `asignaciones`.

No se toca `historias-usuario.md` / `casos-uso.md` en esta iteración
(ver "Fuera de alcance").

## Pruebas

JUnit 5 por service, siguiendo el patrón `*ServiceTest.java` existente
(ej. `EstudianteServiceTest`, `AsistenciaServiceTest`):
- `ArticuloServiceTest`: alta/edición/baja lógica, validación de tipo.
- `MovimientoServiceTest`: ENTRADA/AJUSTE suman stock, SALIDA resta,
  SALIDA que deja stock negativo se rechaza con 422.
- `AsignacionServiceTest`: asignar resta stock, devolver repone stock,
  no se puede devolver dos veces, constraint de destinatario único.

Objetivo: no bajar la cobertura JaCoCo actual (≥60%, ver RNF-09 en la
matriz de trazabilidad).
