#!/usr/bin/env bash
# Autotest de scripts/validate-traceability.sh.
#
# Criterio de aceptacion de la observacion de la Entrega Final: "Una fila
# invalida inyectada en la matriz hace que el validador imprima la violacion
# y termine con codigo distinto de cero". Este script lo comprueba de forma
# reproducible, sobre copias temporales -nunca toca docs/trazabilidad/matriz.csv-.
set -uo pipefail

MATRIZ_REAL="docs/trazabilidad/matriz.csv"
VALIDADOR="scripts/validate-traceability.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fallo() { echo "FALLO DEL AUTOTEST: $1"; exit 1; }

[ -f "$MATRIZ_REAL" ] || fallo "no existe $MATRIZ_REAL"

# Caso A: la matriz real es valida -> exit 0.
cp "$MATRIZ_REAL" "$TMP/ok.csv"
if ! bash "$VALIDADOR" "$TMP/ok.csv" >/dev/null 2>&1; then
  fallo "la matriz real deberia validar (exit 0) y no lo hizo"
fi

# Caso B: fila sin historia, caso de uso ni prueba -> VIOLACIÓN + exit != 0.
cp "$MATRIZ_REAL" "$TMP/bad.csv"
echo 'RF-AUTOTEST,CRUD-ORM,fila deliberadamente sin trazabilidad,,,GET /api/nada,backend/Nada.java,,,Planificado' >> "$TMP/bad.csv"
salida="$(bash "$VALIDADOR" "$TMP/bad.csv" 2>&1)"; codigo=$?

if [ "$codigo" -eq 0 ]; then
  fallo "una fila invalida deberia dar exit != 0; dio 0"
fi
if ! printf '%s\n' "$salida" | grep -q 'VIOLACIÓN: RF-AUTOTEST'; then
  fallo "no se imprimio la VIOLACIÓN de la fila inyectada. Salida: $salida"
fi

# Caso C: referencia Clase.metodo inexistente -> VIOLACIÓN + exit != 0.
cp "$MATRIZ_REAL" "$TMP/badref.csv"
echo 'RF-AUTOTEST2,CRUD-ORM,cita una prueba que no existe,HU-00,CU-00,GET /api/nada,backend/Nada.java,ClaseQueNoExisteTest.metodoFantasma,N/A,Planificado' >> "$TMP/badref.csv"
salida2="$(bash "$VALIDADOR" "$TMP/badref.csv" 2>&1)"; codigo2=$?

if [ "$codigo2" -eq 0 ]; then
  fallo "una referencia de prueba inexistente deberia dar exit != 0; dio 0"
fi
if ! printf '%s\n' "$salida2" | grep -q 'ClaseQueNoExisteTest.metodoFantasma'; then
  fallo "no se detecto la referencia de prueba inexistente. Salida: $salida2"
fi

echo "OK: el validador falla-cerrado ante una fila invalida (codigo $codigo) y ante una referencia de prueba inexistente (codigo $codigo2)."
