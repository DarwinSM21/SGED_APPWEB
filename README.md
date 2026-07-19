# SGED — Sistema de Gestión para la Escuela Deportiva ProFútbol

[![CI](https://github.com/DarwinSM21/SGED_APPWEB/actions/workflows/ci.yml/badge.svg)](https://github.com/DarwinSM21/SGED_APPWEB/actions)
[![DOI](https://zenodo.org/badge/DOI/PENDIENTE.svg)](https://doi.org/PENDIENTE)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Aplicación web para la gestión administrativa y deportiva de la escuela
ProFútbol: estudiantes, entrenadores, asistencias, evaluaciones y reportes.

**Versión de esta entrega:** `v0.9.0-rc` (Tercera Entrega, PFC Aplicaciones Web, UTEQ)

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
git checkout v0.9.0-rc
cp .env.example .env
make up
```

En menos de dos minutos:

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API REST | http://localhost:8080/api |
| OpenAPI 3.0 | http://localhost:8080/api/docs |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |

**Credenciales semilla** (definidas en `db/seed.sql`):

```
usuario:    admin
contraseña: Admin2026!
```

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

Sigue la estructura obligatoria de la guía de la Tercera Entrega:
`db/` (schema, seed, procs), `docs/` (requisitos, observaciones, adr,
mediciones, trazabilidad, ética), `k6/`, `scripts/`, `.github/workflows/`.

## Evidencia y reproducibilidad

* Mediciones crudas: `docs/mediciones/` (perf, sec, sus, lighthouse, jacoco)
* Matriz de trazabilidad: `docs/trazabilidad/matriz.csv`
* Catálogo de procedimientos: `docs/basedatos/CATALOGO-SP.md`
* Video de demostración: PENDIENTE (enlace)
* DOI Zenodo: PENDIENTE

## Integrantes

* ARCALLE GREFA DARWIN ORLANDO
* PALLO PINTO ALEJANDRO DANIEL
* VELEZ LOPEZ RICARDO ELIAS

Roles CRediT: ver `CONTRIBUTORS.md`.

## Licencia

MIT — ver `LICENSE`.
