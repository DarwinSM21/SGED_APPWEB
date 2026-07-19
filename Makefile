# SGED - Tercera Entrega (Bloque B.1)
# Objetivos exigidos: up, down, test, bench, audit, clean
SHELL := /bin/bash

.PHONY: up down test bench audit clean schema logs

## Levanta el sistema completo desde clonación limpia (un solo comando)
up:
	docker compose up -d --build
	@echo "Esperando a que el backend esté saludable..."
	@until docker inspect --format='{{.State.Health.Status}}' sged_backend 2>/dev/null | grep -q healthy; do sleep 3; printf '.'; done
	@echo ""
	@echo "SGED operativo:"
	@echo "  Frontend : http://localhost:4200"
	@echo "  API      : http://localhost:8080/api"
	@echo "  OpenAPI  : http://localhost:8080/api/docs"
	@echo "  Credenciales seed: admin / Admin2026!"

## Apaga y elimina contenedores
down:
	docker compose down

## Ejecuta las pruebas JUnit con reporte JaCoCo
test:
	cd backend && ./mvnw -B test
	@echo "Reporte JaCoCo: backend/target/site/jacoco/index.html"

## Benchmark k6: 3 corridas independientes, 50 VUs, 30s (Bloque C.1)
bench:
	mkdir -p docs/mediciones/perf
	for i in 1 2 3; do \
	  k6 run k6/listado-estudiantes.js \
	    --summary-export docs/mediciones/perf/k6-run$$i.json ; \
	done
	python3 scripts/perf-analysis.py

## Auditoría OWASP (Bloque C.2) + auditoría de SQL dinámico
audit:
	bash scripts/audit-owasp.sh
	bash scripts/audit-sql-dynamic.sh

## Limpia contenedores, volúmenes y artefactos de build
clean:
	docker compose down -v --remove-orphans
	cd backend && ./mvnw -q clean || true
	rm -rf frontend/dist

## Regenera db/schema.sql a partir de las migraciones (uso interno)
schema:
	cat backend/src/main/resources/db/migration/V*.sql > db/schema.sql

logs:
	docker compose logs -f backend
