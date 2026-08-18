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

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listarPaginado(Pageable pageable) {
        return categoriaRepository.findByActivoTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodasActivas() {
        return categoriaRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        return toResponse(c);
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría llamada \"" + request.nombre() + "\"");
        }
        validarEdades(request.edadMin(), request.edadMax());

        Categoria categoria = Categoria.builder()
                .nombre(request.nombre())
                .edadMin(request.edadMin())
                .edadMax(request.edadMax())
                .descripcion(request.descripcion())
                .activo(true)
                .build();

        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse editar(Long id, CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(request.nombre(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe otra categoría llamada \"" + request.nombre() + "\"");
        }
        validarEdades(request.edadMin(), request.edadMax());

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));

        categoria.setNombre(request.nombre());
        categoria.setEdadMin(request.edadMin());
        categoria.setEdadMax(request.edadMax());
        categoria.setDescripcion(request.descripcion());

        return toResponse(categoriaRepository.save(categoria));
    }

    /**
     * Vuelve a poner una categoria en circulacion. Es un metodo aparte y no
     * un efecto de editar(): reactivar es una decision explicita, y si
     * editar el nombre reviviera de paso una categoria dada de baja, seria
     * un cambio de estado que nadie pidio.
     */
    @Transactional
    public CategoriaResponse reactivar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        categoria.setActivo(true);
        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
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