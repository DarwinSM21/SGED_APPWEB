# SGED — Sistema de Gestión para la Escuela Deportiva ProFútbol

[![CI](https://github.com/DarwinSM21/SGED_APPWEB/actions/workflows/ci.yml/badge.svg)](https://github.com/DarwinSM21/SGED_APPWEB/actions)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.22635766.svg)](https://doi.org/10.5281/zenodo.22635766)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Aplicación web para la gestión administrativa y deportiva de la escuela
ProFútbol: estudiantes, entrenadores, asistencias, evaluaciones y reportes.

**Versión de esta entrega:** `v1.0.2` (Entrega Final, PFC Aplicaciones Web, UTEQ)

## Despliegue público

| | URL |
|---|---|
| **Aplicación (frontend)** | https://sged-frontend-jofa.onrender.com |
| **API (backend)** | https://sged-backend-5nh7.onrender.com |
| Punto de salud | https://sged-backend-5nh7.onrender.com/actuator/health → `{"status":"UP"}` |

Alojado en Render (frontend como sitio estático, backend como contenedor
Docker) con PostgreSQL gestionado en Supabase. El plan gratuito de Render
apaga el backend tras 15 min sin tráfico; la primera petición tras ese lapso
tarda ~1 min en responder mientras arranca en frío. Pasos de despliegue en
[`docs/despliegue/render.md`](docs/despliegue/render.md).

> Los sufijos `-jofa` / `-5nh7` los asignó Render porque los nombres
> `sged-frontend` / `sged-backend` ya estaban tomados globalmente en
> `.onrender.com` por otro despliegue del equipo.

## Pila tecnológica

* Backend: Spring Boot 3.2.x (Java 21 LTS), Spring Data JPA, Spring Security (JWT en cookie HttpOnly), Flyway, Redis
* Frontend: Angular 17+
* Base de datos: PostgreSQL 16 (estrategia híbrida ORM + funciones/procedimientos almacenados)
* Orquestación: Docker Compose (imágenes pinadas por digest sha256)

## Arranque en un solo comando (Bloque B.1)

Requisitos: Docker + Docker Compose + GNU Make.

```bash
git clone https://github.com/DarwinSM21/SGED_APPWEB.git
cd SGED_APPWEB
git checkout v1.0.2
cp .env.example .env
make up
```

En menos de dos minutos:

| Servicio | URL |
|---|---|
| Frontend (HTTPS, recomendado) | https://localhost:8443 |
| Frontend (HTTP, sin cookie de sesion) | http://localhost:4200 |
| API REST | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/docs |
| OpenAPI 3.0 (JSON) | http://localhost:8080/api/docs.json |

El certificado TLS de `https://localhost:8443` es autofirmado (generado en
build, solo para desarrollo/evaluacion) — el navegador va a mostrar una
advertencia de certificado no confiable, es esperado.

Swagger UI y el documento OpenAPI solo están expuestos en local. En el
despliegue público se apagan con `SPRINGDOC_ENABLED=false` (`render.yaml`)
para no publicar una interfaz interactiva sin autenticación (ver
`docs/mediciones/sec/zap/REPORT.md`).

**Credenciales semilla** (definidas en `db/seed.sql`):

```
usuario:    admin
contraseña: sged2026
```

### Credenciales de prueba por rol

Además del admin, el seed trae una cuenta "simple" por rol (misma
contraseña `sged2026`) y 13 cuentas realistas (contraseña
`<usuario>2026`), para que todo el equipo pruebe con los mismos datos.

| Rol | Usuario | Contraseña |
|---|---|---|
| ADMINISTRADOR | `admin` | `sged2026` |
| RECEPCIONISTA | `recepcionista` | `sged2026` |
| ENTRENADOR | `entrenador` | `sged2026` |
| REPRESENTANTE | `representante` | `sged2026` |
| ESTUDIANTE | `estudiante` | `sged2026` |
| RECEPCIONISTA | `anatorresat` | `anatorresat2026` |
| ENTRENADOR | `luisveralv` | `luisveralv2026` |
| ENTRENADOR | `pedrosalazarps` | `pedrosalazarps2026` |
| ENTRENADOR | `diegocastillodc` | `diegocastillodc2026` |
| ENTRENADOR | `marcojimenezmj` | `marcojimenezmj2026` |
| REPRESENTANTE | `rosachuquimarcarc` | `rosachuquimarcarc2026` |
| REPRESENTANTE | `elenavargasev` | `elenavargasev2026` |
| REPRESENTANTE | `fernandoriosfr` | `fernandoriosfr2026` |
| REPRESENTANTE | `patriciagomezpg` | `patriciagomezpg2026` |
| ESTUDIANTE | `kevinandradeka` | `kevinandradeka2026` |
| ESTUDIANTE | `sofiaramirezsr` | `sofiaramirezsr2026` |
| ESTUDIANTE | `mateovillacresmv` | `mateovillacresmv2026` |
| ESTUDIANTE | `valentinaortizvo` | `valentinaortizvo2026` |

De las realistas, `luisveralv` (ENTRENADOR) y `rosachuquimarcarc`
(REPRESENTANTE, vinculada a `kevinandradeka`) tienen ficha completa en
su dominio, además del login; las demás ENTRENADOR/REPRESENTANTE solo
inician sesión. Las 5 cuentas ESTUDIANTE (`estudiante`, `kevinandradeka`,
`sofiaramirezsr`, `mateovillacresmv`, `valentinaortizvo`) sí tienen
ficha completa todas — sin ella, `/api/asistencias/qr/marcar` rechaza
al estudiante y no puede marcar asistencia por QR. RECEPCIONISTA y ADMINISTRADOR no tienen
tabla de dominio propia en este esquema.

## Objetivos Make

| Comando | Acción |
|---|---|
| `make up` | Levanta el sistema completo desde clonación limpia |
| `make down` | Apaga los contenedores |
| `make test` | Pruebas JUnit 5 + reporte de cobertura JaCoCo |
| `make bench` | 3 corridas k6 (50 VUs, 30 s) + análisis con IC 95 % |
| `make audit` | Auditoría OWASP (6 controles) + auditoría de SQL dinámico |
| `make clean` | Limpia contenedores, volúmenes y builds |

## Estructura del repositorio

Sigue la estructura obligatoria de la guía de la entrega:
`db/` (schema, seed, procs), `docs/` (requisitos, observaciones, adr,
mediciones, trazabilidad, ética), `k6/`, `scripts/`, `.github/workflows/`.

## Evidencia y reproducibilidad

* **Informe de la Entrega Final (PDF):**
  [`docs/informe/main.pdf`](docs/informe/main.pdf) — 54 páginas,
  cerrado en la etiqueta `v1.0.2`.
* Fuente del informe: [`docs/informe/main.tex`](docs/informe/main.tex),
  compilable con `pdflatex→bibtex→pdflatex→pdflatex`. El PDF de arriba se
  genera de aquí: existe fuente versionada y es reproducible, a diferencia
  de un PDF suelto sin `.tex`/`.docx`, que no sería evidencia verificable
  (Bloque 0 / P4).
* Mediciones crudas: `docs/mediciones/` (perf, sec, sus, lighthouse, jacoco)
* Matriz de trazabilidad: `docs/trazabilidad/matriz.csv`
* Catálogo de procedimientos: `docs/basedatos/CATALOGO-SP.md`
* Video de demostración: PENDIENTE (enlace) — falta grabarlo y enlazarlo
* DOI Zenodo del software: [`10.5281/zenodo.22635766`](https://doi.org/10.5281/zenodo.22635766) — emitido para el corte `v1.0.1` de `DarwinSM21/SGED_APPWEB` (concept DOI: `10.5281/zenodo.21713239`); la entrega avanzó a `v1.0.2` después de esa publicación (recuperación de contraseña y correcciones del SRS) — pendiente publicar una nueva versión en Zenodo sobre ese concept DOI para que quede archivado el corte vigente
* DOI Zenodo del *dataset*: [`10.5281/zenodo.22422305`](https://doi.org/10.5281/zenodo.22422305) — publicado, versión 1.0.0, CC BY 4.0
* Lighthouse SEO: 63 (intencional, ver REPORT.md §3 — privacidad de datos de menores)

## Integrantes

* ARCALLE GREFA DARWIN ORLANDO
* PALLO PINTO ALEJANDRO DANIEL
* VELEZ LOPEZ RICARDO ELIAS

Roles CRediT: ver `CONTRIBUTORS.md`.

## Licencia

MIT — ver `LICENSE`.
