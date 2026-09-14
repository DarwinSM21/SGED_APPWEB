# Registro de consentimientos — evaluación SUS

Constancia de que cada participante de `docs/mediciones/sus/respuestas.csv`
dio su consentimiento informado según [`plantilla.md`](plantilla.md), sin
exponer datos personales en el repositorio público (nombre, cédula o firma
del participante **no van aquí** — el original firmado se archiva fuera del
control de versiones, según la nota de cierre de `plantilla.md`).

Este registro es lo que permite comprobar el punto P13 sin publicar datos
identificables: enlaza el número de participante anónimo (el mismo que usa
`respuestas.csv`) con la fecha en que se obtuvo su consentimiento y dónde
está archivado el original.

**Completar una fila por participante** a medida que se recolecta (o se
confirma retroactivamente) cada constancia. `scripts/verify.sh` comprueba
que las 15 filas existan y que ninguna quede en `PENDIENTE`.

| Participante | Perfil | Fecha de la encuesta | Consentimiento | Fecha de la constancia | Archivo (ruta fuera del repo) |
|---|---|---|---|---|---|
| ENC-01 | entrenador | 2026-07-30 | PENDIENTE | | |
| ENC-02 | recepcionista | 2026-07-30 | PENDIENTE | | |
| ENC-03 | estudiante | 2026-07-30 | PENDIENTE | | |
| ENC-04 | representante | 2026-07-30 | PENDIENTE | | |
| ENC-05 | entrenador | 2026-07-30 | PENDIENTE | | |
| ENC-06 | recepcionista | 2026-07-30 | PENDIENTE | | |
| ENC-07 | estudiante | 2026-07-30 | PENDIENTE | | |
| ENC-08 | representante | 2026-07-30 | PENDIENTE | | |
| ENC-09 | entrenador | 2026-07-30 | PENDIENTE | | |
| ENC-10 | recepcionista | 2026-07-30 | PENDIENTE | | |
| ENC-13 | estudiante | 2026-08-18 | PENDIENTE | | |
| ENC-14 | representante | 2026-08-18 | PENDIENTE | | |
| ENC-15 | estudiante | 2026-08-18 | PENDIENTE | | |
| ENC-16 | representante | 2026-08-18 | PENDIENTE | | |
| ENC-17 | entrenador | 2026-08-18 | PENDIENTE | | |

**Valores válidos para "Consentimiento":**
- `PENDIENTE` — todavía no hay constancia (estado inicial de esta plantilla).
- `OBTENIDO` — existe constancia firmada, archivada fuera del repositorio;
  completar fecha y ruta (aunque sea una ruta local o institucional, no
  tiene que ser accesible públicamente — es solo para que el equipo la
  ubique).
- `NO DISPONIBLE` — el participante ya no puede firmar retroactivamente
  (caso a resolver con el docente-director: ver nota abajo).

## Si la encuesta ya se hizo sin recoger la constancia en su momento

`plantilla.md` está diseñada para firmarse **antes o durante** la sesión de
evaluación. Como las 15 encuestas de este registro ya se administraron
(2026-07-30 y 2026-08-18), si en su momento no se archivó la constancia
firmada, las opciones son:

1. **Volver a contactar a cada participante** y hacerle firmar `plantilla.md`
   ahora, aclarando que es una formalización retroactiva del consentimiento
   ya otorgado verbalmente al participar. Es lo más alineado con lo que pide
   la guía.
2. Si algún participante ya no está disponible, **decírselo directamente al
   docente-director** antes del cierre en vez de inventar o forzar una
   constancia — según Piso 3 de la guía, una constancia fabricada deja la
   calificación en cero sin segunda oportunidad, y ese riesgo es muchísimo
   peor que declarar un dato faltante.
