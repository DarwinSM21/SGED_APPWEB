# Etapa 1: compilar Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa 2: servir con nginx
FROM nginx:1.25-alpine

# Certificado autofirmado de desarrollo para TLS en el proxy (Bloque C.2,
# OWASP A02). Se genera en build, nunca se versiona una clave privada.
RUN apk add --no-cache openssl \
    && mkdir -p /etc/nginx/certs \
    && openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
       -keyout /etc/nginx/certs/dev.key -out /etc/nginx/certs/dev.crt \
       -subj "/CN=localhost" \
       -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

COPY nginx.conf /etc/nginx/conf.d/default.conf
# Angular 17+ application builder deja la app en dist/<proyecto>/browser
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80 443
