// Single source of truth for the SGED system's C4 model (levels 1-3).
// The PNG files in this same directory (L1-contexto, L2-contenedores,
// L3-componentes) are generated from here with `make diagrams`; they are
// not edited by hand. The loose .puml files that used to live in
// docs/diagramas/ were removed for duplicating this model with outdated
// content. docs/diagramas/ only keeps the ER diagram, which is not
// modeled here.
workspace "SGED - ProFútbol" "Management System for the ProFútbol Youth Football Academy. C4 model (levels 1-3) in Structurizr DSL." {

    !identifiers hierarchical

    model {

        # ------------------------------------------------------------
        # Actors (Level 1)
        # ------------------------------------------------------------
        estudiante = person "Student" "Academy player; checks their profile, evaluations and membership status." "User"
        entrenador = person "Coach" "Registers attendance, daily evaluations and match lineups." "User"
        recepcionista = person "Receptionist" "Manages students, payments, memberships and inventory." "User"
        administrador = person "Administrator" "Oversees the system, manages users, permissions and reports." "User"

        # ------------------------------------------------------------
        # External systems (Level 1)
        # NOTE: SMTP and RFID are in the data model / roadmap but are
        # not yet integrated in the code (backend/src). They are marked
        # "planned" so as not to overrepresent the current state.
        # ------------------------------------------------------------
        smtp = softwareSystem "SMTP Server" "Sends notification emails (Gmail/Mailgun). Planned, not yet integrated in backend/src." "External,Planned"
        lectorRfid = softwareSystem "RFID Reader" "Hardware that would report attendance to the attendance endpoint. Column rfid_codigo already exists in the DB; endpoint pending." "External,Planned"

        sged = softwareSystem "SGED (ProFútbol)" "Web application for administrative and sports management: students, coaches, attendance, evaluations and reports." {

            # --------------------------------------------------------
            # Containers (Level 2) - reflect docker-compose.yml
            # --------------------------------------------------------
            spa = container "Angular SPA (served by nginx)" "User interface: login, registration, JWT guard/interceptor, dashboard. The same container serves the static assets and terminates TLS on :8443 with a self-signed certificate (OWASP A02); :4200 remains HTTP for development." "Angular 21, TypeScript, Signals, nginx" "WebApp"

            api = container "Spring Boot API" "Exposes the REST API, applies business rules and security (JWT in HttpOnly cookie, RBAC)." "Spring Boot 3.2.5, Java 21" "API" {

                # ----------------------------------------------------
                # Components (Level 3) - reflect backend/src/main/java
                # after the restructuring into three domains:
                # academico / deportivo / seguridad.
                # ----------------------------------------------------

                # --- Security domain ---
                authController = component "AuthController" "Endpoints /api/auth: login, register, logout, refresh, me." "Spring MVC REST Controller"
                usuarioController = component "UsuarioController" "CRUD for user accounts (/api/usuarios), with pagination and soft delete." "Spring MVC REST Controller"
                personaController = component "PersonaController" "CRUD for personal data (/api/personas); uniqueness of ID number and email." "Spring MVC REST Controller"
                estadoGeneralController = component "EstadoGeneralController" "Read-only catalog of administrative statuses (/api/estados_generales)." "Spring MVC REST Controller"

                jwtAuthFilter = component "JwtAuthenticationFilter" "Filter that validates the JWT from the cookie on every request and populates the security context." "Spring Security Filter"
                jwtService = component "JwtService" "Issuance and validation of JWT tokens (jjwt 0.12), with iss/aud/nbf claims." "Service"
                loginAttemptService = component "LoginAttemptService" "Tracks failed login attempts (brute-force mitigation)." "Service"
                redisBlacklistService = component "RedisBlacklistService" "Blacklist of invalidated tokens (logout) backed by Redis." "Service"
                userDetailsService = component "UserDetailsServiceImpl" "Loads user/roles for Spring Security." "Service"
                securityConfig = component "SecurityConfig" "Filter chain, CORS, BCrypt(12), stateless policy, explicit CSP." "Spring Security Configuration"

                usuarioService = component "UsuarioService" "Account business rules: creation with encoded password, username uniqueness, soft delete." "Service"
                personaService = component "PersonaService" "Person business rules: ID number/email uniqueness on create and edit." "Service"
                estadoGeneralService = component "EstadoGeneralService" "Status catalog lookup." "Service"

                # --- Academic domain ---
                estudianteController = component "EstudianteController" "CRUD for students (/api/estudiantes) with pagination, soft delete and per-category operations." "Spring MVC REST Controller"
                estudianteService = component "EstudianteService" "Student business rules: create, edit, soft delete, count and deactivate by category." "Service"

                # --- Sports domain ---
                categoriaController = component "CategoriaController" "CRUD for the category catalog (/api/categorias), with age-range validation." "Spring MVC REST Controller"
                entrenadorController = component "EntrenadorController" "CRUD for coaches (/api/entrenadores), linked to a person and a user account." "Spring MVC REST Controller"
                categoriaService = component "CategoriaService" "Category catalog business rules." "Service"
                entrenadorService = component "EntrenadorService" "Coach business rules: uniqueness of the linked person and user account." "Service"

                # --- Persistence ---
                seguridadRepository = component "UsuarioRepository / PersonaRepository / RolRepository / EstadoGeneralRepository" "Data access for the security domain via Spring Data JPA." "JPA Repository"
                estudianteRepository = component "EstudianteRepository" "Data access for students via Spring Data JPA; invokes stored procedures with @Procedure." "JPA Repository"
                deportivoRepository = component "CategoriaRepository / EntrenadorRepository" "Data access for the sports domain via Spring Data JPA." "JPA Repository"

                # --- Cross-cutting ---
                globalExceptionHandler = component "GlobalExceptionHandler / ProblemDetailsAuthHandlers" "Translates exceptions into RFC 7807 (Problem Details) responses; @Valid returns 422." "Cross-cutting Component"
                redisCacheConfig = component "RedisCacheConfig" "JSON serialization for the entity cache (support for java.time.Instant and the root @class)." "Configuration"

                # Internal relationships of the API container
                authController -> jwtService "Issues/validates tokens"
                authController -> loginAttemptService "Records failed attempts"
                authController -> redisBlacklistService "Invalidates token on logout"
                authController -> seguridadRepository "Reads/creates user and person"
                userDetailsService -> seguridadRepository "Loads user + roles"
                jwtAuthFilter -> userDetailsService "Resolves the authenticated user"
                jwtAuthFilter -> redisBlacklistService "Checks whether the token is invalidated"
                jwtAuthFilter -> jwtService "Validates token signature and claims"
                securityConfig -> jwtAuthFilter "Registers the filter in the chain"

                estudianteController -> estudianteService "Delegates business rules"
                estudianteService -> estudianteRepository "CRUD, pagination, soft delete and procedure calls"

                usuarioController -> usuarioService "Delegates business rules"
                usuarioService -> seguridadRepository "Account CRUD"
                personaController -> personaService "Delegates business rules"
                personaService -> seguridadRepository "Person CRUD"
                estadoGeneralController -> estadoGeneralService "Delegates lookup"
                estadoGeneralService -> seguridadRepository "Reads the catalog"

                categoriaController -> categoriaService "Delegates business rules"
                categoriaService -> deportivoRepository "Category CRUD"
                entrenadorController -> entrenadorService "Delegates business rules"
                entrenadorService -> deportivoRepository "Coach CRUD"
                entrenadorService -> seguridadRepository "Verifies the linked person and user"
                estudianteService -> deportivoRepository "Resolves the category by foreign key"

                authController -> globalExceptionHandler "Errors handled"
                estudianteController -> globalExceptionHandler "Errors handled"
                usuarioController -> globalExceptionHandler "Errors handled"
                personaController -> globalExceptionHandler "Errors handled"
                categoriaController -> globalExceptionHandler "Errors handled"
                entrenadorController -> globalExceptionHandler "Errors handled"
                estadoGeneralController -> globalExceptionHandler "Errors handled"

                redisCacheConfig -> estudianteService "Configures the entity cache"
            }

            postgres = container "PostgreSQL 16" "Schemas 'seguridad', 'academico' and 'deportivo'. Includes the stored procedures academico.sp_contar_estudiantes_activos and academico.sp_desactivar_estudiantes_categoria, invoked via @Procedure." "PostgreSQL 16" "Database"
            redis = container "Redis 7" "Session cache and blacklist of invalidated JWT tokens." "Redis 7" "Database"
        }

        # ------------------------------------------------------------
        # Level 1 and 2 relationships
        # ------------------------------------------------------------
        estudiante -> sged.spa "Checks profile and evaluations" "HTTPS"
        entrenador -> sged.spa "Registers attendance and evaluations" "HTTPS"
        recepcionista -> sged.spa "Manages students, payments, memberships" "HTTPS"
        administrador -> sged.spa "Manages users and reports" "HTTPS"

        sged.spa -> sged.api "Consumes REST API" "HTTP/JSON, HttpOnly JWT cookie"
        sged.api -> sged.postgres "Reads/writes" "JDBC, Spring Data JPA"
        sged.api -> sged.redis "Reads/writes" "Lettuce/Redis protocol"

        sged -> smtp "Would send notification emails (roadmap)" "SMTP/TLS"
        lectorRfid -> sged "Would send presence signal (roadmap)" "HTTP POST"
    }

    views {

        systemContext sged "C4_Nivel1_Contexto" {
            include *
            autoLayout lr
            title "Level 1 - SGED System Context"
            description "Actors, the SGED system and external systems (one of them planned)."
        }

        container sged "C4_Nivel2_Contenedores" {
            include *
            autoLayout lr
            title "Level 2 - SGED Containers"
            description "Angular SPA, Spring Boot API, PostgreSQL and Redis, per docker-compose.yml (nginx terminates TLS on :8443)."
        }

        component sged.api "C4_Nivel3_Componentes_API" {
            include *
            autoLayout lr
            title "Level 3 - Components of the Spring Boot API Container"
            description "Controllers, services, repositories and security filters of the three domains: academico, deportivo and seguridad."
        }

        styles {
            element "User" {
                background #08427b
                color #ffffff
                shape person
            }
            element "External" {
                background #999999
                color #ffffff
            }
            element "Planned" {
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
            element "Service" {
                background #85bbf0
                color #000000
            }
            element "JPA Repository" {
                background #6ba4e0
                color #000000
                shape cylinder
            }
            element "Spring Security Filter" {
                background #b8a5f5
                color #000000
            }
            element "Spring Security Configuration" {
                background #b8a5f5
                color #000000
                shape folder
            }
            element "Configuration" {
                background #d4c9f7
                color #000000
                shape folder
            }
            element "Cross-cutting Component" {
                background #f5d5a5
                color #000000
            }
        }
    }
}
