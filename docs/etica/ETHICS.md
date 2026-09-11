# Consideraciones éticas y tratamiento de datos personales

**Sistema:** SGED — Sistema de Gestión para la Escuela Deportiva ProFútbol
**Versión:** 1.9 (Entrega Final — revisado tras la reestructuración de
paquetes `academico`/`deportivo`/`seguridad`; 2026-09-08: cada hallazgo abierto
enlaza su requisito de cierre en el SRS (punto A2 de la revisión 29148), y se
cierran **H-01** (RF-49: cédula opcional + dígito verificador + índice único
parcial), **H-02** (RNF-25: topes de longitud, control de acceso y guía de
redacción), **H-03** (RF-50: procedimiento de anonimización `sp_anonimizar_estudiante`
+ endpoint auditado), **H-04** (RF-51, compuerta de consentimiento);
2026-09-09: **H-05** (certificado TLS) resuelto con el despliegue en Render
—certificado de una autoridad reconocida—, **H-06** (peso y altura) resuelto
—base legal documentada, lectura restringida por rol y alcance de
consentimiento `DATOS_FISICO_DEPORTIVOS` propio—, **H-07** (plantilla de
consentimiento) resuelto —nuevo `consentimiento/representante.md` para el
representante legal—; 2026-09-10 (revisión M3 del SRS v1.6): **H-09**
(correo no verificado) se especifica como **RNF-26** y se implementa —
doble opt-in del correo (`correo_verificado`, token de un solo uso,
`POST /api/auth/confirmar-correo`) y `/forgot` no envía el enlace a un
correo no verificado.
**Todos los hallazgos H-01…H-09 están cerrados.**)

---

## 1. Por qué este documento no es un trámite

SGED no gestiona datos de clientes adultos anónimos: gestiona **datos
personales de niños, niñas y adolescentes** — su nombre completo, su cédula,
su fecha de nacimiento, su asistencia diaria georreferenciada en el tiempo, y
una **evaluación numérica periódica de su desempeño físico y actitudinal**.

Esa combinación es sensible por tres motivos concretos:

1. **Son menores de edad.** No pueden otorgar consentimiento informado
   válido por sí mismos.
2. **Los datos de asistencia son datos de localización temporal.** Saber que
   un niño está en un campo determinado, a una hora determinada, tres veces
   por semana, es información de seguridad física, no solo administrativa.
3. **Las evaluaciones son perfilado de personas.** Un histórico de puntajes
   de "actitud" y "condición física" de un menor puede afectar decisiones
   sobre él (convocatorias, promoción de categoría) y, mal usado,
   estigmatizarlo.

---

## 2. Inventario real de datos personales tratados

Extraído directamente del esquema versionado, no de una descripción genérica.

| Dato | Tabla / columna | Categoría | Justificación de necesidad |
|---|---|---|---|
| Nombre y apellido | `seguridad.personas.nombre`, `.apellido` | Identificativo | Identificar al deportista y al personal. |
| Cédula | `seguridad.personas.cedula` | Identificativo — **alto riesgo** | Identificación unívoca ante federaciones. **Ver hallazgo H-01.** |
| Correo y teléfono | `seguridad.personas.correo`, `.telefono` | Contacto | Comunicación con el representante. |
| Fecha de nacimiento | `seguridad.personas.fecha_nacimiento` | Identificativo — **revela minoría de edad** | Determinar la categoría deportiva. |
| Código de estudiante | `academico.estudiantes.codigo_estudiante` | Identificativo | Identificación interna única. |
| Categoría | `academico.estudiantes.id_categoria` → `deportivo.categorias` | Derivado de la edad | Agrupación deportiva por rango etario (`edad_min`/`edad_max`). |
| Peso y altura | `academico.estudiantes.peso`, `.altura` | **Dato de salud — riesgo alto** | Seguimiento físico-deportivo. **Ver hallazgo H-06 — no estaba en el alcance original.** |
| Código RFID | `academico.estudiantes.rfid_codigo` | Identificador de dispositivo | Marcaje de asistencia sin fricción. |
| Asistencia con hora | `deportivo.asistencias` (`hora_entrada`, `estado`, `metodo`) | **Localización temporal** | Control de asistencia y seguridad del menor. |
| Puntajes por criterio | `deportivo.detalle_evaluacion.puntaje` | **Perfilado de desempeño** | Seguimiento formativo del deportista. |
| Observaciones libres | `deportivo.observaciones_estudiante.texto` | **Texto libre — riesgo alto** | Notas cualitativas del entrenador. **Ver hallazgo H-02.** |
| Contraseñas | `seguridad.usuarios.password_hash` | Credencial | Autenticación. Almacenada solo como hash BCrypt (coste 12), nunca reversible. |

