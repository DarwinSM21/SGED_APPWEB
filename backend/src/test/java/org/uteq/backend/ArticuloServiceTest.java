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
import org.springframework.data.domain.Pageable;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.ArticuloRequest;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.ArticuloResponse;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.StockBajoResponse;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.inventario.articulo.entity.Articulo.TipoArticulo;
import org.uteq.backend.inventario.articulo.repository.ArticuloRepository;
import org.uteq.backend.inventario.articulo.service.ArticuloService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticuloServiceTest {

    @Mock
    private ArticuloRepository articuloRepository;

    @InjectMocks
    private ArticuloService articuloService;

    private Articulo balonExistente() {
        return Articulo.builder()
                .idArticulo(1L)
                .nombre("Balón N°5")
                .tipo(TipoArticulo.BALON)
                .stockActual(10)
                .stockMinimo(3)
                .unidadMedida("unidad")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("crear inicia el articulo con stock_actual en cero, sin importar lo que pida el request")
    void crear_inicia_stock_en_cero() {
        ArticuloRequest request = new ArticuloRequest("Uniforme Sub-12", TipoArticulo.UNIFORME, "M", null, 5, null);
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> {
            Articulo a = inv.getArgument(0);
            a.setIdArticulo(2L);
            return a;
        });

        ArticuloResponse resultado = articuloService.crear(request);

        assertThat(resultado.stockActual()).isZero();
        assertThat(resultado.stockMinimo()).isEqualTo(5);
        assertThat(resultado.unidadMedida()).isEqualTo("unidad");
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(articuloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articuloService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("editar no modifica el stock_actual, solo el catalogo")
    void editar_no_toca_stock_actual() {
        Articulo existente = balonExistente();
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));

        ArticuloRequest request = new ArticuloRequest("Balón N°5 (renombrado)", TipoArticulo.BALON, null, null, 4, "unidad");
        ArticuloResponse resultado = articuloService.editar(1L, request);

        assertThat(resultado.nombre()).isEqualTo("Balón N°5 (renombrado)");
        assertThat(resultado.stockMinimo()).isEqualTo(4);
        assertThat(resultado.stockActual()).isEqualTo(10);
    }

    @Test
    @DisplayName("eliminar hace baja logica en vez de borrar el registro")
    void eliminar_hace_baja_logica() {
        Articulo existente = balonExistente();
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));

        articuloService.eliminar(1L);

        assertThat(existente.getActivo()).isFalse();
    }

    @Test
    @DisplayName("listarPaginado delega en el repositorio y mapea a ArticuloResponse")
    void listarPaginado_devuelve_pagina_mapeada() {
        Page<Articulo> pagina = new PageImpl<>(List.of(balonExistente()), PageRequest.of(0, 10), 1);
        when(articuloRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        Page<ArticuloResponse> resultado = articuloService.listarPaginado(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Balón N°5");
    }

    @Test
    @DisplayName("stockBajo combina el listado JPA con el total del procedimiento almacenado")
    void stockBajo_combina_listado_y_total_del_procedimiento() {
        Articulo bajoStock = balonExistente();
        bajoStock.setStockActual(2);
        when(articuloRepository.findConStockBajo()).thenReturn(List.of(bajoStock));
        when(articuloRepository.contarStockBajo()).thenReturn(1L);

        StockBajoResponse resultado = articuloService.stockBajo();

        assertThat(resultado.total()).isEqualTo(1L);
        assertThat(resultado.articulos()).hasSize(1);
        assertThat(resultado.articulos().get(0).stockActual()).isEqualTo(2);
    }

    @Test
    @DisplayName("stockBajo reporta cero cuando el procedimiento almacenado devuelve null")
    void stockBajo_total_null_se_reporta_como_cero() {
        when(articuloRepository.findConStockBajo()).thenReturn(List.of());
        when(articuloRepository.contarStockBajo()).thenReturn(null);

        StockBajoResponse resultado = articuloService.stockBajo();

        assertThat(resultado.total()).isZero();
        assertThat(resultado.articulos()).isEmpty();
    }

    @Test
    @DisplayName("crear respeta la unidad de medida cuando el request si la especifica")
    void crear_respeta_unidad_de_medida_explicita() {
        ArticuloRequest request = new ArticuloRequest("Balón N°5", TipoArticulo.BALON, null, null, 3, "caja");
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));

        ArticuloResponse resultado = articuloService.crear(request);

        assertThat(resultado.unidadMedida()).isEqualTo("caja");
    }

    @Test
    @DisplayName("editar con unidad de medida en blanco conserva la unidad que ya tenia el articulo")
    void editar_unidad_en_blanco_no_sobrescribe() {
        Articulo existente = balonExistente(); // unidadMedida = "unidad"
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));

        ArticuloRequest request = new ArticuloRequest("Balón N°5", TipoArticulo.BALON, null, null, 4, "   ");
        ArticuloResponse resultado = articuloService.editar(1L, request);

        assertThat(resultado.unidadMedida()).isEqualTo("unidad");
    }

    @Test
    @DisplayName("reactivar vuelve a activar un articulo dado de baja")
    void reactivar_reactiva_un_articulo_inactivo() {
        Articulo existente = balonExistente();
        existente.setActivo(false);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Articulo.class))).thenAnswer(inv -> inv.getArgument(0));

        ArticuloResponse resultado = articuloService.reactivar(1L);

        assertThat(resultado.activo()).isTrue();
    }

    @Test
    @DisplayName("reactivar rechaza un articulo que ya esta activo")
    void reactivar_rechaza_un_articulo_ya_activo() {
        Articulo existente = balonExistente(); // activo = true
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> articuloService.reactivar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra activo");
    }
}
