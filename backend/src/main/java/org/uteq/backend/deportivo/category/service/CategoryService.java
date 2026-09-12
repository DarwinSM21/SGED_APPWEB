package org.uteq.backend.deportivo.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.category.dto.CategoryRequest;
import org.uteq.backend.deportivo.category.dto.CategoryResponse;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.category.repository.CategoryRepository;

import java.util.List;
import org.uteq.backend.seguridad.audit.aop.Audited;

/**
 * Lógica de negocio del catálogo de categorías (grupos etarios). El nombre
 * se guarda siempre en la forma canónica {@code SUB-<edad>}; la validación
 * del DTO acepta variantes al teclear y aquí se unifican, para que el
 * catálogo no termine con tres filas que son la misma categoría.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    /**
     * Lista paginada de categorías activas.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link CategoryResponse}
     */
    @Transactional(readOnly = true)
    public Page<CategoryResponse> findPaged(Pageable pageable) {
        return categoryRepository.findActiveTrue(pageable)
                .map(this::toResponse);
    }

    /**
     * Lista completa de categorías activas, sin paginar.
     *
     * @return todas las categorías activas
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllActive() {
        return categoryRepository.findActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca una categoría por su identificador.
     *
     * @param id identificador de la categoría
     * @return la categoría encontrada
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        return toResponse(c);
    }

    /**
     * Crea una categoría con nombre normalizado.
     *
     * @param request nombre, rango de edad y descripción
     * @return la categoría creada
     * @throws IllegalArgumentException si ya existe una categoría con ese
     *                                  nombre o el rango de edad es inválido
     */
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String nombre = normalizeName(request.nombre());
        if (categoryRepository.existsByNameIgnoreCase(nombre)) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría llamada \"" + nombre + "\"");
        }
        validateAges(request.edadMin(), request.edadMax());

        Category categoria = Category.builder()
                .nombre(nombre)
                .edadMin(request.edadMin())
                .edadMax(request.edadMax())
                .descripcion(request.descripcion())
                .activo(true)
                .build();

        return toResponse(categoryRepository.save(categoria));
    }

    /**
     * Actualiza una categoría.
     *
     * @param id      identificador de la categoría a editar
     * @param request datos nuevos
     * @return la categoría actualizada
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si el nombre pertenece a otra
     *                                      categoría o el rango es inválido
     */
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        String nombre = normalizeName(request.nombre());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(nombre, id)) {
            throw new IllegalArgumentException(
                    "Ya existe otra categoría llamada \"" + nombre + "\"");
        }
        validateAges(request.edadMin(), request.edadMax());

        Category categoria = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        categoria.setNombre(nombre);
        categoria.setEdadMin(request.edadMin());
        categoria.setEdadMax(request.edadMax());
        categoria.setDescripcion(request.descripcion());

        return toResponse(categoryRepository.save(categoria));
    }

    /**
     * Vuelve a poner una categoría en circulación. Es un método aparte y no
     * un efecto de {@link #update}: reactivar es una decisión explícita, y si
     * editar el nombre reviviera de paso una categoría dada de baja sería un
     * cambio de estado que nadie pidió.
     *
     * @param id identificador de la categoría
     * @return la categoría reactivada
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional
    public CategoryResponse reactivate(Long id) {
        Category categoria = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        categoria.setActivo(true);
        return toResponse(categoryRepository.save(categoria));
    }

    /**
     * Baja lógica de una categoría ({@code activo = false}).
     *
     * @param id identificador de la categoría
     * @throws ResourceNotFoundException si no existe
     */
    @Audited(action = "ELIMINAR", entity = "Category", idSpel = "#p0",
            descriptionSpel = "'desactivo la categoria #' + #p0")
    @Transactional
    public void delete(Long id) {
        Category categoria = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        categoria.setActivo(false);
        categoryRepository.save(categoria);
    }

    // Deja el nombre en la forma canónica SUB-<edad>: el problema real no es
    // cómo se teclea sino que el catálogo termine con tres filas distintas
    // que son la misma categoría.
    private String normalizeName(String nombre) {
        String digitos = nombre.replaceAll("\\D+", "");
        return "SUB-" + digitos;
    }

    private void validateAges(Short edadMin, Short edadMax) {
        if (edadMin == null || edadMax == null) {
            throw new IllegalArgumentException("Las edades mínima y máxima son obligatorias");
        }

        if (edadMax <= edadMin) {
            throw new IllegalArgumentException("La edad máxima debe ser mayor a la edad mínima");
        }
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getIdCategoria(),
                c.getNombre(),
                c.getEdadMin(),
                c.getEdadMax(),
                c.getDescripcion(),
                c.getActivo(),
                c.getCreatedAt()
        );
    }
}
