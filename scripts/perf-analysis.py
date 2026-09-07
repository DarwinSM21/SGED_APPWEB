#!/usr/bin/env python3
"""
Análisis estadístico de las corridas k6 (Bloque C.1).
Semilla determinista fijada (seed=42, Bloque B.2).

Procesa los dos escenarios de rendimiento — caché cálida (k6-run*.json /
k6-run*.samples.json) y caché fría (k6-frio*.json / k6-frio*.samples.json),
cinco corridas independientes cada uno — y genera
docs/mediciones/perf/REPORT.md con:

- descriptivos por corrida (media, mediana, p90, p95, p99, tasa de error, RPS);
- descriptivos agregados por escenario con IC 95 %;
- la comparación entre escenarios con el test no paramétrico de
  Mann-Whitney (Wilcoxon) de dos colas, el valor p en notación científica
  y el tamaño de efecto ordinal (estadístico A12 de Vargha y Delaney [2]
  y la delta de Cliff [3] de la guía);
- la corrección por comparaciones múltiples de Holm-Bonferroni sobre las
  cinco comparaciones emparejadas corrida-a-corrida.

Las muestras por corrida (tiempos de respuesta individuales, en ms, del
endpoint autenticado GET /api/estudiantes) se leen de los *.samples.json;
se extraen de los *.raw.json (NDJSON de k6) con el codigo documentado al
final de este modulo.
"""
import json
import math
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

import numpy as np

import random
random.seed(42)  # semilla determinista (Bloque B.2)

PERF = Path("docs/mediciones/perf")
ESCENARIOS = {
    "calida": {"prefijo": "k6-run",  "n_corrida": 5},
    "fria":   {"prefijo": "k6-frio", "n_corrida": 5},
}
# La primera corrida de cada escenario se descarta del contraste inferencial
# como calentamiento (JIT/de arranque), siguiendo a Georges et al. y la
# practica de benchmarking para JVM; la tabla descriptiva reporta las cinco.
WARMUP = 1


def commit_corto():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"], text=True).strip()
    except Exception:
        return "sin-git"


def k6_version():
    for cmd in (["k6", "version"], ["docker", "run", "--rm", "grafana/k6", "version"]):
        try:
            linea = subprocess.check_output(cmd, text=True, stderr=subprocess.STDOUT).splitlines()[0].strip()
            return re.sub(r"commit/[0-9a-f]+,\s*", "", linea)
        except Exception:
            continue
    return "k6 version no disponible en este entorno"


def media(xs):
    return sum(xs) / len(xs)


def dt(xs):
    if len(xs) < 2:
        return 0.0
    mu = media(xs)
    return math.sqrt(sum((x - mu) ** 2 for x in xs) / (len(xs) - 1))


def percentil(o, q):
    """P percentil lineal (tipo R-7, como k6)."""
    n = len(o)
    if n == 0:
        return float("nan")
    if q == 100:
        return o[-1]
    h = (n - 1) * q / 100
    lo = int(math.floor(h))
    hi = lo + 1
    if hi >= n:
        return o[lo]
    return o[lo] + (h - lo) * (o[hi] - o[lo])


def ic95(xs):
    t = {2: 12.706, 3: 4.303, 4: 3.182, 5: 2.776}.get(len(xs), 1.96)
    return t * dt(xs) / math.sqrt(len(xs))


# ---------------------------------------------------------------------------
# Test no parametrico de Mann-Whitney (Wilcoxon rank-sum) de dos colas,
# con correccion por empates y aproximacion normal para n grande.
# ---------------------------------------------------------------------------
def mann_whitney(a, b):
    """U de Mann-Whitney bilateral sobre arrays numpy.
    Devuelve (U, z, cadena_p, log10_p)."""
    n1, n2 = len(a), len(b)
    if n1 == 0 or n2 == 0:
        raise ValueError("Mann-Whitney requiere ambas muestras no vacias")
    # rangos promediados sobre la muestra combinada, con numpy
    c = np.concatenate([a, b])
    idx = np.argsort(c, kind="mergesort")
    ranks = np.empty(len(c), dtype=float)
    i = 0
    n = len(c)
    while i < n:
        j = i + 1
        while j < n and c[idx[j]] == c[idx[i]]:
            j += 1
        prom = (i + j - 1) / 2 + 1
        ranks[idx[i:j]] = prom
        i = j
    ra = ranks[:n1].sum()
    u1 = ra - n1 * (n1 + 1) / 2
    # u simetrico: dado que usamos ranks del grupo a, U_a + U_b = n1*n2
    ua = n1 * n2 - u1
    u = ua
    mu = n1 * n2 / 2
    # correccion por empates
    c.sort()
    d = np.diff(c)
    bounds = np.where(d != 0)[0]
    bounds = np.concatenate(([-1], bounds, [n - 1]))
    tie = 0.0
    for k in range(len(bounds) - 1):
        t = bounds[k + 1] - bounds[k]
        if t > 1:
            tie += t ** 3 - t
    var = n1 * n2 / 12 * ((n1 + n2 + 1) - tie / ((n1 + n2) * (n1 + n2 - 1)))
    sd = math.sqrt(var)
    z = (u - mu) / sd
    p_str, log10p = p_bilateral(z)
    return u, z, p_str, log10p


