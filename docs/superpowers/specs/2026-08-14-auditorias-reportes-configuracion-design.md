# Diseño: Auditorías, Reportes PDF y Configuración

**Fecha**: 2026-08-14
**Estado**: Aprobado para plan de implementación

## Contexto

SGED (Spring Boot 3 + Angular 21, sin librerías de componentes UI, roles vía
`@PreAuthorize("hasRole(...)")` en backend y `roleGuard` por ruta en
frontend) no tiene hoy: auditoría de cambios, generación de PDF, ni tema
oscuro/preferencias de apariencia. Se agregan tres capacidades nuevas:

1. **Auditorías** — registro de toda acción relevante (CRUD de negocio,
   autenticación, administración de cuentas), visible solo para
   `ADMINISTRADOR`.
2. **Reportes PDF** — descarga de reportes en PDF, con contenido distinto
   según rol (`ADMINISTRADOR`, `RECEPCIONISTA`, `ENTRENADOR`).
3. **Configuración** — apariencia (tema claro/oscuro, fuente), información
   de la app, política de privacidad y exportación de "mis datos" en PDF,
   disponible para los 5 roles (`ADMINISTRADOR`, `RECEPCIONISTA`,
   `ENTRENADOR`, `REPRESENTANTE`, `ESTUDIANTE`).

Roles exactos usados en el sistema (backend `hasRole`/frontend
`currentUser().rol`): `ADMINISTRADOR`, `RECEPCIONISTA`, `ENTRENADOR`,
`REPRESENTANTE`, `ESTUDIANTE`.

---

## 1. Auditorías

### Modelo de datos

Nueva migración Flyway `V18__auditoria.sql`, tabla `seguridad.auditoria`:

| Columna         | Tipo                        | Notas                                             |
|-----------------|-----------------------------|----------------------------------------------------|
| `id`            | `BIGSERIAL PK`               |                                                     |
| `fecha`         | `TIMESTAMPTZ NOT NULL`       | Zona Ecuador, mismo patrón ya usado en el proyecto |
| `id_usuario`    | `BIGINT NULL`                | FK a `seguridad.usuarios`, `ON DELETE SET NULL`    |
| `usuario_nombre`| `VARCHAR(150) NOT NULL`      | Denormalizado (username), sobrevive borrado de cuenta |
| `rol`           | `VARCHAR(50) NULL`           | Rol del usuario al momento de la acción            |
| `accion`        | `VARCHAR(30) NOT NULL`       | `CREAR`, `EDITAR`, `ELIMINAR`, `LOGIN`, `LOGIN_FALLIDO`, `LOGOUT`, `CAMBIO_ROL`, `ACTIVAR`, `DESACTIVAR` |
| `entidad`       | `VARCHAR(100) NULL`          | Nombre simple de la clase afectada (`Lesion`, `Pago`, `Usuario`, ...) |
| `entidad_id`    | `BIGINT NULL`                | PK de la fila afectada, si aplica                  |
| `descripcion`   | `TEXT NOT NULL`              | Resumen legible ya formado (ej. "Juan Pérez editó Lesión #45 de Ana Torres") |
| `ip`            | `VARCHAR(45) NULL`           | IP de origen de la request                          |

Sin columnas de diff (valores antes/después) — solo resumen legible, según
lo acordado.

Índices: `(fecha DESC)`, `(id_usuario)`, `(entidad)`, `(accion)` para
soportar los filtros de la sección 1.3.

### Captura

- **Entidad `Auditoria`** + `AuditoriaRepository` (Spring Data JPA) en
  paquete `org.uteq.backend.seguridad.auditoria`.
- **`AuditoriaService.registrar(accion, entidad, entidadId, descripcion)`**
  — resuelve `usuario_nombre`/`rol`/`ip` desde el `SecurityContext` y la
  request actual (vía `HttpServletRequest` inyectado o
  `RequestContextHolder`). Nunca propaga excepciones: si falla el guardado,
  se loguea el error (logger existente) y la operación de negocio
  continúa sin interrupción.
- **Aspecto AOP (`@Aspect`, `@Around`)** sobre los métodos públicos
  `crear*`/`actualizar*`/`eliminar*` (o equivalentes) de los servicios de
  negocio ya existentes: `LesionService`, `PagoService`, `EstudianteService`,
  `EvaluacionService`, `PersonaService`, `InventarioService`, etc. El
  aspecto arma automáticamente `entidad` (nombre de la clase de retorno o
  del primer parámetro) y `entidad_id` (vía reflexión sobre el campo
  `id`/getter `getId()` del resultado), y compone `descripcion` con una
  plantilla genérica ("{usuario} {accionEnPasado} {entidad} #{id}"). Los
  servicios existentes no se modifican salvo, si hace falta, ajustar
  nombres de métodos para que calcen con el pointcut.