---

## 3. Principios aplicados

### 3.1 Minimización

> **Corrección respecto a una versión anterior de este documento.** Esta
> sección afirmaba que el equipo había decidido **no** incorporar peso ni
> altura. Eso dejó de ser cierto: `academico.estudiantes` (esquema
> actualizado tras la reestructuración de paquetes) **sí** tiene columnas
> `peso` y `altura`. Se corrige aquí en vez de mantener una afirmación falsa
> en un documento de honestidad académica. El hallazgo correspondiente es
> **H-06**.

El principio de minimización sigue aplicando a lo que el sistema **no**
recolecta: no hay contextura corporal, ni datos de alimentación, ni
historial médico. Esa línea se mantuvo. Lo que cambió es que peso y altura
cruzaron esa línea sin que este documento se actualizara a tiempo — motivo
por el cual ahora se revisa cada vez que cambia el esquema, no solo al
redactar la primera versión.

### 3.2 Limitación de la finalidad

Los datos de asistencia y evaluación se recogen para el seguimiento
formativo del deportista dentro de la escuela. **No deben** usarse para:

- ceder o vender información a terceros (clubes, ojeadores, patrocinadores)
  sin consentimiento explícito y separado del representante;
- construir rankings públicos de menores identificables;
- tomar decisiones automatizadas sobre un menor sin revisión humana.

### 3.3 Confidencialidad e integridad

Controles ya implementados y verificados empíricamente:

| Control | Implementación | Evidencia |
|---|---|---|
| Cifrado en tránsito | Laboratorio: TLS 1.3 (nginx `:8443`, autofirmado). Producción (Render): TLS con certificado de *Google Trust Services*, HTTP→HTTPS y HSTS (H-05 resuelto) | `docs/mediciones/sec/a02-tls.txt`; `docs/despliegue/render.md` |
| Contraseñas no reversibles | BCrypt coste 12 | `db/seed.sql`, `SecurityConfig.java` |
| Credencial no accesible por scripts | JWT en cookie `HttpOnly`, `Secure`, `SameSite=Strict` | `AuthController.java` |
| Revocación efectiva de sesión | Lista negra de `jti` en Redis | `RedisBlacklistService.java` |
| Acceso restringido por rol | `@PreAuthorize` en servidor | `docs/mediciones/sec/a01-acceso-roto.txt` |
| Resistencia a fuerza bruta | 5 intentos / 15 min → `429` | `docs/mediciones/sec/a07-rate-limit.txt` |
| Trazabilidad de accesos | Log de auditoría con IP, fecha y sujeto | `docs/mediciones/sec/a09-logging.txt` |
| Sin SQL dinámico | Parámetros vinculados y procedimientos con parámetros nombrados | `docs/mediciones/sec/a03-inyeccion.txt` |

### 3.4 Conservación y derecho al olvido

