# Consideraciones éticas y tratamiento de datos personales

**Sistema:** SGED — Sistema de Gestión para la Escuela Deportiva ProFútbol
**Versión:** 1.1 (Tercera Entrega — revisado tras la reestructuración de
paquetes `academico`/`deportivo`/`seguridad`)

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
| Cifrado en tránsito | TLS 1.3 (nginx `:8443`) | `docs/mediciones/sec/a02-tls.txt` |
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
nombrar: **la baja lógica no es un borrado**. Si un representante ejerciera
el derecho a la supresión de los datos de su representado, el sistema
actualmente **no** tiene un mecanismo para satisfacerlo. Ver hallazgo H-03.

---

## 4. Hallazgos abiertos (honestidad sobre lo que falta)

Se documentan como riesgos reconocidos, no se ocultan.

### H-01 — La cédula se almacena en claro y sin validación

`seguridad.personas.cedula` es `VARCHAR(10)` sin restricción de unicidad,
sin validación de dígito verificador y sin cifrado en reposo. Para un
identificador nacional de un menor, esto es más permisivo de lo deseable.
**Mitigación propuesta:** hacer el campo opcional, validar el formato
ecuatoriano y evaluar cifrado a nivel de columna si se despliega en
producción real.

### H-02 — Las observaciones de texto libre no tienen control de contenido

`deportivo.observaciones_estudiante.texto` es `TEXT` libre sobre un menor,
sin límite de longitud ni guía para el entrenador. Un campo así puede
terminar conteniendo juicios de valor, datos de salud o comentarios
inapropiados.
**Mitigación propuesta:** guía de redacción para entrenadores, límite de
longitud, y visibilidad restringida al entrenador y a la coordinación.

### H-03 — No existe mecanismo de supresión de datos

Ver §3.4. **Mitigación propuesta:** procedimiento almacenado de anonimización
(sustituir datos identificativos por valores neutros conservando las claves
foráneas y las estadísticas agregadas), invocable solo por
`ADMINISTRADOR` y registrado en auditoría.

### H-04 — El consentimiento del representante no está modelado (resuelto parcialmente el 2026-08-03)

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

Sigue **parcial** a propósito: lo que queda abierto es la mitad que este
mismo hallazgo prohíbe construir sin la otra — el envío real de
notificaciones (RF-22 propiamente dicho, push/email/SMS) no está
implementado. La lectura de informes (evaluación diaria y lesiones) por
parte del representante SÍ está disponible, pero se autoriza únicamente por
el vínculo activo `representante_estudiante` (creado por un administrador),
no por un consentimiento vigente: un guardián consultando los datos del hijo
que él mismo matriculó es uso ordinario esperado, categóricamente distinto
del envío proactivo del sistema que este hallazgo señala como el riesgo real
(ver también H-07). La tabla de consentimientos queda reservada
exclusivamente para gatear esa notificación cuando se construya.

### H-05 — Certificado TLS autofirmado

El despliegue actual usa un certificado autofirmado, adecuado para
desarrollo y evaluación pero **no** para producción con datos reales de
menores. Un despliegue real exige certificado emitido por una autoridad
reconocida.

### H-06 — Peso y altura se agregaron sin base legal documentada

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

### H-07 — La plantilla de consentimiento cubre a los evaluadores del SUS, no a los representantes de los menores

`docs/etica/consentimiento/plantilla.md` es un consentimiento informado
bien construido para adultos que participan en la encuesta de usabilidad
(Bloque C.3) — resuelve el consentimiento de *ese* estudio, no el de H-04.
Son dos cosas distintas: uno es el consentimiento de un adulto para
evaluar el sistema; el otro es el consentimiento de un representante para
que los datos de **su hijo o hija menor de edad** sean tratados por el
sistema en producción. H-04 sigue abierto y ahora es más urgente por H-06:
cuantos más datos sensibles trate el sistema, más pesa no tener resuelto el
consentimiento de quien sí puede otorgarlo legalmente.

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

---

## 5. Ética en el desarrollo del proyecto

### 5.1 Datos usados durante el desarrollo

Todos los datos presentes en el repositorio (`db/seed.sql`, evidencias de
`docs/mediciones/`) son **ficticios**, creados para pruebas. No se utilizaron
datos reales de menores en ningún momento del desarrollo, ni en las
mediciones de rendimiento, ni en las auditorías de seguridad.

Las credenciales del usuario administrador sembrado (`admin` /
`Admin2026!`) están documentadas públicamente en el README **a propósito**,
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
