workspace "SGED - ProFútbol" "Sistema de Gestión para la Escuela Deportiva ProFútbol. Modelo C4 (niveles 1-3) en Structurizr DSL. Sustituye a los .puml sueltos de docs/diagramas/." {

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
            spa = container "SPA Angular" "Interfaz de usuario: login, registro, guard/interceptor JWT, dashboard." "Angular 21, TypeScript, Signals" "WebApp"

            api = container "API Spring Boot" "Expone la API REST, aplica reglas de negocio y seguridad (JWT en cookie HttpOnly, RBAC)." "Spring Boot 3.2.5, Java 21" "API" {

                # ----------------------------------------------------
                # Componentes (Nivel 3) - reflejan backend/src/main/java
                # ----------------------------------------------------
                authController = component "AuthController" "Endpoints /api/auth: login, registro, logout, me." "Spring MVC REST Controller"
                estudianteController = component "EstudianteController" "CRUD de estudiantes con paginación, baja lógica y conteo por categoría." "Spring MVC REST Controller"

                jwtAuthFilter = component "JwtAuthenticationFilter" "Filtro que valida el JWT de la cookie en cada petición y puebla el contexto de seguridad." "Spring Security Filter"
                jwtService = component "JwtService" "Emisión y validación de tokens JWT (jjwt 0.12)." "Servicio"
                loginAttemptService = component "LoginAttemptService" "Control de intentos fallidos de login (mitigación de fuerza bruta)." "Servicio"
                redisBlacklistService = component "RedisBlacklistService" "Lista negra de tokens invalidados (logout) respaldada en Redis." "Servicio"
                userDetailsService = component "UserDetailsServiceImpl" "Carga de usuario/roles para Spring Security." "Servicio"
                securityConfig = component "SecurityConfig" "Cadena de filtros, CORS, BCrypt(12), política stateless, CSP." "Configuración Spring Security"

                estudianteService = component "EstudianteService" "Reglas de negocio de estudiantes: alta, edición, baja lógica, conteo por categoría." "Servicio"

                usuarioRepository = component "UsuarioRepository / PersonaRepository / RolRepository" "Acceso a datos de seguridad vía Spring Data JPA." "Repositorio JPA"
                estudianteRepository = component "EstudianteRepository" "Acceso a datos de estudiantes vía Spring Data JPA (incluye llamadas a funciones PL/pgSQL)." "Repositorio JPA"

                globalExceptionHandler = component "GlobalExceptionHandler / ProblemDetailsAuthHandlers" "Traduce excepciones a respuestas RFC 7807 (Problem Details)." "Componente transversal"

                # Relaciones internas del contenedor API
                authController -> jwtAuthFilter "Delegación de validación de sesión (vía cadena de filtros)"
                authController -> jwtService "Emite/valida tokens"
                authController -> loginAttemptService "Registra intentos fallidos"
                authController -> redisBlacklistService "Invalida token en logout"
                authController -> usuarioRepository "Lee/crea usuario y persona"
                userDetailsService -> usuarioRepository "Carga usuario + roles"
                jwtAuthFilter -> userDetailsService "Resuelve el usuario autenticado"
                jwtAuthFilter -> redisBlacklistService "Verifica si el token está invalidado"

                estudianteController -> estudianteService "Delega reglas de negocio"
                estudianteService -> estudianteRepository "CRUD, paginación, conteo, baja lógica"

                authController -> globalExceptionHandler "Errores gestionados"
                estudianteController -> globalExceptionHandler "Errores gestionados"
            }

            postgres = container "PostgreSQL 16" "Esquemas 'seguridad' y 'deportivo'. Incluye funciones PL/pgSQL fn_contar_estudiantes_activos y fn_desactivar_estudiantes_categoria." "PostgreSQL 16" "Database"
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
            description "SPA Angular, API Spring Boot, PostgreSQL y Redis, según docker-compose.yml."
        }

        component sged.api "C4_Nivel3_Componentes_API" {
            include *
            autoLayout lr
            title "Nivel 3 — Componentes del contenedor API Spring Boot"
            description "Controladores, servicios, filtros de seguridad y repositorios de los módulos auth y estudiante."
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
            element "Servicio" {
                background #85bbf0
                color #000000
            }
            element "Repositorio JPA" {
                background #6ba4e0
                color #000000
                shape cylinder
            }
        }
    }
}
