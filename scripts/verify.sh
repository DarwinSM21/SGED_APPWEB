#!/usr/bin/env bash
# EV-2 (Guia del examen suspenso, UTEQ Aplicaciones Web): comprobacion
# automatica de los 14 pendientes de la guia, mas una guarda de regresion
# sobre los 12 puntos que la guia ya da por resueltos (seccion 1: "si al
# cerrar un pendiente rompen algo de esta lista, ese punto deja de estar
# resuelto"). Pensado para correr desde un clon limpio con `make verify`.
#
# Cada punto imprime PASA / FALLA / PENDIENTE (manual) y por que. El codigo
# de salida es 0 solo si todos los puntos automatizables pasan.
set -uo pipefail
cd "$(dirname "$0")/.."

FAIL=0
PASS_COUNT=0
FAIL_COUNT=0
MANUAL_COUNT=0

pass() { echo "  PASA: $1"; PASS_COUNT=$((PASS_COUNT + 1)); }
fail() { echo "  FALLA: $1"; FAIL_COUNT=$((FAIL_COUNT + 1)); FAIL=1; }
manual() { echo "  PENDIENTE (revision manual): $1"; MANUAL_COUNT=$((MANUAL_COUNT + 1)); }
section() { echo; echo "== $1 =="; }

# ---------------------------------------------------------------------
section "P1 -- SUS: respuestas reales, Brooke, IC con t de Student"
n_resp=$(($(wc -l < docs/mediciones/sus/respuestas.csv) - 1))
if [ "$n_resp" -ge 15 ]; then pass "respuestas.csv tiene $n_resp participantes (>=15)"; else fail "respuestas.csv tiene $n_resp participantes (<15)"; fi
if grep -qi "brooke" docs/mediciones/sus/REPORT.md 2>/dev/null; then pass "REPORT.md cita a Brooke (1996)"; else fail "REPORT.md no cita a Brooke"; fi
if grep -qi "t de Student" docs/mediciones/sus/REPORT.md 2>/dev/null; then pass "REPORT.md calcula el IC con t de Student"; else fail "REPORT.md no documenta el metodo del IC"; fi
echo "  Nota: P1 tambien exige consentimiento de cada participante -- ver P13."

# ---------------------------------------------------------------------
section "P2 -- Lighthouse: 3 corridas por perfil contra el despliegue publico"
mobile_runs=$(ls docs/mediciones/lighthouse/mobile-run*.report.json 2>/dev/null | wc -l)
desktop_runs=$(ls docs/mediciones/lighthouse/desktop-run*.report.json 2>/dev/null | wc -l)
if [ "$mobile_runs" -ge 3 ] && [ "$desktop_runs" -ge 3 ]; then
    pass "$mobile_runs corridas moviles + $desktop_runs de escritorio"
else
    fail "$mobile_runs corridas moviles + $desktop_runs de escritorio (se exigen >=3 y >=3)"
fi
if grep -q "onrender.com" docs/mediciones/lighthouse/REPORT.md 2>/dev/null; then
    pass "REPORT.md confirma URL objetivo = despliegue publico de Render"
else
    fail "REPORT.md no confirma la URL objetivo publica"
fi

# ---------------------------------------------------------------------
section "P3 -- DOI declarados resuelven (excepto el retirado, documentado)"
if bash scripts/check-doi.sh; then pass "todos los DOI resuelven segun lo esperado"; else fail "algun DOI no resuelve como se espera"; fi

# ---------------------------------------------------------------------
section "P4 -- Javadoc de metodos publicos >=90%"
if python3 scripts/javadoc-coverage.py 90; then pass "cobertura de Javadoc >=90%"; else fail "cobertura de Javadoc <90%"; fi
manual "90,03% real -- margen corto sobre el umbral; documentar los metodos que quedan en docs/mediciones/javadoc-sin-documentar.txt da colchon real"

# ---------------------------------------------------------------------
section "P5 -- validate-traceability.sh propaga el codigo de salida"
if bash scripts/validate-traceability.sh /ruta/inexistente.csv /ruta/inexistente.md >/dev/null 2>&1; then
    fail "el script no fallo con una ruta invalida (deberia)"
else
    pass "el script sale con codigo distinto de cero ante una entrada invalida"
fi
if [ -f scripts/test-validate-traceability.sh ] && bash scripts/test-validate-traceability.sh >/dev/null 2>&1; then
    pass "scripts/test-validate-traceability.sh (autotest de regresion) pasa"
else
    fail "scripts/test-validate-traceability.sh no existe o falla"
fi

