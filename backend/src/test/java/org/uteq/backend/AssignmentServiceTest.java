package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.inventario.item.entity.Item;
import org.uteq.backend.inventario.item.entity.Item.ItemType;
import org.uteq.backend.inventario.item.repository.ItemRepository;
import org.uteq.backend.inventario.assignment.dto.AssignmentDtos.AssignmentRequest;
import org.uteq.backend.inventario.assignment.dto.AssignmentDtos.AssignmentResponse;
import org.uteq.backend.inventario.assignment.dto.AssignmentDtos.ReturnRequest;
import org.uteq.backend.inventario.assignment.entity.Assignment;
import org.uteq.backend.inventario.assignment.entity.Assignment.AssignmentStatus;
import org.uteq.backend.inventario.assignment.entity.Assignment.RecipientType;
import org.uteq.backend.inventario.assignment.repository.AssignmentRepository;
import org.uteq.backend.inventario.assignment.service.AssignmentService;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock private AssignmentRepository asignacionRepository;
    @Mock private ItemRepository articuloRepository;
    @Mock private StudentRepository estudianteRepository;
    @Mock private EntrenadorRepository entrenadorRepository;
    @Mock private UserAccountRepository usuarioRepository;

    @InjectMocks
    private AssignmentService asignacionService;

    private Item uniformeConStock(int stockActual) {
        return Item.builder()
                .idArticulo(1L)
                .nombre("Uniforme Sub-12")
                .tipo(ItemType.UNIFORME)
                .stockActual(stockActual)
                .stockMinimo(2)
                .activo(true)
                .build();
    }

    private Student estudiante() {
        Person persona = Person.builder().nombre("Juan").apellido("Perez").build();
        return Student.builder().idEstudiante(5L).persona(persona).build();
    }

    private Entrenador entrenador() {
        Person persona = Person.builder().nombre("Carlos").apellido("Ruiz").build();
        return Entrenador.builder().idEntrenador(7L).persona(persona).build();
    }

    private UserAccount registrador() {
        Person persona = Person.builder().nombre("Ana").apellido("Diaz").build();
        return UserAccount.builder().idUsuario(9L).username("recepcion").persona(persona).build();
    }

    private void stubGuardarAsignacion() {
        when(usuarioRepository.findByUsername("recepcion")).thenReturn(Optional.of(registrador()));
        when(asignacionRepository.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setIdAsignacion(100L);
            return a;
        });
    }

    @Test
    @DisplayName("crear una asignacion a estudiante resta del stock_actual del articulo")
    void crear_asignacion_a_estudiante_resta_stock() {
        Item articulo = uniformeConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante()));
        stubGuardarAsignacion();

        AssignmentRequest request = new AssignmentRequest(1L, 1, RecipientType.ESTUDIANTE, 5L, null, null, null);
        AssignmentResponse resultado = asignacionService.create(request, "recepcion");

        assertThat(articulo.getStockActual()).isEqualTo(9);
        assertThat(resultado.estado()).isEqualTo(AssignmentStatus.ASIGNADO);
        assertThat(resultado.estudiante()).isEqualTo("Juan Perez");
    }

    @Test
    @DisplayName("crear rechaza cuando la cantidad pedida supera el stock disponible")
    void crear_con_stock_insuficiente_lanza_excepcion() {
        Item articulo = uniformeConStock(1);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));

        AssignmentRequest request = new AssignmentRequest(1L, 3, RecipientType.ESTUDIANTE, 5L, null, null, null);

        assertThatThrownBy(() -> asignacionService.create(request, "recepcion"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(articulo.getStockActual()).isEqualTo(1);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear rechaza ESTUDIANTE sin idEstudiante, sin tocar el stock")
    void crear_estudiante_sin_id_lanza_excepcion() {
        AssignmentRequest request = new AssignmentRequest(1L, 1, RecipientType.ESTUDIANTE, null, null, null, null);

        assertThatThrownBy(() -> asignacionService.create(request, "recepcion"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(articuloRepository);
    }

    @Test
    @DisplayName("crear una asignacion a entrenador resuelve el entrenador, no el estudiante")
    void crear_asignacion_a_entrenador() {
        Item articulo = uniformeConStock(10);
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(entrenadorRepository.findById(7L)).thenReturn(Optional.of(entrenador()));
        stubGuardarAsignacion();

        AssignmentRequest request = new AssignmentRequest(1L, 2, RecipientType.ENTRENADOR, null, 7L, null, null);
        AssignmentResponse resultado = asignacionService.create(request, "recepcion");

        assertThat(resultado.entrenador()).isEqualTo("Carlos Ruiz");
        assertThat(resultado.estudiante()).isNull();
        verifyNoInteractions(estudianteRepository);
    }

    private Assignment asignacionActiva() {
        return Assignment.builder()
                .idAsignacion(100L)
                .articulo(uniformeConStock(9))
                .cantidad(1)
                .tipoDestinatario(RecipientType.ESTUDIANTE)
                .estudiante(estudiante())
                .estado(AssignmentStatus.ASIGNADO)
                .registradoPor(registrador())
                .build();
    }

    @Test
    @DisplayName("devolver con estado DEVUELTO repone el stock_actual del articulo")
    void devolver_con_devuelto_repone_stock() {
        Assignment asignacion = asignacionActiva();
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        ReturnRequest request = new ReturnRequest(AssignmentStatus.DEVUELTO, "en buen estado");
        AssignmentResponse resultado = asignacionService.registerReturn(100L, request);

        assertThat(resultado.estado()).isEqualTo(AssignmentStatus.DEVUELTO);
        assertThat(asignacion.getArticulo().getStockActual()).isEqualTo(10);
        assertThat(resultado.fechaDevolucionReal()).isNotNull();
    }

    @Test
    @DisplayName("devolver con estado PERDIDO no repone el stock_actual del articulo")
    void devolver_con_perdido_no_repone_stock() {
        Assignment asignacion = asignacionActiva();
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        ReturnRequest request = new ReturnRequest(AssignmentStatus.PERDIDO, "no se recupero");
        AssignmentResponse resultado = asignacionService.registerReturn(100L, request);

        assertThat(resultado.estado()).isEqualTo(AssignmentStatus.PERDIDO);
        assertThat(asignacion.getArticulo().getStockActual()).isEqualTo(9);
    }

    @Test
    @DisplayName("devolver una asignacion ya resuelta lanza excepcion")
    void devolver_asignacion_ya_resuelta_lanza_excepcion() {
        Assignment asignacion = asignacionActiva();
        asignacion.setEstado(AssignmentStatus.DEVUELTO);
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacion));

        ReturnRequest request = new ReturnRequest(AssignmentStatus.PERDIDO, null);

        assertThatThrownBy(() -> asignacionService.registerReturn(100L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya fue resuelta");

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("devolver con estado ASIGNADO es invalido: no es una transicion de devolucion")
    void devolver_con_estado_asignado_lanza_excepcion() {
        ReturnRequest request = new ReturnRequest(AssignmentStatus.ASIGNADO, null);

        assertThatThrownBy(() -> asignacionService.registerReturn(100L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(asignacionRepository);
    }

    @Test
    @DisplayName("devolver una asignacion inexistente lanza ResourceNotFoundException")
    void devolver_asignacion_inexistente_lanza_excepcion() {
        when(asignacionRepository.findById(404L)).thenReturn(Optional.empty());

        ReturnRequest request = new ReturnRequest(AssignmentStatus.DEVUELTO, null);

        assertThatThrownBy(() -> asignacionService.registerReturn(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
