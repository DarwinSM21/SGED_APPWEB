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
(`StudentService::create`) y registro de asistencia por código QR
(`QrAsistenciaService`, dominio `deportivo`).

Cada diagrama refleja el código tal como quedó después del plan de
corrección de este mismo informe (`AuthController`/`AuthService`
divididos por R-03; `StudentAccessService` extraído por R-06), no un
estado anterior. Los tres flujos usan los nombres reales de clase y
método del backend tras el renombrado a inglés del Punto E1
(`AttendanceQrController`, `QrAttendanceService`, `AttendanceService`,
`TrainingSessionRepository`, etc., ya coinciden con el código). **Única
excepción:** el flujo 3 rotula al componente del estudiante como
`MarkAttendanceComponent`, nombre que todavía no existe en el
frontend — el componente Angular real sigue llamándose
`MarcarAsistenciaComponent`, porque el renombrado a inglés del Punto E1
alcanzó solo al backend; el de los componentes Angular es un trabajo
aparte, pendiente de reparto con el equipo.

---

## 1. Autenticación con JWT en cookie HttpOnly (CU-01)

El token nunca aparece en el cuerpo de la respuesta ni en un header
legible por JavaScript: viaja únicamente en una cookie `HttpOnly` +
`Secure` + `SameSite=Strict` que fija el propio backend (ADR-002,
ADR-008).

```mermaid
sequenceDiagram
    actor U as User
    participant LC as LoginComponent
    participant AC as AuthController
    participant AS as AuthService
    participant LAS as LoginAttemptService
    participant AM as AuthenticationManager
    participant AUD as AuditService
    participant JWT as JwtService

    U->>LC: username + password
    LC->>AC: POST /api/auth/login
    AC->>AS: login(request, ip)
    AS->>LAS: isBlocked(ip)

    alt IP blocked (>=5 failures in 15 min)
        LAS-->>AS: true
        AS-->>AC: TooManyRequestsException
        AC-->>LC: 429 Too Many Requests
        LC-->>U: "Too many attempts. Try again later"
    else IP not blocked
        LAS-->>AS: false
        AS->>AM: authenticate(username, password)

        alt invalid credentials
            AM-->>AS: BadCredentialsException
            AS->>LAS: recordFailure(ip)
            AS->>AUD: recordEventWithIdentity(LOGIN_FAILED)
            AS-->>AC: BadCredentialsException
            AC-->>LC: 401 Unauthorized
            LC-->>U: "Incorrect username or password"
        else valid credentials
            AM-->>AS: Authentication (UserDetails + role)
            AS->>LAS: recordSuccess(ip)
            AS->>AUD: recordEventWithIdentity(LOGIN)
            AS->>JWT: generateToken(username, role)
            JWT-->>AS: accessToken
            AS->>JWT: generateRefreshToken(username, role)
            JWT-->>AS: refreshToken
            AS-->>AC: LoginResult(accessToken, refreshToken, session)
            AC->>AC: setAuthCookies() — Set-Cookie sged_access + sged_refresh (HttpOnly, Secure, SameSite=Strict)
            AC-->>LC: 200 OK { username, name, role } (no token in the body)
            LC->>LC: router.navigate(homeRouteForRole(role))
        end
    end
```

---

## 2. Alta de estudiante (`StudentService::create`)

`create()` da de alta o reactiva la **ficha** de estudiante sobre una
`Person` que ya existe; no crea la cuenta de acceso. Habilitar el
acceso propio (para que el estudiante marque su QR) es un paso aparte,
`POST /api/estudiantes/{id}/acceso`, mostrado al final como
continuación — así es como lo usa la pantalla de Personas del
administrador (`FichaEstudianteComponent` da de alta la ficha;
`CuentaUsuarioComponent`/el botón de acceso crean la cuenta después,
solo si se pide).

> `ESTUDIANTE` en este diagrama es el valor literal del **rol** de
> `UserAccount` (el catálogo de roles de seguridad, ej.
> `hasRole('ESTUDIANTE')`), no el `RecipientType` de `Assignment` — son
> dos vocabularios distintos que comparten la misma palabra en español
> por coincidencia. Los roles no se traducen (quedan fuera del alcance
> del Punto E3): están anclados a `@PreAuthorize`/`hasRole(...)` en
> ~40 archivos del backend y a los guards de rutas del frontend.

