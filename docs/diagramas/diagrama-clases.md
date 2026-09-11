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

**Nota sobre `deportivo`:** los nombres de clase y atributo de ese
dominio (`Coach`, `Category`, `TrainingSession`, `Attendance`,
`DailyEvaluation`, `StudentEvaluation`) están traducidos al inglés en
este diagrama por completitud documental, pero el código Java real de
`deportivo` todavía usa los nombres en español (`Entrenador`, `Categoria`,
`SesionEntrenamiento`, `Asistencia`, `EvaluacionDiaria`,
`EvaluacionEstudiante`) — ese renombrado de código es un trabajo aparte,
pendiente de reparto con el equipo. Este diagrama refleja el diseño
objetivo, no necesariamente el estado exacto del código a la fecha.

## Alcance

Cubre los agregados mínimos de los cuatro dominios que pide la guía:
**Person, UserAccount y Role** (seguridad); **Student, Guardian y
Payment** (académico); **Coach, Category, TrainingSession,
Attendance y DailyEvaluation** (deportivo — nombres traducidos solo en
este diagrama; el código Java de `deportivo` sigue en español, pendiente
de reparto con el equipo); **Item,
StockMovement y Assignment** (inventario). Se agregan
**GuardianStudent** (la clase de asociación real entre Guardian y
Student — sin ella la relación \*-a-\* no se puede dibujar con fidelidad)
y **StudentEvaluation** (el detalle por jugador dentro de una
DailyEvaluation).

Quedan fuera, a propósito, los catálogos de apoyo (`GeneralStatus`,
`Posicion`, `Especialidad`, `Horario`, `Lesion`, `AuditLog`,
`Consent`, `Notification`, `CriterioEvaluacion`,
`DetalleEvaluacion`): añadirlos no cambia la estructura del dominio y
harían el diagrama ilegible. Están documentados en
`docs/basedatos/DATA-DICTIONARY.md`.

Las clases son entidades JPA: cada atributo privado ya tiene su
getter/setter público generado por Lombok (`@Getter @Setter`), así que
listarlos uno a uno no aportaría información — no se muestran. Las únicas
operaciones que se listan son las dos que sí tienen comportamiento propio
más allá de acceso a datos: `Attendance.enablesEvaluation()` y
`DailyEvaluation.isFinished()` (en el código real, todavía
`Asistencia.habilitaEvaluacion()` / `EvaluacionDiaria.estaFinalizada()`
hasta que se traduzca `deportivo`).

## Diagrama

