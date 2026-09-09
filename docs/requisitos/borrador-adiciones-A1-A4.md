# Borrador — Parte 3 de la revisión 29148 (A1–A4)

> **Estado (2026-09-08, tras `e6db415`):**
>
> - **A1** — **hecho por Ricardo** en `e6db415` (M1–M9): validador reescrito
>   en `scripts/validate-traceability.py` con las mismas cuatro comprobaciones
>   (nº exacto de columnas, vocabulario de estado, existencia de clases `*Test`
>   en matriz y SRS, paridad de ids SRS↔matriz). El borrador en bash que se
>   había preparado aquí quedó **superado y descartado**.
> - **A3 y A4** — **aplicados** al `SRS.md` y a `matriz.csv` sobre el trabajo
>   de Ricardo: RNF-24 reforzado; RNF-23 dividido en RNF-23a/RNF-23b, con sus
>   dos filas en la matriz de 11 columnas. `python3 scripts/validate-traceability.py`
>   → exit 0.
> - **A2** — sigue como borrador; **no** incorporado (Ricardo no lo tocó).
>
> Repo: `origin/main` = `e6db415` (Ricardo, M1–M9 + Lighthouse público).

---

## A1 — Validador de trazabilidad  ✅ hecho por Ricardo (`e6db415`)

Ricardo reescribió el validador en Python (`scripts/validate-traceability.py`)
con las comprobaciones que pedía el docente:

| # | Comprobación | Defecto que detectaba |
|---|---|---|
| 1 | Toda fila tiene el mismo nº de columnas que la cabecera (parseando comillas) | **M1** (RF-38 / RF-43) |
| 3 | `estado` ∈ {Implementado, Modelado, Planificado} | **M2** |
| 4 | Toda clase `*Test` citada (matriz + SRS) existe; `Clase.metodo` resuelve | **M4** |
| 5 | Los ids de requisito del SRS y de la matriz coinciden en ambas direcciones | **M5** |

M1/M2/M3/M4/M5 quedaron corregidos en el mismo commit; RF-36 se retiró de la
matriz (no tenía sección propia en el SRS). El validador corre en CI contra los
archivos reales y pasa en verde.

> Nota local: `bash scripts/test-validate-traceability.sh` falla en Windows por
> un problema de codificación de consola (`VIOLACIÓN` → `VIOLACI�N` rompe un
> `grep`), no por contenido. En CI (Ubuntu, UTF-8) pasa. Ya ocurría en
> `e6db415` sin ninguno de nuestros cambios.

---

## A2 — Datos personales de menores: de hallazgo a requisito con criterio de cierre  ✅ aplicado

> Aplicado 2026-09-08 en `SRS.md` (nueva **§3.6** con RF-49/RF-50/RF-51,
> RNF-25 en §4.3, **RNF-17** reescrito como paraguas con tabla hallazgo→requisito),
> `matriz.csv` (4 filas nuevas) y `ETHICS.md` (línea "Requisito de cierre" en
> H-01/H-02/H-03/H-04, versión → 1.2). `python3 scripts/validate-traceability.py`
> → exit 0. Lo de abajo es el razonamiento; el texto final está en el SRS.
>
> **Diferencias con este borrador:** H-09 (doble opt-in del correo) NO se
> convirtió en requisito nuevo — no está en la lista de A2 (cédula, texto
> libre, supresión, consentimiento) — sino que queda como limitación
> documentada de RF-37 en el paraguas RNF-17, con cierre pleno marcado como
> trabajo futuro. Ver aviso al usuario.

**Problema (cita del docente):** «RNF-16 menciona la seudonimización … pero los
hallazgos abiertos de ETHICS.md —cédula en claro, texto libre sin control,
ausencia de mecanismo de supresión, consentimiento del representante— siguen
sin requisito que obligue a cerrarlos. Conviértanlos en requisitos con criterio
y condición de cierre, igual que hicieron con RF-37.»

RNF-17 hoy solo dice "cada hallazgo deberá tener un criterio de cierre y una
fecha" — delega y no cierra nada. Propuesta: RNF-17 pasa a ser el paraguas, y
cada hallazgo se convierte en un requisito propio y verificable.

Fechas objetivo: se proponen como **TBD por el equipo**; la referencia natural
es "antes de la defensa oral (semana 17)" para los que bloquean, y "trabajo
futuro documentado" para los que no.

