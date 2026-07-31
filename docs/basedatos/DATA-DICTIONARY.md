# Diccionario de datos

**Sistema:** SGED — Escuela Deportiva ProFútbol
**Motor:** PostgreSQL 16
**Origen:** `db/schema.sql` (esquema consolidado, generado a partir de las
migraciones Flyway `V1`–`V6`), verificado contra las entidades JPA reales
tras la reestructuración de paquetes (`academico`/`deportivo`/`seguridad`).

Este documento describe el esquema **realmente migrado**. Los
procedimientos almacenados se detallan aparte en
[`CATALOGO-SP.md`](CATALOGO-SP.md).

> No confundir con [`docs/mediciones/DATA-DICTIONARY.md`](../mediciones/DATA-DICTIONARY.md),
> que documenta las variables de los **archivos de medición** (k6, OWASP,
> etc.), no el esquema de base de datos. Alcance distinto, no es un
> duplicado.

**Revisión 2026-07-30:** el equipo reestructuró los paquetes del backend en
tres dominios (`academico`, `deportivo`, `seguridad`) y normalizó la
categoría de estudiante (antes texto libre `VARCHAR`, ahora una entidad con
catálogo). Esta versión del documento refleja ese esquema.

## Convenciones

- Claves primarias: `BIGSERIAL` con nombre `id_<entidad>`.
- Marcas de tiempo: `TIMESTAMPTZ` con zona horaria; los nombres de columna
  varían entre `created_at`/`updated_at` (tablas nuevas de esta entrega) y
  `creado_en`/`actualizado_en` (tablas heredadas de `V3`/`V4`) — inconsistencia
  real, no un error de este documento.
- Baja lógica: columna `activo BOOLEAN NOT NULL DEFAULT TRUE`.
- Esquemas: `seguridad` (identidad y acceso), `academico` (estudiantes),
  `deportivo` (operación deportiva).

---

# Esquema `seguridad`

## `seguridad.estados_general`
Catálogo de estados genéricos reutilizable.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_estado_general` | BIGSERIAL | No | PK | Identificador. |
| `nombre` | VARCHAR(100) | No | — | Nombre del estado. |

## `seguridad.personas`
Datos personales base. Compartida por usuarios, estudiantes y entrenadores.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_persona` | BIGSERIAL | No | PK | Identificador. |
| `nombre` | VARCHAR(100) | No | — | Nombres. |
| `apellido` | VARCHAR(100) | No | — | Apellidos. |
| `cedula` | VARCHAR(10) | **No** | UNIQUE (`idx_personas_cedula`) | Cédula de identidad. Pasó a `NOT NULL` y única en esta entrega. ⚠️ Ver hallazgo H-01 en `docs/etica/ETHICS.md`. |
| `correo` | VARCHAR(200) | No | UNIQUE (`idx_personas_correo`) | Correo de contacto. |
| `telefono` | VARCHAR(15) | Sí | — | Teléfono de contacto. |
| `foto` | TEXT | Sí | — | **Nuevo.** Presumiblemente una URL o base64; no hay endpoint que la escriba todavía. |
| `fecha_nacimiento` | DATE | No | — | Fecha de nacimiento. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `created_at` / `updated_at` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

> **Dato personal sensible:** esta tabla concentra la identificación de
> menores de edad. Ver `docs/etica/ETHICS.md` §2.

## `seguridad.roles`
Catálogo de roles del sistema. Sembrados: `ADMINISTRADOR`, `ENTRENADOR`, `USER`.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_rol` | BIGSERIAL | No | PK | Identificador. |
| `nombre` | VARCHAR(50) | No | — | Nombre del rol. |
| `descripcion` | VARCHAR(255) | Sí | — | Descripción. |

## `seguridad.usuarios`
Cuentas de acceso al sistema. Ahora expuesta también como recurso CRUD
propio vía `UsuarioController` (`/api/usuarios`, 5 endpoints), además de
usarse internamente para login.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_usuario` | BIGSERIAL | No | PK | Identificador. |
| `id_persona` | BIGINT | No | FK → `personas` | Persona titular de la cuenta. |
| `id_estado_general` | BIGINT | No | FK → `estados_general` | Estado administrativo. |
| `username` | VARCHAR(50) | No | UNIQUE (`idx_usuarios_username`) | Nombre de usuario. |
| `password_hash` | TEXT | No | — | Hash BCrypt coste 12. **Nunca texto plano.** |
| `ultimo_acceso` | TIMESTAMPTZ | Sí | — | **Nuevo.** Marca de último login exitoso. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `created_at` / `updated_at` | TIMESTAMPTZ | No | DEFAULT NOW() | Mantenida por `trg_usuarios_updated_at`. |

