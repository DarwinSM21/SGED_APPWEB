# Reporte de usabilidad — SUS (Bloque C.3)

- Fecha del analisis: 2026-07-31T01:24:08.099042+00:00
- Revision (amenazas a la validez): 2026-08-14
- Commit: 5176ad4
- Instrumento: System Usability Scale (Brooke, 1996), 10 items, escala 1-5
- Participantes: **10** (minimo exigido: 10)

## Resultado agregado

| Metrica | Valor |
|---|---|
| Media SUS | **68.25** |
| Desviacion tipica | 22.14 |
| IC 95 % | 68.25 ± 13.72  (54.53 – 81.97) |
| Mediana | 73.75 |
| Minimo | 37.50 |
| Maximo | 90.00 |
| Grado | **C — Aceptable** |

Umbral objetivo del proyecto: SUS >= 68 (media de la industria). Resultado: **CUMPLE**.

## Distribucion por grado

| Grado | Participantes |
|---|---|
| A | 5 |
| D | 2 |
| F | 3 |

## Puntuaciones individuales

| Participante | Perfil | SUS | Grado |
|---|---|---|---|
| ENC-01 | entrenador | 85.0 | A |
| ENC-02 | recepcionista | 62.5 | D |
| ENC-03 | estudiante | 90.0 | A |
| ENC-04 | padre de familia | 37.5 | F |
| ENC-05 | entrenador | 85.0 | A |
| ENC-06 | recepcionista | 37.5 | F |
| ENC-07 | estudiante | 90.0 | A |
| ENC-08 | padre de familia | 50.0 | F |
| ENC-09 | entrenador | 90.0 | A |
| ENC-10 | recepcionista | 55.0 | D |

## Interpretacion

Con 10 participantes, el sistema obtiene una media SUS de 68,25 — cruza el
umbral de 68 por apenas 0,25 puntos. El intervalo de confianza al 95 %
(54,53–81,97) es ancho: con esta muestra no se puede afirmar con confianza
estadistica que la media poblacional real este por encima de 68, solo que
68,25 es la mejor estimacion puntual disponible. No se reporta como un
"aprobado" comodo — se reporta con el margen real.

**Patron por perfil (hallazgo real, no ruido aleatorio):** la distribucion
es marcadamente bimodal y se explica casi por completo por el rol del
participante, no por variacion individual:

| Perfil | Participantes | Puntuaciones SUS | Promedio del perfil |
|---|---|---|---|
| Entrenador | 3 | 85,0 / 85,0 / 90,0 | 86,7 (A) |
| Estudiante | 2 | 90,0 / 90,0 | 90,0 (A) |
| Recepcionista | 3 | 62,5 / 37,5 / 55,0 | 51,7 (F) |
| Padre de familia | 2 | 37,5 / 50,0 | 43,75 (F) |

Entrenadores y estudiantes calificaron el sistema como excelente; recepcionistas
y padres de familia lo calificaron como inaceptable o pobre. Dado que el
CRUD de estudiantes implementado hasta esta entrega (RF-08 a RF-15) es
exactamente el flujo que opera un rol administrativo (dar de alta, editar,
buscar estudiantes) — no el de un entrenador consultando informacion ni el
de un padre de familia buscando visibilidad sobre su representado — el
resultado sugiere que la interfaz actual esta optimizada para un uso que
todavia no es el principal caso de uso de quien la calificó peor. Este
patron es mas informativo que la media agregada: apunta a revisar
usabilidad especificamente para el flujo administrativo y para un futuro
modulo de consulta orientado a representantes (RF-22, hoy sin implementar)
antes de la Entrega Final.

**Nota metodologica:** los perfiles recolectados (entrenador, recepcionista,
estudiante, padre de familia) son mas especificos que las tres categorias
genericas anticipadas en el instrumento original (administrativo,
entrenador, externo) — ver `INSTRUMENTO-SUS.md`. Se preservan tal como se
recolectaron por ser mas informativas, no se fuerzan a las categorias
originales.

Grado agregado: **C (Aceptable)** en la escala adjetival de Bangor, Kortum
y Miller (2009) — pero la agregación esconde el patrón bimodal real.

## Amenazas a la validez

### Tamanho de muestra

n = 10 es el minimo exigido por el instrumento SUS (Brooke, 1996) y el
minimo recomendado por Bangor et al. (2009) para producir estimaciones
estables de la media. Sin embargo, una muestra de 10 no permite
generalizar con confianza a la poblacion total de usuarios del sistema.
Se recomienda ampliar a n >= 20 en futuras iteraciones.

### Intervalo de confianza amplio

El IC 95 % (54,53 -- 81,97) es considerablemente amplio: abarca un
rango de 27 puntos. Esto significa que la verdadera media poblacional
podria estar en cualquier lugar dentro de ese rango, incluyendo valores
significativamente por debajo del umbral de 68. La estimacion puntual
de 68,25 es la mejor estimacion disponible, pero no debe interpretarse
como un resultado definitivo.

### Sesgo de seleccion

Los participantes fueron contactados directamente por los miembros del
equipo. No se aplico ningun mecanismo de muestreo aleatorio. Esto
introduce un sesgo de seleccion: los participantes que aceptaron
responder podrian tener una opinion mas favorable (o mas desfavorable)
que el promedio de la poblacion objetivo. El protocolo de consentimiento
informado (docs/etica/consentimiento/plantilla.md) mitiga parcialmente
este riesgo al garantizar participacion voluntaria.

### Distribucion bimodal

La media agregada de 68,25 oculta una diferencia sustancial entre
perfiles: entrenadores y estudiantes (promedio 87-90, grado A) versus
recepcionistas y padres de familia (promedio 44-52, grado F). Esta
distribucion bimodal indica que el sistema no es igualmente usable
para todos los perfiles de usuario. La media agregada no captura esta
realidad y debe interpretarse con cautela.

### Amenazas externas

- **Efecto halo:** participantes que conocen al equipo podrian haber
  inflado sus respuestas por cortesia.
- **Sesgo de recencia:** la experiencia reciente de uso podria haber
  influenciado las respuestas mas que la experiencia acumulada.
- **Ausencia de medicion comparativa:** no se aplica un pre-test/post-
  test, por lo que no se puede determinar si la usabilidad mejora o
  empeora con el uso repetido.

### Conclusion sobre la validez

Con n = 10 y un IC amplio, el resultado SUS de 68,25 se reporta como
una **estimacion puntual con margen de incertidumbre**, no como un
resultado definitivo. El cumplimiento del umbral de 68 es real pero
marginal (+0,25). La recomendacion principal es ampliar la muestra y
reducir el IC antes de hacer afirmaciones concluyentes sobre la
usabilidad del sistema.

## Referencias

- Brooke, J. (1996). *SUS: A quick and dirty usability scale.*
- Bangor, A., Kortum, P. y Miller, J. (2009). *Determining what individual SUS scores mean.* Journal of Usability Studies, 4(3).
- ISO/IEC 25010:2011 — Usabilidad.