---

### RF-49 — Opcionalidad y validación de la cédula  *(cierra H-01)*

*El sistema deberá tratar `seguridad.personas.cedula` como un dato opcional;
cuando se proporcione, deberá validar el formato de cédula ecuatoriana
(10 dígitos + dígito verificador) y rechazar con `422` los valores que no lo
cumplan. La unicidad de cédula, cuando haya valor, deberá garantizarse a nivel
de esquema.*

- **Prioridad:** Alta · **MoSCoW:** Must · **Método de verificación:** Prueba automatizada
- **Criterio verificable:** prueba que (a) crea una persona sin cédula → `201`;
  (b) cédula con dígito verificador inválido → `422`; (c) cédula duplicada → `422`/`409`.
  Migración Flyway que añade la restricción `UNIQUE` parcial (`WHERE cedula IS NOT NULL`).
- **Condición de cierre:** las tres pruebas en verde + migración aplicada + nota
  en ETHICS.md §H-01 marcada "corregido, fecha".
- **Fecha objetivo:** TBD.
- **Fuera de alcance de este requisito:** cifrado a nivel de columna (se deja
  como recomendación de despliegue real en RNF-21/§H-01, no como obligación de
  esta entrega).

---

### RF-50 — Supresión / anonimización de los datos de un titular  *(cierra H-03)*

*El sistema deberá ofrecer a un usuario ADMINISTRADOR una operación que, ante
una solicitud de supresión del representante legal, anonimice los datos
identificativos de un estudiante (nombre, apellido, cédula, correo, teléfono,
fecha de nacimiento, observaciones de texto libre) sustituyéndolos por valores
neutros, conservando las claves foráneas y las estadísticas agregadas, y
dejando registro del acto en la bitácora de auditoría.*

- **Prioridad:** Media · **MoSCoW:** Should · **Método de verificación:** Prueba automatizada; Demostración
- **Criterio verificable:** procedimiento almacenado versionado
  `academico.sp_anonimizar_estudiante(p_id INT)` (cumple RD-02); endpoint
  `POST /api/estudiantes/{id}/anonimizar` restringido a ADMINISTRADOR y
  auditado (`@Audited`); prueba que verifica que tras la operación
  (a) los campos identificativos quedan en valores neutros,
  (b) las FKs y los conteos de asistencia/evaluación siguen resolviendo,
  (c) queda un evento de auditoría.
- **Condición de cierre:** endpoint + SP + prueba en verde; ETHICS.md §3.4 y
  §H-03 actualizados; RNF-22 deja de decir "el mecanismo no existe".
- **Fecha objetivo:** TBD.

---

### RF-51 — El consentimiento vigente es precondición del envío de notificaciones  *(refuerza H-04 / H-07)*

*El sistema no deberá crear ni enviar una notificación al representante
(RF-22 / RF-40) si no existe un consentimiento vigente de ese representante
para el alcance correspondiente. La ausencia de consentimiento deberá
registrarse como motivo de no-envío, no como error silencioso.*

- **Prioridad:** Alta · **MoSCoW:** Must (condiciona a RF-22) · **Método de verificación:** Prueba automatizada
- **Criterio verificable:** prueba que (a) con consentimiento vigente →
  `NotificacionService` crea la fila; (b) sin consentimiento o revocado →
  no se crea fila y se registra el motivo; (c) al revocar el consentimiento,
  las notificaciones futuras dejan de crearse.
- **Condición de cierre:** `NotificacionService.crear*` consulta
  `academico.consentimientos` antes de insertar; prueba en verde;
  ETHICS.md §H-04 pasa de "parcial" a "resuelto" (la mitad que faltaba era
  precisamente el gateado del envío).
- **Fecha objetivo:** TBD.
- **Nota:** RF-39 ya cubre *registrar/revocar* el consentimiento; lo que falta
  es que el sistema lo *use* como compuerta. Este requisito es esa compuerta.

---

### RNF-25 — Control de contenido de las observaciones de texto libre  *(cierra H-02, desbloquea RF-48)*

*El campo `deportivo.observaciones_estudiante.texto` deberá tener un límite de
longitud aplicado en el servidor, una guía de redacción visible para el
entrenador en el punto de captura, y visibilidad restringida al entrenador
autor y a los roles de coordinación (ADMINISTRADOR). Mientras estas tres
condiciones no se cumplan, RF-48 permanece en estado Won't.*

