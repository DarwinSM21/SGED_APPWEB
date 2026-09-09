#!/usr/bin/env bash
# Autotest de scripts/validate-traceability.sh.
#
# Criterio de aceptacion de la Entrega Final (M1-M9): cualquier invalidez en
# la matriz - fila sin trazabilidad, referencia de prueba inexistente, numero
# de columnas distinto de la cabecera, estado fuera del vocabulario - hace
# que el validador imprima la violacion y termine con codigo distinto de
# cero. Este script lo comprueba de forma reproducible, sobre copias
# temporales - nunca toca docs/trazabilidad/matriz.csv ni el SRS.
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
echo 'RF-AUTOTEST,CRUD-ORM,fila deliberadamente sin trazabilidad,,,GET /api/nada,backend/Nada.java,,,Planificado,' >> "$TMP/bad.csv"
salida="$(bash "$VALIDADOR" "$TMP/bad.csv" 2>&1)"; codigo=$?

if [ "$codigo" -eq 0 ]; then
  fallo "una fila invalida deberia dar exit != 0; dio 0"
fi
if ! printf '%s\n' "$salida" | grep -q 'VIOLACIÓN: RF-AUTOTEST'; then
  fallo "no se imprimio la VIOLACIÓN de la fila inyectada. Salida: $salida"
fi

# Caso C: referencia Clase.metodo inexistente -> VIOLACIÓN + exit != 0.
cp "$MATRIZ_REAL" "$TMP/badref.csv"
echo 'RF-AUTOTEST2,CRUD-ORM,cita una prueba que no existe,HU-00,CU-00,GET /api/nada,backend/Nada.java,ClaseQueNoExisteTest.metodoFantasma,N/A,Planificado,' >> "$TMP/badref.csv"
salida2="$(bash "$VALIDADOR" "$TMP/badref.csv" 2>&1)"; codigo2=$?

if [ "$codigo2" -eq 0 ]; then
  fallo "una referencia de prueba inexistente deberia dar exit != 0; dio 0"
fi
if ! printf '%s\n' "$salida2" | grep -q 'ClaseQueNoExisteTest.metodoFantasma'; then
  fallo "no se detecto la referencia de prueba inexistente. Salida: $salida2"
fi

# Caso D: fila con numero de columnas distinto de la cabecera -> VIOLACIÓN.
cp "$MATRIZ_REAL" "$TMP/badcols.csv"
echo 'RF-AUTOTEST3,,solo 9 columnas,,' >> "$TMP/badcols.csv"
salida3="$(bash "$VALIDADOR" "$TMP/badcols.csv" 2>&1)"; codigo3=$?

if [ "$codigo3" -eq 0 ]; then
  fallo "una fila con columnas mal contadas deberia dar exit != 0; dio 0"
fi
if ! printf '%s\n' "$salida3" | grep -q 'RF-AUTOTEST3: 5 columnas, se esperaban 11'; then
  fallo "no se detecto la fila con columnas mal contadas. Salida: $salida3"
fi

# Caso E: estado fuera del vocabulario -> VIOLACIÓN.
cp "$MATRIZ_REAL" "$TMP/badestado.csv"
echo 'RF-AUTOTEST4,CRUD-ORM,fila con estado inventado,HU-00,CU-00,GET /api/nada,backend/Nada.java,N/A,N/A,Medido 2026-08-18,' >> "$TMP/badestado.csv"
salida4="$(bash "$VALIDADOR" "$TMP/badestado.csv" 2>&1)"; codigo4=$?

if [ "$codigo4" -eq 0 ]; then
  fallo "un estado fuera del vocabulario deberia dar exit != 0; dio 0"
fi
if ! printf '%s\n' "$salida4" | grep -q "estado 'Medido 2026-08-18' no pertenece al vocabulario"; then
  fallo "no se detecto el estado fuera del vocabulario. Salida: $salida4"
fi

# Caso F: cita de ruta inexistente en el SRS (A1) -> VIOLACIÓN.
cp "docs/requisitos/SRS.md" "$TMP/badruta.md"
printf -- '- **Origen:** `docs/ruta/falsa/NOEXISTE.md`\n' >> "$TMP/badruta.md"
salida5="$(bash "$VALIDADOR" "$TMP/ok.csv" "$TMP/badruta.md" 2>&1)"; codigo5=$?

if [ "$codigo5" -eq 0 ]; then
  fallo "una cita de ruta inexistente deberia dar exit != 0; dio 0"
fi
if ! printf '%s\n' "$salida5" | grep -q 'docs/ruta/falsa/NOEXISTE.md'; then
  fallo "no se detecto la ruta inexistente citada en el SRS. Salida: $salida5"
fi

echo "OK: el validador falla-cerrado ante fila sin trazabilidad (codigo $codigo), referencia inexistente (codigo $codigo2), columnas mal contadas (codigo $codigo3), estado fuera del vocabulario (codigo $codigo4) y ruta inexistente citada en el SRS (codigo $codigo5)."