# SGED - Entrega Final (Bloque D.1)
# Objetivos exigidos: up, down, test, bench, audit, clean, all
SHELL := /bin/bash
.DEFAULT_GOAL := up

.PHONY: up down test bench reports audit clean schema logs diagrams docs all

## Reproduccion end-to-end en un solo comando desde clonacion limpia (Bloque D.1).
## clean va primero a proposito: garantiza volumen de Postgres nuevo en cada
## corrida, para que db/seed.sql se vuelva a aplicar via docker-entrypoint-initdb.d
## (initdb solo corre esos scripts la primera vez que el volumen existe).
all: clean up test bench reports audit docs
	@echo ""
	@echo "make all: contenedores + pruebas + benchmarks + reportes + auditoria + PDF, todo en verde."

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

## Benchmark k6: 5 corridas independientes, 50 VUs, 30s (Bloque A.1, Entrega Final)
bench:
	mkdir -p docs/mediciones/perf
	for i in 1 2 3 4 5; do \
	  k6 run k6/listado-estudiantes.js \
	    --summary-export docs/mediciones/perf/k6-run$$i.json ; \
	done
	python3 scripts/perf-analysis.py

## Regenera reportes derivados que no dependen de contenedores (SUS, Bloque C.3)
reports:
	python3 scripts/sus-analysis.py

## Auditoría OWASP (Bloque C.2) + auditoría de SQL dinámico
audit:
	bash scripts/audit-owasp.sh
	bash scripts/audit-sql-dynamic.sh

## Compila el documento academico (LaTeX en contenedor: no depende de tener
## TeX Live instalado en el host, igual que `diagrams` usa contenedores para
## structurizr/plantuml). Copia el resultado a docs/informe-final.pdf, la
## ruta que exige la Guia de la Entrega Final (Bloque B / Entregable 3).
## TODO cuando se reestructure el informe a los 18 apartados del Bloque B:
## renombrar docs/informe/main.tex -> docs/informe-final.tex y actualizar
## este objetivo para compilar directo ahi, en vez de copiar al final.
docs:
	docker run --rm -v "$(CURDIR)/docs/informe:/work" -w /work texlive/texlive \
	  sh -c "pdflatex -interaction=nonstopmode main.tex && \
	         bibtex main && \
	         pdflatex -interaction=nonstopmode main.tex && \
	         pdflatex -interaction=nonstopmode main.tex"
	cp docs/informe/main.pdf docs/informe-final.pdf
	@echo "PDF: docs/informe/main.pdf (copiado a docs/informe-final.pdf)"

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
