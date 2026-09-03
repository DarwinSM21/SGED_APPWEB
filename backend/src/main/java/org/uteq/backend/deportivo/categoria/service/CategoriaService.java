package org.uteq.backend.deportivo.categoria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.dto.CategoriaRequest;
import org.uteq.backend.deportivo.categoria.dto.CategoriaResponse;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;

import java.util.List;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;

/**
 * Lógica de negocio del catálogo de categorías (grupos etarios). El nombre
 * se guarda siempre en la forma canónica {@code SUB-<edad>}; la validación
 * del DTO acepta variantes al teclear y aquí se unifican, para que el
 * catálogo no termine con tres filas que son la misma categoría.
 */
@Service
@RequiredArgsConstructor
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;

    /**
     * Lista paginada de categorías activas.
     *
     * @param pageable paginación y orden
     * @return la página solicitada, mapeada a {@link CategoriaResponse}
     */
    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listarPaginado(Pageable pageable) {
        return categoriaRepository.findByActivoTrue(pageable)
                .map(this::toResponse);
    }

    /**
     * Lista completa de categorías activas, sin paginar.
     *
     * @return todas las categorías activas
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodasActivas() {
        return categoriaRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca una categoría por su identificador.
     *
     * @param id identificador de la categoría
     * @return la categoría encontrada
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
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
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = normalizarNombre(request.nombre());
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría llamada \"" + nombre + "\"");
        }
        validarEdades(request.edadMin(), request.edadMax());

        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .edadMin(request.edadMin())
                .edadMax(request.edadMax())
                .descripcion(request.descripcion())
                .activo(true)
                .build();

        return toResponse(categoriaRepository.save(categoria));
    }

    /**
     * Actualiza una categoría.
     *
     * @param id      identificador de la categoría a editar
     * @param request datos nuevos
     * @return la categoría actualizada
     * @throws RecursoNoEncontradoException si no existe
     * @throws IllegalArgumentException     si el nombre pertenece a otra
     *                                      categoría o el rango es inválido
     */
    @Transactional
    public CategoriaResponse editar(Long id, CategoriaRequest request) {
        String nombre = normalizarNombre(request.nombre());
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(nombre, id)) {
            throw new IllegalArgumentException(
                    "Ya existe otra categoría llamada \"" + nombre + "\"");
        }
        validarEdades(request.edadMin(), request.edadMax());

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));

        categoria.setNombre(nombre);
        categoria.setEdadMin(request.edadMin());
        categoria.setEdadMax(request.edadMax());
        categoria.setDescripcion(request.descripcion());

        return toResponse(categoriaRepository.save(categoria));
    }

    /**
     * Vuelve a poner una categoría en circulación. Es un método aparte y no
     * un efecto de {@link #editar}: reactivar es una decisión explícita, y si
     * editar el nombre reviviera de paso una categoría dada de baja sería un
     * cambio de estado que nadie pidió.
     *
     * @param id identificador de la categoría
     * @return la categoría reactivada
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional
    public CategoriaResponse reactivar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        categoria.setActivo(true);
        return toResponse(categoriaRepository.save(categoria));
    }

    /**
     * Baja lógica de una categoría ({@code activo = false}).
     *
     * @param id identificador de la categoría
     * @throws RecursoNoEncontradoException si no existe
     */
    @Auditado(accion = "ELIMINAR", entidad = "Categoria", idSpel = "#p0",
            descripcionSpel = "'desactivo la categoria #' + #p0")
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    // Deja el nombre en la forma canónica SUB-<edad>: el problema real no es
    // cómo se teclea sino que el catálogo termine con tres filas distintas
    // que son la misma categoría.
    private String normalizarNombre(String nombre) {
        String digitos = nombre.replaceAll("\\D+", "");
        return "SUB-" + digitos;
    }

    private void validarEdades(Short edadMin, Short edadMax) {
        if (edadMin == null || edadMax == null) {
            throw new IllegalArgumentException("Las edades mínima y máxima son obligatorias");
        }

        if (edadMax <= edadMin) {
            throw new IllegalArgumentException("La edad máxima debe ser mayor a la edad mínima");
        }
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(
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
