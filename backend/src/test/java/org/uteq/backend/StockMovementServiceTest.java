package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.inventario.item.entity.Item.ItemType;
import org.uteq.backend.inventario.item.repository.ItemRepository;
import org.uteq.backend.inventario.movement.dto.StockMovementDtos.StockMovementRequest;
import org.uteq.backend.inventario.movement.dto.StockMovementDtos.StockMovementResponse;
import org.uteq.backend.inventario.movement.entity.StockMovement;
import org.uteq.backend.inventario.movement.entity.StockMovement.MovementType;
import org.uteq.backend.inventario.movement.repository.StockMovementRepository;
import org.uteq.backend.inventario.movement.service.StockMovementService;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock private StockMovementRepository movimientoStockRepository;
    @Mock private ItemRepository articuloRepository;
    @Mock private UserAccountRepository usuarioRepository;

    @InjectMocks
    private StockMovementService movimientoStockService;

    private Item articuloConStock(int stockActual) {
        return Item.builder()
                .id(1L)
                .name("Balón N°5")
                .type(ItemType.BALON)
                .currentStock(stockActual)
                .minimumStock(3)
                .active(true)
                .build();
    }

    private UserAccount registrador() {
        Person persona = Person.builder().name("Ana").lastName("Diaz").build();
        return UserAccount.builder().id(9L).username("recepcion").person(persona).build();
    }

    @Test
    @DisplayName("registrar ENTRADA suma al stock_actual del articulo")
    void registrar_entrada_suma_stock() {
        Item articulo = articuloConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(movimientoStockRepository.save(any(StockMovement.class))).thenAnswer(inv -> {
            StockMovement m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        StockMovementRequest request = new StockMovementRequest(1L, MovementType.ENTRADA, 5, "compra");
        StockMovementResponse resultado = movimientoStockService.register(request, "recepcion");

        assertThat(articulo.getCurrentStock()).isEqualTo(15);
        assertThat(resultado.cantidad()).isEqualTo(5);
        verify(articuloRepository).save(articulo);
    }

    @Test
    @DisplayName("registrar AJUSTE suma al stock_actual, igual que ENTRADA")
    void registrar_ajuste_suma_stock() {
        Item articulo = articuloConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(movimientoStockRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(1L, MovementType.AJUSTE, 2, "conteo fisico");
        movimientoStockService.register(request, "recepcion");

        assertThat(articulo.getCurrentStock()).isEqualTo(12);
    }

    @Test
    @DisplayName("registrar SALIDA resta del stock_actual del articulo")
    void registrar_salida_resta_stock() {
        Item articulo = articuloConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(movimientoStockRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(1L, MovementType.SALIDA, 4, "desgaste");
        movimientoStockService.register(request, "recepcion");

        assertThat(articulo.getCurrentStock()).isEqualTo(6);
    }

    @Test
    @DisplayName("registrar SALIDA que dejaria el stock negativo se rechaza sin tocar la base de datos")
    void registrar_salida_excesiva_lanza_excepcion() {
        Item articulo = articuloConStock(3);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));

        StockMovementRequest request = new StockMovementRequest(1L, MovementType.SALIDA, 10, "extravio");

        assertThatThrownBy(() -> movimientoStockService.register(request, "recepcion"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(articulo.getCurrentStock()).isEqualTo(3);
        verify(articuloRepository, never()).save(any());
        verify(movimientoStockRepository, never()).save(any());
    }
}
