# Diccionario de datos

**Sistema:** SGED — Escuela Deportiva ProFútbol
**Motor:** PostgreSQL 16
**Origen:** migraciones Flyway `V1` a `V6` (`backend/src/main/resources/db/migration/`)

Este documento describe el esquema **realmente migrado**, generado a partir
de las migraciones versionadas. Los procedimientos almacenados se detallan
aparte en [`CATALOGO-SP.md`](CATALOGO-SP.md).

## Convenciones

- Claves primarias: `BIGSERIAL` con nombre `id_<entidad>`.
- Marcas de tiempo: `TIMESTAMPTZ` (con zona horaria), nunca `TIMESTAMP` sin zona.
- Baja lógica: columna `activo BOOLEAN NOT NULL DEFAULT TRUE`.
- Auditoría de fila: `creado_en` / `actualizado_en`, esta última mantenida
  automáticamente por disparadores (`set_actualizado_en`).
- Esquemas: `seguridad` (identidad y estudiantes) y `deportivo` (operación
  deportiva).

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
| `cedula` | VARCHAR(10) | Sí | — | Cédula de identidad. ⚠️ Ver hallazgo H-01 en `docs/etica/ETHICS.md`. |
| `correo` | VARCHAR(255) | Sí | — | Correo de contacto. |
| `telefono` | VARCHAR(10) | Sí | — | Teléfono de contacto. |
| `fecha_nacimiento` | DATE | Sí | — | Fecha de nacimiento. Determina la categoría. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |

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
Cuentas de acceso al sistema.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_usuario` | BIGSERIAL | No | PK | Identificador. |
| `id_persona` | BIGINT | Sí | FK → `personas` | Persona titular de la cuenta. |
| `id_estado_general` | BIGINT | Sí | FK → `estados_general` | Estado administrativo. |
| `username` | VARCHAR(100) | No | UNIQUE (`idx_usuarios_username`) | Nombre de usuario. |
| `password_hash` | VARCHAR(255) | No | — | Hash BCrypt coste 12. **Nunca texto plano.** |
| `activo` | BOOLEAN | No | DEFAULT TRUE, CHECK IN (TRUE, FALSE) | Baja lógica. |
| `creado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Alta del registro. |
| `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Mantenida por `trg_usuarios_actualizado_en`. |

## `seguridad.usuario_rol`
Relación N:N entre usuarios y roles.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_usuario_rol` | BIGSERIAL | No | PK | Identificador. |
| `id_usuario` | BIGINT | No | FK → `usuarios` | Usuario. |
| `id_rol` | BIGINT | No | FK → `roles` | Rol asignado. |

## `seguridad.estudiantes`
Deportistas inscritos en la escuela.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_estudiante` | BIGSERIAL | No | PK | Identificador. |
| `id_persona` | BIGINT | Sí | FK → `personas` | Datos personales. |
| `categoria` | VARCHAR(25) | Sí | Validada en la capa de aplicación con el patrón `SUB-NN` | Categoría por edad. |
| `fecha_ingreso` | TIMESTAMPTZ | Sí | — | Fecha de incorporación. |
| `id_posicion` | BIGINT | Sí | FK → `deportivo.posiciones` | Posición habitual (agregada en `V3`). |
| `rfid_codigo` | VARCHAR(100) | Sí | UNIQUE parcial (`WHERE rfid_codigo IS NOT NULL`) | Credencial física para marcaje de asistencia. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica (RF-13). |
| `creado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Alta del registro. |
| `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Mantenida por `trg_estudiantes_actualizado_en`. |

> El índice único sobre `rfid_codigo` es **parcial**: permite muchos
> estudiantes sin credencial RFID (`NULL`) pero impide que dos compartan el
> mismo código.

---

# Esquema `deportivo`

## `deportivo.posiciones`
Catálogo de posiciones de juego. Sembrado con 10 posiciones (POR, DFC, LD,
LI, MCD, MC, MP, ED, EI, DC).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_posicion` | BIGSERIAL | No | PK | Identificador. |
| `nombre` | VARCHAR(50) | No | UNIQUE | Nombre de la posición. |
| `abreviatura` | VARCHAR(5) | Sí | — | Sigla (p. ej. `DC`). |
| `descripcion` | VARCHAR(255) | Sí | — | Descripción funcional. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |

## `deportivo.entrenadores`

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_entrenador` | BIGSERIAL | No | PK | Identificador. |
| `id_persona` | BIGINT | No | FK → `seguridad.personas`, UNIQUE (`idx_entrenadores_persona`) | Persona. Una persona no puede ser dos entrenadores. |
| `especialidad` | VARCHAR(100) | Sí | — | Especialidad. |
| `fecha_contratacion` | DATE | Sí | — | Fecha de contratación. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

## `deportivo.horarios_entrenamiento`
Horarios semanales recurrentes (RF-17).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_horario` | BIGSERIAL | No | PK | Identificador. |
| `id_entrenador` | BIGINT | No | FK → `entrenadores` | Responsable. |
| `categoria` | VARCHAR(25) | No | — | Categoría atendida. |
| `dia_semana` | SMALLINT | No | **CHECK BETWEEN 1 AND 7** | Día (1 = lunes). |
| `hora_inicio` | TIME | No | — | Hora de inicio. |
| `hora_fin` | TIME | No | **CHECK (`hora_fin > hora_inicio`)** | Hora de fin. |
| `campo` | VARCHAR(100) | Sí | — | Campo o cancha. |
| `descripcion` | VARCHAR(255) | Sí | — | Notas. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

