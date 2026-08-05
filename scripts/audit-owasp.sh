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

# El control A07 de este mismo script agota a proposito los intentos de login.
# El contador de LoginAttemptService se lleva por IP de origen
# (login_attempts:<ip>), no por usuario: seis fallos dejan bloqueado durante 15
# minutos a *cualquier* cuenta que venga de ese equipo, incluida la de admin.
# Sin limpiarlo, una segunda corrida de la auditoria no logra autenticarse y
# A01 devuelve 401 en todo. Ese resultado parece correcto y no lo es: probaria
# que hace falta autenticacion, no que se respetan los roles.
docker exec sged_redis sh -c \
  'redis-cli --scan --pattern "login_attempts:*" | xargs -r redis-cli DEL' \
  > /dev/null 2>&1 \
  || echo "AVISO: no se pudo limpiar el contador de intentos; si A01 sale 401, esa es la causa."

echo "== A01: control de acceso (usuario sin rol pide recurso de admin -> 403) =="
# 0. /api/auth/registro exige rol ADMINISTRADOR (Bloque A.1): iniciamos
#    sesion con el admin sembrado para poder registrar la cuenta de prueba.
curl -s -c /tmp/sged_admin.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin2026!"}' > /dev/null
# 1. el admin registra un usuario basico (rol USER por defecto)
curl -s -b /tmp/sged_admin.jar -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Audit","apellido":"A01","cedula":"0912345678","correo":"audit.a01@sged.test","fechaNacimiento":"2000-01-01","username":"audit_a01@sged.test","password":"Passw0rd!"}' > /dev/null
# 2. el usuario basico inicia su propia sesion
curl -s -c /tmp/sged_a01.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"audit_a01@sged.test","password":"Passw0rd!"}' > /dev/null
{ cabecera "A01 - Broken Access Control";
  echo "-- 1. operacion administrativa sobre estudiantes (esperado 403) --";
  # El cuerpo debe ser un id numerico: la reestructuracion cambio la categoria
  # de texto libre (?categoria=SUB-12) a clave foranea. Con el cuerpo mal
  # formado la peticion moria antes de llegar al control de acceso.
  curl --include -s -b /tmp/sged_a01.jar -X POST \
    "$BASE/api/estudiantes/operaciones/desactivar-categoria" \
    -H "Content-Type: application/json" -d '1';
  echo "";
  echo "";
  # Los cinco recursos que agrego la reestructuracion no tenian @PreAuthorize:
  # con anyRequest().authenticated() bastaba una sesion valida de rol USER para
  # leer y modificar personas, categorias y entrenadores. Se comprueba recurso
  # por recurso para que la regresion no pueda repetirse sin que la auditoria
  # lo muestre.
  echo "-- 2. lectura de datos personales de terceros (esperado 403) --";
  curl -s -o /dev/null -w "GET  /api/personas                 -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/personas";
  curl -s -o /dev/null -w "GET  /api/personas/cedula/0000000000 -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/personas/cedula/0000000000";
  echo "";
  echo "-- 3. escritura sobre catalogos y cuentas (esperado 403) --";
  # Los cuerpos deben ser validos: @Valid se evalua al resolver el argumento,
  # antes que @PreAuthorize, asi que un payload invalido devuelve 422 y tapa
  # el resultado del control de acceso que se quiere evidenciar.
  curl -s -o /dev/null -w "POST   /api/categorias   -> %{http_code}\n" \
    -b /tmp/sged_a01.jar -X POST "$BASE/api/categorias" \
    -H "Content-Type: application/json" \
    -d '{"nombre":"AUDIT-A01","edadMin":10,"edadMax":12,"descripcion":"alta no autorizada"}';
  curl -s -o /dev/null -w "DELETE /api/categorias/1 -> %{http_code}\n" \
    -b /tmp/sged_a01.jar -X DELETE "$BASE/api/categorias/1";
  curl -s -o /dev/null -w "PUT    /api/personas/1   -> %{http_code}\n" \
    -b /tmp/sged_a01.jar -X PUT "$BASE/api/personas/1" \
    -H "Content-Type: application/json" \
    -d '{"nombre":"Alterado","apellido":"PorUsuario","cedula":"0999999999","correo":"alterado@sged.test","fechaNacimiento":"1990-01-01"}';
  curl -s -o /dev/null -w "GET    /api/usuarios     -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/usuarios";
  echo "";
  echo "-- 4. lectura permitida al rol USER (esperado 200: no se rompio el uso legitimo) --";
  curl -s -o /dev/null -w "GET /api/categorias/activas -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/categorias/activas";
  curl -s -o /dev/null -w "GET /api/estados_generales  -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/estados_generales"; \
} > "$OUT/a01-acceso-roto.txt"
echo "  -> $OUT/a01-acceso-roto.txt"

echo "== A01 (roles nuevos): RECEPCIONISTA y REPRESENTANTE =="
# Mismo control que arriba (A01), extendido a los dos roles agregados junto
# con el modulo Representante. No es opcional: standaloneSetup en los tests
# de MockMvc no levanta la cadena de Spring Security, asi que @PreAuthorize
# nunca se evalua ahi (fue exactamente lo que dejo pasar el hallazgo H-08 la
# vez pasada). La unica prueba real de que estos dos roles estan bien
# acotados es contra el backend vivo, aqui.
curl -s -c /tmp/sged_admin.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin2026!"}' > /dev/null

