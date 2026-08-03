package org.uteq.backend.deportivo.asistencia.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.asistencia.service.QrAsistenciaService;

/**
 * Asistencia por codigo QR.
 *
 * <p>Dos lados con permisos distintos, y esa asimetria es el punto del diseno:
 *
 * <ul>
 *   <li><b>Emitir</b> el token lo hace la pantalla de recepcion. Restringido a
 *       ADMINISTRADOR: si cualquiera pudiera emitir codigos, bastaria pedir uno
 *       desde casa para marcarse presente.</li>
 *   <li><b>Canjearlo</b> lo hace el estudiante desde su celular, con su propia
 *       sesion. Su identidad sale del token de sesion, nunca del QR.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/asistencias/qr")
@RequiredArgsConstructor
public class AsistenciaQrController {

    private final QrAsistenciaService qrService;

    /**
     * Token vigente para pintar en la pantalla de recepcion. La pantalla debe
     * volver a pedirlo antes de que expire.
     */
    @PostMapping("/sesion/{idSesion}/token")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<QrAsistenciaService.TokenQr> emitir(@PathVariable Long idSesion) {
        return ResponseEntity.ok(qrService.emitir(idSesion));
    }

    /**
     * Marca la asistencia del estudiante autenticado.
     *
     * <p>Todavia no persiste: falta resolver el estudiante a partir de la
     * sesion autenticada y decidir la regla de TARDE segun la hora de inicio.
     * Se deja explicito con 501 en vez de devolver un 200 que aparente algo
     * que no ocurre.
     */
    @PostMapping("/marcar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'USER')")
    public ResponseEntity<Void> marcar(@Valid @RequestBody MarcarQrRequest request) {
        var idSesion = qrService.canjear(request.token());
        if (idSesion.isEmpty()) {
            // Token inexistente, ya usado o expirado. No se distingue el caso
            // a proposito: decir "ya fue usado" confirmaria que existio.
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    public record MarcarQrRequest(@NotBlank String token) {}
}
