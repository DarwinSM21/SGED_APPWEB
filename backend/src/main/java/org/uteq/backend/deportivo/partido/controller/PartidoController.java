package org.uteq.backend.deportivo.partido.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.partido.dto.AlineacionDtos.GuardarAlineacionRequest;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.AlineacionResponse;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.FeedbackAlineacionResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.CrearPartidoRequest;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoPageResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.ResultadoRequest;
import org.uteq.backend.deportivo.partido.service.AlineacionService;
import org.uteq.backend.deportivo.partido.service.PartidoService;

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
public class PartidoController {
    private final PartidoService partidoService;
    private final AlineacionService alineacionService;

    /**
     * Lista paginada de partidos, opcionalmente filtrada por categoría.
     *
     * @param idCategoria categoría por la que filtrar (opcional)
     * @param page        número de página (desde 0)
     * @param size        tamaño de página
     * @return {@code 200 OK} con la página de partidos
     */
    @GetMapping
    public ResponseEntity<PartidoPageResponse> listar(
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(partidoService.listar(idCategoria, page, size));
    }

    /**
     * Detalle de un partido.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con el partido
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{idPartido}")
    public ResponseEntity<PartidoResponse> ver(@PathVariable Long idPartido) {
        return ResponseEntity.ok(partidoService.buscarPorId(idPartido));
    }

    /**
     * Crea un partido.
     *
     * @param request categoría, rival, fecha y localía; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el partido creado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la categoría no existe ({@code 404})
     */
    @PostMapping
    public ResponseEntity<PartidoResponse> crear(@Valid @RequestBody CrearPartidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partidoService.crear(request));
    }

    /**
     * Registra el marcador de un partido y lo cierra.
     *
     * @param idPartido identificador del partido
     * @param request   goles a favor y en contra; validado con {@code @Valid}
     * @return {@code 200 OK} con el partido cerrado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el partido no existe ({@code 404})
     * @throws IllegalArgumentException si el partido ya estaba cerrado
     *         ({@code 422})
     */
    @PutMapping("/{idPartido}/resultado")
    public ResponseEntity<PartidoResponse> registrarResultado(
            @PathVariable Long idPartido, @Valid @RequestBody ResultadoRequest request) {
        return ResponseEntity.ok(partidoService.registrarResultado(idPartido, request));
    }

    /**
     * Reabre un partido cerrado para corregir el resultado o la alineación.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con el partido reabierto
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el partido no existe ({@code 404})
     * @throws IllegalArgumentException si el partido no estaba cerrado
     *         ({@code 422})
     */
    @PostMapping("/{idPartido}/reapertura")
    public ResponseEntity<PartidoResponse> reabrir(@PathVariable Long idPartido) {
        return ResponseEntity.ok(partidoService.reabrir(idPartido));
    }

    /**
     * Elimina un partido.
     *
     * @param idPartido identificador del partido
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{idPartido}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idPartido) {
        partidoService.eliminar(idPartido);
        return ResponseEntity.noContent().build();
    }

    /**
     * Alineación de un partido: el once guardado, o la sugerencia del sistema
     * si no hay ninguno guardado.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con la alineación y la bandera "guardada"
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el partido no existe ({@code 404})
     */
    @GetMapping("/{idPartido}/alineacion")
    @Transactional(readOnly = true)
    public ResponseEntity<AlineacionResponse> verAlineacion(@PathVariable Long idPartido) {
        return ResponseEntity.ok(alineacionService.ver(idPartido));
    }

    /**
     * Guarda el once del entrenador para un partido.
     *
     * @param idPartido identificador del partido
     * @param request   jugadores y su puesto en cancha; validado con
     *                  {@code @Valid}
     * @return {@code 200 OK} con la alineación guardada
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el partido o algún jugador no existen ({@code 404})
     * @throws IllegalArgumentException si el partido está cerrado o la
     *         alineación es inválida ({@code 422})
     */
    @PutMapping("/{idPartido}/alineacion")
    @Transactional
    public ResponseEntity<AlineacionResponse> guardarAlineacion(
            @PathVariable Long idPartido, @Valid @RequestBody GuardarAlineacionRequest request) {
        return ResponseEntity.ok(alineacionService.guardar(idPartido, request));
    }

    /**
     * Descarta el once guardado y vuelve a la sugerencia del sistema.
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con la sugerencia recalculada
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el partido no existe ({@code 404})
     */
    @DeleteMapping("/{idPartido}/alineacion")
    @Transactional
    public ResponseEntity<AlineacionResponse> restablecerAlineacion(@PathVariable Long idPartido) {
        return ResponseEntity.ok(alineacionService.restablecer(idPartido));
    }

    /**
     * Comentario de IA sobre el once, a demanda (no se llama al modelo en
     * cada apertura de la pantalla).
     *
     * @param idPartido identificador del partido
     * @return {@code 200 OK} con el comentario generado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el partido no existe ({@code 404})
     */
    @PostMapping("/{idPartido}/alineacion/feedback")
    @Transactional(readOnly = true)
    public ResponseEntity<FeedbackAlineacionResponse> feedback(@PathVariable Long idPartido) {
        return ResponseEntity.ok(alineacionService.feedback(idPartido));
    }
}
