@echo off
REM ============================================================
REM  SGED - Equivalente a 'make' para Windows
REM
REM  Uso:   make.bat [comando]
REM
REM  Comandos: up, down, test, bench, reports, audit, clean,
REM            schema, logs, all
REM
REM  Requiere: Docker Desktop + Git Bash (para scripts .sh).
REM  En Windows sin make, usa este archivo o Git Bash.
REM ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "CMD=%~1"
if "%CMD%"=="" set "CMD=up"

if /I "%CMD%"=="up" (
    docker compose up -d --build
    echo Esperando a que el backend este saludable...
    :health
    timeout /t 3 /nobreak >nul
    for /f %%h in ('docker inspect --format="{{.State.Health.Status}}" sged_backend 2^>nul') do set "HS=%%h"
    if not "!HS!"=="healthy" goto health
    echo.
    echo  SGED operativo:
    echo    Frontend (HTTPS, recomendado): https://localhost:8443
    echo    Frontend (HTTP):               http://localhost:4200
    echo    API      :                     http://localhost:8080/api
    echo    Credenciales seed: admin / sged2026
    goto end
)

if /I "%CMD%"=="down" (
    docker compose down
    goto end
)

if /I "%CMD%"=="test" (
    cd backend
    call mvnw.cmd -B clean test
    cd ..
    echo Reporte JaCoCo: backend\target\site\jacoco\index.html
    goto end
)

if /I "%CMD%"=="clean" (
    docker compose down -v --remove-orphans
    cd backend
    call mvnw.cmd -q clean
    cd ..
    if exist frontend\dist rmdir /s /q frontend\dist
    goto end
)

if /I "%CMD%"=="bench" (
    echo Necesita k6 + Git Bash. Ejecuta: bash scripts\perf.sh
    goto end
)

if /I "%CMD%"=="reports" (
    python scripts\sus-analysis.py
    goto end
)

if /I "%CMD%"=="audit" (
    echo Necesita Git Bash. Ejecuta: bash scripts\audit-owasp.sh ^&^& bash scripts\audit-sql-dynamic.sh
    goto end
)

if /I "%CMD%"=="schema" (
    echo db/schema.sql no se genera automaticamente.
    echo Es un esquema consolidado, mas estricto que las migraciones.
    goto end
)

if /I "%CMD%"=="logs" (
    docker compose logs -f backend
    goto end
)

if /I "%CMD%"=="all" (
    call make.bat clean
    call make.bat up
    call make.bat test
    echo: all: contenedores + tests listos (bench/reports/audit requieren Unix tools)
    goto end
)

echo Comando desconocido: %CMD%
echo.
echo Uso: make.bat [up^|down^|test^|clean^|bench^|reports^|audit^|schema^|logs^|all]

:end
endlocal
