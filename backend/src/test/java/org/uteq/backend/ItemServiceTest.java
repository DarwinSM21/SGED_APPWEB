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
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.inventario.item.dto.ItemDtos.ItemRequest;
import org.uteq.backend.inventario.item.dto.ItemDtos.ItemResponse;
import org.uteq.backend.inventario.item.dto.ItemDtos.LowStockResponse;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.inventario.item.entity.Item.ItemType;
import org.uteq.backend.inventario.item.repository.ItemRepository;
import org.uteq.backend.inventario.item.service.ItemService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository articuloRepository;

    @InjectMocks
    private ItemService articuloService;

    private Item balonExistente() {
        return Item.builder()
                .id(1L)
                .name("Balón N°5")
                .type(ItemType.BALON)
                .currentStock(10)
                .minimumStock(3)
                .unitOfMeasure("unidad")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("crear inicia el articulo con stock_actual en cero, sin importar lo que pida el request")
    void crear_inicia_stock_en_cero() {
        ItemRequest request = new ItemRequest("Uniforme Sub-12", ItemType.UNIFORME, "M", null, 5, null);
        when(articuloRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        ItemResponse resultado = articuloService.create(request);

        assertThat(resultado.stockActual()).isZero();
        assertThat(resultado.stockMinimo()).isEqualTo(5);
        assertThat(resultado.unidadMedida()).isEqualTo("unidad");
    }

    @Test
    @DisplayName("buscarPorId lanza ResourceNotFoundException cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(articuloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articuloService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("editar no modifica el stock_actual, solo el catalogo")
    void editar_no_toca_stock_actual() {
        Item existente = balonExistente();
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemRequest request = new ItemRequest("Balón N°5 (renombrado)", ItemType.BALON, null, null, 4, "unidad");
        ItemResponse resultado = articuloService.update(1L, request);

        assertThat(resultado.nombre()).isEqualTo("Balón N°5 (renombrado)");
        assertThat(resultado.stockMinimo()).isEqualTo(4);
        assertThat(resultado.stockActual()).isEqualTo(10);
    }

    @Test
    @DisplayName("eliminar hace baja logica en vez de borrar el registro")
    void eliminar_hace_baja_logica() {
        Item existente = balonExistente();
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        articuloService.delete(1L);

        assertThat(existente.getActive()).isFalse();
    }

    @Test
    @DisplayName("listarPaginado delega en el repositorio y mapea a ItemResponse")
    void listarPaginado_devuelve_pagina_mapeada() {
        Page<Item> pagina = new PageImpl<>(List.of(balonExistente()), PageRequest.of(0, 10), 1);
        when(articuloRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        Page<ItemResponse> resultado = articuloService.listPaged(PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Balón N°5");
    }

    @Test
    @DisplayName("stockBajo combina el listado JPA con el total del procedimiento almacenado")
    void stockBajo_combina_listado_y_total_del_procedimiento() {
        Item bajoStock = balonExistente();
        bajoStock.setCurrentStock(2);
        when(articuloRepository.findLowStock()).thenReturn(List.of(bajoStock));
        when(articuloRepository.countLowStock()).thenReturn(1L);

        LowStockResponse resultado = articuloService.lowStock();

        assertThat(resultado.total()).isEqualTo(1L);
        assertThat(resultado.articulos()).hasSize(1);
        assertThat(resultado.articulos().get(0).stockActual()).isEqualTo(2);
    }

    @Test
    @DisplayName("stockBajo reporta cero cuando el procedimiento almacenado devuelve null")
    void stockBajo_total_null_se_reporta_como_cero() {
        when(articuloRepository.findLowStock()).thenReturn(List.of());
        when(articuloRepository.countLowStock()).thenReturn(null);

        LowStockResponse resultado = articuloService.lowStock();

        assertThat(resultado.total()).isZero();
        assertThat(resultado.articulos()).isEmpty();
    }

    @Test
    @DisplayName("crear respeta la unidad de medida cuando el request si la especifica")
    void crear_respeta_unidad_de_medida_explicita() {
        ItemRequest request = new ItemRequest("Balón N°5", ItemType.BALON, null, null, 3, "caja");
        when(articuloRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemResponse resultado = articuloService.create(request);

        assertThat(resultado.unidadMedida()).isEqualTo("caja");
    }

    @Test
    @DisplayName("editar con unidad de medida en blanco conserva la unidad que ya tenia el articulo")
    void editar_unidad_en_blanco_no_sobrescribe() {
        Item existente = balonExistente(); // unidadMedida = "unidad"
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemRequest request = new ItemRequest("Balón N°5", ItemType.BALON, null, null, 4, "   ");
        ItemResponse resultado = articuloService.update(1L, request);

        assertThat(resultado.unidadMedida()).isEqualTo("unidad");
    }

    @Test
    @DisplayName("reactivar vuelve a activar un articulo dado de baja")
    void reactivar_reactiva_un_articulo_inactivo() {
        Item existente = balonExistente();
        existente.setActive(false);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(articuloRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemResponse resultado = articuloService.reactivate(1L);

        assertThat(resultado.activo()).isTrue();
    }

    @Test
    @DisplayName("reactivar rechaza un articulo que ya esta activo")
    void reactivar_rechaza_un_articulo_ya_activo() {
        Item existente = balonExistente(); // activo = true
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> articuloService.reactivate(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya se encuentra activo");
    }
}
