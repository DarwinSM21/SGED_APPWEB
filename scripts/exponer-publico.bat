@echo off
REM ============================================================
REM  SGED - Publica la aplicacion en internet con HTTPS real
REM
REM  Levanta el stack y abre un tunel de Cloudflare. Doble clic y
REM  esperar; al final imprime la direccion publica.
REM
REM  IMPORTANTE: deja esta ventana ABIERTA. Al cerrarla se cae el
REM  tunel y la direccion deja de funcionar.
REM ============================================================

cd /d "%~dp0.."

echo.
echo  === 1/3  Comprobando Docker ===
docker info >nul 2>&1
if errorlevel 1 (
    echo.
    echo  ERROR: Docker Desktop no esta corriendo.
    echo  Abrelo, espera a que termine de iniciar y vuelve a ejecutar esto.
    echo.
    pause
    exit /b 1
)
echo  Docker OK

echo.
echo  === 2/3  Levantando la aplicacion ===
docker compose up -d
if errorlevel 1 (
    echo.
    echo  ERROR: no se pudo levantar el stack.
    pause
    exit /b 1
)

echo  Esperando a que el backend responda...
:esperar
timeout /t 3 /nobreak >nul
for /f "tokens=*" %%s in ('docker inspect -f "{{.State.Health.Status}}" sged_backend 2^>nul') do set ESTADO=%%s
if not "%ESTADO%"=="healthy" goto esperar
echo  Backend listo

echo.
echo  === 3/3  Abriendo el tunel ===
echo.
echo  Busca abajo la direccion que termina en .trycloudflare.com
echo  Esa es la URL publica. CAMBIA cada vez que se ejecuta esto.
echo.

REM --protocol http2 es obligatorio: por defecto cloudflared usa QUIC
REM sobre UDP, que muchas redes bloquean, y falla con
REM "context deadline exceeded" -que parece un error de configuracion
REM pero es de red-.
"C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel --url http://localhost:4200 --protocol http2 --no-autoupdate

echo.
echo  El tunel se cerro. La direccion publica ya no funciona.
pause
