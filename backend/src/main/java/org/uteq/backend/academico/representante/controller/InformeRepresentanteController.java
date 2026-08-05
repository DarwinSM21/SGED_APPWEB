package org.uteq.backend.academico.representante.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.representante.dto.InformeDtos.*;
import org.uteq.backend.academico.representante.service.InformeService;

import java.util.List;

/**
 * Lo que ve un representante autenticado de sus propios representados.
 * La identidad sale siempre de la sesion autenticada
 * (SecurityContextHolder, mismo patron que SesionEntrenamientoController.hoy()
 * y AuthController.me()), nunca de un parametro que el cliente pudiera
 * manipular para ver a un estudiante ajeno.
 *
 * <p>{@code @Transactional(readOnly = true)} va tambien aqui, no solo en
 * el servicio: open-in-view esta deshabilitado (application.yml) y la
 * respuesta navega relaciones LAZY (Persona, Categoria) al construir el
 * DTO. Sin una transaccion activa en ese momento, esa navegacion lanza
 * LazyInitializationException -mismo problema ya resuelto en
 * LesionController y SesionEntrenamientoController-.
 */
@RestController
@RequestMapping("/api/representante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('REPRESENTANTE')")
public class InformeRepresentanteController {

    private final InformeService informeService;

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

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
