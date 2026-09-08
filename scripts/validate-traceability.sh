#!/usr/bin/env bash
# Valida la matriz de trazabilidad (Bloque A.3.3) contra el SRS y el codigo.
# Toda la logica vive en scripts/validate-traceability.py (parsing CSV real,
# con campos entrecomillados). Este envoltorio conserva la interfaz anterior:
#
#   bash scripts/validate-traceability.sh [ruta/al/matriz.csv]
#
# (la ruta opcional existe para el autotest scripts/test-validate-traceability.sh)
set -euo pipefail

PY="$(command -v python3 >/dev/null 2>&1 && echo python3 || echo python)"

exec "$PY" "$(dirname "$0")/validate-traceability.py" "${1:-docs/trazabilidad/matriz.csv}"