# Diagrama de clases

**Sistema:** SGED — Escuela Deportiva ProFútbol
**Notación:** UML, Mermaid `classDiagram` (misma notación que
`docs/requisitos/casos-uso.md`, versionable como texto y revisable en un
pull request).

Resuelve D-01 (informe de evaluación de calidad): el repositorio
documentaba la arquitectura con el modelo C4 y el MER, pero ninguno de los
dos sustituye a un diagrama de clases — el nivel L3 de C4 describe
componentes de despliegue (controlador/servicio/repositorio como bloques),
no la estructura estática con atributos, operaciones y multiplicidades que
pide esta vista.

## Alcance

Cubre los agregados mínimos de los cuatro dominios que pide la guía:
**Persona, Usuario y Rol** (seguridad); **Estudiante, Representante y
Pago** (académico); **Entrenador, Categoria, SesionEntrenamiento,
Asistencia y EvaluacionDiaria** (deportivo); **Articulo, MovimientoStock y
Asignacion** (inventario). Se agregan **RepresentanteEstudiante** (la
clase de asociación real entre Representante y Estudiante — sin ella la
relación \*-a-\* no se puede dibujar con fidelidad) y
**EvaluacionEstudiante** (el detalle por jugador dentro de una
EvaluacionDiaria).

Quedan fuera, a propósito, los catálogos de apoyo (`EstadoGeneral`,
`Posicion`, `Especialidad`, `Horario`, `Lesion`, `Auditoria`,
`Consentimiento`, `Notificacion`, `CriterioEvaluacion`,
`DetalleEvaluacion`): añadirlos no cambia la estructura del dominio y
harían el diagrama ilegible. Están documentados en
`docs/basedatos/DATA-DICTIONARY.md`.

Las clases son entidades JPA: cada atributo privado ya tiene su
getter/setter público generado por Lombok (`@Getter @Setter`), así que
listarlos uno a uno no aportaría información — no se muestran. Las únicas
operaciones que se listan son las dos que sí tienen comportamiento propio
más allá de acceso a datos: `Asistencia.habilitaEvaluacion()` y
`EvaluacionDiaria.estaFinalizada()`.

## Diagrama

