# Etapa 1: compilar Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa 2: servir con nginx
FROM nginx:1.25-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Angular 17+ application builder deja la app en dist/<proyecto>/browser
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