- **Método de verificación:** Inspección; Prueba automatizada
- **Criterio verificable:** `@Size(max = N)` + validación de servidor con `422`;
  texto de guía en el componente de evaluación diaria; `@PreAuthorize` que
  limita la lectura; prueba de que otro entrenador recibe `403`.
- **Condición de cierre:** los tres controles presentes y probados; ETHICS.md
  §H-02 marcado "corregido"; RF-48 puede reevaluarse a Should/Implementado.
- **Fecha objetivo:** TBD.

---

### RNF-26 — Verificación del correo del titular (doble opt-in)  *(cierra H-09)*

*El sistema deberá disponer de un mecanismo de verificación del correo de
`seguridad.personas.correo` (envío de enlace de confirmación y marca
`correo_verificado`). El restablecimiento de contraseña por enlace (RF-37)
solo deberá enviarse a direcciones verificadas; para una dirección no
verificada la respuesta de `/forgot` sigue siendo genérica pero no se envía
correo.*

- **Método de verificación:** Prueba automatizada; Demostración
- **Criterio verificable:** columna `correo_verificado` (migración Flyway);
  endpoint de confirmación; prueba de que `/forgot` sobre correo no verificado
  no invoca al mailer pero responde `202`.
- **Condición de cierre:** flujo de verificación disponible + prueba en verde;
  ETHICS.md §H-09 marcado "corregido".
- **Fecha objetivo:** TBD — es candidato razonable a "trabajo futuro
  documentado" si el equipo decide que excede la entrega; en ese caso el
  requisito se mantiene con MoSCoW **Won't (esta entrega)** y fecha explícita,
  no se borra.

---

### H-06 (peso y altura) — decisión, no requisito nuevo

El docente lo trata en **M7**, no en A2 (RF-11b «Implementado sin resolución
ética» es un estado que no es un estado). La acción es **decidir**:
(a) retirar `peso`/`altura` del esquema y del DTO; o
(b) documentar finalidad concreta + base legal + consentimiento separado y
dejar RF-11b como `Implementado` con esa base citada.
Hasta que se decida, RF-11b y RF-48 no deberían ir a la defensa con un estado
inventado. Esto lo coordina el equipo (Ricardo, pila M).

---

### Cambio propuesto a RNF-17 (paraguas)

> *El sistema deberá cerrar los hallazgos de protección de datos de menores de
> `docs/etica/ETHICS.md` mediante los requisitos **RF-49** (H-01), **RF-50**
> (H-03), **RF-51** (H-04/H-07), **RNF-25** (H-02) y **RNF-26** (H-09). Cada
> uno tiene criterio verificable, condición de cierre y fecha objetivo. H-05
> (certificado TLS) se cierra por **RNF-21**; H-06 (peso y altura) requiere una
> decisión de retirada o de base legal documentada (ver RF-11b).*

---

## A3 — Respaldo y recuperación: reforzar RNF-24  ✅ aplicado

> Aplicado en `SRS.md` (RNF-24) y `matriz.csv` sobre `e6db415`. Lo de abajo es
> el razonamiento; el texto final está en el SRS.

**Problema (cita):** «Respaldo y recuperación … con frecuencia, retención y
objetivos de recuperación. Existe evidencia de recuperación punto en el tiempo
en el repositorio y sigue sin requisito que la exija.»

RNF-24 ya exige respaldo diario, retención 30 días y RTO medido. Le falta:
**RPO explícito**, **declarar la recuperación punto-en-el-tiempo (PITR) del
proveedor** con su retención real, **destino de almacenamiento fijado** (hoy
`BACKUP.md` dice "todavía no está fijado") y **evidencia archivada de una
restauración de prueba cronometrada** (hoy `BACKUP.md` §"Prueba periódica"
dice explícitamente que no existe).

### Enunciado propuesto para RNF-24 (reemplaza el actual)

> *El sistema deberá contar con:
> (a) un respaldo diario de la base de datos completa (`pg_dump -F c`),
> automatizado, con **retención mínima de 30 días** y destino en almacenamiento
> privado externo al repositorio y —cuando el plan lo permita— externo al
> proveedor de base de datos;
> (b) la **recuperación punto-en-el-tiempo (PITR)** del proveedor gestionado
> (Supabase) declarada con su ventana de retención real según el plan vigente;
> (c) un **objetivo de punto de recuperación (RPO) ≤ 24 h** y un **objetivo de
> tiempo de recuperación (RTO) medido**, no estimado;
> (d) un procedimiento de restauración documentado (`RUNBOOK.md` §5) y
> **verificado con evidencia archivada y fechada** de al menos una ejecución
> real contra una base separada.*

