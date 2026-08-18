# Interpretación del SUS — análisis escrito

`REPORT.md` lo **regenera** `scripts/sus-analysis.py` en cada corrida: todo lo
que se escriba allí a mano se pierde al agregar un participante. Por eso el
análisis vive aquí, donde ningún script lo sobrescribe. Los números de este
documento corresponden a la corrida con **n = 14** (2026-08-18); si se agregan
respuestas, hay que actualizarlos a mano.

## Resultado agregado

Con 14 participantes externos, la media SUS es **68,57** (IC 95 %
58,43 – 78,71), grado **C (Aceptable)** en la escala adjetival de Bangor,
Kortum y Miller (2009).

Cruza el umbral de 68 por 0,57 puntos. No se reporta como un aprobado cómodo:
el intervalo de confianza todavía incluye valores por debajo de 68, así que la
afirmación defendible es "la mejor estimación puntual está apenas sobre el
umbral", no "el sistema supera el umbral".

Sí mejoró la precisión respecto de la muestra anterior. Con n = 10 el IC
abarcaba 27 puntos (54,53 – 81,97); con n = 14 abarca 20 (58,43 – 78,71). La
media casi no se movió (68,25 → 68,57), lo que sugiere que la estimación es
estable y que lo que cambió fue la certeza, no el resultado.

## Patrón por perfil — el hallazgo principal

La distribución es marcadamente bimodal y se explica por el **rol** del
participante, no por variación individual:

| Perfil | n | Puntuaciones SUS | Promedio |
|---|---|---|---|
| Entrenador | 3 | 85,0 / 85,0 / 90,0 | **86,7** |
| Estudiante | 4 | 70,0 / 85,0 / 90,0 / 90,0 | **83,8** |
| Representante | 4 | 37,5 / 50,0 / 55,0 / 67,5 | **52,5** |
| Recepcionista | 3 | 37,5 / 55,0 / 62,5 | **51,7** |

Tras unificar las etiquetas (ver nota metodológica), la distribución queda
partida en dos grupos casi sin dispersión interna: dos perfiles alrededor de
85 y dos alrededor de 52. La diferencia dentro de cada grupo es de 2,9 y 0,8
puntos respectivamente; entre grupos, de **35 puntos** — más de una vez y
media la amplitud del intervalo de confianza. No es ruido de muestreo.

Lo que separa a los dos grupos no es ser interno o externo a la escuela: el
recepcionista es personal interno y de uso diario, y puntúa igual de bajo que
el representante. La línea divisoria es **qué hace cada rol con el sistema**.
Entrenador y estudiante sobre todo *consultan* —ver sesiones, marcar
asistencia, revisar el equipo—, y esos son los flujos que más atención de
diseño recibieron. Recepcionista y representante *operan o esperan
información*: el primero da de alta, edita y cobra; el segundo busca
visibilidad sobre su representado y hoy el sistema no le envía nada por
iniciativa propia (RF-22 sigue sin implementarse; solo existe el registro de
consentimiento que lo habilitará).

Dicho de otro modo: quien peor califica el sistema es quien más trabajo
administrativo hace con él.

Los cuatro participantes nuevos **no alteran** el patrón: los dos estudiantes
(70,0 y 85,0) se suman al grupo alto y los dos representantes (55,0 y 67,5)
al bajo. Que sobreviva a una ampliación del 40 % de la muestra lo refuerza.

## Amenazas a la validez

### Tamaño de muestra

n = 14 supera el mínimo de 10 del instrumento (Brooke, 1996), pero **no alcanza
los 15 participantes** que exige la guía de la Entrega Final. Falta uno. Se
reporta así en lugar de presentar la muestra como suficiente.

Para los subgrupos el problema es mayor: con 2 o 3 participantes por perfil,
los promedios por rol son indicativos y no soportan una prueba de hipótesis
entre grupos. El patrón bimodal se sostiene por su magnitud (35 puntos) y por
su consistencia al ampliar la muestra, no por significancia estadística.

### Intervalo de confianza

El IC 95 % abarca 20 puntos e incluye valores por debajo del umbral de 68. La
estimación puntual es la mejor disponible, no un resultado concluyente.

### Sesgo de selección

Los participantes fueron contactados directamente por el equipo, sin muestreo
aleatorio. Quienes aceptaron podrían tener una opinión sistemáticamente
distinta del promedio de la población objetivo. El consentimiento informado
(`docs/etica/consentimiento/plantilla.md`) garantiza voluntariedad, pero no
elimina este sesgo.

### Media agregada frente a distribución bimodal

Reportar 68,57 como resultado único describe mal el sistema: ningún perfil
puntúa cerca de 68. Es el promedio de dos poblaciones distintas, y por eso la
tabla por perfil se reporta junto a la media y no como anexo.

## Notas metodológicas

**Etiquetas de perfil.** Los perfiles recolectados (entrenador, estudiante,
recepcionista, representante) son más específicos que las tres categorías
genéricas del instrumento original (administrativo, entrenador, externo). Se
preservan por ser más informativas, en vez de forzarlas a las categorías
originales. La única corrección aplicada es la unificación descrita abajo.

**Unificación de `padre de familia` en `representante` (2026-08-18).** Las
respuestas llegaron en dos tandas con etiquetas distintas para el mismo rol:
ENC-04 y ENC-08 como `padre de familia`, ENC-14 y ENC-16 como `representante`.
Se unificaron bajo `representante`, que es como el sistema nombra ese rol,
**por confirmación explícita de quien aplicó los cuestionarios**; no fue una
decisión de análisis.

El cambio afecta solo a la etiqueta: ninguna puntuación se modificó, y la
media global, la desviación y el intervalo de confianza son idénticos antes y
después (68,57 / 19,36 / ±10,14). Lo que cambia es la lectura por perfil: el
grupo pasa de dos subgrupos de n=2 (43,8 y 61,2) a uno de n=4 con media 52,5,
que queda junto a recepcionista en vez de aparentar una posición intermedia.
La versión previa de este documento describía a los representantes como
situados "entre los dos extremos" — con la muestra unificada eso ya no es
cierto y se corrige aquí.

**Numeración.** Los identificadores van de ENC-01 a ENC-10 y luego de ENC-13 a
ENC-16: **no existen ENC-11 ni ENC-12**. El salto se conserva tal como llegaron
los datos en vez de renumerar, para no dar la impresión de una serie continua
que no lo es. Si esos dos cuestionarios existen y no se han volcado, agregarlos
cerraría además el requisito de 15.
