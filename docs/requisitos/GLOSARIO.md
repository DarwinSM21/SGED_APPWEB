# Glosario de términos

**Sistema:** SGED — Escuela Deportiva ProFútbol

Este glosario resuelve DOC-02 (informe de evaluación de calidad,
sección 2.3): el vocabulario del sistema combina el dominio deportivo,
el administrativo y el técnico, y algunos términos son ambiguos fuera de
ese contexto. Cada entrada cubre solo lo que un lector externo no puede
deducir del nombre.

---

### Asignación

Vínculo entre un artículo de inventario y la persona (estudiante o
entrenador) que lo tiene en su poder, con fecha de entrega y, si aplica,
fecha de devolución. Gestionada por `AsignacionService`; no debe
confundirse con la asignación de un rol a un usuario.

Términos relacionados: [artículo](#artículo), [movimiento de inventario](#movimiento-de-inventario).

### Artículo

Ítem del catálogo de inventario deportivo (uniformes, balones,
implementos) con existencias controladas por stock. Entidad `Articulo`,
tabla `inventario.articulos`.

### Borrado lógico

Baja de un registro que no elimina la fila de la base de datos: se marca
un campo `activo = false` y las consultas de lectura lo excluyen por
defecto (por ejemplo `findByIdEstudianteAndActivoTrue`). El historial se
conserva para trazabilidad y auditoría; la baja física no se usa en el
dominio de negocio.

### Categoría

Grupo etario o competitivo en el que se organiza a los estudiantes
(por ejemplo, sub-12, sub-15). Determina en qué sesiones de entrenamiento
participa cada estudiante. Entidad `Categoria`.

### Evaluación diaria

Registro del desempeño de un estudiante en una sesión de entrenamiento
concreta, calificado por el entrenador. Es la fuente de datos que usa
`PlantillaService` para ordenar a los jugadores al sugerir una alineación.
Entidad `EvaluacionEstudiante`.

### Membresía

Uno de los dos tipos de [pago](#pago) (`Pago.TipoPago.MEMBRESIA`): cubre
un mes calendario completo (año y mes obligatorios) y no puede repetirse
para el mismo estudiante y periodo. Se distingue del pago `DIARIO`, que es
puntual y no lleva periodo asociado.

### Movimiento de inventario

Registro de entrada o salida de existencias de un artículo (compra,
baja, ajuste). No debe confundirse con una [asignación](#asignación):
el movimiento cambia el stock total, la asignación vincula unidades ya
existentes a una persona.

### Pago

Cobro registrado a un estudiante. Ver [membresía](#membresía) para el
tipo periódico; el tipo `DIARIO` es puntual. Entidad `Pago`, gestionada
por `PagoService`.

### Plantilla

Conjunto de jugadores convocados por el entrenador para una sesión o
partido determinado, generado por `PlantillaService` a partir de reglas
explícitas y auditables (se excluye a los lesionados, se ordena por
promedio de evaluación diaria y se cortan los primeros N). **No** designa
un formato reutilizable de documento, que es el sentido más común del
término en castellano técnico.

Términos relacionados: [evaluación diaria](#evaluación-diaria),
[sesión de entrenamiento](#sesión-de-entrenamiento).

### Representante

Persona responsable legal de un estudiante menor de edad (madre, padre o
tutor). Entidad `RepresentanteEstudiante`, dominio académico.

### Sesión de entrenamiento

Evento programado en el que un entrenador dirige a los estudiantes de una
categoría, con fecha, horario y lugar. Sobre cada sesión se registra
asistencia y evaluación diaria. Entidad `SesionEntrenamiento`.

### Seudonimización

Transformación de datos deportivos de un estudiante para que puedan
enviarse a un proveedor de IA externo (`GeneradorFeedbackIA`) sin datos
identificativos directos. Se implementa en `PerfilJugadorAnonimo`. No
equivale a anonimización irreversible: el sistema conserva la
correspondencia internamente.