## `deportivo.sesiones_entrenamiento`
Instancias concretas de entrenamiento (RF-18).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_sesion` | BIGSERIAL | No | PK | Identificador. |
| `id_horario` | BIGINT | Sí | FK → `horarios_entrenamiento` | Horario del que deriva. Nulo si es sesión extraordinaria. |
| `id_entrenador` | BIGINT | No | FK → `entrenadores` | Responsable. |
| `categoria` | VARCHAR(25) | No | — | Categoría. |
| `fecha` | DATE | No | Índice `idx_sesiones_fecha` | Fecha de la sesión. |
| `hora_inicio` / `hora_fin` | TIME | Sí | — | Horario real. |
| `campo` | VARCHAR(100) | Sí | — | Ubicación. |
| `estado` | VARCHAR(20) | No | **CHECK IN (PROGRAMADA, EN_CURSO, FINALIZADA, CANCELADA)** | Estado del ciclo de vida. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

Índice compuesto: `idx_sesiones_entrenador_fecha (id_entrenador, fecha)`.

## `deportivo.asistencias`
Asistencia de un estudiante a una sesión (RF-19).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_asistencia` | BIGSERIAL | No | PK | Identificador. |
| `id_sesion` | BIGINT | No | FK → `sesiones_entrenamiento` | Sesión. |
| `id_estudiante` | BIGINT | No | FK → `seguridad.estudiantes`, índice `idx_asistencias_estudiante` | Deportista. |
| `hora_entrada` | TIME | Sí | — | Hora de marcaje. |
| `metodo` | VARCHAR(10) | No | **CHECK IN (RFID, MANUAL)**, DEFAULT MANUAL | Medio de registro. |
| `estado` | VARCHAR(15) | No | **CHECK IN (PRESENTE, TARDE, AUSENTE, JUSTIFICADO)** | Resultado. |
| `observacion` | VARCHAR(255) | Sí | — | Nota puntual. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

**Restricción de negocio:** `uq_asistencia_sesion_estudiante UNIQUE
(id_sesion, id_estudiante)` — un estudiante no puede tener dos asistencias en
la misma sesión.

> **Dato sensible:** registra la presencia física de un menor en un lugar y
> hora determinados. Ver `docs/etica/ETHICS.md` §1.

## `deportivo.criterios_evaluacion`
Criterios configurables. Sembrados: Técnica, Condición física, Táctica,
Actitud (todos con puntaje máximo 10).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_criterio` | BIGSERIAL | No | PK | Identificador. |
| `nombre` | VARCHAR(100) | No | UNIQUE | Nombre del criterio. |
| `descripcion` | VARCHAR(255) | Sí | — | Qué evalúa. |
| `puntaje_maximo` | SMALLINT | No | **CHECK (> 0)**, DEFAULT 10 | Escala. |
| `activo` | BOOLEAN | No | DEFAULT TRUE | Baja lógica. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

## `deportivo.evaluaciones_diarias`
Cabecera de la evaluación de una sesión (RF-20).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_evaluacion` | BIGSERIAL | No | PK | Identificador. |
| `id_sesion` | BIGINT | No | FK → `sesiones_entrenamiento`, **UNIQUE** (`uq_evaluacion_sesion`) | Una evaluación por sesión. |
| `id_entrenador` | BIGINT | No | FK → `entrenadores` | Evaluador. |
| `fecha` | DATE | No | Índice `idx_evaluaciones_fecha` | Fecha. |
| `observacion_general` | TEXT | Sí | — | Nota del grupo. |
| `estado` | VARCHAR(15) | No | **CHECK IN (BORRADOR, FINALIZADA)**, DEFAULT BORRADOR | Permite guardado progresivo. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