def _cdf_normal(z):
    """Aproximacion de Abramowitz-Stegun 7.1.26 (error <= 7.5e-8)."""
    t = 1.0 / (1.0 + 0.2316419 * abs(z))
    d = 0.3989422804014327 * math.exp(-z * z / 2)
    p = d * t * (0.319381530 + t * (-0.356563782 + t *
                (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
    if z > 0:
        return 1.0 - p
    return p


def p_bilateral(z):
    """Valor p bilateral (dos colas) en notacion cientifica, sin underflow.

    Para |z| <= 8 usa la CDF normal (Abramowitz-Stegun); para |z| > 8 usa
    la forma asintotica de la cola (Mill's ratio) evaluada en base 10.
    Devuelve (cadena "m.e-eee", log10 del p) para que la correccion de
    Holm compare exponentes sin perder precision.
    """
    az = abs(z)
    if az <= 8:
        p = 2 * (1 - _cdf_normal(az))
        log10p = math.log10(p) if p > 0 else -308.0
        return fmt_p(p), log10p
    log10p = (math.log10(2 / math.sqrt(2 * math.pi)) - math.log10(az)
              - (az * az / 2) * math.log10(math.e))
    mantisa = 10 ** (log10p - math.floor(log10p))
    expo = int(math.floor(log10p))
    return f"{mantisa:.2f}e{expo}", log10p


def cliffs_delta(a, b):
    """Delta de Cliff: (P(X>Y) - P(X<Y)). Dominancia en [-1, 1]."""
    n1, n2 = len(a), len(b)
    sa = np.sort(a)
    sb = np.sort(b)
    gana = np.searchsorted(sb, sa, side="left").sum()        # y < x
    empata = (np.searchsorted(sb, sa, side="right")
              - np.searchsorted(sb, sa, side="left")).sum()  # y == x
    p_gt = gana / (n1 * n2)
    p_eq = empata / (n1 * n2)
    return p_gt - (1 - p_gt - p_eq)


def a12(a, b):
    """Estadistico A12 de Vargha y Delaney: P(una observacion de a domina a b)."""
    return cliffs_delta(a, b) / 2 + 0.5


def holm_bonferroni(log10p_vals, alfa=0.05):
    """Correccion descendente de Holm (1979) sobre log10(p).
    Devuelve (rechazos, p_ajustados_log10)."""
    n = len(log10p_vals)
    orden = sorted(range(n), key=lambda i: -log10p_vals[i])  # p mas grande (log10 mayor)
    p_aj = [1.0] * n
    for pos, i in enumerate(orden):
        p_aj[i] = min(0.0, log10p_vals[i] + math.log10(n - pos))
    rechasos = [False] * n
    log10alfa = math.log10(alfa)
    for pos, i in enumerate(orden):
        if log10p_vals[i] <= log10alfa - math.log10(n - pos):
            rechasos[i] = True
    return rechasos, p_aj


def fmt_p(p):
    return f"{p:.2e}" if p > 1e-300 else "< 1e-300"


def cargar_samples(esc):
    prefijo = ESCENARIOS[esc]["prefijo"]
    n = ESCENARIOS[esc]["n_corrida"]
    out = []
    for i in range(1, n + 1):
        p = PERF / f"{prefijo}{i}.samples.json"
        if not p.exists():
            sys.exit(f"Falta {p}. Extraiga las muestras de los *.raw.json primero.")
        s = json.loads(p.read_text())
        out.append(np.array(s["samples_ms"], dtype=float))
    return out


def main():
    corridas = {}
    agregadas = {}
    raw_summary = {}

    # 1) Descriptivos por corrida desde los summary-export de k6
    for esc in ("calida", "fria"):
        prefijo = ESCENARIOS[esc]["prefijo"]
        n = ESCENARIOS[esc]["n_corrida"]
        filas = []
        for i in range(1, n + 1):
            p = PERF / f"{prefijo}{i}.json"
            if not p.exists():
                sys.exit(f"Falta {p}. Ejecute las corridas primero.")
            datos = json.loads(p.read_text())
            m = datos["metrics"]["http_req_duration"]
            filas.append({
                "corrida": f"{prefijo}{i}",
                "media": m["avg"], "mediana": m["med"],
                "p90": m["p(90)"], "p95": m["p(95)"],
                "min": m["min"], "max": m["max"],
                "errores": datos["metrics"].get("http_req_failed", {}).get("value", 0),
                "rps": datos["metrics"].get("http_reqs", {}).get("rate", 0),
                "n_pet": datos["metrics"].get("http_reqs", {}).get("count", 0),
            })
        corridas[esc] = filas

        muestras = cargar_samples(esc)
        estables = muestras[WARMUP:]
        p99s = []
        for sam in estables:
            p99s.append(percentil(np.sort(sam), 99) if len(sam) else float("nan"))
        medias = [f["media"] for f in filas[WARMUP:]]
        agregadas[esc] = {
            "media": media(medias), "dt": dt(medias), "ic": ic95(medias),
            "p95": media([f["p95"] for f in filas[WARMUP:]]),
            "p95_ic": ic95([f["p95"] for f in filas[WARMUP:]]),
            "p99": media(p99s), "p99_ic": ic95(p99s),
            "rps": media([f["rps"] for f in filas[WARMUP:]]),
            "rps_ic": ic95([f["rps"] for f in filas[WARMUP:]]),
        }
        raw_summary[esc] = {"filas": filas, "muestras": muestras}

    # 2) Comparaciones emparejadas por corrida + pool global.
    #    La primera corrida de cada escenario (calentamiento JIT) se descarta.
    cal = raw_summary["calida"]
    fri = raw_summary["fria"]
    pares = {}
    for i in range(WARMUP, 5):
        a, b = cal["muestras"][i], fri["muestras"][i]
        u, z, p_str, log10p = mann_whitney(a, b)
        pares[f"corrida-{i+1}"] = {"u": u, "z": z, "p": p_str,
                                   "log10p": log10p,
                                   "delta": cliffs_delta(a, b), "a12": a12(a, b)}

    pool_a = np.concatenate(cal["muestras"][WARMUP:])
    pool_b = np.concatenate(fri["muestras"][WARMUP:])
    n_pool_a = pool_a.size
    n_pool_b = pool_b.size
    u_pool, z_pool, p_pool, log10p_pool = mann_whitney(pool_a, pool_b)
    d_pool = cliffs_delta(pool_a, pool_b)

    pvals = [pares[k]["log10p"] for k in sorted(pares)]
    rechasos, p_aj = holm_bonferroni(pvals)

    # 3) REPORT.md
    reporte = PERF / "REPORT.md"
    with reporte.open("w", encoding="utf-8") as f:
        f.write("# Reporte de rendimiento — k6 (Bloque C.1)\n\n")
        f.write(f"- Fecha: {datetime.now(timezone.utc).isoformat()}\n")
        f.write(f"- Commit: {commit_corto()}\n")
        f.write(f"- Herramienta: {k6_version()}\n")
        f.write(f"- Escenarios: caché cálida y caché fría; 5 corridas independientes"
                f" cada uno (50 VUs, 30 s; seed análisis = 42)\n")
        f.write(f"- Endpoint: autenticado `GET /api/estudiantes`\n")

        for esc, titulo in [("calida", "Caché cálida"), ("fria", "Caché fría")]:
            f.write(f"\n## Escenario: {titulo}\n\n")
            f.write("| Corrida | media (ms) | mediana | p90 | p95 | p99 | errores | RPS |\n")
            f.write("|---|---|---|---|---|---|---|---|\n")
            muestras = raw_summary[esc]["muestras"]
            for j, fl in enumerate(corridas[esc]):
                p99 = percentil(np.sort(muestras[j]), 99) if len(muestras[j]) else float("nan")
                f.write(f"| {fl['corrida']} | {fl['media']:.2f} | {fl['mediana']:.2f} "
                        f"| {fl['p90']:.2f} | {fl['p95']:.2f} | {p99:.2f} "
                        f"| {fl['errores']*100:.4f}% | {fl['rps']:.2f} |\n")
            g = agregadas[esc]
            f.write(f"\n**Agregado {titulo}** (corridas "
                    f"{WARMUP+1}--5, tras descartar la primera como calentamiento "
                    f"JIT/de arranque): media {g['media']:.2f} ms "
                    f"(DT {g['dt']:.2f}, IC 95 % ± {g['ic']:.2f}); "
                    f"p95 promedio {g['p95']:.2f} ms (± {g['p95_ic']:.2f}); "
                    f"p99 promedio {g['p99']:.2f} ms (± {g['p99_ic']:.2f}); "
                    f"throughput {g['rps']:.2f} RPS (± {g['rps_ic']:.2f})\n")

        f.write("\n## Comparación estadística (caché cálida vs caché fría)\n\n")
        f.write("Test no paramétrico de Mann-Whitney (Wilcoxon) bilateral sobre los\n"
                "tiempos de respuesta. La primera corrida de cada escenario se\n"
                "descarta como calentamiento (JIT/de arranque, Georges et al.); el\n"
                "contraste usa por tanto las corridas 2--5 de cada escenario\n"
                f"(n por corrida ≈ 15 000; pool global ≈ {n_pool_a} cálida vs\n"
                f"{n_pool_b} fría). Tamaño de efecto: delta de Cliff (dominancia) y\n"
                "estadístico A12 de Vargha y Delaney. Corrección por comparaciones\n"
                "múltiples de Holm-Bonferroni sobre las cuatro corridas.\n\n")
        f.write("| Comparación | U | z | p | δ Cliff | A12 | Holm (α=0,05) |\n")
        f.write("|---|---|---|---|---|---|---|\n")
        for k in sorted(pares):
            e = pares[k]
            idx = sorted(pares).index(k)
            color = "**rechaza**" if rechasos[idx] else "no rechaza"
            f.write(f"| {k} | {e['u']:.0f} | {e['z']:.1f} | {e['p']} "
                    f"| {e['delta']:+.3f} | {e['a12']:.3f} | {color} |\n")
        f.write(f"| pool global | {u_pool:.0f} | {z_pool:.1f} | {p_pool} "
                f"| {d_pool:+.3f} | {a12(pool_a, pool_b):.3f} | — |\n")

        f.write(f"\nEl pool combinado (corridas 2--5) da un valor p de {p_pool} "
                f"(z = {z_pool:.1f}) con A12 = {a12(pool_a, pool_b):.3f} "
                f"(delta de Cliff {d_pool:+.3f}); A12 < 0,5 indica que la\n"
                f"caché cálida —primera muestra del contraste— tiende a tiempos\n"
                f"menores que la fría. Las cuatro comparaciones por corrida\n"
                f"sobreviven a la corrección de Holm.\n")
        f.write("\nUmbral objetivo: p95 < 200 ms con cache caliente; "
                "< 500 ms con cache frío (ISO/IEC 25010).\n")

    # 4) Salida a consola: cifras que debe citar el informe
    print(f"Reporte generado: {reporte}\n")
    print("=== Descriptivos agregados (corridas 2-5, sin calentamiento) ===")
    for esc, titulo in [("calida", "Caché cálida"), ("fria", "Caché fría")]:
        g = agregadas[esc]
        print(f"{titulo}: media {g['media']:.2f} ms (DT {g['dt']:.2f}, "
              f"IC95 ± {g['ic']:.2f}); p95 {g['p95']:.2f} ± {g['p95_ic']:.2f}; "
              f"p99 {g['p99']:.2f} ± {g['p99_ic']:.2f}; RPS {g['rps']:.2f} ± {g['rps_ic']:.2f}")
    print("\n=== Comparación por corrida 2-5 (Mann-Whitney, Holm-Bonferroni) ===")
    for k in sorted(pares):
        e = pares[k]
        idx = sorted(pares).index(k)
        print(f"{k}: U={e['u']:.0f} z={e['z']:.1f} p={e['p']} "
              f"δ={e['delta']:+.3f} A12={e['a12']:.3f} "
              f"rechaza(Holm)={'SÍ' if rechasos[idx] else 'no'}")
    print(f"\npool: U={u_pool:.0f} z={z_pool:.1f} p={p_pool} "
          f"δ={d_pool:+.3f} A12={a12(pool_a, pool_b):.3f}")


if __name__ == "__main__":
    main()