El sistema aplica **baja lógica**, no borrado físico (`activo = FALSE`), para
preservar la integridad referencial del historial deportivo. Esto es una
decisión técnica correcta, pero tiene una consecuencia ética que hay que
nombrar: **la baja lógica no es un borrado**. Para el caso en que un
representante ejerza el derecho a la supresión de los datos de su
representado, el sistema ofrece desde el 2026-09-08 (**RF-50**) una operación
de **anonimización**: `POST /api/estudiantes/{id}/anonimizar` (solo
ADMINISTRADOR, auditada) invoca el procedimiento almacenado
`academico.sp_anonimizar_estudiante` (migración `V27`), que sustituye los
datos identificativos de la persona (nombre, apellido, cédula, correo,
teléfono, foto, fecha de nacimiento) por valores neutros, borra el texto
libre escrito sobre el menor, anonimiza y desactiva la cuenta de acceso y da
de baja lógica la ficha — conservando las claves foráneas y las estadísticas
agregadas (asistencia, evaluaciones, pagos). Ver hallazgo H-03 (resuelto).

---

## 4. Hallazgos y estado de cierre

> Todos los hallazgos **H-01…H-09 están cerrados**. Cada apartado conserva
> la redacción original del hallazgo y el registro de cómo se cerró, por
> honestidad sobre el proceso.

Se documentan como riesgos reconocidos, no se ocultan.

### H-01 — La cédula se almacena en claro y sin validación (resuelto el 2026-09-08 — RF-49)

`seguridad.personas.cedula` es `VARCHAR(10)` sin restricción de unicidad,
sin validación de dígito verificador y sin cifrado en reposo. Para un
identificador nacional de un menor, esto es más permisivo de lo deseable.
**Mitigación propuesta:** hacer el campo opcional, validar el formato
ecuatoriano y evaluar cifrado a nivel de columna si se despliega en
producción real.
**Requisito de cierre:** **RF-49** (SRS §3.6). **Resuelto el 2026-09-08:**
la cédula pasó a ser opcional (sin `@NotBlank`), la anotación `@Cedula`
(`common.validation.CedulaValidator`) valida el dígito verificador ecuatoriano
cuando se proporciona, y la migración `V26__cedula_opcional_y_unica.sql` quita
el `NOT NULL` y crea un índice único parcial `WHERE cedula IS NOT NULL`
(`CedulaValidatorTest`, `PersonControllerTest`, `PersonServiceTest`). Las
cédulas ficticias de `db/seed.sql` se cargan por SQL directo, no pasan por la
validación, y siguen sirviendo como identificadores internos. El cifrado de
columna queda fuera del alcance de esta entrega (recomendación de despliegue
real con datos reales).

### H-02 — Las observaciones de texto libre no tienen control de contenido (resuelto el 2026-09-08 — RNF-25)

`deportivo.observaciones_estudiante.texto` es `TEXT` libre sobre un menor,
sin límite de longitud ni guía para el entrenador. Un campo así puede
terminar conteniendo juicios de valor, datos de salud o comentarios
inapropiados.
**Mitigación propuesta:** guía de redacción para entrenadores, límite de
longitud, y visibilidad restringida al entrenador y a la coordinación.
**Requisito de cierre:** **RNF-25** (SRS §4.3). **Resuelto el 2026-09-08:**
tope de longitud en la capa de aplicación (`EvaluacionDiariaService.finalizar`,
`@Size` de la descripción de lesión y de la observación de asistencia) y a
nivel de motor (`V25__limite_texto_libre_menores.sql`, `CHECK char_length` en
`evaluaciones_diarias`, `observaciones_estudiante` y `lesiones`); lectura
restringida a ADMINISTRADOR/ENTRENADOR por `@PreAuthorize`; y **guía de
redacción** en el formulario de lesión de la pantalla de evaluación diaria
(qué no escribir, con contador y `maxlength`). RF-48 sale de MoSCoW Won't.

### H-03 — No existe mecanismo de supresión de datos (resuelto el 2026-09-08 — RF-50)

