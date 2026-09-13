package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.student.service.MyTeamService;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.specialty.entity.Specialty;
import org.uteq.backend.deportivo.position.entity.Position;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;
import org.uteq.backend.seguridad.person.entity.Person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyTeamServiceTest {

    @Mock private StudentRepository estudianteRepository;
    @Mock private TrainingSessionRepository sesionRepository;

    @InjectMocks private MyTeamService servicio;

    private static final Long ID_CATEGORIA = 3L;

    private Category categoria() {
        return Category.builder()
                .categoryId(ID_CATEGORIA).nombre("SUB-12")
                .edadMin((short) 10).edadMax((short) 12).descripcion("Sub 12 anios")
                .build();
    }

    private Student estudiante(Long id, String nombre, Position posicion) {
        return Student.builder()
                .id(id)
                .person(Person.builder().name(nombre).lastName("Perez").build())
                .category(categoria())
                .position(posicion)
                .build();
    }

    @Test
    @DisplayName("miEquipo responde 404 si la cuenta no tiene fila de estudiante asociada")
    void sinEstudianteAsociado() {
        when(estudianteRepository.findByUserAccount_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.myTeam("huerfano@sged.test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("la posicion viene null si el estudiante no tiene una asignada")
    void sinPosicionAsignada() {
        var yo = estudiante(1L, "Juan", null);
        when(estudianteRepository.findByUserAccount_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoryAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategory_CategoryIdAndActiveTrueAndIdNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.myTeam("juan@sged.test");

        assertThat(equipo.position()).isNull();
    }

    @Test
    @DisplayName("el entrenador viene null si la categoria no tiene ninguna sesion futura")
    void sinSesionFutura() {
        var yo = estudiante(1L, "Juan", null);
        when(estudianteRepository.findByUserAccount_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoryAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategory_CategoryIdAndActiveTrueAndIdNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.myTeam("juan@sged.test");

        assertThat(equipo.coach()).isNull();
    }

    @Test
    @DisplayName("companeros excluye al propio estudiante y trae nombre + posicion de los demas")
    void companerosExcluyeAlPropioEstudiante() {
        var posicionDelantero = Position.builder().nombre("Delantero").abreviatura("DC").build();
        var yo = estudiante(1L, "Juan", null);
        var companero = estudiante(2L, "Carlos", posicionDelantero);

        when(estudianteRepository.findByUserAccount_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoryAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategory_CategoryIdAndActiveTrueAndIdNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of(companero));

        var equipo = servicio.myTeam("juan@sged.test");

        assertThat(equipo.teammates()).hasSize(1);
        assertThat(equipo.teammates().get(0).name()).isEqualTo("Carlos Perez");
        assertThat(equipo.teammates().get(0).position()).isEqualTo("Delantero");
    }

    @Test
    @DisplayName("el entrenador es el de la sesion futura mas proxima, con su especialidad")
    void entrenadorDeLaProximaSesion() {
        var yo = estudiante(1L, "Juan", null);
        var especialidad = Specialty.builder().nombre("Tecnico").build();
        var entrenador = Coach.builder()
                .coachId(9L)
                .persona(Person.builder().name("Pedro").lastName("Gomez").build())
                .especialidad(especialidad)
                .build();
        var proximaSesion = TrainingSession.builder()
                .idSesion(50L).categoria(categoria()).entrenador(entrenador)
                .fecha(LocalDate.of(2026, 8, 20))
                .build();

        when(estudianteRepository.findByUserAccount_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoryAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of(proximaSesion));
        when(estudianteRepository.findByCategory_CategoryIdAndActiveTrueAndIdNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.myTeam("juan@sged.test");

        assertThat(equipo.coach()).isNotNull();
        assertThat(equipo.coach().name()).isEqualTo("Pedro Gomez");
        assertThat(equipo.coach().specialty()).isEqualTo("Tecnico");
    }

    @Test
    @DisplayName("la categoria trae nombre, rango de edad y descripcion completos")
    void categoriaConDatosCompletos() {
        var yo = estudiante(1L, "Juan", null);
        when(estudianteRepository.findByUserAccount_Username("juan@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.findByCategoryAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                eq(ID_CATEGORIA), any(), any())).thenReturn(List.of());
        when(estudianteRepository.findByCategory_CategoryIdAndActiveTrueAndIdNot(ID_CATEGORIA, 1L))
                .thenReturn(List.of());

        var equipo = servicio.myTeam("juan@sged.test");

        assertThat(equipo.category().name()).isEqualTo("SUB-12");
        assertThat(equipo.category().minAge()).isEqualTo(10);
        assertThat(equipo.category().maxAge()).isEqualTo(12);
        assertThat(equipo.category().description()).isEqualTo("Sub 12 anios");
    }
}
