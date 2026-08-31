package org.uteq.backend.deportivo.asistencia.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;
import org.uteq.backend.deportivo.asistencia.service.QrAsistenciaService;

@RestController
@RequestMapping("/api/asistencias/qr")
@RequiredArgsConstructor
public class AsistenciaQrController {
    private final QrAsistenciaService qrService;
    private final AsistenciaService asistenciaService;

    @PostMapping("/sesion/{idSesion}/token")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<QrAsistenciaService.TokenQr> emitir(@PathVariable Long idSesion) {
        return ResponseEntity.ok(qrService.emitir(idSesion));
    }

    @PostMapping("/marcar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'ESTUDIANTE')")
    public ResponseEntity<MarcarQrResponse> marcar(@Valid @RequestBody MarcarQrRequest request) {
        var idSesion = qrService.canjear(request.token());
        if (idSesion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Asistencia asistencia = asistenciaService.marcarPorQr(username, idSesion.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MarcarQrResponse(asistencia.getEstado()));
    }

    public record MarcarQrRequest(@NotBlank String token) {}

    public record MarcarQrResponse(String estado) {}
}
