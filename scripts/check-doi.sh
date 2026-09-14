#!/usr/bin/env bash
# Resuelve cada DOI declarado en README.md/CITATION.cff. El DOI marcado como
# retirado en el propio README (10.5281/zenodo.22635766, tombstone en Zenodo)
# se comprueba aparte: se exige que devuelva 410 (confirma que sigue
# retirado) y que el texto que lo cita siga diciendo explicitamente que no
# debe citarse. El resto de DOI deben resolver a 200.
set -euo pipefail

RETIRED_DOI="10.5281/zenodo.22635766"
fail=0

dois=$(grep -oE '10\.5281/zenodo\.[0-9]+' README.md CITATION.cff 2>/dev/null | cut -d: -f2 | sort -u)

if [ -z "$dois" ]; then
    echo "No se encontro ningun DOI 10.5281/zenodo.* en README.md/CITATION.cff"
    exit 1
fi

for doi in $dois; do
    code=$(curl -sL -o /dev/null -w "%{http_code}" --max-time 15 "https://doi.org/${doi}" || echo "000")
    if [ "$doi" = "$RETIRED_DOI" ]; then
        if [ "$code" = "410" ]; then
            if grep -q "$RETIRED_DOI" README.md && grep -A2 "$RETIRED_DOI" README.md | grep -qi "no debe citarse\|retirado\|tombstone"; then
                echo "OK   $doi -> $code (retirado, documentado como tal; no se cita como vigente)"
            else
                echo "FAIL $doi -> $code, pero el README no explica que esta retirado"
                fail=1
            fi
        else
            echo "FAIL $doi -> $code (se esperaba 410: si volvio a estar disponible, hay que decidir si se cita)"
            fail=1
        fi
        continue
    fi

    if [ "$code" = "200" ]; then
        echo "OK   $doi -> $code"
    else
        echo "FAIL $doi -> $code (se esperaba 200)"
        fail=1
    fi
done

exit $fail
