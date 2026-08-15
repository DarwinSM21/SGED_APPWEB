# Diseño: Registro de lesiones desde Evaluación diaria + pantalla "Mi equipo" del estudiante

**Fecha:** 2026-08-14
**Estado:** Aprobado para implementación

## Contexto

El backend ya tiene un `LesionController` completo (registrar, dar de
alta, listar activas, historial por estudiante) desde una sesión
anterior, pero **ningún frontend lo consume**: `evaluacion-diaria.component.ts`
solo *muestra* un badge de solo lectura ("Lesionado") que ya trae
calculado el backend; no hay botón ni formulario para que el entrenador
registre una lesión nueva o dé de alta una existente.

Por el lado del estudiante, `features/estudiante/` solo tiene
`marcar-asistencia` y `mi-historial` (asistencia). El estudiante no
tiene forma de ver sus propias estadísticas de evaluación, su posición
nominal, quién es su entrenador, ni quiénes son sus compañeros de
categoría — todo eso hoy solo lo puede consultar ADMINISTRADOR,
ENTRENADOR o (para las estadísticas) su propio representante.

Este documento cubre dos features relacionadas por el dominio de
Lesión pero independientes en implementación:

- **A.** El entrenador marca/da de alta una lesión desde Evaluación diaria.
- **B.** Una pantalla nueva para el estudiante ("Mi equipo") con
  estadísticas, posición, entrenador asignado, compañeros y detalles
  de su categoría.

## Alcance

Incluido:
- Endurecer `LesionController.registrar()` para que el `idEntrenador`
  de una cuenta ENTRENADOR salga del token, no del body.
- Agregar `idLesion` a `JugadorEvaluableResponse` para poder dar de
  alta sin una consulta aparte.
- Botones "Marcar lesión" / "Dar de alta" en Evaluación diaria.
- Dos endpoints nuevos bajo `/api/estudiante`, solo rol ESTUDIANTE:
  `mi-informe` (reutiliza la lógica de informe del representante) y
  `mi-equipo` (categoría + posición + entrenador + compañeros).
- Pantalla nueva `/estudiante/mi-equipo` en el frontend.

Explícitamente fuera de alcance:
- Pantalla separada de "Lesiones" para administrador/entrenador (listar
  todas, filtros, etc.) — el usuario ya eligió solo el flujo rápido
  desde Evaluación diaria.
- Editar la posición nominal del estudiante desde esta pantalla — sigue
  siendo de solo lectura para el estudiante (se edita desde Personas,
  como hoy).
- Cambiar qué constituye "mi entrenador" cuando la categoría tiene
  horarios con más de un entrenador en el mismo día — se toma
  simplemente la sesión más próxima por fecha/hora, sin resolver
  empates ni desambiguar turnos.
- Cualquier dato de contacto o promedio de los compañeros de equipo —
  solo nombre y posición, por privacidad (son menores de edad).

## Backend

### A.1 — `LesionController.registrar()`: no confiar en `idEntrenador` del body

Hoy:

```java
var lesion = lesionService.registrar(
        request.idEstudiante(), request.idEntrenador(), request.descripcion(),
        request.fechaLesion(), request.fechaEstimadaRetorno());
```

Cualquier cuenta ENTRENADOR podría mandar el `idEntrenador` de otro
colega. Se inyecta `EntrenadorRepository` en el controller y se
resuelve así (mismo patrón que `SesionEntrenamientoController`):

```java
private Long idEntrenadorEfectivo(Long idEntrenadorDelBody) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    boolean esEntrenador = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ENTRENADOR"));
    if (!esEntrenador) {
        return idEntrenadorDelBody; // ADMINISTRADOR: no tiene id propio, se respeta el del body
    }
    return entrenadorRepository.findByUsuario_Username(auth.getName())
            .orElseThrow(() -> new RecursoNoEncontradoException("No hay un entrenador asociado a esta cuenta"))
            .getIdEntrenador();
}
```

`idEntrenador` deja de ser `@NotNull` en `RegistrarLesionRequest` (una
cuenta ENTRENADOR ya no necesita mandarlo; ADMINISTRADOR sigue
debiendo mandarlo, se valida a mano en el controller si es null en ese
caso).

### A.2 — `idLesion` en la respuesta de evaluación diaria

`EvaluacionDtos.JugadorEvaluableResponse` gana un campo:

```java
public record JugadorEvaluableResponse(
        Long idEstudiante, String nombreCompleto, String categoria,
        Long idPosicion, String posicion, String estadoAsistencia,
        Map<String, BigDecimal> puntajes, boolean precargado,
        boolean lesionado,
        Long idLesion,   // nuevo: null si no esta lesionado
        boolean puedeEvaluarse, String motivoBloqueo
) {}
```

`LesionRepository` gana una consulta que trae el par
estudiante→lesión activa en vez de solo el id de estudiante:

```java
@Query("SELECT l.estudiante.idEstudiante, l.idLesion FROM Lesion l WHERE l.fechaAlta IS NULL")
List<Object[]> idsYLesionActivaPorEstudiante();
```

`EvaluacionDiariaService.abrir()` construye un
`Map<Long, Long> lesionActivaPorEstudiante` a partir de esa consulta
(reemplaza al `Set<Long> lesionados` actual) y `construirJugador()`
saca de ahí tanto el booleano como el id. `idsEstudiantesLesionados()`
(el método viejo, usado por `PlantillaService` para excluir jugadores
lesionados de la alineación) no se toca.

### B.1 — `GET /api/estudiante/mi-informe`

`InformeService.informeDe(String username, Long idEstudiante)` se
refactoriza: la construcción del DTO a partir de un `Estudiante` ya
resuelto se extrae a un método privado `construirInforme(Estudiante
estudiante)` dentro de la misma clase, reutilizado por:

- El método existente (resuelve `Estudiante` vía el vínculo
  representante-estudiante, sin cambios de comportamiento).
- Un método nuevo `miInforme(String username)` que resuelve el
  `Estudiante` vía `estudianteRepository.findByUsuario_Username(username)`
  (404 uniforme "No hay un estudiante asociado a esta cuenta" si no
  existe, mismo criterio que `MiAsistenciaController`).

Expuesto en un controller nuevo (ver B.3).

### B.2 — `GET /api/estudiante/mi-equipo`

Servicio nuevo `MiEquipoService`:

```java
public record CategoriaDetalleResponse(String nombre, Integer edadMin, Integer edadMax, String descripcion) {}
public record PosicionResponse(String nombre, String abreviatura) {}
public record EntrenadorAsignadoResponse(String nombre, String especialidad) {}
public record CompaneroResponse(Long idEstudiante, String nombre, String posicion) {}

public record MiEquipoResponse(
        CategoriaDetalleResponse categoria,
        PosicionResponse posicion,              // null si no tiene asignada
        EntrenadorAsignadoResponse entrenador,   // null si no hay sesion futura
        List<CompaneroResponse> companeros       // sin el estudiante mismo
) {}
```

Repositorios nuevos:

```java
// EstudianteRepository
List<Estudiante> findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(Long idCategoria, Long idEstudiante);

// SesionEntrenamientoRepository
List<SesionEntrenamiento> findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
        Long idCategoria, LocalDate fecha, Pageable pageable);
```

`miEquipo(String username)`: resuelve `Estudiante` (mismo 404 que
arriba), arma `categoria` y `posicion` (null-safe) directo de la
entidad, pide la primera fila de la consulta de sesión próxima
(`PageRequest.of(0, 1)`, `fecha >= hoy en Zonas.ECUADOR`) para
`entrenador` (null si la lista viene vacía), y mapea `companeros` desde
el repositorio nuevo.

### B.3 — Controller