Ver §3.4. **Mitigación propuesta:** procedimiento almacenado de anonimización
(sustituir datos identificativos por valores neutros conservando las claves
foráneas y las estadísticas agregadas), invocable solo por
`ADMINISTRADOR` y registrado en auditoría.
**Requisito de cierre:** **RF-50** (SRS §3.6), que implementa también el
mecanismo que exige RNF-22. **Resuelto el 2026-09-08:** procedimiento
almacenado versionado `academico.sp_anonimizar_estudiante`
(`V27__sp_anonimizar_estudiante.sql`, fuente en
`db/procs/sp_anonimizar_estudiante.sql`), invocado desde
`StudentService.anonymize` y expuesto en `POST /api/estudiantes/{id}/anonimizar`
— restringido a `ADMINISTRADOR`, anotado `@Audited(accion = "ANONIMIZAR")` y con
`@CacheEvict` de la caché de listados. Sustituye nombre, apellido, cédula,
correo, teléfono, foto y fecha de nacimiento por valores neutros; anonimiza y
desactiva la cuenta de acceso; reemplaza el texto libre sobre el menor
(`observaciones_estudiante.texto`, `lesiones.descripcion`) por un marcador; y
da de baja lógica la ficha. No borra ninguna fila: FKs y agregados intactos.
Pruebas: `StudentServiceTest` (`anonimizar_delega_en_sp`,
`anonimizar_estudiante_inexistente_lanza_404`), `StudentControllerTest`
(`anonimizar_devuelve_204`, `anonimizar_estudiante_inexistente_da_404`).

### H-04 — El consentimiento del representante no está modelado (resuelto el 2026-09-08; ver RF-51)

El sistema no registra si el representante legal autorizó el tratamiento de
los datos del menor, ni la fecha de esa autorización. RF-22 (notificaciones
al representante) **no debe implementarse** antes de resolver esto.
**Mitigación propuesta:** tabla de consentimientos con fecha, alcance y
representante otorgante, como precondición del módulo de notificaciones.

**Resolución parcial (2026-08-03).** Se agregó `academico.consentimientos`
(migración `V9__representante_recepcionista.sql`): fecha de otorgamiento,
alcance, quién lo registró y quién lo revocó, con índice único parcial que
permite revocar y volver a otorgar sin perder el historial. El rol
REPRESENTANTE y el vínculo `academico.representante_estudiante` (con su
propio `activo`, para poder cortar el acceso de un tutor puntual sin tocar
su cuenta ni sus otros representados) también se implementaron.

**A la fecha de esta nota (2026-08-03) seguía parcial a propósito** — el
cierre pleno llegó el 2026-09-08 con RF-51 (más abajo). Lo que quedaba
abierto entonces era la mitad que este mismo hallazgo prohíbe construir sin
la otra — el envío real de notificaciones (RF-22 propiamente dicho,
push/email/SMS) no está implementado. La lectura de informes (evaluación diaria y lesiones) por
parte del representante SÍ está disponible, pero se autoriza únicamente por
el vínculo activo `representante_estudiante` (creado por un administrador),
no por un consentimiento vigente: un guardián consultando los datos del hijo
que él mismo matriculó es uso ordinario esperado, categóricamente distinto
del envío proactivo del sistema que este hallazgo señala como el riesgo real
(ver también H-07). La tabla de consentimientos queda reservada
exclusivamente para gatear esa notificación cuando se construya.

**Actualización (2026-08-18) — el consentimiento ya se puede registrar desde
la aplicación.** Hasta esta fecha la resolución era incompleta en un sentido
que no estaba dicho: la tabla y su API existían desde la V9, pero ninguna
pantalla las usaba, así que un consentimiento solo podía registrarse
escribiendo SQL a mano. Documentar una mitigación que en la práctica nadie
puede ejecutar es, para efectos del hallazgo, no haberla implementado.

