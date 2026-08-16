package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.horario.dto.HorarioRequest;
import org.uteq.backend.deportivo.horario.entity.Horario;
import org.uteq.backend.deportivo.horario.repository.HorarioRepository;
import org.uteq.backend.deportivo.horario.service.HorarioService;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La propiedad que importa en generarSesionesDeHoy(): es idempotente. Se
 * llama en cada GET /api/sesiones/hoy y /mias (ver
 * SesionEntrenamientoController), asi que si no evita duplicar la sesion de
 * un horario ya generado hoy, cada recarga de pantalla crearia una fila
 * nueva.
 */
@ExtendWith(MockitoExtension.class)
class HorarioServiceTest {

    @Mock private HorarioRepository horarioRepository;
    @Mock private EntrenadorRepository entrenadorRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;

    @InjectMocks private HorarioService service;

    private Entrenador entrenador(long id) {
        return Entrenador.builder().idEntrenador(id)
                .persona(Persona.builder().nombre("Carlos").apellido("Apellido").build())
                .build();
    }

    @Test
    @DisplayName("crear rechaza una hora de fin que no es posterior a la de inicio")
    void crear_rechaza_horaFin_no_posterior() {
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test"))
                .thenReturn(Optional.of(entrenador(1L)));

        var request = new HorarioRequest(5L, 1, LocalTime.of(17, 0), LocalTime.of(16, 0), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.crear("carlos@sged.test", request));
        verify(horarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear responde 404 si la categoria no existe")
    void crear_categoria_inexistente_lanza_404() {
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test"))
                .thenReturn(Optional.of(entrenador(1L)));
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        var request = new HorarioRequest(999L, 1, LocalTime.of(16, 0), LocalTime.of(17, 0), null, null);

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear("carlos@sged.test", request));
    }

    @Test
    @DisplayName("crear persiste el horario a nombre del entrenador autenticado")
    void crear_persiste_horario() {
        var yo = entrenador(1L);
        var categoria = Categoria.builder().idCategoria(5L).nombre("SUB-12").build();
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> {
            Horario h = inv.getArgument(0);
            h.setIdHorario(10L);
            return h;
        });

        var request = new HorarioRequest(5L, 1, LocalTime.of(16, 0), LocalTime.of(18, 0), "Cancha 1", null);
        var response = service.crear("carlos@sged.test", request);

        assertThat(response.idHorario()).isEqualTo(10L);
        assertThat(response.categoria()).isEqualTo("SUB-12");
        assertThat(response.diaSemana()).isEqualTo(1);
    }

    @Test
    @DisplayName("misHorarios devuelve vacio si la cuenta ENTRENADOR no tiene fila asociada, no falla")
    void misHorarios_sin_entrenador_asociado_lista_vacia() {
        when(entrenadorRepository.findByUsuario_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThat(service.misHorarios("huerfano@sged.test")).isEmpty();
    }

    @Test
    @DisplayName("desactivar responde 404 si el horario no existe o no es del entrenador autenticado")
    void desactivar_ajeno_lanza_404() {
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(entrenador(1L)));
        when(horarioRepository.findByIdHorarioAndEntrenador_IdEntrenador(50L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.desactivar("carlos@sged.test", 50L));
        verify(horarioRepository, never()).save(any());
    }

    /**
     * hoy se calcula con Zonas.ECUADOR, igual que HorarioService.generarSesionesDeHoy():
     * usar LocalDate.now() a secas queda desfasado del servicio en la ventana
     * UTC 00:00-04:59 (19:00-23:59 en Ecuador, donde "hoy" en UTC ya es
     * "mañana" en Ecuador), y ese desfase es justo lo que rompia esta prueba
     * en CI (ubuntu-latest corre en UTC) sin fallar nunca en una maquina ya
     * configurada en hora de Ecuador.
     */
    @Test
    @DisplayName("generarSesionesDeHoy solo crea la sesion de los horarios que todavia no la tienen hoy")
    void generarSesionesDeHoy_crea_solo_las_que_faltan() {
        var yo = entrenador(1L);
        var categoria = Categoria.builder().idCategoria(5L).nombre("SUB-12").build();
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        short diaDeHoy = (short) hoy.getDayOfWeek().getValue();
        var horarioSinSesionHoy = Horario.builder().idHorario(1L).entrenador(yo).categoria(categoria)
                .diaSemana(diaDeHoy).horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(18, 0)).build();
        var horarioYaGenerado = Horario.builder().idHorario(2L).entrenador(yo).categoria(categoria)
                .diaSemana(diaDeHoy).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0)).build();

        when(horarioRepository.findByActivoTrueAndDiaSemana(diaDeHoy))
                .thenReturn(List.of(horarioSinSesionHoy, horarioYaGenerado));
        when(sesionRepository.existsByHorario_IdHorarioAndFecha(1L, hoy)).thenReturn(false);
        when(sesionRepository.existsByHorario_IdHorarioAndFecha(2L, hoy)).thenReturn(true);

        service.generarSesionesDeHoy();

        verify(sesionRepository, times(1)).save(any(SesionEntrenamiento.class));
    }
}
