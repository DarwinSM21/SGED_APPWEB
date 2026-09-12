package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.category.dto.CategoryRequest;
import org.uteq.backend.deportivo.category.dto.CategoryResponse;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.category.repository.CategoryRepository;
import org.uteq.backend.deportivo.category.service.CategoryService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category categoriaSub12() {
        return Category.builder()
                .idCategoria(1L)
                .nombre("Sub-12")
                .edadMin((short) 10)
                .edadMax((short) 12)
                .descripcion("Category formativa")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("listarPaginado delega en el repositorio y mapea a CategoryResponse")
    void listarPaginado_devuelve_pagina_mapeada() {
        Page<Category> pagina = new PageImpl<>(List.of(categoriaSub12()), PageRequest.of(0, 10), 1);
        when(categoryRepository.findActiveTrue(any())).thenReturn(pagina);

        Page<CategoryResponse> resultado = categoryService.findPaged(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Sub-12");
    }

    @Test
    @DisplayName("listarTodasActivas devuelve solo las categorias activas")
    void listarTodasActivas_devuelve_lista() {
        when(categoryRepository.findActiveTrue()).thenReturn(List.of(categoriaSub12()));

        List<CategoryResponse> resultado = categoryService.findAllActive();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).edadMin()).isEqualTo((short) 10);
    }

    @Test
    @DisplayName("buscarPorId devuelve la categoria cuando existe")
    void buscarPorId_existente() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoriaSub12()));

        CategoryResponse resultado = categoryService.findById(1L);

        assertThat(resultado.idCategoria()).isEqualTo(1L);
        assertThat(resultado.nombre()).isEqualTo("Sub-12");
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crear persiste la categoria cuando las edades son validas")
    void crear_persiste_categoria_valida() {
        CategoryRequest request = new CategoryRequest("Sub-15", (short) 13, (short) 15, "Formativa");
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setIdCategoria(2L);
            return c;
        });

        CategoryResponse resultado = categoryService.create(request);

        assertThat(resultado.idCategoria()).isEqualTo(2L);

        assertThat(resultado.nombre()).isEqualTo("SUB-15");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("crear rechaza cuando la edad maxima no es mayor a la minima")
    void crear_con_edad_maxima_invalida_lanza_excepcion() {
        CategoryRequest request = new CategoryRequest("Sub-X", (short) 15, (short) 10, null);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("edad máxima");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar actualiza los campos de una categoria existente")
    void editar_actualiza_categoria() {
        Category existente = categoriaSub12();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest request = new CategoryRequest("Sub-13", (short) 10, (short) 13, "actualizada");
        CategoryResponse resultado = categoryService.update(1L, request);

        assertThat(resultado.nombre()).isEqualTo("SUB-13");
        assertThat(resultado.descripcion()).isEqualTo("actualizada");
    }

    @Test
    @DisplayName("eliminar hace baja logica en vez de borrar el registro")
    void eliminar_hace_baja_logica() {
        Category existente = categoriaSub12();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.delete(1L);

        assertThat(existente.getActivo()).isFalse();
        verify(categoryRepository).save(existente);
    }

    @Test
    @DisplayName("crear rechaza un nombre que ya existe, sin importar mayusculas")
    void crear_rechaza_nombre_duplicado() {
        when(categoryRepository.existsByNameIgnoreCase("SUB-12")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(
                new CategoryRequest("sub-12", (short) 10, (short) 12, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una categoría");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar deja renombrar sin chocar consigo misma")
    void editar_no_se_considera_duplicado_de_si_misma() {
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("SUB-12", 1L))
                .thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoriaSub12()));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CategoryResponse r = categoryService.update(
                1L, new CategoryRequest("Sub-12", (short) 10, (short) 13, "ajuste"));

        assertThat(r.edadMax()).isEqualTo((short) 13);
    }

    @Test
    @DisplayName("editar rechaza tomar el nombre de otra categoria")
    void editar_rechaza_nombre_de_otra() {
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("SUB-14", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(
                1L, new CategoryRequest("Sub-14", (short) 10, (short) 12, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe otra categoría");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("reactivar vuelve a poner activo en true")
    void reactivar_revierte_la_baja_logica() {
        Category dadaDeBaja = categoriaSub12();
        dadaDeBaja.setActivo(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(dadaDeBaja));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CategoryResponse r = categoryService.reactivate(1L);

        assertThat(r.activo()).isTrue();
    }
}