```mermaid
sequenceDiagram
    actor A as Administrator/Receptionist
    participant EC as StudentController
    participant ES as StudentService
    participant EAS as StudentAccessService
    participant UR as UserAccountRepository
    participant ER as StudentRepository
    participant PR as PersonRepository
    participant CR as CategoryRepository

    A->>EC: POST /api/estudiantes {personId, categoryId, studentCode, ...}
    EC->>ES: create(request)
    ES->>EAS: validateConsistencyWithStudentRecord(personId)
    EAS->>UR: findByPerson_IdAndActiveTrue(personId)

    alt the person has an active account with another role
        UR-->>EAS: UserAccount (role != ESTUDIANTE)
        EAS-->>ES: IllegalArgumentException
        ES-->>EC: IllegalArgumentException
        EC-->>A: 400 Bad Request
    else no account, or account already has role ESTUDIANTE
        UR-->>EAS: Optional.empty() / UserAccount(ESTUDIANTE)
        EAS-->>ES: (continues)
        ES->>ER: findByPerson_Id(personId)

        alt an ACTIVE record already exists
            ER-->>ES: Student(active=true)
            ES-->>EC: IllegalArgumentException("already has an active record")
            EC-->>A: 400 Bad Request
        else an INACTIVE record exists (previous soft delete)
            ER-->>ES: Student(active=false)
            ES->>CR: findById(categoryId)
            CR-->>ES: Category
            ES->>ER: save(reactivated student: category, code, active=true)
            ER-->>ES: Student
            ES-->>EC: StudentResponse
            EC-->>A: 201 Created
        else never had a record
            ES->>ER: existsByStudentCode(studentCode)
            ER-->>ES: false
            ES->>PR: findById(personId)
            PR-->>ES: Person
            ES->>CR: findById(categoryId)
            CR-->>ES: Category
            ES->>ER: save(new Student)
            ER-->>ES: Student
            ES-->>EC: StudentResponse
            EC-->>A: 201 Created
        end
    end

    Note over A,EC: Optional continuation, in another request:<br/>enable the student's own access
    A->>EC: POST /api/estudiantes/{id}/acceso {username, password}
    EC->>ES: enableAccess(id, request)
    ES->>EAS: createStudentAccount(person, request)
    EAS->>UR: existsByUsername(username)
    UR-->>EAS: false
    EAS->>UR: save(new UserAccount, role=ESTUDIANTE, hashed password)
    UR-->>EAS: UserAccount
    EAS-->>ES: UserAccount
    ES->>ER: save(student.userAccount = UserAccount)
    ES-->>EC: StudentResponse
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
    actor R as Receptionist
    participant Pantalla as QR Screen (reception)
    participant AQC as AttendanceQrController
    participant QRS as QrAttendanceService
    participant Redis as Redis
    actor E as Student
    participant MAC as MarkAttendanceComponent
    participant AS as AttendanceService
    participant ER as StudentRepository
    participant SR as TrainingSessionRepository
    participant NS as NotificationService

    R->>Pantalla: opens the session's QR
    loop every few seconds (rotation)
        Pantalla->>AQC: POST /api/asistencias/qr/sesion/{sessionId}/token
        AQC->>QRS: issue(sessionId)
        QRS->>Redis: SET qr:attendance:{token} = sessionId (TTL 60s)
        QRS-->>AQC: QrToken(token, expiresInSeconds)
        AQC-->>Pantalla: 200 OK
        Pantalla->>Pantalla: renders the QR with the new token
    end

    E->>MAC: scans the QR with the camera
    MAC->>MAC: jsQR decodes the token (100% client-side)
    MAC->>AQC: POST /api/asistencias/qr/marcar {token}
    AQC->>QRS: redeem(token)
    QRS->>Redis: GETDEL qr:attendance:{token}

    alt token does not exist or already expired/used
        Redis-->>QRS: null
        QRS-->>AQC: Optional.empty()
        AQC-->>MAC: 410 Gone
        MAC-->>E: "That code already expired or was already used"
    else token still valid
        Redis-->>QRS: sessionId
        QRS-->>AQC: Optional(sessionId)
        AQC->>AS: markByQr(username, sessionId)
        AS->>ER: findByUserAccount_Username(username)
        ER-->>AS: Student

        alt already checked in for this session
            AS-->>AQC: IllegalArgumentException
            AQC-->>MAC: 400 Bad Request
            MAC-->>E: "You already checked in for this session"
        else the session's category does not match the student's
            AS-->>AQC: IllegalArgumentException
            AQC-->>MAC: 400 Bad Request
            MAC-->>E: (generic failure message)
        else valid
            AS->>SR: findById(sessionId)
            SR-->>AS: TrainingSession
            AS->>AS: calculateStatus(startTime, now) → PRESENT | LATE
            AS->>AS: save(Attendance)
            AS->>NS: notifyAttendance(student, status)
            NS-->>AS: (catches its own errors: if notifying fails, attendance was already saved)
            AS-->>AQC: Attendance
            AQC-->>MAC: 201 Created { status }
            MAC-->>E: "Present!" / "Marked as late"
        end
    end
```