- **Eventos no-CRUD** (login, login fallido, logout, cambio de rol,
  activar/desactivar cuenta): llamada explícita a
  `AuditoriaService.registrar(...)` desde `AuthController` (líneas donde
  ya existe el log `AUTH_LOGIN_OK`/`AUTH_LOGIN_FAIL`) y desde
  `UsuarioController`. El logger de archivo `AUTH_AUDIT`
  (`logs/sged-auth.log`) se mantiene sin cambios como registro adicional
  de bajo nivel; la tabla `auditoria` es la fuente para la UI de admin.

### API

`GET /api/admin/auditorias` — `@PreAuthorize("hasRole('ADMINISTRADOR')")`

Query params (todos opcionales): `usuario` (contains, case-insensitive),
`accion` (exacto), `entidad` (exacto), `fechaDesde`, `fechaHasta`
(ISO date), `page`, `size` (Spring `Pageable`, mismo patrón que el resto
del proyecto — ordenado por `fecha DESC` por defecto).

Respuesta: página de DTOs `AuditoriaResponse { fecha, usuario, rol,
accion, entidad, entidadId, descripcion }`.

### Frontend

Página `/admin/auditorias` (`roleGuard(['ADMINISTRADOR'])`), componente
standalone en `frontend/src/app/features/auditorias/`. Tabla con
paginación, filtros (usuario, tipo de acción, entidad, rango de fechas)
como formulario reactivo simple encima de la tabla, siguiendo el estilo
visual ya usado en páginas de listado existentes (ej. `personas`,
`inventario`).

---

## 2. Reportes PDF

### Backend

Nueva dependencia en `backend/pom.xml`: **OpenPDF** (`com.github.librepdf:openpdf`).

**`ReportePdfService`** (paquete `org.uteq.backend.reportes`): utilidad
compartida para maquetar PDFs (encabezado con nombre del sistema y logo
opcional, tabla de datos, pie con fecha de generación + usuario que lo
generó). Cada reporte concreto arma sus filas reutilizando los
repositorios/servicios existentes (no se duplica lógica de consulta).

**`ReporteController`**, endpoints devuelven `application/pdf`
(`ResponseEntity<byte[]>` con `Content-Disposition: attachment`):

| Endpoint | Roles | Filtros (query params) |
|---|---|---|
| `GET /api/reportes/estudiantes-fichas` | `ADMINISTRADOR`, `RECEPCIONISTA` | `categoria`, `activo` |
| `GET /api/reportes/pagos` | `ADMINISTRADOR`, `RECEPCIONISTA` | `estudianteId`, `fechaDesde`, `fechaHasta`, `estado` |
| `GET /api/reportes/asistencias` | `ADMINISTRADOR`, `ENTRENADOR` | `estudianteId`, `categoria`, `fechaDesde`, `fechaHasta` |
| `GET /api/reportes/evaluaciones` | `ADMINISTRADOR`, `ENTRENADOR` | `estudianteId`, `categoria`, `fechaDesde`, `fechaHasta` |
| `GET /api/reportes/lesiones` | `ADMINISTRADOR`, `ENTRENADOR` | `estudianteId`, `categoria`, `fechaDesde`, `fechaHasta` |

Si el filtro no devuelve resultados, el endpoint responde `404` con JSON
de error estándar (no genera un PDF vacío) — el interceptor HTTP del
frontend ya maneja errores de API de forma consistente.

### Frontend

Página `/reportes` (`roleGuard(['ADMINISTRADOR','RECEPCIONISTA','ENTRENADOR'])`),
componente standalone en `frontend/src/app/features/reportes/`. Muestra
tarjetas según `currentUser().rol`:

- `RECEPCIONISTA` → Fichas de estudiantes, Pagos
- `ENTRENADOR` → Fichas de estudiantes, Asistencias, Evaluaciones, Lesiones
- `ADMINISTRADOR` → las cinco

Cada tarjeta tiene un formulario de filtros simple y un botón "Generar
PDF" que llama al endpoint correspondiente con `responseType: 'blob'` y
dispara la descarga en el navegador.

---

## 3. Configuración

Página `/configuracion`, accesible a cualquier usuario autenticado (sin
`roleGuard`), componente standalone en
`frontend/src/app/features/configuracion/`, con 4 secciones/pestañas:

### 3.1 Apariencia

- **`ThemeService`** (nuevo, Angular, `providedIn: 'root'`): expone
  signal `tema: 'claro' | 'oscuro'`, método `alternarTema()`. Al cambiar,
  agrega/quita `data-theme="oscuro"` en `document.documentElement` y
  persiste en `localStorage` (`sged-tema`), mismo patrón que el colapso
  de sidebar ya existente en `app-shell.component.ts`. Se inicializa leyendo
  `localStorage` al arrancar la app (`APP_INITIALIZER` o constructor del
  servicio, cargado antes del primer render para evitar parpadeo).