Ahora existe **Consentimientos** (`/admin/consentimientos`, solo
ADMINISTRADOR): se elige al estudiante, se ven sus representantes vinculados
y para cada uno se otorga o revoca el consentimiento, con el historial de
revocaciones visible y fechado. La pantalla ofrece únicamente los
representantes ya vinculados a ese estudiante —la API acepta cualquier par,
pero registrar el consentimiento de quien no es su tutor no significa nada— y
declara explícitamente en su encabezado qué autoriza el consentimiento y qué
no, para que la distinción de este hallazgo no dependa de que alguien haya
leído este documento.

Verificado contra la base real: otorgar, rechazo del segundo consentimiento
vigente con el mismo alcance, revocar, y volver a otorgar conservando la fila
revocada con su fecha.

El riesgo concreto que este hallazgo señalaba —el **envío proactivo sin
consentimiento**— queda cerrado por RF-51 (ver abajo). Los canales de envío
externo (correo/SMS/push) siguen siendo trabajo futuro; cuando se construyan,
pasan por la misma compuerta de consentimiento.

**Requisitos de cierre:** **RF-39** (registrar/revocar el consentimiento) y
**RF-51** (SRS §3.6): el sistema no crea ni envía una notificación al
representante sin consentimiento vigente para ese alcance. **Resuelto:**
`NotificationService.crearParaCadaRepresentante` consulta
`academico.consentimientos` (por alcance, filtrando `revocado_en IS NULL`)
antes de insertar; sin consentimiento registra el motivo y no crea la fila
(`NotificationServiceTest`: casos con/sin consentimiento y aislamiento de
alcance, en verde). Este hallazgo pasa de "parcial" a **resuelto** (2026-09-08).

### H-05 — Certificado TLS autofirmado (resuelto el 2026-09-09 — despliegue en Render)

El entorno de laboratorio (nginx `:8443` de `docker-compose`) usa un
certificado autofirmado, adecuado para desarrollo y evaluación pero **no**
para producción con datos reales de menores.

**Resuelto el 2026-09-09:** el despliegue público está en Render
(`docs/despliegue/render.md`), que termina TLS con un certificado emitido por
una **autoridad reconocida** — *Google Trust Services* (verificado sobre
`https://sged-frontend-jofa.onrender.com` y `https://sged-backend-2p05.onrender.com`),
con redirección de HTTP a HTTPS y HSTS. El certificado autofirmado queda
únicamente en el entorno local de laboratorio, donde no hay datos reales.
**Requisito de cierre:** **RNF-21** (SRS). El endurecimiento TLS del laboratorio
(nginx) sigue como recomendación, no como obligación de esta entrega.

### H-06 — Peso y altura se agregaron sin base legal documentada (resuelto — decisión M7 del 2026-09-08, condiciones cerradas el 2026-09-09)

`academico.estudiantes.peso` y `.altura` son datos de salud de un menor.
Ninguna base legal para tratarlos (finalidad concreta, quién los usa, cuánto
se conservan) está documentada, y ni el SRS ni las historias de usuario
describen una funcionalidad que los use todavía — están en el esquema pero
no aparecen en ningún endpoint de escritura verificado. Es exactamente el
patrón de riesgo que la minimización busca evitar: un dato sensible se
incorpora primero, y su justificación se piensa después.

**Mitigación propuesta:** o (a) se documenta la finalidad concreta (por
ejemplo, seguimiento nutricional-deportivo por un profesional habilitado) y
se le aplica el mismo tratamiento de H-04 (consentimiento explícito del
representante, separado del consentimiento general de inscripción), o (b)
si no hay una funcionalidad concreta que los use en esta entrega, se
recomienda no exponerlos todavía por API y reconsiderar si deben persistir
en el esquema.

**Decisión M7 (2026-09-08) — opción (a): se conservan con base legal
documentada.** Verificado que `peso`/`altura` sí están cableados de punta a
punta: `StudentRequest` (escritura validada, opcional) → `Student` entity /
`academico.estudiantes` (V7) → `StudentResponse` (lectura en el detalle y el
listado). Se documenta:

