package org.uteq.backend.academico.estudiante.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.academico.estudiante.dto.MiEquipoDtos.MiEquipoResponse;
import org.uteq.backend.academico.estudiante.service.MiEquipoService;
import org.uteq.backend.academico.representante.dto.InformeDtos.ComentarioInformeResponse;
import org.uteq.backend.academico.representante.dto.InformeDtos.InformeEstudianteResponse;
import org.uteq.backend.academico.representante.service.InformeService;

@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class MiEquipoController {
    private final InformeService informeService;
    private final MiEquipoService miEquipoService;

    @GetMapping("/mi-informe")
    @Transactional(readOnly = true)
    public ResponseEntity<InformeEstudianteResponse> miInforme() {
        return ResponseEntity.ok(informeService.miInforme(usernameAutenticado()));
    }

    @PostMapping("/mi-informe/comentario")
    @Transactional(readOnly = true)
    public ResponseEntity<ComentarioInformeResponse> miComentario() {
        return ResponseEntity.ok(informeService.miComentario(usernameAutenticado()));
    }

    @GetMapping("/mi-equipo")
    @Transactional(readOnly = true)
    public ResponseEntity<MiEquipoResponse> miEquipo() {
        return ResponseEntity.ok(miEquipoService.miEquipo(usernameAutenticado()));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
