#!/usr/bin/env python3
"""Cuenta metodos/constructores publicos del backend y cuantos tienen Javadoc.

Cubre dos casos:
  1. Metodos con el modificador "public" explicito dentro de una clase,
     record o enum (incluye firmas partidas en varias lineas).
  2. Metodos declarados dentro del cuerpo de nivel superior de una
     "interface" (abstractos, default o static): en Java son publicos
     aunque no lleven la palabra "public", salvo que digan "private"
     explicitamente (metodos privados de interfaz, Java 9+).

Un metodo cuenta como documentado si, subiendo desde su firma y saltando
lineas en blanco y anotaciones (que pueden ocupar varias lineas), la
primera linea de codigo termina en "*/".

Es una heuristica basada en texto, no en un parser de Java real: firmas con
generics complejos, comentarios de bloque a mitad de una firma, o lambdas
declaradas como campo pueden confundirla. Ante la duda revisar
docs/mediciones/javadoc-sin-documentar.txt a mano antes de fiarse del
numero solo.

Uso:
    python3 scripts/javadoc-coverage.py [umbral_porcentaje]

Sale con 0 si el porcentaje >= umbral (default 90), 1 en caso contrario.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "backend" / "src" / "main" / "java"

TOP_TYPE_RE = re.compile(r"^(public\s+)?(final\s+|abstract\s+)*(class|interface|enum|@interface)\s+\w+")
INTERFACE_RE = re.compile(r"\binterface\s+\w+")


def brace_delta(line: str) -> int:
    # aproximacion: no distingue llaves dentro de strings/chars, pero el
    # backend no tiene llaves literales en firmas de metodo relevantes aqui.
    return line.count("{") - line.count("}")


def is_documented(lines, idx):
    j = idx - 1
    while j >= 0:
        s = lines[j].strip()
        if s == "":
            j -= 1
            continue
        if s.startswith("@"):
            j -= 1
            continue
        return s.endswith("*/")
    return False


def join_signature(lines, idx, max_extra=4):
    """Une la linea idx con las siguientes hasta ver '(' + (';' o '{')."""
    joined = lines[idx]
    j = idx
    extra = 0
    while "(" not in joined and extra < max_extra and j + 1 < len(lines):
        j += 1
        joined += " " + lines[j].strip()
        extra += 1
    return joined


def looks_like_method(sig: str) -> bool:
    if "(" not in sig:
        return False
    paren = sig.find("(")
    before = sig[:paren]
    if re.search(r"[=;]", before):
        return False
    tail = sig[paren:]
    if not (tail.rstrip().endswith("{") or tail.rstrip().endswith(";") or tail.rstrip().endswith(")")
            or re.search(r"\)\s*(throws\s+[\w,\s.]+)?\s*[{;]?\s*$", tail)):
        # firma probablemente sigue en otra linea; se acepta igual si ya
        # tiene un nombre + parentesis abierto reconocible
        pass
    return True


def find_class_public_methods(lines):
    methods = []
    for i, raw in enumerate(lines):
        line = raw.strip()
        if not line.startswith("public "):
            continue
        if TOP_TYPE_RE.match(line):
            continue
        sig = join_signature(lines, i)
        if not looks_like_method(sig):
            continue
        methods.append(i)
    return methods


def find_interface_implicit_methods(lines):
    methods = []
    depth = 0
    interface_depths = set()
    in_annotation = False
    annotation_paren_balance = 0

    for i, raw in enumerate(lines):
        stripped = raw.strip()

        if in_annotation:
            annotation_paren_balance += stripped.count("(") - stripped.count(")")
            if annotation_paren_balance <= 0:
                in_annotation = False
            new_depth = depth + brace_delta(stripped)
            if new_depth < depth:
                interface_depths = {d for d in interface_depths if d <= new_depth}
            depth = new_depth
            continue

        if INTERFACE_RE.search(stripped) and "{" in stripped:
            depth_before = depth
            depth += brace_delta(stripped)
            interface_depths.add(depth_before + 1)
            continue

        is_body_line = stripped and not stripped.startswith("//") \
            and not stripped.startswith("*") and not stripped.startswith("/*") \
            and not stripped.startswith("}")

        if depth in interface_depths and is_body_line:
            if stripped.startswith("@"):
                balance = stripped.count("(") - stripped.count(")")
                if balance > 0:
                    in_annotation = True
                    annotation_paren_balance = balance
            elif not stripped.startswith("private") and "(" in stripped:
                sig = join_signature(lines, i)
                before = sig[: sig.find("(")]
                if not re.search(r"[=;]", before) and TOP_TYPE_RE.search(stripped) is None:
                    methods.append(i)

        new_depth = depth + brace_delta(stripped)
        if new_depth < depth:
            interface_depths = {d for d in interface_depths if d <= new_depth}
        depth = new_depth
    return methods


def main():
    threshold = float(sys.argv[1]) if len(sys.argv) > 1 else 90.0
    total = 0
    documented = 0
    undocumented_locations = []

    for java_file in sorted(SRC.rglob("*.java")):
        rel = java_file.relative_to(ROOT)
        lines = java_file.read_text(encoding="utf-8", errors="replace").splitlines()

        method_idxs = set(find_class_public_methods(lines))
        method_idxs.update(find_interface_implicit_methods(lines))

        for idx in sorted(method_idxs):
            total += 1
            if is_documented(lines, idx):
                documented += 1
            else:
                undocumented_locations.append(f"{rel}:{idx + 1}: {lines[idx].strip()[:100]}")

    pct = (documented / total * 100) if total else 0.0
    print(f"Metodos/constructores publicos encontrados: {total}")
    print(f"Con Javadoc inmediatamente encima: {documented}")
    print(f"Cobertura: {pct:.1f}%  (umbral exigido: {threshold:.0f}%)")

    if undocumented_locations:
        out_path = ROOT / "docs" / "mediciones" / "javadoc-sin-documentar.txt"
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text("\n".join(undocumented_locations) + "\n", encoding="utf-8")
        print(f"Lista de {len(undocumented_locations)} metodos sin Javadoc: {out_path.relative_to(ROOT)}")

    if pct >= threshold:
        print("RESULTADO: PASA")
        return 0
    print("RESULTADO: FALLA")
    return 1


if __name__ == "__main__":
    sys.exit(main())
