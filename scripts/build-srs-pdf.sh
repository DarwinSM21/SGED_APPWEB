#!/usr/bin/env bash
# Genera docs/requisitos/SRS.pdf a partir de docs/requisitos/SRS.md.
#
# No usa el mismo pipeline que el informe (pdflatex/LaTeX): el SRS trae
# firmas embebidas con <img> HTML crudo y emoji de estado (checkmark/
# cuadro en blanco) que LaTeX no reproduce de forma confiable (glifos
# faltantes en las fuentes por defecto, <img> HTML descartado al
# renderizar a LaTeX). Este script va Markdown -> HTML autocontenido
# (pandoc, incrusta las firmas como data URI) -> PDF (WeasyPrint, motor
# de render HTML/CSS real, con Noto Color Emoji instalado para que
# el checkmark y el cuadro en blanco salgan con su glifo real).
#
# Requiere Docker. Uso:
#   bash scripts/build-srs-pdf.sh [ruta-salida.pdf]
# Por defecto escribe docs/requisitos/SRS.pdf.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="${1:-docs/requisitos/SRS.pdf}"
WORKDIR="docs/requisitos"
TMP_HTML="$(mktemp -u).html"
TMP_HTML_NAME="$(basename "$TMP_HTML")"

MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd)/$WORKDIR:/work" -w /work python:3.12-slim bash -c "
  set -e
  apt-get update -qq
  apt-get install -y -qq pandoc fonts-noto-color-emoji fonts-noto-core \
    libpango-1.0-0 libpangoft2-1.0-0 libharfbuzz-subset0 >/dev/null
  pip install -q weasyprint
  fc-cache -f >/dev/null
  pandoc SRS.md -o '$TMP_HTML_NAME' --standalone --embed-resources --metadata title='SRS'
  weasyprint '$TMP_HTML_NAME' '$(basename "$OUT")'
  rm -f '$TMP_HTML_NAME'
"

echo "Generado: $OUT"
