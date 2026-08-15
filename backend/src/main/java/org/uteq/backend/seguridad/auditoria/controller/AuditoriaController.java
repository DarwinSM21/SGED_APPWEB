package org.uteq.backend.seguridad.auditoria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.seguridad.auditoria.dto.AuditoriaResponse;
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/auditorias")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<Page<AuditoriaResponse>> listar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
        OffsetDateTime desde = fechaDesde != null
                ? fechaDesde.atStartOfDay(Zonas.ECUADOR).toOffsetDateTime() : null;
        OffsetDateTime hasta = fechaHasta != null
                ? fechaHasta.plusDays(1).atStartOfDay(Zonas.ECUADOR).toOffsetDateTime() : null;

        return ResponseEntity.ok(auditoriaService.buscar(usuario, accion, entidad, desde, hasta, pageable));
    }
}
