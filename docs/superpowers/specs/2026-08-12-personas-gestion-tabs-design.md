# Diseño: Pestañas de gestión en la pantalla de Personas (ADMINISTRADOR)

**Fecha:** 2026-08-12
**Estado:** Aprobado para implementación

## Contexto

`personas-admin.component.ts` (ver
`docs/superpowers/specs/2026-08-12-personas-unificado-design.md`) ya
resuelve el alta de Persona/Usuario/Estudiante/Entrenador/Representante
mediante un maestro-detalle centrado en `Persona`, con badges en cada
fila (rol, estudiante, entrenador, representante).

Esos badges alcanzan para saber *que* una persona tiene una cuenta o un
rol, pero no sirven para **gestionar** cada colección como tal: no hay
forma de ver de un vistazo "todos los Usuarios con rol RECEPCIONISTA",
"todos los Entrenadores activos", o "quién representa a quién", sin
recorrer la lista completa de personas una por una. El usuario pidió
explícitamente un apartado para ver y gestionar usuarios, estudiantes,
entrenadores, recepcionistas y representantes — no solo personas.

## Alcance

Incluido:
- `personas-admin.component.ts` pasa de un solo panel a un layout con
  **pestañas**, mismo patrón que `InventarioComponent`:
  - **Personas** (pestaña por defecto): el maestro-detalle actual, sin
    cambios de comportamiento.
  - **Usuarios**, **Estudiantes**, **Entrenadores**, **Representantes**:
    pestañas nuevas, cada una con buscador de texto, un filtro
    adicional donde aplique, toggle "Mostrar inactivos" (activos por
    defecto), y una tabla de solo lectura.
  - Clic en una fila de cualquier pestaña nueva cambia a la pestaña
    Personas y selecciona esa persona en el maestro-detalle existente
    (reutiliza `seleccionar()` — sin duplicar lógica de edición).
- RECEPCIONISTA no gana acceso a esto: `PersonasAdminComponent` solo se
  renderiza para ADMINISTRADOR (`personas.component.ts`), sin cambios
  ahí.

Explícitamente fuera de alcance:
- Edición o desactivación inline en las tablas nuevas — toda edición
  sigue pasando por el panel de detalle de la pestaña Personas.
- Endpoints o filtros nuevos en el backend: las 4 pestañas nuevas
  reutilizan `listarUsuarios()`, `listarEstudiantes()`,
  `listarEntrenadores()`, `listarRepresentantes()` — ya cargados por
  `ngOnInit()` para construir los badges — filtrando y buscando en el
  cliente.
- "Recepcionista" como entidad propia: es un valor del campo `rol` de
  `UsuarioResponse.roles`, así que aparece como una opción más del
  filtro de rol en la pestaña Usuarios, no como pestaña separada.
- Paginación: mismo criterio que hoy (`size=500`/`size=200`), sin
  paginar en el cliente.

## Frontend

Todo el cambio vive en `personas-admin.component.ts` (no hace falta
tocar `personas.service.ts` ni `personas.models.ts`, los datos ya
están disponibles vía los signals existentes `usuarios`, `estudiantes`,
`entrenadores`, `representantes`).

### Estado nuevo

```ts
type Tab = 'personas' | 'usuarios' | 'estudiantes' | 'entrenadores' | 'representantes';
readonly tabActiva = signal<Tab>('personas');

busquedaUsuarios = '';
filtroRolUsuarios: RolUsuario | 'TODOS' = 'TODOS';
mostrarInactivosUsuarios = signal(false);

busquedaEstudiantes = '';
mostrarInactivosEstudiantes = signal(false);

busquedaEntrenadores = '';
mostrarInactivosEntrenadores = signal(false);

busquedaRepresentantes = '';
mostrarInactivosRepresentantes = signal(false);
```

Cada pestaña tiene su propio `computed` de filtrado (busca por texto +
filtro de rol/estado), análogo al `personasFiltradas()` que ya existe.

### Navegación pestaña → detalle

```ts
irAPersona(idPersona: number): void {
  const p = this.personas().find((x) => x.persona.idPersona === idPersona);
  if (p) { this.tabActiva.set('personas'); this.seleccionar(p); }
}
```

### Columnas por pestaña

- **Usuarios**: nombre completo (`nombrePersona apellidoPersona`),
  `username`, `roles` (chips), `estadoGeneralNombre`, `ultimoAcceso`,
  badge activo/inactivo. Filtro de rol: select con `ROLES_USUARIO` +
  "Todos".
- **Estudiantes**: nombre completo, `codigoEstudiante`,
  `nombreCategoria`, `fechaIngreso`, badge activo/inactivo.
- **Entrenadores**: nombre completo, `especialidad`,
  `experienciaAnios`, `username`, badge activo/inactivo.
- **Representantes**: nombre completo, `parentesco`,
  `telefonoContacto`, `representados.length` ("N estudiante(s)"), badge
  activo/inactivo.

### UI

Barra de pestañas idéntica en estructura a la de `InventarioComponent`
(`.tabs` / `.tab` / `.tab--activo`), insertada arriba del layout
`.maestro-detalle` actual. Cuando `tabActiva() !== 'personas'`, se
oculta `.maestro-detalle` y se muestra una tabla con buscador + filtros
arriba, reutilizando las clases `.card`, `.buscador`, `.badge` ya
definidas en este mismo componente.

## Documentación

- `docs/requisitos/SRS.md`: nota en el "Origen" de RF-23/24/25 (ya
  actualizados por la spec de unificación) mencionando que la consulta
  de estas listas ahora tiene una vista dedicada en `/personas`.
- `docs/trazabilidad/matriz.csv`: sin filas nuevas — no hay RF ni
  endpoint nuevo, es una vista adicional sobre datos ya trazados.

## Pruebas

Sin pruebas de backend nuevas (no hay cambios de backend). No hay
suite de pruebas de frontend en este proyecto (ver componentes
existentes de `personas`/`inventario`, ninguno tiene `.spec.ts`); se
verifica manualmente en navegador: cada pestaña carga, filtra, y el
clic en una fila navega correctamente a Personas con la selección
correcta.
