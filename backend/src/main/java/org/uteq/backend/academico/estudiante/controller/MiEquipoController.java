package org.uteq.backend.academico.estudiante.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.academico.estudiante.dto.MiEquipoDtos.MiEquipoResponse;
import org.uteq.backend.academico.estudiante.service.MiEquipoService;
import org.uteq.backend.academico.representante.dto.InformeDtos.InformeEstudianteResponse;
import org.uteq.backend.academico.representante.service.InformeService;

/**
 * Lo que un ESTUDIANTE ve sobre si mismo mas alla de su asistencia (que
 * ya cubre MiAsistenciaController): sus estadisticas de evaluacion y su
 * equipo (categoria, posicion, entrenador, companeros).
 *
 * <p>mi-informe reutiliza InformeService tal cual lo usa el representante
 * para el informe de un representado -mismas piezas, misma forma de
 * respuesta-, solo que resuelto por la propia cuenta autenticada en vez
 * de por un vinculo representante-estudiante.
 */
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

    @GetMapping("/mi-equipo")
    @Transactional(readOnly = true)
    public ResponseEntity<MiEquipoResponse> miEquipo() {
        return ResponseEntity.ok(miEquipoService.miEquipo(usernameAutenticado()));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