## `seguridad.usuario_rol`
Relación N:N entre usuarios y roles.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_usuario_rol` | BIGSERIAL | No | PK | Identificador. |
| `id_usuario` | BIGINT | No | FK → `usuarios` | Usuario. |
| `id_rol` | BIGINT | No | FK → `roles` | Rol asignado. |

---

# Esquema `academico`

## `academico.estudiantes`
Deportistas inscritos en la escuela. **Movida de `seguridad` a `academico`**
en la reestructuración de paquetes; la categoría dejó de ser texto libre.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_estudiante` | BIGSERIAL | No | PK | Identificador. |
| `id_persona` | BIGINT | No | FK → `seguridad.personas` | Datos personales. |
| `id_categoria` | BIGINT | No | FK → `deportivo.categorias` | **Cambio de tipo:** antes `VARCHAR(25)` validado por patrón `SUB-NN` en la app; ahora clave foránea a un catálogo normalizado. RF-11 se reescribe en el SRS para reflejar esto. |
| `id_estado_general` | BIGINT | No | FK → `seguridad.estados_general` | **Nuevo.** No existía en el modelo anterior. |
| `codigo_estudiante` | VARCHAR(30) | No | UNIQUE (`idx_estudiantes_codigo`) | **Nuevo.** Código interno único, no era parte del modelo anterior. |
| `fecha_ingreso` | DATE | No | — | Fecha de incorporación. |
| `peso` | NUMERIC(5,2) | Sí | `CHECK` en DTO: `> 0`, máx. 3 enteros + 2 decimales | ⚠️ **Dato de salud de un menor. Ver hallazgo H-06 en `docs/etica/ETHICS.md` — agregado sin finalidad ni base legal documentada.** |
| `altura` | NUMERIC(5,2) | Sí | Igual que `peso` | ⚠️ Mismo hallazgo H-06. |
| `id_posicion` | BIGINT | Sí | FK → `deportivo.posiciones` | Posición habitual (aditivo desde `V3`). |
| `rfid_codigo` | VARCHAR(100) | Sí | UNIQUE parcial (`WHERE rfid_codigo IS NOT NULL`) | Credencial física para marcaje de asistencia. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica (RF-13). |
| `created_at` / `updated_at` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

> El índice único sobre `rfid_codigo` es **parcial**: permite muchos
> estudiantes sin credencial RFID (`NULL`) pero impide que dos compartan el
> mismo código.

## `academico.representantes` — ⬜ no existe todavía

`RepresentanteController`, sus DTOs y su `entity` están creados en el código
(`backend/src/main/java/org/uteq/backend/academico/representante/`) pero
**todos los archivos están vacíos** (0 bytes) — es un paquete reservado, no
una funcionalidad parcial. No hay tabla `academico.representantes` en
`db/schema.sql`. No confundir "el paquete existe" con "está implementado".

---

# Esquema `deportivo`

## `deportivo.categorias` — **nueva tabla de esta entrega**
Reemplaza el campo de texto libre `categoria` por un catálogo normalizado.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_categoria` | BIGSERIAL | No | PK | Identificador. |
| `nombre` | VARCHAR(100) | No | — | Nombre de la categoría (p. ej. "Sub-12"). |
| `edad_min` | SMALLINT | No | — | Edad mínima del rango. |
| `edad_max` | SMALLINT | No | — | Edad máxima del rango. |
| `descripcion` | VARCHAR(255) | Sí | — | Descripción libre. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `created_at` / `updated_at` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

Expuesta como recurso CRUD propio: `CategoriaController` (`/api/categorias`,
6 endpoints).

## `deportivo.posiciones`
Catálogo de posiciones de juego. Sembrado con 10 posiciones (POR, DFC, LD,
LI, MCD, MC, MP, ED, EI, DC). Sin cambios respecto a la versión anterior de
este documento.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_posicion` | BIGSERIAL | No | PK | Identificador. |
| `nombre` | VARCHAR(50) | No | UNIQUE | Nombre de la posición. |
| `abreviatura` | VARCHAR(5) | Sí | — | Sigla (p. ej. `DC`). |
| `descripcion` | VARCHAR(255) | Sí | — | Descripción funcional. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |

