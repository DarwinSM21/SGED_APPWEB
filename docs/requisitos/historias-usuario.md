# Historias de usuario

**Sistema:** SGED — Escuela Deportiva ProFútbol
**Formato:** `Como <rol>, quiero <objetivo>, para <beneficio>`
**Criterios de aceptación:** formato Gherkin (Dado / Cuando / Entonces)

Cada historia enlaza con su requisito formal en [`SRS.md`](SRS.md) y con su
estado real de implementación (✅ implementado · 🟡 modelado sin API ·
⬜ planificado).

---

## Épica 1 — Acceso seguro al sistema

### HU-01 · Iniciar sesión ✅
**Como** usuario registrado de la escuela,
**quiero** iniciar sesión con mi usuario y contraseña,
**para** acceder a la información según los permisos de mi rol.

> Cubre RF-02.

**Criterios de aceptación**
- **Dado** que existo como usuario activo, **cuando** envío credenciales
  correctas, **entonces** el sistema responde `200` y establece la cookie de
  sesión con los atributos `HttpOnly`, `Secure` y `SameSite=Strict`.
- **Dado** que envío una contraseña incorrecta, **cuando** intento iniciar
  sesión, **entonces** el sistema responde con error y **no** indica si el
  fallo fue por usuario inexistente o por contraseña errónea.
- **Dado** que inicié sesión correctamente, **cuando** inspecciono el cuerpo
  de la respuesta y el almacenamiento del navegador, **entonces** el token
  **no** aparece en ninguno de los dos.

---

### HU-02 · Protegerme de ataques de fuerza bruta ✅
**Como** administrador responsable del sistema,
**quiero** que las cuentas se bloqueen temporalmente tras varios intentos
fallidos,
**para** que un atacante no pueda adivinar contraseñas por repetición.

> Cubre RF-06.

**Criterios de aceptación**
- **Dado** que se han producido 5 intentos fallidos en menos de 15 minutos,
  **cuando** se realiza el sexto intento, **entonces** el sistema responde
  `429` con un cuerpo `ProblemDetail` explicativo.
- **Dado** que ya hay intentos fallidos acumulados, **cuando** se produce uno
  nuevo dentro de la ventana, **entonces** el tiempo de bloqueo **no** se
  reinicia.
- **Dado** que el usuario acierta la contraseña antes de agotar los intentos,
  **cuando** el login es exitoso, **entonces** el contador se borra.

---

### HU-03 · Cerrar sesión de forma efectiva ✅
**Como** usuario,
**quiero** que al cerrar sesión mi credencial deje de servir de inmediato,
**para** que nadie pueda reutilizarla si llegó a interceptarla.

> Cubre RF-03.

**Criterios de aceptación**
- **Dado** que tengo una sesión abierta, **cuando** cierro sesión,
  **entonces** el identificador del token queda registrado como revocado
  durante el tiempo que le restaba de vida.
- **Dado** que un token fue revocado, **cuando** se presenta nuevamente en
  una petición, **entonces** el sistema lo rechaza aunque su firma y su fecha
  de expiración sigan siendo válidas.

---

### HU-04 · Continuar trabajando sin re-autenticarme ✅
**Como** usuario con una sesión larga,
**quiero** que mi sesión se renueve automáticamente,
**para** no perder el trabajo en curso por una expiración inesperada.

> Cubre RF-04, RF-05.

**Criterios de aceptación**
- **Dado** que poseo un token de refresco vigente, **cuando** solicito la
  renovación, **entonces** recibo una nueva credencial sin introducir mis
  datos otra vez.
- **Dado** que recargo la página, **cuando** el frontend consulta la sesión,
  **entonces** obtiene mi usuario y rol si la sesión sigue activa, o es
  redirigido al inicio de sesión si no lo está.

---

## Épica 2 — Gestión de estudiantes

### HU-05 · Consultar el listado de estudiantes ✅
**Como** entrenador o personal administrativo,
**quiero** ver el listado de estudiantes por páginas,
**para** encontrar rápidamente a un deportista sin cargar toda la base de
datos de una vez.

> Cubre RF-08.

**Criterios de aceptación**
- **Dado** que hay estudiantes registrados, **cuando** solicito la primera
  página, **entonces** recibo los registros junto con el número de página,
  el tamaño, el total de elementos y el total de páginas.
- **Dado** que no tengo sesión iniciada, **cuando** intento consultar el
  listado, **entonces** el sistema responde `401`.

---

### HU-06 · Registrar un nuevo estudiante ✅
**Como** administrador,
**quiero** registrar a un estudiante asociándolo a una persona y una
categoría ya existentes,
**para** incorporarlo formalmente a la escuela.

> Cubre RF-10, RF-11. Reescrita el 2026-07-30: la categoría dejó de ser un
> texto validado por patrón y pasó a ser una referencia a
> `deportivo.categorias`.

**Criterios de aceptación**
- **Dado** que envío datos válidos (persona, categoría y estado general
  existentes), **cuando** registro al estudiante, **entonces** el sistema
  responde `201` con el recurso creado y su identificador.
- **Dado** que la categoría indicada no existe, **cuando** intento
  registrarlo, **entonces** el sistema responde `422` indicando el campo y
  el motivo del rechazo.
- **Dado** que mi rol es ENTRENADOR o USER, **cuando** intento registrar un
  estudiante, **entonces** el sistema responde `403`.

---

