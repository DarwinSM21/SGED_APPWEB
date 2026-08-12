# Diseño: Pantalla unificada de Personas (Persona / Usuario / Estudiante)

**Fecha:** 2026-08-12
**Estado:** Aprobado para implementación

## Contexto

`Persona` es la entidad raíz del esquema `seguridad`: tanto `Usuario`
(cuenta de acceso) como `Estudiante`, `Entrenador` y `Representante`
cuelgan de ella por FK. Hoy esa relación no se refleja en el frontend:

- `/estudiantes/registrar` encadena `POST /api/personas` →
  `POST /api/estudiantes`, sin tocar `Usuario`.
- `/admin/crear-usuario` encadena `POST /api/auth/registro` (que
  **siempre** crea una `Persona` nueva) → opcionalmente
  `POST /api/representantes`, sin relación con `Estudiante`.

Dos pantallas separadas para una misma raíz de datos, cada una creando
su propia `Persona` sin saber de la otra. Este documento reemplaza
ambas por una sola.

## Alcance

Incluido:
- Una pantalla nueva (`/personas`) que reemplaza `/estudiantes/registrar`
  y `/admin/crear-usuario`, con dos modos según rol:
  - **ADMINISTRADOR**: maestro-detalle completo (Persona + Usuario +
    Estudiante + Entrenador + Representante).
  - **RECEPCIONISTA**: alta rápida (Persona + Estudiante, con acciones
    opcionales para vincular Representante y habilitar acceso de
    Estudiante) — sin la tabla completa de personas.
- Backend: `UsuarioRequest` acepta un `rol` opcional y lo asigna al
  crear (generaliza el patrón que hoy solo existe en
  `AuthController.registro`, pero sin forzar una `Persona` nueva).
- Apertura de permisos puntual a RECEPCIONISTA en 3 endpoints ya
  existentes (ver tabla de permisos).

Explícitamente fuera de alcance:
- Endpoint agregador de "resumen de persona" — a esta escala de datos,
  la vista de ADMINISTRADOR cruza en el cliente las listas que ya
  existen (`/api/personas`, `/api/usuarios`, `/api/estudiantes`,
  `/api/entrenadores`, `/api/representantes`).
- Cambios a la creación de Entrenador: sigue exactamente igual
  (`POST /api/entrenadores` con `idPersona`+`idUsuario` ya existentes),
  solo se invoca desde la pantalla nueva en vez de una aparte.
- Cualquier cambio a `Usuario`→múltiples roles simultáneos: sigue
  siendo un rol por cuenta, como hoy.

## Backend

### `UsuarioRequest` — agregar `rol`

```java
public record UsuarioRequest(
        @NotNull Long idPersona,
        @NotNull Long idEstadoGeneral,
        @NotBlank @Size(min = 4, max = 50) String username,
        @NotBlank @Size(min = 6) String password,
        String rol   // nuevo, opcional: si viene, se valida y asigna
) {}
```

`UsuarioService.crear`: si `request.rol()` no es null, busca el `Rol`
por nombre (`RolRepository.findByNombre`, 400 si no existe — mismo
criterio que `AuthController.registro`) y lo asigna en
`usuario.setRoles(Set.of(rol))`. Si es null, el usuario queda sin rol
(caso ya existente: edición de usuarios que no toca el rol).

Con esto, el flujo genérico para darle acceso a una Persona existente
con cualquier rol pasa a ser un solo `POST /api/usuarios`, sin
depender de `/api/auth/registro` (que crea Persona nueva) ni de
duplicar lógica de asignación de rol.

### Permisos — abrir a RECEPCIONISTA

| Endpoint | Antes | Ahora |
|---|---|---|
| `POST /api/estudiantes/{id}/acceso` | ADMINISTRADOR | ADMINISTRADOR, RECEPCIONISTA |
| `POST /api/representantes` | ADMINISTRADOR | ADMINISTRADOR, RECEPCIONISTA |
| `POST /api/representantes/{id}/estudiantes/{idEstudiante}` | ADMINISTRADOR | ADMINISTRADOR, RECEPCIONISTA |
| `GET /api/representantes`, `GET /api/representantes/{id}` | ADMINISTRADOR | ADMINISTRADOR, RECEPCIONISTA |

Sin cambios: `POST /api/usuarios` genérico (con `rol` arbitrario),
`PUT`/`DELETE /api/personas/{id}`, todo `EntrenadorController`,
`PUT`/`DELETE /api/representantes/{id}` — siguen exclusivos de
ADMINISTRADOR.

## Frontend

Un componente nuevo, `frontend/src/app/features/personas/`, montado en
una única ruta `/personas` con `roleGuard(['ADMINISTRADOR',
'RECEPCIONISTA'])`; el propio componente decide qué renderizar según
`AuthService.currentUser().rol`.

### Modo ADMINISTRADOR — maestro-detalle

- **Lista** (izquierda o superior): personas con buscador (nombre/cédula),
  cada fila con badges de estado: tiene cuenta (rol), es estudiante, es
  entrenador, es representante. Construidos cruzando en el cliente
  `/api/personas`, `/api/usuarios`, `/api/estudiantes`,
  `/api/entrenadores`, `/api/representantes` por `idPersona`.
- **Detalle** (al seleccionar una persona, o "+ Nueva persona"):
  - Datos de Persona (crear/editar).
  - Sección Usuario: si existe, username/rol/estado + editar/desactivar;
    si no, formulario para crear (`POST /api/usuarios` con `rol`).
  - Sección Estudiante: si existe, campos editables (categoría, código,
    peso, altura); si no, formulario de alta.
  - Sección Entrenador: si existe, especialidad/experiencia/certificación
    editables; si no y la persona tiene Usuario, formulario de alta
    (`POST /api/entrenadores`).
  - Sección Representante: si existe, parentesco/teléfono + gestión de
    vínculos con estudiantes; si no y la persona tiene Usuario,
    formulario de alta (`POST /api/representantes`).

### Modo RECEPCIONISTA — alta rápida

Mismo formulario que hoy `RegistrarEstudianteComponent` (Persona +
Estudiante en un solo submit), más dos acciones que aparecen después de
guardar un estudiante:
- **"Vincular representante"** → mini-formulario (nombre, cédula, correo,
  parentesco, teléfono, usuario/contraseña) que crea Persona nueva +
  Usuario con rol REPRESENTANTE + `POST /api/representantes` vinculado
  al estudiante recién creado.
- **"Habilitar acceso del estudiante"** → usuario/contraseña, llama
  `POST /api/estudiantes/{id}/acceso`.

También lista (sin buscador global) los estudiantes que esa sesión fue
registrando, igual que hoy.

## Documentación

- `docs/requisitos/SRS.md`: no se agregan RF nuevos (RF-23/24/25 ya
  cubren Categoría/Usuario/Persona como recursos CRUD) — se actualiza
  la nota de "Origen" de cada uno para apuntar a la pantalla unificada,
  y se documenta el campo `rol` nuevo en `UsuarioRequest`.
- `docs/trazabilidad/matriz.csv`: actualizar la fila de endpoint/archivo
  de implementación para RF-23 (Usuario) si cambia el archivo de origen.

## Pruebas

- `UsuarioServiceTest`: nuevo caso — `crear` con `rol` asigna el rol;
  `crear` sin `rol` no asigna ninguno (compatibilidad); `rol` inexistente
  lanza error 400.
- Pruebas de permisos existentes (`RepresentanteControllerTest` si
  existe, o nuevas) para confirmar que RECEPCIONISTA ahora puede crear/
  vincular representantes y ADMINISTRADOR sigue pudiendo todo.
