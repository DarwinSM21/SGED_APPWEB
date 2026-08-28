package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La propiedad que importa en generarSesionesProgramadas(): es idempotente. Se
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
    @Mock private org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository asistenciaRepository;
    @Mock private org.uteq.backend.deportivo.evaluacion.repository.EvaluacionDiariaRepository evaluacionRepository;

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
     * hoy se calcula con Zonas.ECUADOR, igual que HorarioService.generarSesionesProgramadas():
     * usar LocalDate.now() a secas queda desfasado del servicio en la ventana
     * UTC 00:00-04:59 (19:00-23:59 en Ecuador, donde "hoy" en UTC ya es
     * "mañana" en Ecuador), y ese desfase es justo lo que rompia esta prueba
     * en CI (ubuntu-latest corre en UTC) sin fallar nunca en una maquina ya
     * configurada en hora de Ecuador.
     */
    @Test
    @DisplayName("generarSesionesProgramadas solo crea la sesion de los horarios que todavia no la tienen hoy")
    void generarSesionesProgramadas_crea_solo_las_que_faltan() {
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

        service.generarSesionesProgramadas();

        verify(sesionRepository, times(1)).save(any(SesionEntrenamiento.class));
    }

    /**
     * El caso que motivo programar una ventana en vez de solo el dia en
     * curso: con el sistema apagado varios dias, al encenderlo tiene que
     * quedar cubierta la semana completa de una sola vez, no unicamente hoy.
     */
    @Test
    @DisplayName("con la ventana de 7 dias, un horario de lunes a viernes deja programadas 5 sesiones")
    void generarSesionesProgramadas_cubre_toda_la_semana() {
        ReflectionTestUtils.setField(service, "diasProgramados", 7);

        var yo = entrenador(1L);
        var categoria = Categoria.builder().idCategoria(5L).nombre("SUB-12").build();

        // Un horario por cada dia habil, como el caso real de la escuela.
        for (short dia = 1; dia <= 5; dia++) {
            var horario = Horario.builder().idHorario((long) dia).entrenador(yo).categoria(categoria)
                    .diaSemana(dia).horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(18, 0)).build();
            when(horarioRepository.findByActivoTrueAndDiaSemana(dia)).thenReturn(List.of(horario));
        }
        // Sabado y domingo no entrenan.
        when(horarioRepository.findByActivoTrueAndDiaSemana((short) 6)).thenReturn(List.of());
        when(horarioRepository.findByActivoTrueAndDiaSemana((short) 7)).thenReturn(List.of());
        when(sesionRepository.existsByHorario_IdHorarioAndFecha(any(), any())).thenReturn(false);

        service.generarSesionesProgramadas();

        // Cuantos dias habiles caen en la ventana depende del dia en que se
        // ejecute la prueba: de lunes a viernes son 6, pero un sabado o un
        // domingo son 5. Fijar el 6 hacia que la suite fallara los fines de
        // semana por un motivo que no tenia nada que ver con el codigo, asi
        // que el numero se calcula igual que lo hace el servicio.
        long esperadas = java.util.stream.IntStream.rangeClosed(0, 7)
                .mapToObj(i -> LocalDate.now(Zonas.ECUADOR).plusDays(i))
                .filter(fecha -> fecha.getDayOfWeek().getValue() <= 5)
                .count();

        var guardadas = org.mockito.ArgumentCaptor.forClass(SesionEntrenamiento.class);
        verify(sesionRepository, times((int) esperadas)).save(guardadas.capture());
        assertThat(guardadas.getAllValues())
                .allSatisfy(s -> assertThat(s.getFecha().getDayOfWeek().getValue()).isBetween(1, 5));
    }

    @Test
    @DisplayName("nunca se programan fechas pasadas: el horario dice lo que viene, no reconstruye lo que ya paso")
    void generarSesionesProgramadas_no_crea_fechas_pasadas() {
        ReflectionTestUtils.setField(service, "diasProgramados", 7);

        var yo = entrenador(1L);
        var categoria = Categoria.builder().idCategoria(5L).nombre("SUB-12").build();
        for (short dia = 1; dia <= 7; dia++) {
            var horario = Horario.builder().idHorario((long) dia).entrenador(yo).categoria(categoria)
                    .diaSemana(dia).horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(18, 0)).build();
            when(horarioRepository.findByActivoTrueAndDiaSemana(dia)).thenReturn(List.of(horario));
        }
        when(sesionRepository.existsByHorario_IdHorarioAndFecha(any(), any())).thenReturn(false);

        service.generarSesionesProgramadas();

        var guardadas = org.mockito.ArgumentCaptor.forClass(SesionEntrenamiento.class);
        verify(sesionRepository, times(8)).save(guardadas.capture());
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        assertThat(guardadas.getAllValues())
                .allSatisfy(s -> assertThat(s.getFecha()).isAfterOrEqualTo(hoy));
    }

    // --- Editar un horario -----------------------------------------------
    //
    // Antes solo se podia crear y dar de baja. Corregir "entreno a las 16 y no
    // a las 15" obligaba a borrar el horario y reescribirlo entero, y las
    // sesiones ya generadas de esa semana se quedaban con la hora vieja.

    private Horario horarioDe(Entrenador duenio) {
        return Horario.builder().idHorario(9L).entrenador(duenio)
                .categoria(Categoria.builder().idCategoria(5L).nombre("SUB-12").build())
                .diaSemana((short) 1).horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(17, 0))
                .activo(true).build();
    }

    private HorarioRequest peticion(LocalTime inicio, LocalTime fin) {
        return new HorarioRequest(5L, 1, inicio, fin, "Cancha 2", null);
    }

    @Test
    @DisplayName("editar cambia la hora del horario")
    void editar_cambia_la_hora() {
        var yo = entrenador(1L);
        var horario = horarioDe(yo);
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(java.util.Optional.of(yo));
        when(horarioRepository.findByIdHorarioAndEntrenador_IdEntrenador(9L, 1L))
                .thenReturn(java.util.Optional.of(horario));
        when(categoriaRepository.findById(5L)).thenReturn(java.util.Optional.of(horario.getCategoria()));
        when(sesionRepository.findByHorario_IdHorarioAndFechaGreaterThanEqual(eq(9L), any()))
                .thenReturn(java.util.List.of());

        service.editar("carlos@sged.test", 9L, peticion(LocalTime.of(16, 0), LocalTime.of(18, 0)));

        assertThat(horario.getHoraInicio()).isEqualTo(LocalTime.of(16, 0));
        assertThat(horario.getHoraFin()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("editar el horario de otro entrenador da 404, no 403")
    void editar_horario_ajeno_da_404() {
        var yo = entrenador(1L);
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(java.util.Optional.of(yo));
        when(horarioRepository.findByIdHorarioAndEntrenador_IdEntrenador(9L, 1L))
                .thenReturn(java.util.Optional.empty());

        // 404 y no 403 a proposito: un 403 confirmaria que ese horario existe.
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.editar("carlos@sged.test", 9L, peticion(LocalTime.of(16, 0), LocalTime.of(18, 0))));
    }

    @Test
    @DisplayName("editar rechaza una hora de fin que no es posterior a la de inicio")
    void editar_rechaza_horaFin_no_posterior() {
        var yo = entrenador(1L);
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(java.util.Optional.of(yo));
        when(horarioRepository.findByIdHorarioAndEntrenador_IdEntrenador(9L, 1L))
                .thenReturn(java.util.Optional.of(horarioDe(yo)));

        assertThrows(IllegalArgumentException.class,
                () -> service.editar("carlos@sged.test", 9L, peticion(LocalTime.of(18, 0), LocalTime.of(18, 0))));
    }

    @Test
    @DisplayName("al editar NO se borra una sesion futura que ya tiene asistencia registrada")
    void editar_respeta_la_sesion_con_asistencia() {
        var yo = entrenador(1L);
        var horario = horarioDe(yo);
        var yaUsada = SesionEntrenamiento.builder().idSesion(50L).horario(horario)
                .fecha(LocalDate.now(Zonas.ECUADOR)).build();

        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(java.util.Optional.of(yo));
        when(horarioRepository.findByIdHorarioAndEntrenador_IdEntrenador(9L, 1L))
                .thenReturn(java.util.Optional.of(horario));
        when(categoriaRepository.findById(5L)).thenReturn(java.util.Optional.of(horario.getCategoria()));
        when(sesionRepository.findByHorario_IdHorarioAndFechaGreaterThanEqual(eq(9L), any()))
                .thenReturn(java.util.List.of(yaUsada));
        when(asistenciaRepository.findBySesionIdSesion(50L)).thenReturn(java.util.List.of(
                org.uteq.backend.deportivo.asistencia.entity.Asistencia.builder().idAsistencia(1L).build()));

        service.editar("carlos@sged.test", 9L, peticion(LocalTime.of(16, 0), LocalTime.of(18, 0)));

        // Un entrenamiento que ya se dicto es un hecho: moverlo de hora
        // reescribiria a que sesion fue cada estudiante.
        verify(sesionRepository, never()).delete(yaUsada);
    }

    @Test
    @DisplayName("al editar SI se rehace una sesion futura en la que nadie registro nada")
    void editar_rehace_la_sesion_vacia() {
        var yo = entrenador(1L);
        var horario = horarioDe(yo);
        var vacia = SesionEntrenamiento.builder().idSesion(51L).horario(horario)
                .fecha(LocalDate.now(Zonas.ECUADOR).plusDays(3)).build();

        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(java.util.Optional.of(yo));
        when(horarioRepository.findByIdHorarioAndEntrenador_IdEntrenador(9L, 1L))
                .thenReturn(java.util.Optional.of(horario));
        when(categoriaRepository.findById(5L)).thenReturn(java.util.Optional.of(horario.getCategoria()));
        when(sesionRepository.findByHorario_IdHorarioAndFechaGreaterThanEqual(eq(9L), any()))
                .thenReturn(java.util.List.of(vacia));
        when(asistenciaRepository.findBySesionIdSesion(51L)).thenReturn(java.util.List.of());
        when(evaluacionRepository.existsBySesionIdSesion(51L)).thenReturn(false);

        service.editar("carlos@sged.test", 9L, peticion(LocalTime.of(16, 0), LocalTime.of(18, 0)));

        verify(sesionRepository).delete(vacia);
    }

    // ------------------------------------------------------------------
    // Un entrenador no se parte en dos
    //
    // La escuela tiene POCOS entrenadores y uno cubre varias categorias, asi
    // que encadenarlas el mismo dia es lo normal. Lo que no puede es tenerlas
    // a la vez: eso genera una sesion por semana a la que nadie va, y el
    // sistema termina cobrandole esa ausencia a los chicos.
    // ------------------------------------------------------------------

    private Horario horarioDe(long id, String categoria, LocalTime inicio, LocalTime fin) {
        return Horario.builder()
                .idHorario(id)
                .categoria(Categoria.builder().idCategoria(id).nombre(categoria).build())
                .diaSemana((short) 2)
                .horaInicio(inicio).horaFin(fin)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("no deja crear un horario que se cruza con otro del mismo entrenador")
    void crear_rechaza_cruce() {
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test"))
                .thenReturn(Optional.of(entrenador(1L)));
        when(categoriaRepository.findById(5L))
                .thenReturn(Optional.of(Categoria.builder().idCategoria(5L).nombre("SUB-16").build()));
        when(horarioRepository.cruzadosCon(eq(1L), eq((short) 2), any(), any(), any()))
                .thenReturn(List.of(horarioDe(9L, "SUB-14", LocalTime.of(16, 0), LocalTime.of(18, 0))));

        var request = new HorarioRequest(5L, 2, LocalTime.of(16, 0), LocalTime.of(18, 0), null, null);

        var error = assertThrows(IllegalArgumentException.class,
                () -> service.crear("carlos@sged.test", request));
        // El mensaje tiene que nombrar el culpable: con pocos entrenadores se
        // choca seguido y un "no se puede" a secas obliga a ir a buscarlo.
        assertThat(error.getMessage()).contains("SUB-14");
        verify(horarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("dos categorias seguidas SI se permiten: terminar 18:00 y empezar 18:00 no es cruzarse")
    void crear_permite_encadenar() {
        var yo = entrenador(1L);
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(5L))
                .thenReturn(Optional.of(Categoria.builder().idCategoria(5L).nombre("SUB-16").build()));
        // La consulta ya excluye los que solo se tocan por el extremo.
        when(horarioRepository.cruzadosCon(eq(1L), eq((short) 2), any(), any(), any()))
                .thenReturn(List.of());
        when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> {
            Horario h = inv.getArgument(0);
            h.setIdHorario(7L);
            return h;
        });

        var request = new HorarioRequest(5L, 2, LocalTime.of(18, 0), LocalTime.of(20, 0), null, null);
        var respuesta = service.crear("carlos@sged.test", request);

        assertThat(respuesta.idHorario()).isEqualTo(7L);
        verify(horarioRepository).save(any(Horario.class));
    }

    @Test
    @DisplayName("la cancha NO bloquea: dos grupos pueden compartirla")
    void crear_no_valida_la_cancha() {
        var yo = entrenador(1L);
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoriaRepository.findById(5L))
                .thenReturn(Optional.of(Categoria.builder().idCategoria(5L).nombre("SUB-16").build()));
        when(horarioRepository.cruzadosCon(eq(1L), eq((short) 2), any(), any(), any()))
                .thenReturn(List.of());
        when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

        // Misma cancha, misma hora, pero es otro entrenador: se permite.
        var request = new HorarioRequest(5L, 2, LocalTime.of(16, 0), LocalTime.of(18, 0),
                "Cancha principal", null);
        service.crear("carlos@sged.test", request);

        verify(horarioRepository).save(any(Horario.class));
    }

    @Test
    @DisplayName("al editar, el horario no choca consigo mismo")
    void editar_se_excluye_a_si_mismo() {
        var yo = entrenador(1L);
        Horario existente = horarioDe(7L, "SUB-16", LocalTime.of(16, 0), LocalTime.of(18, 0));
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(horarioRepository.findByIdHorarioAndEntrenador_IdEntrenador(7L, 1L))
                .thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(5L))
                .thenReturn(Optional.of(Categoria.builder().idCategoria(5L).nombre("SUB-16").build()));
        when(horarioRepository.cruzadosCon(eq(1L), eq((short) 2), any(), any(), eq(7L)))
                .thenReturn(List.of());
        when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new HorarioRequest(5L, 2, LocalTime.of(16, 30), LocalTime.of(18, 30), null, null);
        service.editar("carlos@sged.test", 7L, request);

        // Lo que se comprueba es el 7L del ultimo parametro: sin esa exclusion,
        // mover un horario media hora chocaria contra su propia version vieja.
        verify(horarioRepository).cruzadosCon(eq(1L), eq((short) 2), any(), any(), eq(7L));
    }

    @Test
    @DisplayName("la lista marca los horarios que YA estaban cruzados")
    void misHorarios_marca_los_cruces_existentes() {
        var yo = entrenador(1L);
        when(entrenadorRepository.findByUsuario_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(horarioRepository
                .findByEntrenador_IdEntrenadorAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(1L))
                .thenReturn(List.of(
                        horarioDe(1L, "SUB-12", LocalTime.of(16, 0), LocalTime.of(18, 0)),
                        horarioDe(2L, "SUB-14", LocalTime.of(16, 0), LocalTime.of(18, 0)),
                        horarioDe(3L, "SUB-18", LocalTime.of(18, 0), LocalTime.of(20, 0))));

        var lista = service.misHorarios("carlos@sged.test");

        // Validar el alta no arregla lo ya cargado: hay que poder verlo.
        assertThat(lista.get(0).chocaCon()).contains("SUB-14");
        assertThat(lista.get(1).chocaCon()).contains("SUB-12");
        // El de 18:00 arranca cuando el otro termina: no se cruza.
        assertThat(lista.get(2).chocaCon()).isNull();
    }
}
