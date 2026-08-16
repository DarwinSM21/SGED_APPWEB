# Diagramas de secuencia

**Sistema:** SGED — Escuela Deportiva ProFútbol
**Notación:** Mermaid `sequenceDiagram` (misma notación que
`docs/requisitos/casos-uso.md`, versionable como texto y revisable en un
pull request).

Resuelve D-01 (informe de evaluación de calidad): el repositorio no tenía
ningún diagrama de secuencia, y el comportamiento dinámico solo estaba
descrito en prosa dentro de los flujos de `docs/requisitos/casos-uso.md`.

Se documentan los tres flujos que sugiere R-01 por ser, además, los tres
puntos de mayor complejidad medida del sistema (Tabla 14 del informe):
autenticación con JWT en cookie `HttpOnly` (CU-01), alta de estudiante
(`EstudianteService::crear`) y registro de asistencia por código QR
(`QrAsistenciaService`).

Cada diagrama refleja el código tal como quedó después del plan de
corrección de este mismo informe (`AuthController`/`AuthService`
divididos por R-03; `EstudianteAccesoService` extraído por R-06), no un
estado anterior.

---

## 1. Autenticación con JWT en cookie HttpOnly (CU-01)

El token nunca aparece en el cuerpo de la respuesta ni en un header
legible por JavaScript: viaja únicamente en una cookie `HttpOnly` +
`Secure` + `SameSite=Strict` que fija el propio backend (ADR-002,
ADR-008).

```mermaid
sequenceDiagram
    actor U as Usuario
    participant LC as LoginComponent
    participant AC as AuthController
    participant AS as AuthService
    participant LAS as LoginAttemptService
    participant AM as AuthenticationManager
    participant AUD as AuditoriaService
    participant JWT as JwtService

    U->>LC: usuario + contraseña
    LC->>AC: POST /api/auth/login
    AC->>AS: login(request, ip)
    AS->>LAS: estaBloqueada(ip)

    alt IP bloqueada (>=5 fallos en 15 min)
        LAS-->>AS: true
        AS-->>AC: TooManyRequestsException
        AC-->>LC: 429 Too Many Requests
        LC-->>U: "Demasiados intentos. Intenta más tarde"
    else IP no bloqueada
        LAS-->>AS: false
        AS->>AM: authenticate(username, password)

        alt credenciales inválidas
            AM-->>AS: BadCredentialsException
            AS->>LAS: registrarFallo(ip)
            AS->>AUD: registrarConIdentidad(LOGIN_FALLIDO)
            AS-->>AC: BadCredentialsException
            AC-->>LC: 401 Unauthorized
            LC-->>U: "Usuario o contraseña incorrectos"
        else credenciales válidas
            AM-->>AS: Authentication (UserDetails + rol)
            AS->>LAS: registrarExito(ip)
            AS->>AUD: registrarConIdentidad(LOGIN)
            AS->>JWT: generateToken(username, rol)
            JWT-->>AS: accessToken
            AS->>JWT: generateRefreshToken(username, rol)
            JWT-->>AS: refreshToken
            AS-->>AC: LoginResult(accessToken, refreshToken, sesion)
            AC->>AC: setAuthCookies() — Set-Cookie sged_access + sged_refresh (HttpOnly, Secure, SameSite=Strict)
            AC-->>LC: 200 OK { username, nombre, rol } (sin token en el cuerpo)
            LC->>LC: router.navigate(homeRouteForRole(rol))
        end
    end
```

---

## 2. Alta de estudiante (`EstudianteService::crear`)

`crear()` da de alta o reactiva la **ficha** de estudiante sobre una
`Persona` que ya existe; no crea la cuenta de acceso. Habilitar el
acceso propio (para que el estudiante marque su QR) es un paso aparte,
`POST /api/estudiantes/{id}/acceso`, mostrado al final como
continuación — así es como lo usa la pantalla de Personas del
administrador (`FichaEstudianteComponent` da de alta la ficha;
`CuentaUsuarioComponent`/el botón de acceso crean la cuenta después,
solo si se pide).

