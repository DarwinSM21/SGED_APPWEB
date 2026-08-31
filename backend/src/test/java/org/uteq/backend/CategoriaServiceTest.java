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
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.dto.CategoriaRequest;
import org.uteq.backend.deportivo.categoria.dto.CategoriaResponse;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.categoria.service.CategoriaService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {
    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoriaSub12() {
        return Categoria.builder()
                .idCategoria(1L)
                .nombre("Sub-12")
                .edadMin((short) 10)
                .edadMax((short) 12)
                .descripcion("Categoria formativa")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("listarPaginado delega en el repositorio y mapea a CategoriaResponse")
    void listarPaginado_devuelve_pagina_mapeada() {
        Page<Categoria> pagina = new PageImpl<>(List.of(categoriaSub12()), PageRequest.of(0, 10), 1);
        when(categoriaRepository.findByActivoTrue(any())).thenReturn(pagina);

        Page<CategoriaResponse> resultado = categoriaService.listarPaginado(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Sub-12");
    }

    @Test
    @DisplayName("listarTodasActivas devuelve solo las categorias activas")
    void listarTodasActivas_devuelve_lista() {
        when(categoriaRepository.findByActivoTrue()).thenReturn(List.of(categoriaSub12()));

        List<CategoriaResponse> resultado = categoriaService.listarTodasActivas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).edadMin()).isEqualTo((short) 10);
    }

    @Test
    @DisplayName("buscarPorId devuelve la categoria cuando existe")
    void buscarPorId_existente() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaSub12()));

        CategoriaResponse resultado = categoriaService.buscarPorId(1L);

        assertThat(resultado.idCategoria()).isEqualTo(1L);
        assertThat(resultado.nombre()).isEqualTo("Sub-12");
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("crear persiste la categoria cuando las edades son validas")
    void crear_persiste_categoria_valida() {
        CategoriaRequest request = new CategoriaRequest("Sub-15", (short) 13, (short) 15, "Formativa");
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setIdCategoria(2L);
            return c;
        });

        CategoriaResponse resultado = categoriaService.crear(request);

        assertThat(resultado.idCategoria()).isEqualTo(2L);

        assertThat(resultado.nombre()).isEqualTo("SUB-15");
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("crear rechaza cuando la edad maxima no es mayor a la minima")
    void crear_con_edad_maxima_invalida_lanza_excepcion() {
        CategoriaRequest request = new CategoriaRequest("Sub-X", (short) 15, (short) 10, null);

        assertThatThrownBy(() -> categoriaService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("edad máxima");

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar actualiza los campos de una categoria existente")
    void editar_actualiza_categoria() {
        Categoria existente = categoriaSub12();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaRequest request = new CategoriaRequest("Sub-13", (short) 10, (short) 13, "actualizada");
        CategoriaResponse resultado = categoriaService.editar(1L, request);

        assertThat(resultado.nombre()).isEqualTo("SUB-13");
        assertThat(resultado.descripcion()).isEqualTo("actualizada");
    }

    @Test
    @DisplayName("eliminar hace baja logica en vez de borrar el registro")
    void eliminar_hace_baja_logica() {
        Categoria existente = categoriaSub12();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        categoriaService.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
        verify(categoriaRepository).save(existente);
    }

    @Test
    @DisplayName("crear rechaza un nombre que ya existe, sin importar mayusculas")
    void crear_rechaza_nombre_duplicado() {
        when(categoriaRepository.existsByNombreIgnoreCase("SUB-12")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crear(
                new CategoriaRequest("sub-12", (short) 10, (short) 12, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una categoría");

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar deja renombrar sin chocar consigo misma")
    void editar_no_se_considera_duplicado_de_si_misma() {
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot("SUB-12", 1L))
                .thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaSub12()));
        when(categoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CategoriaResponse r = categoriaService.editar(
                1L, new CategoriaRequest("Sub-12", (short) 10, (short) 13, "ajuste"));

        assertThat(r.edadMax()).isEqualTo((short) 13);
    }

    @Test
    @DisplayName("editar rechaza tomar el nombre de otra categoria")
    void editar_rechaza_nombre_de_otra() {
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot("SUB-14", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> categoriaService.editar(
                1L, new CategoriaRequest("Sub-14", (short) 10, (short) 12, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe otra categoría");

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("reactivar vuelve a poner activo en true")
    void reactivar_revierte_la_baja_logica() {
        Categoria dadaDeBaja = categoriaSub12();
        dadaDeBaja.setActivo(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(dadaDeBaja));
        when(categoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CategoriaResponse r = categoriaService.reactivar(1L);

        assertThat(r.activo()).isTrue();
    }
}
