# Reparto del sistema para el repaso — 3 integrantes

Base: distribución acordada (imagen del equipo). Cada integrante repasa **su bloque de
frontend + el backend que lo alimenta**, para poder explicar el flujo completo de punta a punta.

Referencia de tamaño (LOC aprox., sin tests):

| Integrante | Frontend | Backend | Peso relativo |
|---|---|---|---|
| Darwin | ~5 900 LOC | ~5 400 LOC | Más pantallas (personas + dashboard) |
| Alejandro | ~3 550 LOC | ~6 100 LOC | Módulo más grande (deportivo completo + IA) |
| Ricardo | ~2 320 LOC | ~3 000 LOC | Menos LOC pero transversal y de mayor riesgo (seguridad) |

---

## 1) Darwin — Académico + Inventario + Reportes

### Interfaces (frontend)
| Feature | LOC | Pantallas clave |
|---|---|---|
| `features/personas` | 2091 | ficha estudiante, `personas-lista`, `personas-gestion`, `persona-detalle`, fichas (estudiante/representante/entrenador), `cuenta-usuario` |
| `features/dashboard` | 1106 | `dashboard`, `graficos`, `mapa-asistencia` |
| `features/inventario` | 685 | `inventario` (artículos, asignaciones, movimientos de stock) |
| `features/pagos` | 625 | `pagos` |
| `features/estudiante` | 583 | `marcar-asistencia`, `mi-historial`, `mi-equipo` |
| `features/representante` | 371 | `representante` (informes, notificaciones) |
| `features/reportes` | 291 | `reportes` |
| `features/recepcion` | 167 | `recepcion` (alta rápida de personas) |

### Backend
- `academico/estudiante` — `EstudianteController`, `MiEquipoController`; `EstudianteService`, `MiEquipoService`, `EstudianteAccesoService`
- `academico/pago` — `PagoController`, `PagoService`
- `academico/alerta` — `AlertaController`, `AlertaService`
- `academico/representante` (parte académica) — `RepresentanteController`, `InformeRepresentanteController`; `RepresentanteService`, `InformeService`, `NotificacionService`
- `inventario/item` — `ItemController`, `ItemService`
- `inventario/assignment` — `AssignmentController`, `AssignmentService`
- `inventario/movement` — `StockMovementController`, `StockMovementService`
- `reportes` — `ReportController`; `ReportService`, `ReportPdfService`
- `seguridad/persona` — `PersonaController`, `PersonaService` *(dato base de las fichas; está en el paquete `seguridad`, coordinar con Ricardo lo que sea de permisos)*

### Puntos de coordinación
- `features/estudiante` (marcar asistencia / mi equipo / mi historial) consume backend **deportivo** de Alejandro (`deportivo/asistencia`, `MiEquipoController`). Repasar juntos ese flujo.
- `features/consentimientos` lo repasa Ricardo, pero su backend (`ConsentimientoController`) vive junto a `academico/representante`. Alinear con Ricardo.

---

## 2) Alejandro — Deportivo completo (+ IA)

### Interfaces (frontend)
| Feature | LOC | Pantallas clave |
|---|---|---|
| `features/entrenador` | 1888 | **`evaluacion-diaria`** (la más grande), `sesiones`, `historial-sesion`, `lista-asistencia` |
| `features/partidos` | 1348 | `partidos`, `alineacion`, `cancha` |
| `features/categorias` | 318 | `categorias` |

### Backend (todo `deportivo/`)
- `deportivo/sesion` — `SesionEntrenamientoController`, `SesionEntrenamientoService`
- `deportivo/evaluacion` — `EvaluacionDiariaController`, `EvaluacionDiariaService`
- `deportivo/asistencia` — `AsistenciaQrController`, `AsistenciaSesionController`, `MiAsistenciaController`, `ResumenAsistenciaController`; `AsistenciaService`, `QrAsistenciaService`
- `deportivo/entrenador` — `EntrenadorController`, `EntrenadorService`
- `deportivo/partido` — `PartidoController`; `PartidoService`, `AlineacionService`, `ConvocatoriaService`
- `deportivo/categoria` — `CategoriaController`, `CategoriaService`
- `deportivo/lesion` — `LesionController`, `LesionService`
- `deportivo/horario` — `HorarioController`, `HorarioService`
- `deportivo/especialidad` — `EspecialidadController`, `EspecialidadService`
- `deportivo/posicion` — `PosicionController`
- `common/ia` — `AIFeedbackGenerator`, `GeminiFeedbackService`, `OpenAiFeedbackService`, `PromptsFeedback`, `AnonymousPlayerProfile` *(feedback IA de la evaluación diaria)*

### Puntos de coordinación
- El QR de asistencia se marca desde `features/estudiante` (Darwin). Revisar el par backend/frontend con Darwin.
- Categorías y horarios también los consume el alta de estudiantes (Darwin).

---

## 3) Ricardo — Seguridad + Accesos + Transversal

### Interfaces (frontend)
| Feature | LOC | Pantallas clave |
|---|---|---|
| `core` | 594 | componentes compartidos (buscador, confirmar-acción, mensajes de error, formato fecha/texto, `theme.service`, descarga de archivos) |
| `auth` | 592 | `login`, `auth.guard`, `role.guard`, `auth.interceptor`, `auth.service` |
| `features/consentimientos` | 402 | `consentimientos` |
| `shell` | 338 | `app-shell` (navegación, layout, menús por rol) |
| `features/auditorias` | 204 | `auditorias` |
| `features/configuracion` | 194 | `configuracion` |

### Backend
- `seguridad/auth` — `AuthController`; `AuthService`, `JwtService`, `LoginAttemptService`, `RedisBlacklistService`
- `seguridad/usuario` — `UsuarioController`, `PerfilController`, `UsuarioService`
- `seguridad/role` — roles y autorización
- `seguridad/status` — `GeneralStatusController`, `GeneralStatusService`
- `seguridad/audit` — `AuditController`, `AuditService` + AOP de auditoría
- `academico/representante` (parte legal) — `ConsentimientoController`, `ConsentimientoService`, entidad `Consentimiento`
- `common/exception` — `GlobalExceptionHandler`, `ApiException`, `ProblemDetailsAuthHandlers`, `ResourceNotFoundException`, `TooManyRequestsException`
- `config` — `SecurityConfig` (CORS, filtros, cadena de seguridad), `RedisCacheConfig`
- `common/Zones.java`

### Puntos de coordinación
- `role.guard` / `SecurityConfig` definen qué rol ve cada pantalla de Darwin y Alejandro: repasar la matriz de roles con ambos.
- `common/exception` afecta el manejo de errores de todos los controllers.
- `core` y `shell` son consumidos por todas las features.

---

## Cómo hacer el repaso (sugerencia)

1. Cada uno recorre sus pantallas en la app desplegada y sigue el flujo hasta el `Controller` → `Service` → `Repository`.
2. Anota: qué hace, qué endpoints usa, qué validaciones/roles aplican, dudas.
3. Sesión conjunta de 30 min para los **puntos de coordinación** (asistencia estudiante↔deportivo, roles↔SecurityConfig, consentimientos↔representante).