```mermaid
sequenceDiagram
    actor A as Administrador/Recepcionista
    participant EC as EstudianteController
    participant ES as EstudianteService
    participant EAS as EstudianteAccesoService
    participant UR as UsuarioRepository
    participant ER as EstudianteRepository
    participant PR as PersonaRepository
    participant CR as CategoriaRepository

    A->>EC: POST /api/estudiantes {idPersona, idCategoria, codigoEstudiante, ...}
    EC->>ES: crear(request)
    ES->>EAS: validarCoherenciaConFichaEstudiante(idPersona)
    EAS->>UR: findByPersona_IdPersonaAndActivoTrue(idPersona)

    alt la persona tiene cuenta activa con otro rol
        UR-->>EAS: Usuario (rol != ESTUDIANTE)
        EAS-->>ES: IllegalArgumentException
        ES-->>EC: IllegalArgumentException
        EC-->>A: 400 Bad Request
    else sin cuenta, o cuenta ya de rol ESTUDIANTE
        UR-->>EAS: Optional.empty() / Usuario(ESTUDIANTE)
        EAS-->>ES: (continúa)
        ES->>ER: findByPersona_IdPersona(idPersona)

        alt ya existe una ficha ACTIVA
            ER-->>ES: Estudiante(activo=true)
            ES-->>EC: IllegalArgumentException("ya cuenta con una ficha activa")
            EC-->>A: 400 Bad Request
        else existe una ficha INACTIVA (baja lógica previa)
            ER-->>ES: Estudiante(activo=false)
            ES->>CR: findById(idCategoria)
            CR-->>ES: Categoria
            ES->>ER: save(estudiante reactivado: categoria, código, activo=true)
            ER-->>ES: Estudiante
            ES-->>EC: EstudianteResponse
            EC-->>A: 201 Created
        else nunca tuvo ficha
            ES->>ER: existsByCodigoEstudiante(codigo)
            ER-->>ES: false
            ES->>PR: findById(idPersona)
            PR-->>ES: Persona
            ES->>CR: findById(idCategoria)
            CR-->>ES: Categoria
            ES->>ER: save(Estudiante nuevo)
            ER-->>ES: Estudiante
            ES-->>EC: EstudianteResponse
            EC-->>A: 201 Created
        end
    end

    Note over A,EC: Continuación opcional, en otra petición:<br/>habilitar el acceso propio del estudiante
    A->>EC: POST /api/estudiantes/{id}/acceso {username, password}
    EC->>ES: habilitarAcceso(id, request)
    ES->>EAS: crearCuentaDeEstudiante(persona, request)
    EAS->>UR: existsByUsername(username)
    UR-->>EAS: false
    EAS->>UR: save(Usuario nuevo, rol=ESTUDIANTE, password hasheada)
    UR-->>EAS: Usuario
    EAS-->>ES: Usuario
    ES->>ER: save(estudiante.usuario = Usuario)
    ES-->>EC: EstudianteResponse
    EC-->>A: 201 Created
```

---

## 3. Registro de asistencia por código QR

Dos roles con permisos distintos: **recepción** emite el token
(`ADMINISTRADOR`/`RECEPCIONISTA`), **el estudiante** lo canjea desde su
propia sesión autenticada. El QR nunca contiene datos personales, solo
un identificador opaco con vencimiento corto en Redis (ver
`QrAsistenciaService`).

```mermaid
sequenceDiagram
    actor R as Recepcionista
    participant Pantalla as Pantalla QR (recepción)
    participant AQC as AsistenciaQrController
    participant QRS as QrAsistenciaService
    participant Redis as Redis
    actor E as Estudiante
    participant MAC as MarcarAsistenciaComponent
    participant AS as AsistenciaService
    participant ER as EstudianteRepository
    participant SR as SesionRepository
    participant NS as NotificacionService

    R->>Pantalla: abre QR de la sesión
    loop cada pocos segundos (rotación)
        Pantalla->>AQC: POST /asistencias/qr/sesion/{idSesion}/token
        AQC->>QRS: emitir(idSesion)
        QRS->>Redis: SET qr:asistencia:{token} = idSesion (TTL 60s)
        QRS-->>AQC: TokenQr(token, ttl)
        AQC-->>Pantalla: 200 OK
        Pantalla->>Pantalla: pinta el QR con el token nuevo
    end

    E->>MAC: enfoca el QR con la cámara
    MAC->>MAC: jsQR decodifica el token (100% en el cliente)
    MAC->>AQC: POST /asistencias/qr/marcar {token}
    AQC->>QRS: canjear(token)
    QRS->>Redis: GETDEL qr:asistencia:{token}

    alt token no existe o ya vencido/usado
        Redis-->>QRS: null
        QRS-->>AQC: Optional.empty()
        AQC-->>MAC: 410 Gone
        MAC-->>E: "Ese código ya expiró o ya se usó"
    else token vigente
        Redis-->>QRS: idSesion
        QRS-->>AQC: Optional(idSesion)
        AQC->>AS: marcarPorQr(username, idSesion)
        AS->>ER: findByUsuario_Username(username)
        ER-->>AS: Estudiante

        alt ya marcó asistencia en esta sesión
            AS-->>AQC: IllegalArgumentException
            AQC-->>MAC: 400 Bad Request
            MAC-->>E: "Ya marcaste tu asistencia en esta sesión"
        else categoría de la sesión no coincide con la del estudiante
            AS-->>AQC: IllegalArgumentException
            AQC-->>MAC: 400 Bad Request
            MAC-->>E: (mensaje genérico de fallo)
        else válido
            AS->>SR: findById(idSesion)
            SR-->>AS: SesionEntrenamiento
            AS->>AS: calcularEstado(horaInicio, ahora) → PRESENTE | TARDE
            AS->>AS: save(Asistencia)
            AS->>NS: notificarAsistencia(estudiante, estado)
            NS-->>AS: (atrapa sus propios errores: si falla notificar, la asistencia ya quedó guardada)
            AS-->>AQC: Asistencia
            AQC-->>MAC: 201 Created { estado }
            MAC-->>E: "¡Presente!" / "Marcado como tarde"
        end
    end
```
