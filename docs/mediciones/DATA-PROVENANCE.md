# Procedencia de datos — SGED ProFútbol (Bloque F)

Para cada tabla y figura del informe (`docs/informe/main.tex`) que reporta
un número o un diagrama derivado de evidencia, esta tabla declara qué
archivo crudo lo origina, qué comando/script lo produce, y en qué
`commit` se generó por última vez. Regla del Bloque C: si una celda de
esta tabla dice "generado", el número es regenerable con un solo comando
desde el dato crudo; si dice "manual", se declara así en vez de
presentarlo como automático — no todo el informe alcanza hoy el nivel de
automatización que exige el badge más alto de la ACM, y es más honesto
decirlo aquí que dejarlo implícito.

| Tabla/figura en el informe | Archivo(s) crudo(s) | Comando/script que lo produce | Último commit de generación |
|---|---|---|---|
| `tab:k6-resultados` (Rendimiento, §Evidencia empírica) | `docs/mediciones/perf/k6-run1.json` … `k6-run5.json` (5 corridas, Bloque A.1) | `make bench` → `scripts/perf-analysis.py` → `docs/mediciones/perf/REPORT.md` (generado) | `73d5114` (regenerado 2026-08-14 contra el sistema local, tras corregir el `.env` apuntado a Supabase) |
| `tab:owasp` (Seguridad OWASP) | `docs/mediciones/sec/a01-acceso-roto.txt`, `a02-tls.txt`, `a03-inyeccion.txt`, `a05-cabeceras.txt`, `a07-rate-limit.txt`, `a09-logging.txt` | `make audit` → `scripts/audit-owasp.sh` genera los `.txt` (generado); la tabla resumen del informe se transcribe a mano de esos archivos (manual) | `73d5114` (regenerado 2026-08-14) |
| `tab:acceso-por-recurso` (H-08, control de acceso por recurso) | `docs/mediciones/sec/a01-roles-nuevos.txt`, `a01-pagos-y-estudiante.txt` | `make audit` → `scripts/audit-owasp.sh` (generado); tabla de criterios por recurso transcrita a mano (manual) | `73d5114` (regenerado 2026-08-14) |
| `tab:pruebas-junit` (Cobertura JaCoCo) | `docs/mediciones/jacoco/jacoco.csv`, `jacoco.xml`, `index.html` | `make test` (`clean test`, plugin JaCoCo) (generado) | `fbc3bb6` (última modificación real del archivo — no `73d5114`, que es de horas antes ese mismo día. Estado actual: 86,12 % líneas, 71,21 % branches, 479 pruebas — cumple el 70 % del Bloque A.1, ver `docs/informe/main.tex` sección~\ref{sec:cobertura}) |
| `tab:lighthouse` (Calidad web) | `docs/mediciones/lighthouse/mobile-run{1,2,3}.report.json`, `desktop-run{1,2,3}.report.json` (6 corridas, Bloque A.1) | Lighthouse CLI conectado a Chrome en contenedor Docker (`zenika/alpine-chrome`, ya que el entorno no tiene navegador local) → `docs/mediciones/lighthouse/REPORT.md` (generado) | `73d5114` (regenerado 2026-08-14, primer perfil desktop archivado — antes solo existía móvil) |
| `tab:sus-agregado`, `tab:sus-perfil` (Usabilidad SUS) | `docs/mediciones/sus/respuestas.csv` | `make reports` → `scripts/sus-analysis.py` → `docs/mediciones/sus/REPORT.md` (generado) | `5176ad4` |
| §Análisis estático (SpotBugs/find-sec-bugs, Bloque A.2.3) | `backend/spotbugs-security-include.xml` + código fuente de `backend/src/` | `mvn com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:check` → `docs/mediciones/sec/static-analysis/spotbugsXml.xml` (generado; integrado en `.github/workflows/ci.yml`) | `73d5114` (nuevo, 2026-08-14) |
| §Escaneo OWASP ZAP baseline (Bloque A.1) | Sistema en ejecución (`docker compose up`, con `SPRINGDOC_ENABLED=false`), sin dato crudo previo | `zap.yaml` (plan de automatización) → `docs/mediciones/sec/zap/zap-report.{html,json,xml}` (generado) | 0fc8b69 (regenerado 2026-09-02: la corrida de `73d5114`/2026-08-14 encontró una alerta alta de DOMPurify en Swagger UI; esta repite el escaneo tras apagar esa interfaz en producción — 0 hallazgos, ver §Corrida anterior en `docs/mediciones/sec/zap/REPORT.md`) |
| `tab:procs` (Procedimientos almacenados, Bloque A.2.1) | `db/procs/*.sql` (7 archivos) | `docs/basedatos/CATALOGO-SP.md`, mantenido a mano contra el código fuente SQL (manual) | ver `git log -- db/procs/` por archivo |
| `tab:reparto-orm-sp` | Regla de diseño de `ADR-006-acceso_datos.md` | Documentación manual, no derivada de una medición (manual) | — |
| `fig:c4-l1`, `fig:c4-l2`, `fig:c4-l3` (Arquitectura C4) | `docs/arquitectura/workspace.dsl` | `make diagrams` (Structurizr CLI + PlantUML, contenedorizado) (generado) | `5579e53` |
| `fig:prisma` (Diagrama de flujo, Trabajos relacionados) | Registro de búsqueda del equipo (no archivado como CSV/JSON aparte) | Diagrama TikZ construido a mano a partir de los conteos declarados en el texto (manual) | — |
| `tab:credit-cifras` (CRediT, evidencia de autoría) | Historial de `git` de la rama `main` | `git log --pretty="AUTOR:%an" --numstat main` (comando declarado en el propio informe; reproducible por cualquiera con acceso al repositorio, pero no hay un `.csv` archivado con la salida) (generado, no archivado) | — (recalculable en cualquier momento) |
| `tab:cobertura` de `docs/basedatos/CATALOGO-SP.md` / matriz de trazabilidad | `docs/trazabilidad/matriz.csv` | Mantenida a mano contra el código y el SRS (manual) | ver `git log -- docs/trazabilidad/matriz.csv` |

## Pendiente declarado

Tres huecos de automatización quedan abiertos para antes del cierre de
la Entrega Final, consistentes con el Bloque D.1 (reproducibilidad en un
solo comando):

1. La tabla de OWASP y la de control de acceso por recurso se transcriben
   a mano de los `.txt` crudos — no hay un `scripts/` que las genere. Un
   `scripts/owasp-resumen.py` que parseara los seis archivos y emitiera
   la tabla en Markdown cerraría esta brecha.
2. El diagrama PRISMA (`fig:prisma`) no tiene un registro de búsqueda
   archivado (CSV con cada cadena de búsqueda, base de datos y resultado
   crudo) del que derivarse — hoy son conteos declarados directamente en
   el texto.
3. La evidencia de CRediT (`tab:credit-cifras`) es reproducible por
   comando pero no se archiva su salida cruda; un `docs/mediciones/credit/git-log-numstat.txt`
   generado por `make` cerraría la trazabilidad completa.