- **Finalidad:** seguimiento físico-deportivo del estudiante por el cuerpo
  técnico (desarrollo físico apropiado a su categoría/edad, dosificación de
  carga en el entrenamiento). No se usan para ranking, selección ni decisiones
  automatizadas.
- **Base legal:** consentimiento del representante legal conforme a la LOPDP
  (Ecuador, datos de NNA), con **alcance específico "datos físico-deportivos"**
  separado del consentimiento general de inscripción — registrado por el
  mecanismo de RF-39 y sujeto a la compuerta de RF-51.
- **Responsables:** roles con acceso a la ficha. **Condición:** la lectura de
  `peso`/`altura` se restringe a `ADMINISTRADOR` y `ENTRENADOR` — RECEPCIONISTA
  no necesita el dato (pendiente de aplicar en `StudentResponse`).
- **Conservación / supresión:** mientras el estudiante esté activo; la baja
  lógica los preserva (RNF-22); se suprimen por RF-50 a solicitud del
  representante.
- **Opcionalidad:** el campo es opcional; la ficha opera sin él.

Este hallazgo queda **resuelto**. Las dos condiciones están cerradas:
(1) la lectura de `peso`/`altura` se restringe a `ADMINISTRADOR` y
`ENTRENADOR` en `StudentController` (`StudentResponse.withoutPhysicalData()`
para `RECEPCIONISTA`), con prueba
`StudentControllerTest.datos_fisicos_solo_para_administrador_y_entrenador`
(2026-09-08); (2) el consentimiento de alcance físico-deportivo tiene un valor
propio, `Consent.ALCANCE_DATOS_FISICO_DEPORTIVOS` (`"DATOS_FISICO_DEPORTIVOS"`),
que el `ADMINISTRADOR` registra por las rutas de RF-39
(`POST /api/consentimientos`) cuando el representante lo autoriza; el uso
proactivo de esos datos queda además bajo la compuerta de RF-51. El sistema no
impone esa autorización como precondición técnica de guardar el dato —peso y
altura siguen siendo opcionales y su tratamiento se ampara en la base legal
declarada aquí—; forzar la compuerta en el alta se deja como endurecimiento
posterior. Especificado en el SRS §RF-11b.

### H-07 — La plantilla de consentimiento cubre a los evaluadores del SUS, no a los representantes de los menores (resuelto el 2026-09-09)

`docs/etica/consentimiento/plantilla.md` es un consentimiento informado
bien construido para adultos que participan en la encuesta de usabilidad
(Bloque C.3) — resuelve el consentimiento de *ese* estudio, no el de H-04.
Son dos cosas distintas: uno es el consentimiento de un adulto para
evaluar el sistema; el otro es el consentimiento de un representante para
que los datos de **su hijo o hija menor de edad** sean tratados por el
sistema en producción. El **mecanismo** de ese consentimiento ya existía
(H-04 resuelto: entidad `Consent`, rutas de RF-39, compuerta de RF-51, y un
alcance propio para los datos físico-deportivos de H-06).

**Resuelto el 2026-09-09:** se añadió el **documento** que faltaba —
[`docs/etica/consentimiento/representante.md`](consentimiento/representante.md):
consentimiento informado dirigido al representante legal, con la base legal
(LOPDP, Constitución Art. 66.19, Código de la Niñez), el inventario de datos
tratados y su finalidad, los límites de uso (no cesión a terceros, no rankings
públicos, no decisiones automatizadas), **autorizaciones separadas y
revocables** mapeadas a los alcances de `Consent`
(`INFORMES`, `NOTIFICACIONES_ASISTENCIA`, `NOTIFICACIONES_LESION`,
`DATOS_FISICO_DEPORTIVOS`, cesión puntual), la política de conservación y
anonimización (RNF-22 / RF-50), y los derechos LOPDP del representante
(acceso vía RF-44, rectificación, supresión, revocación, oposición). El
original firmado se archiva fuera del repositorio; en el sistema solo quedan
las autorizaciones estructuradas de `academico.consentimientos`.

