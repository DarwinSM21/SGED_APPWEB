# SGED - Sistema de Gestión para la Escuela Deportiva ProFútbol

## Descripción del proyecto

SGED (Sistema de Gestión para la Escuela Deportiva ProFútbol) es una aplicación web desarrollada para optimizar la gestión administrativa y deportiva de la escuela. El sistema permitirá administrar estudiantes, entrenadores, asistencias, membresías, equipamiento deportivo, estadísticas de rendimiento y generación de reportes, centralizando toda la información en una única plataforma.

### Tecnologías utilizadas

* Frontend: Angular
* Backend: Spring Boot
* Base de datos: PostgreSQL
* Control de versiones: Git y GitHub

---

## Instrucciones de instalación

### Requisitos previos

Antes de ejecutar el proyecto, asegúrese de tener instalado:

* Node.js (versión LTS recomendada)
* Angular CLI
* Java JDK 21
* Maven
* PostgreSQL
* Git

### Clonar el repositorio

```bash
git https://github.com/DarwinSM21/SGED_APPWEB.git
cd SGED
```

### Configurar y ejecutar el Backend

```bash
cd backend
```

Configurar las credenciales de PostgreSQL en el archivo:

```properties
application.properties
```

Ejecutar:

```bash
mvn spring-boot:run
```

El backend estará disponible en:

```text
http://localhost:8080
```

### Configurar y ejecutar el Frontend

```bash
cd frontend
npm install
ng serve
```

La aplicación estará disponible en:

```text
http://localhost:4200
```

---

## Integrantes

* ARCALLE GREFA DARWIN ORLANDO
* PALLO PINTO ALEJANDRO DANIEL
* VELEZ LOPEZ RICARDO ELIAS

---

## URL del sistema desplegado

Actualmente el sistema se encuentra en fase de desarrollo y aún no dispone de una URL pública de despliegue.
