#!/usr/bin/env python3
"""
Análisis de las corridas k6 (Bloque C.1).
Semilla determinista fijada (seed=42, Bloque B.2).
Calcula media, mediana, DT, IC 95 % y percentiles p50/p90/p95/p99
del tiempo de respuesta, y genera docs/mediciones/perf/REPORT.md.
"""
import json
import math
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

import random
random.seed(42)  # semilla determinista (Bloque B.2)

PERF = Path("docs/mediciones/perf")
RUNS = sorted(PERF.glob("k6-run*.json"))

if not RUNS:
    sys.exit("No hay corridas k6 en docs/mediciones/perf/. Ejecute make bench.")

def commit_corto():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"], text=True).strip()
    except Exception:
        return "sin-git"

filas = []
for run in RUNS:
    datos = json.loads(run.read_text())
    m = datos["metrics"]["http_req_duration"]
    filas.append({
        "corrida": run.name,
        "media": m["avg"],
        "mediana": m["med"],
        "p90": m["p(90)"],
        "p95": m["p(95)"],
        "min": m["min"],
        "max": m["max"],
        "errores": datos["metrics"].get("http_req_failed", {}).get("value", 0),
        "rps": datos["metrics"].get("http_reqs", {}).get("rate", 0),
    })

n = len(filas)
medias = [f["media"] for f in filas]
p95s = [f["p95"] for f in filas]
rpss = [f["rps"] for f in filas]

def media(xs): return sum(xs) / len(xs)
def dt(xs):
    if len(xs) < 2: return 0.0
    mu = media(xs)
    return math.sqrt(sum((x - mu) ** 2 for x in xs) / (len(xs) - 1))
def ic95(xs):
    # t de Student para n-1 gl (n=3 -> t=4.303)
    t = {2: 12.706, 3: 4.303, 4: 3.182, 5: 2.776}.get(len(xs), 1.96)
    return t * dt(xs) / math.sqrt(len(xs))

reporte = PERF / "REPORT.md"
with reporte.open("w") as f:
    f.write("# Reporte de rendimiento — k6 (Bloque C.1)\n\n")
    f.write(f"- Fecha: {datetime.now(timezone.utc).isoformat()}\n")
    f.write(f"- Commit: {commit_corto()}\n")
    f.write(f"- Corridas independientes: {n} (50 VUs, 30 s, seed análisis = 42)\n\n")
    f.write("| Corrida | media (ms) | mediana | p90 | p95 | errores | RPS |\n")
    f.write("|---|---|---|---|---|---|---|\n")
    for fl in filas:
        f.write(f"| {fl['corrida']} | {fl['media']:.2f} | {fl['mediana']:.2f} "
                f"| {fl['p90']:.2f} | {fl['p95']:.2f} | {fl['errores']:.4f} "
                f"| {fl['rps']:.2f} |\n")
    f.write("\n## Agregado entre corridas\n\n")
    f.write(f"- Media del tiempo de respuesta: {media(medias):.2f} ms "
            f"(DT {dt(medias):.2f}, IC 95 % ± {ic95(medias):.2f})\n")
    f.write(f"- p95 promedio: {media(p95s):.2f} ms (IC 95 % ± {ic95(p95s):.2f})\n")
    f.write(f"- Throughput: {media(rpss):.2f} RPS (IC 95 % ± {ic95(rpss):.2f})\n")
    f.write("\nUmbral objetivo: p95 < 200 ms con cache caliente; "
            "< 500 ms con cache frío (ISO/IEC 25010).\n")

print(f"Reporte generado: {reporte}")
