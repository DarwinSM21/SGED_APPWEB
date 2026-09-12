package org.uteq.backend.deportivo.session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.session.dto.SessionCreateRequest;
import org.uteq.backend.deportivo.session.dto.SessionHistoryResponse;
import org.uteq.backend.deportivo.session.dto.SessionTodayResponse;
import org.uteq.backend.deportivo.session.service.TrainingSessionService;

import java.util.List;

/**
 * Punto de entrada del entrenador (y, desde {@code RECEPCIONISTA}, de la
 * pantalla de QR) a sus sesiones: cuáles hay hoy, el historial y el alta.
 *
 * <p>La lógica de negocio vive en {@link TrainingSessionService} (D-03 /
 * R-03 del informe de evaluación de calidad): este controlador solo resuelve
 * la identidad autenticada y traduce HTTP a llamadas de dominio.
 */
@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class TrainingSessionController {
    private final TrainingSessionService sesionService;

    /**
     * Sesiones de hoy. {@code ADMINISTRADOR} y {@code RECEPCIONISTA} ven
     * todas (el recepcionista necesita elegir cualquiera para mostrar su QR);
     * un {@code ENTRENADOR} solo las suyas, resueltas desde su usuario
     * autenticado.
     *
     * @return {@code 200 OK} con las sesiones de hoy visibles para el rol
     */
    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<SessionTodayResponse>> today() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean veTodasLasSesiones = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equals("ROLE_RECEPCIONISTA"));

        return ResponseEntity.ok(sesionService.todaysSessions(auth.getName(), veTodasLasSesiones));
    }

    /**
     * Historial de sesiones (pasadas y futuras), paginado.
     * {@code ADMINISTRADOR} ve las de todos los entrenadores; un
     * {@code ENTRENADOR}, solo las suyas.
     *
     * @param page número de página (desde 0)
     * @param size tamaño de página
     * @return {@code 200 OK} con la página de sesiones
     */
    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<List<SessionTodayResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean veTodasLasSesiones = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        return ResponseEntity.ok(sesionService.mySessions(auth.getName(), veTodasLasSesiones, page, size));
    }

    /**
     * Qué pasó en una sesión concreta: la lista de quién estuvo y quién no.
     * {@code @Transactional} va aquí porque la respuesta navega relaciones
     * LAZY con open-in-view deshabilitado.
     *
     * @param idSesion identificador de la sesión
     * @return {@code 200 OK} con el resumen y la nómina de asistencia
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la sesión no existe ({@code 404})
     */
    @GetMapping("/{idSesion}/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<SessionHistoryResponse> history(@PathVariable Long idSesion) {
        return ResponseEntity.ok(sesionService.history(idSesion));
    }

    /**
     * Crea una sesión propia. El {@code idEntrenador} nunca viene del
     * cliente: se resuelve del usuario autenticado. Un {@code ADMINISTRADOR}
     * pasa el chequeo de rol pero recibe {@code 404} (no "es" un entrenador).
     *
     * @param request categoría, fecha, franja horaria y campo; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con la sesión creada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la cuenta no tiene entrenador asociado o la categoría no
     *         existe ({@code 404})
     * @throws IllegalArgumentException si la franja es inválida o se solapa
     *         con otra sesión de la misma categoría ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<SessionTodayResponse> create(@Valid @RequestBody SessionCreateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SessionTodayResponse creada = sesionService.create(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
