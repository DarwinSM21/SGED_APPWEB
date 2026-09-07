# Registro de búsqueda — Trabajos relacionados (PRISMA)

`registro-busqueda.csv` archiva las **6 cadenas de búsqueda** que
`docs/informe/main.tex` (sección "Estrategia de búsqueda",
`\label{sec:estrategia-busqueda}`) transcribe textualmente, una por cada
uno de los 6 ejes temáticos.

## Limitación honesta, declarada aquí y no escondida

El propio texto dice que se "ejecutaron nueve búsquedas... en seis ejes
temáticos" — pero solo documenta 6 cadenas (una representativa por eje),
no las 9 ejecuciones individuales. Este CSV no completa ese hueco: no hay
un registro de las 3 búsquedas adicionales (variantes de las mismas
cadenas, según recuerda el equipo, pero sin la cadena exacta ni el
resultado crudo por búsqueda anotados en su momento).

Tampoco existe, para ninguna de las 9 búsquedas, un conteo de resultados
por motor/base de datos archivado — el propio `main.tex` ya lo advierte:
los números de la Figura PRISMA (`n≈85`, `n≈28`, `n=12`, `n=8`) son "una
reconstrucción honesta del registro de búsqueda del equipo, no el
resultado de una herramienta de gestión bibliográfica formal". Este CSV
no le agrega precisión a esos conteos agregados — sería inventar datos
que nunca se capturaron. Lo único que hace es sacar las 6 cadenas de
búsqueda reales de la prosa del informe y ponerlas en un formato
estructurado y versionado, para que quien audite no tenga que parsear
LaTeX para encontrarlas.
