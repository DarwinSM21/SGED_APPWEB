package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.inventario.articulo.entity.Articulo.TipoArticulo;
import org.uteq.backend.inventario.articulo.repository.ArticuloRepository;
import org.uteq.backend.inventario.movimiento.dto.MovimientoDtos.MovimientoRequest;
import org.uteq.backend.inventario.movimiento.dto.MovimientoDtos.MovimientoResponse;
import org.uteq.backend.inventario.movimiento.entity.MovimientoStock;
import org.uteq.backend.inventario.movimiento.entity.MovimientoStock.TipoMovimiento;
import org.uteq.backend.inventario.movimiento.repository.MovimientoStockRepository;
import org.uteq.backend.inventario.movimiento.service.MovimientoStockService;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceTest {

    @Mock private MovimientoStockRepository movimientoStockRepository;
    @Mock private ArticuloRepository articuloRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MovimientoStockService movimientoStockService;

    private Articulo articuloConStock(int stockActual) {
        return Articulo.builder()
                .idArticulo(1L)
                .nombre("Balón N°5")
                .tipo(TipoArticulo.BALON)
                .stockActual(stockActual)
                .stockMinimo(3)
                .activo(true)
                .build();
    }

    private Usuario registrador() {
        Persona persona = Persona.builder().nombre("Ana").apellido("Diaz").build();
        return Usuario.builder().idUsuario(9L).username("recepcion").persona(persona).build();
    }

    @Test
    @DisplayName("registrar ENTRADA suma al stock_actual del articulo")
    void registrar_entrada_suma_stock() {
        Articulo articulo = articuloConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> {
            MovimientoStock m = inv.getArgument(0);
            m.setIdMovimiento(1L);
            return m;
        });

        MovimientoRequest request = new MovimientoRequest(1L, TipoMovimiento.ENTRADA, 5, "compra");
        MovimientoResponse resultado = movimientoStockService.registrar(request, "recepcion");

        assertThat(articulo.getStockActual()).isEqualTo(15);
        assertThat(resultado.cantidad()).isEqualTo(5);
        verify(articuloRepository).save(articulo);
    }

    @Test
    @DisplayName("registrar AJUSTE suma al stock_actual, igual que ENTRADA")
    void registrar_ajuste_suma_stock() {
        Articulo articulo = articuloConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoRequest request = new MovimientoRequest(1L, TipoMovimiento.AJUSTE, 2, "conteo fisico");
        movimientoStockService.registrar(request, "recepcion");

        assertThat(articulo.getStockActual()).isEqualTo(12);
    }

    @Test
    @DisplayName("registrar SALIDA resta del stock_actual del articulo")
    void registrar_salida_resta_stock() {
        Articulo articulo = articuloConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoRequest request = new MovimientoRequest(1L, TipoMovimiento.SALIDA, 4, "desgaste");
        movimientoStockService.registrar(request, "recepcion");

        assertThat(articulo.getStockActual()).isEqualTo(6);
    }

    @Test
    @DisplayName("registrar SALIDA que dejaria el stock negativo se rechaza sin tocar la base de datos")
    void registrar_salida_excesiva_lanza_excepcion() {
        Articulo articulo = articuloConStock(3);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));

        MovimientoRequest request = new MovimientoRequest(1L, TipoMovimiento.SALIDA, 10, "extravio");

        assertThatThrownBy(() -> movimientoStockService.registrar(request, "recepcion"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(articulo.getStockActual()).isEqualTo(3);
        verify(articuloRepository, never()).save(any());
        verify(movimientoStockRepository, never()).save(any());
    }
}