### H-08 — Los datos personales quedaron accesibles a cualquier cuenta autenticada (corregido el 2026-07-30)

Este hallazgo se registra **ya corregido**, porque describe una exposición
real que estuvo presente en el código de la Tercera Entrega y su corrección
solo tiene valor documentada.

Los cinco recursos que agregó la reestructuración
(`Categoria`, `Entrenador`, `Usuario`, `Persona`, `EstadoGeneral`) se
crearon sin ninguna anotación `@PreAuthorize`. Como `SecurityConfig`
termina la cadena con `anyRequest().authenticated()`, la única barrera era
tener una sesión válida: **cualquier usuario autenticado, incluido el rol
`USER` más básico, podía listar todas las personas registradas, buscar una
por número de cédula, y crear, editar o eliminar registros.** Como
`seguridad.personas` concentra la identificación de los estudiantes
menores de edad (nombre, apellido, cédula, correo, fecha de nacimiento),
la exposición alcanzaba justamente a los titulares que este documento se
compromete a proteger. `UsuarioController` era el único de los cinco que
sí tenía control de acceso, a nivel de clase.

Es además una regresión de una observación ya emitida: OBS-09 de la
Entrega 1B señalaba que no se aplicaba `@PreAuthorize`, y se había dado
por aplicada. La reestructuración reintrodujo el defecto en el código
nuevo sin que nada lo detectara, porque las pruebas de esos controladores
usan `standaloneSetup`, que no levanta la cadena de seguridad.

**Corrección aplicada.** Se restringió cada recurso según el dato que
maneja, no de forma uniforme: `Persona` queda íntegramente reservado a
`ADMINISTRADOR` (ningún otro rol necesita operar sobre datos
identificativos de un menor); la escritura sobre `Categoria` y
`Entrenador` también, por alterar catálogos de los que dependen los
estudiantes; y la lectura del catálogo de categorías y de estados se
mantiene abierta a los tres roles para no romper el uso legítimo. La
evidencia está en `docs/mediciones/sec/a01-acceso-roto.txt`, que ahora
comprueba recurso por recurso —incluida la búsqueda por cédula— y verifica
también que las lecturas permitidas siguen respondiendo `200`.

### H-09 — El restablecimiento de contraseña se envía a un correo no verificado (resuelto el 2026-09-10 — RNF-26)

El flujo de recuperación de contraseña (RF-37, agregado el 2026-09-07) envía
el enlace de un solo uso al valor de `seguridad.personas.correo` del usuario.
Ese correo lo registra el `ADMINISTRADOR` al crear la cuenta y **el sistema
nunca ha comprobado que la dirección sea real ni que pertenezca al titular**.

En la práctica el riesgo es acotado: quien ya controla el buzón de la
víctima puede tomar su cuenta, pero ese mismo atacante ya tendría acceso a
cualquier otro servicio de la persona atado a ese correo. Aun así se declara
como limitación conocida. Mitigaciones ya presentes: el enlace es de un solo
uso y vence en 30 minutos; la respuesta de `/forgot` es genérica y no revela
si la cuenta existe; hay límite de solicitudes por identificador y por IP; y
al completarse el cambio se invalidan todas las sesiones previas del usuario.

**Requisito de cierre: RNF-26 (SRS §4.3). Resuelto el 2026-09-10.** Tras la
revisión M3 del SRS v1.6 este hallazgo se especificó como requisito y se
implementó:

- **Criterio:** una dirección de correo no confirmada no recibe el enlace de
  restablecimiento; el alta o la modificación del correo exige verificación
  por un token de confirmación de un solo uso con ventana de vigencia.
