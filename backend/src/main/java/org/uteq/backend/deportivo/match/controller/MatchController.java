package org.uteq.backend.deportivo.match.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.match.dto.LineupDtos.SaveLineupRequest;
import org.uteq.backend.deportivo.match.dto.RosterDtos.LineupResponse;
import org.uteq.backend.deportivo.match.dto.RosterDtos.LineupFeedbackResponse;
import org.uteq.backend.deportivo.match.dto.MatchDtos.CreateMatchRequest;
import org.uteq.backend.deportivo.match.dto.MatchDtos.MatchPageResponse;
import org.uteq.backend.deportivo.match.dto.MatchDtos.MatchResponse;
import org.uteq.backend.deportivo.match.dto.MatchDtos.ResultRequest;
import org.uteq.backend.deportivo.match.service.LineupService;
import org.uteq.backend.deportivo.match.service.MatchService;

/**
 * Partidos y su alineación. Todos los endpoints exigen {@code ENTRENADOR} o
 * {@code ADMINISTRADOR}.
 *
 * <p>La alineación es un solo recurso para dos cosas: si el entrenador
 * guardó un once se devuelve ese, y si no, la sugerencia calculada con el
 * rendimiento de las últimas semanas; la bandera "guardada" indica cuál se
 * está viendo.
 */
@RestController
@RequestMapping("/api/partidos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
public class MatchController {
    private final MatchService matchService;
    private final LineupService lineupService;

    /**
     * Lista paginada de partidos, opcionalmente filtrada por categoría.
     *
     * @param idCategoria categoría por la que filtrar (opcional)
     * @param page        número de página (desde 0)
     * @param size        tamaño de página
     * @return {@code 200 OK} con la página de partidos
     */
    @GetMapping
    public ResponseEntity<MatchPageResponse> list(
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(matchService.list(idCategoria, page, size));
    }

    /**
     * Detalle de un partido.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con el partido
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{idPartido}")
    public ResponseEntity<MatchResponse> view(@PathVariable Long idPartido) {
        return ResponseEntity.ok(matchService.findById(idPartido));
    }

    /**
     * Crea un partido.
     *
     * @param request categoría, rival, fecha y localía; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el partido creado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la categoría no existe ({@code 404})
     */
    @PostMapping
    public ResponseEntity<MatchResponse> create(@Valid @RequestBody CreateMatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.create(request));
    }

    /**
     * Registra el marcador de un partido y lo cierra.
     *
     * @param idPartido identificador del partido
     * @param request   goles a favor y en contra; validado con {@code @Valid}
     * @return {@code 200 OK} con el partido cerrado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el partido no existe ({@code 404})
     * @throws IllegalArgumentException si el partido ya estaba cerrado
     *         ({@code 422})
     */
    @PutMapping("/{idPartido}/resultado")
    public ResponseEntity<MatchResponse> registerResult(
            @PathVariable Long idPartido, @Valid @RequestBody ResultRequest request) {
        return ResponseEntity.ok(matchService.registerResult(idPartido, request));
    }

    /**
     * Reabre un partido cerrado para corregir el resultado o la alineación.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con el partido reabierto
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el partido no existe ({@code 404})
     * @throws IllegalArgumentException si el partido no estaba cerrado
     *         ({@code 422})
     */
    @PostMapping("/{idPartido}/reapertura")
    public ResponseEntity<MatchResponse> reopen(@PathVariable Long idPartido) {
        return ResponseEntity.ok(matchService.reopen(idPartido));
    }

    /**
     * Elimina un partido.
     *
     * @param idPartido identificador del partido
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{idPartido}")
    public ResponseEntity<Void> delete(@PathVariable Long idPartido) {
        matchService.delete(idPartido);
        return ResponseEntity.noContent().build();
    }

    /**
     * Alineación de un partido: el once guardado, o la sugerencia del sistema
     * si no hay ninguno guardado.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con la alineación y la bandera "guardada"
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el partido no existe ({@code 404})
     */
    @GetMapping("/{idPartido}/alineacion")
    @Transactional(readOnly = true)
    public ResponseEntity<LineupResponse> viewLineup(@PathVariable Long idPartido) {
        return ResponseEntity.ok(lineupService.view(idPartido));
    }

    /**
     * Guarda el once del entrenador para un partido.
     *
     * @param idPartido identificador del partido
     * @param request   jugadores y su puesto en cancha; validado con
     *                  {@code @Valid}
     * @return {@code 200 OK} con la alineación guardada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el partido o algún jugador no existen ({@code 404})
     * @throws IllegalArgumentException si el partido está cerrado o la
     *         alineación es inválida ({@code 422})
     */
    @PutMapping("/{idPartido}/alineacion")
    @Transactional
    public ResponseEntity<LineupResponse> saveLineup(
            @PathVariable Long idPartido, @Valid @RequestBody SaveLineupRequest request) {
        return ResponseEntity.ok(lineupService.save(idPartido, request));
    }

    /**
     * Descarta el once guardado y vuelve a la sugerencia del sistema.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con la sugerencia recalculada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el partido no existe ({@code 404})
     */
    @DeleteMapping("/{idPartido}/alineacion")
    @Transactional
    public ResponseEntity<LineupResponse> resetLineup(@PathVariable Long idPartido) {
        return ResponseEntity.ok(lineupService.reset(idPartido));
    }

    /**
     * Comentario de IA sobre el once, a demanda (no se llama al modelo en
     * cada apertura de la pantalla).
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con el comentario generado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el partido no existe ({@code 404})
     */
    @PostMapping("/{idPartido}/alineacion/feedback")
    @Transactional(readOnly = true)
    public ResponseEntity<LineupFeedbackResponse> feedback(@PathVariable Long idPartido) {
        return ResponseEntity.ok(lineupService.feedback(idPartido));
    }
}