- **Método de verificación:** Demostración; Inspección
- **Criterio verificable:** `BACKUP.md` con destino fijado + RPO + retención PITR;
  `docs/mediciones/backup/restauracion-YYYY-MM-DD.md` con log y cronómetro;
  RTO y RPO reflejados en la matriz (columna evidencia) con fecha.
- **Condición de cierre:** existe el archivo de evidencia de restauración y el
  RTO figura como medido (no "pendiente de medir").
- **Fecha objetivo:** antes de la defensa oral (la guía —`BACKUP.md`— ya la fija
  en la semana 17).

---

## A4 — Indisponibilidad de Redis: la mitad pendiente de RNF-23  ✅ aplicado

> Aplicado en `SRS.md` (RNF-23 → RNF-23a/RNF-23b) y `matriz.csv` (dos filas
> nuevas, 11 columnas) sobre `e6db415`. Lo de abajo es el razonamiento.


**Problema (cita):** «RNF-23 declara "auth falla cerrado; cache sin degradación
pendiente", … la mitad pendiente necesita su propio criterio y su fecha.»

RNF-23 se divide en la parte cumplida y la pendiente, con la misma disciplina
que RF-19 → RF-19a/RF-19b.

### RNF-23a — Autenticación falla-cerrado ante caída de Redis  *(cumplido)*

*Ante una excepción al consultar la lista de revocación o la época de sesión,
el filtro JWT no deberá autenticar la petición; los recursos protegidos
responden `401`. Un token que no puede comprobarse no se acepta.*

- **Estado:** Implementado · **Método de verificación:** Análisis; Demostración
- **Origen:** `JwtAuthenticationFilter` (try/catch), `JwtAuthenticationFilterTest`.

### RNF-23b — Degradación de la caché de listados ante caída de Redis  *(pendiente)*

*Ante la indisponibilidad de Redis, la caché de listados (RNF-02) deberá
degradarse a consulta directa a la base de datos mediante un
`CacheErrorHandler`; una caída de Redis no deberá producir `5xx` en los
endpoints de listado ni impedir la lectura.*

- **Estado:** Planificado · **MoSCoW:** Should · **Método de verificación:** Prueba automatizada
- **Criterio verificable:** `CacheErrorHandler` registrado en
  `RedisCacheConfig`; prueba de integración con Redis caído que verifica
  `GET /api/estudiantes` → `200` con datos (no `500`); log de degradación.
- **Condición de cierre:** `CacheErrorHandler` presente + prueba en verde;
  RNF-23 (paraguas) deja de citar "cache sin degradación pendiente";
  matriz: fila RNF-23b en estado Implementado.
- **Fecha objetivo:** TBD.

### Matriz

`RNF-23` se parte en `RNF-23a` (Implementado) y `RNF-23b` (Planificado) — mismo
tratamiento que exige M5 para RF-19. Actualizar el enunciado paraguas y añadir
la comprobación 5 del validador cubre que no se vuelva a desincronizar.

---

## Estado

- **A1** ✅ hecho por Ricardo (`e6db415`, validador Python).
- **A2** ✅ aplicado (§3.6: RF-49/50/51, RNF-25, RNF-17 paraguas; ETHICS.md).
- **A3** ✅ aplicado (RNF-24 en SRS + matriz).
- **A4** ✅ aplicado (RNF-23a/RNF-23b en SRS + matriz).

Los cuatro puntos A quedan **especificados**. Falta la **implementación** de lo
que quedó Planificado, que es trabajo de código, no de requisitos:
RF-49 (validación de cédula + migración), RF-50 (SP de anonimización +
endpoint), RF-51 (compuerta de consentimiento en `NotificationService`),
RNF-25 (`@Size` + guía + `@PreAuthorize` del texto libre), RNF-23b
(`CacheErrorHandler`), y la evidencia de una restauración real cronometrada
de RNF-24. Cada uno con su fecha objetivo por fijar. Depende también de la
decisión de M7 sobre peso/altura (RF-11b / H-06).
