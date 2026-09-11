package org.uteq.backend.academico.student.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.uteq.backend.academico.student.dto.UpdatePositionRequest;
import org.uteq.backend.academico.student.dto.StudentPageResponse;
import org.uteq.backend.academico.student.dto.StudentRequest;
import org.uteq.backend.academico.student.dto.StudentResponse;
import org.uteq.backend.academico.student.dto.EnableAccessRequest;
import org.uteq.backend.academico.student.service.StudentService;

/**
 * CRUD de {@code Student} con paginación y baja lógica, más operaciones
 * de conjunto por categoría (conteo, desactivación) y la habilitación del
 * acceso propio del estudiante. Los endpoints de escritura quedan
 * restringidos a {@code ADMINISTRADOR} / {@code RECEPCIONISTA} vía
 * {@code @PreAuthorize}; la lectura la comparte también {@code ENTRENADOR}.
 */
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService estudianteService;

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
    public ResponseEntity<StudentPageResponse<StudentResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idEstudiante,asc") String[] sort,
            Authentication auth) {
        String campo = sort[0];
        Sort.Direction dir = sort.length > 1 && "desc".equalsIgnoreCase(sort[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(dir, campo));

        StudentPageResponse<StudentResponse> pagina = estudianteService.list(pageRequest);
        return ResponseEntity.ok(filterPhysicalData(pagina, auth));
    }

    /**
     * Busca un estudiante por su identificador.
     *
     * @param id identificador del estudiante
     * @param auth sesión autenticada; determina si el peso y la altura se
     *             incluyen en la respuesta (RF-11b: {@code RECEPCIONISTA} no los ve)
     * @return {@code 200 OK} con el estudiante
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<StudentResponse> findById(@PathVariable Long id, Authentication auth) {
        StudentResponse est = estudianteService.findById(id);
        return ResponseEntity.ok(puedeVerDatosFisicos(auth) ? est : est.withoutPhysicalData());
    }

    /**
     * RF-11b / hallazgo H-06: {@code peso} y {@code altura} (datos de salud de
     * un menor) solo los ven {@code ADMINISTRADOR} y {@code ENTRENADOR}. Para
     * {@code RECEPCIONISTA} se devuelven nulos.
     */
    private boolean puedeVerDatosFisicos(Authentication auth) {
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String rol = a.getAuthority();
            if ("ROLE_ADMINISTRADOR".equals(rol) || "ROLE_ENTRENADOR".equals(rol)) return true;
        }
        return false;
    }

    private StudentPageResponse<StudentResponse> filterPhysicalData(
            StudentPageResponse<StudentResponse> pagina, Authentication auth) {
        if (puedeVerDatosFisicos(auth)) return pagina;
        List<StudentResponse> filtrado = pagina.content().stream()
                .map(StudentResponse::withoutPhysicalData)
                .toList();
        return new StudentPageResponse<>(filtrado, pagina.page(), pagina.size(),
                pagina.totalElements(), pagina.totalPages());
    }

    /**
     * Registra un estudiante sobre una persona ya existente; si la persona
     * tenía una ficha inactiva, la reactiva.
     *
     * @param request datos del estudiante; validado con {@code @Valid}
     * @return {@code 201 Created} con el estudiante registrado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la persona, la categoría o el estado referidos no existen
     * @throws IllegalArgumentException si la persona ya tiene ficha activa o
     *         el código de estudiante está en uso ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<StudentResponse> create(
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteService.create(request));
    }

    /**
     * Actualiza la ficha completa de un estudiante.
     *
     * @param id      identificador del estudiante a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con el estudiante actualizado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante o alguna referencia no existen ({@code 404})
     * @throws IllegalArgumentException si el código nuevo pertenece a otro
     *         estudiante ({@code 422})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(estudianteService.update(id, request));
    }

    /**
     * Actualiza solo la posición nominal, no el resto de la ficha: a
     * diferencia de {@link #update}, esto también lo puede usar
     * {@code ENTRENADOR} desde evaluación diaria.
     *
     * @param id      identificador del estudiante
     * @param request cuerpo con {@code idPosicion} ({@code null} para quitarla)
     * @return {@code 200 OK} con el estudiante actualizado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante o la posición no existen ({@code 404})
     */
    @PutMapping("/{id}/posicion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<StudentResponse> updatePosition(
            @PathVariable Long id, @RequestBody UpdatePositionRequest request) {
        return ResponseEntity.ok(estudianteService.updatePosition(id, request.idPosicion()));
    }

    /**
     * Baja lógica de un estudiante ({@code activo = false}).
     *
     * @param id identificador del estudiante
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        estudianteService.delete(id);
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
    public ResponseEntity<Long> countActive(@PathVariable Long idCategoria) {
        return ResponseEntity.ok(estudianteService.countActiveByCategory(idCategoria));
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
    public ResponseEntity<Void> deactivateByCategory(@RequestBody Long idCategoria) {
        estudianteService.deactivateByCategory(idCategoria);
        return ResponseEntity.ok().build();
    }

    /**
     * Reactiva un estudiante dado de baja.
     *
     * @param id identificador del estudiante
     * @return {@code 200 OK} con el estudiante reactivado
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si ya está activo ({@code 422})
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<StudentResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.reactivate(id));
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
    public ResponseEntity<String> nextCode(@RequestParam int anio) {
        return ResponseEntity.ok(estudianteService.generateNextCode(anio));
    }

    /**
     * Contacto rápido del representante del estudiante, para un entrenador
     * ante una emergencia o lesión.
     *
     * @param id identificador del estudiante
     * @return {@code 200 OK} con el texto del contacto
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no existe ({@code 404})
     */
    @GetMapping("/{id}/contacto-emergencia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<String> emergencyContact(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.emergencyContact(id));
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
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no existe ({@code 404})
     * @throws IllegalArgumentException si el {@code username} ya existe o la
     *         persona tiene una cuenta de otro rol ({@code 422})
     */
    @PostMapping("/{id}/acceso")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<StudentResponse> enableAccess(
            @PathVariable Long id, @Valid @RequestBody EnableAccessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estudianteService.enableAccess(id, request));
    }

    /**
     * RF-50 / hallazgo H-03: anonimiza los datos del titular ante una
     * solicitud de supresión del representante legal. Sustituye los datos
     * identificativos de la persona por valores neutros y borra el texto
     * libre escrito sobre el menor, conservando las claves foráneas y las
     * estadísticas agregadas. Solo {@code ADMINISTRADOR}; el acto queda
     * registrado en la bitácora de auditoría.
     *
     * @param id identificador del estudiante a anonimizar
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si el estudiante no existe ({@code 404})
     */
    @PostMapping("/{id}/anonimizar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> anonymize(@PathVariable Long id) {
        estudianteService.anonymize(id);
        return ResponseEntity.noContent().build();
    }
}
