package org.uteq.backend.academico.estudiante.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.academico.estudiante.dto.ActualizarPosicionRequest;
import org.uteq.backend.academico.estudiante.dto.EstudiantePageResponse;
import org.uteq.backend.academico.estudiante.dto.EstudianteRequest;
import org.uteq.backend.academico.estudiante.dto.EstudianteResponse;
import org.uteq.backend.academico.estudiante.dto.HabilitarAccesoRequest;
import org.uteq.backend.academico.estudiante.service.EstudianteService;

/**
 * CRUD de {@code Estudiante} con paginación y baja lógica, más operaciones
 * de conjunto por categoría (conteo, desactivación) y la habilitación del
 * acceso propio del estudiante. Los endpoints de escritura quedan
 * restringidos a {@code ADMINISTRADOR} / {@code RECEPCIONISTA} vía
 * {@code @PreAuthorize}; la lectura la comparte también {@code ENTRENADOR}.
 */
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {
    private final EstudianteService estudianteService;

    /**
     * Lista paginada de estudiantes activos.
     *
     * @param page número de página (desde 0)
     * @param size tamaño de página
     * @param sort par {@code campo[,asc|desc]}; por defecto
     *             {@code idEstudiante,asc}
     * @return {@code 200 OK} con la página solicitada
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudiantePageResponse<EstudianteResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idEstudiante,asc") String[] sort) {
        String campo = sort[0];
        Sort.Direction dir = sort.length > 1 && "desc".equalsIgnoreCase(sort[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(dir, campo));

        return ResponseEntity.ok(estudianteService.listar(pageRequest));
    }

    /**
     * Busca un estudiante por su identificador.
     *
     * @param id identificador del estudiante
     * @return {@code 200 OK} con el estudiante
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.buscarPorId(id));
    }

    /**
     * Registra un estudiante sobre una persona ya existente; si la persona
     * tenía una ficha inactiva, la reactiva.
     *
     * @param request datos del estudiante; validado con {@code @Valid}
     * @return {@code 201 Created} con el estudiante registrado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si la persona, la categoría o el estado referidos no existen
     * @throws IllegalArgumentException si la persona ya tiene ficha activa o
     *         el código de estudiante está en uso ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> crear(
            @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteService.crear(request));
    }

    /**
     * Actualiza la ficha completa de un estudiante.
     *
     * @param id      identificador del estudiante a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con el estudiante actualizado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante o alguna referencia no existen ({@code 404})
     * @throws IllegalArgumentException si el código nuevo pertenece a otro
     *         estudiante ({@code 422})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.editar(id, request));
    }

    /**
     * Actualiza solo la posición nominal, no el resto de la ficha: a
     * diferencia de {@link #editar}, esto también lo puede usar
     * {@code ENTRENADOR} desde evaluación diaria.
     *
     * @param id      identificador del estudiante
     * @param request cuerpo con {@code idPosicion} ({@code null} para quitarla)
     * @return {@code 200 OK} con el estudiante actualizado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante o la posición no existen ({@code 404})
     */
    @PutMapping("/{id}/posicion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<EstudianteResponse> actualizarPosicion(
            @PathVariable Long id, @RequestBody ActualizarPosicionRequest request) {
        return ResponseEntity.ok(estudianteService.actualizarPosicion(id, request.idPosicion()));
    }

    /**
     * Baja lógica de un estudiante ({@code activo = false}).
     *
     * @param id identificador del estudiante
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cuenta los estudiantes activos de una categoría mediante procedimiento
     * almacenado.
     *
     * @param idCategoria identificador de la categoría
     * @return {@code 200 OK} con el conteo
     */
    @GetMapping("/conteo/categoria/{idCategoria}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Long> contarActivos(@PathVariable Long idCategoria) {
        return ResponseEntity.ok(estudianteService.contarActivosPorCategoria(idCategoria));
    }

    /**
     * Desactiva en bloque a todos los estudiantes activos de una categoría,
     * mediante procedimiento almacenado.
     *
     * @param idCategoria identificador de la categoría (cuerpo de la petición)
     * @return {@code 200 OK} sin cuerpo
     */
    @PostMapping("/operaciones/desactivar-categoria")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desactivarPorCategoria(@RequestBody Long idCategoria) {
        estudianteService.desactivarPorCategoria(idCategoria);
        return ResponseEntity.ok().build();
    }

    /**
     * Reactiva un estudiante dado de baja.
     *
     * @param id identificador del estudiante
     * @return {@code 200 OK} con el estudiante reactivado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si ya está activo ({@code 422})
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.reactivar(id));
    }

    /**
     * Propone el siguiente {@code codigo_estudiante} del año. No lo reserva:
     * el alta sigue validando unicidad.
     *
     * @param anio año para el que se genera el código
     * @return {@code 200 OK} con el código propuesto
     */
    @GetMapping("/operaciones/siguiente-codigo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<String> siguienteCodigo(@RequestParam int anio) {
        return ResponseEntity.ok(estudianteService.generarSiguienteCodigo(anio));
    }

    /**
     * Contacto rápido del representante del estudiante, para un entrenador
     * ante una emergencia o lesión.
     *
     * @param id identificador del estudiante
     * @return {@code 200 OK} con el texto del contacto
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante no existe ({@code 404})
     */
    @GetMapping("/{id}/contacto-emergencia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<String> contactoEmergencia(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.contactoDeEmergencia(id));
    }

    /**
     * Habilita el acceso propio de un estudiante que ya existe (rol
     * {@code ESTUDIANTE}), para que pueda marcar su asistencia por QR. No
     * crea una persona nueva: usa la que el estudiante ya tiene.
     *
     * @param id      identificador del estudiante
     * @param request credenciales de la cuenta a crear; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con el estudiante y su acceso habilitado
     * @throws org.uteq.backend.common.exception.RecursoNoEncontradoException
     *         si el estudiante no existe ({@code 404})
     * @throws IllegalArgumentException si el {@code username} ya existe o la
     *         persona tiene una cuenta de otro rol ({@code 422})
     */
    @PostMapping("/{id}/acceso")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EstudianteResponse> habilitarAcceso(
            @PathVariable Long id, @Valid @RequestBody HabilitarAccesoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteService.habilitarAcceso(id, request));
    }
}
