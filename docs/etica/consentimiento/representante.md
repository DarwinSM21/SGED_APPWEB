# Consentimiento informado del representante legal — Tratamiento de datos de un menor en SGED

**Sistema:** SGED — Sistema de Gestión para la Escuela Deportiva ProFútbol
**Responsable del tratamiento:** Escuela Deportiva ProFútbol (institución que
opera el sistema).
**Desarrollo del sistema:** equipo de la Carrera de Ingeniería en Software,
Universidad Técnica Estatal de Quevedo (UTEQ) — proyecto académico
(ver [`../../../CONTRIBUTORS.md`](../../../CONTRIBUTORS.md)).

> Este documento es el consentimiento que **el representante legal** de una
> niña, niño o adolescente (NNA) firma para que sus datos sean tratados por el
> sistema SGED en producción. Es distinto del consentimiento de la evaluación
> de usabilidad, que está en
> [`plantilla.md`](plantilla.md) y va dirigido a adultos evaluadores.

---

## 1. Por qué se pide este consentimiento

La persona a inscribir es **menor de edad** y no puede otorgar por sí misma un
consentimiento informado válido. La **Ley Orgánica de Protección de Datos
Personales del Ecuador (LOPDP, 2021)** exige, para tratar datos de NNA, el
consentimiento de su **representante legal**, y la **Constitución del Ecuador
(Art. 66, num. 19)** reconoce el derecho a la protección de datos personales.
El **Código de la Niñez y Adolescencia** obliga a actuar según el interés
superior del niño.

---

## 2. Datos que trata el sistema y para qué

| Dato | Categoría | Finalidad |
|---|---|---|
| Nombre y apellido del NNA | Identificativo | Identificar al deportista. |
| Cédula (opcional) | Identificativo | Identificación unívoca ante federaciones, si se proporciona. |
| Fecha de nacimiento | Identificativo (revela minoría de edad) | Asignar la categoría deportiva por rango de edad. |
| Correo y teléfono de contacto | Contacto | Comunicación con el representante. |
| Código de estudiante / código RFID | Identificador interno | Ficha única y marcaje de asistencia. |
| Asistencia con fecha y hora | Localización temporal | Control de asistencia a los entrenamientos. |
| Puntajes de evaluación (técnica, físico, táctica, actitud) | Perfilado de desempeño | Seguimiento formativo del deportista. |
| Observaciones del entrenador (texto libre) | Texto libre | Notas cualitativas de seguimiento (con guía de redacción y tope de longitud). |
| Peso y altura (opcional) | **Dato de salud** | Seguimiento físico-deportivo — **requiere autorización separada** (sección 4). |

El sistema **no** recoge historial médico, datos de alimentación, contextura
corporal ni ningún otro dato de salud fuera de peso y altura.

---

## 3. Límites de uso (finalidad limitada)

Los datos se usan **solo** para el seguimiento formativo del deportista dentro
de la escuela. **No se usarán** para:

- ceder o vender información a terceros (clubes, ojeadores, patrocinadores,
  federaciones) sin la autorización separada de la sección 4;
- construir rankings públicos de menores identificables;
- tomar decisiones automatizadas sobre el NNA sin revisión de una persona.

---

## 4. Autorizaciones — marque cada una por separado

El tratamiento base (inscripción, categoría, asistencia y evaluación
formativa) es **necesario para prestar el servicio deportivo**; sin él no es
posible inscribir al NNA. Las demás autorizaciones son **opcionales** y
**revocables en cualquier momento** sin afectar la inscripción.

| | Autorización | Alcance registrado en el sistema |
|---|---|---|
| ☐ | **Tratamiento base** (obligatorio): inscripción, categoría, asistencia y evaluación formativa. | — |
| ☐ | Recibir **informes de rendimiento** de mi representado. | `INFORMES` |
| ☐ | Recibir **notificaciones cuando marque asistencia**. | `NOTIFICACIONES_ASISTENCIA` |
| ☐ | Recibir **notificaciones cuando se registre una lesión**. | `NOTIFICACIONES_LESION` |
| ☐ | Tratamiento de **peso y altura** para seguimiento físico-deportivo. | `DATOS_FISICO_DEPORTIVOS` |
| ☐ | **Cesión de datos a federaciones o clubes** para trámites deportivos concretos que yo solicite. | (cesión puntual, registrada aparte) |

