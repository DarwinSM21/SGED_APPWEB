#!/usr/bin/env python3
"""
Análisis de la encuesta de usabilidad SUS (Bloque C.3).

Lee docs/mediciones/sus/respuestas.csv, calcula la puntuación SUS de cada
participante según Brooke (1996), y genera docs/mediciones/sus/REPORT.md con
media, desviación típica, IC 95 %, mediana, rango, distribución por grado e
interpretación adjetival (Bangor et al., 2009).

Se niega deliberadamente a ejecutarse con menos de 10 participantes: la guía
exige un mínimo de 10 personas externas, y un informe con menos muestra
carece de validez.
"""
import csv
import math
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

SUS_DIR = Path("docs/mediciones/sus")
RESPUESTAS = SUS_DIR / "respuestas.csv"
REPORTE = SUS_DIR / "REPORT.md"

MINIMO_PARTICIPANTES = 10
IMPARES = ["p1", "p3", "p5", "p7", "p9"]   # polaridad positiva
PARES = ["p2", "p4", "p6", "p8", "p10"]    # polaridad negativa
TAREAS = ["t1", "t2", "t3", "t4", "t5", "t6", "t7"]

# t de Student, dos colas, alfa=0.05 (t_{0.975, gl}), por grados de libertad
# n-1. Con una muestra de este tamano usar 1.96 -el valor de la normal, valido
# solo cuando gl es grande- ensancha el intervalo menos de lo que corresponde.
# No hay scipy en este entorno; esta es la tabla estandar de cualquier texto
# de estadistica, verificable termino a termino contra ella. Mas alla de
# gl=30 la diferencia con 1.96 es menor al 1 %, así que ahí se usa la normal.
T_CRITICO_95 = {
    1: 12.706, 2: 4.303, 3: 3.182, 4: 2.776, 5: 2.571,
    6: 2.447, 7: 2.365, 8: 2.306, 9: 2.262, 10: 2.228,
    11: 2.201, 12: 2.179, 13: 2.160, 14: 2.145, 15: 2.131,
    16: 2.120, 17: 2.110, 18: 2.101, 19: 2.093, 20: 2.086,
    21: 2.080, 22: 2.074, 23: 2.069, 24: 2.064, 25: 2.060,
    26: 2.056, 27: 2.052, 28: 2.048, 29: 2.045, 30: 2.042,
}


def t_critico(grados_libertad):
    if grados_libertad in T_CRITICO_95:
        return T_CRITICO_95[grados_libertad]
    if grados_libertad > 30:
        return 1.960
    return T_CRITICO_95[1]  # gl=0 no deberia ocurrir; MINIMO_PARTICIPANTES=10 lo evita


def commit_corto():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"], text=True).strip()
    except Exception:
        return "sin-git"


def grado(sus):
    """Escala adjetival de Bangor, Kortum y Miller (2009)."""
    if sus >= 85:
        return "A", "Excelente"
    if sus >= 72:
        return "B", "Bueno"
    if sus >= 68:
        return "C", "Aceptable"
    if sus >= 51:
        return "D", "Pobre"
    return "F", "Inaceptable"


def leer_respuestas():
    if not RESPUESTAS.exists():
        sys.exit(f"No existe {RESPUESTAS}. Ver docs/mediciones/sus/INSTRUMENTO-SUS.md.")

    filas = []
    with RESPUESTAS.open(encoding="utf-8") as f:
        # Se descartan las líneas de comentario que empiezan con '#'
        limpio = (linea for linea in f if not linea.lstrip().startswith("#"))
        for fila in csv.DictReader(limpio):
            if not fila.get("participante", "").strip():
                continue
            filas.append(fila)
    return filas


def puntuacion_sus(fila):
    """0-100. Impares: respuesta-1. Pares: 5-respuesta. Suma x 2.5."""
    total = 0
    for clave in IMPARES:
        total += int(fila[clave]) - 1
    for clave in PARES:
        total += 5 - int(fila[clave])
    return total * 2.5


