#!/usr/bin/env bash
# Valida la matriz de trazabilidad (Bloque A.3.3):
#   1. Todo requisito debe tener al menos una historia, un caso de uso o una
#      prueba asociada.
#   2. Toda referencia "Clase.metodo" de la columna prueba_automatizada tiene
#      que resolver a un metodo real en backend/src/test.
#
# Uso:  bash scripts/validate-traceability.sh [ruta/al/matriz.csv]
# (la ruta opcional existe para el autotest scripts/test-validate-traceability.sh)
set -euo pipefail

MATRIZ="${1:-docs/trazabilidad/matriz.csv}"

if [ ! -f "$MATRIZ" ]; then
  echo "ERROR: no existe $MATRIZ"
  exit 1
fi

# Columnas reales, verificadas contra la cabecera del propio archivo -no
# contra lo que este script suponia antes-:
# requisito,tipo_acceso,descripcion_corta,historia_usuario,caso_uso,
# endpoint_o_componente,archivo_implementacion,prueba_automatizada,
# evidencia_empirica,estado
#
# No hay columna de prioridad MoSCoW en este CSV: la comprobacion de
# "Must pendiente" que existia antes leia una columna que no es esa -el
# desplazamiento la hacia caer siempre en un campo vacio, asi que nunca
# se disparaba- y se quita en vez de dejarla fingiendo que funciona. Esa
# prioridad vive en el SRS (docs/requisitos/SRS.md), no aqui.
FALLO=0

# --- Comprobacion 1: trazabilidad minima por requisito ---------------------
while IFS=',' read -r req acceso desc hu cu ep archivo prueba evid estado; do
  if [ -z "$hu" ] && [ -z "$cu" ] && [ -z "$prueba" ]; then
    echo "VIOLACIÓN: $req no tiene historia, caso de uso ni prueba."
    FALLO=1
  fi
done < <(tail -n +2 "$MATRIZ")
# El "done < <(...)" en vez de "... | while ...; done" importa: con una
# tuberia, el while corre en un subshell y FALLO=1 se pierde al salir de
# el, asi que el exit code de mas abajo daba siempre 0 sin importar
# cuantas VIOLACIÓN se hubieran impreso. Con sustitucion de proceso el
# bucle corre en este mismo shell y FALLO sale con el valor real.

# --- Comprobacion 2: las referencias de prueba citadas existen -------------
# La observacion de la Entrega Final encontro nueve referencias que
# apuntaban a metodos renombrados o a la clase equivocada. Se corrigieron;
# esto impide que vuelva a pasar sin que el CI lo note. Solo se validan las
# referencias con la forma "ClaseTest.metodo" (las entradas tipo
# "ClaseTest (6)" o en prosa se dejan como estan).
if [ -d backend/src/test ]; then
  REFS=$(tail -n +2 "$MATRIZ" | cut -d',' -f8 \
         | grep -oE '[A-Za-z0-9_]+Tests?\.[A-Za-z0-9_]+' | sort -u || true)
  while IFS= read -r ref; do
    if [ -z "$ref" ]; then continue; fi
    clase="${ref%%.*}"
    metodo="${ref#*.}"
    archivo=$(find backend/src/test -name "${clase}.java" -print -quit 2>/dev/null || true)
    if [ -z "$archivo" ]; then
      echo "VIOLACIÓN: la matriz cita $ref pero no existe la clase de prueba $clase."
      FALLO=1
    elif ! grep -qE "void[[:space:]]+${metodo}[[:space:]]*\(" "$archivo"; then
      echo "VIOLACIÓN: la matriz cita $ref pero $clase no tiene un método $metodo."
      FALLO=1
    fi
  done <<< "$REFS"
fi

echo "Validación de trazabilidad completada."
exit $FALLO
