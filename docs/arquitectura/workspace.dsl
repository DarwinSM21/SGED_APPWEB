// Fuente unica del modelo C4 (niveles 1-3) del sistema SGED.
// Los PNG de este mismo directorio (L1-contexto, L2-contenedores,
// L3-componentes) se generan desde aqui con `make diagrams`; no se editan
// a mano. Los .puml sueltos que antes vivian en docs/diagramas/ se
// eliminaron por duplicar este modelo con un contenido desactualizado.
// docs/diagramas/ conserva solo el MER, que no se modela aqui.
workspace "SGED - ProFútbol" "Sistema de Gestión para la Escuela Deportiva ProFútbol. Modelo C4 (niveles 1-3) en Structurizr DSL." {

    !identifiers hierarchical

    model {

        # ------------------------------------------------------------
        # Actores (Nivel 1)
        # ------------------------------------------------------------
        estudiante = person "Estudiante" "Jugador de la escuela; consulta su perfil, evaluaciones y estado de membresía." "Usuario"
        entrenador = person "Entrenador" "Registra asistencia, evaluaciones diarias y plantillas de partido." "Usuario"
        recepcionista = person "Recepcionista" "Gestiona estudiantes, pagos, membresías e inventario." "Usuario"
        administrador = person "Administrador" "Supervisa el sistema, gestiona usuarios, permisos y reportes." "Usuario"

        # ------------------------------------------------------------
        # Sistemas externos (Nivel 1)
        # NOTA: SMTP y RFID están en el modelo de datos / roadmap pero
        # aún no tienen integración en el código (backend/src). Se marcan
        # como "planned" para no sobre-representar el estado actual.
        # ------------------------------------------------------------
        smtp = softwareSystem "Servidor SMTP" "Envía correos de notificación (Gmail/Mailgun). Planeado, aún no integrado en backend/src." "Externo,Planeado"
        lectorRfid = softwareSystem "Lector RFID" "Hardware que reportaría presencia al endpoint de asistencias. Columna rfid_codigo ya existe en BD; endpoint pendiente." "Externo,Planeado"

        sged = softwareSystem "SGED (ProFútbol)" "Aplicación web para gestión administrativa y deportiva: estudiantes, entrenadores, asistencias, evaluaciones y reportes." {

            # --------------------------------------------------------
            # Contenedores (Nivel 2) - reflejan docker-compose.yml
            # --------------------------------------------------------
            spa = container "SPA Angular (servida por nginx)" "Interfaz de usuario: login, registro, guard/interceptor JWT, dashboard. El mismo contenedor sirve los estáticos y termina TLS en :8443 con certificado autofirmado (OWASP A02); :4200 queda en HTTP para desarrollo." "Angular 21, TypeScript, Signals, nginx" "WebApp"

            api = container "API Spring Boot" "Expone la API REST, aplica reglas de negocio y seguridad (JWT en cookie HttpOnly, RBAC)." "Spring Boot 3.2.5, Java 21" "API" {

                # ----------------------------------------------------
                # Componentes (Nivel 3) - reflejan backend/src/main/java
                # tras la reestructuracion en tres dominios:
                # academico / deportivo / seguridad.
                # ----------------------------------------------------

                # --- Dominio seguridad ---
                authController = component "AuthController" "Endpoints /api/auth: login, registro, logout, refresh, me." "Spring MVC REST Controller"
                usuarioController = component "UsuarioController" "CRUD de cuentas de usuario (/api/usuarios), con paginación y baja lógica." "Spring MVC REST Controller"
                personaController = component "PersonaController" "CRUD de datos personales (/api/personas); unicidad de cédula y correo." "Spring MVC REST Controller"
                estadoGeneralController = component "EstadoGeneralController" "Catálogo de estados administrativos (/api/estados_generales), de solo lectura." "Spring MVC REST Controller"

                jwtAuthFilter = component "JwtAuthenticationFilter" "Filtro que valida el JWT de la cookie en cada petición y puebla el contexto de seguridad." "Spring Security Filter"
                jwtService = component "JwtService" "Emisión y validación de tokens JWT (jjwt 0.12), con claims iss/aud/nbf." "Servicio"
                loginAttemptService = component "LoginAttemptService" "Control de intentos fallidos de login (mitigación de fuerza bruta)." "Servicio"
                redisBlacklistService = component "RedisBlacklistService" "Lista negra de tokens invalidados (logout) respaldada en Redis." "Servicio"
                userDetailsService = component "UserDetailsServiceImpl" "Carga de usuario/roles para Spring Security." "Servicio"
                securityConfig = component "SecurityConfig" "Cadena de filtros, CORS, BCrypt(12), política stateless, CSP explícito." "Configuración Spring Security"

                usuarioService = component "UsuarioService" "Reglas de negocio de cuentas: alta con contraseña codificada, unicidad de username, baja lógica." "Servicio"
                personaService = component "PersonaService" "Reglas de negocio de personas: unicidad de cédula/correo al crear y editar." "Servicio"
                estadoGeneralService = component "EstadoGeneralService" "Consulta del catálogo de estados." "Servicio"

                # --- Dominio academico ---
                estudianteController = component "EstudianteController" "CRUD de estudiantes (/api/estudiantes) con paginación, baja lógica y operaciones por categoría." "Spring MVC REST Controller"
                estudianteService = component "EstudianteService" "Reglas de negocio de estudiantes: alta, edición, baja lógica, conteo y desactivación por categoría." "Servicio"

                # --- Dominio deportivo ---
                categoriaController = component "CategoriaController" "CRUD del catálogo de categorías (/api/categorias), con validación de rango de edad." "Spring MVC REST Controller"
                entrenadorController = component "EntrenadorController" "CRUD de entrenadores (/api/entrenadores), vinculados a persona y cuenta de usuario." "Spring MVC REST Controller"
                categoriaService = component "CategoriaService" "Reglas de negocio del catálogo de categorías." "Servicio"
                entrenadorService = component "EntrenadorService" "Reglas de negocio de entrenadores: unicidad de persona y usuario asociados." "Servicio"

                # --- Persistencia ---
                seguridadRepository = component "UsuarioRepository / PersonaRepository / RolRepository / EstadoGeneralRepository" "Acceso a datos del dominio seguridad vía Spring Data JPA." "Repositorio JPA"
                estudianteRepository = component "EstudianteRepository" "Acceso a datos de estudiantes vía Spring Data JPA; invoca los procedimientos almacenados con @Procedure." "Repositorio JPA"
                deportivoRepository = component "CategoriaRepository / EntrenadorRepository" "Acceso a datos del dominio deportivo vía Spring Data JPA." "Repositorio JPA"

                # --- Transversal ---
                globalExceptionHandler = component "GlobalExceptionHandler / ProblemDetailsAuthHandlers" "Traduce excepciones a respuestas RFC 7807 (Problem Details); @Valid responde 422." "Componente transversal"
                redisCacheConfig = component "RedisCacheConfig" "Serialización JSON de la caché de entidades (soporte de java.time.Instant y del @class raíz)." "Configuración"

                # Relaciones internas del contenedor API
                authController -> jwtService "Emite/valida tokens"
                authController -> loginAttemptService "Registra intentos fallidos"
                authController -> redisBlacklistService "Invalida token en logout"
                authController -> seguridadRepository "Lee/crea usuario y persona"
                userDetailsService -> seguridadRepository "Carga usuario + roles"
                jwtAuthFilter -> userDetailsService "Resuelve el usuario autenticado"
                jwtAuthFilter -> redisBlacklistService "Verifica si el token está invalidado"
                jwtAuthFilter -> jwtService "Valida firma y claims del token"
                securityConfig -> jwtAuthFilter "Registra el filtro en la cadena"

                estudianteController -> estudianteService "Delega reglas de negocio"
                estudianteService -> estudianteRepository "CRUD, paginación, baja lógica y llamada a procedimientos"

                usuarioController -> usuarioService "Delega reglas de negocio"
                usuarioService -> seguridadRepository "CRUD de cuentas"
                personaController -> personaService "Delega reglas de negocio"
                personaService -> seguridadRepository "CRUD de personas"
                estadoGeneralController -> estadoGeneralService "Delega consulta"
                estadoGeneralService -> seguridadRepository "Lee el catálogo"

                categoriaController -> categoriaService "Delega reglas de negocio"
                categoriaService -> deportivoRepository "CRUD de categorías"
                entrenadorController -> entrenadorService "Delega reglas de negocio"
                entrenadorService -> deportivoRepository "CRUD de entrenadores"
                entrenadorService -> seguridadRepository "Verifica persona y usuario asociados"
                estudianteService -> deportivoRepository "Resuelve la categoría por clave foránea"

                authController -> globalExceptionHandler "Errores gestionados"
                estudianteController -> globalExceptionHandler "Errores gestionados"
                usuarioController -> globalExceptionHandler "Errores gestionados"
                personaController -> globalExceptionHandler "Errores gestionados"
                categoriaController -> globalExceptionHandler "Errores gestionados"
                entrenadorController -> globalExceptionHandler "Errores gestionados"
                estadoGeneralController -> globalExceptionHandler "Errores gestionados"

                redisCacheConfig -> estudianteService "Configura la caché de la entidad"
            }

            postgres = container "PostgreSQL 16" "Esquemas 'seguridad', 'academico' y 'deportivo'. Incluye los procedimientos almacenados academico.sp_contar_estudiantes_activos y academico.sp_desactivar_estudiantes_categoria, invocados vía @Procedure." "PostgreSQL 16" "Database"
            redis = container "Redis 7" "Cache de sesión y lista negra de tokens JWT invalidados." "Redis 7" "Database"
        }

        # ------------------------------------------------------------
        # Relaciones Nivel 1 y 2
        # ------------------------------------------------------------
        estudiante -> sged.spa "Consulta perfil y evaluaciones" "HTTPS"
        entrenador -> sged.spa "Registra asistencia y evaluaciones" "HTTPS"
        recepcionista -> sged.spa "Gestiona estudiantes, pagos, membresías" "HTTPS"
        administrador -> sged.spa "Gestiona usuarios y reportes" "HTTPS"

        sged.spa -> sged.api "Consume API REST" "HTTP/JSON, cookie JWT HttpOnly"
        sged.api -> sged.postgres "Lee/escribe" "JDBC, Spring Data JPA"
        sged.api -> sged.redis "Lee/escribe" "Lettuce/Redis protocol"

        sged -> smtp "Enviaría correos de notificación (roadmap)" "SMTP/TLS"
        lectorRfid -> sged "Enviaría señal de presencia (roadmap)" "HTTP POST"
    }

    views {

        systemContext sged "C4_Nivel1_Contexto" {
            include *
            autoLayout lr
            title "Nivel 1 — Contexto del sistema SGED"
            description "Actores, el sistema SGED y los sistemas externos (uno de ellos, planeado)."
        }

        container sged "C4_Nivel2_Contenedores" {
            include *
            autoLayout lr
            title "Nivel 2 — Contenedores de SGED"
            description "SPA Angular, API Spring Boot, PostgreSQL y Redis, según docker-compose.yml (nginx termina TLS en :8443)."
        }

        component sged.api "C4_Nivel3_Componentes_API" {
            include *
            autoLayout lr
            title "Nivel 3 — Componentes del contenedor API Spring Boot"
            description "Controladores, servicios, repositorios y filtros de seguridad de los tres dominios: academico, deportivo y seguridad."
        }

        styles {
            element "Usuario" {
                background #08427b
                color #ffffff
                shape person
            }
            element "Externo" {
                background #999999
                color #ffffff
            }
            element "Planeado" {
                background #cccccc
                color #333333
                border dashed
            }
            element "WebApp" {
                background #1168bd
                color #ffffff
                shape webBrowser
            }
            element "API" {
                background #1168bd
                color #ffffff
            }
            element "Database" {
                background #438dd5
                color #ffffff
                shape cylinder
            }
            element "Spring MVC REST Controller" {
                background #a5cff5
                color #000000
                shape roundedBox
            }
            element "Servicio" {
                background #85bbf0
                color #000000
            }
            element "Repositorio JPA" {
                background #6ba4e0
                color #000000
                shape cylinder
            }
            element "Spring Security Filter" {
                background #b8a5f5
                color #000000
            }
            element "Configuración Spring Security" {
                background #b8a5f5
                color #000000
                shape folder
            }
            element "Configuración" {
                background #d4c9f7
                color #000000
                shape folder
            }
            element "Componente transversal" {
                background #f5d5a5
                color #000000
            }
        }
    }
}