## `deportivo.entrenadores`
Ahora vinculada también a una cuenta de usuario, no solo a una persona.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_entrenador` | BIGSERIAL | No | PK | Identificador. |
| `id_persona` | BIGINT | No | FK → `seguridad.personas`, UNIQUE | Persona. |
| `id_usuario` | BIGINT | No | FK → `seguridad.usuarios`, UNIQUE | **Nuevo.** Cuenta de acceso asociada — antes un entrenador no tenía login propio necesariamente ligado 1:1. |
| `especialidad` | VARCHAR(150) | Sí | — | Especialidad. |
| `experiencia_anios` | SMALLINT | Sí | — | **Nuevo**, reemplaza a `fecha_contratacion`. |
| `certificacion` | VARCHAR(255) | Sí | — | **Nuevo.** |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `created_at` / `updated_at` | TIMESTAMPTZ | No | — | Auditoría de fila (vía `@PrePersist`/`@PreUpdate` en la entidad, no disparador SQL). |

Expuesta como recurso CRUD propio: `EntrenadorController` (`/api/entrenadores`,
5 endpoints).

## `deportivo.equipos` — ⬜ no existe todavía

Igual que `representantes`: `EquipoController` y sus DTOs son paquetes con
clases vacías (`EquipoController` tiene 5 líneas: el `package` y una clase
sin cuerpo). No hay tabla en `db/schema.sql`.

## `deportivo.horarios_entrenamiento`
Horarios semanales recurrentes. **Sin cambios de esquema**, pero nótese la
inconsistencia: sigue usando `categoria VARCHAR(25)` como texto libre,
**no** la nueva FK `id_categoria` que sí adoptó `academico.estudiantes`. No
es un error de este documento — es una inconsistencia real en el esquema
que vale la pena resolver antes de la Entrega Final.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_horario` | BIGSERIAL | No | PK | Identificador. |
| `id_entrenador` | BIGINT | No | FK → `entrenadores` | Responsable. |
| `categoria` | VARCHAR(25) | No | — | ⚠️ Todavía texto libre — ver nota arriba. |
| `dia_semana` | SMALLINT | No | **CHECK BETWEEN 1 AND 7** | Día (1 = lunes). |
| `hora_inicio` | TIME | No | — | Hora de inicio. |
| `hora_fin` | TIME | No | **CHECK (`hora_fin > hora_inicio`)** | Hora de fin. |
| `campo` | VARCHAR(100) | Sí | — | Campo o cancha. |
| `descripcion` | VARCHAR(255) | Sí | — | Notas. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

## `deportivo.sesiones_entrenamiento`
Instancias concretas de entrenamiento. Misma inconsistencia de `categoria`
como texto libre.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_sesion` | BIGSERIAL | No | PK | Identificador. |
| `id_horario` | BIGINT | Sí | FK → `horarios_entrenamiento` | Horario del que deriva. |
| `id_entrenador` | BIGINT | No | FK → `entrenadores` | Responsable. |
| `categoria` | VARCHAR(25) | No | — | ⚠️ Texto libre, igual que en horarios. |
| `fecha` | DATE | No | Índice `idx_sesiones_fecha` | Fecha de la sesión. |
| `hora_inicio` / `hora_fin` | TIME | Sí | — | Horario real. |
| `campo` | VARCHAR(100) | Sí | — | Ubicación. |
| `estado` | VARCHAR(20) | No | **CHECK IN (PROGRAMADA, EN_CURSO, FINALIZADA, CANCELADA)** | Estado del ciclo de vida. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

Índice compuesto: `idx_sesiones_entrenador_fecha (id_entrenador, fecha)`.

## `deportivo.asistencias`
Asistencia de un estudiante a una sesión. **FK actualizada** a
`academico.estudiantes` (antes `seguridad.estudiantes`).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_asistencia` | BIGSERIAL | No | PK | Identificador. |
| `id_sesion` | BIGINT | No | FK → `sesiones_entrenamiento` | Sesión. |
| `id_estudiante` | BIGINT | No | FK → **`academico.estudiantes`**, índice `idx_asistencias_estudiante` | Deportista. |
| `hora_entrada` | TIME | Sí | — | Hora de marcaje. |
| `metodo` | VARCHAR(10) | No | **CHECK IN (RFID, MANUAL)**, DEFAULT MANUAL | Medio de registro. |
| `estado` | VARCHAR(15) | No | **CHECK IN (PRESENTE, TARDE, AUSENTE, JUSTIFICADO)** | Resultado. |
| `observacion` | VARCHAR(255) | Sí | — | Nota puntual. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