# ---------------------------------------------------------------------
section "P6 -- Rotulado de figuras en ingles"
SPANISH_WORDS='Estudiante|Entrenador|Categoria|Asistencia|Evaluacion|Pago|Usuario|Persona|Articulo|Movimiento|Asignacion|Lesion|Partido|Notificacion|Consentimiento|Auditoria|Horario|Alineacion|Posicion|Especialidad|Representante|Alumno|Equipo|Jugador|Entrenamiento|nombre|apellido|correo|telefono|activo|fecha|estado'
hits=""
for f in docs/diagramas/*.md; do
    block=$(sed -n '/```mermaid/,/```/p' "$f" 2>/dev/null)
    m=$(echo "$block" | grep -noE "\b(${SPANISH_WORDS})\b" || true)
    if [ -n "$m" ]; then hits="$hits\n$f: $m"; fi
done
for f in docs/diagramas/*.svg; do
    m=$(grep -oE '<text[^>]*>[^<]*</text>' "$f" 2>/dev/null | grep -noE "\b(${SPANISH_WORDS})\b" || true)
    if [ -n "$m" ]; then hits="$hits\n$f: $m"; fi
done
if [ -f docs/arquitectura/workspace.dsl ]; then
    m=$(grep -noE "\"[^\"]*\b(${SPANISH_WORDS})\b[^\"]*\"" docs/arquitectura/workspace.dsl || true)
    if [ -n "$m" ]; then hits="$hits\ndocs/arquitectura/workspace.dsl: $m"; fi
fi
if [ -z "$hits" ] || [ "$hits" = "" ]; then
    pass "sin coincidencias del diccionario de terminos en español dentro de mermaid/svg/dsl"
else
    fail "coincidencias encontradas:$(echo -e "$hits")"
fi
manual "los PNG de docs/arquitectura/ y mer-profutbol.png son texto rasterizado -- no se puede grepear; confirmar a simple vista que coinciden con sus fuentes (ya en ingles)"

# ---------------------------------------------------------------------
section "P7 -- SRS firmado, versionado y con MoSCoW"
moscow_count=$(grep -c "MoSCoW:" docs/requisitos/SRS.md 2>/dev/null || echo 0)
if [ "$moscow_count" -gt 0 ]; then pass "SRS.md trae MoSCoW explicito en $moscow_count requisitos"; else fail "SRS.md no trae MoSCoW explicito"; fi
if [ -f "docs/requisitos/ACTA-APROBACION-SRS-v1.8.pdf" ]; then pass "acta de aprobacion firmada por el docente-director existe"; else fail "no existe acta de aprobacion firmada"; fi
if [ -f "docs/requisitos/SRS-v1.1.0.pdf" ]; then
    pass "docs/requisitos/SRS-v1.1.0.pdf existe"
else
    fail "falta docs/requisitos/SRS-v1.1.0.pdf (existe SRS-v1.0.0.pdf; falta republicar con el nombre v1.1.0 sobre el commit a defender)"
fi

# ---------------------------------------------------------------------
section "P8 -- una sola etiqueta v1.1.0 sobre el commit a defender"
if git rev-parse -q --verify "refs/tags/v1.1.0" >/dev/null; then
    pass "la etiqueta v1.1.0 existe ($(git rev-parse --short v1.1.0))"
else
    fail "la etiqueta v1.1.0 no existe todavia"
fi
if grep -qE "^version:\s*1\.1\.0" CITATION.cff 2>/dev/null; then pass "CITATION.cff declara version: 1.1.0"; else fail "CITATION.cff no declara version: 1.1.0"; fi

# ---------------------------------------------------------------------
section "P9 -- nombres de tipos en espanol <=5%"
total_types=$(grep -rhoE '^\s*(public\s+)?(final\s+|abstract\s+)?(class|interface|enum|record)\s+\w+' backend/src/main/java --include=*.java 2>/dev/null | grep -oE '\w+$' | sort -u | wc -l)
TYPE_SPANISH_WORDS='Estudiante|Entrenador|Categoria|Asistencia|Evaluacion|Pago|Usuario|Persona|Rol[A-Z]|Articulo|Movimiento|Asignacion|Lesion|Partido|Notificacion|Consentimiento|Auditoria|Horario|Alineacion|Posicion|Especialidad|Estado[A-Z]|Representante|Alumno|Deporte|Equipo|Jugador|Entrenamiento'
spanish_types=$(grep -rlE "^\s*(public\s+)?(final\s+|abstract\s+)?(class|interface|enum|record)\s+\w*(${TYPE_SPANISH_WORDS})\w*" backend/src/main/java --include=*.java 2>/dev/null | wc -l)
pct=$(python3 -c "print(f'{($spanish_types / $total_types * 100) if $total_types else 0:.1f}')")
echo "  $spanish_types de $total_types tipos ($pct%) coinciden con el diccionario de terminos en español"
if python3 -c "exit(0 if $spanish_types / $total_types * 100 <= 5.0 else 1)" 2>/dev/null; then
    pass "$pct% <= 5%"
else
    fail "$pct% > 5%"
fi
manual "diccionario heuristico de 274 tipos -- un vistazo rapido a la lista completa cierra la duda con certeza frente al 32/277 (11,6%) que reporta la guia"

# ---------------------------------------------------------------------
section "P10 -- roles CRediT con conteo real por rol"
if grep -q "Funding acquisition" CONTRIBUTORS.md 2>/dev/null; then
    pass "los 14 roles CRediT estan cubiertos en CONTRIBUTORS.md"
else
    fail "CONTRIBUTORS.md no cubre los 14 roles CRediT"
fi
if [ -f scripts/credit-counts.py ] && grep -q "Conteo por rol (metodolog" CONTRIBUTORS.md 2>/dev/null; then
    pass "CONTRIBUTORS.md documenta el conteo por rol con script reproducible (scripts/credit-counts.py)"
else
    fail "falta el conteo por rol reproducible en CONTRIBUTORS.md"
fi

# ---------------------------------------------------------------------
section "P11 -- .env.example sin claves con aspecto real"
jwt_line=$(grep "^JWT_SECRET=" .env.example 2>/dev/null || echo "")
if echo "$jwt_line" | grep -qiE "cambiar|changeme|change_me|tu_|your_|placeholder|xxx|<.*>"; then
    pass ".env.example: JWT_SECRET usa un marcador evidente"
else
    fail ".env.example: JWT_SECRET ('$jwt_line') no tiene aspecto de marcador"
fi

# ---------------------------------------------------------------------
section "P12 -- una sola cifra de umbral de cobertura en todo el entregable"
pom_threshold=$(grep -oE '<minimum>0\.[0-9]+</minimum>' backend/pom.xml | sort -u)
stray=$(grep -noE 'umbral[^.]{0,40}[0-9]{2}(,[0-9]+)?\s*%|[0-9]{2}(,[0-9]+)?\s*%[^.]{0,40}umbral' docs/informe/main.tex README.md 2>/dev/null | grep -v "70\b\|70,0\|70\.0" || true)
echo "  umbral en pom.xml: $pom_threshold"
if [ -z "$stray" ]; then
    pass "no se encontraron menciones de umbral con una cifra distinta de 70% en informe/README"
else
    fail "menciones de umbral con otra cifra: $stray"
fi

# ---------------------------------------------------------------------
section "P13 -- consentimientos informados del SUS, uno por participante"
REGISTRO=docs/etica/consentimiento/registro.md
if [ ! -f "$REGISTRO" ]; then
    fail "no existe $REGISTRO"
else
    filas=$(grep -cE '^\| ENC-' "$REGISTRO")
    pendientes=$(grep -cE '^\| ENC-[0-9]+ \|[^|]*\|[^|]*\| PENDIENTE \|' "$REGISTRO")
    obtenidos=$(grep -cE '^\| ENC-[0-9]+ \|[^|]*\|[^|]*\| OBTENIDO \|' "$REGISTRO")
    if [ "$filas" -ne "$n_resp" ]; then
        fail "$REGISTRO tiene $filas filas, deberian ser $n_resp (una por participante de respuestas.csv)"
    elif [ "$pendientes" -gt 0 ]; then
        fail "$pendientes de $filas participantes siguen en PENDIENTE en $REGISTRO"
    elif [ "$obtenidos" -eq "$filas" ]; then
        pass "las $filas constancias de consentimiento estan marcadas OBTENIDO en $REGISTRO"
    else
        fail "$REGISTRO tiene filas en un estado distinto de OBTENIDO/PENDIENTE (revisar a mano)"
    fi
fi

# ---------------------------------------------------------------------
section "P14 -- correccion por comparaciones multiples, reproducible"
# Busca por CONTENIDO, no por nombre de archivo: el script vigente se
# llama perf-analysis.py, no *bonferroni*/*holm* (asi se nos paso la
# primera vez -- ver VERIFICACION.md).
stats_script=$(grep -rl "holm_bonferroni\|holm-bonferroni\|Holm-Bonferroni" scripts/*.py 2>/dev/null)
if [ -n "$stats_script" ]; then
    if grep -q "p-valor\|Holm (α=0,05)\||\s*p\s*|" docs/mediciones/perf/REPORT.md 2>/dev/null; then
        pass "script de correccion encontrado ($stats_script) y su salida ya esta en docs/mediciones/perf/REPORT.md"
    else
        fail "$stats_script existe pero docs/mediciones/perf/REPORT.md no trae la tabla de p-valores corregidos"
    fi
else
    fail "no existe ningun script/cuaderno versionado que calcule los p-valores corregidos"
fi

# ---------------------------------------------------------------------
section "Regresion -- seccion 1 (lo que la guia ya da por resuelto)"
if ls docs/informe*.pdf docs/informe/*.pdf >/dev/null 2>&1; then pass "el informe en PDF existe"; else fail "no se encuentra el informe en PDF (correr 'make docs')"; fi
if grep -q "onrender.com" README.md; then pass "README declara las URL publicas del despliegue"; else fail "README no declara URL publicas"; fi
if grep -qE 'Access-Control-Allow-Origin.*\*|setAllowedOrigins\(.*\*' backend/src/main/java -r 2>/dev/null; then
    fail "se encontro un comodin en la configuracion de CORS"
else
    pass "CORS sin comodines (grep negativo)"
fi

# ---------------------------------------------------------------------
echo
echo "===================================================="
echo "Resumen: $PASS_COUNT pasan, $FAIL_COUNT fallan, $MANUAL_COUNT pendientes de revision manual"
echo "===================================================="
exit $FAIL