- **Paleta oscura**: bloque `:root[data-theme="oscuro"] { ... }` en
  `frontend/src/styles.css` que redefine los mismos tokens `--color-*`
  ya usados en todo el proyecto (`--color-surface`, `--color-border`,
  etc.) — los componentes existentes no requieren cambios porque ya
  consumen esas variables.
- **Fuente**: selector de familia (3 opciones: sans actual por defecto,
  serif, monoespaciada) y tamaño (normal/grande/extra grande), aplicados
  sobreescribiendo `--font-sans`/`--font-size-base` en `:root` vía
  estilo inline en `<html>` o clases `font-*`/`size-*`, persistidos en
  `localStorage` (`sged-fuente`, `sged-tamano-fuente`) por el mismo
  `ThemeService` (ampliado a preferencias de apariencia en general).

### 3.2 Acerca de

Contenido estático (sin backend): nombre del sistema, versión (leída de
`package.json`/constante de build), descripción breve, stack tecnológico,
contacto/soporte.

### 3.3 Política de privacidad

Contenido estático (sin backend), texto redactado adaptado a los datos
que maneja SGED (estudiantes, representantes, lesiones, pagos), en el
marco de la LOPDP (Ecuador): qué datos se recopilan, con qué fin, cómo se
protegen, derechos del titular, contacto para ejercerlos.

### 3.4 Mis datos (exportar PDF)

Botón "Descargar mis datos en PDF" → `GET /api/usuarios/me/datos-pdf`
(cualquier usuario autenticado, sin restricción de rol), reutiliza
`ReportePdfService`. Contenido: `username`, `rol`, datos de la `Persona`
vinculada (nombre, cédula, email, teléfono, fecha de nacimiento), fecha
de creación de la cuenta.

---

## 4. Navegación y rutas

`frontend/src/app/app.routes.ts`:

```
/admin/auditorias   roleGuard(['ADMINISTRADOR'])
/reportes            roleGuard(['ADMINISTRADOR','RECEPCIONISTA','ENTRENADOR'])
/configuracion       (sin roleGuard, requiere solo estar autenticado)
```

`app-shell.component.ts` (`NAV_POR_ROL`):

- "Auditorías" → agregado solo al array de `ADMINISTRADOR`.
- "Reportes" → agregado a `ADMINISTRADOR`, `RECEPCIONISTA`, `ENTRENADOR`.
- "Configuración" → ítem fijo renderizado fuera del `switch` por rol
  (visible para los 5 roles), con ícono de engranaje al final del menú.

---

## 5. Manejo de errores

- El aspecto de auditoría nunca bloquea la operación de negocio: cualquier
  fallo al registrar se captura y loguea, sin propagar excepción.
- Los endpoints de reporte sin resultados devuelven `404` JSON estándar
  (no un PDF vacío/corrupto); el interceptor HTTP existente en el
  frontend ya muestra el mensaje de error al usuario.
- Los endpoints nuevos siguen el mismo formato de error ya usado en el
  resto de la API (`GlobalExceptionHandler` existente, sin cambios).

---

## 6. Plan de pruebas

**Backend**:
- Aspecto de auditoría: test de integración que ejecuta un create/update/
  delete sobre un servicio existente (ej. `LesionService`) y verifica que
  se crea la fila esperada en `seguridad.auditoria`.
- `AuditoriaController`: tests de filtros (por usuario, acción, entidad,
  rango de fechas) y de paginación.
- `ReportePdfService`/`ReporteController`: tests que verifican
  content-type `application/pdf`, contenido no vacío, y `403` para roles
  no autorizados en cada endpoint.
- `@PreAuthorize` de los endpoints nuevos: tests de seguridad por rol
  (positivo y negativo), igual patrón que los tests ya existentes de
  `PagoController`/`LesionController`.

**Frontend**:
- `ThemeService`: toggle de tema persiste en `localStorage` y aplica el
  atributo `data-theme` correcto; cambio de fuente persiste y aplica
  variables CSS.
- `roleGuard` en las rutas nuevas (`/admin/auditorias`, `/reportes`)
  redirige correctamente a roles no autorizados.
- Descarga de PDF: test que verifica que el servicio de reportes arma la
  request con `responseType: 'blob'` y dispara la descarga.

---

## Fuera de alcance (explícito)

- Diff de valores antes/después en auditoría (solo resumen legible).
- Persistencia de preferencias de apariencia en base de datos (solo
  `localStorage` en esta versión).
- Filtros avanzados adicionales a los listados en la sección 2 (se puede
  ampliar en una iteración futura).
- Estructural directive de Angular para ocultar fragmentos de plantilla
  por rol (no existe hoy, no se agrega salvo que resulte estrictamente
  necesario para las secciones nuevas).
