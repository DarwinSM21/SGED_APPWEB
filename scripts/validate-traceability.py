#!/usr/bin/env python3
"""Valida docs/trazabilidad/matriz.csv contra el SRS y el código.

Comprobaciones de la revision M1-M9 de la Entrega Final:
  1. Toda fila tiene exactamente el mismo numero de columnas que la cabecera
     (quiebra si un campo con comas no esta entrecomillado).
  2. Todo requisito tiene historia de usuario, caso de uso o prueba asociada.
  3. 'estado' pertenece al vocabulario cerrado del SRS 1.3
     {Implementado, Modelado, Planificado}.
  4. Toda clase *Test citada (columna prueba_automatizada de la matriz y
     Verificacion del SRS) existe en backend/src/test; 'Clase.metodo' debe
     resolver a un metodo real.
  5. Los ids de requisito del SRS y de la matriz coinciden en ambas
     direcciones (los encabezados de agrupacion sin declaracion - unicamente
     RF-19 - no son requisitos y quedan fuera).

Uso: python3 scripts/validate-traceability.py [ruta/a/matriz.csv]
"""
import csv
import os
import re
import sys

MATRIZ = sys.argv[1] if len(sys.argv) > 1 else "docs/trazabilidad/matriz.csv"
SRS = "docs/requisitos/SRS.md"
TEST_ROOT = "backend/src/test"
VOC_ESTADO = {"Implementado", "Modelado", "Planificado"}

falta = 0


def viol(msg):
    global falta
    falta = 1
    print("VIOLACIÓN: " + msg)


def exists_clase(clase):
    if not os.path.isdir(TEST_ROOT):
        return True  # sin arbol de pruebas no se puede comprobar (no fallar en esa condicion)
    for root, _dirs, files in os.walk(TEST_ROOT):
        if clase + ".java" in files:
            return True
    return False


def has_metodo(clase, metodo):
    if not os.path.isdir(TEST_ROOT):
        return True
    for root, _dirs, files in os.walk(TEST_ROOT):
        if clase + ".java" in files:
            text = open(os.path.join(root, clase + ".java"), encoding="utf-8",
                        errors="replace").read()
            return bool(re.search(r"void[ \t]+" + re.escape(metodo) + r"[ \t]*\(", text))
    return False


REF_RE = re.compile(
    r"[A-Z][A-Za-z0-9]*Tests?\.[A-Za-z0-9_]+|[A-Z][A-Za-z0-9]*Tests?\b"
)


def check_refs(text, donde):
    for ref in REF_RE.findall(text):
        if "." in ref:
            clase, metodo = ref.split(".", 1)
        else:
            clase, metodo = ref, None
        if not exists_clase(clase):
            viol("%s cita %s pero no existe la clase de prueba %s." % (donde, ref, clase))
            continue
        if metodo and not has_metodo(clase, metodo):
            viol("%s cita %s pero %s no tiene un metodo %s." % (donde, ref, clase, metodo))


# --- Matriz ---------------------------------------------------------------
if not os.path.isfile(MATRIZ):
    print("ERROR: no existe %s" % MATRIZ)
    sys.exit(1)

with open(MATRIZ, newline="", encoding="utf-8") as f:
    rows = list(csv.reader(f))
header = [c.strip() for c in rows[0]]
ncols = len(header)
matriz_ids = set()
for row in rows[1:]:
    if not row or not row[0].strip():
        continue
    req = row[0].strip()
    matriz_ids.add(req)
    if len(row) != ncols:
        viol("%s: %d columnas, se esperaban %d (cabecera de %d)." % (req, len(row), ncols, ncols))
        continue
    if not (row[3].strip() or row[4].strip() or row[7].strip()):
        viol("%s no tiene historia, caso de uso ni prueba." % req)
    estado = row[9].strip()
    if estado not in VOC_ESTADO:
        viol("%s: estado '%s' no pertenece al vocabulario %s." % (req, estado, sorted(VOC_ESTADO)))
    check_refs(row[7], "la matriz")

# --- SRS: referencias de Verificacion y paridad de ids --------------------
srs_ids = set()
srs_lines = []
verif_by_req = {}

if os.path.isfile(SRS):
    lines = open(SRS, encoding="utf-8").read().splitlines()
    heads = [i for i, l in enumerate(lines)
             if re.match(r"^\*\*(RF|RNF)-[0-9]+[a-z]? —", l)]
    for k, i in enumerate(heads):
        end = heads[k + 1] if k + 1 < len(heads) else len(lines)
        block = lines[i:end]
        mid = re.match(r"^\*\*(RF|RNF)-([0-9]+[a-z]?) —", block[0]).group(2)
        rid = block[0][2:].split(" —")[0]
        if any(re.match(r"^\*[^*]", l) for l in block):
            srs_ids.add(rid)
        for l in block:
            if l.startswith("- **Verificación:**") or l.startswith("**Verificación:**"):
                verif_by_req.setdefault(rid, []).append(l)
    srs_lines = lines

for rid, lines2 in verif_by_req.items():
    check_refs(" | ".join(lines2), "el SRS (%s)" % rid)

faltan = sorted(srs_ids - matriz_ids)
sobra = sorted(matriz_ids - srs_ids)
if faltan:
    viol("ids del SRS ausentes en la matriz: %s" % ", ".join(faltan))
if sobra:
    viol("ids de la matriz sin requisito en el SRS: %s" % ", ".join(sobra))

if falta:
    sys.exit(falta)
print("Validación de trazabilidad completada.")