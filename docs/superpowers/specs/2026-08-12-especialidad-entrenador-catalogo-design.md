# Diseño: Catálogo de Especialidad para Entrenador

**Fecha:** 2026-08-12
**Estado:** Aprobado para implementación

## Contexto

`Entrenador.especialidad` es hoy un campo de texto libre
(`String`, largo 150, `backend/.../entrenador/entity/Entrenador.java`).
Cualquier valor es válido, lo que produce datos inconsistentes
("Fútbol", "futbol", "Preparación física", "prep. física", etc.) y no
permite filtrar ni reportar por especialidad de forma confiable.

El usuario pidió reemplazarlo por un catálogo: una tabla de
especialidades, con un dropdown en el formulario para elegir una.
Explícitamente **una sola especialidad por entrenador** (no
multivaluado) — fuera de alcance cualquier relación muchos-a-muchos.

En la base real del proyecto no existe todavía ningún registro de
`Entrenador` (confirmado navegando la pestaña "Entrenadores" de
`/personas`, que muestra "No hay entrenadores que coincidan"), así que
no hay datos de `especialidad` en texto libre que migrar.

## Alcance

Incluido:
- Tabla nueva `deportivo.especialidades`, calcada de
  `deportivo.categorias` pero sin los campos específicos de
  categoría (`edad_min`/`edad_max`): solo `nombre` (único) + `activo`
  + timestamps.
- Backend: módulo `deportivo.especialidad` completo
  (entity/dto/repository/service/controller), mismo patrón que
  `deportivo.categoria`.
- `Entrenador.especialidad` (String) → `Entrenador.idEspecialidad`
  (FK `Long`, nullable — sigue sin ser obligatorio, igual que hoy).
- Migración `V17__especialidad_entrenador.sql`: crea la tabla, la
  siembra con 6 especialidades genéricas de fútbol formativo, agrega
  `id_especialidad` a `entrenadores`, elimina la columna `especialidad`
  vieja.
- Frontend: el input de texto libre de especialidad en el formulario
  de entrenador (`personas-admin.component.ts`) se reemplaza por un
  `<select>`, mismo patrón que el dropdown de categoría en el
  formulario de estudiante.

Explícitamente fuera de alcance:
- Pantalla de gestión (CRUD) del catálogo de especialidades en el
  frontend — mismo criterio que `categorias`, que tampoco tiene una
  hoy. Si hace falta agregar una especialidad nueva más adelante, se
  hace vía API (Swagger) o directo en la base, igual que con
  categorías.
- Múltiples especialidades por entrenador.
- Migración de datos existentes (no hay ninguno).

## Backend

### Tabla `deportivo.especialidades`

```sql
CREATE TABLE deportivo.especialidades (
    id_especialidad BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO deportivo.especialidades (nombre) VALUES
    ('Preparador físico'), ('Técnico'), ('Táctico'),
    ('Porteros'), ('Fuerza y acondicionamiento'), ('Físico');
```

### `Especialidad` — entity/dto/repository/service/controller

Mismo patrón que `Categoria`/`CategoriaController`/`CategoriaService`,
recortado a los campos de arriba:

- `EspecialidadRepository`: `findByActivoTrue(Pageable)`,
  `findByActivoTrue()` (sin paginar, para el dropdown).
- `EspecialidadController`:
  - `GET /api/especialidades` — paginado, `ADMINISTRADOR`.
  - `GET /api/especialidades/activas` — lista simple,
    `ADMINISTRADOR`, `ENTRENADOR`, `RECEPCIONISTA` (mismos roles que
    `/api/categorias/activas`, para poblar el dropdown).
  - `GET /api/especialidades/{id}` — `ADMINISTRADOR`.
  - `POST`/`PUT`/`DELETE /api/especialidades{/id}` — `ADMINISTRADOR`
    (soft-delete vía `activo=false`, igual que Categoria).
- `EspecialidadService`: validación de `nombre` único (400 si ya
  existe, mismo criterio de negocio de otros catálogos del proyecto).

### `Entrenador` — cambio de columna

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_especialidad")
private Especialidad especialidad;   // antes: String especialidad
```

`EntrenadorRequest` cambia `String especialidad` → `Long idEspecialidad`
(nullable, sin `@NotNull` — sigue siendo opcional). `EntrenadorResponse`
cambia `String especialidad` → `Long idEspecialidad` + `String
nombreEspecialidad` (para no forzar al frontend a resolver el nombre
aparte). `EntrenadorService.crear/editar/toResponse` se actualizan para
buscar el `Especialidad` por id (404 si no existe, mismo criterio que
`idPersona`/`idUsuario`) y exponer `nombreEspecialidad`.

### Migración `V17__especialidad_entrenador.sql`

```sql
-- (tabla + seed de arriba)

ALTER TABLE deportivo.entrenadores
    ADD COLUMN id_especialidad BIGINT REFERENCES deportivo.especialidades(id_especialidad);

ALTER TABLE deportivo.entrenadores DROP COLUMN especialidad;
```

## Frontend

- `personas.models.ts`:
  - Nueva interfaz `EspecialidadOpcion { idEspecialidad: number; nombre: string; }`
    (mismo shape que `CategoriaOpcion`).
  - `EntrenadorRequest.especialidad: string | null` →
    `idEspecialidad: number | null`.
  - `EntrenadorResponse.especialidad: string | null` →
    `idEspecialidad: number | null` + `nombreEspecialidad: string | null`.
- `personas.service.ts`: nuevo método
  `especialidadesActivas() { return this.http.get<EspecialidadOpcion[]>('/api/especialidades/activas'); }`.
- `personas-admin.component.ts`:
  - Nuevo signal `especialidades = signal<EspecialidadOpcion[]>([])`,
    cargado en `ngOnInit()` igual que `categorias`.
  - El bloque "Entrenador" del formulario cambia el `<input
    [(ngModel)]="formEntrenador.especialidad">` de texto por un
    `<select>` con `@for (esp of especialidades(); track
    esp.idEspecialidad)`, igual estructura que el `<select
    id="e-categoria">` de Estudiante.
  - `formEntrenador` cambia `especialidad: string` →
    `idEspecialidad: number | null`.
  - La vista de solo-lectura del entrenador ya registrado
    (`ent.especialidad || 'sin especialidad'`) pasa a
    `ent.nombreEspecialidad || 'sin especialidad'`.
  - La pestaña de gestión "Entrenadores" (agregada en la spec de
    2026-08-12-personas-gestion-tabs-design.md) también lee
    `ent.especialidad` en su columna — se actualiza a
    `ent.nombreEspecialidad`.

## Documentación

- `docs/requisitos/SRS.md`: nota en el RF de Entrenador (backend)
  indicando que `especialidad` pasó de texto libre a catálogo
  (`deportivo.especialidades`).
- `docs/basedatos/DATA-DICTIONARY.md`: agregar `deportivo.especialidades`
  y actualizar la fila de `entrenadores.id_especialidad`.
- `docs/trazabilidad/matriz.csv`: sin fila nueva de RF (es un cambio
  de forma de un campo existente, no un requisito nuevo).

## Pruebas

- `EntrenadorServiceTest`/`EntrenadorControllerTest` (ya existentes):
  actualizar los casos que usan `especialidad` texto libre para usar
  `idEspecialidad` contra una `Especialidad` sembrada en el test;
  agregar caso de `idEspecialidad` inexistente → 404/400.
- Nuevo `EspecialidadServiceTest` (o extender uno existente si el
  proyecto agrupa catálogos): crear con nombre duplicado → error;
  listar activas; soft-delete.