---

## 5. Conservación y supresión

- Los datos se conservan mientras el NNA esté **activo** en la escuela.
- La baja de la escuela aplica **baja lógica** (el registro se marca inactivo)
  para preservar la integridad del historial deportivo; **no es un borrado**.
- A solicitud del representante, la escuela ejecuta un procedimiento de
  **anonimización** que sustituye los datos identificativos (nombre, apellido,
  cédula, correo, teléfono, fecha de nacimiento) por valores neutros y borra
  el texto libre, conservando solo estadísticas agregadas no identificables.

---

## 6. Derechos del representante (LOPDP)

Como representante legal puede, en cualquier momento y sin costo:

- **Acceder** a los datos que el sistema guarda sobre su representado y
  descargarlos en PDF.
- **Rectificar** datos inexactos.
- **Solicitar la supresión / anonimización** de los datos.
- **Revocar** cualquiera de las autorizaciones de la sección 4; la revocación
  queda registrada con su fecha y detiene el uso correspondiente hacia
  adelante.
- **Oponerse** a un tratamiento concreto y presentar reclamo ante la autoridad
  de protección de datos.

Para ejercerlos, diríjase al responsable indicado en la sección 9.

---

## 7. Seguridad

El sistema cifra el tráfico con TLS, almacena las contraseñas solo como hash
irreversible (BCrypt), restringe el acceso a los datos por rol (el personal de
recepción no ve peso ni altura), y registra en una bitácora de auditoría quién
accede o modifica cada dato.

---

## 8. Voluntariedad y registro

La firma de este documento es **voluntaria**. Un `ADMINISTRADOR` de la escuela
registra en el sistema, a partir de este formulario, las autorizaciones
marcadas en la sección 4 (fecha, alcance y quién lo registró). El original
firmado se archiva **fuera del sistema**, en un expediente físico o carpeta
institucional de acceso restringido; **no se sube al repositorio de código**.
Se entrega una copia al representante.

---

## 9. Responsable y contacto

Consultas y ejercicio de derechos sobre el tratamiento de datos:
la Escuela Deportiva ProFútbol como responsable del tratamiento. En el marco
del proyecto académico, el equipo de desarrollo de la UTEQ
([`../../../CONTRIBUTORS.md`](../../../CONTRIBUTORS.md)).

---

## 10. Declaración de consentimiento

Yo, __________________________________________________, con cédula
__________________, en calidad de **representante legal** de la niña / niño /
adolescente __________________________________________________, declaro que:

- He leído y comprendido este documento y he podido hacer preguntas.
- Entiendo qué datos se tratan, con qué finalidad y por cuánto tiempo.
- Entiendo que el tratamiento base es necesario para el servicio y que las
  autorizaciones de la sección 4 son opcionales y revocables.
- Conozco los derechos de la sección 6 y cómo ejercerlos.

Autorizo el tratamiento en los términos marcados en la sección 4.

Firma del representante legal: _________________________________

Fecha: ____ / ____ / ______

---

Recibido y registrado por (personal de la escuela):

Nombre: _________________________________   Rol: ____________________

Firma: _________________________________   Fecha: ____ / ____ / ______

---

*El documento firmado contiene datos personales identificables del
representante y del menor. Por eso **no se archiva en el repositorio público**
de SGED_APPWEB: se guarda en el expediente institucional de acceso restringido
de la escuela. En el sistema solo quedan las autorizaciones estructuradas
(tabla `academico.consentimientos`: representante, estudiante, alcance, fecha,
quién registró, fecha de revocación).*
