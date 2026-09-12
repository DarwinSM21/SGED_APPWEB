package org.uteq.backend.deportivo.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.category.dto.CategoryRequest;
import org.uteq.backend.deportivo.category.dto.CategoryResponse;
import org.uteq.backend.deportivo.category.service.CategoryService;

import java.util.List;

/**
 * CRUD del catálogo de categorías. La lectura la necesitan todos los roles
 * (el formulario de estudiante ofrece las categorías activas); la escritura
 * altera un catálogo del que dependen los estudiantes por clave foránea y
 * queda restringida a {@code ADMINISTRADOR}.
 */
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * Lista paginada de categorías activas.
     *
     * @param pageable paginación y orden
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Page<CategoryResponse>> findPaged(Pageable pageable) {
        return ResponseEntity.ok(categoryService.findPaged(pageable));
    }

    /**
     * Lista completa de categorías activas, sin paginar (para desplegables).
     *
     * @return {@code 200 OK} con todas las categorías activas
     */
    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<CategoryResponse>> findActive() {
        return ResponseEntity.ok(categoryService.findAllActive());
    }

    /**
     * Busca una categoría por su identificador.
     *
     * @param id identificador de la categoría
     * @return {@code 200 OK} con la categoría
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    /**
     * Crea una categoría (el nombre se normaliza a {@code SUB-<edad>}).
     *
     * @param request nombre, rango de edad y descripción; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con la categoría creada
     * @throws IllegalArgumentException si el nombre ya existe o el rango de
     *                                  edad es inválido ({@code 422})
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    /**
     * Actualiza una categoría.
     *
     * @param id      identificador de la categoría a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con la categoría actualizada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si el nombre pertenece a otra
     *                                  categoría o el rango de edad es
     *                                  inválido ({@code 422})
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    /**
     * Deshace una baja: vuelve a poner una categoría en circulación. Es un
     * endpoint aparte porque reactivar es una decisión explícita.
     *
     * @param id identificador de la categoría
     * @return {@code 200 OK} con la categoría reactivada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoryResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.reactivate(id));
    }

    /**
     * Baja lógica de una categoría.
     *
     * @param id identificador de la categoría
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
