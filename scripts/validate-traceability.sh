#!/usr/bin/env bash
# Valida la matriz de trazabilidad (Bloque A.3.3): todo requisito debe tener
# al menos una historia, un caso de uso o una prueba asociada.
set -euo pipefail

MATRIZ="docs/trazabilidad/matriz.csv"

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

echo "Validación de trazabilidad completada."
exit $FALLO
