package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.estudiante.service.MiEquipoService;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.especialidad.entity.Especialidad;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MiEquipoServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;

    @InjectMocks private MiEquipoService servicio;

    private static final Long ID_CATEGORIA = 3L;

    private Categoria categoria() {
        return Categoria.builder()
                .idCategoria(ID_CATEGORIA).nombre("SUB-12")
                .edadMin((short) 10).edadMax((short) 12).descripcion("Sub 12 anios")
                .build();
    }

    private Estudiante estudiante(Long id, String nombre, Posicion posicion) {
        return Estudiante.builder()
                .idEstudiante(id)
                .persona(Persona.builder().nombre(nombre).apellido("Perez").build())
                .categoria(categoria())
                .posicion(posicion)
                .build();
    }

    @Test
    @DisplayName("miEquipo responde 404 si la cuenta no tiene fila de estudiante asociada")
    void sinEstudianteAsociado() {
        when(estudianteRepository.findByUsuario_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.miEquipo("huerfano@sged.test"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("la posicion viene null si el estudiante no tiene una asignada")
    void sinPosicionAsignada() {
        var yo = estudiante(1L, "Juan", null);
        when(estudianteRepository.findByUsuario_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.miEquipo("juan@sged.test");

        assertThat(equipo.posicion()).isNull();
    }

    @Test
    @DisplayName("el entrenador viene null si la categoria no tiene ninguna sesion futura")
    void sinSesionFutura() {
        var yo = estudiante(1L, "Juan", null);
        when(estudianteRepository.findByUsuario_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.miEquipo("juan@sged.test");

        assertThat(equipo.entrenador()).isNull();
    }

    @Test
    @DisplayName("companeros excluye al propio estudiante y trae nombre + posicion de los demas")
    void companerosExcluyeAlPropioEstudiante() {
        var posicionDelantero = Posicion.builder().nombre("Delantero").abreviatura("DC").build();
        var yo = estudiante(1L, "Juan", null);
        var companero = estudiante(2L, "Carlos", posicionDelantero);

        when(estudianteRepository.findByUsuario_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of(companero));

        var equipo = servicio.miEquipo("juan@sged.test");

        assertThat(equipo.companeros()).hasSize(1);
        assertThat(equipo.companeros().get(0).nombre()).isEqualTo("Carlos Perez");
        assertThat(equipo.companeros().get(0).posicion()).isEqualTo("Delantero");
    }

    @Test
    @DisplayName("el entrenador es el de la sesion futura mas proxima, con su especialidad")
    void entrenadorDeLaProximaSesion() {
        var yo = estudiante(1L, "Juan", null);
        var especialidad = Especialidad.builder().nombre("Tecnico").build();
        var entrenador = Entrenador.builder()
                .idEntrenador(9L)
                .persona(Persona.builder().nombre("Pedro").apellido("Gomez").build())
                .especialidad(especialidad)
                .build();
        var proximaSesion = SesionEntrenamiento.builder()
                .idSesion(50L).categoria(categoria()).entrenador(entrenador)
                .fecha(LocalDate.of(2026, 8, 20))
                .build();

        when(estudianteRepository.findByUsuario_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of(proximaSesion));
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.miEquipo("juan@sged.test");

        assertThat(equipo.entrenador()).isNotNull();
        assertThat(equipo.entrenador().nombre()).isEqualTo("Pedro Gomez");
        assertThat(equipo.entrenador().especialidad()).isEqualTo("Tecnico");
    }

    @Test
    @DisplayName("la categoria trae nombre, rango de edad y descripcion completos")
    void categoriaConDatosCompletos() {
        var yo = estudiante(1L, "Juan", null);
        when(estudianteRepository.findByUsuario_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.miEquipo("juan@sged.test");

        assertThat(equipo.categoria().nombre()).isEqualTo("SUB-12");
        assertThat(equipo.categoria().edadMin()).isEqualTo(10);
        assertThat(equipo.categoria().edadMax()).isEqualTo(12);
        assertThat(equipo.categoria().descripcion()).isEqualTo("Sub 12 anios");
    }
}