**Restricción de negocio:** `uq_asistencia_sesion_estudiante UNIQUE
(id_sesion, id_estudiante)`.

> **Dato sensible:** registra la presencia física de un menor en un lugar y
> hora determinados. Ver `docs/etica/ETHICS.md` §1.

## `deportivo.criterios_evaluacion`, `evaluaciones_diarias`, `detalle_evaluacion`, `observaciones_estudiante`

Sin cambios de estructura respecto a la versión anterior de este documento,
salvo que las FK hacia estudiante ahora apuntan a `academico.estudiantes`
en vez de `seguridad.estudiantes`. Ver `db/schema.sql` para el detalle
completo de columnas — no se repite aquí para no duplicar contenido que no
cambió.

> **Datos sensibles (perfilado):** histórico de desempeño individual de un
> menor (`detalle_evaluacion.puntaje`) y texto libre sin control de
> contenido (`observaciones_estudiante.texto`, hallazgo H-02). Ver
> `docs/etica/ETHICS.md`.

---

# Vistas

## `deportivo.v_promedio_evaluacion`
Sin cambios. Promedio de puntajes por estudiante y evaluación, resuelto en
el motor.

| Columna | Origen |
|---|---|
| `id_evaluacion` | `detalle_evaluacion.id_evaluacion` |
| `id_estudiante` | `detalle_evaluacion.id_estudiante` |
| `fecha` | `evaluaciones_diarias.fecha` |
| `promedio` | `ROUND(AVG(puntaje), 1)` |
| `criterios_evaluados` | `COUNT(*)` |

---

# Procedimientos almacenados

Detallados en [`CATALOGO-SP.md`](CATALOGO-SP.md). **Cambio de firma en esta
entrega:**

| Procedimiento | Antes | Ahora |
|---|---|---|
| `sp_contar_estudiantes_activos` | `seguridad.sp_contar_estudiantes_activos(p_categoria VARCHAR)` | `academico.sp_contar_estudiantes_activos(p_categoria INT)` — recibe `id_categoria`, no el nombre de texto |
| `sp_desactivar_estudiantes_categoria` | `seguridad.sp_desactivar_estudiantes_categoria(p_categoria VARCHAR)` | `academico.sp_desactivar_estudiantes_categoria(p_categoria INT)` |

Ambos se movieron del esquema `seguridad` al esquema `academico`, junto con
la tabla que consultan. Motivo de estar en el motor y no en la aplicación:
sigue siendo el mismo (RD-02 del SRS — agregación y actualización masiva
transaccional).

> **Nota histórica.** En la migración `V5` original estos objetos se
> crearon como `FUNCTION`. `V6` los convierte en `PROCEDURE` reales porque
> `@Procedure` de Spring Data invoca vía `CallableStatement` con sintaxis
> `{call ...}`, y PostgreSQL solo acepta `CALL` contra procedimientos, no
> contra funciones. La versión más reciente de `V6` además migra el `DROP`
> de las funciones/procedimientos previos en `seguridad` antes de crear los
> nuevos en `academico`.

---

# Resumen de cambios de esta revisión (2026-07-30)

| Cambio | Detalle |
|---|---|
| Reestructuración de esquema | `estudiantes` se mueve de `seguridad` a `academico` |
| Normalización | `categoria` (texto libre) → `deportivo.categorias` (catálogo con FK) |
| Nuevas columnas en `estudiantes` | `id_estado_general`, `codigo_estudiante`, `peso`, `altura` |
| Nuevas tablas | `deportivo.categorias` |
| Paquetes sin tabla (stubs) | `academico.representante`, `deportivo.equipo` — clases vacías, sin migración |
| Nuevos recursos CRUD expuestos | Categoria (6 ep.), Entrenador (5 ep.), Usuario (5 ep.), Persona (6 ep.), EstadoGeneral (1 ep.) |
| Cambio de firma de procedimientos | `p_categoria` de `VARCHAR` a `INT` (`id_categoria`) |
| Inconsistencia pendiente | `horarios_entrenamiento` y `sesiones_entrenamiento` siguen con `categoria VARCHAR`, no migraron a la FK normalizada |

El esquema **no** se genera por el ORM: `ddl-auto` está en `validate`
(RNF-11), de modo que la aplicación falla al arrancar si el esquema real no
coincide con las entidades.
