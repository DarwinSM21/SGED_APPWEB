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

/**
 * Asistencia por codigo QR.
 *
 * <p>Dos lados con permisos distintos, y esa asimetria es el punto del diseno:
 *
 * <ul>
 *   <li><b>Emitir</b> el token lo hace la pantalla de recepcion. Restringido a
 *       ADMINISTRADOR y RECEPCIONISTA: si cualquiera pudiera emitir codigos,
 *       bastaria pedir uno desde casa para marcarse presente.</li>
 *   <li><b>Canjearlo</b> lo hace el estudiante desde su celular, con su propia
 *       sesion. Su identidad sale del token de sesion, nunca del QR.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/asistencias/qr")
@RequiredArgsConstructor
public class AsistenciaQrController {

    private final QrAsistenciaService qrService;
    private final AsistenciaService asistenciaService;

    /**
     * Token vigente para pintar en la pantalla de recepcion. La pantalla debe
     * volver a pedirlo antes de que expire.
     */
    @PostMapping("/sesion/{idSesion}/token")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<QrAsistenciaService.TokenQr> emitir(@PathVariable Long idSesion) {
        return ResponseEntity.ok(qrService.emitir(idSesion));
    }

    /**
     * Marca la asistencia del estudiante autenticado (rol ESTUDIANTE). El
     * canjeo del token se resuelve aqui mismo, no en el servicio: el 410
     * uniforme para "no existe" y "ya se uso" es deliberado (distinguirlos
     * confirmaria que el token existio). Si el token es valido, se delega a
     * AsistenciaService para decidir PRESENTE/TARDE y persistir.
     */
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
