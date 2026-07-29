# Etapa 1: compilar con Maven
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiar archivos de configuración de Maven primero (aprovecha la caché de Docker)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Convertir saltos de línea Windows (CRLF) a Linux (LF) por si ejecutas desde Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar
COPY src src
RUN ./mvnw package -DskipTests -B

# Etapa 2: imagen de producción con JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]