def main():
    filas = leer_respuestas()
    n = len(filas)

    if n < MINIMO_PARTICIPANTES:
        sys.exit(
            f"Solo hay {n} participante(s) en {RESPUESTAS}.\n"
            f"La guia exige un minimo de {MINIMO_PARTICIPANTES} personas externas "
            f"al equipo.\n"
            f"No se genera el reporte: una muestra insuficiente o inventada "
            f"invalida la medicion."
        )

    puntuaciones = []
    for fila in filas:
        try:
            puntuaciones.append(puntuacion_sus(fila))
        except (KeyError, ValueError) as exc:
            sys.exit(f"Fila invalida ({fila.get('participante', '?')}): {exc}")

    media = sum(puntuaciones) / n
    varianza = sum((x - media) ** 2 for x in puntuaciones) / (n - 1)
    dt = math.sqrt(varianza)
    ic95 = t_critico(n - 1) * dt / math.sqrt(n)

    ordenadas = sorted(puntuaciones)
    if n % 2:
        mediana = ordenadas[n // 2]
    else:
        mediana = (ordenadas[n // 2 - 1] + ordenadas[n // 2]) / 2

    letra, adjetivo = grado(media)

    # Distribución por grado
    conteo = {}
    for p in puntuaciones:
        g, _ = grado(p)
        conteo[g] = conteo.get(g, 0) + 1

    # Tasa de éxito por tarea (si las columnas están presentes)
    tasas = {}
    for tarea in TAREAS:
        valores = [f.get(tarea, "").strip().lower() for f in filas]
        valores = [v for v in valores if v in ("si", "sí", "no")]
        if valores:
            exitos = sum(1 for v in valores if v in ("si", "sí"))
            tasas[tarea] = (exitos, len(valores), 100 * exitos / len(valores))

    lineas = []
    add = lineas.append
    add("# Reporte de usabilidad — SUS (Bloque C.3)\n")
    add(f"- Fecha del analisis: {datetime.now(timezone.utc).isoformat()}")
    add(f"- Commit: {commit_corto()}")
    add(f"- Instrumento: System Usability Scale (Brooke, 1996), 10 items, escala 1-5")
    add(f"- Participantes: **{n}** (minimo exigido: {MINIMO_PARTICIPANTES})\n")

    add("## Resultado agregado\n")
    add("| Metrica | Valor |")
    add("|---|---|")
    add(f"| Media SUS | **{media:.2f}** |")
    add(f"| Desviacion tipica | {dt:.2f} |")
    add(f"| IC 95 % | {media:.2f} ± {ic95:.2f}  ({media - ic95:.2f} – {media + ic95:.2f}) |")
    add(f"| Mediana | {mediana:.2f} |")
    add(f"| Minimo | {min(puntuaciones):.2f} |")
    add(f"| Maximo | {max(puntuaciones):.2f} |")
    add(f"| Grado | **{letra} — {adjetivo}** |\n")

    umbral = "CUMPLE" if media >= 68 else "NO CUMPLE"
    add(f"Umbral objetivo del proyecto: SUS >= 68 (media de la industria). "
        f"Resultado: **{umbral}**.\n")

    add("## Distribucion por grado\n")
    add("| Grado | Participantes |")
    add("|---|---|")
    for g in ("A", "B", "C", "D", "F"):
        if g in conteo:
            add(f"| {g} | {conteo[g]} |")
    add("")

    if tasas:
        add("## Tasa de exito por tarea\n")
        add("| Tarea | Completada | Total | Tasa |")
        add("|---|---|---|---|")
        for tarea, (exitos, total, pct) in tasas.items():
            add(f"| {tarea.upper()} | {exitos} | {total} | {pct:.1f} % |")
        add("")

    add("## Puntuaciones individuales\n")
    add("| Participante | Perfil | SUS | Grado |")
    add("|---|---|---|---|")
    for fila, p in zip(filas, puntuaciones):
        g, _ = grado(p)
        add(f"| {fila['participante']} | {fila.get('perfil', '-')} | {p:.1f} | {g} |")
    add("")

    add("## Interpretacion\n")
    add(f"Con {n} participantes externos, el sistema obtiene una media SUS de "
        f"{media:.2f} (IC 95 % {media - ic95:.2f}–{media + ic95:.2f}), lo que "
        f"corresponde al grado **{letra} ({adjetivo})** en la escala adjetival de "
        f"Bangor, Kortum y Miller (2009).\n")

    # REPORT.md se regenera entero en cada corrida, asi que el analisis
    # escrito a mano vive aparte: ya se perdio una vez al ampliar la
    # muestra de 10 a 14 participantes.
    add("El analisis por perfil y las amenazas a la validez estan en "
        "[INTERPRETACION.md](INTERPRETACION.md), que este script NO "
        "sobrescribe: al agregar participantes hay que actualizarlo a mano.")
    add("")

    add("## Referencias\n")
    add("- Brooke, J. (1996). *SUS: A quick and dirty usability scale.*")
    add("- Bangor, A., Kortum, P. y Miller, J. (2009). *Determining what "
        "individual SUS scores mean.* Journal of Usability Studies, 4(3).")
    add("- ISO/IEC 25010:2011 — Usabilidad.")

    REPORTE.write_text("\n".join(lineas), encoding="utf-8")
    print(f"Generado {REPORTE}")
    print(f"  n={n}  media={media:.2f}  DT={dt:.2f}  IC95=±{ic95:.2f}  grado={letra}")


if __name__ == "__main__":
    main()
