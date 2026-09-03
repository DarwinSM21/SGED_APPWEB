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
 * Asistencia por código QR, con dos lados de permisos distintos:
 * <ul>
 *   <li><b>Emitir</b> el token lo hace la pantalla de recepción
 *       ({@code ADMINISTRADOR} / {@code RECEPCIONISTA}): si cualquiera
 *       pudiera emitir códigos, bastaría pedir uno desde casa para marcarse
 *       presente.</li>
 *   <li><b>Canjearlo</b> lo hace el estudiante desde su celular, con su
 *       propia sesión. Su identidad sale del token de sesión, nunca del
 *       QR.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/asistencias/qr")
@RequiredArgsConstructor
public class AsistenciaQrController {
    private final QrAsistenciaService qrService;
    private final AsistenciaService asistenciaService;

    /**
     * Emite un token QR vigente para una sesión, para pintarlo en la pantalla
     * de recepción. La pantalla debe volver a pedirlo antes de que expire.
     *
     * @param idSesion identificador de la sesión
     * @return {@code 200 OK} con el token y su vencimiento
     */
    @PostMapping("/sesion/{idSesion}/token")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<QrAsistenciaService.TokenQr> emitir(@PathVariable Long idSesion) {
        return ResponseEntity.ok(qrService.emitir(idSesion));
    }

    /**
     * Marca la asistencia del estudiante autenticado. El canjeo del token se
     * resuelve aquí: el {@code 410} uniforme para "no existe" y "ya se usó"
     * es deliberado (distinguirlos confirmaría que el token existió).
     *
     * @param request cuerpo con el {@code token} del QR; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el estado marcado
     *         ({@code PRESENTE} / {@code TARDE}), o {@code 410 Gone} si el
     *         token no es válido o ya se usó
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la cuenta no tiene ficha de estudiante ({@code 404})
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

    /**
     * Cuerpo de {@link #marcar}.
     *
     * @param token token del código QR escaneado
     */
    public record MarcarQrRequest(@NotBlank String token) {}

    /**
     * Respuesta de {@link #marcar}.
     *
     * @param estado estado de asistencia resuelto ({@code PRESENTE} /
     *               {@code TARDE})
     */
    public record MarcarQrResponse(String estado) {}
}
