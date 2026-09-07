#!/usr/bin/env python3
"""Genera un resumen Markdown recalculable a partir de los .txt crudos de
docs/mediciones/sec/ (evidencia OWASP capturada con curl).

Cierra el hueco #1 declarado en docs/mediciones/DATA-PROVENANCE.md: hoy
tab:owasp y tab:acceso-por-recurso se transcriben a mano de esos .txt.
Este script no reemplaza esa transcripcion linea por linea -- la columna
"Evidencia observada" del informe interpreta y resume, y eso sigue siendo
trabajo humano -- pero automatiza lo que SI es mecanico: que codigos HTTP
aparecieron y que cabeceras de seguridad estan presentes en cada archivo,
para que quien audite pueda verificar la transcripcion manual contra un
resumen generado por comando, no solo releyendo los .txt a ojo.

Los 6 archivos tienen formato distinto entre si (dump de headers plano,
salida verbose de curl con prefijos '<'/'>'/'*', lineas de log) porque
cada uno se capturo con un comando curl distinto segun lo que ese control
necesitaba mostrar. El parseo es deliberadamente tolerante a eso: busca
patrones (codigos de estado, nombres de cabecera) en vez de asumir una
estructura de lineas fija.

Uso: python3 scripts/owasp-resumen.py
Salida: docs/mediciones/sec/RESUMEN-GENERADO.md
"""
import re
from pathlib import Path

SEC_DIR = Path(__file__).resolve().parent.parent / "docs" / "mediciones" / "sec"

ARCHIVOS = [
    ("A01", "Broken Access Control", "a01-acceso-roto.txt"),
    ("A02", "Cryptographic Failures", "a02-tls.txt"),
    ("A03", "Injection", "a03-inyeccion.txt"),
    ("A05", "Security Misconfiguration", "a05-cabeceras.txt"),
    ("A07", "Identification and Authentication Failures", "a07-rate-limit.txt"),
    ("A09", "Security Logging and Monitoring Failures", "a09-logging.txt"),
]

# Cabeceras de seguridad que interesa confirmar presentes/ausentes.
CABECERAS = [
    "X-Content-Type-Options",
    "X-Frame-Options",
    "Content-Security-Policy",
    "Strict-Transport-Security",
    "Cache-Control",
]

# Dos formatos de codigo de estado conviven en estos archivos:
# 1) dump de cabeceras completo: 'HTTP/1.1 403' o, con curl -v, '< HTTP/1.1 200'
# 2) resumen compacto de un lote de peticiones: 'GET /api/x -> 403'
RE_STATUS_HEADER = re.compile(r"^[<\s]*HTTP/[\d.]+\s+(\d{3})", re.MULTILINE)
RE_STATUS_COMPACTO = re.compile(r"->\s*(\d{3})\b")
RE_LOG_EVENT = re.compile(r"\b(AUTH_LOGIN_OK|AUTH_LOGIN_FAIL)\b")


def analizar(path: Path) -> dict:
    texto = path.read_text(encoding="utf-8", errors="replace")
    codigos = RE_STATUS_HEADER.findall(texto) + RE_STATUS_COMPACTO.findall(texto)
    cabeceras_presentes = [c for c in CABECERAS if re.search(rf"^{re.escape(c)}:", texto, re.MULTILINE | re.IGNORECASE)]
    eventos_log = RE_LOG_EVENT.findall(texto)
    return {
        "codigos_http": sorted(set(codigos), key=int) if codigos else [],
        "conteo_codigos": {c: codigos.count(c) for c in sorted(set(codigos), key=int)},
        "cabeceras_presentes": cabeceras_presentes,
        "eventos_log": {e: eventos_log.count(e) for e in sorted(set(eventos_log))} if eventos_log else {},
        "lineas": texto.count("\n") + 1,
    }


def main():
    filas = []
    for control, nombre, archivo in ARCHIVOS:
        path = SEC_DIR / archivo
        if not path.exists():
            filas.append(f"| {control} | {nombre} | `{archivo}` | **ARCHIVO NO ENCONTRADO** | | |")
            continue
        r = analizar(path)
        codigos_str = ", ".join(f"`{c}`×{n}" for c, n in r["conteo_codigos"].items()) or "(sin código HTTP detectado)"
        cabeceras_str = ", ".join(f"`{c}`" for c in r["cabeceras_presentes"]) or "(ninguna de las buscadas)"
        eventos_str = ", ".join(f"`{e}`×{n}" for e, n in r["eventos_log"].items()) or "—"
        filas.append(f"| {control} | {nombre} | `{archivo}` ({r['lineas']} líneas) | {codigos_str} | {cabeceras_str} | {eventos_str} |")

    out = ["# Resumen generado — evidencia OWASP cruda", "",
           "Generado por `scripts/owasp-resumen.py` a partir de los `.txt` de este mismo",
           "directorio. **No reemplaza** la columna \"Evidencia observada\" de",
           "`tab:owasp`/`tab:acceso-por-recurso` en el informe (esa interpreta y resume;",
           "esto solo extrae lo mecánicamente verificable: códigos HTTP, presencia de",
           "cabeceras de seguridad, eventos de auditoría). Cierra el hueco #1 de",
           "`docs/mediciones/DATA-PROVENANCE.md`: permite verificar por comando que la",
           "transcripción manual no omite ni inventa nada.", "",
           "| Control | Nombre OWASP | Archivo | Códigos HTTP | Cabeceras de seguridad presentes | Eventos de auditoría |",
           "|---|---|---|---|---|---|"]
    out.extend(filas)
    out.append("")
    out.append(f"Total de archivos analizados: {len(ARCHIVOS)}.")

    dest = SEC_DIR / "RESUMEN-GENERADO.md"
    dest.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"Escrito: {dest}")


if __name__ == "__main__":
    main()