## `deportivo.detalle_evaluacion`
Puntaje por estudiante y criterio (RF-20).

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_detalle` | BIGSERIAL | No | PK | Identificador. |
| `id_evaluacion` | BIGINT | No | FK → `evaluaciones_diarias` **ON DELETE CASCADE** | Evaluación padre. |
| `id_estudiante` | BIGINT | No | FK → `seguridad.estudiantes`, índice `idx_detalle_estudiante` | Evaluado. |
| `id_criterio` | BIGINT | No | FK → `criterios_evaluacion` | Criterio aplicado. |
| `id_posicion_jugada` | BIGINT | Sí | FK → `posiciones` | Posición jugada **ese día** (puede diferir de la habitual). |
| `puntaje` | NUMERIC(4,1) | No | **CHECK (>= 0)** | Calificación. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

**Restricción de negocio:** `uq_detalle_eval_est_criterio UNIQUE
(id_evaluacion, id_estudiante, id_criterio)` — no se puede calificar dos
veces el mismo criterio al mismo estudiante en la misma evaluación.

> **Dato sensible (perfilado):** histórico de desempeño individual de un
> menor. Ver `docs/etica/ETHICS.md` §1.3.

## `deportivo.observaciones_estudiante`
Notas cualitativas individuales.

| Columna | Tipo | Nulo | Restricción | Descripción |
|---|---|---|---|---|
| `id_observacion` | BIGSERIAL | No | PK | Identificador. |
| `id_evaluacion` | BIGINT | No | FK → `evaluaciones_diarias` **ON DELETE CASCADE** | Evaluación. |
| `id_estudiante` | BIGINT | No | FK → `seguridad.estudiantes`, índice `idx_observaciones_estudiante` | Sujeto. |
| `id_entrenador` | BIGINT | No | FK → `entrenadores` | Autor. |
| `texto` | TEXT | No | — | Observación. ⚠️ Texto libre sobre un menor — ver hallazgo H-02. |
| `creado_en` / `actualizado_en` | TIMESTAMPTZ | No | DEFAULT NOW() | Auditoría de fila. |

---

# Vistas

## `deportivo.v_promedio_evaluacion`
Promedio de puntajes por estudiante y evaluación (RF-21). La agregación se
resuelve en el motor, conforme a RD-02.

| Columna | Origen |
|---|---|
| `id_evaluacion` | `detalle_evaluacion.id_evaluacion` |
| `id_estudiante` | `detalle_evaluacion.id_estudiante` |
| `fecha` | `evaluaciones_diarias.fecha` |
| `promedio` | `ROUND(AVG(puntaje), 1)` |
| `criterios_evaluados` | `COUNT(*)` |

---

# Funciones y disparadores

| Objeto | Tipo | Propósito |
|---|---|---|
| `seguridad.set_actualizado_en()` | Función de disparador | Fija `actualizado_en = NOW()` antes de cada UPDATE. |
| `deportivo.set_actualizado_en()` | Función de disparador | Equivalente para el esquema deportivo. |
| `trg_usuarios_actualizado_en` | Disparador BEFORE UPDATE | `seguridad.usuarios` |
| `trg_estudiantes_actualizado_en` | Disparador BEFORE UPDATE | `seguridad.estudiantes` |
| `trg_entrenadores_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.entrenadores` |
| `trg_horarios_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.horarios_entrenamiento` |
| `trg_sesiones_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.sesiones_entrenamiento` |
| `trg_asistencias_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.asistencias` |
| `trg_criterios_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.criterios_evaluacion` |
| `trg_evaluaciones_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.evaluaciones_diarias` |
| `trg_detalle_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.detalle_evaluacion` |
| `trg_observaciones_actualizado_en` | Disparador BEFORE UPDATE | `deportivo.observaciones_estudiante` |

# Procedimientos almacenados

Detallados en [`CATALOGO-SP.md`](CATALOGO-SP.md):

| Procedimiento | Requisito | Motivo de estar en el motor |
|---|---|---|
| `seguridad.sp_contar_estudiantes_activos` | RF-14 | Agregación `COUNT` (RD-02). |
| `seguridad.sp_desactivar_estudiantes_categoria` | RF-15 | Actualización masiva transaccional con criterio de negocio (RD-02). |

> **Nota histórica.** En la migración `V5` estos objetos se crearon como
> `FUNCTION`. La migración `V6` los convierte en `PROCEDURE` reales porque
> `@Procedure` de Spring Data invoca vía `CallableStatement` con sintaxis
> `{call ...}`, y PostgreSQL solo acepta `CALL` contra procedimientos, no
> contra funciones.

---

# Historial de migraciones

| Versión | Archivo | Contenido |
|---|---|---|
| V1 | `V1__schema_inicial.sql` | Esquemas `seguridad` y `deportivo`; tablas de identidad y acceso. |
| V2 | `V2__estudiantes.sql` | Tabla de estudiantes. |
| V3 | `V3__dominio_deportivo.sql` | Posiciones, entrenadores, horarios, sesiones, asistencias; amplía estudiantes con posición y RFID. |
| V4 | `V4__evaluaciones.sql` | Criterios, evaluaciones diarias, detalle, observaciones y vista de promedios. |
| V5 | `V5__procedimientos_almacenados.sql` | Primeras funciones de agregación y baja masiva. |
| V6 | `V6__convertir_a_procedimientos_almacenados.sql` | Conversión de esas funciones a procedimientos reales invocables desde JPA. |

El esquema **no** se genera por el ORM: `ddl-auto` está en `validate`
(RNF-11), de modo que la aplicación falla al arrancar si el esquema real no
coincide con las entidades.
