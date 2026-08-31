# Instrumento de evaluación de usabilidad — System Usability Scale (SUS)

**Sistema evaluado:** SGED — Escuela Deportiva ProFútbol
**Instrumento:** System Usability Scale (Brooke, 1996)
**Escala:** Likert de 5 puntos — 1 = Totalmente en desacuerdo · 5 = Totalmente de acuerdo
**Participantes requeridos:** mínimo 10 personas externas al equipo de desarrollo

---

## Estado de esta medición

Recolectada por primera vez el 2026-07-30 con 10 participantes (media SUS
68,25). La muestra se amplió después en dos tandas hasta **15
participantes**, y esa es la medición vigente de esta entrega —
**media SUS 69,33**, detallada en
[`INTERPRETACION.md`](INTERPRETACION.md) y
[`respuestas.csv`](respuestas.csv)—, no la corrida de 10 de más arriba.
La corrida de 10 queda como el primer punto de la serie, no como el
resultado final.

Los formularios en papel originales son la fuente de verdad; este CSV es su
transcripción. No se registran nombres, solo un código de participante
(`ENC-01`…`ENC-10`) y su perfil, siguiendo el mismo principio de anonimato
del consentimiento informado (`docs/etica/consentimiento/plantilla.md`).

**No se deben inventar respuestas.** Una muestra fabricada invalida la
medición y constituye falta de honestidad académica. El análisis
(`scripts/sus-analysis.py`) rechaza deliberadamente ejecutarse con menos de
10 participantes reales — la muestra actual cumple ese mínimo exactamente.

---

## Protocolo de aplicación

1. **Perfil de participantes.** Personas externas al equipo de desarrollo.
   Idealmente representativas de los actores reales: personal
   administrativo, entrenadores, o estudiantes de otras carreras que no
   conozcan el sistema.

2. **Tareas previas.** Antes de responder, cada participante debe intentar
   completar estas tareas sin ayuda:

   | # | Tarea | Requisito relacionado |
   |---|---|---|
   | T1 | Iniciar sesión con las credenciales entregadas | RF-02 |
   | T2 | Encontrar cuántos estudiantes hay registrados en total | RF-08 |
   | T3 | Registrar un estudiante nuevo en la categoría SUB-15 | RF-10 |
   | T4 | Intentar registrar un estudiante con categoría "JUVENIL" y explicar qué pasó | RF-11 |
   | T5 | Modificar la categoría de un estudiante existente | RF-12 |
   | T6 | Dar de baja a un estudiante | RF-13 |
   | T7 | Cerrar sesión | RF-03 |

3. **Registro.** Anotar por participante: si completó cada tarea (sí/no) y el
   tiempo aproximado. Luego aplicar el cuestionario.

4. **Condiciones.** El sistema debe estar levantado con `make up`. Cada
   participante usa datos de prueba, nunca datos reales de menores
   (ver `docs/etica/ETHICS.md` §5.1).

---

## Cuestionario SUS (10 ítems)

Responder cada afirmación del 1 al 5.

| # | Afirmación | Polaridad |
|---|---|---|
| 1 | Creo que me gustaría usar este sistema con frecuencia. | Positiva |
| 2 | Encontré el sistema innecesariamente complejo. | Negativa |
| 3 | Pensé que el sistema era fácil de usar. | Positiva |
| 4 | Creo que necesitaría el apoyo de una persona con conocimientos técnicos para poder usar este sistema. | Negativa |
| 5 | Encontré que las diversas funciones del sistema estaban bien integradas. | Positiva |
| 6 | Pensé que había demasiada inconsistencia en este sistema. | Negativa |
| 7 | Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente. | Positiva |
| 8 | Encontré el sistema muy engorroso de usar. | Negativa |
| 9 | Me sentí muy seguro/a usando el sistema. | Positiva |
| 10 | Necesité aprender muchas cosas antes de poder empezar a usar este sistema. | Negativa |

---

## Cálculo de la puntuación

Para cada participante:

1. **Ítems impares (1, 3, 5, 7, 9):** contribución = `respuesta − 1`
2. **Ítems pares (2, 4, 6, 8, 10):** contribución = `5 − respuesta`
3. **Puntuación SUS** = suma de las 10 contribuciones × 2,5 → rango 0–100

> La puntuación SUS **no es un porcentaje**. Un 68 no significa "68 % de
> satisfacción": es el percentil 50 de la distribución de referencia.

## Interpretación (Bangor, Kortum y Miller, 2009 · Sauro, 2011)

| Rango SUS | Grado | Adjetivo | Percentil aprox. |
|---|---|---|---|
| 85 – 100 | A | Excelente | > 96 |
| 72 – 84,9 | B | Bueno | 70 – 96 |
| 68 – 71,9 | C | Aceptable (media de la industria) | ~50 |
| 51 – 67,9 | D | Pobre | 15 – 50 |
| 0 – 50,9 | F | Inaceptable | < 15 |

**Umbral objetivo de este proyecto:** SUS ≥ 68 (media de la industria).

---

## Cómo registrar y analizar

1. Volcar las respuestas en [`respuestas.csv`](respuestas.csv), una fila por
   participante.
2. Ejecutar el análisis:

```bash
python scripts/sus-analysis.py
```

Genera `docs/mediciones/sus/REPORT.md` con media, desviación típica,
intervalo de confianza al 95 %, mediana, mínimo, máximo, distribución por
grado e interpretación.

---

## Referencias

- Brooke, J. (1996). *SUS: A "quick and dirty" usability scale.* En
  *Usability Evaluation in Industry*, Taylor & Francis.
- Bangor, A., Kortum, P. y Miller, J. (2009). *Determining what individual
  SUS scores mean: adding an adjective rating scale.* Journal of Usability
  Studies, 4(3), 114–123.
- Sauro, J. (2011). *A Practical Guide to the System Usability Scale.*
  Measuring Usability LLC.
- ISO/IEC 25010:2011 — característica de calidad *Usabilidad*.
