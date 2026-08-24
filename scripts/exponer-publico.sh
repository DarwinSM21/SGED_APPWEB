#!/usr/bin/env bash
# ============================================================
#  SGED - Publica la aplicacion en internet con HTTPS real
#
#  Levanta el stack y abre un tunel de Cloudflare. Ejecuta y
#  espera; al final imprime la direccion publica.
#
#  IMPORTANTE: deja esta terminal ABIERTA. Al cerrarla se cae
#  el tunel y la direccion deja de funcionar.
# ============================================================

set -euo pipefail
cd "$(dirname "$0")/.."

echo ""
echo " === 1/3  Comprobando Docker ==="
if ! docker info > /dev/null 2>&1; then
  echo ""
  echo "  ERROR: Docker no esta corriendo."
  echo "  Ejecuta: sudo systemctl start docker"
  exit 1
fi
echo "  Docker OK"

echo ""
echo " === 2/3  Levantando la aplicacion ==="
docker compose up -d
if [ $? -ne 0 ]; then
  echo ""
  echo "  ERROR: no se pudo levantar el stack."
  exit 1
fi

echo "  Esperando a que el backend responda..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' sged_backend 2>/dev/null)" = "healthy" ]; do
  sleep 3
done
echo "  Backend listo"

echo ""
echo " === 3/3  Abriendo el tunel ==="
echo ""
echo "  Busca abajo la direccion que termina en .trycloudflare.com"
echo "  Esa es la URL publica. CAMBIA cada vez que se ejecuta esto."
echo ""

cloudflared tunnel --url http://localhost:4200 --protocol http2 --no-autoupdate