### HU-07 · Corregir los datos de un estudiante ✅
**Como** administrador,
**quiero** actualizar los datos de un estudiante,
**para** mantener la información al día cuando cambia de categoría o se
detecta un error de digitación.

> Cubre RF-12.

**Criterios de aceptación**
- **Dado** que el estudiante existe, **cuando** envío los datos corregidos,
  **entonces** el sistema los persiste y devuelve el recurso actualizado.
- **Dado** que el identificador no existe, **cuando** intento actualizarlo,
  **entonces** el sistema responde `404` con cuerpo `ProblemDetail`.

---

### HU-08 · Dar de baja sin perder el historial ✅
**Como** administrador,
**quiero** que al dar de baja a un estudiante su información no se borre,
**para** conservar su historial deportivo y poder reincorporarlo más
adelante.

> Cubre RF-13.

**Criterios de aceptación**
- **Dado** que el estudiante está activo, **cuando** lo doy de baja,
  **entonces** el sistema responde `204` y el registro permanece en la base
  de datos marcado como inactivo.
- **Dado** que el estudiante fue dado de baja, **cuando** consulto el listado
  de activos, **entonces** ya no aparece.

---

### HU-09 · Conocer cuántos deportistas hay por categoría ✅
**Como** entrenador o administrador,
**quiero** saber cuántos estudiantes activos tiene una categoría,
**para** planificar los grupos de entrenamiento y decidir si abro un nuevo
horario.

> Cubre RF-14.

**Criterios de aceptación**
- **Dado** que existen estudiantes activos e inactivos en una categoría,
  **cuando** consulto el conteo, **entonces** obtengo únicamente el número de
  activos.
- **Dado** que el conteo se solicita, **cuando** se ejecuta, **entonces** la
  agregación se resuelve en el motor de base de datos mediante un
  procedimiento almacenado, no recorriendo registros en la aplicación.

---

### HU-10 · Cerrar una categoría completa al final de temporada ✅
**Como** administrador,
**quiero** dar de baja a todos los estudiantes de una categoría en una sola
operación,
**para** cerrar la temporada sin tener que desactivarlos uno por uno.

> Cubre RF-15.

**Criterios de aceptación**
- **Dado** que una categoría tiene N estudiantes activos, **cuando** ejecuto
  la desactivación masiva, **entonces** el sistema informa exactamente cuántos
  registros fueron afectados.
- **Dado** que la operación falla a mitad de camino, **cuando** se
  interrumpe, **entonces** ningún registro queda parcialmente modificado
  (atomicidad).

---

## Épica 3 — Operación deportiva

> **Actualizado 2026-07-30:** HU-10b (entrenadores) pasó a ✅ con la
> reestructuración de paquetes. El resto sigue con modelo de datos migrado
> y versionado, pero **sin API REST expuesta** todavía.

### HU-10b · Registrar entrenadores del equipo ✅
**Como** administrador,
**quiero** registrar entrenadores vinculando su persona y su cuenta de
usuario, con especialidad y experiencia,
**para** poder asignarlos a horarios y evaluaciones más adelante.
> Cubre RF-16. El esquema impide que la misma persona o la misma cuenta se
> registren dos veces como entrenador.

### HU-11 · Programar los entrenamientos de la semana 🟡
**Como** entrenador,
**quiero** definir mis horarios semanales por categoría,
**para** que los deportistas y sus representantes sepan cuándo entrenar.
> Cubre RF-17. Restricción ya garantizada en el esquema: la hora de fin debe
> ser posterior a la de inicio, y el día debe estar entre 1 y 7.

### HU-12 · Marcar asistencia sin fricción 🟡
**Como** entrenador,
**quiero** que la asistencia se marque por RFID y también manualmente,
**para** no perder tiempo pasando lista y poder corregir casos puntuales.
> Cubre RF-19. El esquema ya impide registrar dos asistencias del mismo
> estudiante en la misma sesión.

### HU-13 · Evaluar el desempeño diario 🟡
**Como** entrenador,
**quiero** calificar a cada deportista por criterios y registrar la posición
que jugó ese día,
**para** seguir su evolución a lo largo de la temporada.
> Cubre RF-20, RF-21. Criterios sembrados: técnica, condición física, táctica
> y actitud.

### HU-14 · Avisar al representante ⬜
**Como** representante de un deportista,
**quiero** recibir un aviso cuando mi representado llega al entrenamiento o
sufre una lesión,
**para** estar tranquilo y actuar a tiempo si hace falta.
> Cubre RF-22. **No implementado y sin esquema.** Requiere definir primero el
> mecanismo de consentimiento del representante, por tratarse de datos de
> menores de edad (ver [`../etica/ETHICS.md`](../etica/ETHICS.md)).

---

## Resumen de cobertura

| Épica | Historias | ✅ | 🟡 | ⬜ |
|---|---|---|---|---|
| 1 — Acceso seguro | HU-01…HU-04 | 4 | 0 | 0 |
| 2 — Gestión de estudiantes | HU-05…HU-10 | 6 | 0 | 0 |
| 3 — Operación deportiva | HU-10b…HU-14 | 1 | 3 | 1 |

No incluye HU dedicadas a los recursos nuevos RF-23…RF-26 (categorías,
usuarios, personas, estados) — se documentan directamente en el SRS por
haber aparecido como CRUD administrativo, no como una necesidad de un actor
específico articulada previamente.
| **Total** | **14** | **10** | **3** | **1** |
