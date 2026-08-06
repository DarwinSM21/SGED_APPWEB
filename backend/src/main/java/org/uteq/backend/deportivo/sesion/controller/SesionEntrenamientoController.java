package org.uteq.backend.deportivo.sesion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionDiariaRepository;
import org.uteq.backend.deportivo.sesion.dto.SesionHoyResponse;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Punto de entrada del entrenador (y, desde RECEPCIONISTA, de la pantalla de
 * QR): que sesiones hay hoy.
 *
 * <p>Sin esto, la pantalla de evaluacion diaria (que exige un id de sesion en
 * la ruta) no tiene forma de descubrirse a si misma: el entrenador tendria
 * que conocer el numero de antemano. Lo mismo le pasaria a recepcion para
 * elegir para cual sesion mostrar el QR. Es deliberadamente de solo lectura y
 * no incluye alta de sesiones ni de horarios recurrentes, que quedan fuera
 * del alcance de este modulo.
 */
@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionEntrenamientoController {

    private final SesionEntrenamientoRepository sesionRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final EvaluacionDiariaRepository evaluacionRepository;

    /**
     * Sesiones de hoy. Un ADMINISTRADOR o RECEPCIONISTA ve todas (el
     * recepcionista necesita elegir cualquiera de ellas para mostrar su QR,
     * no esta atado a un entrenador concreto); un ENTRENADOR solo las suyas,
     * resuelto desde su propio usuario autenticado y no desde un parametro
     * que el cliente pudiera manipular para ver la agenda de otro.
     */
    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SesionHoyResponse>> hoy() {
        LocalDate hoy = LocalDate.now();
        boolean veTodasLasSesiones = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equals("ROLE_RECEPCIONISTA"));

        List<SesionEntrenamiento> sesiones;
        if (veTodasLasSesiones) {
            sesiones = sesionRepository.findByFechaOrderByHoraInicioAsc(hoy);
        } else {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            var entrenador = entrenadorRepository.findByUsuario_Username(username).orElse(null);
            sesiones = entrenador == null
                    ? List.of()
                    : sesionRepository.findByFechaOrderByHoraInicioAsc(hoy).stream()
                        .filter(s -> s.getEntrenador().getIdEntrenador().equals(entrenador.getIdEntrenador()))
                        .toList();
        }

        return ResponseEntity.ok(sesiones.stream().map(this::aResponse).toList());
    }

    private SesionHoyResponse aResponse(SesionEntrenamiento s) {
        var persona = s.getEntrenador().getPersona();
        return new SesionHoyResponse(
                s.getIdSesion(),
                s.getCategoria().getNombre(),
                persona.getNombre() + " " + persona.getApellido(),
                s.getHoraInicio(),
                s.getHoraFin(),
                s.getCampo(),
                s.getEstado(),
                evaluacionRepository.existsBySesionIdSesion(s.getIdSesion()));
    }
}
