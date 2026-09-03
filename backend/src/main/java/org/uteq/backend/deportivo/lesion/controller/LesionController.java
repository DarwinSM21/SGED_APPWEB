package org.uteq.backend.deportivo.lesion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.lesion.dto.LesionDtos.*;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.service.LesionService;

/**
 * Lesiones. Las registra el entrenador; la recepcionista no interviene. La
 * descripción es un dato de salud de un menor, así que la lectura queda
 * restringida a entrenador y administrador.
 *
 * <p>Los métodos llevan {@code @Transactional} propio: {@code aResponse()}
 * navega {@code Lesion -> Estudiante -> Persona} (LAZY) con open-in-view
 * deshabilitado, y la transacción de {@code LesionService} ya se cerró al
 * volver aquí.
 */
@RestController
@RequestMapping("/api/lesiones")
@RequiredArgsConstructor
public class LesionController {
    private final LesionService lesionService;
    private final EntrenadorRepository entrenadorRepository;

    /**
     * Lista paginada de lesiones activas.
     *
     * @param pageable paginación; por defecto 20 por página
     * @return {@code 200 OK} con la página de lesiones activas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<LesionResponse>> listarActivas(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lesionService.listarActivas(pageable).map(this::aResponse));
    }

    /**
     * Historial de lesiones de un estudiante, de la más reciente a la más
     * antigua.
     *
     * @param idEstudiante identificador del estudiante
     * @param pageable     paginación; por defecto 20 por página
     * @return {@code 200 OK} con la página de lesiones del estudiante
     */
    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<LesionResponse>> historial(
            @PathVariable Long idEstudiante,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lesionService.historialDe(idEstudiante, pageable).map(this::aResponse));
    }

    /**
     * Registra una lesión. El {@code idEntrenador} del cuerpo se ignora si
     * quien llama tiene rol {@code ENTRENADOR} (se resuelve del token); solo
     * {@code ADMINISTRADOR} debe especificarlo, y para esa cuenta es
     * obligatorio.
     *
     * @param request estudiante, entrenador (opcional para ENTRENADOR),
     *                descripción y fechas; validado con {@code @Valid}
     * @return {@code 201 Created} con la lesión registrada
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante o el entrenador no existen ({@code 404})
     * @throws IllegalArgumentException si el estudiante ya tiene una lesión
     *         activa, faltó {@code idEntrenador} para una cuenta no
     *         entrenadora, o la fecha estimada de retorno es anterior a la
     *         de la lesión ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<LesionResponse> registrar(@Valid @RequestBody RegistrarLesionRequest request) {
        var lesion = lesionService.registrar(
                request.idEstudiante(), idEntrenadorEfectivo(request.idEntrenador()), request.descripcion(),
                request.fechaLesion(), request.fechaEstimadaRetorno());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(lesion));
    }

    /**
     * Da de alta una lesión: el jugador vuelve a entrar en las plantillas.
     *
     * @param idLesion identificador de la lesión
     * @param request  fecha de alta (opcional; por defecto hoy)
     * @return {@code 200 OK} con la lesión dada de alta
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la lesión no existe ({@code 404})
     * @throws IllegalArgumentException si la lesión ya tiene alta o la fecha
     *         es anterior a la de la lesión ({@code 422})
     */
    @PostMapping("/{idLesion}/alta")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<LesionResponse> darDeAlta(@PathVariable Long idLesion,
                                                    @RequestBody(required = false) DarDeAltaRequest request) {
        var fecha = request == null ? null : request.fechaAlta();
        return ResponseEntity.ok(aResponse(lesionService.darDeAlta(idLesion, fecha)));
    }

    // El idEntrenador de una lesión no puede salir del body sin más: una
    // cuenta ENTRENADOR podría mandar el id de un colega. Si quien llama
    // tiene ese rol, se ignora el body y se resuelve del token.
    private Long idEntrenadorEfectivo(Long idEntrenadorDelBody) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esEntrenador = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ENTRENADOR"));
        if (!esEntrenador) {
            if (idEntrenadorDelBody == null) {
                throw new IllegalArgumentException("idEntrenador es obligatorio para esta cuenta");
            }
            return idEntrenadorDelBody;
        }
        return entrenadorRepository.findByUsuario_Username(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un entrenador asociado a esta cuenta"))
                .getIdEntrenador();
    }

    private LesionResponse aResponse(Lesion l) {
        var persona = l.getEstudiante().getPersona();
        return new LesionResponse(
                l.getIdLesion(),
                l.getEstudiante().getIdEstudiante(),
                persona.getNombre() + " " + persona.getApellido(),
                l.getDescripcion(),
                l.getFechaLesion(),
                l.getFechaEstimadaRetorno(),
                l.getFechaAlta(),
                l.estaActiva());
    }
}