```mermaid
classDiagram
    direction LR

    %% ===== Dominio: seguridad =====
    class Persona {
        -Long idPersona
        -String nombre
        -String apellido
        -String cedula
        -String correo
        -String telefono
        -LocalDate fechaNacimiento
        -Boolean activo
    }

    class Usuario {
        -Long idUsuario
        -String username
        -String passwordHash
        -OffsetDateTime ultimoAcceso
        -Boolean activo
    }

    class Rol {
        -Long idRol
        -String nombre
        -String descripcion
    }

    %% ===== Dominio: academico =====
    class Estudiante {
        -Long idEstudiante
        -String codigoEstudiante
        -LocalDate fechaIngreso
        -BigDecimal peso
        -BigDecimal altura
        -Boolean activo
    }

    class Representante {
        -Long idRepresentante
        -String parentesco
        -String telefonoContacto
        -Boolean activo
    }

    class RepresentanteEstudiante {
        -Long idRepresentanteEstudiante
        -String relacion
        -Boolean contactoPrincipal
        -Boolean activo
    }

    class Pago {
        -Long idPago
        -TipoPago tipo
        -Short anio
        -Short mes
        -BigDecimal monto
        -LocalDate fechaPago
    }

    %% ===== Dominio: deportivo =====
    class Entrenador {
        -Long idEntrenador
        -Short experienciaAnios
        -String certificacion
        -Boolean activo
    }

    class Categoria {
        -Long idCategoria
        -String nombre
        -Short edadMin
        -Short edadMax
        -Boolean activo
    }

    class SesionEntrenamiento {
        -Long idSesion
        -LocalDate fecha
        -LocalTime horaInicio
        -LocalTime horaFin
        -String campo
        -String estado
    }

    class Asistencia {
        -Long idAsistencia
        -LocalTime horaEntrada
        -String metodo
        -String estado
        -String observacion
        +boolean habilitaEvaluacion()
    }

    class EvaluacionDiaria {
        -Long idEvaluacion
        -LocalDate fecha
        -String observacionGeneral
        -String estado
        +boolean estaFinalizada()
    }

    class EvaluacionEstudiante {
        -Long idEvaluacionEstudiante
    }

    %% ===== Dominio: inventario =====
    class Articulo {
        -Long idArticulo
        -String nombre
        -TipoArticulo tipo
        -String talla
        -Integer stockActual
        -Integer stockMinimo
        -String unidadMedida
        -Boolean activo
    }

    class MovimientoStock {
        -Long idMovimiento
        -TipoMovimiento tipoMovimiento
        -Integer cantidad
        -String motivo
        -Instant fechaMovimiento
    }

    class Asignacion {
        -Long idAsignacion
        -Integer cantidad
        -TipoDestinatario tipoDestinatario
        -LocalDate fechaAsignacion
        -LocalDate fechaDevolucionEsperada
        -LocalDate fechaDevolucionReal
        -EstadoAsignacion estado
        -String observaciones
    }

    %% ===== Relaciones: seguridad =====
    Persona "1" -- "0..1" Usuario : tiene cuenta
    Usuario "*" -- "*" Rol : usuario_rol

    %% ===== Relaciones: academico =====
    Persona "1" -- "0..1" Estudiante : es
    Categoria "1" -- "0..*" Estudiante : agrupa
    Estudiante "1" -- "0..1" Usuario : accede como
    Estudiante "1" -- "0..*" Pago : genera
    Usuario "1" -- "0..*" Pago : registra

    Persona "1" -- "1" Representante : es
    Usuario "1" -- "1" Representante : accede como
    Representante "1" -- "0..*" RepresentanteEstudiante : vincula
    Estudiante "1" -- "0..*" RepresentanteEstudiante : es representado en

    %% ===== Relaciones: deportivo =====
    Persona "1" -- "1" Entrenador : es
    Usuario "1" -- "1" Entrenador : accede como
    Entrenador "1" -- "0..*" SesionEntrenamiento : dirige
    Categoria "1" -- "0..*" SesionEntrenamiento : convoca

    SesionEntrenamiento "1" -- "0..*" Asistencia : registra
    Estudiante "1" -- "0..*" Asistencia : marca

    SesionEntrenamiento "1" -- "0..1" EvaluacionDiaria : tiene
    Entrenador "1" -- "0..*" EvaluacionDiaria : califica
    EvaluacionDiaria "1" *-- "0..*" EvaluacionEstudiante : contiene
    Estudiante "1" -- "0..*" EvaluacionEstudiante : es calificado en
    Categoria "1" -- "0..*" EvaluacionEstudiante : categoriaDia

    %% ===== Relaciones: inventario =====
    Articulo "1" -- "0..*" MovimientoStock : mueve stock
    Usuario "1" -- "0..*" MovimientoStock : registra
    Articulo "1" -- "0..*" Asignacion : se asigna
    Estudiante "0..1" -- "0..*" Asignacion : recibe
    Entrenador "0..1" -- "0..*" Asignacion : recibe
    Usuario "1" -- "0..*" Asignacion : registra
```

## Notas de fidelidad

- **Enumeraciones.** `Pago.tipo` (`MEMBRESIA`/`DIARIO`), `Articulo.tipo`
  (`UNIFORME`/`BALON`/`IMPLEMENTO`/`OTRO`),
  `MovimientoStock.tipoMovimiento` (`ENTRADA`/`SALIDA`/`AJUSTE`),
  `Asignacion.tipoDestinatario` (`ESTUDIANTE`/`ENTRENADOR`) y
  `Asignacion.estado` (`ASIGNADO`/`DEVUELTO`/`PERDIDO`) son enums Java
  (`@Enumerated(EnumType.STRING)`), no texto libre.
- **`Persona`–`Usuario` y `Persona`–`Estudiante` son `0..1` por regla de
  negocio, no por restricción de base de datos.** El código JPA declara
  ambas relaciones como `@ManyToOne` simple, sin `unique = true`; es
  `UsuarioService`/`EstudianteService` quien impide en tiempo de ejecución
  que una persona tenga más de una cuenta o ficha de estudiante activa a
  la vez (ver `validarRolCoherente`, `validarCoherenciaConFichaEstudiante`).
  Distinto de `Entrenador`/`Representante`, donde el `1..1` con Persona y
  Usuario sí lo impone la columna `unique = true` de la migración.
- **`Asignacion` es XOR, no una relación libre con ambas puntas.** Exactamente
  una de `estudiante`/`entrenador` va poblada según `tipoDestinatario`; la
  base de datos lo exige con un `CHECK` (migración V15), no el modelo de
  objetos.
