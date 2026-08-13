# Diseño: Validación de rol en Entrenador/Representante y edición de Usuario

**Fecha:** 2026-08-12
**Estado:** Aprobado para implementación

## Contexto

Auditoría de código encontró dos huecos reales:

1. `EntrenadorService.crear()` y `RepresentanteService.crear()` verifican
   que la `Persona`/`Usuario` no estén duplicados, pero **nunca**
   verifican que el `Usuario` vinculado tenga el rol correspondiente
   (`ENTRENADOR`/`REPRESENTANTE`). Hoy se puede registrar como
   Entrenador a un Usuario con rol RECEPCIONISTA, ADMINISTRADOR o
   cualquier otro.
2. `UsuarioService.editar()` actualiza `username` y re-hashea
   `password` correctamente, pero **nunca lee `request.rol()`** — el
   propio comentario del DTO lo documenta como no soportado. Tampoco
   existe ninguna UI en el frontend para editar un Usuario ya creado
   (solo alta y "Desactivar cuenta").

## Alcance

Incluido:
- Validación de rol al crear Entrenador y Representante (backend).
- `UsuarioService.editar()` gana soporte real para cambiar `rol` y
  `username`, con `password` opcional (en blanco = no se cambia).
- Guarda de integridad: no se puede cambiar el rol de un Usuario que
  tiene un Entrenador o Representante **activo** — evita dejar esos
  registros apuntando a una cuenta con un rol distinto al que
  declaran.
- Frontend: botón "Editar cuenta" en la sección Usuario del panel de
  detalle de Personas, que despliega el formulario de alta precargado.

Explícitamente fuera de alcance:
- Multi-rol por cuenta (`Usuario.roles` sigue siendo un `Set` de
  tamaño 1, mismo criterio que el resto del proyecto).
- Cambiar la validación de duplicado existente en
  `crear()` (`existsByPersona_IdPersona`/`existsByUsuario_IdUsuario`,
  sin filtro `activo`) — no forma parte de este bug.
- Edición de Estudiante/Entrenador/Representante desde este mismo
  flujo — ya existe y no cambia.

## Backend

### `EntrenadorService.crear()` / `RepresentanteService.crear()`

Después de cargar `usuario` (ya viene con `roles` en `FetchType.EAGER`,
sin query extra):

```java
boolean tieneRolEntrenador = usuario.getRoles().stream()
        .anyMatch(r -> "ENTRENADOR".equals(r.getNombre()));
if (!tieneRolEntrenador) {
    throw new IllegalArgumentException(
        "El usuario debe tener el rol ENTRENADOR para registrarse como entrenador");
}
```

Mismo patrón en `RepresentanteService.crear()` comparando contra
`"REPRESENTANTE"`. `editar()` en ambos servicios no toca `usuario`, así
que no necesita este chequeo.

### `EntrenadorRepository` / `RepresentanteRepository`

Nuevo método en ambos (para la guarda de `UsuarioService.editar()`):

```java
boolean existsByUsuario_IdUsuarioAndActivoTrue(Long idUsuario);
```

### `UsuarioRequest`

`password` pierde `@NotBlank`, conserva `@Size(min = 6, ...)` (null-safe
— no dispara si el valor es `null`).

### `UsuarioService`

- `crear()`: agrega el chequeo explícito que antes daba `@NotBlank`:

```java
if (request.password() == null || request.password().isBlank()) {
    throw new IllegalArgumentException("La contraseña es obligatoria");
}
```

- `editar()`, agrega, después de aplicar `username`:

```java
if (request.password() != null && !request.password().isBlank()) {
    usuario.setPassword_Hash(passwordEncoder.encode(request.password()));
}

if (request.rol() != null) {
    String rolActual = usuario.getRoles().stream().findFirst().map(Rol::getNombre).orElse(null);
    if (!request.rol().equals(rolActual)) {
        boolean vinculado = entrenadorRepository.existsByUsuario_IdUsuarioAndActivoTrue(usuario.getIdUsuario())
                || representanteRepository.existsByUsuario_IdUsuarioAndActivoTrue(usuario.getIdUsuario());
        if (vinculado) {
            throw new IllegalArgumentException(
                "No se puede cambiar el rol: el usuario tiene un registro de Entrenador o Representante activo");
        }
        usuario.setRoles(Set.of(buscarRol(request.rol())));
    }
}
```

Nuevas dependencias inyectadas en `UsuarioService`:
`EntrenadorRepository`, `RepresentanteRepository` (mismo estilo de
inyección directa de repositorios entre paquetes que ya usa
`EntrenadorService`/`RepresentanteService` hacia `UsuarioRepository`,
en la dirección opuesta).

## Frontend

- `personas.models.ts`: `UsuarioRequest.password` pasa de `string` a
  `string | null`.
- `personas-admin.component.ts`, sección "Cuenta de usuario":
  - Nuevo signal `editandoUsuario = signal(false)`.
  - Botón "Editar cuenta" (junto a "Desactivar cuenta", solo si
    `u.activo`) que llama `iniciarEdicionUsuario(u)`: precarga
    `formUsuario = { username: u.username, password: '', rol: u.roles[0] as RolUsuario }`
    y pone `editandoUsuario.set(true)`.
  - Mientras `editandoUsuario()` es `true`, se muestra el mismo bloque
    de formulario que hoy usa el alta (usuario/contraseña/rol), con el
    campo contraseña con `placeholder="Dejar en blanco para no cambiarla"`
    y sin `required`. Botones "Guardar" (llama `guardarEdicionUsuario()`)
    y "Cancelar" (`editandoUsuario.set(false)`).
  - `guardarEdicionUsuario()`: llama
    `servicio.editarUsuario(u.idUsuario, { idPersona, idEstadoGeneral: u.idEstadoGeneral, username, password: password || null, rol })`
    (ya existe `editarUsuario()` en `personas.service.ts`, sin cambios
    ahí). Al éxito, `editandoUsuario.set(false)` y `cargarPersonas(true)`.
  - Errores (incluido el 400 de "no se puede cambiar el rol") se
    muestran con el mismo patrón `errorUsuario`/`manejarError` que ya
    usa el resto del formulario.

## Pruebas

- `EntrenadorServiceTest`: nuevo caso — `crear` con Usuario sin rol
  ENTRENADOR lanza `IllegalArgumentException`. Ajustar el `usuario()`
  helper existente para que por defecto tenga rol ENTRENADOR (así los
  casos ya existentes de `crear` válido siguen pasando).
- `RepresentanteServiceTest` (si no existe, revisar; si existe,
  extender): mismo caso con rol REPRESENTANTE.
- `UsuarioServiceTest`: casos nuevos — `editar` cambia el rol cuando no
  hay Entrenador/Representante activo; `editar` rechaza el cambio de
  rol cuando sí lo hay; `editar` con `password` en blanco no toca el
  hash existente; `editar` con `password` nuevo lo re-hashea; `crear`
  sin `password` lanza error.