```mermaid
classDiagram
    direction LR

    %% ===== Domain: seguridad =====
    class Person {
        -Long id
        -String name
        -String lastName
        -String nationalId
        -String email
        -String phone
        -LocalDate birthDate
        -Boolean active
    }

    class UserAccount {
        -Long id
        -String username
        -String passwordHash
        -OffsetDateTime lastAccess
        -Boolean active
    }

    class Role {
        -Long id
        -String name
        -String description
    }

    %% ===== Domain: academico =====
    class Student {
        -Long id
        -String studentCode
        -LocalDate enrollmentDate
        -BigDecimal weight
        -BigDecimal height
        -Boolean active
    }

    class Guardian {
        -Long id
        -String relationship
        -String contactPhone
        -Boolean active
    }

    class GuardianStudent {
        -Long id
        -String relationship
        -Boolean primaryContact
        -Boolean active
    }

    class Payment {
        -Long id
        -PaymentType type
        -Short year
        -Short month
        -BigDecimal amount
        -LocalDate paymentDate
    }

    %% ===== Domain: deportivo =====
    class Coach {
        -Long id
        -Short yearsOfExperience
        -String certification
        -Boolean active
    }

    class Category {
        -Long id
        -String name
        -Short minAge
        -Short maxAge
        -Boolean active
    }

    class TrainingSession {
        -Long id
        -LocalDate date
        -LocalTime startTime
        -LocalTime endTime
        -String field
        -String status
    }

    class Attendance {
        -Long id
        -LocalTime checkInTime
        -String method
        -String status
        -String notes
        +boolean enablesEvaluation()
    }

    class DailyEvaluation {
        -Long id
        -LocalDate date
        -String generalNotes
        -String status
        +boolean isFinished()
    }

    class StudentEvaluation {
        -Long id
    }

    %% ===== Domain: inventario =====
    class Item {
        -Long id
        -String name
        -ItemType type
        -String size
        -Integer currentStock
        -Integer minimumStock
        -String unitOfMeasure
        -Boolean active
    }

    class StockMovement {
        -Long id
        -MovementType movementType
        -Integer quantity
        -String reason
        -Instant movementDate
    }

    class Assignment {
        -Long id
        -Integer quantity
        -RecipientType recipientType
        -LocalDate assignmentDate
        -LocalDate expectedReturnDate
        -LocalDate actualReturnDate
        -AssignmentStatus status
        -String notes
    }

    %% ===== Relationships: seguridad =====
    Person "1" -- "0..1" UserAccount : has account
    UserAccount "*" -- "*" Role : has role

    %% ===== Relationships: academico =====
    Person "1" -- "0..1" Student : is
    Category "1" -- "0..*" Student : groups
    Student "1" -- "0..1" UserAccount : logs in as
    Student "1" -- "0..*" Payment : generates
    UserAccount "1" -- "0..*" Payment : registers

    Person "1" -- "1" Guardian : is
    UserAccount "1" -- "1" Guardian : logs in as
    Guardian "1" -- "0..*" GuardianStudent : links
    Student "1" -- "0..*" GuardianStudent : is represented in

    %% ===== Domain: deportivo =====
    Person "1" -- "1" Coach : is
    UserAccount "1" -- "1" Coach : logs in as
    Coach "1" -- "0..*" TrainingSession : leads
    Category "1" -- "0..*" TrainingSession : schedules

    TrainingSession "1" -- "0..*" Attendance : logs
    Student "1" -- "0..*" Attendance : checks in

    TrainingSession "1" -- "0..1" DailyEvaluation : has
    Coach "1" -- "0..*" DailyEvaluation : evaluates
    DailyEvaluation "1" *-- "0..*" StudentEvaluation : contains
    Student "1" -- "0..*" StudentEvaluation : is evaluated in
    Category "1" -- "0..*" StudentEvaluation : categoryOfDay

    %% ===== Relationships: inventario =====
    Item "1" -- "0..*" StockMovement : moves stock
    UserAccount "1" -- "0..*" StockMovement : registers
    Item "1" -- "0..*" Assignment : is assigned
    Student "0..1" -- "0..*" Assignment : receives
    Coach "0..1" -- "0..*" Assignment : receives
    UserAccount "1" -- "0..*" Assignment : registers
```

## Notas de fidelidad

- **Enumeraciones.** `Payment.type` (`MEMBRESIA`/`DIARIO`), `Item.type`
  (`UNIFORME`/`BALON`/`IMPLEMENTO`/`OTRO`),
  `StockMovement.movementType` (`ENTRADA`/`SALIDA`/`AJUSTE`),
  `Assignment.recipientType` (`ESTUDIANTE`/`ENTRENADOR`) y
  `Assignment.status` (`ASIGNADO`/`DEVUELTO`/`PERDIDO`) son enums Java
  (`@Enumerated(EnumType.STRING)`), no texto libre. Los valores del enum
  no se tradujeron junto con el resto del código.
- **`Person`–`UserAccount` y `Person`–`Student` son `0..1` por regla de
  negocio, no por restricción de base de datos.** El código JPA declara
  ambas relaciones como `@ManyToOne` simple, sin `unique = true`; es
  `UserAccountService`/`StudentAccessService` quien impide en tiempo de
  ejecución que una persona tenga más de una cuenta o ficha de estudiante
  activa a la vez (ver `UserAccountService.validateRoleCoherent`,
  `StudentAccessService.validateConsistencyWithStudentRecord`).
  Distinto de `Coach`/`Guardian`, donde el `1..1` con Person y
  UserAccount sí lo impone la columna `unique = true` de la migración.
- **`Assignment` es XOR, no una relación libre con ambas puntas.** Exactamente
  una de `student`/`coach` va poblada según `recipientType`; la
  base de datos lo exige con un `CHECK` (migración V15), no el modelo de
  objetos.
