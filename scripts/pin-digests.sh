#!/usr/bin/env bash
# Pina las imágenes del docker-compose.yml por digest sha256 (Bloque B.1).
# Requiere Docker en ejecución y acceso a Docker Hub.
set -euo pipefail

pin() {
  local imagen="$1"
  docker pull -q "$imagen" >/dev/null
  local digest
  digest=$(docker inspect --format='{{index .RepoDigests 0}}' "$imagen")
  echo "  $imagen -> $digest"
  # Reemplaza 'image: <imagen>' por 'image: <repo>@sha256:...'
  sed -i.bak "s|image: ${imagen}[[:space:]]*# PIN_DIGEST|image: ${digest}|" docker-compose.yml
}

echo "Pinando imágenes por digest sha256..."
pin "postgres:16"
pin "redis:7"
rm -f docker-compose.yml.bak
echo "Listo. Verifique con: grep 'image:' docker-compose.yml"
