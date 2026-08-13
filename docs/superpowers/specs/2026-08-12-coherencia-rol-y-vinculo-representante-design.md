# Diseño: Coherencia rol↔ficha y vínculo Representante–Estudiante

**Fecha:** 2026-08-12
**Estado:** Aprobado para implementación

Extiende `2026-08-12-validaciones-rol-usuario-design.md`, que cerró la
mitad del problema (validar el rol al crear Entrenador/Representante).
Este documento cierra la otra mitad y agrega el vínculo faltante.

## Contexto

Dos fallas encontradas probando la pantalla de Personas:

**A.** Se creó una cuenta con rol ENTRENADOR para Fernanda Castro, que
ya tenía ficha de Estudiante. Nada lo impidió. `UsuarioService.crear()`
no valida coherencia alguna, y la guarda que se agregó en `editar()`
solo miraba Entrenador/Representante (nunca Estudiante) y solo al
cambiar el rol, no al crearlo.

**B.** La sección Representante solo muestra `parentesco` y
`telefonoContacto`. No hay forma de ver ni asignar **a quién representa**
un representante. El backend ya soporta el vínculo
(`academico.representante_estudiante`, endpoints de vincular/desvincular,
`RepresentanteResponse.representados`) y `personas.service.ts` ya tiene
los métodos — pero **ningún componente los llama**.

## Alcance

Incluido:
- Regla de coherencia rol↔ficha, validada al crear y al editar Usuario,
  y al crear Estudiante.
- Corrección del dato existente de Fernanda Castro.
- Exponer `relacion` y `contactoPrincipal` del vínculo (hoy son columnas
  muertas: el backend nunca las escribe ni las devuelve).
- UI para ver, vincular y desvincular representantes de un estudiante.

Explícitamente fuera de alcance:
- Multi-rol por cuenta (`Usuario.roles` sigue siendo un `Set` de tamaño 1).
- Impedir que una Persona tenga más de un Usuario — hueco distinto, no
  es el bug reportado. (La regla de coherencia igual lo mitiga en parte:
  una segunda cuenta con rol incoherente ahora se rechaza.)
- Migración de datos masiva: solo se corrige la fila de Fernanda.

## A. Coherencia rol ↔ ficha

### La regla

> Si una Persona tiene una ficha **activa**, el rol de su Usuario debe
> ser el correspondiente a esa ficha.

| Ficha activa de la Persona | Rol permitido |
|---|---|
| Estudiante | `ESTUDIANTE` |
| Entrenador | `ENTRENADOR` |
| Representante | `REPRESENTANTE` |
| ninguna | cualquiera |

El caso "ninguna ficha → cualquier rol" no es una excepción cómoda: es
**necesario**. El flujo de alta actual crea la cuenta con rol ENTRENADOR
*primero* y la ficha de Entrenador después (el formulario de Entrenador
solo aparece cuando ya existe Usuario). Sin esa puerta no se podría
crear ningún entrenador. Como efecto colateral, `ADMINISTRADOR` y
`RECEPCIONISTA` solo son asignables a Personas sin fichas, que es lo
correcto: no tienen ficha de dominio propia.

Solo cuentan las fichas **activas**. Las tres entidades usan baja
lógica; si a alguien le dieron de baja su ficha de Entrenador, queda
libre para tomar otro rol — que es justamente para lo que sirve la baja
lógica.

### Repositorios

Método nuevo en los tres repos (`EstudianteRepository`,
`EntrenadorRepository`, `RepresentanteRepository`):

```java
boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);
```

### `UsuarioService`

Se inyecta `EstudianteRepository` (ya tiene los otros dos). Método
privado nuevo, llamado desde `crear()` y desde `editar()`:

```java
private void validarRolCoherente(Long idPersona, String rol) {
    if (estudianteRepository.existsByPersona_IdPersonaAndActivoTrue(idPersona)
            && !"ESTUDIANTE".equals(rol)) {
        throw new IllegalArgumentException(
            "La persona tiene una ficha de estudiante activa: su cuenta solo puede tener el rol ESTUDIANTE");
    }
    // idem ENTRENADOR y REPRESENTANTE
}
```

En `crear()` se llama siempre que `request.rol() != null`. En `editar()`
**reemplaza** la guarda actual (`existsByUsuario_IdUsuarioAndActivoTrue`
sobre Entrenador/Representante), que queda obsoleta por tres razones:
no cubre Estudiante, no cubre el alta, y al estar indexada por
`idUsuario` no detecta una segunda cuenta creada para la misma Persona.
Los métodos `existsByUsuario_IdUsuarioAndActivoTrue` de
`EntrenadorRepository`/`RepresentanteRepository` quedan sin uso y **se
eliminan** junto con sus mocks en los tests.

### `EstudianteService.crear()` — guarda simétrica

