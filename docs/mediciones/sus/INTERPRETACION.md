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
| Estudiante | 4 | 90,0 / 90,0 / 70,0 / 85,0 | **83,8** |
| Representante | 2 | 55,0 / 67,5 | **61,2** |
| Recepcionista | 3 | 62,5 / 37,5 / 55,0 | **51,7** |
| Padre de familia | 2 | 37,5 / 50,0 | **43,8** |

Entrenadores y estudiantes califican el sistema como excelente; recepcionistas
y padres de familia, como pobre o inaceptable. La brecha entre el perfil mejor
y el peor evaluado es de **43 puntos**, más del doble de la amplitud del
intervalo de confianza: no es ruido de muestreo.

La explicación más plausible es de alcance funcional, no de estética. El
flujo que un rol administrativo opera a diario —dar de alta, editar y buscar
estudiantes, registrar pagos— recibió menos atención de diseño que las
pantallas de consulta que usan entrenador y estudiante. Quien peor califica el
sistema es justamente quien más lo usa.

Los cuatro participantes nuevos **no alteran** este patrón: los dos estudiantes
(70,0 y 85,0) se suman al grupo alto y los dos representantes (55,0 y 67,5) al
bajo. Que el patrón sobreviva a una ampliación del 40 % de la muestra lo
refuerza.

Vale la pena notar dónde caen los representantes: 61,2, entre los dos extremos.
Es el perfil al que apunta RF-22 (notificaciones), que sigue sin implementarse
—solo existe el registro de consentimiento que lo habilitará—. Su puntuación
intermedia es coherente con un rol que hoy puede consultar informes pero no
recibe nada del sistema por iniciativa propia.

## Amenazas a la validez

### Tamaño de muestra

n = 14 supera el mínimo de 10 del instrumento (Brooke, 1996), pero **no alcanza
los 15 participantes** que exige la guía de la Entrega Final. Falta uno. Se
reporta así en lugar de presentar la muestra como suficiente.

Para los subgrupos el problema es mayor: con 2 o 3 participantes por perfil,
los promedios por rol son indicativos y no soportan una prueba de hipótesis
entre grupos. El patrón bimodal se sostiene por su magnitud (43 puntos) y por
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

**Etiquetas de perfil.** Los perfiles recolectados son más específicos que las
tres categorías genéricas del instrumento original (administrativo, entrenador,
externo). Se preservan tal como se recolectaron.

Queda una inconsistencia sin resolver: `padre de familia` (n = 2) y
`representante` (n = 2) designan probablemente el mismo rol —el sistema lo
llama REPRESENTANTE— y están registrados por separado porque así se
recolectaron en tandas distintas. Unificarlos daría un solo grupo de n = 4 con
promedio 52,5. **No se unifican sin confirmarlo con quien aplicó los
cuestionarios**, porque reetiquetar datos ya recolectados para que cuadren
mejor es exactamente lo que un análisis honesto no debe hacer.

**Numeración.** Los identificadores van de ENC-01 a ENC-10 y luego de ENC-13 a
ENC-16: **no existen ENC-11 ni ENC-12**. El salto se conserva tal como llegaron
los datos en vez de renumerar, para no dar la impresión de una serie continua
que no lo es. Si esos dos cuestionarios existen y no se han volcado, agregarlos
cerraría además el requisito de 15.