- **Implementación:** columna `seguridad.personas.correo_verificado`
  (migración `V28`; las personas preexistentes se dieron por verificadas,
  las altas nuevas nacen sin verificar). `EmailVerificationTokenStore`
  (Redis, SHA-256 del token, TTL 48 h). `EmailVerificationService.enviarConfirmacion`
  se dispara al crear una persona (`PersonService.create`,
  `AuthService.register`) y al cambiar su correo (`PersonService.update`, que
  además vuelve a marcarlo sin verificar). `POST /api/auth/confirmar-correo`
  canjea el token (auditado `EMAILVERIFY_CONFIRMADO`); pantalla
  `/#/confirmar-correo`. `PasswordResetService.solicitar` no emite el enlace
  de RF-37 si `correo_verificado` es `false` (la respuesta de `/forgot` sigue
  siendo `202` genérica).
- **Condición de cierre:** cumplida — flujo implementado y probado
  (`EmailVerificationServiceTest`, `EmailVerificationTokenStoreTest`,
  `PasswordResetServiceTest.solicitar_correo_no_verificado_no_envia`,
  `AuthControllerTest`, `PersonServiceTest`, mailers, y
  `confirmar-correo.component.spec` en el frontend).
- **Despliegue:** `V28` se aplica a la Supabase de producción por el
  procedimiento incremental de `docs/despliegue/render.md` (Paso 3b), junto a
  `V25`–`V27`. Mitigaciones de RF-37 siguen vigentes (enlace de un solo uso,
  30 min, respuesta genérica, rate limit, invalidación de sesiones).

---

## 5. Ética en el desarrollo del proyecto

### 5.1 Datos usados durante el desarrollo

Todos los datos presentes en el repositorio (`db/seed.sql`, evidencias de
`docs/mediciones/`) son **ficticios**, creados para pruebas. No se utilizaron
datos reales de menores en ningún momento del desarrollo, ni en las
mediciones de rendimiento, ni en las auditorías de seguridad.

Las credenciales del usuario administrador sembrado (`admin` /
`sged2026`) están documentadas públicamente en el README **a propósito**,
porque son de un entorno de evaluación desechable. **No deben reutilizarse
en ningún despliegue real.**

### 5.2 Uso de herramientas de asistencia por IA

Declarado en detalle en [`../../CONTRIBUTORS.md`](../../CONTRIBUTORS.md).

### 5.3 Licenciamiento y atribución

El proyecto se publica bajo licencia MIT (ver `LICENSE`). Las dependencias de
terceros (Spring Boot, Angular, PostgreSQL, Redis, k6, JaCoCo) se usan bajo
sus respectivas licencias de código abierto, sin modificación ni
reatribución.

---

## 6. Marco de referencia

- **Constitución del Ecuador, Art. 66 numeral 19** — derecho a la protección
  de datos de carácter personal.
- **Ley Orgánica de Protección de Datos Personales (LOPDP), Ecuador, 2021** —
  en particular el tratamiento de datos de niños, niñas y adolescentes, que
  exige el consentimiento del representante legal.
- **Código de la Niñez y Adolescencia (Ecuador)** — interés superior del
  niño.
- **ACM Code of Ethics and Professional Conduct (2018)** — §1.6 (privacidad),
  §1.7 (confidencialidad), §2.5 (evaluación exhaustiva de riesgos).
- **OWASP Top 10:2021** — controles verificados en `docs/mediciones/sec/`.
- **SWEBOK v4.0** — capítulo de práctica profesional y transparencia.

---

## 7. Responsable

Consultas sobre el tratamiento de datos en este proyecto académico:
el equipo de desarrollo listado en
[`../../CONTRIBUTORS.md`](../../CONTRIBUTORS.md), Carrera de Ingeniería en
Software, Universidad Técnica Estatal de Quevedo.
