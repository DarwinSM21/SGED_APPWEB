#!/usr/bin/env bash
# Valida la matriz de trazabilidad (Bloque A.3.3): todo requisito debe tener
# al menos una historia, un caso de uso o una prueba asociada.
set -euo pipefail

MATRIZ="docs/trazabilidad/matriz.csv"

if [ ! -f "$MATRIZ" ]; then
  echo "ERROR: no existe $MATRIZ"
  exit 1
fi

# Columnas esperadas:
# id_requisito,tipo,prioridad_moscow,historia_usuario,caso_de_uso,modulo_codigo,
# endpoint_api,prueba_automatizada,tipo_acceso,evidencia_empirica,estado
FALLO=0
tail -n +2 "$MATRIZ" | while IFS=',' read -r req tipo prio hu cu mod ep prueba acceso evid estado; do
  if [ -z "$hu" ] && [ -z "$cu" ] && [ -z "$prueba" ]; then
    echo "VIOLACIÓN: $req no tiene historia, caso de uso ni prueba."
    FALLO=1
  fi
  if [ "$prio" = "Must" ] && [ "$estado" = "pendiente" ]; then
    echo "ADVERTENCIA: requisito Must pendiente: $req"
  fi
done

echo "Validación de trazabilidad completada."
exit $FALLO
