@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.2
set DB_PASSWORD=123
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sged_db
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=123
set REDIS_HOST=localhost
set FLYWAY_ENABLED=true
cd /d %~dp0
mvnw.cmd spring-boot:run