# Dos estudiantes reales cualesquiera, para no depender de ids fijos entre
# corridas: el primero se vincula al representante de prueba (caso legitimo),
# el segundo se usa como "ajeno" para probar el 404 de pertenencia.
IDS_ESTUDIANTES=($(docker exec sged_postgres psql -U postgres -d sged_db -tAc \
  "SELECT id_estudiante FROM academico.estudiantes ORDER BY id_estudiante LIMIT 2;" 2>/dev/null))
EST_PROPIO="${IDS_ESTUDIANTES[0]:-1}"
EST_AJENO="${IDS_ESTUDIANTES[1]:-2}"

curl -s -b /tmp/sged_admin.jar -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Audit","apellido":"Recepcion","cedula":"0912345601","correo":"audit.recepcion@sged.test","fechaNacimiento":"1995-01-01","username":"audit_recepcion@sged.test","password":"Passw0rd!","rol":"RECEPCIONISTA"}' > /dev/null
curl -s -c /tmp/sged_recepcion.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"audit_recepcion@sged.test","password":"Passw0rd!"}' > /dev/null

REGISTRO_REPRESENTANTE=$(curl -s -b /tmp/sged_admin.jar -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Audit","apellido":"Representante","cedula":"0912345602","correo":"audit.representante@sged.test","fechaNacimiento":"1980-01-01","username":"audit_representante@sged.test","password":"Passw0rd!","rol":"REPRESENTANTE"}')
ID_PERSONA=$(echo "$REGISTRO_REPRESENTANTE" | grep -o '"idPersona":[0-9]*' | grep -o '[0-9]*')
ID_USUARIO=$(echo "$REGISTRO_REPRESENTANTE" | grep -o '"idUsuario":[0-9]*' | grep -o '[0-9]*')
curl -s -b /tmp/sged_admin.jar -X POST "$BASE/api/representantes" \
  -H "Content-Type: application/json" \
  -d "{\"idPersona\":$ID_PERSONA,\"idUsuario\":$ID_USUARIO,\"idsEstudiantesIniciales\":[$EST_PROPIO]}" > /dev/null
curl -s -c /tmp/sged_representante.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"audit_representante@sged.test","password":"Passw0rd!"}' > /dev/null

{ cabecera "A01 (roles nuevos) - Broken Access Control";
  echo "-- RECEPCIONISTA: emitir QR de una sesion real (esperado 200, uso legitimo) --";
  ID_SESION=$(docker exec sged_postgres psql -U postgres -d sged_db -tAc \
    "SELECT id_sesion FROM deportivo.sesiones_entrenamiento ORDER BY id_sesion LIMIT 1;" 2>/dev/null)
  curl -s -o /dev/null -w "POST /api/asistencias/qr/sesion/${ID_SESION:-1}/token -> %{http_code}\n" \
    -b /tmp/sged_recepcion.jar -X POST "$BASE/api/asistencias/qr/sesion/${ID_SESION:-1}/token";
  echo "";
  echo "-- RECEPCIONISTA: recursos administrativos ajenos a su funcion (esperado 403) --";
  curl -s -o /dev/null -w "GET  /api/usuarios        -> %{http_code}\n" \
    -b /tmp/sged_recepcion.jar "$BASE/api/usuarios";
  curl -s -o /dev/null -w "POST /api/representantes  -> %{http_code}\n" \
    -b /tmp/sged_recepcion.jar -X POST "$BASE/api/representantes" \
    -H "Content-Type: application/json" -d '{"idPersona":1,"idUsuario":1}';
  echo "";
  echo "-- REPRESENTANTE: sus propios representados (esperado 200) --";
  curl -s -o /dev/null -w "GET /api/representante/estudiantes                        -> %{http_code}\n" \
    -b /tmp/sged_representante.jar "$BASE/api/representante/estudiantes";
  curl -s -o /dev/null -w "GET /api/representante/estudiantes/$EST_PROPIO/informe -> %{http_code}\n" \
    -b /tmp/sged_representante.jar "$BASE/api/representante/estudiantes/$EST_PROPIO/informe";
  echo "";
  echo "-- REPRESENTANTE: un estudiante ajeno, IDOR/BOLA (esperado 404, nunca 200) --";
  curl -s -o /dev/null -w "GET /api/representante/estudiantes/$EST_AJENO/informe -> %{http_code}\n" \
    -b /tmp/sged_representante.jar "$BASE/api/representante/estudiantes/$EST_AJENO/informe";
  echo "";
  echo "-- REPRESENTANTE: listado interno de estudiantes, fuera de su rol (esperado 403) --";
  curl -s -o /dev/null -w "GET /api/estudiantes -> %{http_code}\n" \
    -b /tmp/sged_representante.jar "$BASE/api/estudiantes"; \
} > "$OUT/a01-roles-nuevos.txt"
echo "  -> $OUT/a01-roles-nuevos.txt"
rm -f /tmp/sged_admin.jar /tmp/sged_recepcion.jar /tmp/sged_representante.jar

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
  echo "-- via HTTP directo al backend ($BASE) --";
  curl -I -s "$BASE/api/auth/ping";
  echo "";
  echo "-- via HTTPS/nginx (HSTS solo aplica sobre conexion segura) --";
  curl -Ik -s "https://localhost:8443/api/auth/ping"; \
} > "$OUT/a05-cabeceras.txt"
echo "  -> $OUT/a05-cabeceras.txt"

echo "== A07: 6 intentos fallidos -> 429 =="
{ cabecera "A07 - Identification and Authentication Failures";
  for i in 1 2 3 4 5 6; do
    code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"incorrecta"}');
    echo "--- intento $i -> $code ---";
  done;
  echo "--- intento 7 (respuesta completa, confirma ProblemDetails en el 429) ---";
  curl --include -s -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"incorrecta"}'; \
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