```java
@RestController
@RequestMapping("/api/estudiante")
@PreAuthorize("hasRole('ESTUDIANTE')")
@RequiredArgsConstructor
public class MiEquipoController {

    private final InformeService informeService; // el mismo que ya usa el representante
    private final MiEquipoService miEquipoService;

    @GetMapping("/mi-informe")
    @Transactional(readOnly = true)
    public ResponseEntity<InformeEstudianteResponse> miInforme() {
        return ResponseEntity.ok(informeService.miInforme(usernameAutenticado()));
    }

    @GetMapping("/mi-equipo")
    @Transactional(readOnly = true)
    public ResponseEntity<MiEquipoResponse> miEquipo() {
        return ResponseEntity.ok(miEquipoService.miEquipo(usernameAutenticado()));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

(`InformeDtos.InformeEstudianteResponse` y `PromedioCriterioResponse` /
`LesionResumenResponse` se reutilizan tal cual — viven en el paquete
`representante.dto` pero no hay problema en importarlos desde
`estudiante`, igual que ya se reutiliza `SesionHoy` entre dashboard y
recepción en el frontend.)

## Frontend

### A — Evaluación diaria

`evaluacion.service.ts`: dos métodos nuevos.

```ts
registrarLesion(idEstudiante: number, descripcion: string, fechaEstimadaRetorno?: string) {
  return this.http.post<LesionResponse>('/api/lesiones', { idEstudiante, descripcion, fechaEstimadaRetorno });
}
darDeAltaLesion(idLesion: number) {
  return this.http.post<LesionResponse>(`/api/lesiones/${idLesion}/alta`, {});
}
```

`evaluacion-diaria.component.ts`: junto al badge "Lesionado" por
jugador —
- No lesionado → botón "Marcar lesión" abre un formulario chico
  inline (textarea descripción, requerido; input date fecha estimada
  de retorno, opcional) con botones Guardar/Cancelar.
- Lesionado (usa `j.idLesion`) → botón "Dar de alta" que actúa directo
  al click, sin diálogo de confirmación: el proyecto no usa
  `confirm()` en ninguna pantalla (verificado — ni "Baja" de persona,
  ni "Desactivar cuenta", ni "Devuelto"/"Perdido" de inventario lo
  usan), así que introducir uno aquí rompería la convención en vez de
  seguirla.

Al guardar, se actualiza el jugador en el signal local (mismo patrón
que el autoguardado de puntajes) para no recargar toda la pantalla.

### B — Pantalla "Mi equipo"

Archivos `mi-equipo.*` junto a los que ya existen en
`features/estudiante/` (`marcar-asistencia.*`, `mi-historial.*`), sin
subcarpeta propia — mismo patrón plano que ya usa ese directorio, no
uno nuevo:

- `mi-equipo.models.ts`, `mi-equipo.service.ts` (dos llamadas: GET
  mi-informe, GET mi-equipo, en paralelo con `forkJoin` o dos
  `subscribe` independientes — igual que `representante.component.ts`
  ya hace con informe + notificaciones).
- `mi-equipo.component.ts`: layout con tarjetas —
  - Categoría (nombre, rango de edad, descripción).
  - Mi posición (o "Sin posición asignada todavía").
  - Mi entrenador (o "Sin sesiones programadas todavía").
  - Estadísticas: mismo layout visual que ya usa
    `representante.component.ts` para el informe (promedio por
    criterio + badge de % asistencia + historial de lesiones) —
    reutilizar esos estilos, no reinventarlos.
  - Compañeros de equipo: lista simple nombre + posición.

Ruta nueva en `app.routes.ts`: `estudiante/mi-equipo`, con
`roleGuard(['ESTUDIANTE'])`, igual que las otras dos rutas de
estudiante. Entrada nueva en `NAV_POR_ROL.ESTUDIANTE` en
`app-shell.component.ts`.

## Documentación

- `docs/requisitos/SRS.md`: verificado — hoy no existe ningún RF para
  "registrar/dar de alta una lesión" ni para "el estudiante consulta su
  propia información deportiva" (la única mención de "lesion" en todo
  el SRS es de paso, en el contexto de notificaciones al representante).
  Se agregan dos RF nuevos con el siguiente número disponible del
  catálogo, uno por cada feature de este documento.
- `docs/trazabilidad/matriz.csv`: fila nueva para cada RF agregado,
  apuntando a `LesionController`/`MiEquipoController` según corresponda,
  con `tipo_acceso` = CRUD-ORM (ninguno de los dos usa procedimiento
  almacenado).

## Pruebas

- `LesionControllerTest`: caso nuevo — una cuenta ENTRENADOR no puede
  registrar una lesión "a nombre de" otro `idEntrenador` distinto al
  suyo (el controller lo ignora y usa el propio); ADMINISTRADOR sigue
  pudiendo especificar cualquiera.
- `EvaluacionDiariaServiceTest` (si existe) o pruebas nuevas: un
  jugador lesionado trae su `idLesion`; uno sano trae `idLesion: null`.
- `InformeServiceTest` (nuevo o el que corresponda): `miInforme` con
  estudiante inexistente da 404; con estudiante real arma el mismo DTO
  que ya arma `informeDe`.
- `MiEquipoServiceTest` nuevo: sin posición asignada (`null`), sin
  sesión futura (`entrenador: null`), lista de compañeros que excluye
  al propio estudiante, categoría con datos completos.
- `MiEquipoControllerTest` nuevo: 200 con el DTO completo, 404 si la
  cuenta no tiene estudiante asociado.
