@echo off
title SGED - Arrancando todos los servicios...
echo ============================================
echo   SGED - Sistema de Gestion Escuela Deportiva
echo ============================================
echo.

echo [1/4] Verificando PostgreSQL...
tasklist /FI "IMAGENAME eq postgres.exe" | find /I "postgres" >nul
if %errorlevel% neq 0 (
    echo       PostgreSQL no esta corriendo. Iniciando...
    "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe" start -D "C:\Program Files\PostgreSQL\18\data" -w
    timeout /t 3 >nul
) else (
    echo       PostgreSQL ya esta corriendo.
)

echo [2/4] Verificando Redis...
tasklist /FI "IMAGENAME eq redis-server.exe" | find /I "redis" >nul
if %errorlevel% neq 0 (
    echo       Redis no esta corriendo. Iniciando...
    start "" /B "C:\Users\Admin\Documents\Redis\redis-server.exe" "C:\Users\Admin\Documents\Redis\redis.windows-service.conf"
    timeout /t 2 >nul
) else (
    echo       Redis ya esta corriendo.
)

echo [3/4] Iniciando Backend (Spring Boot)...
cd /d C:\Users\Admin\Documents\GitHub\SGED_APPWEB\backend
start "SGED Backend" cmd /k "title SGED Backend && set JAVA_HOME=C:\Program Files\Java\jdk-25.0.2 && set DB_PASSWORD=123 && set REDIS_HOST=localhost && mvnw.cmd spring-boot:run"

echo [4/4] Iniciando Frontend (Angular)...
cd /d C:\Users\Admin\Documents\GitHub\SGED_APPWEB\frontend
start "SGED Frontend" cmd /k "title SGED Frontend && set PATH=C:\Program Files\nodejs;%%PATH%% && npx ng serve"

echo.
echo ============================================
echo   Todos los servicios se estan iniciando...
echo   Espera ~15 segundos y abre en tu navegador:
echo.
echo   http://localhost:4200
echo ============================================
echo.
echo Presiona cualquier tecla para cerrar esta ventana...
pause >nul
