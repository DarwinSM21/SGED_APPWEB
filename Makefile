# SGED - Tercera Entrega (Bloque B.1)
# Objetivos exigidos: up, down, test, bench, audit, clean
SHELL := /bin/bash

.PHONY: up down test bench audit clean schema logs diagrams

## Levanta el sistema completo desde clonación limpia (un solo comando)
up:
	docker compose up -d --build
	@echo "Esperando a que el backend esté saludable..."
	@until docker inspect --format='{{.State.Health.Status}}' sged_backend 2>/dev/null | grep -q healthy; do sleep 3; printf '.'; done
	@echo ""
	@echo "SGED operativo:"
	@echo "  Frontend (HTTPS, recomendado): https://localhost:8443"
	@echo "  Frontend (HTTP, sin cookie de sesion): http://localhost:4200"
	@echo "  API      : http://localhost:8080/api"
	@echo "  OpenAPI  : http://localhost:8080/api/docs"
	@echo "  Credenciales seed: admin / Admin2026!"
	@echo "  Nota: el certificado TLS es autofirmado (desarrollo); el navegador va a advertir, es esperado."

## Apaga y elimina contenedores
down:
	docker compose down

## Ejecuta las pruebas JUnit con reporte JaCoCo
## `clean` es obligatorio, no una precaución: sin él, JaCoCo instrumenta los
## .class que queden en target/ de compilaciones anteriores. Tras la
## reestructuración de paquetes eso produjo un reporte que incluía paquetes
## ya inexistentes (org.uteq.backend.auth.*, org.uteq.backend.estudiante.*)
## y por lo tanto un porcentaje de cobertura no verificable.
test:
	cd backend && ./mvnw -B clean test
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

## Regenera los PNG del modelo C4 desde docs/arquitectura/workspace.dsl
## (Bloque D). Los PNG son artefactos derivados: no se editan a mano.
## Nota: la imagen structurizr/cli quedó deprecada y su entrypoint solo
## imprime un aviso sin exportar nada; hay que usar structurizr/structurizr.
diagrams:
	docker run --rm -v "$(CURDIR)/docs/arquitectura:/work" -w /work \
	  structurizr/structurizr:latest \
	  export -workspace workspace.dsl -format plantuml/c4plantuml
	docker run --rm -v "$(CURDIR)/docs/arquitectura:/work" -w /work \
	  plantuml/plantuml:latest -tpng "structurizr-*.puml"
	cd docs/arquitectura && \
	  mv -f structurizr-C4_Nivel1_Contexto.png L1-contexto.png && \
	  mv -f structurizr-C4_Nivel2_Contenedores.png L2-contenedores.png && \
	  mv -f structurizr-C4_Nivel3_Componentes_API.png L3-componentes.png && \
	  rm -f structurizr-*.puml
	@echo "Diagramas C4 regenerados en docs/arquitectura/"

## Regenera db/schema.sql a partir de las migraciones (uso interno)
schema:
	cat backend/src/main/resources/db/migration/V*.sql > db/schema.sql

logs:
	docker compose logs -f backend
