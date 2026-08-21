# Exposición pública con Cloudflare Tunnel

Publica el stack local en internet con una URL y un **certificado TLS real**,
sin tarjeta de crédito y sin cambiar nada del código.

## Qué resuelve y qué no

**Resuelve** el hallazgo H-05 de `docs/etica/ETHICS.md` en la sesión expuesta:
el certificado deja de ser autofirmado y pasa a ser uno válido de Cloudflare.

**No resuelve** la independencia del equipo: la aplicación sigue corriendo en
la máquina local, así que la URL solo responde mientras esa máquina esté
encendida, con Docker levantado y con el túnel abierto. Para un despliegue
permanente ver `fly-io.md`.

## Requisitos

- Docker Desktop en ejecución con el stack arriba (`make up` o
  `docker compose up -d`).
- `cloudflared` instalado:

```bash
winget install --id Cloudflare.cloudflared
```

## Levantar el túnel

### Atajo

Doble clic en `scripts/exponer-publico.bat`. Comprueba Docker, levanta el
stack, espera a que el backend responda y abre el túnel. La dirección aparece
al final; **la ventana debe quedar abierta**, al cerrarla se cae el túnel.

### A mano


```bash
cloudflared tunnel --url http://localhost:4200 --protocol http2
```

`--protocol http2` no es opcional en la práctica: por defecto `cloudflared`
usa QUIC sobre UDP, que muchas redes bloquean o degradan. Sin esa bandera el
arranque falla con `failed to request quick Tunnel: context deadline
exceeded`, que parece un problema de configuración pero es de red.

El comando imprime la URL pública, del estilo
`https://<palabras-aleatorias>.trycloudflare.com`. **Cambia en cada
ejecución**: si necesitas una URL estable hay que crear un túnel con nombre y
una cuenta de Cloudflare.

Apunta a `http://localhost:4200` (el nginx en HTTP), no a `:8443`. Cloudflare
ya pone el TLS por delante; atravesar el TLS autofirmado obligaría a
deshabilitar la verificación del certificado, que es justo lo contrario de lo
que se busca.

## Importante: `COOKIE_SECURE`

Con el túnel, `.env` debe tener:

```
COOKIE_SECURE=true
```

Sin eso las cookies de sesión salen **sin el atributo `Secure`**, lo que
contradice lo que declara el Bloque A.1 y deja el token expuesto a viajar por
HTTP plano. Se detectó exactamente así, inspeccionando la respuesta real del
despliegue:

```
Set-Cookie: sged_access=<jwt>; Path=/api; HttpOnly; SameSite=Strict
```

Con la corrección:

```
Set-Cookie: sged_access=<jwt>; Path=/api; Secure; HttpOnly; SameSite=Strict
```

`true` es seguro también en local: los navegadores tratan `http://localhost`
como origen confiable y aceptan cookies `Secure` ahí. El único caso que se
rompe es entrar por HTTP plano desde **otro dispositivo** (el celular contra
`http://192.168.x.x:4200`). Para probar el QR desde el teléfono, usa la URL
del túnel: va por HTTPS y funciona.

## Verificar

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://TU-URL.trycloudflare.com/
```

```bash
curl -s -D - -o /dev/null -X POST https://TU-URL.trycloudflare.com/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"TU_PASSWORD"}' | grep -i set-cookie
```

Debe responder `200` y las dos cookies deben mostrar `Secure; HttpOnly;
SameSite=Strict`.

## Antes de exponerla a terceros

`db/seed.sql` trae una contraseña conocida para `admin`. Aceptable en una red
local, **no** en una URL pública: cámbiala antes de compartir el enlace.

Ten presente además que el túnel gratuito no tiene garantía de disponibilidad
—lo advierte el propio `cloudflared` al arrancar— y que la URL cambia en cada
ejecución. Para una demo evaluada conviene levantarlo con antelación y
comprobar que responde justo antes de empezar.
