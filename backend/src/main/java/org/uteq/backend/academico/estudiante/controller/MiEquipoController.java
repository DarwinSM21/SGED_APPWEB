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

/**
 * Lo que un {@code ESTUDIANTE} ve sobre sí mismo más allá de su asistencia
 * (que ya cubre {@code MiAsistenciaController}): sus estadísticas de
 * evaluación y su equipo (categoría, posición, entrenador, compañeros).
 *
 * <p>{@code mi-informe} reutiliza {@link InformeService} tal cual lo usa el
 * representante para el informe de un representado —mismas piezas, misma
 * forma de respuesta—, solo que resuelto por la propia cuenta autenticada.
 */
@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class MiEquipoController {
    private final InformeService informeService;
    private final MiEquipoService miEquipoService;

    /**
     * Informe de evaluación del estudiante autenticado.
     *
     * @return {@code 200 OK} con las estadísticas del estudiante
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la cuenta no tiene ficha de estudiante asociada
     */
    @GetMapping("/mi-informe")
    @Transactional(readOnly = true)
    public ResponseEntity<InformeEstudianteResponse> miInforme() {
        return ResponseEntity.ok(informeService.miInforme(usernameAutenticado()));
    }

    /**
     * Comentario en lenguaje natural sobre el informe del estudiante
     * autenticado. Va en {@code POST} porque cada llamada gasta cuota de un
     * servicio externo: se pide a demanda, no al abrir la pantalla.
     *
     * @return {@code 200 OK} con el comentario generado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la cuenta no tiene ficha de estudiante asociada
     */
    @PostMapping("/mi-informe/comentario")
    @Transactional(readOnly = true)
    public ResponseEntity<ComentarioInformeResponse> miComentario() {
        return ResponseEntity.ok(informeService.miComentario(usernameAutenticado()));
    }

    /**
     * Equipo del estudiante autenticado: categoría, posición, entrenador de
     * la próxima sesión y compañeros de categoría.
     *
     * @return {@code 200 OK} con los datos del equipo
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la cuenta no tiene ficha de estudiante asociada
     */
    @GetMapping("/mi-equipo")
    @Transactional(readOnly = true)
    public ResponseEntity<MiEquipoResponse> miEquipo() {
        return ResponseEntity.ok(miEquipoService.miEquipo(usernameAutenticado()));
    }

    private String usernameAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
