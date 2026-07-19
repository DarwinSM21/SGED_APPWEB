# ADR-006: Estrategia de Despliegue y Orquestación de Servicios

## Estado
Aprobado

## Contexto
El equipo de desarrollo necesita asegurar que el entorno de desarrollo local, el entorno de pruebas y el entorno de producción final sean idénticos, eliminando el clásico problema de "en mi máquina sí funciona". Adicionalmente, considerando el crecimiento de la pila tecnológica (Backend en Spring Boot, Frontend en Angular, y un servidor en memoria Redis), se requiere un método de empaquetado, distribución y orquestación que simplifique el aprovisionamiento y permita un despliegue ágil en plataformas cloud (como VPS, Render, AWS o plataformas similares).

## Decisión
Adoptar la **Contenedorización** integral de toda la arquitectura de la aplicación mediante **Docker**, y gestionar los entornos a través de **Docker Compose** para la orquestación local y de ambientes pre-producción. Las directrices establecidas son:

1.  **Backend:** Se creará un `Dockerfile` multi-etapa (Multi-stage build) basado en una imagen de JDK 24 para compilar el código fuente con Maven y generar un archivo JAR optimizado y ligero sobre una imagen de ejecución (JRE) alpina mínima.
2.  **Frontend:** Se creará un `Dockerfile` que compilará el código de Angular CLI utilizando Node.js 24 y, en su etapa final, servirá los archivos estáticos optimizados utilizando un servidor web **Nginx**, configurado correctamente para manejar el enrutamiento SPA (*Single Page Application*).
3.  **Orquestación (`docker-compose.yml`):** Se diseñará un archivo de orquestación unificado que levante y conecte de forma transparente en una misma red virtual aislada cuatro servicios esenciales con sus respectivos controles de salud (*healthchecks*): el contenedor del backend, el del frontend, la instancia local de Redis para desarrollo, y las variables de entorno inyectadas de forma segura a través de archivos `.env`.

## Consecuencias
*   **Positivas:**
    *   **Portabilidad absoluta:** La aplicación completa puede ejecutarse de manera idéntica en cualquier sistema operativo que soporte Docker.
    *   **Aislamiento:** Los servicios se comunican internamente mediante nombres de servicio DNS de Docker, protegiendo los puertos sensibles del exterior.
    *   **Facilidad de despliegue en la nube:** Preparación nativa para canalizaciones de Integración y Despliegue Continuo (CI/CD) hacia plataformas basadas en contenedores.
*   **Negativas / Desafíos:**
    *   **Consumo de recursos en desarrollo:** Ejecutar múltiples contenedores de forma simultánea en las máquinas locales de los desarrolladores exige una mayor cantidad de memoria RAM y almacenamiento en disco.
    *   **Curva de aprendizaje:** Todo el equipo debe dominar los comandos básicos de Docker y la gestión de volúmenes para evitar pérdidas accidentales de configuraciones locales.