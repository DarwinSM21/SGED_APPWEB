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
 * Lesiones. Las registra el entrenador; la recepcionista no interviene.
 *
 * <p>La descripcion es un dato de salud de un menor, asi que la lectura queda
 * restringida a entrenador y administrador. Un rol USER no tiene por que ver
 * el historial medico de nadie.
 *
 * <p>Los cuatro metodos llevan {@code @Transactional} propio: aResponse()
 * navega Lesion -&gt; Estudiante -&gt; Persona (ambas relaciones LAZY), y
 * open-in-view esta deshabilitado en este proyecto (application.yml), asi
 * que sin una transaccion activa en el momento de mapear, esa navegacion
 * lanza LazyInitializationException. El propio LesionService ya es
 * transaccional, pero esa transaccion se cierra al volver aqui.
 */
@RestController
@RequestMapping("/api/lesiones")
@RequiredArgsConstructor
public class LesionController {

    private final LesionService lesionService;
    private final EntrenadorRepository entrenadorRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<LesionResponse>> listarActivas(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lesionService.listarActivas(pageable).map(this::aResponse));
    }

    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<LesionResponse>> historial(
            @PathVariable Long idEstudiante,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lesionService.historialDe(idEstudiante, pageable).map(this::aResponse));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<LesionResponse> registrar(@Valid @RequestBody RegistrarLesionRequest request) {
        var lesion = lesionService.registrar(
                request.idEstudiante(), idEntrenadorEfectivo(request.idEntrenador()), request.descripcion(),
                request.fechaLesion(), request.fechaEstimadaRetorno());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(lesion));
    }

    @PostMapping("/{idLesion}/alta")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Transactional
    public ResponseEntity<LesionResponse> darDeAlta(@PathVariable Long idLesion,
                                                    @RequestBody(required = false) DarDeAltaRequest request) {
        var fecha = request == null ? null : request.fechaAlta();
        return ResponseEntity.ok(aResponse(lesionService.darDeAlta(idLesion, fecha)));
    }

    /**
     * El idEntrenador de una lesion no puede salir del body sin mas: una
     * cuenta ENTRENADOR podria mandar el id de un colega. Si quien llama
     * tiene ese rol, se ignora lo que venga en el body y se resuelve del
     * token (mismo criterio que SesionEntrenamientoController). Solo
     * ADMINISTRADOR -que no tiene un id de entrenador propio- puede
     * especificar cualquiera, y para eso si es obligatorio.
     */
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
