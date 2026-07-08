#!/usr/bin/env bash
# Audita que no exista SQL dinámico ni concatenación de entrada de usuario
# (regla transversal 7 de la rúbrica: tumba C1 y C6 automáticamente).
set -uo pipefail

FALLO=0

echo "== Auditoría de SQL dinámico y concatenación =="

# 1. EXECUTE dinámico dentro de procedimientos/funciones
if grep -rniE "EXECUTE[[:space:]]+(IMMEDIATE|format|')" db/procs backend/src/main/resources/db 2>/dev/null; then
  echo "VIOLACIÓN: SQL dinámico detectado en procedimientos."
  FALLO=1
fi

# 2. Concatenación en @Query / createQuery / createNativeQuery en Java
if grep -rn --include="*.java" -E '(createQuery|createNativeQuery|@Query)\(.*"\s*\+' backend/src/main/java 2>/dev/null; then
  echo "VIOLACIÓN: concatenación de strings en queries Java."
  FALLO=1
fi

if [ "$FALLO" -eq 0 ]; then
  echo "OK: sin SQL dinámico ni concatenación de entrada de usuario."
fi
exit $FALLO
