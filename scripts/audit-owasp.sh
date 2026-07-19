#!/usr/bin/env bash
# Auditoría automática de los 6 controles OWASP mínimos (Bloque C.2).
# Guarda la evidencia cruda en docs/mediciones/sec/ con fecha y commit.
set -uo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
OUT="docs/mediciones/sec"
mkdir -p "$OUT"
FECHA=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "sin-git")

cabecera() {
  echo "# Evidencia OWASP $1"
  echo "# Fecha: $FECHA | Commit: $COMMIT | curl: $(curl --version | head -1)"
  echo "#"
}

echo "== A01: control de acceso (usuario sin rol pide recurso de admin -> 403) =="
# 1. login como usuario básico
curl -s -c /tmp/sged_a01.jar -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Audit","apellido":"A01","username":"audit_a01","password":"Passw0rd!"}' > /dev/null
curl -s -c /tmp/sged_a01.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"audit_a01","password":"Passw0rd!"}' > /dev/null
{ cabecera "A01 - Broken Access Control";
  curl --include -s -b /tmp/sged_a01.jar -X POST \
    "$BASE/api/estudiantes/operaciones/desactivar-categoria?categoria=SUB-12"; \
} > "$OUT/a01-acceso-roto.txt"
echo "  -> $OUT/a01-acceso-roto.txt"

echo "== A02: criptografía en tránsito (TLS 1.3) =="
{ cabecera "A02 - Cryptographic Failures";
  curl -vk "https://localhost:8443/actuator/health" 2>&1 | grep -Ei "TLS|SSL|cipher|subject|HTTP" || \
  echo "NOTA: TLS expuesto por el proxy/nginx en despliegue; capturar contra el puerto 8443/443 del entorno con TLS."; \
} > "$OUT/a02-tls.txt"
echo "  -> $OUT/a02-tls.txt"

echo "== A03: inyección (payload ' OR '1'='1 -> 422 ProblemDetails) =="
{ cabecera "A03 - Injection";
  curl --include -s -b /tmp/sged_a01.jar -X POST "$BASE/api/estudiantes" \
    -H "Content-Type: application/json" \
    -d "{\"nombre\":\"' OR '1'='1\",\"apellido\":\"\",\"categoria\":\"' OR '1'='1\"}"; \
} > "$OUT/a03-inyeccion.txt"
echo "  -> $OUT/a03-inyeccion.txt"

echo "== A05: cabeceras de seguridad =="
{ cabecera "A05 - Security Misconfiguration";
  curl -I -s "$BASE/api/auth/ping"; \
} > "$OUT/a05-cabeceras.txt"
echo "  -> $OUT/a05-cabeceras.txt"

echo "== A07: 6 intentos fallidos -> 429 =="
{ cabecera "A07 - Identification and Authentication Failures";
  for i in 1 2 3 4 5 6 7; do
    echo "--- intento $i ---";
    curl --include -s -X POST "$BASE/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"incorrecta"}' | head -1;
  done; \
} > "$OUT/a07-rate-limit.txt"
echo "  -> $OUT/a07-rate-limit.txt"

echo "== A09: log de autenticación con ip, timestamp, sub =="
{ cabecera "A09 - Security Logging and Monitoring Failures";
  docker exec sged_backend sh -c 'grep -E "AUTH_LOGIN_(OK|FAIL)" logs/sged-auth.log | tail -20' 2>/dev/null || \
  grep -E "AUTH_LOGIN_(OK|FAIL)" backend/logs/sged-auth.log 2>/dev/null | tail -20 || \
  echo "Ejecutar tras algunos logins; el log vive en logs/sged-auth.log del backend."; \
} > "$OUT/a09-logging.txt"
echo "  -> $OUT/a09-logging.txt"

echo "Auditoría OWASP completada. Evidencia en $OUT/"
