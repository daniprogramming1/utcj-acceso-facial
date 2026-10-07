#!/usr/bin/env bash
# Descarga un MobileFaceNet .tflite de código abierto (licencia permisiva) a
# app/src/main/assets/models/mobilefacenet.tflite
#
# Origen por defecto: MCarlomagno/FaceRecognitionAuth (BSD-3-Clause), modelo
# MobileFaceNet 112×112 → embedding 192-d, normalización (p-127.5)/128.
# El código de entrenamiento original de MobileFaceNet (sirius-ai) es Apache-2.0.
#
# Uso:
#   ./scripts/download_mobilefacenet.sh
#   MODEL_URL=https://... ./scripts/download_mobilefacenet.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST_DIR="$ROOT/app/src/main/assets/models"
DEST="$DEST_DIR/mobilefacenet.tflite"

# BSD-3-Clause demo app que incluye mobilefacenet.tflite (~5 MB)
DEFAULT_URL="https://github.com/MCarlomagno/FaceRecognitionAuth/raw/master/assets/mobilefacenet.tflite"
URL="${MODEL_URL:-$DEFAULT_URL}"

mkdir -p "$DEST_DIR"

if [[ -f "$DEST" ]]; then
  SIZE=$(wc -c < "$DEST" | tr -d ' ')
  if [[ "$SIZE" -gt 100000 ]]; then
    echo "Ya existe $DEST (${SIZE} bytes). Borra el archivo para volver a descargar."
    exit 0
  fi
  echo "Archivo existente demasiado pequeño (${SIZE} bytes); se reemplaza."
fi

echo "Descargando MobileFaceNet TFLite desde:"
echo "  $URL"
echo "→ $DEST"

TMP="$(mktemp)"
cleanup() { rm -f "$TMP"; }
trap cleanup EXIT

if command -v curl >/dev/null 2>&1; then
  curl -fL --retry 3 --retry-delay 2 -o "$TMP" "$URL"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$TMP" "$URL"
else
  echo "Error: se necesita curl o wget." >&2
  exit 1
fi

SIZE=$(wc -c < "$TMP" | tr -d ' ')
if [[ "$SIZE" -lt 100000 ]]; then
  echo "Error: la descarga parece incompleta (${SIZE} bytes)." >&2
  exit 1
fi

# Comprobación mínima de cabecera TFLite (flatbuffer suele empezar tras meta; al menos no es HTML)
if file "$TMP" 2>/dev/null | grep -qi "HTML\|ASCII text"; then
  echo "Error: la URL devolvió texto/HTML, no un modelo binario." >&2
  exit 1
fi

mv "$TMP" "$DEST"
trap - EXIT
echo "Listo: $DEST (${SIZE} bytes)"
echo ""
echo "IMPORTANTE: tras cambiar de modelo, los alumnos deben volver a registrarse."
echo "Umbral coseno sugerido (MobileFaceNet): 0.60  (Configuración en la app)."
echo "Compila de nuevo: ./gradlew assembleDebug"
