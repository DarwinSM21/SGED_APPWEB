#!/usr/bin/env python3
"""Conteo por rol CRediT, a partir de que archivos toco cada commit (Punto P10).

La tabla anterior de CONTRIBUTORS.md ponia, junto a cada rol, el TOTAL de
commits de la persona (el mismo numero repetido en cada fila donde
aparece) -- no dice nada especifico de ESE rol. Esto calcula, por
persona y por rol, cuantos commits tocaron al menos un archivo de las
rutas asociadas a ese rol (mapeo declarado abajo, editable).

No todos los roles CRediT se pueden inferir de rutas de archivo
(Supervision, Project administration y Funding acquisition son en buena
parte trabajo que no deja huella en el arbol de archivos: coordinacion,
decisiones, revisar el estado del repositorio). Para esos el script
declara explicitamente que no son cuantificables asi, en vez de inventar
un numero.

Uso:
    python3 scripts/credit-counts.py
Imprime la tabla; no escribe nada solo (revisen y peguen a mano en
CONTRIBUTORS.md, o extiendan el script para que lo haga si prefieren
automatizarlo del todo).
"""
import subprocess
from collections import defaultdict

AUTORES = {
    "dpallop@uteq.edu.ec": "Pallo Pinto Alejandro Daniel",
    "rvelezl3@uteq.edu.ec": "Velez Lopez Ricardo Elias",
    "darcalleg@uteq.edu.ec": "Arcalle Grefa Darwin Orlando",
    "darwinarcalle@gmail.com": "Arcalle Grefa Darwin Orlando",
}

# Mapeo rol -> prefijos de ruta. Declarado a proposito (no oculto en el
# codigo): si el equipo no esta de acuerdo con algun prefijo, se edita
# aqui y se vuelve a correr -- no hay que tocar CONTRIBUTORS.md a mano.
ROLES_POR_RUTA = {
    "Conceptualization": ["docs/requisitos/", "docs/arquitectura/workspace.dsl"],
    "Data curation": ["db/", "/entity/", "docs/basedatos/"],
    "Formal analysis": ["scripts/sus-analysis.py", "scripts/perf-analysis.py",
                          "docs/mediciones/sus/REPORT.md", "docs/mediciones/sus/INTERPRETACION.md",
                          "docs/mediciones/perf/REPORT.md"],
    "Investigation": ["docs/requisitos/historias-usuario.md", "docs/requisitos/casos-uso.md"],
    "Methodology": ["k6/", "docs/mediciones/sus/INSTRUMENTO-SUS.md", "lighthouserc.js"],
    "Resources": ["docker-compose.yml", "Dockerfile", "render.yaml", ".github/workflows/"],
    "Software": ["backend/src/main/", "frontend/src/"],
    "Validation": ["backend/src/test/", "docs/mediciones/sec/", "docs/mediciones/perf/",
                     "docs/mediciones/sus/respuestas.csv", "docs/mediciones/lighthouse/",
                     "scripts/validate-traceability"],
    "Visualization": ["docs/diagramas/", "docs/arquitectura/"],
    "Writing – original draft": ["docs/informe/", "docs/requisitos/SRS.md"],
}
# Roles que NO se infieren de rutas (trabajo de coordinacion/decision, no
# de archivos concretos). Se listan para que la tabla los declare como
# tal en vez de omitirlos u inventar un numero.
ROLES_NO_CUANTIFICABLES = ["Project administration", "Supervision", "Funding acquisition"]
# "Writing - review & editing" se aproxima con commits que MODIFICAN
# (no crean por primera vez) archivos de documentacion ya existentes;
# ver logica mas abajo en vez de una lista fija de rutas.
DOC_PATHS_PREFIX = ("docs/", "README.md", "CITATION.cff", "CONTRIBUTORS.md")


def commits_autor():
    out = subprocess.check_output(
        ["git", "log", "--pretty=%H\t%ae", "main"], text=True)
    por_autor = defaultdict(list)
    for line in out.strip().splitlines():
        h, ae = line.split("\t")
        por_autor[ae].append(h)
    return por_autor


def archivos_de(commit_hash):
    out = subprocess.check_output(
        ["git", "show", "--name-only", "--pretty=format:", commit_hash], text=True)
    return [f for f in out.strip().splitlines() if f]


def toca_alguna(archivos, prefijos):
    return any(p in a for a in archivos for p in prefijos)


def es_primera_vez(commit_hash, archivo, cache_primeros):
    if archivo not in cache_primeros:
        out = subprocess.check_output(
            ["git", "log", "--follow", "--diff-filter=A", "--pretty=%H", "--", archivo],
            text=True).strip().splitlines()
        cache_primeros[archivo] = set(out)
    return commit_hash in cache_primeros[archivo]


def main():
    por_autor = commits_autor()
    conteo = {ae: defaultdict(int) for ae in por_autor}
    cache_primeros = {}

    for ae, hashes in por_autor.items():
        for h in hashes:
            archivos = archivos_de(h)
            for rol, prefijos in ROLES_POR_RUTA.items():
                if toca_alguna(archivos, prefijos):
                    conteo[ae][rol] += 1
            docs_tocados = [a for a in archivos if a.startswith(DOC_PATHS_PREFIX)]
            if docs_tocados and not all(
                    es_primera_vez(h, a, cache_primeros) for a in docs_tocados):
                conteo[ae]["Writing – review & editing"] += 1

    personas = {}
    for ae, nombre in AUTORES.items():
        personas.setdefault(nombre, defaultdict(int))
        for rol, n in conteo.get(ae, {}).items():
            personas[nombre][rol] += n

    todos_roles = list(ROLES_POR_RUTA.keys()) + ["Writing – review & editing"]
    print(f"{'Rol':<30}" + "".join(f"{p:<40}" for p in personas))
    for rol in todos_roles:
        fila = f"{rol:<30}"
        for p in personas:
            fila += f"{personas[p].get(rol, 0):<40}"
        print(fila)
    print()
    print("No cuantificables por ruta de archivo (declarar aparte, criterio cualitativo):")
    for rol in ROLES_NO_CUANTIFICABLES:
        print(f"  - {rol}")


if __name__ == "__main__":
    main()
