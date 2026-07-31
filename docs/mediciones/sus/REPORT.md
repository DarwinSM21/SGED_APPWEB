# Reporte de usabilidad — SUS (Bloque C.3)

- Fecha del analisis: 2026-07-31T01:24:08.099042+00:00
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

## Referencias

- Brooke, J. (1996). *SUS: A quick and dirty usability scale.*
- Bangor, A., Kortum, P. y Miller, J. (2009). *Determining what individual SUS scores mean.* Journal of Usability Studies, 4(3).
- ISO/IEC 25010:2011 — Usabilidad.