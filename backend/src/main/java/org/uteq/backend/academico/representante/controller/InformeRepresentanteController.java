package org.uteq.backend.academico.representante.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.representante.dto.InformeDtos.*;
import org.uteq.backend.academico.representante.dto.NotificacionDtos.*;
import org.uteq.backend.academico.representante.service.InformeService;
import org.uteq.backend.academico.representante.service.NotificacionService;

import java.util.List;

@RestController
@RequestMapping("/api/representante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('REPRESENTANTE')")
public class InformeRepresentanteController {
    private final InformeService informeService;
    private final NotificacionService notificacionService;

    @GetMapping("/estudiantes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EstudianteResumenResponse>> misRepresentados() {
        return ResponseEntity.ok(informeService.misRepresentados(usernameAutenticado()));
    }

    @GetMapping("/estudiantes/{idEstudiante}/informe")
    @Transactional(readOnly = true)
    public ResponseEntity<InformeEstudianteResponse> informe(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(informeService.informeDe(usernameAutenticado(), idEstudiante));
    }

    @PostMapping("/estudiantes/{idEstudiante}/informe/comentario")
    @Transactional(readOnly = true)
    public ResponseEntity<ComentarioInformeResponse> comentario(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(informeService.comentarioDe(usernameAutenticado(), idEstudiante));
    }

    @GetMapping("/notificaciones")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NotificacionResponse>> misNotificaciones() {
        return ResponseEntity.ok(notificacionService.misNotificaciones(usernameAutenticado()));
    }

    @GetMapping("/notificaciones/no-leidas")
    @Transactional(readOnly = true)
    public ResponseEntity<ConteoNoLeidasResponse> conteoNoLeidas() {
        return ResponseEntity.ok(new ConteoNoLeidasResponse(notificacionService.conteoNoLeidas(usernameAutenticado())));
    }

    @PostMapping("/notificaciones/{idNotificacion}/leida")
    @Transactional
    public ResponseEntity<Void> marcarLeida(@PathVariable Long idNotificacion) {
        notificacionService.marcarLeida(usernameAutenticado(), idNotificacion);
        return ResponseEntity.noContent().build();
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