Ya tiene `UsuarioRepository` inyectado. Si la Persona ya tiene Usuario,
su rol debe ser `ESTUDIANTE`. `UsuarioRepository` solo expone
`existsByPersona_IdPersona`, así que hace falta agregar:

```java
Optional<Usuario> findByPersona_IdPersonaAndActivoTrue(Long idPersona);
```

`EstudianteService.habilitarAcceso()` no cambia: ya fija el rol
`ESTUDIANTE` a mano, nunca fue parte del problema.

### Dato existente

Fernanda Castro queda con ficha de Estudiante activa + cuenta con rol
ENTRENADOR. Se corrige su rol a `ESTUDIANTE` directo en la base como
parte de este cambio (`UPDATE seguridad.usuario_rol`). Sin esto,
cualquier edición futura de su cuenta sería rechazada por la regla
nueva. No se escribe migración Flyway: es una corrección puntual de un
dato de prueba, no un cambio de esquema.

## B. Vínculo Representante–Estudiante

### Backend

`RepresentanteEstudiante` ya tiene `relacion` (VARCHAR 50) y
`contactoPrincipal` (boolean NOT NULL). `RepresentanteService.vincular()`
nunca las escribe y `EstudianteVinculadoResponse` no las devuelve. Se
exponen ahora:

- DTO nuevo `VinculoRequest(String relacion, Boolean contactoPrincipal)`.
- `POST /api/representantes/{id}/estudiantes/{idEstudiante}` acepta ese
  body. Es seguro cambiar la firma: hoy **ningún** componente del
  frontend llama a ese endpoint.
- `EstudianteVinculadoResponse` pasa de
  `(idEstudiante, nombreCompleto, categoria)` a
  `(idEstudiante, nombreCompleto, categoria, relacion, contactoPrincipal)`.
- `vincular()` escribe ambos campos, tanto al crear el vínculo como al
  reactivar uno dado de baja.
- **Un solo contacto principal por estudiante**: si el vínculo entrante
  viene con `contactoPrincipal = true`, los demás vínculos activos de
  *ese estudiante* se marcan en `false`. Se resuelve con
  `findByEstudiante_IdEstudianteAndActivoTrue`, que **ya existe** en
  `RepresentanteEstudianteRepository` — no hace falta método nuevo.
- `crear()` con `idsEstudiantesIniciales` sigue vinculando sin relación
  ni contacto principal (`null` / `false`): esa lista son solo IDs y no
  se cambia su forma.

### Frontend

Todo en `personas-admin.component.ts`, sección "Ficha de estudiante".
Hoy, cuando la ficha ya existe, esa sección renderiza una sola línea de
resumen. Se le agrega debajo un bloque de representantes:

- **Lista de vinculados**: se cruza en el cliente desde el signal
  `representantes()` que el componente ya carga — los representantes
  cuyo `representados[]` incluye el `idEstudiante` de la ficha actual.
  Cada fila muestra nombre · relación · badge "Contacto principal" si
  corresponde, con un botón "Desvincular"
  (`desvincularEstudianteDeRepresentante`, ya existe en el service).
- **Alta de vínculo**: `<select>` de representantes (excluyendo los ya
  vinculados) + input de relación + checkbox "Contacto principal" +
  botón "Vincular".
- El bloque solo aparece cuando la ficha de estudiante **ya existe**: el
  endpoint necesita un `idEstudiante`, que no existe hasta después de
  crearla. Al crear una ficha nueva, primero se guarda y después se
  vinculan representantes.
- `personas.models.ts`: `EstudianteVinculado` gana `relacion` y
  `contactoPrincipal`; `vincularEstudianteARepresentante` en
  `personas.service.ts` pasa a mandar el body nuevo.

## Documentación

- `docs/basedatos/DATA-DICTIONARY.md`: `representante_estudiante` —
  marcar `relacion` y `contacto_principal` como efectivamente usadas
  (hasta ahora eran columnas muertas).
- `docs/requisitos/SRS.md`: nota en el RF de Representante sobre la
  asignación de representados desde la pantalla de Personas, y nota en
  el RF de Usuario sobre la regla de coherencia rol↔ficha.

## Pruebas

- `UsuarioServiceTest`: `crear` con rol incoherente respecto de una
  ficha activa lanza error (un caso por cada una de las tres fichas);
  `crear` con rol coherente pasa; `crear` sin fichas acepta cualquier
  rol; `editar` con rol incoherente lanza error. Se reemplazan los dos
  casos que hoy mockean `existsByUsuario_IdUsuarioAndActivoTrue`.
- `EstudianteServiceTest`: `crear` para una Persona con Usuario de rol
  distinto de ESTUDIANTE lanza error; con rol ESTUDIANTE o sin Usuario
  pasa.
- `RepresentanteServiceTest`: `vincular` guarda `relacion` y
  `contactoPrincipal`; marcar un vínculo como principal desmarca los
  otros vínculos activos del mismo estudiante.
